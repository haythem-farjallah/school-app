import React, { useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface MiniCalendarProps {
  className?: string;
}

const MiniCalendar: React.FC<MiniCalendarProps> = ({ className }) => {
  const [currentDate, setCurrentDate] = useState(new Date());
  
  const today = new Date();
  const currentMonth = currentDate.getMonth();
  const currentYear = currentDate.getFullYear();

  // Get first day of month and number of days
  const firstDayOfMonth = new Date(currentYear, currentMonth, 1);
  const lastDayOfMonth = new Date(currentYear, currentMonth + 1, 0);
  const daysInMonth = lastDayOfMonth.getDate();
  const startingDayOfWeek = firstDayOfMonth.getDay();

  // Generate calendar days
  const calendarDays = [];
  
  // Add empty cells for days before month starts
  for (let i = 0; i < startingDayOfWeek; i++) {
    calendarDays.push(null);
  }
  
  // Add days of the month
  for (let day = 1; day <= daysInMonth; day++) {
    calendarDays.push(day);
  }

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  const navigateMonth = (direction: 'prev' | 'next') => {
    setCurrentDate(prev => {
      const newDate = new Date(prev);
      if (direction === 'prev') {
        newDate.setMonth(prev.getMonth() - 1);
      } else {
        newDate.setMonth(prev.getMonth() + 1);
      }
      return newDate;
    });
  };

  const isToday = (day: number) => {
    return today.getDate() === day && 
           today.getMonth() === currentMonth && 
           today.getFullYear() === currentYear;
  };

  return (
    <Card className={cn("w-full", className)}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base">
            {monthNames[currentMonth]} {currentYear}
          </CardTitle>
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigateMonth('prev')}
              className="h-7 w-7 p-0"
            >
              <ChevronLeft className="h-3 w-3" />
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigateMonth('next')}
              className="h-7 w-7 p-0"
            >
              <ChevronRight className="h-3 w-3" />
            </Button>
          </div>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0">
        {/* Calendar Grid */}
        <div className="grid grid-cols-7 gap-1">
          {/* Day headers */}
          {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((day, index) => (
            <div 
              key={day} 
              className={cn(
                "p-1 text-center text-xs font-medium",
                index === 0 ? "text-red-600" : "text-gray-500"
              )}
            >
              {day}
            </div>
          ))}
          
          {/* Calendar days */}
          {calendarDays.map((day, index) => {
            if (!day) {
              return <div key={index} className="p-1 h-7"></div>;
            }

            const isTodayDate = isToday(day);

            return (
              <div
                key={day}
                className={cn(
                  "p-1 h-7 flex items-center justify-center text-xs cursor-pointer rounded transition-colors hover:bg-gray-100",
                  isTodayDate ? "bg-blue-400 text-white hover:bg-blue-500" : "text-gray-900"
                )}
              >
                {day}
              </div>
            );
          })}
        </div>

        {/* Events section */}
        <div className="mt-4">
          <h5 className="font-medium text-sm text-gray-700 mb-2">Events</h5>
          <div className="space-y-1">
            <div className="p-2 bg-gray-50 rounded text-xs">
              <div className="font-medium">Book Fair</div>
              <div className="text-gray-600 text-xs">Browse and purchase books</div>
            </div>
            <div className="p-2 bg-gray-50 rounded text-xs">
              <div className="font-medium">Sports Day</div>
              <div className="text-gray-600 text-xs">Athletic events and competitions</div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default MiniCalendar;
