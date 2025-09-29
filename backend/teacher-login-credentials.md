# Teacher Login Credentials

## Default Teacher Accounts

The following teacher accounts are available for testing:

| Email | Password | Name |
|-------|----------|------|
| tom.teacher@school.test | ChangeMe123 | Tom Teacher |
| sara.teacher@school.test | ChangeMe123 | Sara Teacher |
| john.teacher@school.test | ChangeMe123 | John Teacher |

## Important Notes

1. **Password Change Required**: All teacher accounts have `password_change_required = TRUE`, which means:
   - You can login with the default password `ChangeMe123`
   - After successful login, you'll be redirected to the password change page
   - You must change your password before accessing the main application

2. **Login Process**:
   - Use any of the teacher emails above
   - Use password: `ChangeMe123`
   - After login, you'll be redirected to `/change-password`
   - Set your new password
   - You'll then be redirected to the dashboard

## Troubleshooting

If you're getting a 401 Unauthorized error:

1. **Check the password**: Make sure you're using exactly `ChangeMe123` (case-sensitive)
2. **Check the email**: Use one of the exact emails listed above
3. **Database state**: Ensure the teacher records exist in the database

## Alternative Solution

If you want to disable the password change requirement for testing, run the SQL script `fix-teacher-login.sql` in the backend directory.
