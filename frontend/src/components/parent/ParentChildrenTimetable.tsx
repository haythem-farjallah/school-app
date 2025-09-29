import React, { useState } from 'react';
import { Clock, MapPin, User, BookOpen } from 'lucide-react';

interface TimetableSlot {
  id: number;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  periodIndex: number;
  courseName: string;
  courseCode: string;
  teacherName: string;
  roomName: string;
}

interface ChildTimetable {
  studentId: number;
  studentName: string;
  timetables: Array<{
    classId: number;
    className: string;
    slots: TimetableSlot[];
  }>;
}

interface ParentChildrenTimetableProps {
  data: {
    children: ChildTimetable[];
    totalChildren: number;
  };
  selectedChild?: number | null;
  onChildSelect: (childId: number | null) => void;
}

const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
const TIME_SLOTS = [
  { period: 1, time: '08:00 - 08:45' },
  { period: 2, time: '08:45 - 09:30' },
  { period: 3, time: '09:30 - 10:15' },
  { period: 4, time: '10:30 - 11:15' },
  { period: 5, time: '11:15 - 12:00' },
  { period: 6, time: '12:00 - 12:45' },
  { period: 7, time: '13:45 - 14:30' },
  { period: 8, time: '14:30 - 15:15' },
];

export const ParentChildrenTimetable: React.FC<ParentChildrenTimetableProps> = ({
  data,
  selectedChild,
  onChildSelect
}) => {
  const [activeTab, setActiveTab] = useState<number>(
    selectedChild || (data.children.length > 0 ? data.children[0].studentId : 0)
  );

  const activeChildData = data.children.find(child => child.studentId === activeTab);
  
  // Combine all slots from all classes for the selected child
  const allSlots = activeChildData?.timetables.flatMap(timetable => 
    timetable.slots.map(slot => ({
      ...slot,
      className: timetable.className
    }))
  ) || [];

  // Create timetable grid
  const createTimetableGrid = () => {
    const grid: { [key: string]: any } = {};
    
    allSlots.forEach(slot => {
      const key = `${slot.dayOfWeek}-${slot.periodIndex}`;
      grid[key] = slot;
    });
    
    return grid;
  };

  const timetableGrid = createTimetableGrid();

  const getSlotColor = (courseName: string) => {
    const colors = [
      'bg-blue-100 border-blue-300 text-blue-800',
      'bg-green-100 border-green-300 text-green-800',
      'bg-purple-100 border-purple-300 text-purple-800',
      'bg-orange-100 border-orange-300 text-orange-800',
      'bg-pink-100 border-pink-300 text-pink-800',
      'bg-indigo-100 border-indigo-300 text-indigo-800',
    ];
    
    const hash = courseName.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return colors[hash % colors.length];
  };

  if (!data.children.length) {
    return (
      <div className="text-center py-8">
        <BookOpen className="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p className="text-gray-500">No timetable data available</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Child Selection Tabs */}
      <div className="flex space-x-1 bg-gray-100 p-1 rounded-lg">
        {data.children.map((child) => (
          <button
            key={child.studentId}
            onClick={() => {
              setActiveTab(child.studentId);
              onChildSelect(child.studentId);
            }}
            className={`flex-1 px-4 py-2 text-sm font-medium rounded-md transition-colors ${
              activeTab === child.studentId
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {child.studentName}
          </button>
        ))}
      </div>

      {/* Timetable Grid */}
      {activeChildData && (
        <div className="overflow-x-auto">
          <div className="min-w-full">
            {/* Header */}
            <div className="grid grid-cols-6 gap-2 mb-4">
              <div className="font-semibold text-gray-700 p-3 text-center">Time</div>
              {DAYS_OF_WEEK.map(day => (
                <div key={day} className="font-semibold text-gray-700 p-3 text-center">
                  {day.charAt(0) + day.slice(1).toLowerCase()}
                </div>
              ))}
            </div>

            {/* Time slots */}
            {TIME_SLOTS.map(timeSlot => (
              <div key={timeSlot.period} className="grid grid-cols-6 gap-2 mb-2">
                {/* Time column */}
                <div className="p-3 text-sm text-gray-600 text-center border rounded-lg bg-gray-50">
                  <div className="font-medium">Period {timeSlot.period}</div>
                  <div className="text-xs">{timeSlot.time}</div>
                </div>

                {/* Day columns */}
                {DAYS_OF_WEEK.map(day => {
                  const slotKey = `${day}-${timeSlot.period}`;
                  const slot = timetableGrid[slotKey];

                  return (
                    <div
                      key={`${day}-${timeSlot.period}`}
                      className={`p-3 rounded-lg border-2 min-h-[80px] ${
                        slot
                          ? `${getSlotColor(slot.courseName)} border-dashed`
                          : 'bg-gray-50 border-gray-200'
                      }`}
                    >
                      {slot && (
                        <div className="space-y-1">
                          <div className="font-semibold text-xs">{slot.courseCode}</div>
                          <div className="text-xs">{slot.courseName}</div>
                          <div className="flex items-center text-xs opacity-75">
                            <User className="h-3 w-3 mr-1" />
                            {slot.teacherName}
                          </div>
                          <div className="flex items-center text-xs opacity-75">
                            <MapPin className="h-3 w-3 mr-1" />
                            {slot.roomName}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Legend */}
      <div className="bg-gray-50 rounded-lg p-4">
        <h4 className="font-medium text-gray-900 mb-3">Legend</h4>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div className="flex items-center">
            <div className="w-4 h-4 bg-blue-100 border-2 border-blue-300 border-dashed rounded mr-2"></div>
            <span>Class Period</span>
          </div>
          <div className="flex items-center">
            <User className="h-4 w-4 text-gray-600 mr-2" />
            <span>Teacher</span>
          </div>
          <div className="flex items-center">
            <MapPin className="h-4 w-4 text-gray-600 mr-2" />
            <span>Room</span>
          </div>
          <div className="flex items-center">
            <Clock className="h-4 w-4 text-gray-600 mr-2" />
            <span>Time Period</span>
          </div>
        </div>
      </div>
    </div>
  );
};
