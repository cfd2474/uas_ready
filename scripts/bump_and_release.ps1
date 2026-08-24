<#
.SYNOPSIS
    Automates version bumping, signed APK generation, and release archiving for UASReady.

.DESCRIPTION
    1. Bumps version index / name in version.properties.
    2. Moves any existing release APKs from 'releases/current/' to 'releases/archive/'.
    3. Executes Gradle release build with APK signing.
    4. Copies the signed release APK to 'releases/current/'.
    5. Optionally commits and pushes changes to GitHub.

.PARAMETER BumpType
    Type of version bump: 'patch' (default), 'minor', 'major', or 'build'.

.PARAMETER GitPush
    Switch to automatically commit and push the release to the configured git remote.

.PARAMETER CommitMessage
    Optional custom git commit message.
#>

param(
    [ValidateSet("patch", "minor", "major", "build")]
    [string]$BumpType = "patch",

    [switch]$GitPush = $false,

    [string]$CommitMessage = ""
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $ProjectRoot

$VersionFile = Join-Path $ProjectRoot "version.properties"
$CurrentDir = Join-Path $ProjectRoot "releases\current"
$ArchiveDir = Join-Path $ProjectRoot "releases\archive"

# Ensure directories exist
if (-not (Test-Path $CurrentDir)) { New-Item -ItemType Directory -Path $CurrentDir -Force | Out-Null }
if (-not (Test-Path $ArchiveDir)) { New-Item -ItemType Directory -Path $ArchiveDir -Force | Out-Null }

# Read current version
$code = 1
$major = 1
$minor = 0
$patch = 0

if (Test-Path $VersionFile) {
    $lines = [System.IO.File]::ReadAllLines($VersionFile)
    foreach ($line in $lines) {
        if ($line -match "^VERSION_CODE\s*=\s*(\d+)") {
            $code = [int]$matches[1]
        }
        if ($line -match "^VERSION_NAME\s*=\s*(\d+)\.(\d+)\.(\d+)") {
            $major = [int]$matches[1]
            $minor = [int]$matches[2]
            $patch = [int]$matches[3]
        }
    }
}

# Increment version code
$newCode = $code + 1

# Increment version name based on bump type
switch ($BumpType) {
    "major" {
        $major++
        $minor = 0
        $patch = 0
    }
    "minor" {
        $minor++
        $patch = 0
    }
    "patch" {
        $patch++
    }
    "build" {
        # Keep version name same, only code increments
    }
}

$newName = "$major.$minor.$patch"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " UASReady Release Engine" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Previous: v$major.$minor.$patch (Build $code)" -ForegroundColor Yellow
Write-Host "New:      v$newName (Build $newCode)" -ForegroundColor Green

# Write new version.properties
$newProps = "VERSION_CODE=$newCode`r`nVERSION_NAME=$newName`r`n"
[System.IO.File]::WriteAllText($VersionFile, $newProps, [System.Text.Encoding]::UTF8)

# Move existing APKs in releases/current/ to releases/archive/
$existingCurrentApks = Get-ChildItem -Path $CurrentDir -Filter "*.apk"
foreach ($apk in $existingCurrentApks) {
    $targetArchive = Join-Path $ArchiveDir $apk.Name
    if (Test-Path $targetArchive) {
        $timestamp = (Get-Date).ToString("yyyyMMdd-HHmmss")
        $baseName = [System.IO.Path]::GetFileNameWithoutExtension($apk.Name)
        $ext = [System.IO.Path]::GetExtension($apk.Name)
        $targetArchive = Join-Path $ArchiveDir "${baseName}_${timestamp}${ext}"
    }
    Write-Host "Archiving previous release: $($apk.Name) -> releases\archive\" -ForegroundColor DarkYellow
    Move-Item -Path $apk.FullName -Destination $targetArchive -Force
}

# Build signed release APK
Write-Host "`nBuilding Signed Release APK with Gradle..." -ForegroundColor Cyan
& .\gradlew.bat assembleRelease

if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed with exit code $LASTEXITCODE"
    exit $LASTEXITCODE
}

$builtApk = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $builtApk)) {
    Write-Error "Could not find built release APK at $builtApk"
    exit 1
}

# Copy to current releases folder
$targetApkName = "UASReady-v$newName.apk"
$targetApkPath = Join-Path $CurrentDir $targetApkName
Copy-Item -Path $builtApk -Destination $targetApkPath -Force

Write-Host "`nSigned Release Created Successfully!" -ForegroundColor Green
Write-Host "Current Release: $targetApkPath" -ForegroundColor Green

# Git commit and push if requested
if ($GitPush) {
    $currentBranch = (git branch --show-current).Trim()
    Write-Host "`nCommitting and pushing release to GitHub ($currentBranch)..." -ForegroundColor Cyan
    if ([string]::IsNullOrWhiteSpace($CommitMessage)) {
        $CommitMessage = "Release v$newName (Build $newCode)"
    }
    git add .
    git commit -m $CommitMessage
    git push origin $currentBranch
    Write-Host "Pushed release to origin/$currentBranch successfully!" -ForegroundColor Green
}
