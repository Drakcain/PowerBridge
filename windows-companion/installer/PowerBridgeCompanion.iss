#ifndef MyAppVersion
  #define MyAppVersion "0.6.1"
#endif
#ifndef MyPayloadDir
  #define MyPayloadDir "..\build\payload"
#endif

#define MyAppName "PowerBridge Windows Companion"
#define MyAppPublisher "PowerBridge Project"
#define MyAppURL "https://github.com/Drakcain/PowerBridge"

[Setup]
AppId=PowerBridgeWindowsCompanion
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}/issues
AppUpdatesURL={#MyAppURL}/releases
DefaultDirName=C:\Tools\PowerBridgeCompanion
DefaultGroupName=PowerBridge Windows Companion
OutputDir=..\dist
OutputBaseFilename=PowerBridge-Companion-Setup-v{#MyAppVersion}
SetupIconFile=..\assets\icon\PowerBridgeCompanion.ico
UninstallDisplayIcon={app}\assets\icon\PowerBridgeCompanion.ico
Compression=lzma2/ultra64
SolidCompression=yes
DisableProgramGroupPage=no
DisableDirPage=no
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
WizardStyle=modern
SetupLogging=yes
Uninstallable=yes
UninstallDisplayName=PowerBridge Windows Companion
MinVersion=10.0.17763
InfoBeforeFile=..\..\INSTALL-NOTICE.txt
CloseApplications=no

[Files]
Source: "{#MyPayloadDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "..\..\LICENSE"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\INSTALL-NOTICE.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\THIRD-PARTY-NOTICES.md"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\SIGNING.md"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\VERSION"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\PowerBridge Windows Companion"; Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoLogo -NoProfile -ExecutionPolicy Bypass -File ""{app}\scripts\Start-PowerBridgeCompanionGui.ps1"""; WorkingDir: "{app}"; IconFilename: "{app}\assets\icon\PowerBridgeCompanion.ico"
Name: "{group}\Check for PowerBridge Updates"; Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoLogo -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File ""{app}\scripts\Update-PowerBridgeCompanion.ps1"""; WorkingDir: "{app}"; IconFilename: "{app}\assets\icon\PowerBridgeCompanion.ico"
Name: "{group}\PowerBridge Companion README"; Filename: "{sys}\notepad.exe"; Parameters: """{app}\README.md"""; WorkingDir: "{app}"
Name: "{group}\PowerBridge on GitHub"; Filename: "{#MyAppURL}"
Name: "{group}\Uninstall PowerBridge Windows Companion"; Filename: "{uninstallexe}"; IconFilename: "{app}\assets\icon\PowerBridgeCompanion.ico"
Name: "{commondesktop}\PowerBridge Windows Companion"; Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoLogo -NoProfile -ExecutionPolicy Bypass -File ""{app}\scripts\Start-PowerBridgeCompanionGui.ps1"""; WorkingDir: "{app}"; Tasks: desktopicon; IconFilename: "{app}\assets\icon\PowerBridgeCompanion.ico"

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop shortcut"; Flags: unchecked

[Run]
Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoLogo -NoProfile -ExecutionPolicy Bypass -File ""{app}\scripts\Start-PowerBridgeCompanionGui.ps1"""; Description: "Launch PowerBridge Windows Companion"; Flags: nowait postinstall skipifsilent
