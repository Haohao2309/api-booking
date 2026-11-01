param([switch]$SkipDatabase)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    if (-not (Test-Path .env)) { throw 'Copy .env.example to .env and set DB_PASSWORD before running.' }
    foreach ($line in Get-Content .env) {
        if ($line -match '^\s*([A-Z][A-Z0-9_]*)=(.*)$') {
            $key = $matches[1]
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value, 'Process')
        }
    }
    if (-not $env:DB_PASSWORD -or $env:DB_PASSWORD -eq 'replace-with-local-password') {
        throw 'Set a non-placeholder DB_PASSWORD in .env.'
    }
    if (-not $SkipDatabase) {
        docker compose up -d --wait postgres
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed. Start Docker Desktop or use -SkipDatabase with your existing database.' }
    }
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Backend exited with an error.' }
} finally { Pop-Location }
