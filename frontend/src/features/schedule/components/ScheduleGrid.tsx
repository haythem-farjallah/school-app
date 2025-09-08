import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { Clock, MapPin, Users, User, BookOpen } from 'lucide-react';
import { TimetableSlot, DayOfWeek } from '../../../types/timetable';
import { Period } from '../../../types/period';

interface ScheduleGridProps {
  slots: TimetableSlot[];
  periods: Period[];
  title?: string;
  showTeacher?: boolean;
  showRoom?: boolean;
  showClass?: boolean;
  className?: string;
  emptyMessage?: string;
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

const SLOT_COLORS = [
  'bg-blue-100 border-blue-300 text-blue-800',
  'bg-green-100 border-green-300 text-green-800',
  'bg-purple-100 border-purple-300 text-purple-800',
  'bg-orange-100 border-orange-300 text-orange-800',
  'bg-pink-100 border-pink-300 text-pink-800',
  'bg-indigo-100 border-indigo-300 text-indigo-800',
];

export function ScheduleGrid({
  slots,
  periods,
  title,
  showTeacher = true,
  showRoom = true,
  showClass = false,
  className = '',
  emptyMessage = 'No schedule available'
}: ScheduleGridProps) {
  // Sort periods by index
  const sortedPeriods = [...periods].sort((a, b) => a.index - b.index);
  
  // Create a map of slots by day and period
  const slotMap = new Map<string, TimetableSlot>();
  slots.forEach(slot => {
    const key = `${slot.dayOfWeek}-${slot.period.id}`;
    slotMap.set(key, slot);
  });

  // Get color for a slot based on course or teacher
  const getSlotColor = (slot: TimetableSlot) => {
    const hash = (slot.forCourse?.name || slot.teacher?.firstName || '').split('').reduce((a, b) => {
      a = ((a << 5) - a) + b.charCodeAt(0);
      return a & a;
    }, 0);
    return SLOT_COLORS[Math.abs(hash) % SLOT_COLORS.length];
  };

  if (slots.length === 0) {
    return (
      <Card className={className}>
        {title && (
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Clock className="w-5 h-5" />
              {title}
            </CardTitle>
          </CardHeader>
        )}
        <CardContent>
          <div className="text-center py-8 text-gray-500">
            <Clock className="w-12 h-12 mx-auto mb-4 opacity-50" />
            <p>{emptyMessage}</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      {title && (
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Clock className="w-5 h-5" />
            {title}
          </CardTitle>
        </CardHeader>
      )}
      <CardContent>
        <div className="overflow-x-auto">
          <div className="min-w-full">
            {/* Header */}
            <div className="grid grid-cols-7 gap-2 mb-4">
              <div className="font-semibold text-sm text-gray-600 p-2">Time</div>
              {DAYS.map(day => (
                <div key={day} className="font-semibold text-sm text-gray-600 p-2 text-center">
                  {DAY_NAMES[day]}
                </div>
              ))}
            </div>

            {/* Schedule Grid */}
            <div className="space-y-2">
              {sortedPeriods.map(period => (
                <div key={period.id} className="grid grid-cols-7 gap-2">
                  {/* Time Column */}
                  <div className="p-2 text-sm font-medium text-gray-700 bg-gray-50 rounded border">
                    <div>{period.name}</div>
                    <div className="text-xs text-gray-500">
                      {period.startTime} - {period.endTime}
                    </div>
                  </div>

                  {/* Day Columns */}
                  {DAYS.map(day => {
                    const slotKey = `${day}-${period.id}`;
                    const slot = slotMap.get(slotKey);

                    if (!slot) {
                      return (
                        <div key={`${day}-${period.id}`} className="p-2 border border-gray-200 rounded min-h-[80px]">
                          {/* Empty slot */}
                        </div>
                      );
                    }

                    return (
                      <div
                        key={`${day}-${period.id}`}
                        className={`p-3 border-2 rounded-lg min-h-[80px] ${getSlotColor(slot)}`}
                      >
                        <div className="space-y-1">
                          {/* Course Name */}
                          {slot.forCourse && (
                            <div className="font-semibold text-sm flex items-center gap-1">
                              <BookOpen className="w-3 h-3" />
                              {slot.forCourse.name}
                            </div>
                          )}

                          {/* Class Name (for teacher view) */}
                          {showClass && slot.forClass && (
                            <div className="text-xs flex items-center gap-1">
                              <Users className="w-3 h-3" />
                              {slot.forClass.name}
                            </div>
                          )}

                          {/* Teacher Name (for student/parent view) */}
                          {showTeacher && slot.teacher && (
                            <div className="text-xs flex items-center gap-1">
                              <User className="w-3 h-3" />
                              {slot.teacher.firstName} {slot.teacher.lastName}
                            </div>
                          )}

                          {/* Room */}
                          {showRoom && slot.room && (
                            <div className="text-xs flex items-center gap-1">
                              <MapPin className="w-3 h-3" />
                              {slot.room.name}
                            </div>
                          )}

                          {/* Description */}
                          {slot.description && (
                            <div className="text-xs opacity-75">
                              {slot.description}
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
