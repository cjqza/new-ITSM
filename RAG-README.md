# ITSM RAG Knowledge Base System

This system integrates a RAG (Retrieval-Augmented Generation) knowledge base with your AI customer service agent, enabling it to provide more accurate and context-aware responses based on your ITSM documentation.

## System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Java ITSM     │    │  RAG Enhanced   │    │   Python AI     │
│   Server        │───▶│  Agent          │───▶│   Agent         │
│   (port 8080)   │    │  (port 8092)    │    │   (port 8090)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   RAG Service   │
                       │   (port 8091)   │
                       └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   FAISS Index   │
                       │   (local files) │
                       └─────────────────┘
```

## Components

### 1. Knowledge Base (`C:\work\java\ITSM\knowledge\`)
- Contains your ITSM documentation in Markdown format
- Currently includes: product specifications, API docs, project plans
- Add new documents to this directory to expand the knowledge base

### 2. FAISS Index (`C:\work\java\ITSM\faiss_index\`)
- Vector embeddings of all knowledge chunks
- Built using DashScope text-embedding-v3 API
- Stored locally for fast retrieval

### 3. RAG Service (port 8091)
- HTTP API for knowledge base search
- Endpoints:
  - `GET /api/v1/rag/health` - Health check
  - `POST /api/v1/rag/search` - Search knowledge base
  - `POST /api/v1/rag/rebuild` - Rebuild index

### 4. RAG Enhanced Agent (port 8092)
- Proxy service that enhances AI agent responses with RAG context
- Automatically retrieves relevant knowledge before generating responses
- Compatible with the original AI agent API

## Quick Start

### 1. Start All Services
```powershell
.\start-rag-system.ps1
```

### 2. Verify Services
```powershell
# Check RAG service
curl http://localhost:8091/api/v1/rag/health

# Check Enhanced Agent
curl http://localhost:8092/api/v1/ai/health
```

### 3. Test RAG Search
```powershell
.\manage-knowledge.ps1 search -Query "如何转人工客服"
```

## Knowledge Base Management

### List Documents
```powershell
.\manage-knowledge.ps1 list
```

### Add New Document
```powershell
.\manage-knowledge.ps1 add -File "C:\path\to\new-document.md"
```

### Rebuild Index
```powershell
.\manage-knowledge.ps1 rebuild
```

## API Endpoints

### RAG Service (port 8091)

#### Search Knowledge Base
```bash
POST http://localhost:8091/api/v1/rag/search
Content-Type: application/json

{
  "query": "你的问题",
  "top_k": 5
}
```

Response:
```json
{
  "context": "[知识库片段1] (来源: xxx.md, 相关度: 0.85)\n...",
  "results": [
    {
      "source": "文档名.md",
      "content": "相关内容...",
      "score": 0.85
    }
  ]
}
```

#### Rebuild Index
```bash
POST http://localhost:8091/api/v1/rag/rebuild
Content-Type: application/json

{
  "doc_dir": "C:\\work\\java\\ITSM\\knowledge"  // optional
}
```

### RAG Enhanced Agent (port 8092)

#### Chat with RAG Enhancement
```bash
POST http://localhost:8092/api/v1/ai/chat
Content-Type: application/json

{
  "message": "我的电脑连不上WiFi了",
  "history": []
}
```

Response includes RAG-enhanced answers with knowledge base references.

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DASHSCOPE_API_KEY` | (required) | DashScope API key for embeddings |
| `KNOWLEDGE_DIR` | `C:\work\java\ITSM\knowledge` | Knowledge documents directory |
| `INDEX_DIR` | `C:\work\java\ITSM\faiss_index` | FAISS index storage directory |
| `RAG_SERVICE_PORT` | `8091` | RAG service port |
| `ENHANCED_AGENT_PORT` | `8092` | Enhanced agent port |
| `AI_AGENT_URL` | `http://localhost:8090` | Original AI agent URL |
| `CHUNK_SIZE` | `500` | Text chunk size (characters) |
| `CHUNK_OVERLAP` | `100` | Overlap between chunks |
| `RAG_TOP_K` | `5` | Number of results to retrieve |

## Adding New Knowledge

1. Add your Markdown/Text documents to `C:\work\java\ITSM\knowledge\`
2. Run the rebuild command:
   ```powershell
   .\manage-knowledge.ps1 rebuild
   ```
3. The index will be updated automatically

## Troubleshooting

### Services Not Starting
- Check if ports 8091/8092 are available
- Verify DASHSCOPE_API_KEY is set in `ai-agent\.env`
- Check Python dependencies: `pip install faiss-cpu numpy httpx fastapi uvicorn`

### Poor Search Results
- Increase `RAG_TOP_K` for more context
- Adjust `CHUNK_SIZE` and `CHUNK_OVERLAP` for better granularity
- Add more relevant documents to the knowledge base

### Slow Performance
- Reduce `CHUNK_SIZE` for faster processing
- Use smaller `RAG_TOP_K` values
- Consider using a faster embedding model

## File Structure

```
C:\work\java\ITSM\
├── knowledge/                    # Knowledge base documents
├── faiss_index/                  # Vector index files
├── ai-rag/                       # RAG system code
│   ├── rag_core.py              # Core RAG logic
│   ├── rag_build.py             # Index builder
│   ├── rag_service.py           # Search API
│   └── rag_enhanced_agent.py    # Enhanced agent
├── ai-agent/                     # Original AI agent
├── start-rag-system.ps1         # Startup script
└── manage-knowledge.ps1         # Management script
```

## Next Steps

1. Add more ITSM documentation to the knowledge base
2. Customize the RAG parameters for your use case
3. Integrate with your existing ITSM workflows
4. Monitor and improve search relevance
