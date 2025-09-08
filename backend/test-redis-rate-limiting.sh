#!/bin/bash

# Test script for Redis caching and rate limiting
# Make sure Redis is running and the application is started

echo "Testing Redis Caching and Rate Limiting Implementation"
echo "======================================================"

BASE_URL="http://localhost:8088"
REDIS_URL="localhost:6379"

# Check if Redis is running
echo "1. Checking Redis connection..."
if redis-cli -h $REDIS_URL ping > /dev/null 2>&1; then
    echo "✅ Redis is running"
else
    echo "❌ Redis is not running. Please start Redis first:"
    echo "   docker-compose -f docker-compose-redis.yml up -d"
    exit 1
fi

# Check if application is running
echo "2. Checking application health..."
if curl -s "$BASE_URL/actuator/health" > /dev/null; then
    echo "✅ Application is running"
else
    echo "❌ Application is not running. Please start the application first."
    exit 1
fi

# Test rate limiting on auth endpoint
echo "3. Testing rate limiting on authentication endpoint..."
echo "   Making 15 requests to /api/auth/login (limit: 10/minute)..."

for i in {1..15}; do
    response=$(curl -s -w "%{http_code}" -o /dev/null -X POST "$BASE_URL/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"test@example.com","password":"test"}')
    
    if [ "$response" = "429" ]; then
        echo "   ✅ Rate limit triggered at request $i (HTTP 429)"
        break
    elif [ "$i" -eq 15 ]; then
        echo "   ⚠️  Rate limit not triggered after 15 requests"
    fi
done

# Test cache management endpoints (requires admin token)
echo "4. Testing cache management endpoints..."
echo "   Note: These require admin authentication"

# Test cache stats endpoint
echo "   Testing cache stats endpoint..."
response=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/api/v1/admin/cache/stats")
if [ "$response" = "401" ]; then
    echo "   ✅ Cache stats endpoint requires authentication (HTTP 401)"
else
    echo "   ⚠️  Unexpected response: HTTP $response"
fi

# Test Redis keys
echo "5. Checking Redis keys..."
keys=$(redis-cli -h $REDIS_URL keys "*" | wc -l)
echo "   Found $keys keys in Redis"

# Show some Redis keys
echo "   Sample Redis keys:"
redis-cli -h $REDIS_URL keys "*" | head -5

echo ""
echo "Test completed!"
echo ""
echo "To test with authentication:"
echo "1. Register/login to get a JWT token"
echo "2. Use the token in Authorization header: 'Bearer YOUR_TOKEN'"
echo "3. Test cache management endpoints"
echo ""
echo "Example:"
echo "curl -H 'Authorization: Bearer YOUR_TOKEN' $BASE_URL/api/v1/admin/cache/stats"
