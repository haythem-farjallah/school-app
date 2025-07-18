# School Management API - Apidog Collection

This collection is specifically enhanced for **Apidog** with comprehensive test coverage, automated workflows, and advanced features.

## 🚀 Quick Start

### 1. Import Collection
1. Open Apidog
2. Go to **Collections** → **Import**
3. Select `school-admin.apidog_collection.json`
4. Import the environment file `apidog-environment.json`

### 2. Setup Environment
1. Select the **School Management** environment
2. Update the `HOST` variable to your server URL (default: `localhost:8088`)
3. The `TOKEN` will be automatically set after login

### 3. Authentication
1. Navigate to **🔐 Authentication** folder
2. Run the **Login** request
3. The JWT token will be automatically stored for subsequent requests

## 📋 Collection Structure

### 🔐 Authentication
- **Login**: Authenticate and get JWT token
- **Register**: Create new user account
- **Forgot Password**: Request password reset

### 👨‍🎓 Students
- **CRUD Operations**: Create, Read, Update, Delete students
- **Advanced Filtering**: Multi-criteria search and filtering
- **Statistics**: Student enrollment statistics
- **Bulk Operations**: Mass operations on student data

### 👨‍🏫 Teachers
- **Teacher Management**: Full CRUD for teacher profiles
- **Qualifications**: Manage teacher qualifications and subjects
- **Schedule Management**: Handle teacher availability

### 📚 Classes & Courses
- **Class Management**: Create and manage academic classes
- **Course Management**: Subject and curriculum management
- **Enrollment**: Link students to classes and courses

### 📋 Learning Resources
- **Content Management**: Upload and manage educational content
- **Resource Types**: Support for videos, documents, URLs
- **Access Control**: Public/private resource management

### 💬 Resource Comments
- **Feedback System**: Comments and ratings on resources
- **Moderation**: Comment management and approval

### 📊 Admin Dashboard
- **Analytics**: System overview and statistics
- **Role-specific Dashboards**: Tailored views for different user roles

### 🔔 Admin Feeds
- **Activity Monitoring**: Real-time activity feeds
- **Notifications**: System alerts and updates

### 🎓 Enrollment Management
- **Student Enrollment**: Manage student-class relationships
- **Transfer Management**: Handle student transfers
- **Enrollment Validation**: Check enrollment eligibility

### 🔐 Permissions
- **Role Management**: Define and manage user roles
- **Permission Catalog**: Available system permissions
- **Access Control**: Fine-grained permission management

## 🧪 Test Features

### Comprehensive Test Coverage
Each request includes extensive tests that verify:
- ✅ **Response Status**: Correct HTTP status codes
- ✅ **Data Validation**: Response structure and data types
- ✅ **Business Logic**: Domain-specific validations
- ✅ **Performance**: Response time validation
- ✅ **Security**: Authorization and data protection

### Test Categories

#### 🔍 **Functional Tests**
```javascript
pm.test('Student created successfully', function () {
    pm.response.to.have.status(201);
});

pm.test('Created student has valid structure', function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.data).to.have.property('id');
    pm.expect(jsonData.data.profile.role).to.equal('STUDENT');
});
```

#### 🎯 **Data Validation Tests**
```javascript
pm.test('Email format is valid', function () {
    const jsonData = pm.response.json();
    const email = jsonData.data.profile.email;
    pm.expect(email).to.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/);
});
```

#### ⚡ **Performance Tests**
```javascript
pm.test('Response time is acceptable', function () {
    pm.expect(pm.response.responseTime).to.be.below(5000);
});
```

#### 🔒 **Security Tests**
```javascript
pm.test('Password not returned in response', function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.data.user).to.not.have.property('password');
});
```

### Automated Data Management
- **Dynamic Test Data**: Uses Faker.js variables for realistic test data
- **Environment Variables**: Automatic storage and retrieval of created IDs
- **Cleanup Scripts**: Automatic cleanup of test data

## 🔄 Automated Workflows

### Pre-request Scripts
Global pre-request scripts handle:
- Token validation
- Timestamp generation
- Request logging

### Test Scripts
Global test scripts provide:
- Response time validation
- Content type verification
- Error logging and debugging

### Data Flow
```
Login → Store Token → Create Test Data → Run Tests → Cleanup
```

## 🛠️ Advanced Features

### Dynamic Variables
The collection uses dynamic variables for realistic testing:
```json
{
  "firstName": "{{$randomFirstName}}",
  "lastName": "{{$randomLastName}}",
  "email": "user{{$randomInt}}@school.test",
  "gradeLevel": "{{$randomArrayElement(['ELEMENTARY', 'MIDDLE', 'HIGH'])}}"
}
```

### Conditional Logic
Tests include conditional logic for different scenarios:
```javascript
if (jsonData.data.content.length > 0) {
    // Test with data
} else {
    // Test empty state
}
```

### Cross-request Dependencies
Requests automatically share data:
- Login stores token for authentication
- Create operations store IDs for subsequent operations
- Cleanup uses stored IDs for proper teardown

## 📊 Running Tests

### Individual Tests
1. Select any request
2. Click **Send**
3. View test results in the **Test Results** tab

### Collection Runner
1. Right-click on the collection
2. Select **Run Collection**
3. Configure iteration count and delay
4. Monitor real-time test execution

### Automated Testing
Set up automated testing with:
- **Scheduled Runs**: Daily/weekly validation
- **CI/CD Integration**: Include in deployment pipeline
- **Performance Monitoring**: Track API response times

## 🐛 Debugging

### Test Debugging
- Use `console.log()` in test scripts for debugging
- Check the **Console** tab for detailed logs
- Verify environment variables are set correctly

### Common Issues
1. **Token Expired**: Re-run the login request
2. **Environment Variables**: Ensure correct environment is selected
3. **Server Connection**: Verify HOST variable points to running server

## 📈 Best Practices

### Test Organization
- Run **Authentication** tests first
- Use **Cleanup** folder to remove test data
- Group related tests in logical folders

### Data Management
- Use unique identifiers for test data
- Clean up created resources after testing
- Verify environment variables are properly set

### Performance
- Set reasonable timeout expectations
- Monitor response times across different environments
- Use pagination for large data sets

## 🔧 Customization

### Adding New Tests
```javascript
pm.test('Your custom test', function () {
    // Your test logic here
    pm.expect(condition).to.be.true;
});
```

### Environment Variables
Add custom variables in the environment file:
```json
{
    "key": "CUSTOM_VAR",
    "value": "your_value",
    "description": "Description of your variable"
}
```

### Request Modifications
Customize requests for your specific needs:
- Update endpoints
- Modify request bodies
- Add custom headers

## 🆘 Support

For issues or questions:
1. Check the **Console** tab for error messages
2. Verify all environment variables are set
3. Ensure the API server is running and accessible
4. Review test logs for specific failure details

---

**Happy Testing! 🎉**

This collection provides comprehensive coverage of the School Management API with robust testing and validation. 