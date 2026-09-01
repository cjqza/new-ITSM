# ITSM AI Agent - Start Script
# Usage: powershell -File start.ps1
# Optional env vars: DASHSCOPE_API_KEY, DASHSCOPE_BASE_URL, DASHSCOPE_MODEL, AI_AGENT_PORT

$ErrorActionPreference = 'Stop'

$envFile = Join-Path $PSScriptRoot '.env'
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
}

if (-not $env:DASHSCOPE_API_KEY) {
    Write-Host 'WARNING: DASHSCOPE_API_KEY is not set. Create ai-agent/.env with DASHSCOPE_API_KEY=...' -ForegroundColor Yellow
}

$env:DASHSCOPE_BASE_URL = if ($env:DASHSCOPE_BASE_URL) { $env:DASHSCOPE_BASE_URL } else { 'https://dashscope.aliyuncs.com/compatible-mode/v1' }
$env:DASHSCOPE_MODEL = if ($env:DASHSCOPE_MODEL) { $env:DASHSCOPE_MODEL } else { 'qwen-plus' }
$env:AI_AGENT_PORT = if ($env:AI_AGENT_PORT) { $env:AI_AGENT_PORT } else { '8090' }

Write-Host "Starting ITSM AI Agent on port $env:AI_AGENT_PORT ..." -ForegroundColor Green
python server.py
