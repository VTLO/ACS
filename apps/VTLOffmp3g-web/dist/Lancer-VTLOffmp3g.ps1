# VTLOffmp3g : petit serveur local (aucune installation requise) puis ouverture du navigateur.
# Sert ce dossier sur http://localhost:8765/ tant que cette fenetre reste ouverte.
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$port = 8765
$types = @{
  ".html" = "text/html; charset=utf-8"; ".js" = "text/javascript; charset=utf-8"; ".mjs" = "text/javascript; charset=utf-8"
  ".css" = "text/css; charset=utf-8"; ".json" = "application/json"; ".wasm" = "application/wasm"
  ".png" = "image/png"; ".ttf" = "font/ttf"; ".ffpreset" = "text/plain"; ".txt" = "text/plain; charset=utf-8"
}
$listener = New-Object System.Net.HttpListener
while ($true) {
  try {
    $listener.Prefixes.Clear()
    $listener.Prefixes.Add("http://localhost:$port/")
    $listener.Start()
    break
  } catch {
    $port++
    $listener = New-Object System.Net.HttpListener
    if ($port -gt 8800) { throw }
  }
}
$url = "http://localhost:$port/"
Write-Host ""
Write-Host "  VTLOffmp3g est pret : $url" -ForegroundColor Green
Write-Host "  Laissez cette fenetre ouverte pendant l'utilisation. Fermez-la pour arreter."
Write-Host ""
Start-Process $url
while ($listener.IsListening) {
  $ctx = $listener.GetContext()
  $res = $ctx.Response
  try {
    $path = [Uri]::UnescapeDataString($ctx.Request.Url.AbsolutePath.TrimStart("/"))
    if ($path -eq "") { $path = "index.html" }
    $file = [IO.Path]::GetFullPath((Join-Path $root $path))
    if (-not $file.StartsWith($root) -or -not (Test-Path $file -PathType Leaf)) {
      $res.StatusCode = 404
    } else {
      $ext = [IO.Path]::GetExtension($file).ToLower()
      $res.ContentType = if ($types.ContainsKey($ext)) { $types[$ext] } else { "application/octet-stream" }
      $bytes = [IO.File]::ReadAllBytes($file)
      $res.ContentLength64 = $bytes.Length
      $res.OutputStream.Write($bytes, 0, $bytes.Length)
    }
  } catch {
    $res.StatusCode = 500
  } finally {
    $res.Close()
  }
}
