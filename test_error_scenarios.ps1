# Test error handling scenarios

Write-Host "=== Testing Error Handling Scenarios ===`n"

# Test 1: GET event by non-existent ID
Write-Host "1. Testing GET event by non-existent ID (ID: 999)"
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/events/999" -Method Get
    Write-Host "Unexpected success: $response"
} catch {
    Write-Host "Expected error caught: $($_.Exception.Message)"
    Write-Host "Status code: $($_.Exception.Response.StatusCode.value__)"
}

# Test 2: PUT update non-existent event
Write-Host "`n2. Testing PUT update non-existent event (ID: 999)"
try {
    $updateJson = @{
        title = "Updated Event"
        description = "Updated description"
        startTime = "2024-12-15T09:00:00"
        endTime = "2024-12-15T17:00:00"
        location = "Updated Location"
    } | ConvertTo-Json -Depth 10
    
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/events/999" -Method Put -Body $updateJson -ContentType 'application/json'
    Write-Host "Unexpected success: $response"
} catch {
    Write-Host "Expected error caught: $($_.Exception.Message)"
    Write-Host "Status code: $($_.Exception.Response.StatusCode.value__)"
}

# Test 3: DELETE non-existent event
Write-Host "`n3. Testing DELETE non-existent event (ID: 999)"
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/events/999" -Method Delete
    Write-Host "Unexpected success: $response"
} catch {
    Write-Host "Expected error caught: $($_.Exception.Message)"
    Write-Host "Status code: $($_.Exception.Response.StatusCode.value__)"
}

# Test 4: POST with invalid data (missing required fields)
Write-Host "`n4. Testing POST with invalid data (missing required fields)"
try {
    $invalidJson = @{
        title = ""
        description = "Test event"
        # Missing startTime, endTime, location
    } | ConvertTo-Json -Depth 10
    
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/events" -Method Post -Body $invalidJson -ContentType 'application/json'
    Write-Host "Unexpected success: $response"
} catch {
    Write-Host "Expected error caught: $($_.Exception.Message)"
    Write-Host "Status code: $($_.Exception.Response.StatusCode.value__)"
}

# Test 5: POST with invalid date format
Write-Host "`n5. Testing POST with invalid date format"
try {
    $invalidDateJson = @{
        title = "Test Event"
        description = "Test event description"
        startTime = "invalid-date"
        endTime = "2024-12-15T17:00:00"
        location = "Test Location"
    } | ConvertTo-Json -Depth 10
    
    $response = Invoke-RestMethod -Uri "http://localhost:8083/api/events" -Method Post -Body $invalidDateJson -ContentType 'application/json'
    Write-Host "Unexpected success: $response"
} catch {
    Write-Host "Expected error caught: $($_.Exception.Message)"
    Write-Host "Status code: $($_.Exception.Response.StatusCode.value__)"
}

Write-Host "`n=== Error Testing Complete ==="