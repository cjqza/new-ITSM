
"""RAG-Enhanced AI Agent - Integrates knowledge base retrieval with LangGraph agent."""

import os
import logging
import re
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional

import httpx
from rag_core import search_knowledge

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

AI_AGENT_URL = os.getenv("AI_AGENT_URL", "http://localhost:8090")
RAG_SERVICE_URL = os.getenv("RAG_SERVICE_URL", "http://localhost:8091")


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("RAG Enhanced Agent starting on port 8092")
    yield


app = FastAPI(title="ITSM RAG Enhanced Agent", version="1.0", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1)
    history: Optional[List[ChatMessage]] = Field(default_factory=list)


class ChatResponse(BaseModel):
    response: str
    classification: str
    priority: str
    confidence: float
    shouldHandoff: bool
    handoffReason: str


def enhance_prompt_with_rag(user_message: str) -> str:
    try:
        context = search_knowledge(user_message, top_k=3)
        if not context:
            return user_message
        enhanced = "Based on the following knowledge base information, answer the user question:\n\n" + context + "\n\n---\n\nUser question: " + user_message
        logger.info("Enhanced prompt with RAG context (%d chars)", len(context))
        return enhanced
    except Exception as e:
        logger.error("RAG enhancement failed: %s", e)
        return user_message


@app.post("/api/v1/ai/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    try:
        enhanced_message = enhance_prompt_with_rag(req.message)
        agent_request = {"message": enhanced_message, "history": [m.dict() for m in (req.history or [])]}
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(f"{AI_AGENT_URL}/api/v1/ai/chat", json=agent_request)
            if response.status_code != 200:
                raise HTTPException(status_code=502, detail=f"AI Agent error: {response.status_code}")
            result = response.json()
            rag_ctx = search_knowledge(req.message, top_k=1)
            if rag_ctx:
                src = re.search(r"source: ([^,]+)", rag_ctx)
                if src:
                    result["response"] = "[Knowledge ref: " + src.group(1) + "]\n\n" + result["response"]
            return ChatResponse(**result)
    except httpx.TimeoutException:
        raise HTTPException(status_code=504, detail="AI Agent timeout")
    except Exception as e:
        logger.exception("Enhanced chat failed")
        raise HTTPException(status_code=500, detail=str(e)[:200])


@app.get("/api/v1/ai/health")
async def health():
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            ai_ok = (await client.get(f"{AI_AGENT_URL}/api/v1/ai/health")).status_code == 200
            rag_ok = (await client.get(f"{RAG_SERVICE_URL}/api/v1/rag/health")).status_code == 200
        return {"status": "ok", "ai_agent": ai_ok, "rag_service": rag_ok, "enhanced": True}
    except Exception as e:
        return {"status": "error", "error": str(e)}


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("ENHANCED_AGENT_PORT", "8092"))
    uvicorn.run(app, host="0.0.0.0", port=port)
