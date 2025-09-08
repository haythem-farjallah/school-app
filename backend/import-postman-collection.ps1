# PowerShell script to help import Postman collection and environment
# This script provides instructions and validation for importing the Redis cache tests

Write-Host "Postman Collection Import Helper" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green

# Check if files exist
$collectionFile = "redis-cache-rate-limiting.postman_collection.json"
$environmentFile = "redis-cache-test-environment.json"

Write-Host "`nChecking files..." -ForegroundColor Yellow

if (Test-Path $collectionFile) {
    Write-Host "✅ Collection file found: $collectionFile" -ForegroundColor Green
} else {
    Write-Host "❌ Collection file not found: $collectionFile" -ForegroundColor Red
    exit 1
}

if (Test-Path $environmentFile) {
    Write-Host "✅ Environment file found: $environmentFile" -ForegroundColor Green
} else {
    Write-Host "❌ Environment file not found: $environmentFile" -ForegroundColor Red
    exit 1
}

Write-Host "`nImport Instructions:" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan

Write-Host "`n1. Open Postman" -ForegroundColor White
Write-Host "2. Click the 'Import' button (top-left)" -ForegroundColor White
Write-Host "3. Select 'Upload Files'" -ForegroundColor White
Write-Host "4. Choose both files:" -ForegroundColor White
Write-Host "   - $collectionFile" -ForegroundColor Gray
Write-Host "   - $environmentFile" -ForegroundColor Gray
Write-Host "5. Click 'Import'" -ForegroundColor White

Write-Host "`n6. Set up Environment:" -ForegroundColor White
Write-Host "   - Select 'Redis Cache & Rate Limiting Test Environment' from dropdown" -ForegroundColor Gray
Write-Host "   - Update HOST variable if needed (default: http://localhost:8088)" -ForegroundColor Gray

Write-Host "`n7. Get Authentication Tokens:" -ForegroundColor White
Write-Host "   - Run 'Login (Rate Limit Test)' request" -ForegroundColor Gray
Write-Host "   - Copy accessToken from response" -ForegroundColor Gray
Write-Host "   - Set ADMIN_TOKEN environment variable" -ForegroundColor Gray

Write-Host "`nTest Categories Available:" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan

Write-Host "`n📋 Authentication Tests" -ForegroundColor Yellow
Write-Host "   - Login rate limiting" -ForegroundColor Gray
Write-Host "   - Registration cache eviction" -ForegroundColor Gray

Write-Host "`n👥 Student Endpoints (Caching Tests)" -ForegroundColor Yellow
Write-Host "   - Individual student caching" -ForegroundColor Gray
Write-Host "   - Cache hit performance" -ForegroundColor Gray
Write-Host "   - Paginated list caching" -ForegroundColor Gray

Write-Host "`n⚡ Rate Limiting Stress Tests" -ForegroundColor Yellow
Write-Host "   - Multiple rapid requests" -ForegroundColor Gray
Write-Host "   - Rate limit triggering" -ForegroundColor Gray

Write-Host "`n🔧 Cache Management (Admin)" -ForegroundColor Yellow
Write-Host "   - Cache statistics" -ForegroundColor Gray
Write-Host "   - Clear all caches" -ForegroundColor Gray
Write-Host "   - Selective cache clearing" -ForegroundColor Gray

Write-Host "`n📊 Cache Performance Tests" -ForegroundColor Yellow
Write-Host "   - Cache miss vs hit comparison" -ForegroundColor Gray
Write-Host "   - Performance improvement measurement" -ForegroundColor Gray

Write-Host "`n🎯 Rate Limiting Different Endpoints" -ForegroundColor Yellow
Write-Host "   - API endpoint limits" -ForegroundColor Gray
Write-Host "   - Listing endpoint limits" -ForegroundColor Gray

Write-Host "`nPrerequisites:" -ForegroundColor Cyan
Write-Host "==============" -ForegroundColor Cyan

Write-Host "`n✅ Redis server running (docker-compose -f docker-compose-redis.yml up -d)" -ForegroundColor Green
Write-Host "✅ Spring Boot application running (./mvnw spring-boot:run)" -ForegroundColor Green
Write-Host "✅ Admin user account exists" -ForegroundColor Green

Write-Host "`nQuick Start:" -ForegroundColor Cyan
Write-Host "============" -ForegroundColor Cyan

Write-Host "`n1. Start services:" -ForegroundColor White
Write-Host "   docker-compose -f docker-compose-redis.yml up -d" -ForegroundColor Gray
Write-Host "   ./mvnw spring-boot:run" -ForegroundColor Gray

Write-Host "`n2. Import collection and environment into Postman" -ForegroundColor White

Write-Host "`n3. Run 'Login (Rate Limit Test)' to get admin token" -ForegroundColor White

Write-Host "`n4. Set ADMIN_TOKEN in environment variables" -ForegroundColor White

Write-Host "`n5. Run test collection or individual tests" -ForegroundColor White

Write-Host "`nDocumentation:" -ForegroundColor Cyan
Write-Host "=============" -ForegroundColor Cyan
Write-Host "📖 See POSTMAN_REDIS_CACHE_TESTS.md for detailed documentation" -ForegroundColor White

Write-Host "`nHappy Testing! 🚀" -ForegroundColor Green
