import { useState, useEffect, useCallback, useMemo } from 'react';
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
  User
} from 'lucide-react';
import { useTimetable, usePeriods } from '../hooks';
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

// Simple Test Drop Zone
function TestDropZone() {
  const { isOver, setNodeRef } = useDroppable({
    id: 'test-drop-zone',
  });

  useEffect(() => {
    console.log('🧪 TEST DROP ZONE registered with ID: test-drop-zone');
  }, []);

  return (
    <div
      ref={setNodeRef}
      className={`
        p-4 border-2 border-dashed border-red-300 rounded-lg text-center transition-all duration-200
        ${isOver ? 'bg-red-100 border-red-500 scale-105' : 'hover:bg-red-50'}
      `}
    >
      <div className="text-red-600 font-bold">🧪 TEST DROP ZONE</div>
      <div className="text-xs text-red-500">Drop here to test</div>
      <div className="text-xs text-gray-500">isOver: {isOver ? 'YES' : 'NO'}</div>
    </div>
  );
}

// Draggable Teacher-Course Card
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

// Droppable Time Slot
function DroppableTimeSlot({ 
  day, 
  period, 
  slot, 
  localSlot,
  onDeleteSlot,
  onDeleteLocalSlot
}: { 
  day: DayOfWeek; 
  period: Period; 
  slot?: TimetableSlot;
  localSlot?: { teacher: Teacher; course: Course; room?: Room };
  onDeleteSlot: (slotId: number) => void;
  onDeleteLocalSlot: (slotId: string) => void;
}) {
  const slotId = `${day}-${period.id}`;
  const { isOver, setNodeRef } = useDroppable({
    id: slotId,
  });

  // Debug drop zone registration
  useEffect(() => {
    console.log('📍 DROP ZONE registered:', slotId);
  }, [slotId]);

  // Use local slot if available, otherwise use API slot
  const displaySlot = localSlot || slot;
  
  // Handle both nested and flat course color structures
  const courseColor = localSlot?.course?.color || 
                     (slot as any)?.forCourse?.color || 
                     (slot as any)?.forCourseColor || 
                     '#e5e7eb';



  return (
    <div
      ref={setNodeRef}
      className={`
        p-3 border-2 rounded-lg min-h-[120px] w-full transition-all duration-200
        ${displaySlot 
          ? 'border-solid hover:opacity-80' 
          : 'border-dashed border-gray-300 hover:border-gray-400 hover:bg-gray-50'
        }
        ${isOver ? 'ring-4 ring-blue-500 ring-opacity-75 bg-blue-100 border-blue-500 scale-105' : ''}
        relative cursor-pointer
      `}
      style={{
        backgroundColor: displaySlot ? `${courseColor}15` : (isOver ? '#dbeafe' : undefined),
        borderColor: displaySlot ? `${courseColor}60` : (isOver ? '#3b82f6' : undefined),
        pointerEvents: 'auto',
      }}

    >
      {displaySlot ? (
        <div className="space-y-2">
                          {/* Course Header */}
                          <div className="flex items-center gap-2 mb-2">
                            <div 
                              className="w-4 h-4 rounded-full flex-shrink-0"
                              style={{ backgroundColor: courseColor }}
                            />
                            <div className="flex-1 min-w-0">
                              <div className="font-semibold text-sm text-gray-800 truncate">
                                {localSlot?.course?.name || 
                                 (slot as any)?.forCourse?.name || 
                                 (slot as any)?.forCourseName || 
                                 'Unknown Course'}
                              </div>
                              <div className="text-xs text-gray-500">
                                {localSlot ? 'Local Assignment' : `Saved (ID: ${slot?.id})`}
                              </div>
                            </div>
                          </div>

          {/* Teacher */}
          <div className="flex items-center gap-1 text-sm text-gray-700">
            <User className="w-3 h-3" />
            <span className="truncate">
              {localSlot?.teacher?.firstName || 
               (slot as any)?.teacher?.firstName || 
               (slot as any)?.teacherFirstName || 
               'Unknown'} {localSlot?.teacher?.lastName || 
                          (slot as any)?.teacher?.lastName || 
                          (slot as any)?.teacherLastName || 
                          'Teacher'}
            </span>
          </div>

          {/* Room */}
          <div className="flex items-center gap-1 text-xs text-gray-600">
            <MapPin className="w-3 h-3" />
            <span className="truncate">{localSlot?.room?.name || 
                                              (slot as any)?.room?.name || 
                                              (slot as any)?.roomName || 
                                              'No Room Assigned'}</span>
          </div>

          {/* Actions */}
          <div className="flex gap-1 mt-2" style={{ pointerEvents: 'auto' }}>
            <Button
              size="sm"
              variant="outline"
              className="h-6 px-2 text-xs text-red-600 hover:text-red-700"
              onClick={(e) => {
                e.stopPropagation();
                if (localSlot) {
                  onDeleteLocalSlot(slotId);
                } else if (slot) {
                  onDeleteSlot(slot.id);
                }
              }}
              style={{ pointerEvents: 'auto' }}
            >
              <Trash2 className="w-3 h-3" />
            </Button>
          </div>
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center h-full text-gray-400">
          <Plus className="w-6 h-6" />
          <span className="text-xs mt-1 opacity-50">{slotId}</span>
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
  const [currentDropTarget, setCurrentDropTarget] = useState<string | null>(null);
  const [localSlots, setLocalSlots] = useState<Record<string, { teacher: Teacher; course: Course; room?: Room }>>({});

  // Optimized sensors configuration
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5, // Reduced for better responsiveness
      },
    }),
    useSensor(MouseSensor, {
      activationConstraint: {
        distance: 5, // Reduced for better responsiveness
      },
    })
  );



  // Data fetching hooks
  const { data: timetableData, isLoading: timetableLoading, refetch: refetchTimetable } = useTimetable(classId);
  const { data: periodsData, isLoading: periodsLoading } = usePeriods();

  // Extract and process data with safe fallbacks
  const periods = Array.isArray(periodsData) ? periodsData : [];
  const timetableSlots = useMemo(() => {
    const data = timetableData as { slots?: TimetableSlot[] } | null | undefined;
    const slots = Array.isArray(data?.slots) ? data.slots : [];
    console.log('📊 Timetable slots updated:', slots.length, 'slots');
    

    
    return slots;
  }, [timetableData]);
  const sortedPeriods = periods.sort((a: Period, b: Period) => (a.index || 0) - (b.index || 0));

  // Create slot map with useMemo for performance
  const slotMap = useMemo(() => {
    const map = new Map<string, TimetableSlot>();
    if (Array.isArray(timetableSlots)) {
      timetableSlots.forEach((slot: any) => {
        // Handle both nested period object and flat periodId structure
        const periodId = slot.period?.id || slot.periodId;
        if (periodId) {
          const key = `${slot.dayOfWeek}-${periodId}`;
          map.set(key, slot);
          
          // Handle both nested and flat course/teacher names
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

  // Fetch data
  useEffect(() => {
    const fetchData = async () => {
      try {
        // Fetch teachers
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

        // Fetch courses
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

        // Fetch rooms
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

        // Ensure arrays
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

        // Create teacher-course combinations - all teachers but only their first subject
        const combos: TeacherCourseCombo[] = [];
        if (finalTeachers.length > 0 && finalCourses.length > 0) {
          // Show ALL teachers (no limit) but only their first subject
          finalTeachers.forEach((teacher: Teacher) => {
            if (teacher && teacher.id) {
              // Each teacher gets only their FIRST subject for cleaner UI
              const firstCourse = finalCourses[0]; // Use first available course for all teachers
              if (firstCourse && firstCourse.id) {
                combos.push({
                  teacher,
                  course: firstCourse,
                  id: `teacher-${teacher.id}-course-${firstCourse.id}`
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

  // Optimized drag over handler
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
      const combo = active.data.current as TeacherCourseCombo;
      console.log('🎯 DRAG START - Setting active item:', combo);
      console.log('🎯 DRAG START - Combo teacher:', combo.teacher);
      console.log('🎯 DRAG START - Combo course:', combo.course);
      setActiveItem(combo);
    }
  }, []);

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    console.log('🎯 DRAG END - Starting drop process...');
    const { over } = event;
    
    // Use currentDropTarget if over is null (common issue with DndKit)
    const dropTargetId = over?.id || currentDropTarget;
    
    console.log('🎯 Drop target ID:', dropTargetId);
    console.log('🎯 Active item:', activeItem);
    console.log('🎯 Active item keys:', activeItem ? Object.keys(activeItem) : 'null');
    console.log('🎯 Active item type:', typeof activeItem);
    
    // Handle the nested data structure - activeItem might have a 'current' property
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
    
    // Handle test drop zone
    if (targetSlotId === 'test-drop-zone') {
      toast.success(`🎉 SUCCESS! Dropped ${actualData.teacher?.firstName} ${actualData.teacher?.lastName} teaching ${actualData.course?.name} on test zone!`);
      setActiveItem(null);
      setCurrentDropTarget(null);
      return;
    }
    
    const [day, periodIdStr] = targetSlotId.split('-');
    console.log('🎯 Parsed day:', day, 'period string:', periodIdStr);
    
    // For now, just update local state without API calls
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
    
    setActiveItem(null);
    setCurrentDropTarget(null);
  }, [activeItem, rooms, currentDropTarget]);

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
      
      for (const [slotId, slotData] of Object.entries(localSlots)) {
        const [day, periodIdStr] = slotId.split('-');
        const requestedPeriodId = Number(periodIdStr);
        
        // Find the correct period
        let actualPeriod = periods.find(p => p.id === requestedPeriodId);
        if (!actualPeriod) {
          actualPeriod = periods.find(p => p.index === requestedPeriodId);
        }
        
        if (!actualPeriod) {
          console.warn(`⚠️ Period not found for slot ${slotId}, skipping`);
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

        console.log(`💾 Preparing to save slot ${slotId}:`, {
          originalSlotId: slotId,
          day,
          periodIdStr,
          actualPeriod: { id: actualPeriod.id, index: actualPeriod.index },
          slotPayload
        });

        slotsToSave.push(slotPayload);
      }

      console.log('🎯 Saving slots:', slotsToSave);

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

      const results = await Promise.all(savePromises);
      console.log('🎯 Save results:', results);
      
      toast.success(`✅ Successfully saved ${slotsToSave.length} timetable slots!`);
      
      // Refresh the timetable data multiple times to ensure we get the latest
      console.log('🔄 Refreshing timetable data...');
      await refetchTimetable();
      
      // Wait a bit and refresh again to ensure backend has processed the save
      setTimeout(async () => {
        console.log('🔄 Second refresh to ensure data consistency...');
        await refetchTimetable();
      }, 1000);
      
      // Don't clear local slots immediately - keep them until we confirm they're in the API response
      console.log('✅ Timetable refreshed successfully');
      
      // Check if our saved slots are now in the API response
      const savedSlotKeys = Object.keys(localSlots);
      let allSlotsFound = true;
      
      savedSlotKeys.forEach(slotKey => {
        if (!slotMap.has(slotKey)) {
          console.warn(`⚠️ Saved slot ${slotKey} not found in API response`);
          allSlotsFound = false;
        }
      });
      
      // Only clear local slots if all saved slots are confirmed in API
      if (allSlotsFound && savedSlotKeys.length > 0) {
        console.log('✅ All saved slots confirmed in API, clearing local slots');
        setLocalSlots({});
      } else if (savedSlotKeys.length > 0) {
        console.log('⚠️ Some saved slots missing from API, keeping local slots for display');
      }
      
    } catch (error) {
      console.error('❌ Error saving timetable:', error);
      toast.error('❌ Failed to save timetable. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  // Auto-generate
  const handleAutoGenerate = async () => {
    if (!confirm('This will replace the existing timetable. Are you sure?')) return;

    setIsLoading(true);
    try {
      await http.post(`/v1/timetables/debug/class/${classId}/optimize`);
      toast.success('Timetable generated successfully');
      await refetchTimetable();
    } catch (error) {
      console.error('Error generating timetable:', error);
      toast.error('Failed to generate timetable');
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

          {/* Simple Test Drop Zone */}
          <TestDropZone />
          
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Users className="w-5 h-5" />
                Teachers & Subjects
                <Badge variant="secondary">{teacherCourseCombos.length}</Badge>
                {activeItem && <Badge variant="destructive">DRAGGING!</Badge>}
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
                Drag teachers from the sidebar to assign them to time slots
          </p>
        </div>
        <div className="flex gap-2">
          <Button 
            onClick={() => refetchTimetable()} 
            variant="outline"
            disabled={isLoading}
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
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
          <Button 
            onClick={handleAutoGenerate}
            disabled={isLoading}
            variant="outline"
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Auto Generate
          </Button>
        </div>
      </div>

      {/* Statistics */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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
            <div className="min-w-full">
              {/* Header */}
              <div className="grid grid-cols-7 gap-2 mb-4">
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
              <div className="space-y-2">
                      {sortedPeriods.map((period: Period) => (
                  <div key={period.id} className="grid grid-cols-7 gap-2">
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