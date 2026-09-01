"""
RAG Core - Knowledge base indexing and retrieval.
Uses DashScope text-embedding API for vectorization and FAISS for local vector search.
"""

import os
import json
import hashlib
import logging
import re
from pathlib import Path
from typing import List, Dict, Optional

import httpx
import faiss
import numpy as np

logger = logging.getLogger(__name__)

# ---------- Configuration ----------
DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY", "")
DASHSCOPE_BASE_URL = os.getenv("DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "text-embedding-v3")
EMBEDDING_DIM = int(os.getenv("EMBEDDING_DIM", "1024"))

KNOWLEDGE_DIR = os.getenv("KNOWLEDGE_DIR", os.path.join(os.path.dirname(__file__), "..", "knowledge"))
INDEX_DIR = os.getenv("INDEX_DIR", os.path.join(os.path.dirname(__file__), "..", "faiss_index"))

CHUNK_SIZE = int(os.getenv("CHUNK_SIZE", "500"))
CHUNK_OVERLAP = int(os.getenv("CHUNK_OVERLAP", "100"))
TOP_K = int(os.getenv("RAG_TOP_K", "5"))


def load_documents(doc_dir: str) -> List[Dict]:
    """Load all .md and .txt files from a directory recursively."""
    docs = []
    doc_path = Path(doc_dir)
    if not doc_path.exists():
        logger.warning("Knowledge directory does not exist: %s", doc_dir)
        return docs
    for fp in doc_path.rglob("*"):
        if fp.suffix.lower() in (".md", ".txt", ".markdown"):
            try:
                content = fp.read_text(encoding="utf-8")
                if content.strip():
                    docs.append({"source": str(fp.relative_to(doc_path)), "content": content})
                    logger.info("Loaded: %s (%d chars)", fp.name, len(content))
            except Exception as e:
                logger.error("Failed to load %s: %s", fp, e)
    return docs


def chunk_text(text: str, chunk_size: int = CHUNK_SIZE, overlap: int = CHUNK_OVERLAP) -> List[str]:
    """Split text into overlapping chunks respecting paragraph boundaries."""
    paragraphs = re.split(r"\n{2,}", text.strip())
    chunks, current = [], ""
    for para in paragraphs:
        para = para.strip()
        if not para:
            continue
        if current and len(current) + len(para) + 2 > chunk_size:
            chunks.append(current.strip())
            current = current[-overlap:] + "\n\n" + para if overlap > 0 and len(current) > overlap else para
        else:
            current = (current + "\n\n" + para).strip() if current else para
    if current.strip():
        chunks.append(current.strip())
    final = []
    for c in chunks:
        if len(c) <= chunk_size * 1.5:
            final.append(c)
        else:
            for i in range(0, len(c), chunk_size - overlap):
                sub = c[i:i + chunk_size]
                if sub.strip():
                    final.append(sub.strip())
    return final


def build_chunk_records(docs: List[Dict]) -> List[Dict]:
    records = []
    for doc in docs:
        chunks = chunk_text(doc["content"])
        for i, chunk in enumerate(chunks):
            records.append({"source": doc["source"], "chunk_index": i, "content": chunk,
                            "hash": hashlib.md5(chunk.encode()).hexdigest()})
    logger.info("Built %d chunks from %d documents", len(records), len(docs))
    return records


def get_embeddings(texts: List[str]) -> List[List[float]]:
    """Get embeddings from DashScope API."""
    if not DASHSCOPE_API_KEY:
        raise ValueError("DASHSCOPE_API_KEY is not set")
    all_emb = []
    for i in range(0, len(texts), 10):
        batch = texts[i:i + 10]
        resp = httpx.post(
            f"{DASHSCOPE_BASE_URL}/embeddings",
            headers={"Authorization": f"Bearer {DASHSCOPE_API_KEY}", "Content-Type": "application/json"},
            json={"model": EMBEDDING_MODEL, "input": batch, "dimensions": EMBEDDING_DIM},
            timeout=60,
        )
        if resp.status_code != 200:
            logger.error('Embedding API error %d: %s', resp.status_code, resp.text[:500])
        resp.raise_for_status()
        for item in sorted(resp.json()["data"], key=lambda x: x["index"]):
            all_emb.append(item["embedding"])
    return all_emb


class KnowledgeIndex:
    def __init__(self):
        self.index = None
        self.chunks = []
        self.index_path = Path(INDEX_DIR)

    def build(self, doc_dir=None):
        doc_dir = doc_dir or KNOWLEDGE_DIR
        docs = load_documents(doc_dir)
        if not docs:
            logger.warning("No documents found in %s", doc_dir)
            return
        self.chunks = build_chunk_records(docs)
        if not self.chunks:
            return
        texts = [c["content"] for c in self.chunks]
        logger.info("Generating embeddings for %d chunks...", len(texts))
        embeddings = get_embeddings(texts)
        vectors = np.array(embeddings, dtype="float32")
        faiss.normalize_L2(vectors)
        self.index = faiss.IndexFlatIP(vectors.shape[1])
        self.index.add(vectors)
        self.index_path.mkdir(parents=True, exist_ok=True)
        faiss.write_index(self.index, str(self.index_path / "index.faiss"))
        with open(self.index_path / "chunks.json", "w", encoding="utf-8") as f:
            json.dump(self.chunks, f, ensure_ascii=False, indent=2)
        logger.info("Index built: %d vectors, dim=%d", self.index.ntotal, vectors.shape[1])

    def load(self) -> bool:
        idx_f = self.index_path / "index.faiss"
        chk_f = self.index_path / "chunks.json"
        if not idx_f.exists() or not chk_f.exists():
            return False
        self.index = faiss.read_index(str(idx_f))
        with open(chk_f, "r", encoding="utf-8") as f:
            self.chunks = json.load(f)
        logger.info("Index loaded: %d vectors, %d chunks", self.index.ntotal, len(self.chunks))
        return True

    def search(self, query: str, top_k: int = TOP_K) -> List[Dict]:
        if self.index is None or self.index.ntotal == 0:
            return []
        embeddings = get_embeddings([query])
        qv = np.array(embeddings, dtype="float32")
        faiss.normalize_L2(qv)
        scores, indices = self.index.search(qv, min(top_k, self.index.ntotal))
        results = []
        for score, idx in zip(scores[0], indices[0]):
            if idx < 0:
                continue
            chunk = self.chunks[idx].copy()
            chunk["score"] = float(score)
            results.append(chunk)
        return results


_index = None

def get_index() -> KnowledgeIndex:
    global _index
    if _index is None:
        _index = KnowledgeIndex()
        if not _index.load():
            logger.warning("Knowledge index not loaded. Run rag_build.py first.")
    return _index

def rebuild_index(doc_dir=None) -> KnowledgeIndex:
    global _index
    _index = KnowledgeIndex()
    _index.build(doc_dir)
    return _index

def search_knowledge(query: str, top_k: int = TOP_K) -> str:
    """Search knowledge base and return formatted context string."""
    results = get_index().search(query, top_k)
    if not results:
        return ""
    parts = []
    for i, r in enumerate(results, 1):
        parts.append(f"[知识库片段{i}] (来源: {r['source']}, 相关度: {r['score']:.2f})\n{r['content']}")
    return "\n\n---\n\n".join(parts)
