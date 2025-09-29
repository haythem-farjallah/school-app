import { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { Button } from '../../../components/ui/button';
import { 
  Clock, 
  BookOpen, 
  MapPin, 
  Users,
  Calendar,
  AlertCircle,
  RefreshCw,
  GraduationCap
} from 'lucide-react';
import { usePeriods } from '../hooks';
import { Period } from '../../../types/period';
import { TimetableSlot, DayOfWeek } from '../../../types/timetable';
import { http } from '../../../lib/http';
import toast from 'react-hot-toast';

interface TeacherTimetableViewProps {
  teacherId: number;
  teacherName?: string;
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

export function TeacherTimetableView({ teacherId, teacherName }: TeacherTimetableViewProps) {
  const [teacherSlots, setTeacherSlots] = useState<TimetableSlot[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  
  // Data fetching hooks
  const { data: periodsData, isLoading: periodsLoading } = usePeriods();

  // Extract and process data with safe fallbacks
  const periods = Array.isArray(periodsData) ? periodsData : [];
  const sortedPeriods = periods.sort((a: Period, b: Period) => (a.index || 0) - (b.index || 0));

  // Create slot map with useMemo for performance
  const slotMap = useMemo(() => {
    const map = new Map<string, TimetableSlot>();
    if (Array.isArray(teacherSlots)) {
      teacherSlots.forEach((slot: TimetableSlot) => {
        if (slot.period?.id) {
          const key = `${slot.dayOfWeek}-${slot.period.id}`;
          map.set(key, slot);
        }
      });
    }
    return map;
  }, [teacherSlots]);

  // Fetch teacher's timetable
  useEffect(() => {
    const fetchTeacherTimetable = async () => {
      setIsLoading(true);
      try {
        console.log('🔍 Fetching timetable for teacher:', teacherId);
        
        const response = await http.get(`/v1/timetables/teacher/${teacherId}`);
        console.log('📥 Teacher timetable response:', response);
        
        let slots: TimetableSlot[] = [];
        
        // Handle response structure
        if (response?.data) {
          if (Array.isArray(response.data)) {
            slots = response.data;
          } else if (response.data.data && Array.isArray(response.data.data)) {
            slots = response.data.data;
          }
        }
        
        console.log('✅ Found teacher slots:', slots.length);
        setTeacherSlots(slots);
        
      } catch (error) {
        console.error('❌ Error fetching teacher timetable:', error);
        toast.error('Failed to load teacher schedule. Please try again.');
        setTeacherSlots([]);
      } finally {
        setIsLoading(false);
      }
    };

    if (teacherId) {
      fetchTeacherTimetable();
    }
  }, [teacherId]);

  // Calculate statistics
  const stats = useMemo(() => {
    const uniqueClasses = new Set(teacherSlots.map(slot => slot.forClass?.id).filter(Boolean));
    const uniqueCourses = new Set(teacherSlots.map(slot => slot.forCourse?.id).filter(Boolean));
    const uniqueRooms = new Set(teacherSlots.map(slot => slot.room?.id).filter(Boolean));
    
    return {
      totalClasses: teacherSlots.length,
      uniqueClasses: uniqueClasses.size,
      uniqueCourses: uniqueCourses.size,
      uniqueRooms: uniqueRooms.size
    };
  }, [teacherSlots]);

  if (isLoading || periodsLoading) {
    return (
      <Card>
        <CardContent className="p-8">
          <div className="flex items-center justify-center">
            <RefreshCw className="w-6 h-6 animate-spin mr-2" />
            <span>Loading your teaching schedule...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">My Teaching Schedule</h2>
          <p className="text-gray-600">
            {teacherName ? `Teacher: ${teacherName}` : `Teacher ID: ${teacherId}`}
          </p>
        </div>
        <div className="flex gap-2">
          <Badge variant="secondary" className="text-sm">
            {stats.totalClasses} classes this week
          </Badge>
          <Button 
            onClick={() => window.location.reload()} 
            variant="outline" 
            size="sm"
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
        </div>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Total Classes</p>
                <p className="text-2xl font-bold text-blue-600">{stats.totalClasses}</p>
              </div>
              <Clock className="w-8 h-8 text-blue-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Classes</p>
                <p className="text-2xl font-bold text-green-600">{stats.uniqueClasses}</p>
              </div>
              <Users className="w-8 h-8 text-green-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Subjects</p>
                <p className="text-2xl font-bold text-purple-600">{stats.uniqueCourses}</p>
              </div>
              <BookOpen className="w-8 h-8 text-purple-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Rooms</p>
                <p className="text-2xl font-bold text-orange-600">{stats.uniqueRooms}</p>
              </div>
              <MapPin className="w-8 h-8 text-orange-600" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Timetable Grid */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="w-5 h-5" />
            Weekly Teaching Schedule
          </CardTitle>
        </CardHeader>
        <CardContent>
          {sortedPeriods.length === 0 ? (
            <div className="text-center py-8">
              <AlertCircle className="w-12 h-12 mx-auto mb-4 text-red-500" />
              <p className="text-red-600 font-medium">No schedule available!</p>
              <p className="text-gray-600 text-sm">Please contact your administrator.</p>
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

                        return (
                          <div
                            key={slotKey}
                            className={`
                              p-3 border-2 rounded-lg min-h-[120px] w-full transition-all duration-200
                              ${slot 
                                ? 'border-solid' 
                                : 'border-dashed border-gray-200'
                              }
                            `}
                            style={{
                              backgroundColor: slot?.forCourse?.color ? `${slot.forCourse.color}15` : '#f9fafb',
                              borderColor: slot?.forCourse?.color ? `${slot.forCourse.color}60` : '#e5e7eb',
                            }}
                          >
                            {slot ? (
                              <div className="space-y-2">
                                {/* Course Header */}
                                {slot.forCourse && (
                                  <div className="flex items-center gap-2 mb-2">
                                    <div 
                                      className="w-4 h-4 rounded-full flex-shrink-0"
                                      style={{ backgroundColor: slot.forCourse.color || '#6b7280' }}
                                    />
                                    <div className="flex-1 min-w-0">
                                      <div className="font-semibold text-sm text-gray-800 truncate">
                                        {slot.forCourse.name}
                                      </div>
                                    </div>
                                  </div>
                                )}

                                {/* Class Info */}
                                {slot.forClass && (
                                  <div className="flex items-center gap-1 text-sm text-gray-700">
                                    <GraduationCap className="w-3 h-3" />
                                    <span className="truncate font-medium">
                                      Class {slot.forClass.name || slot.forClass.id}
                                    </span>
                                  </div>
                                )}

                                {/* Room */}
                                {slot.room && (
                                  <div className="flex items-center gap-1 text-xs text-gray-600">
                                    <MapPin className="w-3 h-3" />
                                    <span className="truncate">{slot.room.name}</span>
                                  </div>
                                )}

                                {/* Time indicator for teacher view */}
                                <div className="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
                                  {period.startTime} - {period.endTime}
                                </div>
                              </div>
                            ) : (
                              <div className="flex flex-col items-center justify-center h-full text-gray-300">
                                <div className="text-xs">Free Period</div>
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
          )}
        </CardContent>
      </Card>
    </div>
  );
}
