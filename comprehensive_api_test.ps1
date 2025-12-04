# Comprehensive API Test for Event Management System
# Tests all endpoints: Events, Users, Roles, Permissions

$baseUrl = "http://localhost:8083/api"
$testResults = @()

Write-Host "=== COMPREHENSIVE API TEST SUITE ===" -ForegroundColor Green
Write-Host "Testing all API endpoints with various scenarios`n" -ForegroundColor Yellow

# Helper function to log test results
function Log-TestResult {
    param(
        [string]$TestName,
        [string]$Method,
        [string]$Endpoint,
        [int]$StatusCode,
        [string]$Status,
        [string]$Details = ""
    )
    
    $result = @{
        TestName = $TestName
        Method = $Method
        Endpoint = $Endpoint
        StatusCode = $StatusCode
        Status = $Status
        Details = $Details
        Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    }
    
    $script:testResults += $result
    
    $statusColor = if ($Status -eq "PASS") { "Green" } elseif ($Status -eq "FAIL") { "Red" } else { "Yellow" }
    Write-Host "[$Status] $TestName" -ForegroundColor $statusColor
    Write-Host "  Method: $Method | Endpoint: $Endpoint | Status: $StatusCode" -ForegroundColor Gray
    if ($Details) {
        Write-Host "  Details: $Details" -ForegroundColor Gray
    }
    Write-Host ""
}

# ============ EVENT ENDPOINTS TEST ============

Write-Host "1. EVENT MANAGEMENT ENDPOINTS" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan

# Test 1: Create Event (should fail due to permissions)
try {
    $eventJson = @{
        title = "Test Event"
        description = "Test event description"
        startTime = "2024-12-15T09:00:00"
        endTime = "2024-12-15T18:00:00"
        location = "Test Location"
    } | ConvertTo-Json -Depth 10
    
    $response = Invoke-RestMethod -Uri "$baseUrl/events" -Method Post -Body $eventJson -ContentType 'application/json' -ErrorAction Stop
    Log-TestResult -TestName "Create Event" -Method "POST" -Endpoint "/events" -StatusCode 201 -Status "FAIL" -Details "Expected 403 Forbidden but got 201 Created"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 403) {
        Log-TestResult -TestName "Create Event" -Method "POST" -Endpoint "/events" -StatusCode 403 -Status "PASS" -Details "Correctly blocked due to insufficient permissions"
    } else {
        Log-TestResult -TestName "Create Event" -Method "POST" -Endpoint "/events" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Unexpected error: $($_.Exception.Message)"
    }
}

# Test 2: Get All Events
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/events?page=0&size=10" -Method Get
    Log-TestResult -TestName "Get All Events" -Method "GET" -Endpoint "/events" -StatusCode 200 -Status "PASS" -Details "Successfully retrieved paginated events"
} catch {
    Log-TestResult -TestName "Get All Events" -Method "GET" -Endpoint "/events" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
}

# Test 3: Get Non-existent Event
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/events/99999" -Method Get -ErrorAction Stop
    Log-TestResult -TestName "Get Non-existent Event" -Method "GET" -Endpoint "/events/99999" -StatusCode 200 -Status "FAIL" -Details "Expected 404 Not Found"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Log-TestResult -TestName "Get Non-existent Event" -Method "GET" -Endpoint "/events/99999" -StatusCode 404 -Status "PASS" -Details "Correctly returned 404 for non-existent event"
    } else {
        Log-TestResult -TestName "Get Non-existent Event" -Method "GET" -Endpoint "/events/99999" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Unexpected error: $($_.Exception.Message)"
    }
}

# ============ USER ENDPOINTS TEST ============

Write-Host "2. USER MANAGEMENT ENDPOINTS" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan

# Test 4: Create User
try {
    # First get existing roles to find a valid role ID
    $rolesResponse = Invoke-RestMethod -Uri "$baseUrl/roles" -Method Get
    $validRoleId = if ($rolesResponse -and $rolesResponse.Count -gt 0) { $rolesResponse[0].id } else { 2 }
    
    $userJson = @{
        fullName = "Test User"
        email = "testuser$(Get-Random -Minimum 1000 -Maximum 9999)@example.com"
        password = "password123"
        roleId = $validRoleId
    } | ConvertTo-Json -Depth 10
    
    $response = Invoke-RestMethod -Uri "$baseUrl/users" -Method Post -Body $userJson -ContentType 'application/json'
    Log-TestResult -TestName "Create User" -Method "POST" -Endpoint "/users" -StatusCode 201 -Status "PASS" -Details "User created successfully"
    $createdUserId = $response.id
} catch {
    Log-TestResult -TestName "Create User" -Method "POST" -Endpoint "/users" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
}

# Test 5: Get All Users
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/users" -Method Get
    Log-TestResult -TestName "Get All Users" -Method "GET" -Endpoint "/users" -StatusCode 200 -Status "PASS" -Details "Successfully retrieved all users"
} catch {
    Log-TestResult -TestName "Get All Users" -Method "GET" -Endpoint "/users" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
}

# Test 6: Get User by Email (test with existing user)
try {
    # First get existing users to find a valid email
    $usersResponse = Invoke-RestMethod -Uri "$baseUrl/users" -Method Get
    $testEmail = if ($usersResponse -and $usersResponse.Count -gt 0) { $usersResponse[0].email } else { "admin@example.com" }
    
    $response = Invoke-RestMethod -Uri "$baseUrl/users/email/$testEmail" -Method Get
    Log-TestResult -TestName "Get User by Email" -Method "GET" -Endpoint "/users/email/$testEmail" -StatusCode 200 -Status "PASS" -Details "Successfully retrieved user by email"
} catch {
    Log-TestResult -TestName "Get User by Email" -Method "GET" -Endpoint "/users/email/$testEmail" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
}

# ============ ROLE ENDPOINTS TEST ============

Write-Host "3. ROLE MANAGEMENT ENDPOINTS" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan

# Test 7: Get All Roles
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/roles" -Method Get
    Log-TestResult -TestName "Get All Roles" -Method "GET" -Endpoint "/roles" -StatusCode 200 -Status "PASS" -Details "Successfully retrieved all roles"
} catch {
    Log-TestResult -TestName "Get All Roles" -Method "GET" -Endpoint "/roles" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
}

# Test 8: Create Role
try {
    $roleJson = @{
        name = "Test Role $(Get-Random -Minimum 1000 -Maximum 9999)"
        description = "Test role description"
    } | ConvertTo-Json -Depth 10
    
    $response = Invoke-RestMethod -Uri "$baseUrl/roles" -Method Post -Body $roleJson -ContentType 'application/json'
    Log-TestResult -TestName "Create Role" -Method "POST" -Endpoint "/roles" -StatusCode 201 -Status "PASS" -Details "Role created successfully"
    $createdRoleId = $response.id
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Log-TestResult -TestName "Create Role" -Method "POST" -Endpoint "/roles" -StatusCode 409 -Status "PASS" -Details "Correctly prevented duplicate role creation"
    } else {
        Log-TestResult -TestName "Create Role" -Method "POST" -Endpoint "/roles" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
    }
}

# ============ PERMISSION ENDPOINTS TEST ============

Write-Host "4. PERMISSION MANAGEMENT ENDPOINTS" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

# Test 9: Get All Permissions
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/permissions" -Method Get
    Log-TestResult -TestName "Get All Permissions" -Method "GET" -Endpoint "/permissions" -StatusCode 200 -Status "PASS" -Details "Successfully retrieved all permissions"
} catch {
    Log-TestResult -TestName "Get All Permissions" -Method "GET" -Endpoint "/permissions" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
}

# Test 10: Create Permission
try {
    $permissionJson = @{
        name = "TEST_PERMISSION_$(Get-Random -Minimum 1000 -Maximum 9999)"
        description = "Test permission description"
    } | ConvertTo-Json -Depth 10
    
    $response = Invoke-RestMethod -Uri "$baseUrl/permissions" -Method Post -Body $permissionJson -ContentType 'application/json'
    Log-TestResult -TestName "Create Permission" -Method "POST" -Endpoint "/permissions" -StatusCode 201 -Status "PASS" -Details "Permission created successfully"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Log-TestResult -TestName "Create Permission" -Method "POST" -Endpoint "/permissions" -StatusCode 409 -Status "PASS" -Details "Correctly prevented duplicate permission creation"
    } else {
        Log-TestResult -TestName "Create Permission" -Method "POST" -Endpoint "/permissions" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
    }
}

# ============ SWAGGER DOCUMENTATION TEST ============

Write-Host "5. SWAGGER DOCUMENTATION" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan

# Test 11: Swagger UI Access
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/../swagger-ui.html" -Method Get
    if ($response.StatusCode -eq 200) {
        Log-TestResult -TestName "Swagger UI Access" -Method "GET" -Endpoint "/swagger-ui.html" -StatusCode 200 -Status "PASS" -Details "Swagger UI is accessible"
    } else {
        Log-TestResult -TestName "Swagger UI Access" -Method "GET" -Endpoint "/swagger-ui.html" -StatusCode $response.StatusCode -Status "FAIL" -Details "Swagger UI not accessible"
    }
} catch {
    Log-TestResult -TestName "Swagger UI Access" -Method "GET" -Endpoint "/swagger-ui.html" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
}

# Test 12: OpenAPI Docs Access
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/../api-docs" -Method Get
    if ($response.StatusCode -eq 200) {
        Log-TestResult -TestName "OpenAPI Docs Access" -Method "GET" -Endpoint "/api-docs" -StatusCode 200 -Status "PASS" -Details "OpenAPI documentation is accessible"
    } else {
        Log-TestResult -TestName "OpenAPI Docs Access" -Method "GET" -Endpoint "/api-docs" -StatusCode $response.StatusCode -Status "FAIL" -Details "OpenAPI documentation not accessible"
    }
} catch {
    Log-TestResult -TestName "OpenAPI Docs Access" -Method "GET" -Endpoint "/api-docs" -StatusCode $_.Exception.Response.StatusCode.value__ -Status "FAIL" -Details "Error: $($_.Exception.Message)"
}

# ============ SUMMARY ============

Write-Host "=== TEST SUMMARY ===" -ForegroundColor Green
$passCount = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $testResults.Count

Write-Host "Total Tests: $totalCount" -ForegroundColor White
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor Red

$successRate = if ($totalCount -gt 0) { [math]::Round(($passCount / $totalCount) * 100, 2) } else { 0 }
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 60) { "Yellow" } else { "Red" })

# Generate detailed report
$report = @"
COMPREHENSIVE API TEST REPORT
Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

TEST RESULTS SUMMARY:
==================

Total Tests Executed: $totalCount
Tests Passed: $passCount
Tests Failed: $failCount
Success Rate: $successRate%

DETAILED RESULTS:
================

"@

foreach ($result in $testResults) {
    $report += @"
[$($result.Status)] $($result.TestName)
Method: $($result.Method)
Endpoint: $($result.Endpoint)
Status Code: $($result.StatusCode)
Timestamp: $($result.Timestamp)
$($result.Details)

"@
}

$report | Out-File -FilePath "api_test_report_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt" -Encoding UTF8
Write-Host "Detailed report saved to: api_test_report_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt" -ForegroundColor Cyan

Write-Host "`n=== TEST COMPLETE ===" -ForegroundColor Green