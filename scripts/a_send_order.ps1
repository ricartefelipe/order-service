param(
  [string]$OrderServiceUrl = $env:ORDER_SERVICE_URL,
  [string]$ExternalOrderId = $null
)

if ([string]::IsNullOrWhiteSpace($OrderServiceUrl)) {
  $OrderServiceUrl = "http://localhost:8080"
}

if ([string]::IsNullOrWhiteSpace($ExternalOrderId)) {
  $ExternalOrderId = "A-" + [int][double]::Parse((Get-Date -UFormat %s))
}

$correlationId = $env:X_CORRELATION_ID
if ([string]::IsNullOrWhiteSpace($correlationId)) {
  $correlationId = [guid]::NewGuid().ToString()
}

$payload = @{
  externalOrderId = $ExternalOrderId
  items = @(
    @{ productId = "SKU-1"; quantity = 2; unitPrice = 10.50 },
    @{ productId = "SKU-2"; quantity = 1; unitPrice = 5.00 }
  )
} | ConvertTo-Json -Depth 5

Write-Host "POST $OrderServiceUrl/orders"
Write-Host "X-Correlation-Id: $correlationId"
Write-Host "externalOrderId: $ExternalOrderId"

try {
  $response = Invoke-RestMethod -Method Post -Uri "$OrderServiceUrl/orders" -Body $payload -ContentType "application/json" -Headers @{"X-Correlation-Id"=$correlationId}
  $response | ConvertTo-Json -Depth 10
} catch {
  Write-Host "Request failed: $($_.Exception.Message)"
  if ($_.Exception.Response -ne $null) {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $reader.ReadToEnd()
  }
  exit 1
}
