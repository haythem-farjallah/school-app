import React from 'react';
import TimetableGrid from '../../features/timetable/TimetableGrid';

const TimetablePage: React.FC = () => {
  return (
    <div style={{ padding: 32 }}>
      <h1>Class Timetable Management</h1>
      <TimetableGrid />
    </div>
  );
};

export default TimetablePage; 