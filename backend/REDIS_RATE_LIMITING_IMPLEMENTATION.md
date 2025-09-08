# Redis Caching and Rate Limiting Implementation

This document describes the Redis caching and rate limiting implementation for the School Management System.

## Overview

The application now includes:
- **Redis Caching**: For improved performance and reduced database load
- **Rate Limiting**: To prevent abuse and ensure fair usage
- **Cache Management**: Administrative tools for cache control

## Features

### 1. Redis Caching
- **User Data Caching**: Caches user details, authentication data
- **Listing Caching**: Caches paginated lists of students, teachers, classes, etc.
- **Configurable TTL**: Different cache expiration times for different data types
- **Cache Eviction**: Automatic cache invalidation on data updates

### 2. Rate Limiting
- **Endpoint-based Limits**: Different rate limits for different endpoint types
- **IP-based Tracking**: Rate limiting per client IP address
- **Distributed Rate Limiting**: Uses Redis for distributed rate limiting across multiple instances
- **Configurable Limits**: Easy to adjust rate limits for different endpoint categories

### 3. Cache Management
- **Administrative Endpoints**: Clear caches programmatically
- **Selective Cache Clearing**: Clear specific caches or all caches
- **Cache Statistics**: Monitor cache usage and performance

## Rate Limiting Configuration

### Endpoint Categories and Limits

| Category | Endpoints | Rate Limit | Refill Rate |
|----------|-----------|------------|-------------|
| **Authentication** | `/api/auth/*` | 10 requests | 10 per minute |
| **Admin Operations** | `/api/v1/admins/*`, DELETE operations | 50 requests | 50 per minute |
| **File Uploads** | `/upload`, `/export`, `/import` | 20 requests | 20 per minute |
| **Listings** | GET requests to list endpoints | 200 requests | 200 per minute |
| **General API** | All other endpoints | 100 requests | 100 per minute |

### Rate Limiting Headers

The API returns the following headers for rate limiting information:
- `X-Rate-Limit-Remaining`: Number of requests remaining in the current window
- `X-Rate-Limit-Reset`: Time in seconds until the rate limit resets

## Cache Configuration

### Cache Categories and TTL

| Cache Name | Data Type | TTL | Description |
|------------|-----------|-----|-------------|
| `auth` | Authentication data | 15 minutes | User authentication and session data |
| `users` | User details | 60 minutes | Individual user information |
| `students` | Student data | 60 minutes | Student records and lists |
| `teachers` | Teacher data | 60 minutes | Teacher records and lists |
| `staff` | Staff data | 60 minutes | Staff records and lists |
| `admins` | Admin data | 60 minutes | Admin records and lists |
| `classes` | Class data | 10 minutes | Class information and lists |
| `courses` | Course data | 10 minutes | Course information and lists |
| `timetables` | Timetable data | 5 minutes | Timetable information |
| `announcements` | Announcements | 10 minutes | Announcement lists |
| `resources` | Learning resources | 10 minutes | Resource lists |
| `grades` | Grade data | 10 minutes | Grade information |

## Setup Instructions

### 1. Start Redis Server

#### Option A: Using Docker Compose (Recommended)
```bash
cd backend
docker-compose -f docker-compose-redis.yml up -d
```

#### Option B: Using Docker directly
```bash
docker run -d --name school-management-redis -p 6379:6379 redis:7-alpine
```

#### Option C: Install Redis locally
```bash
# Ubuntu/Debian
sudo apt-get install redis-server

# macOS
brew install redis

# Start Redis
redis-server
```

### 2. Update Application Properties

The Redis configuration is already added to `application.properties`:

```properties
# Redis connection settings
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.database=0
spring.data.redis.timeout=2000ms

# Cache configuration
spring.cache.type=redis
spring.cache.redis.time-to-live=1800000
spring.cache.redis.cache-null-values=false
spring.cache.redis.enable-statistics=true
```

### 3. Start the Application

```bash
cd backend
./mvnw spring-boot:run
```

## API Endpoints

### Cache Management (Admin Only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/admin/cache/clear-all` | Clear all caches |
| POST | `/api/v1/admin/cache/clear-users` | Clear user-related caches |
| POST | `/api/v1/admin/cache/clear-listings` | Clear listing caches |
| POST | `/api/v1/admin/cache/clear/{cacheName}` | Clear specific cache |
| POST | `/api/v1/admin/cache/clear/{cacheName}/{key}` | Clear specific cache entry |
| GET | `/api/v1/admin/cache/stats` | Get cache statistics |

### Example Usage

```bash
# Clear all caches
curl -X POST "http://localhost:8088/api/v1/admin/cache/clear-all" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Clear student cache
curl -X POST "http://localhost:8088/api/v1/admin/cache/clear/students" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Get cache statistics
curl -X GET "http://localhost:8088/api/v1/admin/cache/stats" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Monitoring and Troubleshooting

### 1. Redis Monitoring

Connect to Redis CLI to monitor:
```bash
redis-cli
> INFO
> MONITOR
> KEYS *
```

### 2. Application Logs

Monitor application logs for cache and rate limiting information:
```bash
tail -f logs/application.log | grep -E "(cache|rate|redis)"
```

### 3. Health Checks

Check Redis connectivity:
```bash
curl http://localhost:8088/actuator/health
```

## Performance Benefits

### Caching Benefits
- **Reduced Database Load**: Frequently accessed data is served from cache
- **Faster Response Times**: Cache hits are significantly faster than database queries
- **Improved Scalability**: Reduced database connections and queries

### Rate Limiting Benefits
- **DoS Protection**: Prevents abuse and ensures service availability
- **Fair Usage**: Ensures all users get fair access to resources
- **Resource Protection**: Prevents overwhelming of backend services

## Configuration Tuning

### Adjusting Rate Limits

Edit `RateLimitingConfig.java` to modify rate limits:

```java
// Example: Increase API rate limit to 200 requests per minute
public static final Bandwidth API_BANDWIDTH = Bandwidth.classic(200, Refill.intervally(200, Duration.ofMinutes(1)));
```

### Adjusting Cache TTL

Edit `RedisConfig.java` to modify cache expiration:

```java
// Example: Increase user cache TTL to 2 hours
RedisCacheConfiguration userCacheConfig = defaultCacheConfig.entryTtl(Duration.ofHours(2));
```

### Redis Connection Pool

Adjust connection pool settings in `application.properties`:

```properties
spring.data.redis.lettuce.pool.max-active=16
spring.data.redis.lettuce.pool.max-idle=8
spring.data.redis.lettuce.pool.min-idle=2
```

## Security Considerations

1. **Redis Security**: Ensure Redis is not exposed to the public internet
2. **Rate Limiting**: Monitor for abuse patterns and adjust limits accordingly
3. **Cache Security**: Sensitive data should have shorter TTL or be excluded from cache
4. **Admin Access**: Cache management endpoints are restricted to ADMIN role only

## Troubleshooting

### Common Issues

1. **Redis Connection Failed**
   - Check if Redis server is running
   - Verify connection settings in application.properties
   - Check firewall settings

2. **Rate Limiting Too Strict**
   - Adjust rate limits in RateLimitingConfig.java
   - Consider different limits for different user roles

3. **Cache Not Working**
   - Check Redis connectivity
   - Verify cache annotations are properly applied
   - Check cache configuration

4. **Memory Issues**
   - Monitor Redis memory usage
   - Adjust cache TTL settings
   - Consider cache size limits

## Future Enhancements

1. **Cache Warming**: Pre-populate frequently accessed data
2. **Advanced Rate Limiting**: User-based rate limiting in addition to IP-based
3. **Cache Analytics**: Detailed cache hit/miss statistics
4. **Distributed Caching**: Multi-node Redis cluster for high availability
5. **Cache Compression**: Compress cached data to save memory
