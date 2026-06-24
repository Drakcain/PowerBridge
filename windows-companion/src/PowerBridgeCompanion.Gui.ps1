[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'PowerBridgeCompanion.ps1')

Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName WindowsBase

[xml]$xaml = @'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Title="PowerBridge Windows Companion"
        Width="1220"
        Height="1048"
        WindowStartupLocation="CenterScreen"
        ResizeMode="NoResize"
        Background="#070A0F"
        Foreground="#F8FAFC"
        FontFamily="Segoe UI">
    <Window.Resources>
        <SolidColorBrush x:Key="BgBrush" Color="#070A0F" />
        <SolidColorBrush x:Key="SurfaceBrush" Color="#111827" />
        <SolidColorBrush x:Key="SurfaceRaisedBrush" Color="#151B26" />
        <SolidColorBrush x:Key="BorderBrushTheme" Color="#263244" />
        <SolidColorBrush x:Key="TextPrimaryBrush" Color="#F8FAFC" />
        <SolidColorBrush x:Key="TextSecondaryBrush" Color="#CBD5E1" />
        <SolidColorBrush x:Key="TextMutedBrush" Color="#94A3B8" />
        <SolidColorBrush x:Key="YellowBrush" Color="#FCEE09" />
        <SolidColorBrush x:Key="CyanBrush" Color="#00E5FF" />
        <SolidColorBrush x:Key="MagentaBrush" Color="#FF2A6D" />
        <SolidColorBrush x:Key="SuccessBrush" Color="#00D26A" />
        <SolidColorBrush x:Key="ErrorBrush" Color="#FF1744" />
        <SolidColorBrush x:Key="DividerBrush" Color="#F8FAFC" />

        <Style x:Key="CardBorderStyle" TargetType="Border">
            <Setter Property="Background" Value="{StaticResource SurfaceBrush}" />
            <Setter Property="BorderBrush" Value="{StaticResource BorderBrushTheme}" />
            <Setter Property="BorderThickness" Value="1" />
            <Setter Property="CornerRadius" Value="22" />
            <Setter Property="Padding" Value="18" />
            <Setter Property="Margin" Value="0,0,0,16" />
        </Style>

        <Style x:Key="AccentButtonStyle" TargetType="Button">
            <Setter Property="Foreground" Value="#05070B" />
            <Setter Property="Background" Value="{StaticResource YellowBrush}" />
            <Setter Property="BorderBrush" Value="{StaticResource YellowBrush}" />
            <Setter Property="BorderThickness" Value="1" />
            <Setter Property="Padding" Value="14,10" />
            <Setter Property="Margin" Value="0,0,10,10" />
            <Setter Property="FontWeight" Value="SemiBold" />
            <Setter Property="Cursor" Value="Hand" />
        </Style>

        <Style x:Key="SecondaryButtonStyle" TargetType="Button" BasedOn="{StaticResource AccentButtonStyle}">
            <Setter Property="Foreground" Value="{StaticResource TextPrimaryBrush}" />
            <Setter Property="Background" Value="{StaticResource SurfaceRaisedBrush}" />
            <Setter Property="BorderBrush" Value="{StaticResource CyanBrush}" />
        </Style>

        <Style x:Key="FieldLabelStyle" TargetType="TextBlock">
            <Setter Property="Foreground" Value="{StaticResource TextMutedBrush}" />
            <Setter Property="FontSize" Value="12" />
            <Setter Property="Margin" Value="0,0,0,6" />
        </Style>

        <Style x:Key="FieldBoxStyle" TargetType="TextBox">
            <Setter Property="Background" Value="{StaticResource SurfaceRaisedBrush}" />
            <Setter Property="Foreground" Value="{StaticResource TextPrimaryBrush}" />
            <Setter Property="BorderBrush" Value="{StaticResource BorderBrushTheme}" />
            <Setter Property="BorderThickness" Value="1" />
            <Setter Property="Padding" Value="10,8" />
            <Setter Property="Margin" Value="0,0,0,12" />
            <Setter Property="IsReadOnly" Value="True" />
        </Style>

        <Style x:Key="ComboStyle" TargetType="ComboBox">
            <Setter Property="Background" Value="{StaticResource SurfaceRaisedBrush}" />
            <Setter Property="Foreground" Value="#000000" />
            <Setter Property="BorderBrush" Value="{StaticResource CyanBrush}" />
            <Setter Property="BorderThickness" Value="1" />
            <Setter Property="Padding" Value="8,6" />
            <Setter Property="Margin" Value="0,0,0,12" />
            <Setter Property="IsEditable" Value="False" />
            <Setter Property="FontWeight" Value="Bold" />
        </Style>

        <Style TargetType="ComboBoxItem">
            <Setter Property="Foreground" Value="#000000" />
            <Setter Property="Background" Value="#FFFFFF" />
            <Setter Property="FontWeight" Value="Bold" />
        </Style>
    </Window.Resources>

    <Grid Background="{StaticResource BgBrush}">
        <Grid.RowDefinitions>
            <RowDefinition Height="Auto" />
            <RowDefinition Height="*" />
        </Grid.RowDefinitions>

        <Border Grid.Row="0" Padding="28,24,28,18" Background="{StaticResource BgBrush}">
            <Grid>
                <Grid.ColumnDefinitions>
                    <ColumnDefinition Width="*" />
                    <ColumnDefinition Width="320" />
                </Grid.ColumnDefinitions>

                <StackPanel>
                    <TextBlock FontSize="34" FontWeight="Bold" Foreground="{StaticResource TextPrimaryBrush}">
                        <Run Text="Power" />
                        <Run Text="Bridge" Foreground="{StaticResource YellowBrush}" />
                        <Run Text=" Windows Companion" />
                    </TextBlock>
                    <TextBlock Margin="0,8,0,0" Foreground="{StaticResource TextSecondaryBrush}" FontSize="15" TextWrapping="Wrap">
                        Select the adapter you want to pair, detect this PC, generate a scan-safe QR, and scan it from PowerBridge on Android to prepare a Local Wi-Fi Wake profile.
                    </TextBlock>
                </StackPanel>

                <Border Grid.Column="1" Background="{StaticResource SurfaceBrush}" BorderBrush="{StaticResource BorderBrushTheme}" BorderThickness="1" CornerRadius="18" Padding="16">
                    <StackPanel>
                        <TextBlock Text="Import Flow" Foreground="{StaticResource YellowBrush}" FontSize="16" FontWeight="Bold" />
                        <TextBlock Margin="0,8,0,0" Foreground="{StaticResource TextSecondaryBrush}" TextWrapping="Wrap">
                            1. Select adapter
                            2. Detect This PC
                            3. Generate QR Code
                            4. On Android, tap Scan Setup QR and scan it
                        </TextBlock>
                    </StackPanel>
                </Border>
            </Grid>
        </Border>

        <Grid Grid.Row="1" Margin="28,0,28,24">
            <Grid.RowDefinitions>
                <RowDefinition Height="495" />
                <RowDefinition Height="16" />
                <RowDefinition Height="330" />
            </Grid.RowDefinitions>
            <Grid.ColumnDefinitions>
                <ColumnDefinition Width="1.08*" />
                <ColumnDefinition Width="16" />
                <ColumnDefinition Width="5" />
                <ColumnDefinition Width="16" />
                <ColumnDefinition Width="0.92*" />
            </Grid.ColumnDefinitions>

            <Border Grid.Row="0" Grid.Column="0" Style="{StaticResource CardBorderStyle}" Margin="0,0,0,0">
                <StackPanel>
                    <TextBlock Text="PC Detection" FontSize="20" FontWeight="Bold" Foreground="{StaticResource YellowBrush}" />
                    <TextBlock Margin="0,8,0,16" Foreground="{StaticResource TextSecondaryBrush}" TextWrapping="Wrap">
                        This setup helper stays local-only. No credentials, relay tokens, cloud calls, or Windows changes are used.
                    </TextBlock>

                    <WrapPanel Margin="0,0,0,8">
                        <Button x:Name="DetectButton" Content="Detect This PC" Style="{StaticResource AccentButtonStyle}" MinWidth="170" />
                        <Button x:Name="RefreshButton" Content="Refresh Adapters" Style="{StaticResource SecondaryButtonStyle}" MinWidth="150" />
                        <Button x:Name="GenerateButton" Content="Generate QR Code" Style="{StaticResource SecondaryButtonStyle}" MinWidth="170" />
                    </WrapPanel>

                        <TextBlock Text="Selected Adapter" Style="{StaticResource FieldLabelStyle}" />
                        <ComboBox x:Name="AdapterComboBox" Style="{StaticResource ComboStyle}" DisplayMemberPath="DisplayLabel" />

                    <Grid Margin="0,4,0,0">
                        <Grid.ColumnDefinitions>
                            <ColumnDefinition Width="*" />
                            <ColumnDefinition Width="24" />
                            <ColumnDefinition Width="*" />
                        </Grid.ColumnDefinitions>
                        <Grid.RowDefinitions>
                            <RowDefinition Height="Auto" />
                            <RowDefinition Height="Auto" />
                            <RowDefinition Height="Auto" />
                            <RowDefinition Height="Auto" />
                        </Grid.RowDefinitions>

                        <StackPanel Margin="0,0,12,0">
                            <TextBlock Text="PC Name" Style="{StaticResource FieldLabelStyle}" />
                            <TextBox x:Name="PcNameTextBox" Style="{StaticResource FieldBoxStyle}" />

                            <TextBlock Text="Adapter Type" Style="{StaticResource FieldLabelStyle}" />
                            <TextBox x:Name="AdapterTypeTextBox" Style="{StaticResource FieldBoxStyle}" />

                            <TextBlock Text="MAC Address" Style="{StaticResource FieldLabelStyle}" />
                            <TextBox x:Name="MacAddressTextBox" Style="{StaticResource FieldBoxStyle}" />
                        </StackPanel>

                        <StackPanel Grid.Column="2" Margin="12,0,0,0">
                            <TextBlock Text="IPv4 Address" Style="{StaticResource FieldLabelStyle}" />
                            <TextBox x:Name="Ipv4TextBox" Style="{StaticResource FieldBoxStyle}" />

                            <TextBlock Text="Gateway" Style="{StaticResource FieldLabelStyle}" />
                            <TextBox x:Name="GatewayTextBox" Style="{StaticResource FieldBoxStyle}" />

                            <TextBlock Text="Subnet Prefix" Style="{StaticResource FieldLabelStyle}" />
                            <TextBox x:Name="PrefixTextBox" Style="{StaticResource FieldBoxStyle}" />

                            <TextBlock Text="Broadcast IP" Style="{StaticResource FieldLabelStyle}" />
                            <TextBox x:Name="BroadcastTextBox" Style="{StaticResource FieldBoxStyle}" />
                        </StackPanel>
                    </Grid>
                </StackPanel>
            </Border>

            <Border Grid.Row="2" Grid.Column="0" Style="{StaticResource CardBorderStyle}" Margin="0,0,0,0">
                <StackPanel>
                    <TextBlock Text="Status And Warnings" FontSize="20" FontWeight="Bold" Foreground="{StaticResource MagentaBrush}" />
                    <TextBlock x:Name="StatusTextBlock" Margin="0,10,0,10" Foreground="{StaticResource TextPrimaryBrush}" FontSize="14" TextWrapping="Wrap" />
                    <TextBox x:Name="WarningsTextBox"
                             Background="{StaticResource SurfaceRaisedBrush}"
                             Foreground="{StaticResource TextSecondaryBrush}"
                             BorderBrush="{StaticResource BorderBrushTheme}"
                             BorderThickness="1"
                             Padding="12"
                             TextWrapping="Wrap"
                             AcceptsReturn="True"
                             IsReadOnly="True"
                             Height="180"
                             VerticalScrollBarVisibility="Auto" />
                </StackPanel>
            </Border>

            <Border Grid.Row="0" Grid.RowSpan="3" Grid.Column="2" Background="{StaticResource DividerBrush}" CornerRadius="2.5" Margin="0,18,0,18" />

            <Border Grid.Row="0" Grid.Column="4" Style="{StaticResource CardBorderStyle}" Margin="0,0,0,0">
                    <StackPanel HorizontalAlignment="Center">
                        <TextBlock Text="Live QR Code" FontSize="20" FontWeight="Bold" Foreground="{StaticResource YellowBrush}" HorizontalAlignment="Left" />
                        <Border Margin="0,16,0,12"
                                Width="370"
                                Height="370"
                                Background="#FFFFFF"
                                BorderBrush="#FFFFFF"
                                BorderThickness="1"
                                CornerRadius="18"
                                Padding="24">
                            <Image x:Name="QrImage" Stretch="Uniform" />
                        </Border>
                        <TextBlock Foreground="{StaticResource TextSecondaryBrush}" TextWrapping="Wrap" TextAlignment="Center" Width="360">
                            This QR uses a plain black-on-white scan-safe render with a large quiet zone. Closing the companion clears it.
                        </TextBlock>
                    </StackPanel>
                </Border>

            <Grid Grid.Row="2" Grid.Column="4">
                <Grid.RowDefinitions>
                    <RowDefinition Height="1*" />
                    <RowDefinition Height="16" />
                    <RowDefinition Height="1*" />
                </Grid.RowDefinitions>
                <Border Grid.Row="0" Style="{StaticResource CardBorderStyle}" Margin="0,0,0,0">
                    <StackPanel>
                        <TextBlock Text="Android Instructions" FontSize="20" FontWeight="Bold" Foreground="{StaticResource CyanBrush}" />
                        <TextBlock Margin="0,10,0,0" Foreground="{StaticResource TextSecondaryBrush}" TextWrapping="Wrap">
                            Open PowerBridge on Android, go to Settings, open PC Profiles, tap Scan Setup QR, and scan the code shown here.
                        </TextBlock>
                        <TextBlock Margin="0,10,0,0" Foreground="{StaticResource TextMutedBrush}" TextWrapping="Wrap">
                            After the scan, review the setup preview, then tap Create New Profile or Update Current Profile to save a Local Wi-Fi Wake setup.
                        </TextBlock>
                    </StackPanel>
                </Border>

                <Border Grid.Row="2" Style="{StaticResource CardBorderStyle}" Margin="0,0,0,0">
                    <StackPanel>
                        <TextBlock Text="Privacy Note" FontSize="20" FontWeight="Bold" Foreground="{StaticResource YellowBrush}" />
                        <TextBlock Margin="0,10,0,0" Foreground="{StaticResource TextSecondaryBrush}" TextWrapping="Wrap">
                            Only local setup data is encoded: PC name, adapter name/type, MAC, IPv4, gateway, subnet prefix, broadcast IP, source tag, and creation time.
                        </TextBlock>
                        <TextBlock Margin="0,8,0,0" Foreground="{StaticResource TextSecondaryBrush}" TextWrapping="Wrap">
                            No Windows passwords, relay tokens, API keys, or secrets are collected. The Windows Companion does not wake the PC directly or solve remote wake by itself.
                        </TextBlock>
                    </StackPanel>
                </Border>
            </Grid>
        </Grid>
    </Grid>
</Window>
'@

$reader = New-Object System.Xml.XmlNodeReader $xaml
$window = [Windows.Markup.XamlReader]::Load($reader)

$controls = @{
    DetectButton       = $window.FindName('DetectButton')
    RefreshButton      = $window.FindName('RefreshButton')
    GenerateButton     = $window.FindName('GenerateButton')
    AdapterComboBox    = $window.FindName('AdapterComboBox')
    PcNameTextBox      = $window.FindName('PcNameTextBox')
    AdapterTypeTextBox = $window.FindName('AdapterTypeTextBox')
    MacAddressTextBox  = $window.FindName('MacAddressTextBox')
    Ipv4TextBox        = $window.FindName('Ipv4TextBox')
    GatewayTextBox     = $window.FindName('GatewayTextBox')
    PrefixTextBox      = $window.FindName('PrefixTextBox')
    BroadcastTextBox   = $window.FindName('BroadcastTextBox')
    StatusTextBlock    = $window.FindName('StatusTextBlock')
    WarningsTextBox    = $window.FindName('WarningsTextBox')
    QrImage            = $window.FindName('QrImage')
}

$state = [pscustomobject]@{
    Candidates        = @()
    SelectedCandidate = $null
    DetectedCandidate = $null
    Artifacts         = $null
}

function Set-Status {
    param(
        [string]$Message,
        [string]$Color = '#F8FAFC'
    )

    $controls.StatusTextBlock.Text = $Message
    $controls.StatusTextBlock.Foreground = [System.Windows.Media.BrushConverter]::new().ConvertFromString($Color)
}

function Set-Warnings {
    param([string[]]$Lines)

    $controls.WarningsTextBox.Text = ($Lines | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join [Environment]::NewLine
}

function Clear-DetectedFields {
    foreach ($name in 'PcNameTextBox','AdapterTypeTextBox','MacAddressTextBox','Ipv4TextBox','GatewayTextBox','PrefixTextBox','BroadcastTextBox') {
        $controls.$name.Text = ''
    }
    $controls.QrImage.Source = $null
    $state.DetectedCandidate = $null
    $state.Artifacts = $null
}

function Convert-QrBytesToImageSource {
    param([byte[]]$Bytes)

    $bitmap = [System.Windows.Media.Imaging.BitmapImage]::new()
    $stream = [System.IO.MemoryStream]::new($Bytes)
    try {
        $bitmap.BeginInit()
        $bitmap.CacheOption = [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad
        $bitmap.StreamSource = $stream
        $bitmap.EndInit()
        $bitmap.Freeze()
        return $bitmap
    } finally {
        $stream.Dispose()
    }
}

function Format-AdapterDisplayLabel {
    param($Candidate)

    $description = if ($Candidate.InterfaceDescription) { $Candidate.InterfaceDescription } else { $Candidate.InterfaceAlias }
    $statusLabel = if ($Candidate.Status) { $Candidate.Status } else { 'Unknown' }
    $routeLabel = if ($Candidate.HasDefaultRoute) { 'Internet route' } else { 'No internet route' }
    return '{0} | {1} | {2} | {3}' -f $Candidate.InterfaceAlias, $description, $statusLabel, $routeLabel
}

function Show-SelectedAdapterSummary {
    param($Candidate)

    if (-not $Candidate) {
        return
    }

    $state.SelectedCandidate = $Candidate
    Set-Warnings -Lines @(
        "Selected adapter: $($Candidate.InterfaceAlias)",
        "Hardware: $($Candidate.InterfaceDescription)",
        "Status: $($Candidate.Status)",
        $(if ($Candidate.HasDefaultRoute) { 'This adapter currently carries the default internet route.' } else { 'This adapter is available, but it is not the current default internet route.' })
    ) + @(Get-CompanionWarnings -SelectedAdapter $Candidate)
}

function Detect-SelectedAdapter {
    param($Candidate)

    if (-not $Candidate) {
        throw 'Select an adapter first.'
    }

    $state.SelectedCandidate = $Candidate
    $state.DetectedCandidate = $Candidate
    $controls.PcNameTextBox.Text = $env:COMPUTERNAME
    $controls.AdapterTypeTextBox.Text = if ($Candidate.AdapterType -eq 'wifi') { 'Wireless' } else { 'Wired' }
    $controls.MacAddressTextBox.Text = if ($Candidate.MacAddress) { Normalize-MacAddress -MacAddress $Candidate.MacAddress } else { '' }
    $controls.Ipv4TextBox.Text = $Candidate.IPv4Address
    $controls.GatewayTextBox.Text = $Candidate.DefaultGateway
    $controls.PrefixTextBox.Text = if ($null -ne $Candidate.PrefixLength) { "/$($Candidate.PrefixLength)" } else { '' }
    $controls.BroadcastTextBox.Text = if ($Candidate.IPv4Address -and $null -ne $Candidate.PrefixLength) { Get-BroadcastIp -IpAddress $Candidate.IPv4Address -PrefixLength $Candidate.PrefixLength } else { '' }
    $warnings = @(
        "Detected adapter: $($Candidate.InterfaceAlias)",
        "Hardware: $($Candidate.InterfaceDescription)",
        "Connection type: $(if ($Candidate.AdapterType -eq 'wifi') { 'Wireless' } else { 'Wired' })"
    ) + @(Get-CompanionWarnings -SelectedAdapter $Candidate)
    if (-not ($warnings | Where-Object { $_ -like 'Selected adapter*' -or $_ -like 'Detected adapter*' })) {
        $warnings = @("Detected adapter: $($Candidate.InterfaceAlias)") + $warnings
    }
    Set-Warnings -Lines $warnings
}

function Load-AdapterInventory {
    Set-Status -Message 'Loading available adapters...' -Color '#FCEE09'
    Clear-DetectedFields

    $candidates = @(Get-ScoredAdapterCandidates)
    if (-not $candidates) {
        $controls.AdapterComboBox.ItemsSource = $null
        Set-Warnings -Lines @('No adapters were found on this PC.')
        Set-Status -Message 'Could not load any adapters.' -Color '#FF1744'
        return
    }

    foreach ($candidate in $candidates) {
        $candidate | Add-Member -NotePropertyName DisplayLabel -NotePropertyValue (Format-AdapterDisplayLabel -Candidate $candidate) -Force
    }

    $state.Candidates = $candidates
    $controls.AdapterComboBox.ItemsSource = $candidates
    $controls.AdapterComboBox.SelectedIndex = 0
    Show-SelectedAdapterSummary -Candidate $candidates[0]
    $message = if ($candidates.Count -gt 1) {
        "Loaded $($candidates.Count) adapters. Select the one you want, then click Detect This PC."
    } else {
        'Loaded 1 adapter. Click Detect This PC to use it.'
    }
    Set-Status -Message $message -Color '#00E5FF'
}

function Generate-SetupArtifactsFromSelection {
    if (-not $state.SelectedCandidate) {
        throw 'Select an adapter first.'
    }
    if (-not $state.DetectedCandidate) {
        throw 'Click Detect This PC before generating the QR code.'
    }
    if ($state.DetectedCandidate.InterfaceIndex -ne $state.SelectedCandidate.InterfaceIndex) {
        throw 'The selected adapter changed after detection. Click Detect This PC again before generating the QR code.'
    }

    $artifacts = New-PowerBridgeSetupArtifacts -SelectedAdapter $state.DetectedCandidate -NoClipboard -InMemoryOnly
    $state.Artifacts = $artifacts
    $controls.QrImage.Source = Convert-QrBytesToImageSource -Bytes $artifacts.QrBytes

    $warnings = @($artifacts.Warnings)
    if (-not $warnings) {
        $warnings = @('No issues detected for the selected adapter.')
    }
    Set-Warnings -Lines $warnings
    Set-Status -Message 'Scan-safe QR generated. Ready for Android scan.' -Color '#00D26A'
}

$controls.AdapterComboBox.Add_SelectionChanged({
    if ($controls.AdapterComboBox.SelectedItem) {
        Clear-DetectedFields
        Show-SelectedAdapterSummary -Candidate $controls.AdapterComboBox.SelectedItem
        $controls.QrImage.Source = $null
        $state.Artifacts = $null
        Set-Status -Message "Adapter selected: $($controls.AdapterComboBox.SelectedItem.InterfaceAlias). Click Detect This PC to use it." -Color '#00E5FF'
    }
})

$controls.DetectButton.Add_Click({
    try {
        Detect-SelectedAdapter -Candidate $controls.AdapterComboBox.SelectedItem
        Set-Status -Message "Detected settings for $($controls.AdapterComboBox.SelectedItem.InterfaceAlias)." -Color '#00D26A'
    } catch {
        Set-Warnings -Lines @($_.Exception.Message)
        Set-Status -Message $_.Exception.Message -Color '#FF1744'
    }
})

$controls.RefreshButton.Add_Click({
    try {
        Load-AdapterInventory
    } catch {
        Set-Warnings -Lines @($_.Exception.Message)
        Set-Status -Message $_.Exception.Message -Color '#FF1744'
    }
})

$controls.GenerateButton.Add_Click({
    try {
        Generate-SetupArtifactsFromSelection
    } catch {
        Set-Warnings -Lines @($_.Exception.Message)
        Set-Status -Message $_.Exception.Message -Color '#FF1744'
    }
})

$window.Add_Closed({
    $state.Artifacts = $null
})

Load-AdapterInventory

$window.ShowDialog() | Out-Null
