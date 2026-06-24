[CmdletBinding()]
param(
    [string]$AdapterName,
    [switch]$ListAdapters,
    [string]$OutputPath,
    [switch]$NoClipboard,
    [switch]$JsonOnly
)

$sourceScript = Join-Path $PSScriptRoot '..\src\PowerBridgeCompanion.ps1'
pwsh -NoProfile -ExecutionPolicy Bypass -File $sourceScript @PSBoundParameters
