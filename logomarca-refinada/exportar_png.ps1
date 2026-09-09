$ErrorActionPreference = 'Stop'

$assetRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$svgDirectory = Join-Path $assetRoot 'svg'
$pngDirectory = Join-Path $assetRoot 'png'
$temporaryRoot = [System.IO.Path]::GetFullPath($env:TEMP)

$browserCandidates = @(
    'C:\Program Files\Google\Chrome\Application\chrome.exe',
    'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    'C:\Program Files\Microsoft\Edge\Application\msedge.exe'
)
$browser = $browserCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $browser) {
    throw 'Google Chrome ou Microsoft Edge não encontrado.'
}

New-Item -ItemType Directory -Path $pngDirectory -Force | Out-Null

function Export-CineGiroPng {
    param(
        [Parameter(Mandatory)] [string] $SourceName,
        [Parameter(Mandatory)] [string] $OutputName,
        [Parameter(Mandatory)] [int] $Size
    )

    $sourcePath = Join-Path $svgDirectory $SourceName
    $outputPath = Join-Path $pngDirectory $OutputName
    if (-not (Test-Path -LiteralPath $sourcePath)) {
        throw "SVG não encontrado: $sourcePath"
    }

    $sourceUri = [System.Uri]::new((Resolve-Path -LiteralPath $sourcePath).Path).AbsoluteUri
    $temporaryHtml = Join-Path $temporaryRoot "cinegiro-export-$([guid]::NewGuid()).html"
    $html = @"
<!doctype html><html><head><meta charset="utf-8"><style>
html,body{width:${Size}px;height:${Size}px;margin:0;overflow:hidden;background:transparent}
img{display:block;width:${Size}px;height:${Size}px}
</style></head><body><img src="$sourceUri"></body></html>
"@

    try {
        Set-Content -LiteralPath $temporaryHtml -Value $html -Encoding utf8
        $temporaryUri = [System.Uri]::new($temporaryHtml).AbsoluteUri
        $arguments = @(
            '--headless',
            '--disable-gpu',
            '--hide-scrollbars',
            '--default-background-color=00000000',
            '--force-device-scale-factor=1',
            "--screenshot=`"$outputPath`"",
            "--window-size=$Size,$Size",
            "`"$temporaryUri`""
        )
        Start-Process -FilePath $browser -ArgumentList $arguments -Wait -WindowStyle Hidden
        if (-not (Test-Path -LiteralPath $outputPath)) {
            throw "Falha ao exportar $OutputName"
        }
    }
    finally {
        $resolvedTemporaryHtml = [System.IO.Path]::GetFullPath($temporaryHtml)
        if ($resolvedTemporaryHtml.StartsWith($temporaryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
            Remove-Item -LiteralPath $resolvedTemporaryHtml -Force -ErrorAction SilentlyContinue
        }
    }
}

Export-CineGiroPng -SourceName 'cinegiro-icone-app.svg' -OutputName 'cinegiro-icon-192.png' -Size 192
Export-CineGiroPng -SourceName 'cinegiro-icone-app.svg' -OutputName 'cinegiro-icon-512.png' -Size 512
Export-CineGiroPng -SourceName 'cinegiro-icone-app.svg' -OutputName 'apple-touch-icon-180.png' -Size 180
Export-CineGiroPng -SourceName 'cinegiro-icone-maskable.svg' -OutputName 'cinegiro-maskable-512.png' -Size 512
Export-CineGiroPng -SourceName 'cinegiro-favicon.svg' -OutputName 'cinegiro-favicon-32.png' -Size 32

Write-Output "PNGs exportados em $pngDirectory"
