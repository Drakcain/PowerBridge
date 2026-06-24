[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot '..\src\PowerBridgeCompanion.ps1')

function Assert-Equal {
    param(
        $Actual,
        $Expected,
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message Expected '$Expected' but got '$Actual'."
    }
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

Assert-Equal (Normalize-MacAddress 'aabbccddeeff') 'AA:BB:CC:DD:EE:FF' 'MAC normalization failed.'
Assert-Equal (Normalize-MacAddress 'aa-bb-cc-dd-ee-ff') 'AA:BB:CC:DD:EE:FF' 'MAC normalization with separators failed.'
Assert-Equal (Get-BroadcastIp -IpAddress '192.168.1.60' -PrefixLength 24) '192.168.1.255' 'Broadcast /24 failed.'
Assert-Equal (Get-BroadcastIp -IpAddress '10.0.5.12' -PrefixLength 16) '10.0.255.255' 'Broadcast /16 failed.'

$mockAdapter = [pscustomobject]@{
    InterfaceAlias = 'Ethernet'
    AdapterType = 'ethernet'
    IPv4Address = '192.168.1.60'
    PrefixLength = 24
    DefaultGateway = '192.168.1.1'
    MacAddress = 'aa-bb-cc-dd-ee-ff'
}

$payload = Get-PowerBridgeSetupPayload -SelectedAdapter $mockAdapter
$json = $payload | ConvertTo-Json -Depth 4
$data = $json | ConvertFrom-Json
$qrBytes = New-SetupQrBytes -Json $json

Assert-Equal $data.schema 'powerbridge.local_setup.v1' 'Schema field mismatch.'
Assert-Equal $data.targetMac 'AA:BB:CC:DD:EE:FF' 'Payload MAC mismatch.'
Assert-Equal $data.broadcastIp '192.168.1.255' 'Payload broadcast mismatch.'
Assert-Equal $data.source 'windows_companion' 'Payload source mismatch.'
Assert-True ([bool]$data.createdAt) 'createdAt was not generated.'
Assert-True ($qrBytes.Length -gt 0) 'QR bytes were not generated.'

$jsonLower = $json.ToLowerInvariant()
foreach ($forbidden in @('username','password','relaytoken','token','apikey','secret','credential','privatekey','sshkey','winrmpassword','rdppassword')) {
    Assert-True (-not $jsonLower.Contains('"' + $forbidden + '"')) "Forbidden field '$forbidden' was present."
}

Write-Host 'PowerBridge Companion tests passed.' -ForegroundColor Green
