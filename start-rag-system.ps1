# ITSM RAG System Startup Script
# Starts all RAG-related services

Write-Host "Starting ITSM RAG System..." -ForegroundColor Green

# Load environment variables from .env file
$envFile = "C:\work\java\ITSM\ai-agent\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $parts = $line.Split('=', 2)
        if ($parts.Count -eq 2) {
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()
            if ($name -ne '' -and -not (Test-Path "Env:$name")) {
                Set-Item "Env:$name" $value
            }
        }
    }
    Write-Host "Loaded environment variables from .env" -ForegroundColor Yellow
}

# Set RAG-specific environment variables
$env:INDEX_DIR = "C:\work\java\ITSM\faiss_index"
$env:KNOWLEDGE_DIR = "C:\work\java\ITSM\knowledge"
$env:RAG_SERVICE_PORT = "8091"
$env:ENHANCED_AGENT_PORT = "8092"

# Start RAG Service (port 8091)
Write-Host "Starting RAG Service on port 8091..." -ForegroundColor Cyan
Start-Process -FilePath "python" -ArgumentList "C:\work\java\ITSM\ai-rag\rag_service.py" -WorkingDirectory "C:\work\java\ITSM\ai-rag" -WindowStyle Hidden
Start-Sleep -Seconds 2

# Start RAG Enhanced Agent (port 8092)
Write-Host "Starting RAG Enhanced Agent on port 8092..." -ForegroundColor Cyan
Start-Process -FilePath "python" -ArgumentList "C:\work\java\ITSM\ai-rag\rag_enhanced_agent.py" -WorkingDirectory "C:\work\java\ITSM\ai-rag" -WindowStyle Hidden
Start-Sleep -Seconds 2

# Check services
Write-Host "`nChecking services..." -ForegroundColor Yellow

$services = @(
    @{Name="RAG Service"; Port=8091},
    @{Name="RAG Enhanced Agent"; Port=8092}
)

foreach ($svc in $services) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$($svc.Port)/api/v1/rag/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($svc.Port -eq 8092) {
            $response = Invoke-WebRequest -Uri "http://localhost:$($svc.Port)/api/v1/ai/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
        }
        Write-Host "✓ $($svc.Name) (port $($svc.Port)): Running" -ForegroundColor Green
    } catch {
        Write-Host "✗ $($svc.Name) (port $($svc.Port)): Failed to start" -ForegroundColor Red
    }
}

Write-Host "`nRAG System started!" -ForegroundColor Green
Write-Host "RAG Service: http://localhost:8091" -ForegroundColor White
Write-Host "Enhanced Agent: http://localhost:8092" -ForegroundColor White
Write-Host "`nJava service should be configured to use: http://localhost:8092" -ForegroundColor Yellow
