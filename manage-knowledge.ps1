# ITSM Knowledge Base Management Script
# Usage: .\manage-knowledge.ps1 [command]
# Commands: rebuild, add, list, search

param(
    [string]$Command = "rebuild",
    [string]$Query = "",
    [string]$File = ""
)

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
}

$env:INDEX_DIR = "C:\work\java\ITSM\faiss_index"
$env:KNOWLEDGE_DIR = "C:\work\java\ITSM\knowledge"

switch ($Command) {
    "rebuild" {
        Write-Host "Rebuilding knowledge index..." -ForegroundColor Cyan
        python C:\work\java\ITSM\ai-rag\rag_build.py --doc-dir C:\work\java\ITSM\knowledge
    }
    "add" {
        if (-not $File) {
            Write-Host "Please specify file: .\manage-knowledge.ps1 add -File 'path\to\file.md'" -ForegroundColor Red
            exit 1
        }
        if (-not (Test-Path $File)) {
            Write-Host "File not found: $File" -ForegroundColor Red
            exit 1
        }
        $dest = Join-Path $env:KNOWLEDGE_DIR (Split-Path $File -Leaf)
        Copy-Item $File $dest -Force
        Write-Host "Added: $dest" -ForegroundColor Green
        Write-Host "Run '.\manage-knowledge.ps1 rebuild' to update the index." -ForegroundColor Yellow
    }
    "list" {
        Write-Host "Knowledge documents:" -ForegroundColor Cyan
        Get-ChildItem -Path $env:KNOWLEDGE_DIR -File | ForEach-Object {
            Write-Host "  - $($_.Name) ($([math]::Round($_.Length/1KB, 1)) KB)" -ForegroundColor White
        }
    }
    "search" {
        if (-not $Query) {
            Write-Host "Please specify query: .\manage-knowledge.ps1 search -Query 'your question'" -ForegroundColor Red
            exit 1
        }
        Write-Host "Searching: $Query" -ForegroundColor Cyan
        $body = @{query=$Query; top_k=3} | ConvertTo-Json
        $result = Invoke-RestMethod -Uri "http://localhost:8091/api/v1/rag/search" -Method POST -ContentType "application/json" -Body $body
        Write-Host "`nResults:" -ForegroundColor Yellow
        $result.results | ForEach-Object {
            Write-Host "  [$([math]::Round($_.score, 2))] $($_.source)" -ForegroundColor Green
            Write-Host "    $($_.content.Substring(0, [Math]::Min(100, $_.content.Length)))..." -ForegroundColor Gray
        }
    }
    default {
        Write-Host "Usage: .\manage-knowledge.ps1 [rebuild|add|list|search] [-Query 'question'] [-File 'path']" -ForegroundColor Yellow
    }
}
