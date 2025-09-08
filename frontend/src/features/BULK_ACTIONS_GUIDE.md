# Enhanced Bulk Actions - User Guide

## Overview
The enhanced bulk actions feature provides comprehensive export, email, and status management capabilities for Teachers, Students, and Staff across the School Management System.

## Available Actions

### 🗑️ Bulk Delete
- **Description:** Soft delete multiple users at once
- **Endpoint:** `DELETE /api/admin/{entity}/bulk`
- **Frontend:** Select rows → Delete button → Confirmation dialog

### 📊 Export Features
- **CSV Export:** Plain text format for spreadsheet applications
- **Excel Export:** Rich formatted Excel files with styled headers
- **Endpoints:** 
  - `POST /api/admin/{entity}/export/csv`
  - `POST /api/admin/{entity}/export/excel`
- **Frontend:** Select rows (optional) → Export → Choose format → Auto-download

### 📧 Bulk Email
- **Description:** Send personalized emails to multiple users
- **Templates:** Welcome, Reminder, Notification, Custom
- **Personalization:** Automatic {firstName}, {lastName} substitution
- **Action Buttons:** Optional call-to-action buttons in emails
- **Endpoint:** `POST /api/admin/{entity}/bulk/email`
- **Frontend:** Select rows → Email button → Composition dialog

### ✏️ Status Updates
- **Description:** Update user status (ACTIVE, INACTIVE, SUSPENDED) in bulk
- **Audit Logging:** All status changes are logged with reason
- **Endpoint:** `PATCH /api/admin/{entity}/bulk/status`
- **Frontend:** Select rows → Status dropdown → Reason (optional)

## User Experience Features

### 🎯 Smart File Downloads
- Automatic filename generation with timestamps
- Proper MIME types for different formats
- Browser-native download handling
- Progress feedback during export

### 📨 Email Composition
- Pre-built templates for common scenarios
- Rich text editor with preview
- Personalization variables
- Optional action buttons with custom URLs
- Recipient count display

### 🔄 Real-time Feedback
- Toast notifications for all operations
- Loading states during processing
- Success/error indicators
- Proper error messages

## Usage Examples

### Export Selected Teachers to Excel
```typescript
// Frontend hook usage
const exportMutation = useBulkExportTeachers();

const handleExport = (selectedIds: number[]) => {
  exportMutation.mutate({ 
    ids: selectedIds, 
    format: 'xlsx' 
  });
};
```

### Send Welcome Email to New Students
```typescript
// Frontend hook usage
const emailMutation = useBulkEmailStudents();

const sendWelcomeEmail = (studentIds: number[]) => {
  emailMutation.mutate({
    ids: studentIds,
    subject: "Welcome to Our School!",
    message: "Welcome {firstName}! We're excited to have you join us.",
    actionUrl: "/student/dashboard",
    actionText: "Access Dashboard"
  });
};
```

### Bulk Status Update with Reason
```typescript
// Frontend hook usage
const statusMutation = useBulkUpdateStaffStatus();

const updateStatus = (ids: number[], status: string, reason: string) => {
  statusMutation.mutate({ 
    ids, 
    status, 
    reason 
  });
};
```

## Backend API Responses

### Export Endpoints
```json
// CSV Response
Content-Type: text/csv
Content-Disposition: attachment; filename="teachers_export_20241201_143022.csv"

// Excel Response  
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="teachers_export_20241201_143022.xlsx"
```

### Email Endpoint
```json
{
  "status": "success",
  "message": "Bulk email initiated for 5 teachers",
  "data": null
}
```

### Status Update Response
```json
{
  "status": "success", 
  "message": "Teacher statuses updated successfully",
  "data": null
}
```

## Error Handling

### Frontend Error States
- Network connectivity issues
- Invalid file formats
- Missing required fields
- Server validation errors

### User Feedback
- Descriptive error messages
- Retry mechanisms where appropriate
- Graceful fallbacks
- Loading state management

## Performance Considerations

### Export Operations
- Large datasets are handled server-side
- Streaming responses for memory efficiency
- Client-side file generation for small datasets

### Email Operations  
- Asynchronous processing prevents UI blocking
- Batch processing on server side
- Progress indicators for user feedback

### Status Updates
- Batch database operations for efficiency
- Transaction rollback on partial failures
- Audit trail creation

## Browser Compatibility
- Modern browsers (Chrome 90+, Firefox 88+, Safari 14+, Edge 90+)
- File download APIs
- Blob and ArrayBuffer support
- Modern JavaScript features (async/await, optional chaining)

## Security Features
- Role-based access control
- Input validation and sanitization  
- CSRF protection
- Audit logging for all operations
- Email rate limiting

---

*Last updated: Day 2 of Enhanced Bulk Actions implementation*
