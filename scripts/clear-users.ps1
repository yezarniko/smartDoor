param([switch]$Force)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$sqlPath = Join-Path $PSScriptRoot "clear-users.sql"

if (-not $Force) {
    $answer = Read-Host "This permanently deletes every user, QR credential, schedule, permission, and user access event. Type CLEAR-USERS to continue"
    if ($answer -cne "CLEAR-USERS") {
        Write-Host "Cancelled. No database records were changed."
        exit 0
    }
}

Push-Location $projectRoot
try {
    Get-Content -Raw $sqlPath | docker compose exec -T mariadb sh -c 'mariadb -u root -p"$MARIADB_ROOT_PASSWORD" "$MARIADB_DATABASE"'
    if ($LASTEXITCODE -ne 0) { throw "MariaDB cleanup failed with exit code $LASTEXITCODE" }
    Write-Host "All user-related records were deleted. The next generated user code will be 1."
}
finally {
    Pop-Location
}
