# Timetable Auto-Generate Test Guide

## Prerequisites
1. Backend server is running
2. Frontend is running
3. Database has sample data (teachers, periods, rooms, classes)

## Test Steps

### 1. Verify Sample Data Exists
Check that the following data exists in the database:
- At least 1 class
- At least 1 teacher
- At least 1 period
- At least 1 room

### 2. Test Auto-Generate Functionality

#### Step 1: Navigate to Timetable Page
1. Go to the timetable page for a specific class
2. Ensure you're logged in as an admin user

#### Step 2: Click Auto-Generate Button
1. Click the "Auto-generate" button
2. Check the browser console for any errors
3. Check the backend logs for any errors

#### Step 3: Verify Results
1. The timetable should be populated with random teacher assignments
2. Each slot should have:
   - A teacher assigned
   - A room assigned
   - A period assigned
   - A day of the week assigned

### 3. Expected Behavior

#### Success Case:
- Auto-generate button should complete without errors
- Timetable grid should show teachers assigned to various slots
- Success notification should appear
- Data should persist after page refresh

#### Error Cases:
- If no teachers exist: Should show "No teachers available" error
- If no periods exist: Should show "No periods available" error
- If no rooms exist: Should show "No rooms available" error
- If class doesn't exist: Should show "Class not found" error

### 4. Debug Endpoint Test
The system includes a debug endpoint that can be tested directly:

```bash
curl -X POST http://localhost:8080/api/v1/timetables/debug/class/1/test-optimization \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

This endpoint will:
- Check if class exists
- Check if teachers exist
- Check if periods exist
- Check if rooms exist
- Attempt optimization
- Return detailed status information

### 5. Manual Save Test
After auto-generation:
1. Drag teachers between slots
2. Click "Save Timetable" button
3. Verify changes are persisted
4. Refresh page and verify data is still there

## Troubleshooting

### Common Issues:
1. **DayOfWeek Enum Mismatch**: Fixed - frontend now uses 'MONDAY', 'TUESDAY', etc.
2. **Missing Room Assignment**: Fixed - optimization now assigns random rooms
3. **Data Structure Mismatch**: Fixed - frontend types updated to match backend
4. **Save Format Issue**: Fixed - frontend now sends correct data format

### Log Locations:
- Backend logs: Check console output for detailed error messages
- Frontend logs: Check browser console for JavaScript errors
- Network tab: Check for failed API requests

### Database Verification:
```sql
-- Check if sample data exists
SELECT COUNT(*) FROM teacher;
SELECT COUNT(*) FROM periods;
SELECT COUNT(*) FROM rooms;
SELECT COUNT(*) FROM classes;

-- Check generated timetable slots
SELECT * FROM timetable_slots WHERE for_class_id = 1;
``` 