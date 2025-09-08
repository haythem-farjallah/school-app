# Test script for Redis caching and rate limiting
# Make sure Redis is running and the application is started

Write-Host "Testing Redis Caching and Rate Limiting Implementation" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green

$BASE_URL = "http://localhost:8088"
$REDIS_URL = "localhost:6379"

# Check if Redis is running
Write-Host "1. Checking Redis connection..." -ForegroundColor Yellow
try {
    $redisTest = redis-cli -h $REDIS_URL ping 2>$null
    if ($redisTest -eq "PONG") {
        Write-Host "✅ Redis is running" -ForegroundColor Green
    } else {
        throw "Redis not responding"
    }
} catch {
    Write-Host "❌ Redis is not running. Please start Redis first:" -ForegroundColor Red
    Write-Host "   docker-compose -f docker-compose-redis.yml up -d" -ForegroundColor Yellow
    exit 1
}

# Check if application is running
Write-Host "2. Checking application health..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-WebRequest -Uri "$BASE_URL/actuator/health" -UseBasicParsing -TimeoutSec 5
    if ($healthResponse.StatusCode -eq 200) {
        Write-Host "✅ Application is running" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Application is not running. Please start the application first." -ForegroundColor Red
    exit 1
}

# Test rate limiting on auth endpoint
Write-Host "3. Testing rate limiting on authentication endpoint..." -ForegroundColor Yellow
Write-Host "   Making 15 requests to /api/auth/login (limit: 10/minute)..." -ForegroundColor Cyan

$rateLimitTriggered = $false
for ($i = 1; $i -le 15; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "$BASE_URL/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"test@example.com","password":"test"}' -UseBasicParsing -TimeoutSec 5
        Write-Host "   Request $i`: HTTP $($response.StatusCode)" -ForegroundColor Gray
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 429) {
            Write-Host "   ✅ Rate limit triggered at request $i (HTTP 429)" -ForegroundColor Green
            $rateLimitTriggered = $true
            break
        } else {
            Write-Host "   Request $i`: HTTP $statusCode" -ForegroundColor Gray
        }
    }
}

if (-not $rateLimitTriggered) {
    Write-Host "   ⚠️  Rate limit not triggered after 15 requests" -ForegroundColor Yellow
}

# Test cache management endpoints (requires admin token)
Write-Host "4. Testing cache management endpoints..." -ForegroundColor Yellow
Write-Host "   Note: These require admin authentication" -ForegroundColor Cyan

# Test cache stats endpoint
Write-Host "   Testing cache stats endpoint..." -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/api/v1/admin/cache/stats" -UseBasicParsing -TimeoutSec 5
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 401) {
        Write-Host "   ✅ Cache stats endpoint requires authentication (HTTP 401)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Unexpected response: HTTP $statusCode" -ForegroundColor Yellow
    }
}

# Test Redis keys
Write-Host "5. Checking Redis keys..." -ForegroundColor Yellow
try {
    $keys = redis-cli -h $REDIS_URL keys "*" 2>$null
    $keyCount = ($keys | Measure-Object).Count
    Write-Host "   Found $keyCount keys in Redis" -ForegroundColor Green
    
    if ($keyCount -gt 0) {
        Write-Host "   Sample Redis keys:" -ForegroundColor Cyan
        $keys | Select-Object -First 5 | ForEach-Object { Write-Host "     $_" -ForegroundColor Gray }
    }
} catch {
    Write-Host "   ⚠️  Could not retrieve Redis keys" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Test completed!" -ForegroundColor Green
Write-Host ""
Write-Host "To test with authentication:" -ForegroundColor Cyan
Write-Host "1. Register/login to get a JWT token" -ForegroundColor White
Write-Host "2. Use the token in Authorization header: 'Bearer YOUR_TOKEN'" -ForegroundColor White
Write-Host "3. Test cache management endpoints" -ForegroundColor White
Write-Host ""
Write-Host "Example:" -ForegroundColor Cyan
Write-Host "Invoke-WebRequest -Uri '$BASE_URL/api/v1/admin/cache/stats' -Headers @{'Authorization'='Bearer YOUR_TOKEN'}" -ForegroundColor White
