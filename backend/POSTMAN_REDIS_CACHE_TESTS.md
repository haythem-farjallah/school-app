# Postman Redis Cache & Rate Limiting Tests

This document describes the comprehensive Postman collection for testing Redis caching and rate limiting functionality.

## Collection Overview

The collection includes:
- **Authentication Tests**: Rate limiting on login/register endpoints
- **Student Endpoints**: Caching tests for student data
- **Rate Limiting Stress Tests**: Multiple requests to trigger rate limits
- **Cache Management**: Admin endpoints for cache control
- **Cache Performance Tests**: Cache hit/miss performance comparison
- **Rate Limiting Different Endpoints**: Tests for various endpoint categories

## Setup Instructions

### 1. Import Collection and Environment

1. **Import Collection**:
   - Open Postman
   - Click "Import" button
   - Select `redis-cache-rate-limiting.postman_collection.json`

2. **Import Environment**:
   - Click "Import" button
   - Select `redis-cache-test-environment.json`
   - Select the environment in the top-right dropdown

### 2. Start Required Services

```bash
# Start Redis
cd backend
docker-compose -f docker-compose-redis.yml up -d

# Start Application
./mvnw spring-boot:run
```

### 3. Get Authentication Tokens

Before running tests, you need to get authentication tokens:

1. **Get Admin Token**:
   - Run the "Login (Rate Limit Test)" request
   - Copy the `accessToken` from the response
   - Set it in the `ADMIN_TOKEN` environment variable

2. **Get Other Tokens** (optional):
   - Create test users for different roles
   - Login and set their tokens in environment variables

## Test Categories

### 1. Authentication Tests

#### Login (Rate Limit Test)
- **Purpose**: Test rate limiting on authentication endpoints
- **Rate Limit**: 10 requests per minute
- **Pre-request Script**: Clears rate limit data
- **Test Script**: 
  - Checks for rate limiting headers
  - Stores rate limit information
  - Tests for 429 responses when limit exceeded

#### Register (Cache Eviction Test)
- **Purpose**: Test cache eviction on user creation
- **Pre-request Script**: Generates unique email
- **Test Script**: Verifies registration and cache eviction

### 2. Student Endpoints (Caching Tests)

#### Get Student by ID (Cache Test)
- **Purpose**: Test individual student caching
- **Cache Key**: Student ID
- **Test Script**: 
  - Verifies successful retrieval
  - Measures response time
  - Stores response time for comparison

#### Get Student by ID (Cache Hit Test)
- **Purpose**: Test cache hit performance
- **Test Script**:
  - Compares response times
  - Verifies cache hit is faster
  - Measures performance improvement

#### List Students (Pagination Cache Test)
- **Purpose**: Test paginated list caching
- **Cache Key**: Page + size + filters
- **Test Script**: Verifies pagination data and performance

### 3. Rate Limiting Stress Tests

#### Auth Rate Limit Test (10 requests)
- **Purpose**: Trigger rate limiting on auth endpoints
- **Method**: Multiple rapid requests
- **Expected**: Rate limit exceeded after 10 requests
- **Test Script**: 
  - Monitors rate limit headers
  - Detects 429 responses
  - Logs rate limit status

### 4. Cache Management (Admin)

#### Get Cache Statistics
- **Purpose**: Retrieve cache performance statistics
- **Authentication**: Requires admin token
- **Test Script**: Verifies successful retrieval

#### Clear All Caches
- **Purpose**: Clear all cached data
- **Authentication**: Requires admin token
- **Test Script**: Verifies successful cache clearing

#### Clear User Caches
- **Purpose**: Clear user-related caches only
- **Authentication**: Requires admin token
- **Test Script**: Verifies selective cache clearing

#### Clear Listing Caches
- **Purpose**: Clear listing-related caches only
- **Authentication**: Requires admin token
- **Test Script**: Verifies selective cache clearing

#### Clear Specific Cache (Students)
- **Purpose**: Clear specific cache by name
- **Authentication**: Requires admin token
- **Test Script**: Verifies specific cache clearing

### 5. Cache Performance Tests

#### Cache Miss Test (First Request)
- **Purpose**: Measure cache miss performance
- **Method**: Clears caches before request
- **Test Script**: Records response time for comparison

#### Cache Hit Test (Second Request)
- **Purpose**: Measure cache hit performance
- **Method**: Same request as cache miss test
- **Test Script**: 
  - Compares response times
  - Calculates performance improvement
  - Verifies cache hit is faster

### 6. Rate Limiting Different Endpoints

#### API Endpoint Rate Limit Test
- **Purpose**: Test rate limiting on general API endpoints
- **Rate Limit**: 100 requests per minute
- **Test Script**: Monitors rate limit headers

#### Listing Endpoint Rate Limit Test
- **Purpose**: Test rate limiting on listing endpoints
- **Rate Limit**: 200 requests per minute
- **Test Script**: Monitors rate limit headers

## Test Scripts Explained

### Pre-request Scripts

Pre-request scripts run before each request and typically:
- Set up test data
- Clear previous test state
- Generate unique identifiers
- Log test information

### Test Scripts

Test scripts run after each request and typically:
- Verify response codes
- Check response structure
- Measure performance
- Store data for comparison
- Log test results

### Global Scripts

#### Pre-request Script (Collection Level)
- Logs test suite start
- Checks environment variables
- Warns about missing tokens

#### Test Script (Collection Level)
- Verifies reasonable response times
- Logs response details
- Provides common test functionality

## Rate Limiting Test Runner

The `rate-limiting-test-runner.js` file provides utility functions for advanced testing:

### Functions Available

1. **runRateLimitTest(testType, requestCount)**
   - Runs multiple requests to test rate limiting
   - Supports: 'auth', 'api', 'listing'
   - Returns detailed results

2. **runCachePerformanceTest(endpoint, iterations)**
   - Tests cache hit/miss performance
   - Measures response time improvements
   - Returns performance metrics

3. **runCacheEvictionTest(endpoint)**
   - Tests cache eviction functionality
   - Clears cache and measures impact
   - Verifies cache clearing works

### Usage Example

```javascript
// In Postman pre-request or test script
const results = runRateLimitTest('auth', 15);
const cacheResults = runCachePerformanceTest('/api/v1/students/1', 5);
runCacheEvictionTest('/api/v1/students/1');
```

## Expected Results

### Rate Limiting Tests

| Endpoint Type | Rate Limit | Expected Behavior |
|---------------|------------|-------------------|
| Authentication | 10/min | 429 after 10 requests |
| API Endpoints | 100/min | 429 after 100 requests |
| Listing Endpoints | 200/min | 429 after 200 requests |
| Admin Endpoints | 50/min | 429 after 50 requests |
| Upload Endpoints | 20/min | 429 after 20 requests |

### Caching Tests

| Test Type | Expected Result |
|-----------|-----------------|
| Cache Miss | Response time > 100ms |
| Cache Hit | Response time < 200ms |
| Cache Improvement | 50-90% faster response |
| Cache Eviction | Response time returns to cache miss levels |

### Response Headers

All requests should include:
- `X-Rate-Limit-Remaining`: Number of requests remaining
- `X-Rate-Limit-Reset`: Time until rate limit resets

## Troubleshooting

### Common Issues

1. **Tests Failing with 401**
   - Check if `ADMIN_TOKEN` is set
   - Verify token is valid and not expired
   - Login again to get fresh token

2. **Rate Limiting Not Triggering**
   - Check if Redis is running
   - Verify rate limiting filter is active
   - Check application logs for errors

3. **Cache Tests Not Working**
   - Verify Redis connection
   - Check cache configuration
   - Ensure cache annotations are applied

4. **Response Times Too High**
   - Check database performance
   - Verify Redis is responding quickly
   - Check network latency

### Debug Steps

1. **Check Redis Connection**:
   ```bash
   redis-cli ping
   ```

2. **Check Application Logs**:
   ```bash
   tail -f logs/application.log | grep -E "(cache|rate|redis)"
   ```

3. **Verify Environment Variables**:
   - Check all required variables are set
   - Verify HOST points to correct URL
   - Ensure tokens are valid

## Advanced Testing

### Custom Test Scenarios

1. **Load Testing**:
   - Use Postman Runner to run tests multiple times
   - Monitor rate limiting under load
   - Test cache performance under stress

2. **Cache Invalidation Testing**:
   - Create/update data
   - Verify cache is invalidated
   - Test cache refresh behavior

3. **Multi-User Testing**:
   - Test with different user roles
   - Verify role-based rate limits
   - Test cache isolation

### Performance Monitoring

Monitor these metrics during tests:
- Response times
- Cache hit ratios
- Rate limit effectiveness
- Memory usage
- Redis performance

## Integration with CI/CD

The collection can be integrated with CI/CD pipelines:

1. **Newman CLI**:
   ```bash
   newman run redis-cache-rate-limiting.postman_collection.json -e redis-cache-test-environment.json
   ```

2. **Automated Testing**:
   - Run tests after deployments
   - Monitor performance regressions
   - Alert on rate limiting failures

This comprehensive test suite ensures your Redis caching and rate limiting implementation works correctly and performs well under various conditions.
