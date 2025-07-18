/**
 * Apidog Test Runner for School Management API
 * 
 * This script provides utilities for running comprehensive tests
 * in the correct order with proper data cleanup.
 */

// Test execution order for complete API validation
const TEST_EXECUTION_ORDER = [
    // 1. Authentication (Required first)
    "🔐 Authentication/Login",
    
    // 2. Get initial data for subsequent tests
    "👨‍🎓 Students/Get Students (Paged)",
    "👨‍🏫 Teachers/Get Teachers (Paged)",
    "📚 Classes & Courses/Get Classes",
    "📚 Classes & Courses/Get Courses",
    "📋 Learning Resources/Get Learning Resources",
    
    // 3. Create operations
    "👨‍🎓 Students/Create Student",
    "👨‍🏫 Teachers/Create Teacher",
    "📚 Classes & Courses/Create Class",
    "📚 Classes & Courses/Create Course",
    "📋 Learning Resources/Create Learning Resource (URL)",
    "💬 Resource Comments/Create Comment",
    
    // 4. Read operations with new data
    "👨‍🎓 Students/Get Student by ID",
    "👨‍🎓 Students/Search Students",
    "👨‍🎓 Students/Advanced Filter Students",
    "👨‍🎓 Students/Get Student Statistics",
    "📋 Learning Resources/Filter Resources by Type",
    "💬 Resource Comments/Get Comments by Resource",
    
    // 5. Enrollment and relationships
    "🎓 Enrollment Management/Check Can Enroll Student",
    "🎓 Enrollment Management/Enroll Student in Class",
    "🎓 Enrollment Management/Get Student Enrollments",
    
    // 6. Update operations
    "👨‍🎓 Students/Update Student",
    
    // 7. Dashboard and feeds
    "📊 Admin Dashboard/Get System Overview",
    "📊 Admin Dashboard/Get Admin Dashboard",
    "🔔 Admin Feeds/Get Admin Feeds",
    "🔔 Admin Feeds/Mark Feed as Read",
    "🔔 Admin Feeds/Get Unread Feeds",
    
    // 8. Permissions
    "🔐 Permissions/Get Permission Catalogue",
    "🔐 Permissions/Get Role Default Permissions",
    
    // 9. Cleanup (Required last)
    "🧹 Cleanup/Delete Created Comment",
    "🧹 Cleanup/Delete Created Resource",
    "🧹 Cleanup/Delete Created Course",
    "🧹 Cleanup/Delete Created Class",
    "🧹 Cleanup/Delete Created Teacher",
    "🧹 Cleanup/Delete Created Student"
];

// Test validation helpers
const TestValidators = {
    /**
     * Validate response status codes
     */
    validateStatus: function(expectedStatus) {
        pm.test(`Response status is ${expectedStatus}`, function () {
            pm.response.to.have.status(expectedStatus);
        });
    },

    /**
     * Validate response time
     */
    validateResponseTime: function(maxTime = 5000) {
        pm.test('Response time is acceptable', function () {
            pm.expect(pm.response.responseTime).to.be.below(maxTime);
        });
    },

    /**
     * Validate JSON response structure
     */
    validateJsonStructure: function(requiredFields) {
        pm.test('Response has required JSON structure', function () {
            const jsonData = pm.response.json();
            requiredFields.forEach(field => {
                pm.expect(jsonData).to.have.property(field);
            });
        });
    },

    /**
     * Validate pagination structure
     */
    validatePagination: function() {
        pm.test('Response has valid pagination', function () {
            const jsonData = pm.response.json();
            pm.expect(jsonData.data).to.have.property('content');
            pm.expect(jsonData.data).to.have.property('totalElements');
            pm.expect(jsonData.data).to.have.property('totalPages');
            pm.expect(jsonData.data).to.have.property('number');
            pm.expect(jsonData.data).to.have.property('size');
            
            pm.expect(jsonData.data.content).to.be.an('array');
            pm.expect(jsonData.data.totalElements).to.be.a('number');
            pm.expect(jsonData.data.totalPages).to.be.a('number');
            pm.expect(jsonData.data.number).to.be.a('number');
            pm.expect(jsonData.data.size).to.be.a('number');
        });
    },

    /**
     * Validate entity ID
     */
    validateEntityId: function(idField = 'id') {
        pm.test(`Entity has valid ${idField}`, function () {
            const jsonData = pm.response.json();
            pm.expect(jsonData.data).to.have.property(idField);
            pm.expect(jsonData.data[idField]).to.be.a('number');
            pm.expect(jsonData.data[idField]).to.be.above(0);
        });
    },

    /**
     * Validate email format
     */
    validateEmail: function(emailPath) {
        pm.test('Email format is valid', function () {
            const jsonData = pm.response.json();
            const email = pm.utils.getNestedProperty(jsonData, emailPath);
            pm.expect(email).to.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/);
        });
    }
};

// Environment variable helpers
const EnvironmentHelper = {
    /**
     * Store entity ID for later use
     */
    storeEntityId: function(responseIdPath, environmentKey) {
        if (pm.response.code === 200 || pm.response.code === 201) {
            const jsonData = pm.response.json();
            const entityId = pm.utils.getNestedProperty(jsonData, responseIdPath);
            if (entityId) {
                pm.environment.set(environmentKey, entityId);
                console.log(`Stored ${environmentKey}: ${entityId}`);
            }
        }
    },

    /**
     * Store authentication token
     */
    storeAuthToken: function() {
        if (pm.response.code === 200) {
            const jsonData = pm.response.json();
            if (jsonData.data && jsonData.data.token) {
                pm.environment.set('TOKEN', jsonData.data.token);
                pm.environment.set('USER_ID', jsonData.data.user.id);
                pm.environment.set('USER_ROLE', jsonData.data.user.role);
                console.log('Authentication token stored successfully');
            }
        }
    },

    /**
     * Clear test data variables
     */
    clearTestData: function() {
        const testVariables = [
            'CREATED_STUDENT_ID',
            'CREATED_TEACHER_ID',
            'CREATED_CLASS_ID',
            'CREATED_COURSE_ID',
            'CREATED_RESOURCE_ID',
            'CREATED_COMMENT_ID',
            'CREATED_ENROLLMENT_ID'
        ];
        
        testVariables.forEach(variable => {
            pm.environment.unset(variable);
        });
        
        console.log('Test data variables cleared');
    }
};

// Test suite configurations
const TestSuites = {
    /**
     * Basic smoke test suite
     */
    smoke: [
        "🔐 Authentication/Login",
        "👨‍🎓 Students/Get Students (Paged)",
        "📊 Admin Dashboard/Get System Overview"
    ],

    /**
     * CRUD operations test suite
     */
    crud: [
        "🔐 Authentication/Login",
        "👨‍🎓 Students/Create Student",
        "👨‍🎓 Students/Get Student by ID",
        "👨‍🎓 Students/Update Student",
        "🧹 Cleanup/Delete Created Student"
    ],

    /**
     * Search and filtering test suite
     */
    search: [
        "🔐 Authentication/Login",
        "👨‍🎓 Students/Get Students (Paged)",
        "👨‍🎓 Students/Search Students",
        "👨‍🎓 Students/Advanced Filter Students",
        "📋 Learning Resources/Filter Resources by Type"
    ],

    /**
     * Complete integration test suite
     */
    full: TEST_EXECUTION_ORDER
};

// Performance monitoring
const PerformanceMonitor = {
    /**
     * Track response times across requests
     */
    trackResponseTime: function(requestName) {
        const responseTime = pm.response.responseTime;
        const globalTimes = pm.globals.get('responseTimes') || {};
        globalTimes[requestName] = responseTime;
        pm.globals.set('responseTimes', JSON.stringify(globalTimes));
    },

    /**
     * Generate performance report
     */
    generateReport: function() {
        const responseTimes = JSON.parse(pm.globals.get('responseTimes') || '{}');
        const times = Object.values(responseTimes);
        
        if (times.length > 0) {
            const avgTime = times.reduce((a, b) => a + b, 0) / times.length;
            const maxTime = Math.max(...times);
            const minTime = Math.min(...times);
            
            console.log('Performance Report:');
            console.log(`Average Response Time: ${avgTime.toFixed(2)}ms`);
            console.log(`Max Response Time: ${maxTime}ms`);
            console.log(`Min Response Time: ${minTime}ms`);
            
            return { average: avgTime, max: maxTime, min: minTime };
        }
        
        return null;
    }
};

// Export utilities for use in Apidog tests
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        TestValidators,
        EnvironmentHelper,
        TestSuites,
        PerformanceMonitor,
        TEST_EXECUTION_ORDER
    };
}

// Usage examples for test scripts:
/*
// In pre-request script:
PerformanceMonitor.trackResponseTime(pm.info.requestName);

// In test script:
TestValidators.validateStatus(200);
TestValidators.validateResponseTime();
TestValidators.validatePagination();
EnvironmentHelper.storeEntityId('data.id', 'CREATED_STUDENT_ID');

// For authentication requests:
EnvironmentHelper.storeAuthToken();

// For cleanup:
EnvironmentHelper.clearTestData();
*/ 