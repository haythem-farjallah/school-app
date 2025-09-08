import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { 
  Clock, 
  BookOpen, 
  MapPin, 
  User,
  Calendar,
  GraduationCap,
  Users,
  RefreshCw,
  AlertCircle,
  CheckCircle
} from 'lucide-react';
import { Teacher } from '../../../types/teacher';
import { Period } from '../../../types/period';
import { TimetableSlot, DayOfWeek } from '../../../types/timetable';
import { http } from '../../../lib/http';
import toast from 'react-hot-toast';
import { usePeriods } from '../hooks';

interface TeacherScheduleViewProps {
  teacherId: number;
  teacher?: Teacher;
}

interface TeacherScheduleData {
  teacher: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    subjectsTaught: string;
    weeklyCapacity: number;
  };
  slots: TimetableSlot[];
  workloadSummary: {
    totalSlots: number;
    weeklyHours: number;
    availableHours: number;
    subjects: string[];
    classes: string[];
  };
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

export function TeacherScheduleView({ teacherId, teacher }: TeacherScheduleViewProps) {
  const [scheduleData, setScheduleData] = useState<TeacherScheduleData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { data: periodsData, isLoading: periodsLoading } = usePeriods();
  const periods = Array.isArray(periodsData) ? periodsData : [];
  const sortedPeriods = periods.sort((a: Period, b: Period) => (a.index || 0) - (b.index || 0));

  // Fetch teacher schedule
  const fetchTeacherSchedule = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      // Fetch teacher timetable slots
      const slotsResponse = await http.get(`/v1/timetables/teacher/${teacherId}`);
      const slots = slotsResponse?.data || [];

      // Fetch teacher details if not provided
      let teacherData = teacher;
      if (!teacherData) {
        const teacherResponse = await http.get(`/admin/teachers/${teacherId}`);
        teacherData = teacherResponse?.data;
      }

      if (!teacherData) {
        throw new Error('Teacher not found');
      }

      // Calculate workload summary
      const subjects = [...new Set(slots
        .filter((slot: TimetableSlot) => slot.forCourse?.name)
        .map((slot: TimetableSlot) => slot.forCourse!.name)
      )];

      const classes = [...new Set(slots
        .filter((slot: TimetableSlot) => slot.forClass?.name)
        .map((slot: TimetableSlot) => slot.forClass!.name)
      )];

      const workloadSummary = {
        totalSlots: slots.length,
        weeklyHours: slots.length, // Assuming 1 slot = 1 hour
        availableHours: Math.max(0, (teacherData.weeklyCapacity || 40) - slots.length),
        subjects,
        classes
      };

      setScheduleData({
        teacher: {
          id: teacherData.id,
          firstName: teacherData.firstName,
          lastName: teacherData.lastName,
          email: teacherData.email,
          subjectsTaught: teacherData.subjectsTaught || '',
          weeklyCapacity: teacherData.weeklyCapacity || 40
        },
        slots,
        workloadSummary
      });

    } catch (err) {
      console.error('Error fetching teacher schedule:', err);
      setError('Failed to load teacher schedule');
      toast.error('Failed to load teacher schedule');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (teacherId) {
      fetchTeacherSchedule();
    }
  }, [teacherId]);

  // Create slot map for easy lookup
  const slotMap = new Map<string, TimetableSlot>();
  if (scheduleData?.slots) {
    scheduleData.slots.forEach((slot: TimetableSlot) => {
      if (slot.period?.id) {
        const key = `${slot.dayOfWeek}-${slot.period.id}`;
        slotMap.set(key, slot);
      }
    });
  }

  if (isLoading || periodsLoading) {
    return (
      <Card>
        <CardContent className="p-8">
          <div className="flex items-center justify-center">
            <RefreshCw className="w-6 h-6 animate-spin mr-2" />
            <span>Loading teacher schedule...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error || !scheduleData) {
    return (
      <Card>
        <CardContent className="p-8">
          <div className="text-center">
            <AlertCircle className="w-12 h-12 mx-auto mb-4 text-red-500" />
            <p className="text-red-600 font-medium">{error || 'No schedule data available'}</p>
            <Button 
              onClick={fetchTeacherSchedule}
              variant="outline"
              className="mt-4"
            >
              <RefreshCw className="w-4 h-4 mr-2" />
              Retry
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  const { teacher: teacherInfo, workloadSummary } = scheduleData;
  const workloadPercentage = teacherInfo.weeklyCapacity > 0 
    ? Math.round((workloadSummary.weeklyHours / teacherInfo.weeklyCapacity) * 100)
    : 0;

  return (
    <div className="space-y-6">
      {/* Teacher Header */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                <GraduationCap className="w-6 h-6 text-blue-600" />
              </div>
              <div>
                <CardTitle className="text-2xl">
                  {teacherInfo.firstName} {teacherInfo.lastName}
                </CardTitle>
                <p className="text-gray-600">{teacherInfo.email}</p>
                {teacherInfo.subjectsTaught && (
                  <p className="text-sm text-gray-500 mt-1">
                    Subjects: {teacherInfo.subjectsTaught}
                  </p>
                )}
              </div>
            </div>
            <Button 
              onClick={fetchTeacherSchedule}
              variant="outline"
            >
              <RefreshCw className="w-4 h-4 mr-2" />
              Refresh
            </Button>
          </div>
        </CardHeader>
      </Card>

      {/* Workload Summary */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Weekly Hours</p>
                <p className="text-2xl font-bold text-blue-600">
                  {workloadSummary.weeklyHours}
                </p>
                <p className="text-xs text-gray-500">
                  of {teacherInfo.weeklyCapacity} capacity
                </p>
              </div>
              <Clock className="w-8 h-8 text-blue-600" />
            </div>
            <div className="mt-2">
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div 
                  className={`h-2 rounded-full ${
                    workloadPercentage > 90 ? 'bg-red-500' :
                    workloadPercentage > 70 ? 'bg-yellow-500' : 'bg-green-500'
                  }`}
                  style={{ width: `${Math.min(workloadPercentage, 100)}%` }}
                />
              </div>
              <p className="text-xs text-gray-500 mt-1">{workloadPercentage}% capacity</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Total Slots</p>
                <p className="text-2xl font-bold text-green-600">
                  {workloadSummary.totalSlots}
                </p>
              </div>
              <Calendar className="w-8 h-8 text-green-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Subjects</p>
                <p className="text-2xl font-bold text-purple-600">
                  {workloadSummary.subjects.length}
                </p>
              </div>
              <BookOpen className="w-8 h-8 text-purple-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Classes</p>
                <p className="text-2xl font-bold text-orange-600">
                  {workloadSummary.classes.length}
                </p>
              </div>
              <Users className="w-8 h-8 text-orange-600" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Subject & Class Lists */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BookOpen className="w-5 h-5" />
              Subjects Taught
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-2">
              {workloadSummary.subjects.length > 0 ? (
                workloadSummary.subjects.map((subject, index) => (
                  <Badge key={index} variant="secondary">
                    {subject}
                  </Badge>
                ))
              ) : (
                <p className="text-gray-500 text-sm">No subjects assigned</p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="w-5 h-5" />
              Classes Taught
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap gap-2">
              {workloadSummary.classes.length > 0 ? (
                workloadSummary.classes.map((className, index) => (
                  <Badge key={index} variant="outline">
                    {className}
                  </Badge>
                ))
              ) : (
                <p className="text-gray-500 text-sm">No classes assigned</p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Weekly Schedule */}
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
              <p className="text-red-600 font-medium">No periods configured</p>
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
                              p-3 border-2 rounded-lg min-h-[100px] transition-all duration-200
                              ${slot 
                                ? 'border-solid' 
                                : 'border-dashed border-gray-200'
                              }
                            `}
                            style={{
                              backgroundColor: slot?.forCourse?.color ? `${slot.forCourse.color}15` : undefined,
                              borderColor: slot?.forCourse?.color ? `${slot.forCourse.color}60` : undefined,
                            }}
                          >
                            {slot ? (
                              <div className="space-y-2">
                                {/* Course */}
                                {slot.forCourse && (
                                  <div className="flex items-center gap-2">
                                    <div 
                                      className="w-3 h-3 rounded-full flex-shrink-0"
                                      style={{ backgroundColor: slot.forCourse.color }}
                                    />
                                    <div className="flex-1 min-w-0">
                                      <div className="font-semibold text-sm text-gray-800 truncate">
                                        {slot.forCourse.name}
                                      </div>
                                      <div className="text-xs text-gray-500">
                                        {slot.forCourse.code}
                                      </div>
                                    </div>
                                  </div>
                                )}

                                {/* Class */}
                                {slot.forClass && (
                                  <div className="flex items-center gap-1 text-sm text-gray-700">
                                    <Users className="w-3 h-3" />
                                    <span className="truncate">{slot.forClass.name}</span>
                                  </div>
                                )}

                                {/* Room */}
                                {slot.room && (
                                  <div className="flex items-center gap-1 text-xs text-gray-600">
                                    <MapPin className="w-3 h-3" />
                                    <span className="truncate">{slot.room.name}</span>
                                  </div>
                                )}

                                {/* Status indicator */}
                                <div className="flex items-center gap-1 text-xs">
                                  <CheckCircle className="w-3 h-3 text-green-500" />
                                  <span className="text-green-600">Scheduled</span>
                                </div>
                              </div>
                            ) : (
                              <div className="flex items-center justify-center h-full text-gray-300">
                                <div className="text-center">
                                  <Clock className="w-6 h-6 mx-auto mb-1" />
                                  <p className="text-xs">Free</p>
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
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
