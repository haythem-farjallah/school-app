import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, Calendar as CalendarIcon, Clock, MapPin, User } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

interface ExamEvent {
  id: string;
  subject: string;
  title: string;
  date: Date;
  startTime: string;
  endTime: string;
  room: string;
  teacher: string;
  type: 'midterm' | 'final' | 'quiz' | 'test';
  status: 'upcoming' | 'completed' | 'in-progress';
}

interface ExamCalendarProps {
  exams?: ExamEvent[];
  className?: string;
}

const ExamCalendar: React.FC<ExamCalendarProps> = ({ 
  exams = [], 
  className 
}) => {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(new Date());

  const today = new Date();
  const currentMonth = currentDate.getMonth();
  const currentYear = currentDate.getFullYear();

  // Sample exam data
  const sampleExams: ExamEvent[] = [
    {
      id: '1',
      subject: 'English',
      title: 'English Midterm Exam',
      date: new Date(2024, 11, 20),
      startTime: '09:00',
      endTime: '11:00',
      room: 'Room 3A',
      teacher: 'Noah',
      type: 'midterm',
      status: 'upcoming'
    },
    {
      id: '2',
      subject: 'Biology',
      title: 'Biology Final Exam',
      date: new Date(2024, 11, 22),
      startTime: '10:00',
      endTime: '12:00',
      room: 'Room 2A',
      teacher: 'Daniel',
      type: 'final',
      status: 'upcoming'
    },
    {
      id: '3',
      subject: 'Computer Science',
      title: 'Programming Quiz',
      date: new Date(2024, 11, 25),
      startTime: '14:00',
      endTime: '15:30',
      room: 'Lab 1',
      teacher: 'Chloe',
      type: 'quiz',
      status: 'upcoming'
    }
  ];

  const allExams = [...exams, ...sampleExams];

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

  const isSelected = (day: number) => {
    return selectedDate.getDate() === day && 
           selectedDate.getMonth() === currentMonth && 
           selectedDate.getFullYear() === currentYear;
  };

  const getExamsForDay = (day: number) => {
    const dayDate = new Date(currentYear, currentMonth, day);
    return allExams.filter(exam => 
      exam.date.getDate() === day &&
      exam.date.getMonth() === currentMonth &&
      exam.date.getFullYear() === currentYear
    );
  };

  const getExamTypeColor = (type: string) => {
    switch (type) {
      case 'final': return 'bg-red-500';
      case 'midterm': return 'bg-orange-500';
      case 'quiz': return 'bg-blue-400';
      case 'test': return 'bg-green-500';
      default: return 'bg-gray-500';
    }
  };

  const getExamTypeBadge = (type: string) => {
    switch (type) {
      case 'final': return 'bg-red-100 text-red-800 border-red-200';
      case 'midterm': return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'quiz': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'test': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  return (
    <Card className={cn("w-full", className)}>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <CalendarIcon className="h-5 w-5" />
            Exam Calendar
          </CardTitle>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigateMonth('prev')}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentDate(new Date())}
            >
              Today
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigateMonth('next')}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
        <div className="text-lg font-semibold text-center">
          {monthNames[currentMonth]} {currentYear}
        </div>
      </CardHeader>
      
      <CardContent>
        {/* Calendar Grid */}
        <div className="grid grid-cols-7 gap-1 mb-4">
          {/* Day headers */}
          {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day, index) => (
            <div 
              key={day} 
              className={cn(
                "p-2 text-center text-sm font-medium",
                index === 0 ? "text-red-600" : "text-gray-500"
              )}
            >
              {day}
            </div>
          ))}
          
          {/* Calendar days */}
          {calendarDays.map((day, index) => {
            if (!day) {
              return <div key={index} className="p-2 h-24"></div>;
            }

            const dayExams = getExamsForDay(day);
            const isTodayDate = isToday(day);
            const isSelectedDate = isSelected(day);

            return (
              <div
                key={day}
                className={cn(
                  "p-1 h-24 border border-gray-200 cursor-pointer transition-colors hover:bg-gray-50 rounded",
                  isTodayDate && "bg-blue-50 border-blue-300",
                  isSelectedDate && "bg-blue-100 border-blue-400"
                )}
                onClick={() => setSelectedDate(new Date(currentYear, currentMonth, day))}
              >
                <div className={cn(
                  "text-sm font-medium mb-1",
                  isTodayDate ? "text-blue-600" : "text-gray-900"
                )}>
                  {day}
                </div>
                
                {/* Exam indicators */}
                <div className="space-y-1">
                  {dayExams.slice(0, 2).map(exam => (
                    <div
                      key={exam.id}
                      className={cn(
                        "w-full h-1 rounded-full",
                        getExamTypeColor(exam.type)
                      )}
                      title={`${exam.subject} - ${exam.startTime}`}
                    />
                  ))}
                  {dayExams.length > 2 && (
                    <div className="text-xs text-gray-500">
                      +{dayExams.length - 2} more
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Selected day exams */}
        {selectedDate && (
          <div className="mt-4 p-4 bg-gray-50 rounded-lg">
            <h4 className="font-medium mb-3">
              {selectedDate.toLocaleDateString('en-US', { 
                weekday: 'long', 
                month: 'long', 
                day: 'numeric' 
              })}
            </h4>
            
            {getExamsForDay(selectedDate.getDate()).length === 0 ? (
              <p className="text-sm text-gray-500">No exams scheduled</p>
            ) : (
              <div className="space-y-3">
                {getExamsForDay(selectedDate.getDate()).map(exam => (
                  <div key={exam.id} className="p-3 bg-white rounded-lg border">
                    <div className="flex items-start justify-between mb-2">
                      <h5 className="font-medium text-sm">{exam.title}</h5>
                      <Badge 
                        variant="outline" 
                        className={cn("text-xs", getExamTypeBadge(exam.type))}
                      >
                        {exam.type}
                      </Badge>
                    </div>
                    
                    <div className="space-y-1 text-xs text-gray-600">
                      <div className="flex items-center gap-2">
                        <Clock className="h-3 w-3" />
                        <span>{exam.startTime} - {exam.endTime}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <MapPin className="h-3 w-3" />
                        <span>{exam.room}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <User className="h-3 w-3" />
                        <span>{exam.teacher}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Legend */}
        <div className="mt-4 p-3 bg-gray-50 rounded-lg">
          <h5 className="font-medium text-sm mb-2">Exam Types</h5>
          <div className="flex flex-wrap gap-3 text-xs">
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 bg-red-500 rounded-full"></div>
              <span>Final</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 bg-orange-500 rounded-full"></div>
              <span>Midterm</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 bg-blue-400 rounded-full"></div>
              <span>Quiz</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 bg-green-500 rounded-full"></div>
              <span>Test</span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default ExamCalendar;
