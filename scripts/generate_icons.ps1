Add-Type -AssemblyName System.Drawing
$uploadedFile = Get-ChildItem -Path "C:\Users\Michael\.gemini\antigravity-ide\brain\d2737f68-ed1b-470f-aac8-e283ca8794cc\.user_uploaded" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $uploadedFile) {
    Write-Error "Source image not found."
    exit 1
}

Write-Host "Using Source Image: $($uploadedFile.FullName)"
$srcImg = [System.Drawing.Image]::FromFile($uploadedFile.FullName)

$densities = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

$resDir = "d:\Projects\cursor\UAS_Ready\app\src\main\res"

foreach ($entry in $densities.GetEnumerator()) {
    $folder = $entry.Key
    $size = $entry.Value
    $targetFolder = Join-Path $resDir $folder
    if (-not (Test-Path $targetFolder)) {
        New-Item -ItemType Directory -Path $targetFolder -Force | Out-Null
    }
    
    $destBitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($destBitmap)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    
    $graphics.DrawImage($srcImg, 0, 0, $size, $size)
    $graphics.Dispose()
    
    $outPath = Join-Path $targetFolder "ic_launcher.png"
    $outPathRound = Join-Path $targetFolder "ic_launcher_round.png"
    $destBitmap.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $destBitmap.Save($outPathRound, [System.Drawing.Imaging.ImageFormat]::Png)
    $destBitmap.Dispose()
    Write-Host "Generated $folder/ic_launcher.png ($size x $size)"
}

$drawableDir = Join-Path $resDir "drawable"
if (-not (Test-Path $drawableDir)) { New-Item -ItemType Directory -Path $drawableDir -Force | Out-Null }
$logoBitmap = New-Object System.Drawing.Bitmap(512, 512)
$logoG = [System.Drawing.Graphics]::FromImage($logoBitmap)
$logoG.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$logoG.DrawImage($srcImg, 0, 0, 512, 512)
$logoG.Dispose()
$logoPath = Join-Path $drawableDir "app_logo.png"
$logoBitmap.Save($logoPath, [System.Drawing.Imaging.ImageFormat]::Png)
$logoBitmap.Dispose()

$srcImg.Dispose()
Write-Host "Successfully generated all Android launcher icon mipmaps and app_logo.png with new icon!"
