import React, { useState, useCallback, useRef } from 'react';
import { DndContext, closestCenter, DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import { useDroppable, useDraggable, DragOverlay } from '@dnd-kit/core';

import { useDispatch } from 'react-redux';
import { addNotification } from '../../stores/notificationSlice';
import { http } from '../../lib/http';
import { useTimetable, usePeriods } from './hooks';
import { useTeachers } from '../teachers/hooks/use-teachers';
import { Teacher } from '../../types/teacher';
import { Period } from '../../types/period';
import { Timetable, TimetableSlot } from '../../types/timetable';
import { Course } from '../../types/course';
import { Room } from '../../types/room';

interface TimetableGridProps {
  classId: number;
}

interface EnhancedSlot {
  id: string;
  teacher?: Teacher;
  course?: Course;
  room?: Room;
  description?: string;
  isMultiPeriod?: boolean;
  periodSpan?: number;
  isPartOfMultiPeriod?: boolean;
  masterSlotId?: string;
}

interface ProcessedSlotData {
  [key: string]: EnhancedSlot | null;
}


// Function to process slots for multi-period display
const processMultiPeriodSlots = (slots: TimetableSlot[], periods: Period[]): ProcessedSlotData => {
  const processed: ProcessedSlotData = {};
  const slotsByDay: { [day: string]: TimetableSlot[] } = {};
  
  // Group slots by day
  slots.forEach(slot => {
    const day = slot.dayOfWeek;
    if (!slotsByDay[day]) slotsByDay[day] = [];
    slotsByDay[day].push(slot);
  });
  
  // Process each day
  Object.keys(slotsByDay).forEach(day => {
    const daySlots = slotsByDay[day].sort((a, b) => 
      (a.period?.index || 0) - (b.period?.index || 0)
    );
    
    // Detect multi-period courses
    for (let i = 0; i < daySlots.length; i++) {
      const currentSlot = daySlots[i];
      const slotKey = `${day}-${currentSlot.period?.id}`;
      
      // Skip if this slot was already processed as part of a multi-period course
      if (slotKey in processed) {
        continue;
      }
      
      // Check if this is the start of a multi-period course
      let consecutiveCount = 1;
      let j = i + 1;
      
      while (j < daySlots.length) {
        const nextSlot = daySlots[j];
        const expectedPeriodIndex = (currentSlot.period?.index || 0) + consecutiveCount;
        
        if (nextSlot.forCourse?.id === currentSlot.forCourse?.id &&
            nextSlot.teacher?.id === currentSlot.teacher?.id &&
            (nextSlot.period?.index || 0) === expectedPeriodIndex) {
          consecutiveCount++;
          j++;
        } else {
          break;
        }
      }
      
      if (consecutiveCount > 1) {
        // This is a multi-period course
        console.log(`Found multi-period course: ${currentSlot.forCourse?.name} with ${consecutiveCount} periods on ${day}`);
        processed[slotKey] = {
          id: slotKey,
          teacher: currentSlot.teacher,
          course: currentSlot.forCourse,
          room: currentSlot.room,
          description: `${currentSlot.forCourse?.name || 'Course'} (${consecutiveCount} periods)`,
          isMultiPeriod: true,
          periodSpan: consecutiveCount
        };
        
        // Mark subsequent periods as part of this multi-period course
        for (let k = 1; k < consecutiveCount; k++) {
          const followupSlot = daySlots[i + k];
          const followupKey = `${day}-${followupSlot.period?.id}`;
          console.log(`Marking ${followupKey} as null (part of multi-period)`);
          processed[followupKey] = null; // Will be skipped in rendering
        }
        
        i += consecutiveCount - 1; // Skip the processed slots
      } else {
        // Single period course
        processed[slotKey] = {
          id: slotKey,
          teacher: currentSlot.teacher,
          course: currentSlot.forCourse,
          room: currentSlot.room,
          description: currentSlot.forCourse?.name || currentSlot.description || 'Free Period',
          isMultiPeriod: false,
          periodSpan: 1
        };
      }
    }
  });
  
  // Fill empty slots
  periods.forEach((period: Period) => {
    ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'].forEach(day => {
      const slotKey = `${day}-${period.id}`;
      if (!(slotKey in processed)) {
        processed[slotKey] = {
          id: slotKey,
          isMultiPeriod: false,
          periodSpan: 1
        };
      }
    });
  });
  
  return processed;
};

export const TimetableGrid: React.FC<TimetableGridProps> = ({ classId }) => {
  const selectedClassId = classId;
  const dispatch = useDispatch();

  
  // Data fetching
  const { data: teachersData, isLoading: teachersLoading, error: teachersError } = useTeachers();
  const { data: periodsRaw, isLoading: periodsLoading, error: periodsError } = usePeriods();
  const { data: timetableRaw, isLoading: timetableLoading, refetch } = useTimetable(selectedClassId);
  
  const periods = periodsRaw as Period[] | undefined;
  console.log('periods:', periods);
  const timetable = timetableRaw as Timetable | null; // Changed from undefined to null
  const [activeTeacher, setActiveTeacher] = useState<Teacher | null>(null);
  const [dragSourceSlotId, setDragSourceSlotId] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [hasChanges, setHasChanges] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [activeSlot, setActiveSlot] = useState<EnhancedSlot | null>(null);
  const [contextMenu, setContextMenu] = useState<{ x: number; y: number; slotId: string } | null>(null);
  const contextMenuRef = useRef<HTMLDivElement>(null);

  // Extract teachers from the response
  const availableTeachers = teachersData?.data || [];
  console.log('Available teachers count:', availableTeachers.length);
  
  // Drag handlers
  const handleDragStart = (event: DragStartEvent) => {
    console.log('Drag start event:', event);
    const activeId = event.active.id;
    
    // Check if dragging a teacher from sidebar
    if (typeof activeId === 'number') {
      const teacher = availableTeachers.find((t: Teacher) => t.id === activeId);
      if (teacher) {
        setActiveTeacher(teacher);
        console.log('Active teacher set:', teacher);
        return;
      }
    }
    
    // Check if dragging an existing slot
    if (typeof activeId === 'string' && activeId.includes('-')) {
      const slot = processedSlots[activeId];
      if (slot && (slot.teacher || slot.course)) {
        setActiveSlot(slot);
        setDragSourceSlotId(activeId);
        console.log('Active slot set:', slot);
      }
    }
  };

  // Local state for slots - now includes full slot data
  interface LocalSlotData {
    teacher?: Teacher;
    course?: Course;
    room?: Room;
    isOver: boolean;
  }
  const [localSlots, setLocalSlots] = useState<Record<string, LocalSlotData>>({});
  const [processedSlots, setProcessedSlots] = useState<ProcessedSlotData>({});
  const [originalSlots, setOriginalSlots] = useState<Record<string, LocalSlotData>>({});
  
  const handleDragEnd = useCallback((event: DragEndEvent) => {
    const { over } = event;
    console.log('Drag end event:', { over, activeTeacher, activeSlot, dragSourceSlotId });
    
    if (over) {
      const targetSlotId = over.id as string;
      
      // Handle teacher drop
      if (activeTeacher) {
        console.log('Dropping teacher', activeTeacher.id, 'onto slot', targetSlotId);
        
        setLocalSlots(prev => {
          const newSlots = { ...prev };
          const existingSlot = prev[targetSlotId];
          const existingProcessedSlot = processedSlots[targetSlotId];
          
          newSlots[targetSlotId] = { 
            teacher: activeTeacher,
            course: existingSlot?.course || existingProcessedSlot?.course || undefined,
            room: existingSlot?.room || existingProcessedSlot?.room || undefined,
            isOver: false 
          };
          return newSlots;
        });
        setHasChanges(true);
      }
      
      // Handle slot move
      if (activeSlot && dragSourceSlotId && dragSourceSlotId !== targetSlotId) {
        console.log('Moving slot from', dragSourceSlotId, 'to', targetSlotId);
        
        setLocalSlots(prev => {
          const newSlots = { ...prev };
          const sourceSlot = prev[dragSourceSlotId];
          
          // Move the slot (not swap) - clear source and place in target
          newSlots[targetSlotId] = sourceSlot;
          newSlots[dragSourceSlotId] = { isOver: false }; // Clear source slot
          
          return newSlots;
        });
        setHasChanges(true);
      }
    }
    
    // Reset drag state
    setActiveTeacher(null);
    setActiveSlot(null);
    setDragSourceSlotId(null);
  }, [activeTeacher, activeSlot, dragSourceSlotId, processedSlots]);

    // Delete slot - just clear it locally
  const deleteSlot = useCallback((slotId: string) => {
    console.log('=== DELETE SLOT:', slotId, '===');
    
    setLocalSlots(prev => {
      const newSlots = { ...prev };
      // Clear the slot completely
      newSlots[slotId] = { 
        teacher: undefined, 
        course: undefined, 
        room: undefined, 
        isOver: false 
      };
      console.log('Slot cleared:', slotId);
      return newSlots;
    });
    
    setHasChanges(true);
    
    dispatch(addNotification({
      type: 'info',
      title: 'Slot Removed',
      message: 'Remember to click Save to persist changes'
    }));
  }, [dispatch]);

  // Remove duplicate saveChanges function - we'll use handleSave only

  // Context menu handlers
  const handleContextMenu = useCallback((e: React.MouseEvent, slotId: string) => {
    e.preventDefault();
    setContextMenu({ x: e.clientX, y: e.clientY, slotId });
  }, []);

  const closeContextMenu = useCallback(() => {
    setContextMenu(null);
  }, []);

  // Click outside to close context menu
  React.useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (contextMenuRef.current && !contextMenuRef.current.contains(event.target as Node)) {
        closeContextMenu();
      }
    };

    if (contextMenu) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [contextMenu, closeContextMenu]);

  // Initialize slots when timetable or periods change
  React.useEffect(() => {
    console.log('=== LOADING DATA FROM BACKEND ===');
    console.log('Timetable ID:', timetable?.id || 'null');
    console.log('Periods count:', periods?.length || 0);
    console.log('Slots from backend:', timetable?.slots?.length || 0);
    
    if (timetable?.slots && timetable.slots.length > 0) {
      console.log('📋 Backend returned these slots:');
      timetable.slots.forEach((slot, index) => {
        console.log(`  ${index + 1}. ${slot.dayOfWeek} Period ${slot.period?.id}: ${slot.forCourse?.name || 'No Course'} - ${slot.teacher?.firstName || 'No Teacher'}`);
      });
    } else {
      console.log('📋 No slots found in backend data');
    }
    
    if (timetable && periods) {
      
      // Map slots by key for fast access
      const newLocalSlots: Record<string, LocalSlotData> = {};
      
      periods.forEach((period: Period) => {
        ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'].forEach(day => {
          const slotKey = `${day}-${period.id}`;
          
          // Find the slot that matches both day and period
          const existingSlot = timetable.slots?.find((s: TimetableSlot) => {
            return s.dayOfWeek === day && s.period?.id === period.id;
          });
          
          newLocalSlots[slotKey] = {
            teacher: existingSlot?.teacher || undefined,
            course: existingSlot?.forCourse || undefined,
            room: existingSlot?.room || undefined,
            isOver: false
          };
        });
      });
      
      const filledSlots = Object.entries(newLocalSlots).filter(([, slot]) => slot.teacher || slot.course).length;
      console.log(`Processed ${Object.keys(newLocalSlots).length} total slots, ${filledSlots} have content`);
      
      setLocalSlots(newLocalSlots);
      setOriginalSlots(newLocalSlots);
      console.log('Initialized slots from server data');
      
    } else if (periods) {
      // Create empty slots if no timetable exists
      const emptySlots: Record<string, LocalSlotData> = {};
      periods.forEach((period: Period) => {
        ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'].forEach(day => {
          emptySlots[`${day}-${period.id}`] = { 
            teacher: undefined, 
            course: undefined,
            room: undefined,
            isOver: false 
          };
        });
      });
      setLocalSlots(emptySlots);
      setOriginalSlots(emptySlots);
      console.log('Created empty slots:', emptySlots);
    }
  }, [timetable, periods]);

  // Track changes
  React.useEffect(() => {
    const allSlotKeys = new Set([...Object.keys(localSlots), ...Object.keys(originalSlots)]);
    
    const hasAnyChanges = Array.from(allSlotKeys).some(key => {
      const current = localSlots[key];
      const original = originalSlots[key];
      
      // Helper function to check if slot has content
      const hasContent = (slot: LocalSlotData | undefined) => 
        slot && (slot.teacher || slot.course);
      
      const currentHasContent = hasContent(current);
      const originalHasContent = hasContent(original);
      
      // If one has content and the other doesn't, there's a change
      if (currentHasContent !== originalHasContent) {
        return true;
      }
      
      // If both have content, compare the content
      if (currentHasContent && originalHasContent) {
        return current!.teacher?.id !== original!.teacher?.id ||
               current!.course?.id !== original!.course?.id ||
               current!.room?.id !== original!.room?.id;
      }
      
      // If both are empty, no change
      return false;
    });
    
    setHasChanges(hasAnyChanges);
    console.log('Change detection:', { hasAnyChanges, localSlots, originalSlots });
  }, [localSlots, originalSlots]);

  // Process multi-period slots including local changes
  React.useEffect(() => {
    if (periods) {
      // Convert localSlots to TimetableSlot format for processing
      const virtualSlots: TimetableSlot[] = [];
      
      // Add existing timetable slots
      if (timetable?.slots) {
        timetable.slots.forEach(slot => {
          const slotKey = `${slot.dayOfWeek}-${slot.period?.id}`;
          const localSlot = localSlots[slotKey];
          
          // If there's a local change, check what type of change it is
          if (localSlot !== undefined) {
            // If the local slot has been cleared (moved away), don't show the original slot
            if (!localSlot.teacher && !localSlot.course && localSlot.isOver === false) {
              // This slot has been cleared, don't add it to virtualSlots
              return;
            }
            // If the local slot has content, use it instead of the original
            if (localSlot.teacher || localSlot.course) {
              virtualSlots.push({
                ...slot,
                teacher: localSlot.teacher,
                forCourse: localSlot.course,
                room: localSlot.room
              } as TimetableSlot);
              return;
            }
          }
          // No local changes, use the original slot
          virtualSlots.push(slot);
        });
      }
      
      // Add new local slots that don't exist in timetable
      Object.entries(localSlots).forEach(([slotKey, localSlot]) => {
        if (localSlot.teacher || localSlot.course) {
          const [dayOfWeek, periodIdStr] = slotKey.split('-');
          const periodId = parseInt(periodIdStr);
          const period = periods.find(p => p.id === periodId);
          
          // Check if this slot already exists in virtualSlots
          const exists = virtualSlots.some(vs => 
            vs.dayOfWeek === dayOfWeek && vs.period?.id === periodId
          );
          
          if (!exists && period) {
            virtualSlots.push({
              id: 0,
              dayOfWeek,
              period,
              teacher: localSlot.teacher,
              forCourse: localSlot.course,
              room: localSlot.room,
              forClass: undefined,
              timetable: null,
              description: ''
            } as unknown as TimetableSlot);
          }
        }
      });
      
      // Process all slots for multi-period detection
      const processed = processMultiPeriodSlots(virtualSlots, periods);
      setProcessedSlots(processed);
      console.log('Processed slots with local changes and multi-period detection:', processed);
    }
  }, [localSlots, periods, timetable]);

  const handleSave = async () => {
    console.log('=== SAVE BUTTON CLICKED ===');
    console.log('hasChanges:', hasChanges, 'isSaving:', isSaving);
    
    if (!hasChanges || isSaving) {
      console.log('Save skipped - no changes or already saving');
      return;
    }
    
    setIsSaving(true);
    
    try {
      // Build the slots array to save
      const slotsToSave: Partial<TimetableSlot>[] = [];
      
      console.log('Current localSlots:', localSlots);
      
      Object.entries(localSlots).forEach(([slotKey, slotData]) => {
        if (slotData.teacher || slotData.course) {
          const [dayOfWeek, periodId] = slotKey.split('-');
          
          // Find the actual period from the periods array
          const actualPeriod = periods?.find(p => p.id === parseInt(periodId));
          
          const slotToSave = {
            dayOfWeek: dayOfWeek as 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY',
            period: actualPeriod || { 
              id: parseInt(periodId),
              label: `Period ${periodId}`,
              startTime: '08:00',
              endTime: '09:00',
              index: parseInt(periodId)
            },
            teacher: slotData.teacher,
            forCourse: slotData.course,
            room: slotData.room,
            forClass: { id: selectedClassId, name: `Class ${selectedClassId}` }
          };
          
          slotsToSave.push(slotToSave);
          console.log(`Slot ${slotKey}:`, slotToSave);
        }
      });
      
      console.log(`SAVING ${slotsToSave.length} slots to backend...`);
      console.log('API Endpoint:', `/v1/timetables/class/${selectedClassId}/slots`);
      console.log('Request body:', JSON.stringify(slotsToSave, null, 2));
      
      const saveResponse = await http.put(`/v1/timetables/class/${selectedClassId}/slots`, slotsToSave);
      console.log('Backend response:', saveResponse);
      console.log('Save API call successful!');
      
      dispatch(addNotification({ title: 'Success', message: 'Timetable saved successfully!', type: 'success' }));
      
      console.log('=== SAVE SUCCESSFUL ===');
      
      // Immediately refetch from backend to confirm save and update cache
      console.log('Refetching data from backend to confirm save...');
      
      try {
        const refetchResult = await refetch();
        console.log('Data refetched from backend after save:', refetchResult);
        console.log('Save confirmed - data should persist on page reload!');
        
        // Force the local state to sync with server data
        if (refetchResult.data && periods) {
          const apiResponse = refetchResult.data as { data?: Timetable } | Timetable;
          const freshTimetable = 'data' in apiResponse ? apiResponse.data : apiResponse as Timetable;
          
          if (!freshTimetable || !('slots' in freshTimetable)) return;
          
          const newLocalSlots: Record<string, LocalSlotData> = {};
          
          periods.forEach((period: Period) => {
            ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'].forEach(day => {
              const slotKey = `${day}-${period.id}`;
              const existingSlot = freshTimetable.slots?.find((s: TimetableSlot) => 
                s.dayOfWeek === day && s.period?.id === period.id
              );
              
              newLocalSlots[slotKey] = {
                teacher: existingSlot?.teacher || undefined,
                course: existingSlot?.forCourse || undefined,
                room: existingSlot?.room || undefined,
                isOver: false
              };
            });
          });
          
          setLocalSlots(newLocalSlots);
          setOriginalSlots(newLocalSlots);
          console.log('Local state synchronized with fresh server data');
        }
      } catch (refetchError) {
        console.error('Error refetching after save:', refetchError);
      }
      
      setHasChanges(false);
    } catch (error: unknown) {
      console.error('Error saving timetable:', error);
      
      let errorMessage = 'Error saving timetable. Please try again.';
      
      if (error instanceof Error && 'response' in error) {
        const axiosError = error as { response?: { status?: number; data?: { message?: string } } };
        console.error('API Response:', axiosError.response?.data);
        console.error('Status Code:', axiosError.response?.status);
        
        if (axiosError.response?.status === 409) {
          errorMessage = 'Conflict detected: This may be due to a teacher or room being assigned to multiple classes at the same time. Please check for conflicts and try again.';
        } else if (axiosError.response?.status === 500) {
          errorMessage = 'Server error occurred while saving the timetable. This may be due to invalid data format or missing required fields. Please check the console for details.';
        } else if (axiosError.response?.data?.message) {
          errorMessage = axiosError.response.data.message;
        }
      }
      
      dispatch(addNotification({ 
        title: 'Save Failed', 
        message: errorMessage, 
        type: 'error' 
      }));
    } finally {
      setIsSaving(false);
    }
  };

  const handleClearSlot = useCallback((slotKey: string) => {
    console.log('Clearing slot:', slotKey);
    setLocalSlots(prev => {
      const newSlots = { ...prev };
      
      // Always clear just the single slot (per user's request)
      newSlots[slotKey] = { 
        teacher: undefined, 
        course: undefined,
        room: undefined,
        isOver: false 
      };
      
      console.log('Updated slots after clear:', newSlots);
      return newSlots;
    });
  }, []);

  // Expose clear handler globally for slot cells
  React.useEffect(() => {
    // Use a more direct approach with proper typing
    const win = window as Window & { clearSlotHandler?: (key: string) => void };
    win.clearSlotHandler = handleClearSlot;
    return () => {
      delete win.clearSlotHandler;
    };
  }, [handleClearSlot]);

  // Removed unused template function
  
  // Auto-generate handler
  const handleAutoGenerate = async () => {
    try {
      setIsGenerating(true);
      console.log('Starting auto-generation for class:', selectedClassId);
      
      // Call the optimization endpoint
      const response = await http.post(`/v1/timetables/debug/class/${selectedClassId}/optimize`);
      console.log('Debug optimization response:', response);
      
      // Wait a bit for the backend to complete the optimization
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Refetch the timetable data
      const refetchResult = await refetch();
      
      // Update local slots with the new timetable data
      if (refetchResult.data && periods) {
        const apiResponse = refetchResult.data as { data?: Timetable } | Timetable;
        const freshTimetable = 'data' in apiResponse ? apiResponse.data : apiResponse as Timetable;
        
        if (!freshTimetable || !('slots' in freshTimetable)) return;
        
        const newLocalSlots: Record<string, LocalSlotData> = {};
        
        periods.forEach((period: Period) => {
          ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'].forEach(day => {
            const slotKey = `${day}-${period.id}`;
            const existingSlot = freshTimetable.slots?.find((s: TimetableSlot) => 
              s.dayOfWeek === day && s.period?.id === period.id
            );
            
            newLocalSlots[slotKey] = {
              teacher: existingSlot?.teacher || undefined,
              course: existingSlot?.forCourse || undefined,
              room: existingSlot?.room || undefined,
              isOver: false
            };
          });
        });
        
        setLocalSlots(newLocalSlots);
        setOriginalSlots(newLocalSlots);
        console.log('Updated local slots after auto-generation:', newLocalSlots);
      }
      
      dispatch(addNotification({
        title: 'Success',
        type: 'success',
        message: 'Timetable optimization completed successfully!'
      }));
      
    } catch (error: unknown) {
      console.error('Auto-generation failed:', error);
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      dispatch(addNotification({
        title: 'Error',
        type: 'error',
        message: `Auto-generation failed: ${errorMessage}`
      }));
    } finally {
      setIsGenerating(false);
    }
  };
  // Loading and error states
  if (teachersLoading || periodsLoading || timetableLoading) return <div>Loading...</div>;
  if (teachersError) return <div>Error loading teachers.</div>;
  if (periodsError) return <div>Error loading periods.</div>;

  // If timetable is null (404), render an empty grid
  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'] as const;
  const dayDisplayNames: Record<string, string> = {
    'MONDAY': 'Monday',
    'TUESDAY': 'Tuesday', 
    'WEDNESDAY': 'Wednesday',
    'THURSDAY': 'Thursday',
    'FRIDAY': 'Friday',
    'SATURDAY': 'Saturday'
  };

  
  // Debug logging
  
  return (
    <DndContext onDragStart={handleDragStart} onDragEnd={handleDragEnd} collisionDetection={closestCenter}>
      <div className="flex flex-col gap-6">
        {/* ONLY Auto-generate and Save Buttons */}
        <div className="mb-4 flex gap-3 items-center">
          <button 
            onClick={handleAutoGenerate} 
            disabled={isGenerating}
            className={`
              px-6 py-3 text-white border-none rounded-md font-bold text-base transition-all duration-200
              ${isGenerating 
                ? 'bg-gray-500 cursor-not-allowed opacity-60' 
                : 'bg-emerald-500 hover:bg-emerald-600 cursor-pointer'
              }
            `}
          >
            {isGenerating ? '⏳ Generating...' : '🎯 Auto-generate'}
          </button>
          <button 
            onClick={handleSave} 
            disabled={!hasChanges || isSaving}
            className={`
              px-6 py-3 text-white border-none rounded-md font-bold text-base transition-all duration-200
              ${hasChanges && !isSaving 
                ? 'bg-blue-500 hover:bg-blue-600 cursor-pointer opacity-100' 
                : 'bg-slate-400 cursor-not-allowed opacity-60'
              }
            `}
            title={!hasChanges ? 'No changes to save' : isSaving ? 'Saving...' : 'Save changes'}
          >
            {isSaving ? '⏳ Saving...' : hasChanges ? '💾 Save Changes' : '✅ No Changes'}
          </button>
          <button 
            onClick={() => {
              console.log('🔄 Testing persistence by reloading data...');
              window.location.reload();
            }}
            className="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white border-none rounded-md cursor-pointer font-bold text-sm transition-all duration-200"
            title="Reload page to test if changes persist"
          >
            🔄 Test Persistence
          </button>
        </div>
        <div className="flex gap-8">
          {/* Teacher List */}
          <div className="min-w-[250px] max-w-[300px]">
            <h3 className="mb-4 text-gray-700 text-lg font-semibold">Available Teachers</h3>
            <DroppableTeacherList>
              {availableTeachers.map(teacher => (
                <DraggableTeacher key={teacher.id} teacher={teacher} />
              ))}
              {availableTeachers.length === 0 && (
                <div className="p-4 text-center text-gray-500 italic">
                  All teachers are assigned
                </div>
              )}
            </DroppableTeacherList>
          </div>
          {/* Enhanced Timetable Grid */}
          <div className="flex-1">
            <h3 className="mb-4 text-gray-700 text-center text-lg font-semibold">
              Class Timetable - Enhanced Auto-Generated Schedule
            </h3>
            <div className="overflow-x-auto border-2 border-gray-700 rounded-lg">
              <table className="border-collapse min-w-[800px] w-full font-sans" cellPadding={8}>
                <thead>
                  <tr className="bg-gray-700 text-white">
                    <th className="py-3 px-2 border border-gray-500 font-bold text-center min-w-[100px]">
                      Time / Day
                    </th>
                    {days.map(day => (
                      <th key={day} className="py-3 px-2 border border-gray-500 font-bold min-w-[140px] text-center">
                        {dayDisplayNames[day]}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {periods && periods.map((period: Period) => (
                    <tr key={period.id}>
                      <td className="p-2 border border-gray-300 bg-gray-100 font-bold text-center min-w-[100px]">
                        <div className="text-xs leading-tight">
                          <div className="text-gray-700">
                            {period.startTime} - {period.endTime}
                          </div>
                          <div className="text-[10px] text-gray-500">
                            Period {period.index}
                          </div>
                        </div>
                      </td>
                                             {days.map(day => {
                         const slotKey = `${day}-${period.id}`;
                         const slot = processedSlots[slotKey];
                         
                         // Skip if this is part of a multi-period course (null)
                         if (slot === null) return null;
                         
                         return (
                           <EnhancedSlotCell
                             key={slotKey}
                             id={slotKey}
                             slot={slot}
                             onContextMenu={handleContextMenu}
                             onDeleteSlot={deleteSlot}
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
          {activeSlot ? (
            <div 
              className="min-w-[140px] min-h-[60px] border-2 border-blue-500 rounded p-2 shadow-lg text-xs text-gray-800"
              style={{ backgroundColor: getSubjectColor(activeSlot.course) }}
            >
              <div className="font-bold mb-1">
                {activeSlot.course?.name || 'Unknown Subject'}
              </div>
              <div className="text-[10px]">
                {activeSlot.teacher ? `${activeSlot.teacher.firstName} ${activeSlot.teacher.lastName}` : 'No Teacher'}
              </div>
            </div>
          ) : null}
        </DragOverlay>
        

        
        {/* Context Menu */}
        {contextMenu && (
          <div
            ref={contextMenuRef}
            className="fixed bg-white border-2 border-gray-200 rounded-lg py-2 shadow-xl z-50 min-w-[140px] backdrop-blur-sm"
            style={{ top: contextMenu.y, left: contextMenu.x }}
          >
            <button
              onClick={() => {
                deleteSlot(contextMenu.slotId);
                closeContextMenu();
              }}
              className="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 hover:text-red-700 transition-all duration-150 flex items-center gap-2 font-medium"
            >
              <span className="text-red-500">🗑️</span>
              Remove Slot
            </button>
          </div>
        )}
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
      className={`
        px-4 py-3 my-2 border-2 border-gray-300 rounded-lg cursor-grab 
        transition-all duration-200 ease-in-out font-medium text-sm
        ${isDragging 
          ? 'bg-gray-200 shadow-md' 
          : 'bg-white shadow-sm hover:shadow-md'
        }
      `}
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
      className={`
        min-h-[200px] p-4 rounded-lg border-2 border-dashed transition-all duration-200 ease-in-out
        ${isOver 
          ? 'bg-blue-50 border-blue-500' 
          : 'bg-slate-50 border-gray-300'
        }
      `}
    >
      {children}
    </div>
  );
};

// Unused components commented out to fix lint errors
/*
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
*/

// Teacher card (used in both list and slot)
const TeacherCard: React.FC<{ teacher: Teacher }> = ({ teacher }) => (
  <div className="px-3 py-2 bg-gray-100 rounded-md border border-gray-300 text-sm font-medium">
    {teacher.firstName} {teacher.lastName}
  </div>
);

// Enhanced slot cell component for multi-period courses
const EnhancedSlotCell: React.FC<{ 
  id: string; 
  slot?: EnhancedSlot;
  onContextMenu?: (e: React.MouseEvent, slotId: string) => void;
  onDeleteSlot?: (slotId: string) => void;
}> = ({ id, slot, onContextMenu, onDeleteSlot }) => {
  const { setNodeRef: setDropRef, isOver } = useDroppable({ id });
  
  // Make slot draggable if it has content
  const {
    attributes,
    listeners,
    setNodeRef: setDragRef,
    isDragging,
  } = useDraggable({
    id: id,
    disabled: !slot || (!slot.teacher && !slot.course)
  });

  // Combine refs for both drop and drag functionality
  const combinedRef = useCallback((node: HTMLTableCellElement | null) => {
    setDropRef(node);
    setDragRef(node);
  }, [setDropRef, setDragRef]);
  
  if (!slot || (!slot.teacher && !slot.course)) {
    // Empty slot
    return (
      <td
        ref={combinedRef}
        className={`
          min-w-[140px] min-h-[60px] border border-gray-300 p-2 text-center 
          transition-all duration-200 ${isOver ? 'bg-green-100' : 'bg-white'}
        `}
      >
        <span className="text-gray-400 italic text-xs">
          Free Period
        </span>
      </td>
    );
  }

  return (
    <td
      ref={combinedRef}
      rowSpan={slot.isMultiPeriod ? slot.periodSpan : 1}
      className={`
        min-w-[140px] border-2 border-gray-300 p-2 text-center transition-all duration-200 
        relative group hover:border-blue-400 hover:shadow-md cursor-default
        ${isDragging ? 'opacity-50' : 'opacity-100'}
        ${isOver ? 'bg-green-100' : ''}
      `}
      style={{
        minHeight: slot.isMultiPeriod ? (60 * (slot.periodSpan || 1)) : 60,
        backgroundColor: isOver ? undefined : getSubjectColor(slot.course)
      }}
      onContextMenu={(e) => onContextMenu?.(e, id)}
      {...attributes}
    >
      <div className="flex flex-col justify-center h-full text-xs text-gray-800">
        {/* Drag Handle - positioned at the top of the content */}
        <div 
          {...listeners}
          className="absolute top-2 left-2 w-4 h-4 cursor-grab active:cursor-grabbing opacity-30 hover:opacity-70 transition-opacity duration-200 z-10"
          title="Drag to move this slot"
        >
          <svg viewBox="0 0 16 16" className="w-full h-full text-gray-600">
            <circle cx="4" cy="4" r="1" fill="currentColor" />
            <circle cx="8" cy="4" r="1" fill="currentColor" />
            <circle cx="12" cy="4" r="1" fill="currentColor" />
            <circle cx="4" cy="8" r="1" fill="currentColor" />
            <circle cx="8" cy="8" r="1" fill="currentColor" />
            <circle cx="12" cy="8" r="1" fill="currentColor" />
            <circle cx="4" cy="12" r="1" fill="currentColor" />
            <circle cx="8" cy="12" r="1" fill="currentColor" />
            <circle cx="12" cy="12" r="1" fill="currentColor" />
          </svg>
        </div>

        {/* Course Name */}
        <div className={`
          font-bold mb-1 text-gray-900 text-center
          ${slot.isMultiPeriod ? 'text-sm' : 'text-xs'}
        `}>
          {slot.course?.name || 'Unknown Subject'}
        </div>
        
        {/* Teacher Name */}
        <div className="text-[11px] text-gray-600 mb-1 text-center">
          {slot.teacher ? `${slot.teacher.firstName} ${slot.teacher.lastName}` : 'No Teacher'}
        </div>
        
        {/* Room */}
        {slot.room && (
          <div className="text-[10px] text-gray-500 text-center">
            Room: {slot.room.name}
          </div>
        )}
        
        {/* Course ID display */}
        {slot.course?.id && (
          <div className="text-[9px] text-gray-400 mt-1 text-center">
            ID: {slot.course.id}
          </div>
        )}
        
        {/* Clear button for individual slots - only show if slot has content */}
        {(slot.teacher || slot.course) && (
          <button
            onClick={(e) => {
              console.log('Delete button clicked for slot:', id);
              e.stopPropagation();
              e.preventDefault();
              onDeleteSlot?.(id);
            }}
            className="absolute top-1 right-1 w-6 h-6 bg-red-500 hover:bg-red-600 text-white border-none rounded-full text-sm cursor-pointer flex items-center justify-center opacity-60 group-hover:opacity-100 hover:!opacity-100 z-20 transition-all duration-200 shadow-md hover:shadow-lg hover:scale-110"
            title="Remove this slot"
          >
            ×
          </button>
        )}
        
        {/* Multi-period indicator */}
        {slot.isMultiPeriod && (
          <div className="absolute top-1 left-1 text-[10px] bg-blue-500 text-white px-1 py-0.5 rounded z-10">
            {slot.periodSpan}h
          </div>
        )}
      </div>
    </td>
  );
};

// Color coding for courses - use course color from database with transparency
const getSubjectColor = (course: Course | undefined): string => {
  // Use the course color from database if available
  if (course?.color) {
    // Add transparency for better readability with text
    return course.color + '30'; // 30% opacity
  }
  
  // Fallback to light gray if no course
  return '#f9fafb';
};

// Helper function for color handling removed as unused

export default TimetableGrid;