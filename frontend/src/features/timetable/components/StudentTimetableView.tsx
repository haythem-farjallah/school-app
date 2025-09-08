import { useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Badge } from '../../../components/ui/badge';
import { 
  Clock, 
  BookOpen, 
  MapPin, 
  User,
  Calendar,
  AlertCircle,
  RefreshCw
} from 'lucide-react';
import { useTimetable, usePeriods } from '../hooks';
import { Period } from '../../../types/period';
import { TimetableSlot, DayOfWeek } from '../../../types/timetable';

interface StudentTimetableViewProps {
  classId: number;
  className?: string;
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

export function StudentTimetableView({ classId, className }: StudentTimetableViewProps) {
  // Data fetching hooks
  const { data: timetableData, isLoading: timetableLoading, refetch: refetchTimetable } = useTimetable(classId);
  const { data: periodsData, isLoading: periodsLoading } = usePeriods();

  // Extract and process data with safe fallbacks
  const periods = Array.isArray(periodsData) ? periodsData : [];
  const timetableSlots = useMemo(() => {
    const data = timetableData as { slots?: TimetableSlot[] } | null | undefined;
    return Array.isArray(data?.slots) ? data.slots : [];
  }, [timetableData]);
  const sortedPeriods = periods.sort((a: Period, b: Period) => (a.index || 0) - (b.index || 0));

  // Create slot map with useMemo for performance
  const slotMap = useMemo(() => {
    const map = new Map<string, TimetableSlot>();
    if (Array.isArray(timetableSlots)) {
      timetableSlots.forEach((slot: TimetableSlot) => {
        if (slot.period?.id) {
          const key = `${slot.dayOfWeek}-${slot.period.id}`;
          map.set(key, slot);
        }
      });
    }
    return map;
  }, [timetableSlots]);

  if (timetableLoading || periodsLoading) {
    return (
      <Card>
        <CardContent className="p-8">
          <div className="flex items-center justify-center">
            <RefreshCw className="w-6 h-6 animate-spin mr-2" />
            <span>Loading your schedule...</span>
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
          <h2 className="text-2xl font-bold text-gray-900">My Schedule</h2>
          <p className="text-gray-600">
            {className ? `Class: ${className}` : `Class ID: ${classId}`}
          </p>
        </div>
        <Badge variant="secondary" className="text-sm">
          {timetableSlots.length} classes this week
        </Badge>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Total Classes</p>
                <p className="text-2xl font-bold text-blue-600">{timetableSlots.length}</p>
              </div>
              <Clock className="w-8 h-8 text-blue-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Subjects</p>
                <p className="text-2xl font-bold text-green-600">
                  {new Set(timetableSlots.map(slot => slot.forCourse?.id).filter(Boolean)).size}
                </p>
              </div>
              <BookOpen className="w-8 h-8 text-green-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Teachers</p>
                <p className="text-2xl font-bold text-purple-600">
                  {new Set(timetableSlots.map(slot => slot.teacher?.id).filter(Boolean)).size}
                </p>
              </div>
              <User className="w-8 h-8 text-purple-600" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Timetable Grid */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="w-5 h-5" />
            Weekly Schedule
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
                              p-3 border-2 rounded-lg min-h-[100px] w-full transition-all duration-200
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

                                {/* Teacher */}
                                {slot.teacher && (
                                  <div className="flex items-center gap-1 text-sm text-gray-700">
                                    <User className="w-3 h-3" />
                                    <span className="truncate">
                                      {slot.teacher.firstName} {slot.teacher.lastName}
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
