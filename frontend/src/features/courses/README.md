# Courses Feature

This feature provides comprehensive course management functionality for the admin panel.

## 🎯 Overview

The courses feature includes:
- **Advanced Data Table** with filtering, sorting, pagination
- **CRUD Operations** for course management
- **Professional UI** with blue/white theme
- **Responsive Design** for all devices
- **URL State Management** for shareable views

## 📁 File Structure

```
courses/
├── components/
│   ├── course-columns.tsx     # Table column definitions
│   ├── courses-table.tsx      # Main table component
│   └── courses-example.tsx    # Usage examples
├── hooks/
│   └── use-courses.ts         # API hooks for data fetching
└── README.md                  # This file
```

## 🚀 Features

### ✅ **Advanced Data Table**
- Server-side pagination, sorting, filtering
- Column visibility management
- Row selection with bulk actions
- URL state synchronization
- Loading states and error handling

### ✅ **Course Management**
- View course details
- Edit course information
- Delete courses (with confirmation)
- Visual color display
- Coefficient badges
- Teacher assignment

### ✅ **Professional UI**
- Clean blue/white theme
- Responsive design
- Action dropdowns
- Loading skeletons
- Stats cards

## 🔧 API Integration

**Endpoint**: `/v1/courses`

**Expected Response Format**:
```json
{
  "status": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Physics",
        "color": "#ff00ff",
        "coefficient": 2.0,
        "teacherId": 7
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 31
  }
}
```

## 🎨 Usage

### In Admin Panel
The courses page is automatically available at `/admin/courses` and includes:
- Navigation menu integration
- Proper routing setup
- Role-based access (ADMIN only)

### Direct Component Usage
```tsx
import { CoursesTable } from '@/features/courses/components/courses-table';

function MyPage() {
  return <CoursesTable />;
}
```

## 🔗 Dependencies

- `@tanstack/react-table` - Advanced table functionality
- `@tanstack/react-query` - Data fetching and caching
- `nuqs` - URL state management
- `react-hot-toast` - Notifications
- `lucide-react` - Icons

## 🎯 Next Steps

1. **Add Course Creation Modal** - Form to create new courses
2. **Implement Teacher Selection** - Dropdown to select teachers
3. **Add Bulk Actions** - Delete multiple courses at once
4. **Export Functionality** - Export courses as CSV/PDF
5. **Course Statistics** - Analytics and reporting 