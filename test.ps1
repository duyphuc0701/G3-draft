$response = Invoke-RestMethod -Uri "http://localhost:8080/onboarding" -Method POST
$sessionId = $response.sessionId
Write-Output "Session ID: $sessionId"

$details = @{
    firstName = "John"
    lastName = "Doe"
    email = "john@example.com"
    contactPhone = "1234567890"
    dob = "1990-01-01"
}
Invoke-RestMethod -Uri "http://localhost:8080/onboarding/$sessionId/personal-details" -Method PUT -Body ($details | ConvertTo-Json) -ContentType "application/json"

$docs = @{
    type = "PASSPORT"
    frontImage = "base64-front"
    backImage = "base64-back"
    selfieImage = "base64-selfie"
}
Invoke-RestMethod -Uri "http://localhost:8080/onboarding/$sessionId/documents" -Method POST -Body ($docs | ConvertTo-Json) -ContentType "application/json"

Invoke-RestMethod -Uri "http://localhost:8080/onboarding/$sessionId/verify" -Method POST
