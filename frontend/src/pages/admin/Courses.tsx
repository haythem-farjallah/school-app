import { useTranslation } from 'react-i18next';
import { CoursesTable } from '@/features/courses/components/courses-table';

const AdminCourses = () => {
  const { t } = useTranslation();

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          {t('Courses Management')}
        </h1>
        <p className="text-gray-600">
          {t('Manage and organize your courses, set credits, and assign teachers')}
        </p>
      </div>
      
      {/* Courses Table */}
      <CoursesTable />
    </div>
  );
};

export default AdminCourses; 