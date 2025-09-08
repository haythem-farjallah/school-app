/**
 * Rate Limiting Test Runner for Postman
 * This script can be used to run multiple requests to test rate limiting
 */

// Rate limiting test configuration
const RATE_LIMIT_TESTS = {
    auth: {
        endpoint: '/api/auth/login',
        method: 'POST',
        limit: 10,
        body: {
            email: 'test@example.com',
            password: 'wrongpassword'
        }
    },
    api: {
        endpoint: '/api/v1/teachers',
        method: 'GET',
        limit: 100,
        headers: {
            'Authorization': 'Bearer {{ADMIN_TOKEN}}'
        }
    },
    listing: {
        endpoint: '/api/v1/students?page=0&size=10',
        method: 'GET',
        limit: 200,
        headers: {
            'Authorization': 'Bearer {{ADMIN_TOKEN}}'
        }
    }
};

/**
 * Run rate limiting test for a specific endpoint
 * @param {string} testType - Type of test (auth, api, listing)
 * @param {number} requestCount - Number of requests to make
 */
function runRateLimitTest(testType, requestCount = 15) {
    const config = RATE_LIMIT_TESTS[testType];
    if (!config) {
        console.error(`Unknown test type: ${testType}`);
        return;
    }

    console.log(`Running ${requestCount} requests to ${config.endpoint} (limit: ${config.limit}/min)`);
    
    const results = {
        totalRequests: requestCount,
        successfulRequests: 0,
        rateLimitedRequests: 0,
        otherErrors: 0,
        responseTimes: [],
        rateLimitTriggered: false
    };

    // Make requests
    for (let i = 1; i <= requestCount; i++) {
        try {
            const startTime = Date.now();
            
            const request = {
                url: pm.environment.get('HOST') + config.endpoint,
                method: config.method,
                header: config.headers || {},
                body: config.body ? JSON.stringify(config.body) : undefined
            };

            if (config.body) {
                request.header['Content-Type'] = 'application/json';
            }

            const response = pm.sendRequest(request);
            const responseTime = Date.now() - startTime;
            
            results.responseTimes.push(responseTime);

            if (response.code === 429) {
                results.rateLimitedRequests++;
                results.rateLimitTriggered = true;
                console.log(`Request ${i}: Rate limited (429) - ${responseTime}ms`);
                break; // Stop after rate limit is hit
            } else if (response.code >= 200 && response.code < 300) {
                results.successfulRequests++;
                console.log(`Request ${i}: Success (${response.code}) - ${responseTime}ms`);
            } else {
                results.otherErrors++;
                console.log(`Request ${i}: Error (${response.code}) - ${responseTime}ms`);
            }

            // Check rate limit headers
            const remaining = response.headers.get('X-Rate-Limit-Remaining');
            const reset = response.headers.get('X-Rate-Limit-Reset');
            
            if (remaining !== null) {
                console.log(`  Rate limit remaining: ${remaining}, Reset in: ${reset}s`);
            }

        } catch (error) {
            results.otherErrors++;
            console.error(`Request ${i}: Exception - ${error.message}`);
        }
    }

    // Log results
    console.log('\n=== Rate Limiting Test Results ===');
    console.log(`Test Type: ${testType}`);
    console.log(`Total Requests: ${results.totalRequests}`);
    console.log(`Successful: ${results.successfulRequests}`);
    console.log(`Rate Limited: ${results.rateLimitedRequests}`);
    console.log(`Other Errors: ${results.otherErrors}`);
    console.log(`Rate Limit Triggered: ${results.rateLimitTriggered}`);
    
    if (results.responseTimes.length > 0) {
        const avgResponseTime = results.responseTimes.reduce((a, b) => a + b, 0) / results.responseTimes.length;
        const minResponseTime = Math.min(...results.responseTimes);
        const maxResponseTime = Math.max(...results.responseTimes);
        
        console.log(`Average Response Time: ${avgResponseTime.toFixed(2)}ms`);
        console.log(`Min Response Time: ${minResponseTime}ms`);
        console.log(`Max Response Time: ${maxResponseTime}ms`);
    }

    return results;
}

/**
 * Cache performance test
 * @param {string} endpoint - Endpoint to test
 * @param {number} iterations - Number of iterations
 */
function runCachePerformanceTest(endpoint, iterations = 5) {
    console.log(`\n=== Cache Performance Test ===`);
    console.log(`Endpoint: ${endpoint}`);
    console.log(`Iterations: ${iterations}`);

    const results = {
        firstRequestTime: 0,
        subsequentRequestTimes: [],
        cacheHitImprovement: 0
    };

    for (let i = 1; i <= iterations; i++) {
        try {
            const startTime = Date.now();
            
            const request = {
                url: pm.environment.get('HOST') + endpoint,
                method: 'GET',
                header: {
                    'Authorization': 'Bearer {{ADMIN_TOKEN}}'
                }
            };

            const response = pm.sendRequest(request);
            const responseTime = Date.now() - startTime;

            if (i === 1) {
                results.firstRequestTime = responseTime;
                console.log(`First request (cache miss): ${responseTime}ms`);
            } else {
                results.subsequentRequestTimes.push(responseTime);
                console.log(`Request ${i} (cache hit): ${responseTime}ms`);
            }

        } catch (error) {
            console.error(`Request ${i}: Exception - ${error.message}`);
        }
    }

    if (results.subsequentRequestTimes.length > 0) {
        const avgSubsequentTime = results.subsequentRequestTimes.reduce((a, b) => a + b, 0) / results.subsequentRequestTimes.length;
        results.cacheHitImprovement = results.firstRequestTime - avgSubsequentTime;
        
        console.log(`\nCache Performance Results:`);
        console.log(`First request (cache miss): ${results.firstRequestTime}ms`);
        console.log(`Average subsequent requests: ${avgSubsequentTime.toFixed(2)}ms`);
        console.log(`Cache hit improvement: ${results.cacheHitImprovement.toFixed(2)}ms (${((results.cacheHitImprovement/results.firstRequestTime)*100).toFixed(1)}%)`);
    }

    return results;
}

/**
 * Cache eviction test
 * @param {string} endpoint - Endpoint to test
 */
function runCacheEvictionTest(endpoint) {
    console.log(`\n=== Cache Eviction Test ===`);
    console.log(`Endpoint: ${endpoint}`);

    try {
        // First request (should be cached)
        const startTime1 = Date.now();
        const request1 = {
            url: pm.environment.get('HOST') + endpoint,
            method: 'GET',
            header: {
                'Authorization': 'Bearer {{ADMIN_TOKEN}}'
            }
        };
        const response1 = pm.sendRequest(request1);
        const responseTime1 = Date.now() - startTime1;
        console.log(`First request: ${responseTime1}ms`);

        // Clear cache
        const clearRequest = {
            url: pm.environment.get('HOST') + '/api/v1/admin/cache/clear-all',
            method: 'POST',
            header: {
                'Authorization': 'Bearer {{ADMIN_TOKEN}}'
            }
        };
        const clearResponse = pm.sendRequest(clearRequest);
        console.log(`Cache cleared: ${clearResponse.code}`);

        // Second request (should be cache miss again)
        const startTime2 = Date.now();
        const request2 = {
            url: pm.environment.get('HOST') + endpoint,
            method: 'GET',
            header: {
                'Authorization': 'Bearer {{ADMIN_TOKEN}}'
            }
        };
        const response2 = pm.sendRequest(request2);
        const responseTime2 = Date.now() - startTime2;
        console.log(`Second request (after cache clear): ${responseTime2}ms`);

        console.log(`Cache eviction test completed. Both requests should have similar response times.`);

    } catch (error) {
        console.error(`Cache eviction test failed: ${error.message}`);
    }
}

// Export functions for use in Postman
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        runRateLimitTest,
        runCachePerformanceTest,
        runCacheEvictionTest,
        RATE_LIMIT_TESTS
    };
}

// Example usage in Postman pre-request script:
/*
// Run auth rate limit test
const results = runRateLimitTest('auth', 15);

// Run cache performance test
const cacheResults = runCachePerformanceTest('/api/v1/students/1', 5);

// Run cache eviction test
runCacheEvictionTest('/api/v1/students/1');
*/
