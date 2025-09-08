import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, Calendar as CalendarIcon } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

interface ScheduleEvent {
  id: string;
  title: string;
  subject: string;
  startTime: string;
  endTime: string;
  day: string;
  color: string;
  room?: string;
}

interface WeeklyScheduleProps {
  events?: ScheduleEvent[];
  className?: string;
}

const WeeklySchedule: React.FC<WeeklyScheduleProps> = ({ 
  events = [], 
  className 
}) => {
  const [currentWeek, setCurrentWeek] = useState(new Date());
  const [viewMode, setViewMode] = useState<'week' | 'day'>('week');

  // Get current week dates
  const getWeekDates = (date: Date) => {
    const week = [];
    const startOfWeek = new Date(date);
    const day = startOfWeek.getDay();
    const diff = startOfWeek.getDate() - day + (day === 0 ? -6 : 1); // Adjust for Monday start
    startOfWeek.setDate(diff);

    for (let i = 0; i < 7; i++) {
      const weekDay = new Date(startOfWeek);
      weekDay.setDate(startOfWeek.getDate() + i);
      week.push(weekDay);
    }
    return week;
  };

  const weekDates = getWeekDates(currentWeek);
  const today = new Date();

  // Time slots from 8:00 AM to 6:00 PM
  const timeSlots = [
    '8:00 AM', '9:00 AM', '10:00 AM', '11:00 AM', '12:00 PM',
    '1:00 PM', '2:00 PM', '3:00 PM', '4:00 PM', '5:00 PM', '6:00 PM'
  ];

  const dayNames = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
  
  const getDayHeaderColor = (index: number, date: Date) => {
    const isSunday = index === 6; // Sunday is the last day
    const isTodayDate = isToday(date);
    
    if (isTodayDate) {
      return "bg-blue-400 text-white";
    } else if (isSunday) {
      return "bg-red-100 text-red-700 border-red-200";
    } else {
      return "bg-gray-100 text-gray-700";
    }
  };
  const fullDayNames = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

  const navigateWeek = (direction: 'prev' | 'next') => {
    setCurrentWeek(prev => {
      const newDate = new Date(prev);
      const days = direction === 'prev' ? -7 : 7;
      newDate.setDate(prev.getDate() + days);
      return newDate;
    });
  };

  const isToday = (date: Date) => {
    return date.toDateString() === today.toDateString();
  };

  const getCurrentWeekRange = () => {
    const start = weekDates[0];
    const end = weekDates[6];
    return `${start.toLocaleDateString('en-US', { month: 'long', day: 'numeric' })} - ${end.toLocaleDateString('en-US', { day: 'numeric' })}`;
  };

  const getEventsForTimeSlot = (day: Date, timeSlot: string) => {
    const dayName = fullDayNames[day.getDay() === 0 ? 6 : day.getDay() - 1]; // Adjust for Monday start
    return events.filter(event => 
      event.day.toLowerCase() === dayName.toLowerCase() && 
      event.startTime === timeSlot
    );
  };

  // Sample events data
  const sampleEvents: ScheduleEvent[] = [
    {
      id: '1',
      title: '4A - Physics',
      subject: 'Physics',
      startTime: '8:00 AM',
      endTime: '8:45 AM',
      day: 'Monday',
      color: 'bg-blue-100 border-blue-300 text-blue-800',
      room: 'Room 4A'
    },
    {
      id: '2',
      title: '1A - Chemistry',
      subject: 'Chemistry',
      startTime: '8:00 AM',
      endTime: '8:45 AM',
      day: 'Tuesday',
      color: 'bg-green-100 border-green-300 text-green-800',
      room: 'Room 1A'
    },
    {
      id: '3',
      title: '4A - Chemistry',
      subject: 'Chemistry',
      startTime: '8:00 AM',
      endTime: '8:45 AM',
      day: 'Wednesday',
      color: 'bg-green-100 border-green-300 text-green-800',
      room: 'Room 4A'
    },
    {
      id: '4',
      title: '3B - Physics',
      subject: 'Physics',
      startTime: '9:00 AM',
      endTime: '9:45 AM',
      day: 'Monday',
      color: 'bg-blue-100 border-blue-300 text-blue-800',
      room: 'Room 3B'
    },
    {
      id: '5',
      title: '2B - Physics',
      subject: 'Physics',
      startTime: '9:00 AM',
      endTime: '9:45 AM',
      day: 'Tuesday',
      color: 'bg-blue-100 border-blue-300 text-blue-800',
      room: 'Room 2B'
    },
    {
      id: '6',
      title: '6A - Physics',
      subject: 'Physics',
      startTime: '9:00 AM',
      endTime: '9:45 AM',
      day: 'Wednesday',
      color: 'bg-blue-100 border-blue-300 text-blue-800',
      room: 'Room 6A'
    }
  ];

  const allEvents = [...events, ...sampleEvents];

  return (
    <Card className={cn("w-full", className)}>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between mb-4">
          <CardTitle className="flex items-center gap-2">
            <CalendarIcon className="h-5 w-5" />
            Schedule (4A)
          </CardTitle>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigateWeek('prev')}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentWeek(new Date())}
            >
              Today
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigateWeek('next')}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {/* Week Range and View Toggle */}
        <div className="flex items-center justify-between">
          <div className="text-sm text-gray-600">
            {getCurrentWeekRange()}
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant={viewMode === 'week' ? 'default' : 'outline'}
              size="sm"
              onClick={() => setViewMode('week')}
              className={viewMode === 'week' ? 'bg-blue-400 hover:bg-blue-500 text-white' : ''}
            >
              Work Week
            </Button>
            <Button
              variant={viewMode === 'day' ? 'default' : 'outline'}
              size="sm"
              onClick={() => setViewMode('day')}
              className={viewMode === 'day' ? 'bg-blue-400 hover:bg-blue-500 text-white' : ''}
            >
              Day
            </Button>
          </div>
        </div>
      </CardHeader>
      
      <CardContent>
        {viewMode === 'week' ? (
          /* Weekly View */
          <div className="overflow-x-auto">
            <div className="min-w-[700px]">
              {/* Header with days */}
              <div className="grid grid-cols-8 gap-1 mb-2">
                <div className="p-2"></div> {/* Empty corner */}
                {weekDates.slice(0, 7).map((date, index) => (
                  <div
                    key={index}
                    className={cn(
                      "p-2 text-center text-sm font-medium rounded-lg border",
                      getDayHeaderColor(index, date)
                    )}
                  >
                    <div>{dayNames[index]}</div>
                    <div className="text-xs mt-1">{date.getDate()}</div>
                  </div>
                ))}
              </div>

              {/* Time slots and events */}
              <div className="space-y-1">
                {timeSlots.map((timeSlot) => (
                  <div key={timeSlot} className="grid grid-cols-8 gap-1">
                    {/* Time label */}
                    <div className="p-2 text-sm font-medium text-gray-600 text-right">
                      {timeSlot}
                    </div>
                    
                    {/* Day columns */}
                    {weekDates.slice(0, 7).map((date, dayIndex) => {
                      const dayEvents = getEventsForTimeSlot(date, timeSlot);
                      return (
                        <div
                          key={dayIndex}
                          className="min-h-[60px] p-1 border border-gray-200 rounded"
                        >
                          {dayEvents.map((event) => (
                            <div
                              key={event.id}
                              className={cn(
                                "p-2 rounded text-xs border",
                                event.color
                              )}
                            >
                              <div className="font-medium">{event.title}</div>
                              <div className="text-xs opacity-75">
                                {event.startTime} - {event.endTime}
                              </div>
                            </div>
                          ))}
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>
            </div>
          </div>
        ) : (
          /* Day View */
          <div className="space-y-4">
            <div className="text-center">
              <h3 className="text-lg font-semibold">
                {today.toLocaleDateString('en-US', { 
                  weekday: 'long', 
                  month: 'long', 
                  day: 'numeric' 
                })}
              </h3>
            </div>
            
            <div className="space-y-2">
              {timeSlots.map((timeSlot) => {
                const todayEvents = getEventsForTimeSlot(today, timeSlot);
                return (
                  <div key={timeSlot} className="flex items-center gap-4">
                    <div className="w-20 text-sm font-medium text-gray-600 text-right">
                      {timeSlot}
                    </div>
                    <div className="flex-1 min-h-[60px] p-2 border border-gray-200 rounded">
                      {todayEvents.map((event) => (
                        <div
                          key={event.id}
                          className={cn(
                            "p-3 rounded border",
                            event.color
                          )}
                        >
                          <div className="font-medium">{event.title}</div>
                          <div className="text-sm opacity-75">
                            {event.startTime} - {event.endTime}
                          </div>
                          {event.room && (
                            <div className="text-xs mt-1">{event.room}</div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export default WeeklySchedule;
