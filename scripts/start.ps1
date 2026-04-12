# Run from repo root: .\scripts\start.ps1
# Loads .env.local if present (see .env.local.example). Or set env vars yourself.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path (Join-Path $root "pom.xml"))) {
    Write-Error "Run this script from wxcloudrun-gomoku (pom.xml not found)."
}
Set-Location $root

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jdk = "C:\Program Files\Java\jdk1.8.0_45"
    if (Test-Path "$jdk\bin\java.exe") {
        $env:JAVA_HOME = $jdk
    }
}
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    Write-Error "Set JAVA_HOME to a JDK 8+ install (e.g. C:\Program Files\Java\jdk1.8.0_45)."
}

$envFile = Join-Path $root ".env.local"
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0) { return }
        if ($line.StartsWith("#")) { return }
        $eq = $line.IndexOf("=")
        if ($eq -lt 1) { return }
        $k = $line.Substring(0, $eq).Trim()
        $v = $line.Substring($eq + 1).Trim()
        [Environment]::SetEnvironmentVariable($k, $v, "Process")
    }
}

if (-not $env:SERVER_PORT) {
    $env:SERVER_PORT = "8080"
}

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvn) {
    Write-Host "mvn: $($mvn.Source) JAVA_HOME=$env:JAVA_HOME SERVER_PORT=$env:SERVER_PORT"
    & mvn -q spring-boot:run
    exit $LASTEXITCODE
}

$mvnw = Join-Path $root "mvnw.cmd"
if (Test-Path $mvnw) {
    Write-Host "mvnw.cmd JAVA_HOME=$env:JAVA_HOME SERVER_PORT=$env:SERVER_PORT"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    & cmd /c "`"$mvnw`" -q spring-boot:run"
    exit $LASTEXITCODE
}

Write-Error "Neither 'mvn' nor mvnw.cmd worked. Install Apache Maven or fix Maven Wrapper (.mvn/wrapper)."
