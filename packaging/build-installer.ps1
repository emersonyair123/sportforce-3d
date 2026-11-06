<#
.SYNOPSIS
    Genera el instalador de Windows de SPORTFORCE 3D con jlink + jpackage (no hace falta Java en el equipo destino).

.DESCRIPTION
    1. Compila y ejecuta las pruebas (mvnw verify), salvo con -SkipTests.
    2. Copia el .jar de la aplicación y sus dependencias a target\installer\input.
    3. Crea con jlink un Java 21 reducido (target\installer\runtime) con los módulos que usa la aplicación.
    4. Con jpackage crea:
         - target\installer\dist\SPORTFORCE 3D-1.0.0.exe   (instalador; necesita WiX Toolset 3.x)
         - o, sin WiX, la carpeta portable target\installer\dist\SPORTFORCE 3D\ con "SPORTFORCE 3D.exe".
    Ver README, sección "Generar el instalador".

.PARAMETER Type
    exe (por defecto), msi o app-image (carpeta portable, no necesita WiX).

.PARAMETER SkipTests
    No ejecuta las pruebas antes de empaquetar.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File packaging\build-installer.ps1
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File packaging\build-installer.ps1 -Type app-image -SkipTests
#>
param(
    [ValidateSet('exe', 'msi', 'app-image')]
    [string]$Type = 'exe',
    [switch]$SkipTests
)

# Continue: java, mvnw y jpackage escriben avisos en stderr; los errores se detectan con $LASTEXITCODE.
$ErrorActionPreference = 'Continue'
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$AppName = 'SPORTFORCE 3D'
$Version = '1.0.0'
$MainClass = 'com.sportforce3d.app.Launcher'
# Mismo UUID en todas las versiones: así un instalador nuevo actualiza el anterior en lugar de duplicarlo.
$UpgradeUuid = '6f1c2b8e-3d4a-4f0b-9a57-5f3d2e1c0b3d'
$Icon = Join-Path $Root 'src\packaging\windows\sportforce3d.ico'
$Out = Join-Path $Root 'target\installer'

# Módulos del JDK que usa la aplicación (jdeps sobre la app y sus dependencias), más:
#   jdk.localedata  fechas y meses en español (es-PE)      jdk.charsets  codificaciones de POI/OpenPDF
#   jdk.crypto.ec   TLS moderno (no se usa red, se deja por robustez)      jdk.unsupported  lo usa JavaFX
# java.desktop es obligatorio: TextureComposer dibuja las texturas con Java2D y OpenPDF usa java.awt.Color.
$Modules = 'java.base,java.desktop,java.logging,java.management,java.naming,java.prefs,java.scripting,' +
           'java.sql,java.xml,java.xml.crypto,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.zipfs'

function Find-JdkTool([string]$tool) {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME "bin\$tool.exe" }
    $java = Get-Command java -ErrorAction SilentlyContinue
    if ($java) {
        $jdkHome = (& $java.Source -XshowSettings:properties -version 2>&1 | Select-String 'java.home = (.*)').Matches |
                ForEach-Object { $_.Groups[1].Value.Trim() } | Select-Object -First 1
        if ($jdkHome) { $candidates += Join-Path $jdkHome "bin\$tool.exe" }
    }
    foreach ($c in $candidates) { if (Test-Path $c) { return $c } }
    throw "No se encontró $tool.exe. Instala el JDK 21 (no solo el JRE) y define JAVA_HOME. Ver README."
}

$jlink = Find-JdkTool 'jlink'
$jpackage = Find-JdkTool 'jpackage'
$javaVersion = (& (Join-Path (Split-Path $jlink) 'java.exe') -version 2>&1 | Select-Object -First 1)
Write-Host "JDK: $javaVersion"
if ($javaVersion -notmatch '"21\.') {
    throw "Se necesita el JDK 21 (encontrado: $javaVersion). Apunta JAVA_HOME al JDK 21."
}

if ($Type -ne 'app-image') {
    $candle = Get-Command candle.exe -ErrorAction SilentlyContinue
    $light = Get-Command light.exe -ErrorAction SilentlyContinue
    if (-not ($candle -and $light)) {
        $wixBin = Get-ChildItem 'C:\Program Files (x86)\WiX Toolset v3*\bin' -Directory -ErrorAction SilentlyContinue |
                  Sort-Object Name -Descending | Select-Object -First 1
        if ($wixBin) {
            $env:PATH = "$($wixBin.FullName);$env:PATH"
        } else {
            Write-Warning @"
Falta WiX Toolset 3.x (jpackage del JDK 21 lo necesita para crear .exe o .msi).
Instálalo con UNA de estas opciones y vuelve a ejecutar este script en una terminal nueva:
  1) winget install --id WiXToolset.WiXToolset -e
  2) Descarga wix314.exe de https://github.com/wixtoolset/wix3/releases (v3.14.1) e instálalo.
Mientras tanto se genera la versión portable (carpeta con SPORTFORCE 3D.exe, sin instalador).
"@
            $Type = 'app-image'
        }
    }
}

# 1. Compilar (y probar)
$mvnw = Join-Path $Root 'mvnw.cmd'
if ($SkipTests) { & $mvnw -q -DskipTests package } else { & $mvnw -q verify }
if ($LASTEXITCODE -ne 0) { throw 'La compilación o las pruebas fallaron: revisa la salida de Maven.' }

# 2. Jar + dependencias
if (Test-Path $Out) { Remove-Item -Recurse -Force $Out }
$InputDir = Join-Path $Out 'input'
New-Item -ItemType Directory -Force $InputDir | Out-Null
& $mvnw -q dependency:copy-dependencies -DincludeScope=runtime "-DoutputDirectory=$InputDir"
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron copiar las dependencias.' }
Copy-Item (Join-Path $Root "target\sportforce3d-$Version.jar") $InputDir

# 3. Java reducido
$Runtime = Join-Path $Out 'runtime'
& $jlink --add-modules $Modules --strip-debug --no-header-files --no-man-pages --compress=zip-6 `
         --include-locales=es,es-PE,en --output $Runtime
if ($LASTEXITCODE -ne 0) { throw 'jlink falló.' }

# 4. Instalador (o carpeta portable)
$Dist = Join-Path $Out 'dist'
$jpArgs = @(
    '--type', $Type,
    '--name', $AppName,
    '--app-version', $Version,
    '--vendor', 'SPORTFORCE 3D',
    '--copyright', 'SPORTFORCE 3D',
    '--description', 'Tienda de ropa deportiva con diseñador 3D',
    '--icon', $Icon,
    '--input', $InputDir,
    '--main-jar', "sportforce3d-$Version.jar",
    '--main-class', $MainClass,
    '--runtime-image', $Runtime,
    '--java-options', '-Dfile.encoding=UTF-8',
    '--java-options', '-Xmx1g',
    '--dest', $Dist
)
if ($Type -ne 'app-image') {
    $jpArgs += @('--win-menu', '--win-menu-group', $AppName, '--win-shortcut', '--win-shortcut-prompt',
               '--win-dir-chooser', '--win-upgrade-uuid', $UpgradeUuid)
}
& $jpackage @jpArgs
if ($LASTEXITCODE -ne 0) { throw 'jpackage falló.' }

Write-Host ''
if ($Type -eq 'app-image') {
    Write-Host "Listo: carpeta portable en $Dist\$AppName (ejecuta `"$AppName.exe`")."
} else {
    Get-ChildItem $Dist -Filter "*.$Type" | ForEach-Object { Write-Host "Listo: instalador $($_.FullName)" }
}
Write-Host "Al instalarla, la base de datos se crea en %APPDATA%\SportForce3D\data\sportforce3d.db"
