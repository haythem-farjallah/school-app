# Teacher Login Issue - Solution

## Problem Analysis

The 401 Unauthorized error during teacher login is caused by one of these issues:

1. **Incorrect credentials**: Using wrong email/password combination
2. **Password change requirement**: Teachers have `password_change_required = TRUE`
3. **Missing teacher records**: Teacher users exist but no corresponding teacher records

## Solution

### Option 1: Use Correct Credentials (Recommended)

**Default Teacher Accounts:**
- **Email**: `tom.teacher@school.test`
- **Password**: `ChangeMe123`
- **Name**: Tom Teacher

**Alternative Teacher Accounts:**
- **Email**: `sara.teacher@school.test`
- **Password**: `ChangeMe123`
- **Name**: Sara Teacher

- **Email**: `john.teacher@school.test`
- **Password**: `ChangeMe123`
- **Name**: John Teacher

### Option 2: Disable Password Change Requirement

Run the SQL script to disable password change requirement:

```sql
-- Run this in your database
UPDATE users 
SET password_change_required = FALSE 
WHERE role = 'TEACHER' 
AND password_change_required = TRUE;
```

### Option 3: Create New Teacher Account

If you want to create a new teacher account with a custom password:

1. Use the registration endpoint: `POST /api/auth/register`
2. Set role to `TEACHER`
3. The system will automatically set `password_change_required = TRUE`

## Login Process

1. **First Login** (with `password_change_required = TRUE`):
   - Login with email: `tom.teacher@school.test`
   - Password: `ChangeMe123`
   - You'll be redirected to password change page
   - Set your new password
   - You'll be redirected to dashboard

2. **Subsequent Logins** (after password change):
   - Use your new password
   - Direct access to dashboard

## Troubleshooting

### Check Database State
```sql
SELECT 
    u.email,
    u.first_name,
    u.last_name,
    u.role,
    u.password_change_required,
    u.status,
    CASE WHEN t.id IS NOT NULL THEN 'Teacher record exists' ELSE 'No teacher record' END as teacher_status
FROM users u
LEFT JOIN teacher t ON u.id = t.id
WHERE u.role = 'TEACHER';
```

### Verify Backend is Running
- Backend should be running on `http://localhost:8088`
- Check if `/api/auth/login` endpoint is accessible
- Verify database connection

### Check Frontend Configuration
- Frontend should be running on `http://localhost:5173`
- API_URL should be set to `http://localhost:8088/api`
- Check browser network tab for actual request details

## Files Modified

1. `backend/fix-teacher-login.sql` - SQL script to fix teacher accounts
2. `backend/teacher-login-credentials.md` - Detailed credentials documentation

## Testing

1. **Test with correct credentials**:
   - Email: `tom.teacher@school.test`
   - Password: `ChangeMe123`

2. **Expected behavior**:
   - Login should succeed
   - Redirect to password change page
   - After password change, redirect to dashboard

3. **If still getting 401**:
   - Check database connection
   - Verify teacher records exist
   - Check backend logs for detailed error messages
