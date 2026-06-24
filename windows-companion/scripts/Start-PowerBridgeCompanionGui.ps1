[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$guiScript = Join-Path $PSScriptRoot '..\src\PowerBridgeCompanion.Gui.ps1'
& $guiScript
