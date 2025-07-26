import React, { useState } from 'react';
import { DndContext, closestCenter } from '@dnd-kit/core';
import { useDroppable, useDraggable, DragOverlay } from '@dnd-kit/core';
import { Teacher } from '../../types/teacher';
import { TimetableSlot } from '../../types/timetable';
import { Period } from '../../types/period';
import { Timetable } from '../../types/timetable';
import { useTeachers } from '../teachers/hooks/use-teachers';
import { usePeriods, useTimetable } from './hooks';
import { http } from '../../lib/http';
import { useDispatch } from 'react-redux';
import { addNotification } from '../../stores/notificationSlice';
import { useQueryClient } from '@tanstack/react-query';

interface TimetableGridProps {
  classId: number;
}

export const TimetableGrid: React.FC<TimetableGridProps> = ({ classId }) => {
  const selectedClassId = classId;
  const dispatch = useDispatch();
  const queryClient = useQueryClient();
  const { data: teachersData, isLoading: teachersLoading, error: teachersError } = useTeachers({ size: 100 });
  const { data: periodsRaw, isLoading: periodsLoading, error: periodsError } = usePeriods();
  console.log('periodsError:', periodsError, 'periodsRaw:', periodsRaw);
  const periods = periodsRaw as Period[] | undefined;
  console.log('periods:', periods);
  const { data: timetableRaw, isLoading: timetableLoading } = useTimetable(selectedClassId);
  const timetable = timetableRaw as Timetable | null; // Changed from undefined to null
  const [activeTeacher, setActiveTeacher] = useState<Teacher | null>(null);
  // Local state for slot assignments (for drag-and-drop)
  const [localSlots, setLocalSlots] = useState<Record<string, TimetableSlot>>({});
  const [dragSourceSlotId, setDragSourceSlotId] = useState<string | null>(null);

  React.useEffect(() => {
    if (timetable && periods) {
      // Map slots by key for fast access
      const slotMap: Record<string, TimetableSlot> = {};
      periods.forEach((period: Period) => {
        ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'].forEach(day => {
          const slot = timetable.slots?.find((s: TimetableSlot) => s.dayOfWeek === day && s.period.id === period.id);
          slotMap[`${day}-${period.id}`] = slot || { id: 0, dayOfWeek: day, period, forClassId: selectedClassId };
        });
      });
      setLocalSlots(slotMap);
    } else if (!timetable && periods) {
      // Initialize localSlots with empty grid if no timetable exists
      const gridSlots: Record<string, TimetableSlot> = {};
      periods.forEach((period: Period) => {
        ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'].forEach(day => {
          gridSlots[`${day}-${period.id}`] = { id: 0, dayOfWeek: day, period, forClassId: selectedClassId };
        });
      });
      setLocalSlots(gridSlots);
    }
  }, [timetable, periods, selectedClassId]);
  // Teachers not assigned to any slot
  const assignedTeacherIds = Object.values(localSlots).map(s => s.teacher?.id).filter(Boolean);
  const availableTeachers = (teachersData?.data || []).filter(t => !assignedTeacherIds.includes(t.id));
  
  console.log('Assigned teacher IDs:', assignedTeacherIds);
  console.log('Available teachers count:', availableTeachers.length);
  // Drag handlers
  const handleDragStart = (event: { active: { id: number | string } }) => {
    console.log('Drag start event:', event);
    console.log('Available teachers in drag start:', availableTeachers);
    
    // If dragging from a slot, id is slot key; if from teacher list, id is teacher id (number)
    if (typeof event.active.id === 'string') {
      setDragSourceSlotId(event.active.id);
      const slot = localSlots[event.active.id];
      setActiveTeacher(slot?.teacher || null);
    } else {
      setDragSourceSlotId(null);
      const teacherId = event.active.id;
      console.log('Looking for teacher with ID:', teacherId);
      const teacher = availableTeachers.find(t => t.id === teacherId);
      console.log('Found teacher:', teacher);
      setActiveTeacher(teacher || null);
    }
  };

  const handleDragEnd = (event: { active: { id: number | string }, over: { id: string | number } | null }) => {
    const { over } = event;
    console.log('Drag end event:', { over, activeTeacher, dragSourceSlotId });
    
    if (over) {
      // Dropped on a slot
      if (typeof over.id === 'string' && activeTeacher) {
        console.log('Dropping teacher on slot:', over.id, activeTeacher);
        setLocalSlots(prev => {
          const newSlots = {
            ...prev,
            [over.id]: { ...prev[over.id], teacher: activeTeacher },
            ...(dragSourceSlotId ? { [dragSourceSlotId]: { ...prev[dragSourceSlotId], teacher: undefined } } : {}),
          };
          console.log('Updated slots:', newSlots);
          return newSlots;
        });
      }
      // Dropped on teacher list (unassign)
      if (over.id === 'teacher-list' && dragSourceSlotId) {
        console.log('Unassigning teacher from slot:', dragSourceSlotId);
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
    try {
      const slotsToSave = Object.values(localSlots).filter(s => s.teacher);
      await http.put(`/v1/timetables/class/${selectedClassId}/slots`, { slots: slotsToSave });
      dispatch(addNotification({ title: 'Success', message: 'Timetable saved successfully!', type: 'success' }));
    } catch (error) {
      console.error('Error saving timetable:', error);
      dispatch(addNotification({ title: 'Error', message: 'Error saving timetable. Please try again.', type: 'error' }));
    }
  };
  
  // Auto-generate handler
  const handleAutoGenerate = async () => {
    try {
      console.log('Starting optimization for class:', selectedClassId);
      await http.post(`/v1/timetables/class/${selectedClassId}/optimize`);
      dispatch(addNotification({ title: 'Success', message: 'Timetable optimization completed! Refreshing data...', type: 'success' }));
      // Invalidate and refetch the timetable data
      await queryClient.invalidateQueries({ queryKey: ['timetable', selectedClassId] });
    } catch (error) {
      console.error('Error starting optimization:', error);
      dispatch(addNotification({ title: 'Error', message: 'Error during optimization. Please try again or check the backend logs.', type: 'error' }));
    }
  };
  // Loading and error states
  if (teachersLoading || periodsLoading || timetableLoading) return <div>Loading...</div>;
  if (teachersError) return <div>Error loading teachers.</div>;
  if (periodsError) return <div>Error loading periods.</div>;

  // If timetable is null (404), render an empty grid
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  const slotsToUse = localSlots;
  
  // Debug logging
  console.log('Current slots:', slotsToUse);
  console.log('Available teachers:', availableTeachers);
  console.log('Sample teacher data:', availableTeachers[0]);
  console.log('Teachers data from API:', teachersData?.data);
  
  return (
    <DndContext onDragStart={handleDragStart} onDragEnd={handleDragEnd} collisionDetection={closestCenter}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
        {/* Save and Auto-generate Buttons */}
        <div style={{ marginBottom: 16, display: 'flex', gap: 12 }}>
          <button 
            onClick={handleAutoGenerate} 
            style={{ 
              padding: '10px 20px', 
              backgroundColor: '#10b981', 
              color: 'white', 
              border: 'none', 
              borderRadius: '6px', 
              cursor: 'pointer',
              fontWeight: 'bold'
            }}
          >
            Auto-generate
          </button>
          <button 
            onClick={handleSave} 
            style={{ 
              padding: '10px 20px', 
              backgroundColor: '#3b82f6', 
              color: 'white', 
              border: 'none', 
              borderRadius: '6px', 
              cursor: 'pointer',
              fontWeight: 'bold'
            }}
          >
            Save Timetable
          </button>
        </div>
        <div style={{ display: 'flex', gap: 32 }}>
          {/* Teacher List */}
          <div style={{ minWidth: 250, maxWidth: 300 }}>
            <h3 style={{ marginBottom: 16, color: '#374151' }}>Available Teachers</h3>
            <DroppableTeacherList>
              {availableTeachers.map(teacher => (
                <DraggableTeacher key={teacher.id} teacher={teacher} />
              ))}
              {availableTeachers.length === 0 && (
                <div style={{ padding: 16, textAlign: 'center', color: '#6b7280', fontStyle: 'italic' }}>
                  All teachers are assigned
                </div>
              )}
            </DroppableTeacherList>
          </div>
          {/* Timetable Grid */}
          <div style={{ flex: 1 }}>
            <h3 style={{ marginBottom: 16, color: '#374151' }}>Timetable Grid</h3>
            <div style={{ overflowX: 'auto' }}>
              <table border={1} cellPadding={8} style={{ borderCollapse: 'collapse', minWidth: 800 }}>
                <thead>
                  <tr style={{ backgroundColor: '#f3f4f6' }}>
                    <th style={{ padding: '12px 8px', border: '1px solid #d1d5db', fontWeight: 'bold' }}>Period</th>
                    {days.map(day => (
                      <th key={day} style={{ padding: '12px 8px', border: '1px solid #d1d5db', fontWeight: 'bold', minWidth: 120 }}>
                        {day}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {periods && periods.map((period: Period) => (
                    <tr key={period.id}>
                      <td style={{ padding: '8px', border: '1px solid #d1d5db', backgroundColor: '#f9fafb', fontWeight: 'bold' }}>
                        {period.label || `${period.startTime} - ${period.endTime}`}
                      </td>
                      {days.map(day => {
                        const slot = slotsToUse[`${day}-${period.id}`];
                        return (
                          <DroppableSlot
                            key={`${day}-${period.id}`}
                            id={`${day}-${period.id}`}
                            teacher={slot?.teacher}
                          />
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <DragOverlay>
          {activeTeacher ? <TeacherCard teacher={activeTeacher} /> : null}
        </DragOverlay>
      </div>
    </DndContext>
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
        padding: '12px 16px',
        margin: '8px 0',
        background: isDragging ? '#e5e7eb' : '#ffffff',
        border: '2px solid #d1d5db',
        borderRadius: '8px',
        cursor: 'grab',
        boxShadow: isDragging ? '0 4px 6px -1px rgba(0, 0, 0, 0.1)' : '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
        transition: 'all 0.2s ease',
        fontWeight: '500',
        fontSize: '14px'
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
        minHeight: 200,
        background: isOver ? '#dbeafe' : '#f8fafc',
        padding: '16px',
        borderRadius: '8px',
        border: isOver ? '2px dashed #3b82f6' : '2px dashed #d1d5db',
        transition: 'all 0.2s ease'
      }}
    >
      {children}
    </div>
  );
};

// Droppable timetable slot (for unassigning)
const DroppableSlot: React.FC<{ id: string; teacher?: Teacher }> = ({ id, teacher }) => {
  const { setNodeRef, isOver } = useDroppable({ id });
  console.log(`Slot ${id}:`, { teacher, isOver });
  
  return (
    <td
      ref={setNodeRef}
      style={{
        minWidth: 120,
        minHeight: 60,
        background: isOver ? '#dcfce7' : '#ffffff',
        textAlign: 'center',
        border: '1px solid #d1d5db',
        padding: '8px',
        transition: 'all 0.2s ease'
      }}
    >
      {teacher ? (
        <DraggableSlotTeacher id={id} teacher={teacher} />
      ) : (
        <span style={{ color: '#9ca3af', fontStyle: 'italic', fontSize: '12px' }}>
          Drop here
        </span>
      )}
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
        padding: '8px 12px',
        background: isDragging ? '#e5e7eb' : '#f3f4f6',
        border: '1px solid #d1d5db',
        borderRadius: '6px',
        cursor: 'grab',
        fontSize: '12px',
        fontWeight: '500',
        boxShadow: isDragging ? '0 4px 6px -1px rgba(0, 0, 0, 0.1)' : 'none',
        transition: 'all 0.2s ease'
      }}
    >
      {teacher.firstName} {teacher.lastName}
    </div>
  );
};

// Teacher card (used in both list and slot)
const TeacherCard: React.FC<{ teacher: Teacher }> = ({ teacher }) => (
  <div style={{ 
    padding: '8px 12px', 
    background: '#f3f4f6', 
    borderRadius: '6px', 
    border: '1px solid #d1d5db',
    fontSize: '14px',
    fontWeight: '500'
  }}>
    {teacher.firstName} {teacher.lastName}
  </div>
);

export default TimetableGrid;