param(
    [string]$DbUrl,
    [string]$DbUsername,
    [string]$DbPassword
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot ".env"

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) {
            return
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            return
        }

        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if ($key -ne "" -and [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key, "Process"))) {
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

if (-not [string]::IsNullOrWhiteSpace($DbUrl)) {
    $env:DB_URL = $DbUrl
}
if (-not [string]::IsNullOrWhiteSpace($DbUsername)) {
    $env:DB_USERNAME = $DbUsername
}
if (-not [string]::IsNullOrWhiteSpace($DbPassword)) {
    $env:DB_PASSWORD = $DbPassword
}

$missing = @("DB_URL", "DB_USERNAME", "DB_PASSWORD") | Where-Object {
    [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, "Process"))
}
if ($missing.Count -gt 0) {
    throw "Missing required SQL Server environment variable(s): $($missing -join ', '). Copy .env.example to .env and fill in local values, or pass -DbUrl, -DbUsername, and -DbPassword."
}

$env:SPRING_PROFILES_ACTIVE = "sqlserver"

& "$PSScriptRoot\mvnw-local.ps1" spring-boot:run -f "$projectRoot\pom.xml"
if ($LASTEXITCODE -ne 0) {
    throw "Application failed with exit code $LASTEXITCODE"
}
