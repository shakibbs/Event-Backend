# Comprehensive RBAC Test - Final Version
# This script tests the complete RBAC functionality including:
# - User creation by superadmin
# - Authentication for all user types
# - Role-based access control
# - Permission enforcement

$BaseUrl = "http://localhost:8083/api"
$TestResults = @()
$AdminToken = ""
$UserToken = ""
$OrganizerToken = ""
$NewAdminId = 0
$NewUserId = 0
$NewOrganizerId = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Endpoint,
        [string]$Token = "",
        [string]$Body = "",
        [int[]]$ExpectedStatusCodes = @(200, 201)
    )
    
    $headers = @{"Content-Type" = "application/json"}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    
    try {
        $params = @{
            Uri             = "$BaseUrl$Endpoint"
            Method          = $Method
            Headers         = $headers
            ErrorAction     = "SilentlyContinue"
            UseBasicParsing = $true
        }
        if ($Body) { $params["Body"] = $Body }
        
        $response = Invoke-WebRequest @params
        $statusCode = $response.StatusCode
        $passed = $statusCode -in $ExpectedStatusCodes
        
        $TestResults += [PSCustomObject]@{
            Name       = $Name
            Endpoint   = $Endpoint
            Method     = $Method
            Status     = if ($passed) { "✅ PASS" } else { "❌ FAIL" }
            StatusCode = $statusCode
            Expected   = ($ExpectedStatusCodes -join ", ")
            Response   = if ($response.Content.Length -lt 200) { $response.Content } else { $response.Content.Substring(0, 100) + "..." }
        }
        
        return $response
    }
    catch {
        $TestResults += [PSCustomObject]@{
            Name       = $Name
            Endpoint   = $Endpoint
            Method     = $Method
            Status     = "❌ FAIL"
            StatusCode = $_.Exception.Response.StatusCode
            Expected   = ($ExpectedStatusCodes -join ", ")
            Response   = $_.Exception.Message
        }
        return $null
    }
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "COMPREHENSIVE RBAC TEST SUITE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Phase 1: Superadmin Authentication
Write-Host "PHASE 1: Superadmin Authentication" -ForegroundColor Yellow
$response = Test-Endpoint "Superadmin Login" "POST" "/auth/login" "" '{"email":"superadmin@ems.com","password":"password"}' @(200)
if ($response) {
    $data = $response.Content | ConvertFrom-Json
    $AdminToken = $data.accessToken
    Write-Host "  ✅ Superadmin authenticated, token obtained" -ForegroundColor Green
}
Write-Host ""

# Phase 2: Create Test Users
Write-Host "PHASE 2: Create Test Users (by superadmin)" -ForegroundColor Yellow

# Create ADMIN user
$adminBody = @{
    email    = "admin@ems.com"
    password = "admin123"
    full_name = "Test Admin"
    role_id  = 320
} | ConvertTo-Json

$response = Test-Endpoint "Create Admin User" "POST" "/users" $AdminToken $adminBody @(201)
if ($response) {
    $data = $response.Content | ConvertFrom-Json
    $NewAdminId = $data.id
    Write-Host "  ✅ Admin user created (ID: $NewAdminId)" -ForegroundColor Green
}

# Create USER (Attendee)
$userBody = @{
    email    = "user@ems.com"
    password = "user123"
    full_name = "Test Attendee"
    role_id  = 321
} | ConvertTo-Json

$response = Test-Endpoint "Create Attendee User" "POST" "/users" $AdminToken $userBody @(201)
if ($response) {
    $data = $response.Content | ConvertFrom-Json
    $NewUserId = $data.id
    Write-Host "  ✅ Attendee user created (ID: $NewUserId)" -ForegroundColor Green
}

# Create ORGANIZER user
$organizerBody = @{
    email    = "organizer@ems.com"
    password = "org123"
    full_name = "Test Organizer"
    role_id  = 322
} | ConvertTo-Json

$response = Test-Endpoint "Create Organizer User" "POST" "/users" $AdminToken $organizerBody @(201)
if ($response) {
    $data = $response.Content | ConvertFrom-Json
    $NewOrganizerId = $data.id
    Write-Host "  ✅ Organizer user created (ID: $NewOrganizerId)" -ForegroundColor Green
}
Write-Host ""

# Phase 3: Authenticate Created Users
Write-Host "PHASE 3: Authenticate Created Users" -ForegroundColor Yellow

$response = Test-Endpoint "Admin User Login" "POST" "/auth/login" "" '{"email":"admin@ems.com","password":"admin123"}' @(200)
if ($response) {
    $data = $response.Content | ConvertFrom-Json
    $AdminToken = $data.accessToken
    Write-Host "  ✅ Admin user authenticated" -ForegroundColor Green
}

$response = Test-Endpoint "Attendee User Login" "POST" "/auth/login" "" '{"email":"user@ems.com","password":"user123"}' @(200)
if ($response) {
    $data = $response.Content | ConvertFrom-Json
    $UserToken = $data.accessToken
    Write-Host "  ✅ Attendee user authenticated" -ForegroundColor Green
}

$response = Test-Endpoint "Organizer User Login" "POST" "/auth/login" "" '{"email":"organizer@ems.com","password":"org123"}' @(200)
if ($response) {
    $data = $response.Content | ConvertFrom-Json
    $OrganizerToken = $data.accessToken
    Write-Host "  ✅ Organizer user authenticated" -ForegroundColor Green
}
Write-Host ""

# Phase 4: Test RBAC - User Management
Write-Host "PHASE 4: RBAC - User Management (Admin permissions)" -ForegroundColor Yellow
Write-Host "  Testing GET /users with different tokens:" -ForegroundColor Cyan

# Admin should be able to read users
$response = Test-Endpoint "Admin - GET /users" "GET" "/users" $AdminToken "" @(200)
if ($response) {
    Write-Host "    ✅ Admin CAN read users (expected)" -ForegroundColor Green
}

# User (attendee) should NOT be able to read users
$response = Test-Endpoint "Attendee - GET /users" "GET" "/users" $UserToken "" @(403)
if ($response.StatusCode -eq 403) {
    Write-Host "    ✅ Attendee CANNOT read users (expected permission denied)" -ForegroundColor Green
}

# Organizer should NOT be able to read users
$response = Test-Endpoint "Organizer - GET /users" "GET" "/users" $OrganizerToken "" @(403)
if ($response.StatusCode -eq 403) {
    Write-Host "    ✅ Organizer CANNOT read users (expected permission denied)" -ForegroundColor Green
}
Write-Host ""

# Phase 5: Test RBAC - Event Management
Write-Host "PHASE 5: RBAC - Event Management" -ForegroundColor Yellow

# Create an event (Admin should be able to)
$eventBody = @{
    name        = "Test Event"
    description = "Test event for RBAC"
    event_date  = (Get-Date).AddDays(5).ToString("yyyy-MM-dd HH:mm:ss")
    location    = "Test Location"
    max_capacity = 100
    visibility  = "PUBLIC"
} | ConvertTo-Json

$response = Test-Endpoint "Admin - POST /events" "POST" "/events" $AdminToken $eventBody @(201)
$eventId = 0
if ($response) {
    $data = $response.Content | ConvertFrom-Json
    $eventId = $data.id
    Write-Host "  ✅ Admin CAN create events (ID: $eventId)" -ForegroundColor Green
}

# Attendee tries to create event (should fail)
$response = Test-Endpoint "Attendee - POST /events" "POST" "/events" $UserToken $eventBody @(403)
if ($response.StatusCode -eq 403) {
    Write-Host "  ✅ Attendee CANNOT create events (expected permission denied)" -ForegroundColor Green
}

# Test READ event
$response = Test-Endpoint "Attendee - GET /events/$eventId" "GET" "/events/$eventId" $UserToken "" @(200)
if ($response.StatusCode -eq 200) {
    Write-Host "  ✅ Attendee CAN read events (public data allowed)" -ForegroundColor Green
}
Write-Host ""

# Phase 6: Test RBAC - Role Management
Write-Host "PHASE 6: RBAC - Role Management (Admin only)" -ForegroundColor Yellow

# Admin should be able to read roles
$response = Test-Endpoint "Admin - GET /roles" "GET" "/roles" $AdminToken "" @(200)
if ($response.StatusCode -eq 200) {
    Write-Host "  ✅ Admin CAN read roles" -ForegroundColor Green
}

# Attendee should NOT be able to read roles
$response = Test-Endpoint "Attendee - GET /roles" "GET" "/roles" $UserToken "" @(403)
if ($response.StatusCode -eq 403) {
    Write-Host "  ✅ Attendee CANNOT read roles (permission denied)" -ForegroundColor Green
}
Write-Host ""

# Phase 7: Test Token Management
Write-Host "PHASE 7: Token Management (Refresh & Logout)" -ForegroundColor Yellow

# First, get a token to refresh
$response = Test-Endpoint "Admin - Refresh Token" "POST" "/auth/refresh" $AdminToken "" @(200)
if ($response.StatusCode -eq 200) {
    Write-Host "  ✅ Admin token refreshed successfully" -ForegroundColor Green
}

# Test logout
$response = Test-Endpoint "Admin - Logout" "POST" "/auth/logout" $AdminToken "" @(200)
if ($response.StatusCode -eq 200) {
    Write-Host "  ✅ Admin logged out successfully" -ForegroundColor Green
}
Write-Host ""

# Phase 8: Test Invalid Scenarios
Write-Host "PHASE 8: Invalid Scenarios & Error Handling" -ForegroundColor Yellow

# Invalid email format login
$response = Test-Endpoint "Invalid Email Login" "POST" "/auth/login" "" '{"email":"invalid","password":"test"}' @(400, 401)
if ($response.StatusCode -in @(400, 401)) {
    Write-Host "  ✅ Invalid email rejected (400 or 401)" -ForegroundColor Green
}

# Access protected endpoint without token
$response = Test-Endpoint "No Token - GET /users" "GET" "/users" "" "" @(401)
if ($response.StatusCode -eq 401) {
    Write-Host "  ✅ Protected endpoint requires authentication (401)" -ForegroundColor Green
}

# Invalid token
$response = Test-Endpoint "Invalid Token - GET /users" "GET" "/users" "invalid.token.here" "" @(401, 403)
if ($response.StatusCode -in @(401, 403)) {
    Write-Host "  ✅ Invalid token rejected" -ForegroundColor Green
}
Write-Host ""

# Summary Report
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY REPORT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$passCount = ($TestResults | Where-Object { $_.Status -eq "✅ PASS" }).Count
$failCount = ($TestResults | Where-Object { $_.Status -eq "❌ FAIL" }).Count
$totalCount = $TestResults.Count
$passPercentage = if ($totalCount -gt 0) { [math]::Round(($passCount / $totalCount) * 100, 2) } else { 0 }

Write-Host "Total Tests: $totalCount"
Write-Host "Passed: $passCount ✅" -ForegroundColor Green
Write-Host "Failed: $failCount ❌" -ForegroundColor Red
Write-Host "Pass Rate: $passPercentage%"
Write-Host ""

Write-Host "Detailed Results:" -ForegroundColor Cyan
$TestResults | Format-Table -AutoSize -Property Name, Status, Endpoint, Method, StatusCode, Expected

# Generate report file
$reportPath = "c:\Users\Shakib\IdeaProjects\event_management_system\RBAC_TEST_REPORT_FINAL.txt"
$report = @"
================================================
COMPREHENSIVE RBAC TEST REPORT
Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
================================================

SUMMARY:
- Total Tests: $totalCount
- Passed: $passCount
- Failed: $failCount
- Pass Rate: $passPercentage%

TEST DETAILS:
$(($TestResults | Format-Table -AutoSize -Property Name, Status, Endpoint, Method, StatusCode, Expected | Out-String))

CONCLUSION:
$(if ($failCount -eq 0) { "ALL TESTS PASSED! ✅ RBAC system is working correctly." } else { "Some tests failed. Review details above." })
"@

$report | Out-File -FilePath $reportPath -Force
Write-Host "Report saved to: $reportPath" -ForegroundColor Green
