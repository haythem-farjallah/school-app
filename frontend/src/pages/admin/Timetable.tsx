import React from 'react';
import TimetableGrid from '../../features/timetable/TimetableGrid';
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

  if (isLoading) return <div>Loading classes...</div>;
  if (error) return <div>Error loading classes.</div>;
  if (!classes.length) return <div>No classes found.</div>;

  return (
    <div style={{ padding: 32 }}>
      <h1>Class Timetable Management</h1>
      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        {classes.map((cls: import('@/types/class').Class) => (
          <button
            key={cls.id}
            onClick={() => navigate(`/admin/timetable/${cls.id}`)}
            style={{
              padding: '8px 16px',
              borderRadius: 4,
              border: selectedClassId === cls.id ? '2px solid #6366f1' : '1px solid #ccc',
              background: selectedClassId === cls.id ? '#6366f1' : '#fff',
              color: selectedClassId === cls.id ? '#fff' : '#222',
              fontWeight: selectedClassId === cls.id ? 'bold' : 'normal',
              cursor: 'pointer',
            }}
          >
            {cls.name}
          </button>
        ))}
      </div>
      {selectedClassId && <TimetableGrid classId={selectedClassId} />}
    </div>
  );
};

export default TimetablePage; 