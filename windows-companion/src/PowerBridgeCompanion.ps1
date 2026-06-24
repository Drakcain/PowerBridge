[CmdletBinding()]
param(
    [string]$AdapterName,
    [switch]$ListAdapters,
    [string]$OutputPath,
    [switch]$NoClipboard,
    [switch]$JsonOnly,
    [switch]$OpenQr
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Normalize-MacAddress {
    param([string]$MacAddress)

    $clean = ($MacAddress -replace '[^0-9A-Fa-f]', '').ToUpperInvariant()
    if ([string]::IsNullOrWhiteSpace($clean) -or $clean.Length -ne 12) {
        throw "Invalid MAC address: $MacAddress"
    }

    return (($clean -split '(.{2})' | Where-Object { $_ }) -join ':')
}

function ConvertTo-UInt32FromIp {
    param([string]$IpAddress)

    $bytes = ([System.Net.IPAddress]::Parse($IpAddress)).GetAddressBytes()
    [Array]::Reverse($bytes)
    return [BitConverter]::ToUInt32($bytes, 0)
}

function ConvertTo-IpFromUInt32 {
    param([uint32]$Value)

    $bytes = [BitConverter]::GetBytes($Value)
    [Array]::Reverse($bytes)
    return ([System.Net.IPAddress]::new($bytes)).ToString()
}

function Get-BroadcastIp {
    param(
        [string]$IpAddress,
        [int]$PrefixLength
    )

    if ($PrefixLength -lt 0 -or $PrefixLength -gt 32) {
        throw "Invalid prefix length: $PrefixLength"
    }

    $ipValue = ConvertTo-UInt32FromIp -IpAddress $IpAddress
    $mask = if ($PrefixLength -eq 0) {
        [uint32]0
    } else {
        [uint32]::MaxValue -shl (32 - $PrefixLength)
    }
    $network = $ipValue -band $mask
    $broadcast = $network -bor (-bnot $mask)
    return ConvertTo-IpFromUInt32 -Value $broadcast
}

function Get-AdapterTypeLabel {
    param($NetAdapter)

    $name = "$($NetAdapter.InterfaceDescription) $($NetAdapter.Name)".ToLowerInvariant()
    if ($name -match 'wi-?fi|wireless|802\.11') { return 'wifi' }
    if ($name -match 'ethernet') { return 'ethernet' }
    if ($name -match 'vpn|wireguard|tailscale|zerotier|tap') { return 'vpn' }
    if ($name -match 'virtual|vmware|hyper-v|virtualbox|loopback') { return 'virtual' }
    return 'other'
}

function Test-IsVirtualAdapter {
    param($NetAdapter)

    $name = "$($NetAdapter.InterfaceDescription) $($NetAdapter.Name)".ToLowerInvariant()
    return $name -match 'virtual|vmware|hyper-v|virtualbox|loopback|vpn|wireguard|tailscale|zerotier|tap|npcap'
}

function Select-PrimaryIpv4Address {
    param($Config)

    $empty = [pscustomobject]@{
        IPAddress    = $null
        PrefixLength = $null
    }

    if (-not $Config) {
        return $empty
    }

    $ipv4Addresses = @($Config.IPv4Address)
    if (-not $ipv4Addresses) {
        return $empty
    }

    $preferred = $ipv4Addresses |
        Where-Object { $_.IPAddress -and $_.IPAddress -notlike '169.254.*' } |
        Select-Object -First 1
    $selected = if ($preferred) { $preferred } else { $ipv4Addresses | Select-Object -First 1 }

    if (-not $selected) {
        return $empty
    }

    [pscustomobject]@{
        IPAddress    = $selected.IPAddress
        PrefixLength = $selected.PrefixLength
    }
}

function Get-AdapterCandidates {
    $configsByIndex = @{}
    foreach ($config in @(Get-NetIPConfiguration)) {
        $configsByIndex["$($config.InterfaceIndex)"] = $config
    }

    $ipInterfacesByIndex = @{}
    foreach ($ipInterface in @(Get-NetIPInterface -AddressFamily IPv4 -ErrorAction SilentlyContinue)) {
        $ipInterfacesByIndex["$($ipInterface.InterfaceIndex)"] = $ipInterface
    }

    $defaultRoutesByIndex = @{}
    foreach ($route in @(Get-NetRoute -AddressFamily IPv4 -DestinationPrefix '0.0.0.0/0' -ErrorAction SilentlyContinue)) {
        $routeKey = "$($route.InterfaceIndex)"
        if (-not $defaultRoutesByIndex.ContainsKey($routeKey)) {
            $defaultRoutesByIndex[$routeKey] = @()
        }
        $defaultRoutesByIndex[$routeKey] += $route
    }

    foreach ($adapter in @(Get-NetAdapter -ErrorAction SilentlyContinue)) {
        $adapterKey = "$($adapter.InterfaceIndex)"
        $config = $configsByIndex[$adapterKey]
        $ipInterface = $ipInterfacesByIndex[$adapterKey]
        $defaultRoutes = @($defaultRoutesByIndex[$adapterKey])

        $selectedIpv4 = Select-PrimaryIpv4Address -Config $config
        $bestDefaultRoute = $defaultRoutes |
            Sort-Object -Property @{ Expression = 'RouteMetric'; Descending = $false }, @{ Expression = 'InterfaceMetric'; Descending = $false } |
            Select-Object -First 1
        $gatewayEntry = if ($config) { $config.IPv4DefaultGateway | Select-Object -First 1 } else { $bestDefaultRoute }
        $defaultGateway = if ($bestDefaultRoute) { $bestDefaultRoute.NextHop } elseif ($gatewayEntry) { $gatewayEntry.NextHop } else { $null }

        [pscustomobject]@{
            InterfaceIndex        = $adapter.InterfaceIndex
            InterfaceAlias        = $adapter.Name
            InterfaceDescription  = $adapter.InterfaceDescription
            Status                = $adapter.Status
            MacAddress            = $adapter.MacAddress
            HardwareInterface     = $adapter.HardwareInterface
            AdapterType           = Get-AdapterTypeLabel -NetAdapter $adapter
            IsVirtual             = Test-IsVirtualAdapter -NetAdapter $adapter
            IPv4Address           = $selectedIpv4.IPAddress
            PrefixLength          = $selectedIpv4.PrefixLength
            DefaultGateway        = $defaultGateway
            HasGateway            = [bool]$gatewayEntry
            HasDefaultRoute       = $defaultRoutes.Count -gt 0
            RouteMetric           = if ($bestDefaultRoute) { $bestDefaultRoute.RouteMetric } else { [int]::MaxValue }
            EffectiveMetric       = if ($bestDefaultRoute) { $bestDefaultRoute.RouteMetric + $bestDefaultRoute.InterfaceMetric } else { [int]::MaxValue }
            IsApipa               = [bool]($selectedIpv4.IPAddress -like '169.254.*')
            InterfaceMetric       = if ($ipInterface) { $ipInterface.InterfaceMetric } else { [int]::MaxValue }
            LinkSpeed             = $adapter.LinkSpeed
        }
    }
}

function Get-AdapterScore {
    param($Candidate)

    $score = 0
    if ($Candidate.Status -eq 'Up') { $score += 50 }
    if ($Candidate.IPv4Address) { $score += 40 }
    if ($Candidate.HasGateway) { $score += 50 }
    if ($Candidate.HasDefaultRoute) { $score += 80 }
    if (-not $Candidate.IsApipa) { $score += 20 }
    if ($Candidate.HardwareInterface) { $score += 20 }
    if ($Candidate.InterfaceMetric -lt 100) { $score += 10 }
    if ($Candidate.EffectiveMetric -lt 100) { $score += 20 }
    elseif ($Candidate.EffectiveMetric -lt 250) { $score += 10 }
    switch ($Candidate.AdapterType) {
        'ethernet' { $score += 30 }
        'wifi'     { $score += 25 }
        'other'    { $score += 5 }
        'vpn'      { $score -= 20 }
        'virtual'  { $score -= 30 }
    }
    if ($Candidate.IsVirtual) { $score -= 15 }
    return $score
}

function Get-ScoredAdapterCandidates {
    $candidates = @(Get-AdapterCandidates)
    if (-not $candidates) {
        return @()
    }

    return $candidates |
        ForEach-Object {
            $_ | Add-Member -NotePropertyName Score -NotePropertyValue (Get-AdapterScore -Candidate $_) -PassThru
        } |
        Sort-Object -Property @{ Expression = 'Score'; Descending = $true }, @{ Expression = 'HasGateway'; Descending = $true }, @{ Expression = 'InterfaceMetric'; Descending = $false }
}

function Select-BestAdapter {
    param(
        [Parameter(Mandatory)]
        [array]$Candidates,
        [string]$PreferredAdapterName
    )

    if ($PreferredAdapterName) {
        $match = $Candidates | Where-Object {
            $_.InterfaceAlias -eq $PreferredAdapterName -or $_.InterfaceDescription -eq $PreferredAdapterName
        } | Select-Object -First 1
        if (-not $match) {
            throw "Requested adapter '$PreferredAdapterName' was not found."
        }
        return $match
    }

    $selected = $Candidates |
        Sort-Object -Property @{ Expression = 'Score'; Descending = $true }, @{ Expression = 'HasGateway'; Descending = $true }, @{ Expression = 'InterfaceMetric'; Descending = $false } |
        Select-Object -First 1

    if (-not $selected) {
        throw 'No adapter candidates were available.'
    }

    return $selected
}

function Get-PowerBridgeSetupPayload {
    param($SelectedAdapter)

    if (-not $SelectedAdapter.IPv4Address) {
        throw 'Selected adapter does not have an IPv4 address.'
    }
    if (-not $SelectedAdapter.MacAddress) {
        throw 'Selected adapter does not have a MAC address.'
    }

    $payload = [ordered]@{
        schema       = 'powerbridge.local_setup.v1'
        pcName       = $env:COMPUTERNAME
        targetMac    = Normalize-MacAddress -MacAddress $SelectedAdapter.MacAddress
        targetIp     = $SelectedAdapter.IPv4Address
        gateway      = $SelectedAdapter.DefaultGateway
        subnetPrefix = [int]$SelectedAdapter.PrefixLength
        broadcastIp  = Get-BroadcastIp -IpAddress $SelectedAdapter.IPv4Address -PrefixLength $SelectedAdapter.PrefixLength
        adapterName  = $SelectedAdapter.InterfaceAlias
        adapterType  = $SelectedAdapter.AdapterType
        source       = 'windows_companion'
        createdAt    = ([DateTimeOffset]::UtcNow).ToString('o')
    }

    return [pscustomobject]$payload
}

function ConvertTo-CompactSetupJson {
    param($Payload)

    return ($Payload | ConvertTo-Json -Depth 4 -Compress)
}

function Get-DefaultOutputPath {
    $root = Split-Path -Parent $PSScriptRoot
    $outputDir = Join-Path $root 'output'
    if (-not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
    }
    return Join-Path $outputDir ("powerbridge-setup-{0}-{1}.json" -f (Get-Date -Format 'yyyyMMdd-HHmmssfff'), [guid]::NewGuid().ToString('N').Substring(0, 6))
}

function Get-PowerBridgeAppRoot {
    $candidates = @(
        (Split-Path -Parent $PSScriptRoot),
        (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
    )
    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (Test-Path (Join-Path $candidate 'VERSION')) {
            return $candidate
        }
    }
    return (Split-Path -Parent $PSScriptRoot)
}

function Get-PowerBridgeInstalledVersion {
    $versionPath = Join-Path (Get-PowerBridgeAppRoot) 'VERSION'
    if (Test-Path -LiteralPath $versionPath) {
        return (Get-Content -LiteralPath $versionPath -Raw).Trim()
    }
    return '0.0.0'
}

function Get-NormalizedSemanticVersion {
    param([string]$Value)

    try {
        return [version]($Value.Trim().TrimStart('v'))
    } catch {
        return [version]'0.0.0'
    }
}

function Get-LatestPowerBridgeRelease {
    $headers = @{
        Accept = 'application/vnd.github+json'
        'User-Agent' = 'PowerBridge-Companion-Updater'
    }
    return Invoke-RestMethod -Uri 'https://api.github.com/repos/Drakcain/PowerBridge/releases/latest' -Headers $headers
}

function Open-PowerBridgeReleasesPage {
    Start-Process -FilePath 'https://github.com/Drakcain/PowerBridge/releases'
}

function Invoke-PowerBridgeCompanionSelfUpdate {
    Add-Type -AssemblyName PresentationFramework

    $currentVersion = Get-PowerBridgeInstalledVersion
    $release = Get-LatestPowerBridgeRelease
    $latestTag = [string]$release.tag_name
    $latestVersion = $latestTag.TrimStart('v')

    if ((Get-NormalizedSemanticVersion $latestVersion) -le (Get-NormalizedSemanticVersion $currentVersion)) {
        [System.Windows.MessageBox]::Show(
            "PowerBridge Windows Companion is already up to date.`n`nCurrent version: v$currentVersion",
            'PowerBridge Windows Companion',
            'OK',
            'Information'
        ) | Out-Null
        return
    }

    $installer = $release.assets |
        Where-Object { $_.name -like 'PowerBridge-Companion-Setup-*.exe' } |
        Select-Object -First 1
    $checksum = $release.assets |
        Where-Object { $_.name -like 'PowerBridge-Companion-Setup-*.exe.sha256' } |
        Select-Object -First 1
    if (-not $installer) {
        throw 'Latest PowerBridge release does not include a Windows Companion installer asset.'
    }

    $prompt = [System.Windows.MessageBox]::Show(
        "Installed: v$currentVersion`nLatest: $latestTag`n`nDownload and launch the new Windows Companion installer now?",
        'PowerBridge update available',
        'YesNo',
        'Information'
    )
    if ($prompt -ne 'Yes') {
        return
    }

    $tempDir = Join-Path $env:TEMP ("powerbridge-companion-update-{0}" -f [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
    $installerPath = Join-Path $tempDir $installer.name
    Invoke-WebRequest -Uri $installer.browser_download_url -OutFile $installerPath -Headers @{ 'User-Agent' = 'PowerBridge-Companion-Updater' }

    if ($checksum) {
        $checksumPath = Join-Path $tempDir $checksum.name
        Invoke-WebRequest -Uri $checksum.browser_download_url -OutFile $checksumPath -Headers @{ 'User-Agent' = 'PowerBridge-Companion-Updater' }
        $checksumText = (Get-Content -LiteralPath $checksumPath -Raw).Trim()
        $expectedHash = ($checksumText -split '\s+')[0].Trim().ToUpperInvariant()
        $actualHash = (Get-FileHash -LiteralPath $installerPath -Algorithm SHA256).Hash.ToUpperInvariant()
        if ($expectedHash -ne $actualHash) {
            throw "Installer checksum mismatch. Expected $expectedHash, got $actualHash."
        }
    }

    Start-Process -FilePath $installerPath -Verb RunAs
    [System.Windows.MessageBox]::Show(
        "Downloaded and launched PowerBridge Windows Companion $latestTag.`n`nFollow the installer prompts to finish the update.",
        'PowerBridge Windows Companion',
        'OK',
        'Information'
    ) | Out-Null
}

function Write-SetupJsonFile {
    param(
        [string]$Json,
        [string]$Path
    )

    $targetPath = if ($Path) { $Path } else { Get-DefaultOutputPath }
    if ($Path -and (Test-Path $Path) -and (Get-Item -LiteralPath $Path).PSIsContainer) {
        $targetPath = Join-Path $Path ("powerbridge-setup-{0}-{1}.json" -f (Get-Date -Format 'yyyyMMdd-HHmmssfff'), [guid]::NewGuid().ToString('N').Substring(0, 6))
    }
    $parent = Split-Path -Parent $targetPath
    if ($parent -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    Set-Content -Path $targetPath -Value $Json -Encoding UTF8
    return $targetPath
}

function Get-QrCoderAssemblyPath {
    $root = Split-Path -Parent $PSScriptRoot
    $assemblyPath = Join-Path $root 'lib\QRCoder.dll'
    if (-not (Test-Path -LiteralPath $assemblyPath)) {
        throw "QR dependency was not found: $assemblyPath"
    }
    return $assemblyPath
}

function New-SetupQrBytes {
    param([string]$Json)

    if (-not ('QRCoder.QRCodeGenerator' -as [type])) {
        Add-Type -Path (Get-QrCoderAssemblyPath)
    }

    $generator = [QRCoder.QRCodeGenerator]::new()
    $qrData = $null
    try {
        $qrData = $generator.CreateQrCode($Json, [QRCoder.QRCodeGenerator+ECCLevel]::M)
        $renderer = [QRCoder.PngByteQRCode]::new($qrData)
        return $renderer.GetGraphic(
            16,
            [byte[]](0x00, 0x00, 0x00),
            [byte[]](0xFF, 0xFF, 0xFF),
            $true
        )
    } finally {
        if ($qrData) {
            $qrData.Dispose()
        }
        $generator.Dispose()
    }
}

function Write-SetupQrFile {
    param(
        [byte[]]$Bytes,
        [string]$JsonPath
    )

    $qrPath = [System.IO.Path]::ChangeExtension($JsonPath, '.png')
    [System.IO.File]::WriteAllBytes($qrPath, $Bytes)
    return $qrPath
}

function Show-AndroidImportInstructions {
    param(
        [string]$OutputFile,
        [string]$QrFile,
        [bool]$CopiedToClipboard
    )

    Write-Host ''
    Write-Host 'Next steps for Android:' -ForegroundColor Cyan
    Write-Host '1. Open PowerBridge on Android.'
    Write-Host '2. Go to Settings -> PC Profiles.'
    Write-Host '3. Tap Scan Setup QR and scan the generated QR code.'
    Write-Host '4. Review the scanned setup, then create or update the Local Wi-Fi Wake profile.'
    Write-Host ''
    Write-Host 'Honest limitations:' -ForegroundColor Yellow
    Write-Host '- This script prepares setup data only.'
    Write-Host '- Wake still depends on Wake-on-LAN support and a valid network path.'
    Write-Host '- Off-site or cellular wake still requires a home anchor, smart plug path, or a user-owned relay.'
    Write-Host '- No Windows credentials, relay tokens, or secrets are collected.'
    Write-Host ''
    Write-Host "JSON file: $OutputFile"
    Write-Host "QR file: $QrFile"
    Write-Host ("Copied to clipboard: {0}" -f ($(if ($CopiedToClipboard) { 'yes' } else { 'no' })))
}

function Copy-SetupJsonToClipboard {
    param([string]$Json)

    Set-Clipboard -Value $Json
}

function Get-CompanionWarnings {
    param($SelectedAdapter)

    $warnings = New-Object System.Collections.Generic.List[string]
    if (-not $SelectedAdapter.HasGateway) {
        $warnings.Add('Selected adapter has no default gateway. Remote routing assumptions may be incomplete.')
    }
    if ($SelectedAdapter.IsVirtual) {
        $warnings.Add('Selected adapter appears virtual or VPN-based. Verify it is the adapter you want Android to use.')
    }
    if (-not $SelectedAdapter.HardwareInterface) {
        $warnings.Add('Selected adapter does not report as a hardware interface.')
    }
    if (-not $SelectedAdapter.MacAddress) {
        $warnings.Add('Selected adapter does not report a MAC address.')
    }
    if (-not $SelectedAdapter.IPv4Address) {
        $warnings.Add('Selected adapter does not report an IPv4 address.')
    }
    if (-not $SelectedAdapter.HasDefaultRoute) {
        $warnings.Add('Selected adapter is not currently carrying the default internet route.')
    }
    if ($SelectedAdapter.AdapterType -eq 'wifi') {
        $warnings.Add('Wi-Fi wake may not work on all laptops. Ethernet is recommended for the most reliable Wake-on-LAN support.')
    }
    return $warnings
}

function New-PowerBridgeSetupArtifacts {
    param(
        [Parameter(Mandatory)]
        $SelectedAdapter,
        [string]$OutputPath,
        [switch]$NoClipboard,
        [switch]$InMemoryOnly
    )

    $payload = Get-PowerBridgeSetupPayload -SelectedAdapter $SelectedAdapter
    $json = ConvertTo-CompactSetupJson -Payload $payload
    $qrBytes = New-SetupQrBytes -Json $json
    $jsonPath = $null
    $qrPath = $null

    if (-not $InMemoryOnly) {
        $jsonPath = Write-SetupJsonFile -Json $json -Path $OutputPath
        $qrPath = Write-SetupQrFile -Bytes $qrBytes -JsonPath $jsonPath
    }

    $copied = $false
    if (-not $NoClipboard) {
        try {
            Copy-SetupJsonToClipboard -Json $json
            $copied = $true
        } catch {
            Write-Warning "Clipboard copy failed: $($_.Exception.Message)"
        }
    }

    [pscustomobject]@{
        Payload           = $payload
        Json              = $json
        JsonPath          = $jsonPath
        QrBytes           = $qrBytes
        QrPath            = $qrPath
        CopiedToClipboard = $copied
        Warnings          = @(Get-CompanionWarnings -SelectedAdapter $SelectedAdapter)
        SelectedAdapter   = $SelectedAdapter
    }
}

function Invoke-PowerBridgeCompanion {
    param(
        [string]$AdapterName,
        [switch]$ListAdapters,
        [string]$OutputPath,
        [switch]$NoClipboard,
        [switch]$JsonOnly,
        [switch]$OpenQr
    )

    $candidates = @(Get-ScoredAdapterCandidates)
    if (-not $candidates) {
        throw 'No network adapters were found.'
    }

    if ($ListAdapters) {
        $candidates | Format-Table InterfaceAlias, AdapterType, Status, IPv4Address, DefaultGateway, IsVirtual, Score -AutoSize
        return
    }

    $selected = Select-BestAdapter -Candidates $candidates -PreferredAdapterName $AdapterName
    $artifacts = New-PowerBridgeSetupArtifacts -SelectedAdapter $selected -OutputPath $OutputPath -NoClipboard:$NoClipboard
    $payload = $artifacts.Payload

    if ($JsonOnly) {
        $artifacts.Json
        return
    }

    Write-Host 'PowerBridge Windows Companion Prototype' -ForegroundColor Cyan
    Write-Host "PC name: $($payload.pcName)"
    Write-Host "Adapter: $($payload.adapterName)"
    Write-Host "Adapter type: $($payload.adapterType)"
    Write-Host "MAC: $($payload.targetMac)"
    Write-Host "IPv4: $($payload.targetIp)"
    Write-Host "Gateway: $($payload.gateway)"
    Write-Host "Prefix: /$($payload.subnetPrefix)"
    Write-Host "Broadcast: $($payload.broadcastIp)"

    foreach ($warning in $artifacts.Warnings) {
        Write-Warning $warning
    }

    if ($OpenQr) {
        Start-Process -FilePath $artifacts.QrPath
    }

    Show-AndroidImportInstructions -OutputFile $artifacts.JsonPath -QrFile $artifacts.QrPath -CopiedToClipboard:$artifacts.CopiedToClipboard
}

if ($MyInvocation.InvocationName -ne '.') {
    Invoke-PowerBridgeCompanion @PSBoundParameters
}
