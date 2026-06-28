$ErrorActionPreference = "SilentlyContinue"

$ports = 8080, 5173
$processIds = Get-NetTCPConnection -LocalPort $ports -State Listen |
    Select-Object -ExpandProperty OwningProcess -Unique

foreach ($processId in $processIds) {
    Stop-Process -Id $processId
}

if ($processIds) {
    Write-Host "Stopped local dev processes on ports 8080 and 5173."
} else {
    Write-Host "No local dev processes found on ports 8080 or 5173."
}
