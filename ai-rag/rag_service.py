"""
RAG Retrieval Service - HTTP microservice for knowledge base search.

Runs on port 8091 by default. The Python AI agent calls this service
to retrieve relevant knowledge before generating responses.

POST /api/v1/rag/search
  Request:  { "query": "...", "top_k": 5 }
  Response: { "context": "...", "results": [...] }

POST /api/v1/rag/rebuild
  Response: { "status": "ok", "chunks": 123, "vectors": 123 }

GET /api/v1/rag/health
  Response: { "status": "ok", "vectors": 123 }
"""

import os
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional

from rag_core import get_index, rebuild_index, search_knowledge, KNOWLEDGE_DIR

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    idx = get_index()
    if idx.index and idx.index.ntotal > 0:
        logger.info("RAG service ready (%d vectors)", idx.index.ntotal)
    else:
        logger.warning("Knowledge index is empty. POST /api/v1/rag/rebuild to build it.")
    yield


app = FastAPI(title="ITSM RAG Service", version="1.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class SearchRequest(BaseModel):
    query: str = Field(..., min_length=1)
    top_k: Optional[int] = Field(default=5, ge=1, le=20)


class SearchResult(BaseModel):
    source: str
    content: str
    score: float


class SearchResponse(BaseModel):
    context: str
    results: List[SearchResult]


class RebuildRequest(BaseModel):
    doc_dir: Optional[str] = None


@app.get("/api/v1/rag/health")
def health():
    idx = get_index()
    return {"status": "ok", "vectors": idx.index.ntotal if idx.index else 0, "chunks": len(idx.chunks)}


@app.post("/api/v1/rag/search", response_model=SearchResponse)
def search(req: SearchRequest):
    try:
        context = search_knowledge(req.query, req.top_k or 5)
        results = get_index().search(req.query, req.top_k or 5)
        return SearchResponse(
            context=context,
            results=[SearchResult(source=r["source"], content=r["content"], score=r["score"]) for r in results],
        )
    except Exception as e:
        logger.exception("RAG search failed")
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)[:200]}")


@app.post("/api/v1/rag/rebuild")
def rebuild(req: RebuildRequest = None):
    try:
        doc_dir = (req.doc_dir if req and req.doc_dir else None) or KNOWLEDGE_DIR
        index = rebuild_index(doc_dir)
        return {
            "status": "ok",
            "vectors": index.index.ntotal if index.index else 0,
            "chunks": len(index.chunks),
        }
    except Exception as e:
        logger.exception("RAG rebuild failed")
        raise HTTPException(status_code=500, detail=f"Rebuild failed: {str(e)[:200]}")


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("RAG_SERVICE_PORT", "8091"))
    uvicorn.run(app, host="0.0.0.0", port=port)
