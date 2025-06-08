import { CoursesTable } from "./courses-table";

/**
 * Example usage of the CoursesTable component
 * 
 * To use this in your routing:
 * 1. Import this component in your routes
 * 2. The table will automatically:
 *    - Fetch data from /v1/courses?page=0&size=10
 *    - Handle pagination, sorting, and filtering
 *    - Sync state with URL parameters
 *    - Provide view, edit, delete actions
 * 
 * Features included:
 * - Advanced filtering (text, number, range)
 * - Column visibility management
 * - Row selection with bulk actions
 * - Server-side pagination
 * - Professional UI with blue/white theme
 * - Responsive design
 */
export function CoursesPageExample() {
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto py-8 px-4 max-w-7xl">
        <CoursesTable />
      </div>
    </div>
  );
}

export default CoursesPageExample; 