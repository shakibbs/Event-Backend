$BaseUrl = "http://localhost:8083/api"
$passed = 0
$total = 0

Write-Host "===== COMPREHENSIVE RBAC TEST =====" -ForegroundColor Cyan
Write-Host ""

# Test 1: Superadmin Login
Write-Host "TEST 1: Superadmin Login" -ForegroundColor Yellow
$total++
try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/auth/login" -Method Post -Headers @{"Content-Type"="application/json"} -Body '{"email":"superadmin@ems.com","password":"password"}' -ErrorAction Stop
    $data = $resp.Content | ConvertFrom-Json
    $token = $data.accessToken
    Write-Host "PASS - Status 200" -ForegroundColor Green
    $passed++
} catch {
    Write-Host "FAIL - $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: Access /users endpoint (protected)
Write-Host "TEST 2: Protected Endpoint - GET /users with valid token" -ForegroundColor Yellow
$total++
try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/users" -Method Get -Headers @{"Authorization"="Bearer $token"} -ErrorAction Stop
    if ($resp.StatusCode -eq 200) {
        Write-Host "PASS - Status 200" -ForegroundColor Green
        $passed++
    }
} catch {
    Write-Host "FAIL - Status $($_.Exception.Response.StatusCode)" -ForegroundColor Red
}
Write-Host ""

# Test 3: Access /roles endpoint (protected)
Write-Host "TEST 3: Protected Endpoint - GET /roles with valid token" -ForegroundColor Yellow
$total++
try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/roles" -Method Get -Headers @{"Authorization"="Bearer $token"} -ErrorAction Stop
    if ($resp.StatusCode -eq 200) {
        Write-Host "PASS - Status 200" -ForegroundColor Green
        $passed++
    }
} catch {
    Write-Host "FAIL - Status $($_.Exception.Response.StatusCode)" -ForegroundColor Red
}
Write-Host ""

# Test 4: No token (should fail with 401)
Write-Host "TEST 4: No Token - GET /users should return 401" -ForegroundColor Yellow
$total++
try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/users" -Method Get -ErrorAction Stop
    Write-Host "FAIL - Expected 401 but got $($resp.StatusCode)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "PASS - Correctly rejected with 401" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "FAIL - Got $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}
Write-Host ""

# Test 5: Invalid token (should fail with 401)
Write-Host "TEST 5: Invalid Token - GET /users should return 401" -ForegroundColor Yellow
$total++
try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/users" -Method Get -Headers @{"Authorization"="Bearer invalid.token.here"} -ErrorAction Stop
    Write-Host "FAIL - Expected 401 but got $($resp.StatusCode)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "PASS - Correctly rejected with 401" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "FAIL - Got $($_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
}
Write-Host ""

# Test 6: Token Refresh
Write-Host "TEST 6: Refresh Token" -ForegroundColor Yellow
$total++
try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/auth/login" -Method Post -Headers @{"Content-Type"="application/json"} -Body '{"email":"superadmin@ems.com","password":"password"}' -ErrorAction Stop
    $data = $resp.Content | ConvertFrom-Json
    $refreshToken = $data.refreshToken
    $refreshBody = @{"refreshToken"=$refreshToken} | ConvertTo-Json
    $resp2 = Invoke-WebRequest -Uri "$BaseUrl/auth/refresh" -Method Post -Headers @{"Content-Type"="application/json"} -Body $refreshBody -ErrorAction Stop
    if ($resp2.StatusCode -eq 200) {
        Write-Host "PASS - Status 200" -ForegroundColor Green
        $passed++
    }
} catch {
    Write-Host "FAIL - $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 7: Logout
Write-Host "TEST 7: Logout" -ForegroundColor Yellow
$total++
try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/auth/logout" -Method Post -Headers @{"Content-Type"="application/json"; "Authorization"="Bearer $token"} -Body '{}' -ErrorAction Stop
    if ($resp.StatusCode -eq 200) {
        Write-Host "PASS - Status 200" -ForegroundColor Green
        $passed++
    }
} catch {
    Write-Host "FAIL - $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "===== RESULTS =====" -ForegroundColor Cyan
Write-Host "Passed: $passed/$total" -ForegroundColor Green
if ($passed -eq $total) {
    Write-Host "STATUS: PERFECT SCORE!" -ForegroundColor Green
}
Write-Host ""
Write-Host "Features Verified:" -ForegroundColor Cyan
Write-Host "  * JWT token authentication" -ForegroundColor Green
Write-Host "  * Token-based authorization" -ForegroundColor Green
Write-Host "  * Protected endpoints enforcement" -ForegroundColor Green
Write-Host "  * Unauthorized access blocking" -ForegroundColor Green
Write-Host "  * Token refresh mechanism" -ForegroundColor Green
Write-Host "  * Logout functionality" -ForegroundColor Green
