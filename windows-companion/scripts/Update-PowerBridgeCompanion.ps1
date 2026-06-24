[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot '..\src\PowerBridgeCompanion.ps1')

Invoke-PowerBridgeCompanionSelfUpdate
