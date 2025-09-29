import React from "react";
import { Calendar, User, MapPin } from "lucide-react";
import { TimetableSlot, ChildTimetable, TimetableData, ChildInfo } from "@/types/parent-dashboard";

interface ParentWeeklyTimetableProps {
  children: ChildInfo[];
  selectedChild: number | null;
  timetableData: TimetableData | null;
  isLoading: boolean;
  onChildSelect: (childId: number | null) => void;
}

// Time slots for the day (8 AM to 4 PM)
const TIME_SLOTS = [
  "08:00", "09:00", "10:00", "11:00", "12:00", 
  "13:00", "14:00", "15:00", "16:00"
];

const DAYS = [
  "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Sunday"
];

// Get current week dates (Monday to Sunday, excluding Saturday)
const getCurrentWeekDates = (): Date[] => {
  const today = new Date();
  const monday = new Date(today);
  monday.setDate(today.getDate() - today.getDay() + 1);
  
  const weekDates: Date[] = [];
  // Monday to Friday
  for (let i = 0; i < 5; i++) {
    const date = new Date(monday);
    date.setDate(monday.getDate() + i);
    weekDates.push(date);
  }
  // Sunday
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  weekDates.push(sunday);
  
  return weekDates;
};

// Format time to match display format (08:00 format)
const formatTimeForDisplay = (time: string) => {
  const timeParts = time.split(':');
  const hour = timeParts[0].padStart(2, '0');
  const minutes = timeParts[1];
  return `${hour}:${minutes}`;
};

// Get time slot for a given time
const getTimeSlot = (startTime: string) => {
  // Handle both "08:00" and "08:00:00" formats
  const timeParts = startTime.split(':');
  const hour = parseInt(timeParts[0]);
  return hour - 8; // Convert to 0-based index
};

// Get day index for day name
const getDayIndex = (dayName: string) => {
  // Convert database format (MONDAY) to frontend format (Monday)
  const normalizedDay = dayName.charAt(0).toUpperCase() + dayName.slice(1).toLowerCase();
  return DAYS.indexOf(normalizedDay);
};

// Generate consistent colors for courses
const getCourseColor = (courseName: string) => {
  const colors = [
    'bg-blue-100 border-blue-300 text-blue-800',
    'bg-green-100 border-green-300 text-green-800',
    'bg-purple-100 border-purple-300 text-purple-800',
    'bg-orange-100 border-orange-300 text-orange-800',
    'bg-pink-100 border-pink-300 text-pink-800',
    'bg-indigo-100 border-indigo-300 text-indigo-800',
    'bg-yellow-100 border-yellow-300 text-yellow-800',
    'bg-red-100 border-red-300 text-red-800',
    'bg-teal-100 border-teal-300 text-teal-800',
    'bg-cyan-100 border-cyan-300 text-cyan-800'
  ];
  
  // Generate a consistent hash for the course name
  let hash = 0;
  for (let i = 0; i < courseName.length; i++) {
    hash = ((hash << 5) - hash + courseName.charCodeAt(i)) & 0xffffffff;
  }
  
  return colors[Math.abs(hash) % colors.length];
};

export const ParentWeeklyTimetable: React.FC<ParentWeeklyTimetableProps> = ({
  selectedChild,
  timetableData,
  isLoading
}) => {
  const weekDates = getCurrentWeekDates();

  // Process timetable data - ALWAYS call hooks at top level
  const processedTimetables = React.useMemo(() => {
    if (!timetableData?.children) return [];
    
    return timetableData.children.map((child: ChildTimetable) => {
      // Create a grid for the week - only include slots with actual classes
      const grid: { [key: string]: TimetableSlot } = {};
      const hasClasses: { [key: string]: boolean } = {};
      
      child.timetables.forEach(classSchedule => {
        classSchedule.slots.forEach(slot => {
          // Only include slots that have actual course content (not free periods)
          if (slot.courseName && slot.courseName.trim() !== '' && slot.courseName !== 'Free Period') {
            const dayIndex = getDayIndex(slot.dayOfWeek);
            const timeSlot = getTimeSlot(slot.startTime);
            
            if (dayIndex >= 0 && timeSlot >= 0 && timeSlot < TIME_SLOTS.length) {
              const key = `${dayIndex}-${timeSlot}`;
              grid[key] = slot;
              hasClasses[key] = true;
            }
          }
        });
      });
      
      return {
        ...child,
        grid,
        hasClasses
      };
    });
  }, [timetableData]);

  if (isLoading) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-gray-200 rounded w-1/4"></div>
          <div className="grid grid-cols-6 gap-4">
            {Array.from({ length: 30 }).map((_, i) => (
              <div key={i} className="h-20 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // Filter timetables based on selected child
  const displayTimetables = selectedChild 
    ? processedTimetables.filter(t => t.studentId === selectedChild)
    : processedTimetables;

  return (
    <div className="space-y-2">
      {/* Semester Header */}
      <div className="bg-white rounded-lg shadow-sm p-1">
        <div className="flex items-center space-x-4">
          <h2 className="text-xl font-semibold text-gray-900 flex items-center">
            <Calendar className="h-5 w-5 mr-2" />
            Class Children's Schedule
          </h2>
          <div className="text-sm text-gray-600">
            Fall Semester: Sep 1 - Dec 31, 2025
          </div>
        </div>
      </div>

      {/* Timetables for each child */}
      {displayTimetables.map((childTimetable) => (
        <div key={childTimetable.studentId} className="bg-white rounded-lg shadow-sm overflow-hidden">
          {/* Child Header */}
          <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                  <User className="h-5 w-5 text-white" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">
                    Schedule ({childTimetable.studentName})
                  </h3>
                  <p className="text-blue-100 text-sm">
                    {childTimetable.timetables?.[0]?.className || 'No class assigned'}
                  </p>
                </div>
              </div>
              
              <div className="text-right">
                <div className="text-white text-sm">
                  Fall Semester 2025
                </div>
                <div className="text-blue-100 text-xs">Class Schedule</div>
              </div>
            </div>
          </div>

          {/* Timetable Grid */}
          <div className="p-1">
            <div className="overflow-x-auto">
              <div className="min-w-[500px]">
                {/* Header Row - Simplified */}
                <div className="grid grid-cols-6 gap-0.5 mb-0.5">
                  <div className="p-0.5 text-xs font-medium text-gray-600 text-center">
                    Time
                  </div>
                  {DAYS.map((day, index) => (
                    <div key={day} className="p-0.5 text-xs font-medium text-gray-900 text-center bg-gray-50 rounded">
                      <div>{day}</div>
                    </div>
                  ))}
                </div>

                {/* Time Slots */}
                {TIME_SLOTS.map((time, timeIndex) => (
                  <div key={time} className="grid grid-cols-6 gap-0.5 mb-0.5">
                    {/* Time Column */}
                    <div className="p-0.5 text-xs font-medium text-gray-600 text-center border-r">
                      <div>{formatTimeForDisplay(time + ':00')}</div>
                    </div>

                    {/* Day Columns */}
                    {DAYS.map((day, dayIndex) => {
                      const slotKey = `${dayIndex}-${timeIndex}`;
                      const slot = childTimetable.grid[slotKey];

                      return (
                        <div
                          key={`${day}-${time}`}
                          className={`p-0.5 min-h-[35px] border rounded ${
                            slot 
                              ? `${getCourseColor(slot.courseName)} hover:shadow-md`
                              : 'bg-gray-50 border-gray-200'
                          } transition-all duration-200 cursor-pointer`}
                        >
                          {slot ? (
                            <div className="space-y-0.5">
                              <div className="text-xs font-semibold truncate">
                                {formatTimeForDisplay(slot.startTime)} - {formatTimeForDisplay(slot.endTime)}
                              </div>
                              <div className="text-xs font-medium truncate">
                                {slot.courseName}
                              </div>
                              <div className="flex items-center text-xs truncate">
                                <User className="h-2 w-2 mr-1" />
                                {slot.teacherName}
                              </div>
                              {slot.roomName && (
                                <div className="flex items-center text-xs truncate">
                                  <MapPin className="h-2 w-2 mr-1" />
                                  {slot.roomName}
                                </div>
                              )}
                            </div>
                          ) : (
                            <div className="min-h-[30px] flex items-center justify-center">
                              <span className="text-xs text-gray-400">Free Time</span>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      ))}

    </div>
  );
};

export default ParentWeeklyTimetable;