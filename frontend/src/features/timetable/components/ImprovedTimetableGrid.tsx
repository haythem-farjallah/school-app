import { useState, useEffect, useCallback, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { 
  DndContext, 
  closestCenter,
  DragEndEvent, 
  DragStartEvent, 
  DragOverlay,
  PointerSensor,
  MouseSensor,
  useSensor,
  useSensors,
  DragOverEvent
} from '@dnd-kit/core';
import { useDraggable, useDroppable } from '@dnd-kit/core';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { 
  Clock, 
  BookOpen, 
  MapPin, 
  Plus,
  Trash2,
  RefreshCw,
  Calendar,
  AlertCircle,
  Users,
  User,
  Edit3,
  GripVertical,
  UserCheck
} from 'lucide-react';
import { useTimetable, usePeriods, useClassStudentCount } from '../hooks';
import { Teacher } from '../../../types/teacher';
import { Period } from '../../../types/period';
import { TimetableSlot, DayOfWeek } from '../../../types/timetable';
import { Course } from '../../../types/course';
import { Room } from '../../../types/room';
import { http } from '../../../lib/http';
import toast from 'react-hot-toast';

interface ImprovedTimetableGridProps {
  classId: number;
}

interface TeacherCourseCombo {
  teacher: Teacher;
  course: Course;
  id: string;
}

interface ExistingSlotData {
  teacher: Teacher;
  course: Course;
  room?: Room;
  slotId: string;
  isLocal: boolean;
  originalSlot?: TimetableSlot;
}

const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
const DAY_NAMES = {
  MONDAY: 'Monday',
  TUESDAY: 'Tuesday', 
  WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday',
  FRIDAY: 'Friday',
  SATURDAY: 'Saturday',
  SUNDAY: 'Sunday'
};


function DraggableTeacherCourse({ teacherCourse }: { teacherCourse: TeacherCourseCombo }) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    isDragging,
  } = useDraggable({
    id: teacherCourse.id,
    data: {
      current: teacherCourse
    }
  });

  // Optimized: removed excessive logging

  const transformStyle = transform ? {
    transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`,
  } : undefined;

  return (
    <div
      ref={setNodeRef}
      style={{
        ...transformStyle,
        borderLeftColor: teacherCourse.course?.color || '#9660EB',
        backgroundColor: `${teacherCourse.course?.color || '#9660EB'}15`,
        borderColor: `${teacherCourse.course?.color || '#9660EB'}60`
      }}
      {...listeners}
      {...attributes}
      className={`
        p-3 rounded-lg border-2 cursor-grab active:cursor-grabbing
        ${isDragging ? 'opacity-50 bg-blue-100' : 'hover:shadow-md'}
        border-l-4
      `}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <div 
            className="w-3 h-3 rounded-full"
            style={{ backgroundColor: teacherCourse.course?.color || '#9660EB' }}
          />
          <span className="font-medium text-sm text-gray-800">
            {teacherCourse.teacher?.firstName || 'Unknown'} {teacherCourse.teacher?.lastName || 'Teacher'}
          </span>
        </div>
        <div className="flex gap-1">
          <Badge variant="secondary" className="text-xs">
            {isDragging ? 'Dragging...' : 'Drag me!'}
          </Badge>
          <Button 
            size="sm" 
            variant="outline" 
            className="h-5 px-2 text-xs"
            onClick={(e) => {
              e.stopPropagation();
            }}
          >
            🎯
          </Button>
        </div>
      </div>
      
      <div className="space-y-1">
        <div className="flex items-center gap-1 text-xs text-gray-600">
          <BookOpen className="w-3 h-3" />
          <span className="font-medium" style={{ color: teacherCourse.course?.color || '#9660EB' }}>
            {teacherCourse.course?.name || 'Unknown Course'}
          </span>
        </div>
        <div className="flex items-center gap-1 text-xs text-gray-500">
          <Clock className="w-3 h-3" />
          <span>{teacherCourse.course?.weeklyCapacity || 3}h/week</span>
        </div>
      </div>
    </div>
  );
}

function RoomEditor({ 
  currentRoom, 
  rooms, 
  onRoomChange
}: { 
  currentRoom?: Room; 
  rooms: Room[]; 
  onRoomChange: (room: Room | undefined) => void;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [buttonRect, setButtonRect] = useState<DOMRect | null>(null);

  const handleRoomSelect = (room: Room) => {
    onRoomChange(room);
    setIsOpen(false);
    toast.success(`Room changed to ${room.name}`);
  };

  const handleRemoveRoom = () => {
    onRoomChange(undefined);
    setIsOpen(false);
    toast.success('Room removed');
  };

  const handleButtonClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    if (!isOpen) {
      const rect = e.currentTarget.getBoundingClientRect();
      setButtonRect(rect);
    }
    setIsOpen(!isOpen);
  };

  useEffect(() => {
    if (isOpen) {
      const handleClickOutside = () => setIsOpen(false);
      const handleScroll = (e: Event) => {
        const target = e.target as Element;
        if (target && target.closest('[data-dropdown="true"]')) {
          return;
        }
        setIsOpen(false);
      };
      
      document.addEventListener('click', handleClickOutside);
      window.addEventListener('scroll', handleScroll, true);
      
      return () => {
        document.removeEventListener('click', handleClickOutside);
        window.removeEventListener('scroll', handleScroll, true);
      };
    }
  }, [isOpen]);

  const dropdown = isOpen && buttonRect ? (
    <div 
      className="fixed bg-white border border-gray-200 rounded-lg shadow-lg p-2 w-[160px] z-[999999]"
      style={{ 
        top: buttonRect.bottom + 4,
        left: buttonRect.right - 160,
        zIndex: 999999,
        isolation: 'isolate'
      }}
      onClick={(e) => e.stopPropagation()}
      data-dropdown="true"
    >
      <div className="text-xs font-medium text-gray-600 mb-2">Select Room:</div>
      <div className="space-y-1 max-h-32 overflow-y-auto">
        {rooms.map((room) => (
          <button
            key={room.id}
            className="w-full text-left px-2 py-1 text-xs hover:bg-gray-100 rounded"
            onClick={() => handleRoomSelect(room)}
          >
            {room.name}
          </button>
        ))}
        <button
          className="w-full text-left px-2 py-1 text-xs text-red-600 hover:bg-red-50 rounded"
          onClick={handleRemoveRoom}
        >
          Remove Room
        </button>
      </div>
    </div>
  ) : null;

  return (
    <>
      <Button
        size="sm"
        variant="outline"
        className="h-7 px-3 text-xs text-blue-600 hover:text-blue-700 hover:bg-blue-50 border-blue-200"
        onClick={handleButtonClick}
        style={{ pointerEvents: 'auto' }}
      >
        <Edit3 className="w-3 h-3 mr-1" />
        {currentRoom ? currentRoom.name : 'Edit Room'}
      </Button>
      
      {dropdown && createPortal(dropdown, document.body)}
    </>
  );
}

function DroppableTimeSlot({ 
  day, 
  period, 
  slot, 
  localSlot,
  onDeleteSlot,
  onDeleteLocalSlot,
  onUpdateRoom,
  localSlots,
  rooms
}: { 
  day: DayOfWeek; 
  period: Period; 
  slot?: TimetableSlot;
  localSlot?: { teacher: Teacher; course: Course; room?: Room };
  onDeleteSlot: (slotId: number) => void;
  onDeleteLocalSlot: (slotId: string) => void;
  onUpdateRoom: (slotId: string, room: Room | undefined) => void;
  localSlots: Record<string, { teacher: Teacher; course: Course; room?: Room; _deleteOriginal?: boolean; _originalSlotId?: number }>;
  rooms: Room[];
}) {
  const slotId = `${day}-${period.id}`;
  const { isOver, setNodeRef } = useDroppable({
    id: slotId,
  });


  const isMarkedForDeletion = localSlots[`DELETE_${slotId}`]?._deleteOriginal;
  const displaySlot = isMarkedForDeletion ? null : (localSlot || slot);
  
  const {
    attributes: dragAttributes,
    listeners: dragListeners,
    setNodeRef: setDragRef,
    transform: dragTransform,
    isDragging: isSlotDragging,
  } = useDraggable({
    id: `existing-slot-${slotId}`,
    data: {
      type: 'existing-slot',
      slotId,
      slot: displaySlot,
      isLocal: !!localSlot,
      originalSlot: slot,
      teacher: localSlot?.teacher || (slot as any)?.teacher || {
        firstName: (slot as any)?.teacherFirstName || (slot as any)?.teacher?.firstName,
        lastName: (slot as any)?.teacherLastName || (slot as any)?.teacher?.lastName,
        id: (slot as any)?.teacherId || (slot as any)?.teacher?.id
      },
      course: localSlot?.course || (slot as any)?.forCourse || {
        name: (slot as any)?.forCourseName || (slot as any)?.forCourse?.name,
        id: (slot as any)?.forCourseId || (slot as any)?.forCourse?.id,
        color: (slot as any)?.forCourseColor || (slot as any)?.forCourse?.color
      },
      room: localSlot?.room || (slot as any)?.room || {
        name: (slot as any)?.roomName || (slot as any)?.room?.name,
        id: (slot as any)?.roomId || (slot as any)?.room?.id
      }
    },
    disabled: !displaySlot
  });
  
  const courseColor = localSlot?.course?.color || 
                     (slot as any)?.forCourse?.color || 
                     (slot as any)?.forCourseColor || 
                     '#e5e7eb';

  const dragTransformStyle = dragTransform ? {
    transform: `translate3d(${dragTransform.x}px, ${dragTransform.y}px, 0)`,
  } : undefined;



  return (
    <div
      ref={(node) => {
        setNodeRef(node);
        setDragRef(node);
      }}
      className={`
        p-4 border-2 rounded-xl min-h-[140px] w-full min-w-[180px] transition-all duration-300 ease-in-out
        ${displaySlot 
          ? 'border-solid shadow-md hover:shadow-lg hover:scale-[1.02] bg-white' 
          : 'border-dashed border-gray-300 hover:border-gray-400 hover:bg-gray-50'
        }
        ${isOver ? 'ring-4 ring-blue-500 ring-opacity-75 bg-blue-50 border-blue-500 scale-105 shadow-xl' : ''}
        ${isSlotDragging ? 'opacity-60 bg-blue-50 shadow-xl' : ''}
        relative cursor-pointer group
      `}
      style={{
        backgroundColor: displaySlot ? `${courseColor}08` : (isOver ? '#dbeafe' : undefined),
        borderColor: displaySlot ? `${courseColor}80` : (isOver ? '#3b82f6' : undefined),
        borderLeftWidth: displaySlot ? '4px' : '2px',
        borderLeftColor: displaySlot ? courseColor : undefined,
        pointerEvents: 'auto',
        ...dragTransformStyle,
      }}
      {...(displaySlot ? dragListeners : {})}
      {...(displaySlot ? dragAttributes : {})}
    >
      {displaySlot ? (
        <div className="space-y-3">
          <div className="flex items-start gap-3 mb-3">
            <div 
              className="w-5 h-5 rounded-full flex-shrink-0 shadow-sm"
              style={{ backgroundColor: courseColor }}
            />
            <div className="flex-1 min-w-0">
              <div className="font-bold text-lg text-gray-900 leading-tight">
                {localSlot?.course?.name || 
                 (slot as any)?.forCourse?.name || 
                 (slot as any)?.forCourseName || 
                 'Unknown Course'}
              </div>
            </div>
            {displaySlot && (
              <div className="flex items-center justify-center w-6 h-6 rounded-full bg-gray-100 hover:bg-gray-200 transition-colors cursor-grab active:cursor-grabbing">
                <GripVertical className="w-4 h-4 text-gray-500" />
              </div>
            )}
          </div>

          <div className="flex items-center gap-2 text-sm text-gray-800 bg-gray-50 rounded-lg p-2">
            <User className="w-4 h-4 text-gray-600" />
            <span className="font-medium truncate">
              {localSlot?.teacher?.firstName || 
               (slot as any)?.teacher?.firstName || 
               (slot as any)?.teacherFirstName || 
               'Unknown'} {localSlot?.teacher?.lastName || 
                          (slot as any)?.teacher?.lastName || 
                          (slot as any)?.teacherLastName || 
                          'Teacher'}
            </span>
          </div>

          <div className="flex items-center gap-2 text-sm text-gray-700 bg-gray-50 rounded-lg p-2">
            <div className="flex items-center gap-2 flex-1 min-w-0">
              <MapPin className="w-4 h-4 text-gray-600 flex-shrink-0" />
              <span className="font-medium truncate">
                {localSlot?.room?.name || 
                 (slot as any)?.room?.name || 
                 (slot as any)?.roomName || 
                 'No Room Assigned'}
              </span>
            </div>
          </div>


          <div className="flex justify-end gap-2 mt-3 pt-2 border-t border-gray-200" style={{ pointerEvents: 'auto' }}>
            <RoomEditor
              currentRoom={localSlot?.room || (slot as any)?.room || {
                name: (slot as any)?.roomName || (slot as any)?.room?.name,
                id: (slot as any)?.roomId || (slot as any)?.room?.id
              }}
              rooms={rooms}
              onRoomChange={(room) => onUpdateRoom(slotId, room)}
            />
            <button
              className="p-2 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors"
              onClick={(e) => {
                e.stopPropagation();
                if (localSlot) {
                  onDeleteLocalSlot(slotId);
                } else if (slot) {
                  onDeleteSlot(slot.id);
                }
              }}
              style={{ pointerEvents: 'auto' }}
              title="Remove slot"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center h-full text-gray-400 group-hover:text-gray-600 transition-colors">
          <div className="w-12 h-12 rounded-full bg-gray-100 group-hover:bg-gray-200 flex items-center justify-center mb-2 transition-colors">
            <Plus className="w-6 h-6" />
          </div>
          <span className="text-sm font-medium">Drop here</span>
          <span className="text-xs mt-1 opacity-60">{slotId}</span>
        </div>
      )}
    </div>
  );
}

export function ImprovedTimetableGrid({ classId }: ImprovedTimetableGridProps) {
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [teacherCourseCombos, setTeacherCourseCombos] = useState<TeacherCourseCombo[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [activeItem, setActiveItem] = useState<TeacherCourseCombo | null>(null);
  const [activeExistingSlot, setActiveExistingSlot] = useState<ExistingSlotData | null>(null);
  const [currentDropTarget, setCurrentDropTarget] = useState<string | null>(null);
  const [localSlots, setLocalSlots] = useState<Record<string, { teacher: Teacher; course: Course; room?: Room; _deleteOriginal?: boolean; _originalSlotId?: number }>>({});

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5,
      },
    }),
    useSensor(MouseSensor, {
      activationConstraint: {
        distance: 5,
      },
    })
  );



  const { data: timetableData, isLoading: timetableLoading, refetch: refetchTimetable } = useTimetable(classId);
  const { data: periodsData, isLoading: periodsLoading } = usePeriods();
  const { data: studentCount } = useClassStudentCount(classId);

  const periods = Array.isArray(periodsData) ? periodsData : [];
  const timetableSlots = useMemo(() => {
    const data = timetableData as { slots?: TimetableSlot[] } | null | undefined;
    const slots = Array.isArray(data?.slots) ? data.slots : [];
    console.log('📊 Timetable slots updated:', slots.length, 'slots');
    

    
    return slots;
  }, [timetableData]);
  const sortedPeriods = periods.sort((a: Period, b: Period) => (a.index || 0) - (b.index || 0));

  const slotMap = useMemo(() => {
    const map = new Map<string, TimetableSlot>();
    if (Array.isArray(timetableSlots)) {
      timetableSlots.forEach((slot: any) => {
        const periodId = slot.period?.id || slot.periodId;
        if (periodId) {
          const key = `${slot.dayOfWeek}-${periodId}`;
          map.set(key, slot);
          
          const courseName = (slot as any).forCourse?.name || (slot as any).forCourseName || 'Unknown Course';
          const teacherName = (slot as any).teacher?.firstName || (slot as any).teacherFirstName || 'Unknown Teacher';
          
          console.log(`📍 Added slot to map: ${key} ->`, courseName, 'by', teacherName);
        } else {
          console.warn('⚠️ Slot missing period information:', slot);
        }
      });
    }
    console.log('🗺️ Slot map created with', map.size, 'entries');
    return map;
  }, [timetableSlots]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        let teachersData: Teacher[] = [];
        try {
          console.log('🔍 Fetching teachers from /admin/teachers...');
          const teachersResponse = await http.get('/admin/teachers?size=100');
          console.log('📥 Teachers API response:', teachersResponse);
          console.log('📥 Teachers response type:', typeof teachersResponse);
          console.log('📥 Teachers response keys:', Object.keys(teachersResponse || {}));
          
          // Try different data extraction methods
          if (teachersResponse?.data) {
            if (Array.isArray(teachersResponse.data)) {
              teachersData = teachersResponse.data;
              console.log('✅ Found teachers in response.data (array):', teachersData.length);
            } else if (teachersResponse.data.content && Array.isArray(teachersResponse.data.content)) {
              teachersData = teachersResponse.data.content;
              console.log('✅ Found teachers in response.data.content:', teachersData.length);
            } else {
              console.log('❌ Teachers response.data structure:', teachersResponse.data);
            }
          } else if (Array.isArray(teachersResponse)) {
            teachersData = teachersResponse;
            console.log('✅ Found teachers in direct response:', teachersData.length);
          } else {
            console.log('❌ Unexpected teachers response structure:', teachersResponse);
          }
        } catch (error) {
          console.error('❌ Error fetching teachers:', error);
        }

        let coursesData: Course[] = [];
        try {
          console.log('🔍 Fetching courses from /v1/courses...');
          const coursesResponse = await http.get('/v1/courses?size=100');
          console.log('📥 Courses API response:', coursesResponse);
          console.log('📥 Courses response type:', typeof coursesResponse);
          console.log('📥 Courses response keys:', Object.keys(coursesResponse || {}));
          
          // Try different data extraction methods
          if (coursesResponse?.data) {
            if (Array.isArray(coursesResponse.data)) {
              coursesData = coursesResponse.data;
              console.log('✅ Found courses in response.data (array):', coursesData.length);
            } else if (coursesResponse.data.content && Array.isArray(coursesResponse.data.content)) {
              coursesData = coursesResponse.data.content;
              console.log('✅ Found courses in response.data.content:', coursesData.length);
            } else {
              console.log('❌ Courses response.data structure:', coursesResponse.data);
            }
          } else if (Array.isArray(coursesResponse)) {
            coursesData = coursesResponse;
            console.log('✅ Found courses in direct response:', coursesData.length);
          } else {
            console.log('❌ Unexpected courses response structure:', coursesResponse);
          }
        } catch (error) {
          console.error('❌ Error fetching courses:', error);
        }

        let roomsData: Room[] = [];
        try {
          console.log('🔍 Fetching rooms from /v1/rooms...');
          const roomsResponse = await http.get('/v1/rooms?size=100');
          console.log('📥 Rooms API response:', roomsResponse);
          console.log('📥 Rooms response type:', typeof roomsResponse);
          console.log('📥 Rooms response keys:', Object.keys(roomsResponse || {}));
          
          // Try different data extraction methods
          if (roomsResponse?.data) {
            if (Array.isArray(roomsResponse.data)) {
              roomsData = roomsResponse.data;
              console.log('✅ Found rooms in response.data (array):', roomsData.length);
            } else if (roomsResponse.data.content && Array.isArray(roomsResponse.data.content)) {
              roomsData = roomsResponse.data.content;
              console.log('✅ Found rooms in response.data.content:', roomsData.length);
            } else {
              console.log('❌ Rooms response.data structure:', roomsResponse.data);
            }
          } else if (Array.isArray(roomsResponse)) {
            roomsData = roomsResponse;
            console.log('✅ Found rooms in direct response:', roomsData.length);
          } else {
            console.log('❌ Unexpected rooms response structure:', roomsResponse);
          }
        } catch (error) {
          console.error('❌ Error fetching rooms:', error);
        }

        const finalTeachers = Array.isArray(teachersData) ? teachersData : [];
        const finalCourses = Array.isArray(coursesData) ? coursesData : [];
        const finalRooms = Array.isArray(roomsData) ? roomsData : [];

        setTeachers(finalTeachers);
        setCourses(finalCourses);
        setRooms(finalRooms);
        
        console.log('📊 Final data loaded:');
        console.log('👨‍🏫 Teachers:', finalTeachers.map(t => ({ id: t.id, name: `${t.firstName} ${t.lastName}` })));
        console.log('📚 Courses:', finalCourses.map(c => ({ id: c.id, name: c.name, color: c.color })));
        console.log('🏢 Rooms:', finalRooms.map(r => ({ id: r.id, name: r.name })));

        const combos: TeacherCourseCombo[] = [];
        if (finalTeachers.length > 0 && finalCourses.length > 0) {
          finalTeachers.forEach((teacher: Teacher, index: number) => {
            if (teacher && teacher.id) {
              const courseIndex = index % finalCourses.length;
              const assignedCourse = finalCourses[courseIndex];
              
              if (assignedCourse && assignedCourse.id) {
                combos.push({
                  teacher,
                  course: assignedCourse,
                  id: `teacher-${teacher.id}-course-${assignedCourse.id}`
                });
              }
            }
          });
        }

        setTeacherCourseCombos(combos);

      } catch (error) {
        console.error('Error fetching data:', error);
        toast.error('Failed to load data');
      }
    };

    fetchData();
  }, []);

  const handleDragOver = useCallback((event: DragOverEvent) => {
    const dropTargetId = event.over?.id;
    console.log('🎯 DRAG OVER - Target ID:', dropTargetId);
    if (dropTargetId !== currentDropTarget) {
      const newTarget = dropTargetId ? String(dropTargetId) : null;
      console.log('🎯 Setting current drop target:', newTarget);
      setCurrentDropTarget(newTarget);
    }
  }, [currentDropTarget]);

  const handleDragStart = useCallback((event: DragStartEvent) => {
    const { active } = event;
    console.log('🎯 DRAG START - Active:', active);
    console.log('🎯 DRAG START - Active data:', active.data);
    
    if (active.data.current) {
      const data = active.data.current;
      
      if (data.type === 'existing-slot') {
        const existingSlotData: ExistingSlotData = {
          teacher: data.teacher,
          course: data.course,
          room: data.room,
          slotId: data.slotId,
          isLocal: data.isLocal,
          originalSlot: data.originalSlot
        };
        console.log('🎯 DRAG START - Setting active existing slot:', existingSlotData);
        console.log('🎯 DRAG START - Teacher:', existingSlotData.teacher);
        console.log('🎯 DRAG START - Course:', existingSlotData.course);
        console.log('🎯 DRAG START - Room:', existingSlotData.room);
        setActiveExistingSlot(existingSlotData);
        setActiveItem(null);
      } else {
        const combo = data.current as TeacherCourseCombo;
        console.log('🎯 DRAG START - Setting active item:', combo);
        console.log('🎯 DRAG START - Combo teacher:', combo.teacher);
        console.log('🎯 DRAG START - Combo course:', combo.course);
        setActiveItem(combo);
        setActiveExistingSlot(null);
      }
    }
  }, []);

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    console.log('🎯 DRAG END - Starting drop process...');
    const { over } = event;
    
    const dropTargetId = over?.id || currentDropTarget;
    
    console.log('🎯 Drop target ID:', dropTargetId);
    console.log('🎯 Active item:', activeItem);
    console.log('🎯 Active existing slot:', activeExistingSlot);
    
    if (activeExistingSlot && dropTargetId) {
      const targetSlotId = String(dropTargetId);
      
      if (targetSlotId === activeExistingSlot.slotId) {
        console.log('❌ Cannot drop on same slot');
        setActiveExistingSlot(null);
        setCurrentDropTarget(null);
        return;
      }
      
      console.log('🎯 Moving existing slot from', activeExistingSlot.slotId, 'to', targetSlotId);
      
      try {
        setLocalSlots(prev => {
          const newSlots = { ...prev };
          
          if (activeExistingSlot.isLocal) {
            delete newSlots[activeExistingSlot.slotId];
          } else {
            newSlots[`DELETE_${activeExistingSlot.slotId}`] = {
              _deleteOriginal: true,
              _originalSlotId: activeExistingSlot.originalSlot?.id,
              teacher: activeExistingSlot.teacher,
              course: activeExistingSlot.course,
              room: activeExistingSlot.room
            };
          }
          
          newSlots[targetSlotId] = {
            teacher: activeExistingSlot.teacher,
            course: activeExistingSlot.course,
            room: activeExistingSlot.room
          };
          
          return newSlots;
        });
        
        toast.success(`✅ Moved ${activeExistingSlot.course?.name || 'course'} to ${targetSlotId}`);
        console.log('✅ Existing slot move completed successfully!');
        
      } catch (error) {
        console.error('❌ Error moving existing slot:', error);
        toast.error(`❌ Failed to move slot`);
      }
    }
    else if (activeItem && dropTargetId) {
      let actualData = activeItem;
      if (activeItem && 'current' in activeItem) {
        actualData = (activeItem as { current: TeacherCourseCombo }).current;
      }
      console.log('🎯 Actual data:', actualData);
      console.log('🎯 Actual data teacher:', actualData?.teacher);
      console.log('🎯 Actual data course:', actualData?.course);
      
      if (!dropTargetId || !actualData || !actualData.course || !actualData.teacher) {
        console.log('❌ Drop failed - missing data:', { 
          dropTargetId: !!dropTargetId, 
          activeItem: !!activeItem, 
          actualData: !!actualData,
          course: !!actualData?.course, 
          teacher: !!actualData?.teacher 
        });
        setActiveItem(null);
        setCurrentDropTarget(null);
        return;
      }

    const targetSlotId = String(dropTargetId);
    console.log('🎯 Target slot ID:', targetSlotId);
    
    const [day, periodIdStr] = targetSlotId.split('-');
    console.log('🎯 Parsed day:', day, 'period string:', periodIdStr);
    
    try {
      const defaultRoom = rooms.length > 0 ? rooms[0] : undefined;
      
      // Update local slots state
      setLocalSlots(prev => ({
        ...prev,
        [targetSlotId]: {
          teacher: actualData.teacher,
          course: actualData.course,
          room: defaultRoom
        }
      }));
      
      toast.success(`✅ Added ${actualData.course?.name} with ${actualData.teacher?.firstName} ${actualData.teacher?.lastName} to ${day} ${periodIdStr}`);
      console.log('✅ Drop process completed successfully!');
      
    } catch (error) {
      console.error('❌ Error updating slot:', error);
      toast.error(`❌ Failed to update slot`);
    }
    }
    
    setActiveItem(null);
    setActiveExistingSlot(null);
    setCurrentDropTarget(null);
  }, [activeItem, activeExistingSlot, rooms, currentDropTarget]);

  // Delete slot
  const handleDeleteSlot = async (slotId: number) => {
    if (!confirm('Are you sure you want to delete this slot?')) return;

    setIsLoading(true);
    try {
      await http.delete(`/v1/timetables/slots/${slotId}`);
      toast.success('Slot deleted successfully');
      await refetchTimetable();
    } catch (error) {
      console.error('Error deleting slot:', error);
      toast.error('Failed to delete slot');
    } finally {
      setIsLoading(false);
    }
  };

  // Delete local slot
  const handleDeleteLocalSlot = (slotId: string) => {
    setLocalSlots(prev => {
      const newSlots = { ...prev };
      delete newSlots[slotId];
      return newSlots;
    });
    toast.success('Local assignment removed');
  };

  // Update room for a slot
  const handleUpdateRoom = (slotId: string, room: Room | undefined) => {
    setLocalSlots(prev => {
      const newSlots = { ...prev };
      if (newSlots[slotId]) {
        newSlots[slotId] = {
          ...newSlots[slotId],
          room
        };
      }
      return newSlots;
    });
  };

  // Save timetable
  const handleSaveTimetable = async () => {
    if (Object.keys(localSlots).length === 0) {
      toast.error('No changes to save');
      return;
    }

    if (!confirm(`Save ${Object.keys(localSlots).length} timetable changes? This will create/update slots in the database.`)) {
      return;
    }

    setIsLoading(true);
    try {
      const slotsToSave = [];
      const slotsToDelete = [];
      
      for (const [slotId, slotData] of Object.entries(localSlots)) {
        // Handle deletion markers for moved slots
        if (slotId.startsWith('DELETE_') && slotData._deleteOriginal) {
          slotsToDelete.push({
            slotId: slotData._originalSlotId,
            originalSlotKey: slotId.replace('DELETE_', '')
          });
          continue;
        }
        
        // Skip deletion markers
        if (slotId.startsWith('DELETE_')) {
          continue;
        }
        
        const [day, periodIdStr] = slotId.split('-');
        const requestedPeriodId = Number(periodIdStr);
        
        // Find the correct period
        let actualPeriod = periods.find(p => p.id === requestedPeriodId);
        if (!actualPeriod) {
          actualPeriod = periods.find(p => p.index === requestedPeriodId);
        }
        
        if (!actualPeriod) {
          continue;
        }

        const slotPayload = {
          dayOfWeek: day as DayOfWeek,
          periodId: actualPeriod.id,
          forClassId: classId,
          forCourseId: slotData.course?.id || null,
          teacherId: slotData.teacher?.id || null,
          roomId: slotData.room?.id || null,
          description: `${slotData.course?.name || 'Course'} - ${slotData.teacher?.firstName || 'Teacher'} ${slotData.teacher?.lastName || ''}`
        };


        slotsToSave.push(slotPayload);
      }


      // Delete original slots first (for moved slots)
      const deletePromises = slotsToDelete.map(deleteData => {
        return http.delete(`/v1/timetables/slots/${deleteData.slotId}`);
      });
      
      if (deletePromises.length > 0) {
        await Promise.all(deletePromises);
      }

      // Save all slots
      const savePromises = slotsToSave.map(slotData => {
        // Check if slot already exists
        const existingSlot = slotMap.get(`${slotData.dayOfWeek}-${slotData.periodId}`);
        
        if (existingSlot) {
          return http.put(`/v1/timetables/slots/${existingSlot.id}`, slotData);
        } else {
          return http.post('/v1/timetables/slots', slotData);
        }
      });

      await Promise.all(savePromises);
      
      toast.success(`✅ Successfully saved ${slotsToSave.length} timetable slots!`);
      
      // Clear local slots immediately after successful save
      setLocalSlots({});
      
      // Refresh the timetable data to get the latest from API
      await refetchTimetable();
      
    } catch (error) {
      console.error('Error saving timetable:', error);
      toast.error('Failed to save timetable. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };


  if (timetableLoading || periodsLoading) {
    return (
      <Card>
        <CardContent className="p-8">
          <div className="flex items-center justify-center">
            <RefreshCw className="w-6 h-6 animate-spin mr-2" />
            <span>Loading timetable data...</span>
          </div>
        </CardContent>
      </Card>
    );
  }



  // Optimized: removed excessive logging

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragStart={handleDragStart}
      onDragOver={handleDragOver}
      onDragEnd={handleDragEnd}
    >
      <div className="grid grid-cols-1 xl:grid-cols-4 gap-6">
        {/* Teacher Sidebar */}
        <div className="xl:col-span-1 space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Users className="w-5 h-5" />
                Teachers & Subjects
                <Badge variant="secondary">{teacherCourseCombos.length}</Badge>
                {activeItem && <Badge variant="destructive">DRAGGING!</Badge>}
                {activeExistingSlot && <Badge variant="destructive">MOVING SLOT!</Badge>}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 max-h-[600px] overflow-y-auto">
              {teacherCourseCombos.map((combo) => (
                <DraggableTeacherCourse 
                  key={combo.id}
                  teacherCourse={combo} 
                />
              ))}
              
              {teacherCourseCombos.length === 0 && (
                <div className="text-center py-8 text-gray-500">
                  <Users className="w-12 h-12 mx-auto mb-2 opacity-50" />
                  <p>No teachers or courses found</p>
                  <p className="text-xs mt-2">Teachers: {teachers?.length || 0}, Courses: {courses?.length || 0}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Timetable Grid */}
        <div className="xl:col-span-3 space-y-4">
          {/* Header */}
      <div className="flex items-center justify-between">
        <div>
              <h2 className="text-2xl font-bold text-gray-900">Enhanced Timetable</h2>
          <p className="text-gray-600">
                Drag teachers from the sidebar to assign them to time slots, or drag existing slots to move them
          </p>
        </div>
        <div className="flex gap-2">
          <Button 
            onClick={handleSaveTimetable}
            disabled={isLoading || Object.keys(localSlots).length === 0}
            className={Object.keys(localSlots).length === 0 ? "bg-gray-400" : "bg-green-600 hover:bg-green-700"}
          >
            <Calendar className="w-4 h-4 mr-2" />
            {Object.keys(localSlots).length === 0 
              ? "All Saved ✓" 
              : `Save Changes (${Object.keys(localSlots).length})`
            }
          </Button>
        </div>
      </div>

      {/* Statistics */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Total Slots</p>
                    <p className="text-2xl font-bold text-blue-600">{timetableSlots?.length || 0}</p>
              </div>
              <Clock className="w-8 h-8 text-blue-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Teachers</p>
                    <p className="text-2xl font-bold text-green-600">{teachers?.length || 0}</p>
              </div>
                  <Users className="w-8 h-8 text-green-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Courses</p>
                    <p className="text-2xl font-bold text-purple-600">{courses?.length || 0}</p>
              </div>
              <BookOpen className="w-8 h-8 text-purple-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Students</p>
                <p className="text-2xl font-bold text-orange-600">
                  {studentCount || 0}
                </p>
              </div>
              <UserCheck className="w-8 h-8 text-orange-600" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Timetable Grid */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="w-5 h-5" />
                Weekly Schedule ({sortedPeriods.length} periods)
          </CardTitle>
        </CardHeader>
        <CardContent>
              {sortedPeriods.length === 0 ? (
                <div className="text-center py-8">
                  <AlertCircle className="w-12 h-12 mx-auto mb-4 text-red-500" />
                  <p className="text-red-600 font-medium">No periods found!</p>
                  <p className="text-gray-600 text-sm">Please create periods first to use the timetable.</p>
                </div>
              ) : (
          <div className="overflow-x-auto">
            <div className="min-w-[800px]">
              {/* Header */}
              <div className="grid grid-cols-7 gap-3 mb-4">
                <div className="font-semibold text-sm text-gray-600 p-3 bg-gray-50 rounded">
                  Time
                </div>
                {DAYS.map(day => (
                  <div key={day} className="font-semibold text-sm text-gray-600 p-3 bg-gray-50 rounded text-center">
                    {DAY_NAMES[day]}
                  </div>
                ))}
              </div>

              {/* Schedule Grid */}
              <div className="space-y-3">
                      {sortedPeriods.map((period: Period) => (
                  <div key={period.id} className="grid grid-cols-7 gap-3">
                    {/* Time Column */}
                    <div className="p-3 text-sm font-medium text-gray-700 bg-gray-50 rounded border">
                      <div className="font-semibold">Period {period.index}</div>
                      <div className="text-xs text-gray-500">
                        {period.startTime} - {period.endTime}
                      </div>
                    </div>

                    {/* Day Columns */}
                    {DAYS.map(day => {
                      const slotKey = `${day}-${period.id}`;
                      const slot = slotMap.get(slotKey);
                      const localSlot = localSlots[slotKey];



                      return (
                              <DroppableTimeSlot
                                key={slotKey}
                                day={day}
                                period={period}
                                slot={slot}
                                localSlot={localSlot}
                                onDeleteSlot={handleDeleteSlot}
                                onDeleteLocalSlot={handleDeleteLocalSlot}
                                onUpdateRoom={handleUpdateRoom}
                                localSlots={localSlots}
                                rooms={rooms}
                              />
                      );
                    })}
                  </div>
                ))}
              </div>
            </div>
          </div>

              )}
        </CardContent>
      </Card>
              </div>
              </div>

      {/* Drag Overlay */}
      <DragOverlay dropAnimation={null}>
        {(() => {
          // Handle existing slot overlay
          if (activeExistingSlot) {
            return (
              <div 
                className="p-3 rounded-lg border-2 shadow-lg bg-white opacity-90 pointer-events-none"
                style={{ 
                  transform: 'rotate(5deg)',
                  pointerEvents: 'none',
                  borderLeftColor: activeExistingSlot.course?.color || '#9660EB',
                  backgroundColor: `${activeExistingSlot.course?.color || '#9660EB'}15`,
                  borderColor: `${activeExistingSlot.course?.color || '#9660EB'}60`
                }}
              >
                <div className="flex items-center gap-2">
                  <div 
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: activeExistingSlot.course?.color || '#9660EB' }}
                  />
                  <span className="font-medium text-sm">
                    {activeExistingSlot.teacher?.firstName} {activeExistingSlot.teacher?.lastName}
                  </span>
                </div>
                <div className="text-xs text-gray-600 mt-1">
                  {activeExistingSlot.course?.name}
                </div>
                <div className="text-xs text-gray-500 mt-1">
                  Moving existing slot...
                </div>
              </div>
            );
          }
          
          // Handle teacher-course combo overlay
          let overlayData = activeItem;
          if (activeItem && 'current' in activeItem) {
            overlayData = (activeItem as { current: TeacherCourseCombo }).current;
          }
          return overlayData && overlayData.course && overlayData.teacher ? (
            <div 
              className="p-3 rounded-lg border-2 shadow-lg bg-white opacity-90 pointer-events-none"
              style={{ 
                transform: 'rotate(5deg)',
                pointerEvents: 'none',
                borderLeftColor: overlayData.course.color || '#9660EB',
                backgroundColor: `${overlayData.course.color || '#9660EB'}15`,
                borderColor: `${overlayData.course.color || '#9660EB'}60`
              }}
            >
              <div className="flex items-center gap-2">
                <div 
                  className="w-3 h-3 rounded-full"
                  style={{ backgroundColor: overlayData.course.color || '#9660EB' }}
                />
                <span className="font-medium text-sm">
                  {overlayData.teacher.firstName} {overlayData.teacher.lastName}
                </span>
              </div>
              <div className="text-xs text-gray-600 mt-1">
                {overlayData.course.name}
              </div>
            </div>
          ) : null;
        })()}
      </DragOverlay>
    </DndContext>
  );
}