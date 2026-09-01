"""
FastAPI server wrapping the LangGraph AI agent.

POST /api/v1/ai/chat
  Request body: { "message": "...", "history": [{ "role": "user|assistant", "content": "..." }] }
  Response:      { "response": "...", "classification": "...", "priority": "...",
                   "confidence": 0.85, "shouldHandoff": false, "handoffReason": "" }

GET  /api/v1/ai/health
  Response: { "status": "ok" }
"""

import os
import logging
import json
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional

from agent import invoke_agent

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

# ---------- Lifespan ----------
@asynccontextmanager
async def lifespan(app: FastAPI):
    api_key = os.getenv("DASHSCOPE_API_KEY", "")
    if not api_key:
        logger.warning("DASHSCOPE_API_KEY is not set — agent calls will fail")
    else:
        logger.info("AI agent ready (model=%s)", os.getenv("DASHSCOPE_MODEL", "qwen-plus"))
    yield

app = FastAPI(title="ITSM AI Agent", version="1.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------- Models ----------
class ChatMessage(BaseModel):
    role: str = Field(..., description="user or assistant")
    content: str

class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, description="User message content")
    history: Optional[List[ChatMessage]] = Field(default_factory=list, description="Chat history")

class ChatResponse(BaseModel):
    response: str
    classification: str
    priority: str
    confidence: float
    shouldHandoff: bool
    handoffReason: str

# ---------- Routes ----------
@app.get(/)
async def root():
    return {"status": "ok", "name": "ITSM AI Agent"}

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    try:
        body = await request.body()
        body_text = body.decode("utf-8", errors="replace")
    except Exception:
        body_text = "<unreadable>"
    logger.error("422 validation error: %s | body=%s", exc.errors(), body_text)
    return JSONResponse(status_code=422, content={"detail": exc.errors()})

@app.get("/api/v1/ai/health")
def health():
    return {"status": "ok"}

@app.post("/api/v1/ai/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    try:
        history = [{"role": m.role, "content": m.content} for m in (req.history or [])]
        result = invoke_agent(req.message, history)
        return ChatResponse(**result)
    except Exception as e:
        logger.exception("Agent invoke failed")
        raise HTTPException(status_code=503, detail=f"Agent unavailable: {str(e)[:200]}")

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("AI_AGENT_PORT", "8090"))
    uvicorn.run(app, host="0.0.0.0", port=port)
