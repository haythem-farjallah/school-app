import React, { useState } from 'react';
import { DndContext, closestCenter } from '@dnd-kit/core';
import { useDroppable, useDraggable, DragOverlay } from '@dnd-kit/core';
import { Teacher } from '../../types/teacher';
import { TimetableSlot } from '../../types/timetable';
import { Period } from '../../types/period';
import { Timetable } from '../../types/timetable';
import { useTeachers } from '../teachers/hooks/use-teachers';
import { usePeriods, useTimetable } from './hooks';
import { Class } from '../../types/class';
import { http } from '../../lib/http';

// Dummy class list for selector (replace with API call if needed)
const classes: Class[] = [
  { id: 1, name: 'Class 1A', yearOfStudy: 1, maxStudents: 30, studentIds: [], courseIds: [], teacherIds: [], assignedRoomId: null },
  { id: 2, name: 'Class 2B', yearOfStudy: 2, maxStudents: 28, studentIds: [], courseIds: [], teacherIds: [], assignedRoomId: null },
];

export const TimetableGrid: React.FC = () => {
  const [selectedClassId, setSelectedClassId] = useState<number>(classes[0].id);
  const { data: teachersData, isLoading: teachersLoading, error: teachersError } = useTeachers({ size: 100 });
  const { data: periodsRaw, isLoading: periodsLoading, error: periodsError } = usePeriods();
  const { data: timetableRaw, isLoading: timetableLoading, error: timetableError } = useTimetable(selectedClassId);
  const periods = periodsRaw as Period[] | undefined;
  const timetable = timetableRaw as Timetable | undefined;
  const [activeTeacher, setActiveTeacher] = useState<Teacher | null>(null);
  // Local state for slot assignments (for drag-and-drop)
  const [localSlots, setLocalSlots] = useState<Record<string, TimetableSlot>>({});
  const [dragSourceSlotId, setDragSourceSlotId] = useState<string | null>(null);

  React.useEffect(() => {
    if (timetable && periods) {
      // Map slots by key for fast access
      const slotMap: Record<string, TimetableSlot> = {};
      periods.forEach((period: Period) => {
        ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'].forEach(day => {
          const slot = timetable.slots.find((s: TimetableSlot) => s.dayOfWeek === day && s.period.id === period.id);
          slotMap[`${day}-${period.id}`] = slot || { id: 0, dayOfWeek: day, period, forClassId: selectedClassId };
        });
      });
      setLocalSlots(slotMap);
    }
  }, [timetable, periods, selectedClassId]);
  // Teachers not assigned to any slot
  const assignedTeacherIds = Object.values(localSlots).map(s => s.teacher?.id).filter(Boolean);
  const availableTeachers = (teachersData?.data || []).filter(t => !assignedTeacherIds.includes(t.id));
  // Drag handlers
  const handleDragStart = (event: { active: { id: number | string } }) => {
    // If dragging from a slot, id is slot key; if from teacher list, id is teacher id (number)
    if (typeof event.active.id === 'string') {
      setDragSourceSlotId(event.active.id);
      const slot = localSlots[event.active.id];
      setActiveTeacher(slot?.teacher || null);
    } else {
      setDragSourceSlotId(null);
      const teacherId = event.active.id;
      const teacher = availableTeachers.find(t => t.id === teacherId);
      setActiveTeacher(teacher || null);
    }
  };

  const handleDragEnd = (event: { active: { id: number | string }, over: { id: string | number } | null }) => {
    const { over } = event;
    if (over) {
      // Dropped on a slot
      if (typeof over.id === 'string' && activeTeacher) {
        setLocalSlots(prev => ({
          ...prev,
          [over.id]: { ...prev[over.id], teacher: activeTeacher },
          ...(dragSourceSlotId ? { [dragSourceSlotId]: { ...prev[dragSourceSlotId], teacher: undefined } } : {}),
        }));
      }
      // Dropped on teacher list (unassign)
      if (over.id === 'teacher-list' && dragSourceSlotId) {
        setLocalSlots(prev => ({
          ...prev,
          [dragSourceSlotId]: { ...prev[dragSourceSlotId], teacher: undefined },
        }));
      }
    }
    setActiveTeacher(null);
    setDragSourceSlotId(null);
  };
  // Save timetable handler
  const handleSave = async () => {
    const slotsToSave = Object.values(localSlots).filter(s => s.teacher);
    await http.put(`/api/v1/timetables/class/${selectedClassId}/slots`, { slots: slotsToSave });
    alert('Timetable saved!');
  };
  // Auto-generate handler
  const handleAutoGenerate = async () => {
    await http.post(`/api/v1/timetables/class/${selectedClassId}/optimize`);
    window.location.reload(); // Reload to fetch new optimized timetable
  };
  // Loading and error states
  if (teachersLoading || periodsLoading || timetableLoading) return <div>Loading...</div>;
  if (teachersError || periodsError || timetableError) return <div>Error loading data.</div>;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Class Selector */}
      <div style={{ marginBottom: 16 }}>
        <label>Class: </label>
        <select value={selectedClassId} onChange={e => setSelectedClassId(Number(e.target.value))}>
          {classes.map(cls => (
            <option key={cls.id} value={cls.id}>{cls.name}</option>
          ))}
        </select>
        <button onClick={handleAutoGenerate} style={{ marginLeft: 16 }}>Auto-generate</button>
        <button onClick={handleSave} style={{ marginLeft: 8 }}>Save</button>
      </div>
      <div style={{ display: 'flex', gap: 32 }}>
        {/* Teacher List */}
        <div style={{ minWidth: 200 }}>
          <h3>Teachers</h3>
          <DndContext onDragStart={handleDragStart} onDragEnd={handleDragEnd} collisionDetection={closestCenter}>
            <DroppableTeacherList>
              {availableTeachers.map(teacher => (
                <DraggableTeacher key={teacher.id} teacher={teacher} />
              ))}
            </DroppableTeacherList>
            <DragOverlay>
              {activeTeacher ? <TeacherCard teacher={activeTeacher} /> : null}
            </DragOverlay>
          </DndContext>
        </div>
        {/* Timetable Grid */}
        <div>
          <table border={1} cellPadding={8} style={{ borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th>Period</th>
                {['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'].map(day => (
                  <th key={day}>{day}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {periods && periods.map((period: Period) => (
                <tr key={period.id}>
                  <td>{period.label}</td>
                  {['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'].map(day => (
                    <DroppableSlot
                      key={`${day}-${period.id}`}
                      id={`${day}-${period.id}`}
                      teacher={localSlots[`${day}-${period.id}`]?.teacher}
                    />
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

// Draggable teacher card
const DraggableTeacher: React.FC<{ teacher: Teacher }> = ({ teacher }) => {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: teacher.id,
  });
  return (
    <div
      ref={setNodeRef}
      {...attributes}
      {...listeners}
      style={{
        padding: 8,
        margin: '8px 0',
        background: isDragging ? '#eee' : '#fff',
        border: '1px solid #ccc',
        borderRadius: 4,
        cursor: 'grab',
      }}
    >
      {teacher.firstName} {teacher.lastName}
    </div>
  );
};

// Droppable teacher list (for unassigning)
const DroppableTeacherList: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { setNodeRef, isOver } = useDroppable({ id: 'teacher-list' });
  return (
    <div
      ref={setNodeRef}
      style={{
        minHeight: 100,
        background: isOver ? '#e0e7ff' : undefined,
        padding: 4,
        borderRadius: 4,
      }}
    >
      {children}
    </div>
  );
};

// Droppable timetable slot (for unassigning)
const DroppableSlot: React.FC<{ id: string; teacher?: Teacher }> = ({ id, teacher }) => {
  const { setNodeRef, isOver } = useDroppable({ id });
  return (
    <td
      ref={setNodeRef}
      style={{
        minWidth: 100,
        minHeight: 40,
        background: isOver ? '#d0f0c0' : '#fafafa',
        textAlign: 'center',
      }}
    >
      {teacher ? <DraggableSlotTeacher id={id} teacher={teacher} /> : <span style={{ color: '#bbb' }}>Drop here</span>}
    </td>
  );
};

// Draggable teacher in a slot
const DraggableSlotTeacher: React.FC<{ id: string; teacher: Teacher }> = ({ id, teacher }) => {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({ id });
  return (
    <div
      ref={setNodeRef}
      {...attributes}
      {...listeners}
      style={{
        padding: 4,
        background: isDragging ? '#eee' : '#f5f5f5',
        border: '1px solid #ccc',
        borderRadius: 4,
        cursor: 'grab',
      }}
    >
      {teacher.firstName} {teacher.lastName}
    </div>
  );
};

// Teacher card (used in both list and slot)
const TeacherCard: React.FC<{ teacher: Teacher }> = ({ teacher }) => (
  <div style={{ padding: 4, background: '#f5f5f5', borderRadius: 4 }}>{teacher.firstName} {teacher.lastName}</div>
);

export default TimetableGrid; 