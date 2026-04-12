# 以管理员身份运行：右键 PowerShell -> 以管理员身份运行，然后执行：
#   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
#   & 'f:\work\wxcloudrun-gomoku\scripts\remove-oracle-javapath-admin.ps1'

$ErrorActionPreference = 'Stop'

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Error 'Please run this script as Administrator (right-click PowerShell -> Run as administrator).'
    exit 1
}

$oracle = 'C:\ProgramData\Oracle\Java\javapath'
$mp = [Environment]::GetEnvironmentVariable('Path', 'Machine')
if ([string]::IsNullOrEmpty($mp)) {
    Write-Host 'Machine PATH is empty; nothing to fix.'
    exit 0
}

$parts = $mp -split ';' | Where-Object { $_ -ne '' -and $_.Trim() -ne $oracle }
$newMp = $parts -join ';'
[Environment]::SetEnvironmentVariable('Path', $newMp, 'Machine')

Write-Host 'Removed from System PATH:' $oracle
Write-Host 'Close ALL cmd/PowerShell/IDE windows, open a new cmd, then run: java -version'
