import React from 'react';
import { ImprovedTimetableGrid } from '../../features/timetable/components/ImprovedTimetableGrid';
import { useClasses } from '@/features/classes/hooks/use-classes';
import { useNavigate, useParams } from 'react-router-dom';

const TimetablePage: React.FC = () => {
  const navigate = useNavigate();
  const { classId } = useParams<{ classId?: string }>();
  const { data, isLoading, error } = useClasses({ size: 100 });
  const classes = data?.data || [];
  const selectedClassId = classId ? Number(classId) : (classes[0]?.id || null);

  if (!classId && classes.length > 0) {
    navigate(`/admin/timetable/${classes[0].id}`, { replace: true });
  }

  if (isLoading) return <div className="p-8 text-gray-600">Loading classes...</div>;
  if (error) return <div className="p-8 text-red-600">Error loading classes.</div>;
  if (!classes.length) return <div className="p-8 text-gray-600">No classes found.</div>;

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Class Timetable Management</h1>
      <div className="flex gap-2 mb-6">
        {classes.map((cls: import('@/types/class').Class) => (
          <button
            key={cls.id}
            onClick={() => navigate(`/admin/timetable/${cls.id}`)}
            className={`
              px-4 py-2 rounded cursor-pointer transition-all duration-200
              ${selectedClassId === cls.id 
                ? 'border-2 border-indigo-500 bg-indigo-500 text-white font-bold' 
                : 'border border-gray-300 bg-white text-gray-800 font-normal hover:bg-gray-50'
              }
            `}
          >
            {cls.name}
          </button>
        ))}
      </div>
      {selectedClassId && <ImprovedTimetableGrid classId={selectedClassId} />}
    </div>
  );
};

export default TimetablePage; 