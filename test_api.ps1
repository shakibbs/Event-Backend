# Test POST create event endpoint
try {
    $json = Get-Content 'test_event.json' -Raw
    $response = Invoke-RestMethod -Uri 'http://localhost:8083/api/events' -Method Post -Body $json -ContentType 'application/json'
    Write-Host "Event created successfully:"
    $response | ConvertTo-Json -Depth 10
    $createdEventId = $response.id
    Write-Host "Created Event ID: $createdEventId"
    
    # Test GET event by ID endpoint
    Write-Host "`n--- Testing GET event by ID ---"
    $getByIdResponse = Invoke-RestMethod -Uri "http://localhost:8083/api/events/$createdEventId" -Method Get
    Write-Host "Event retrieved by ID:"
    $getByIdResponse | ConvertTo-Json -Depth 10
    
    # Test PUT update event endpoint
    Write-Host "`n--- Testing PUT update event ---"
    $updateJson = @{
        title = "Updated Tech Conference 2024"
        description = "Updated annual technology conference featuring latest innovations"
        startTime = "2024-12-15T09:00:00"
        endTime = "2024-12-15T18:00:00"
        location = "Updated Convention Center"
    } | ConvertTo-Json -Depth 10
    
    $updateResponse = Invoke-RestMethod -Uri "http://localhost:8083/api/events/$createdEventId" -Method Put -Body $updateJson -ContentType 'application/json'
    Write-Host "Event updated successfully:"
    $updateResponse | ConvertTo-Json -Depth 10
    
    # Test DELETE event endpoint
    Write-Host "`n--- Testing DELETE event ---"
    $deleteResponse = Invoke-RestMethod -Uri "http://localhost:8083/api/events/$createdEventId" -Method Delete
    Write-Host "Event deleted successfully. Status code: $($deleteResponse.StatusCode)"
    
} catch {
    Write-Host "Error occurred: $($_.Exception.Message)"
    Write-Host "Status code: $($_.Exception.Response.StatusCode.value__)"
    Write-Host "Response: $($_.Exception.Response.GetResponseStream())"
}