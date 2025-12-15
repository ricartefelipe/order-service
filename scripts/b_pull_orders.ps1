param(
  [string]$OrderServiceUrl = $env:ORDER_SERVICE_URL,
  [string]$Status = "CALCULATED",
  [int]$Page = 0,
  [int]$Size = 20,
  [string]$Sort = "createdAt,desc"
)

if ([string]::IsNullOrWhiteSpace($OrderServiceUrl)) {
  $OrderServiceUrl = "http://localhost:8080"
}

$correlationId = $env:X_CORRELATION_ID
if ([string]::IsNullOrWhiteSpace($correlationId)) {
  $correlationId = [guid]::NewGuid().ToString()
}

$url = "$OrderServiceUrl/orders?status=$Status&page=$Page&size=$Size&sort=$Sort"

Write-Host "GET $url"
Write-Host "X-Correlation-Id: $correlationId"

try {
  $response = Invoke-RestMethod -Method Get -Uri $url -Headers @{"X-Correlation-Id"=$correlationId}
  $response | ConvertTo-Json -Depth 10
} catch {
  Write-Host "Request failed: $($_.Exception.Message)"
  if ($_.Exception.Response -ne $null) {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $reader.ReadToEnd()
  }
  exit 1
}
