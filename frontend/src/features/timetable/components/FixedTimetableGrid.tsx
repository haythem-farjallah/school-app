import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../../components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../../../components/ui/dialog';
import { 
  Clock, 
  BookOpen, 
  MapPin, 
  Plus,
  Edit,
  Trash2,
  Save,
  RefreshCw,
  User,
  Calendar,
  Palette
} from 'lucide-react';
import { useTimetable, usePeriods } from '../hooks';
import { Teacher } from '../../../types/teacher';
import { Period } from '../../../types/period';
import { TimetableSlot, DayOfWeek } from '../../../types/timetable';
import { Course } from '../../../types/course';
import { Room } from '../../../types/room';
import { http } from '../../../lib/http';
import toast from 'react-hot-toast';

interface FixedTimetableGridProps {
  classId: number;
}

interface SlotEditData {
  dayOfWeek: DayOfWeek;
  periodId: number;
  teacherId?: number;
  courseId?: number;
  roomId?: number;
  description?: string;
}

interface TimetableApiResponse {
  status: string;
  data: {
    slots: TimetableSlot[];
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

// Teacher-Course color combinations for easy identification
const TEACHER_COLORS = [
  { bg: 'bg-blue-50', border: 'border-blue-200', text: 'text-blue-800', accent: 'bg-blue-500' },
  { bg: 'bg-green-50', border: 'border-green-200', text: 'text-green-800', accent: 'bg-green-500' },
  { bg: 'bg-purple-50', border: 'border-purple-200', text: 'text-purple-800', accent: 'bg-purple-500' },
  { bg: 'bg-orange-50', border: 'border-orange-200', text: 'text-orange-800', accent: 'bg-orange-500' },
  { bg: 'bg-pink-50', border: 'border-pink-200', text: 'text-pink-800', accent: 'bg-pink-500' },
  { bg: 'bg-indigo-50', border: 'border-indigo-200', text: 'text-indigo-800', accent: 'bg-indigo-500' },
  { bg: 'bg-cyan-50', border: 'border-cyan-200', text: 'text-cyan-800', accent: 'bg-cyan-500' },
  { bg: 'bg-emerald-50', border: 'border-emerald-200', text: 'text-emerald-800', accent: 'bg-emerald-500' },
];

export function FixedTimetableGrid({ classId }: FixedTimetableGridProps) {
  const [editingSlot, setEditingSlot] = useState<SlotEditData | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [teacherColorMap, setTeacherColorMap] = useState<Map<number, typeof TEACHER_COLORS[0]>>(new Map());

  // Data fetching hooks
  const { data: timetableData, isLoading: timetableLoading, refetch: refetchTimetable } = useTimetable(classId);
  const { data: periodsData, isLoading: periodsLoading } = usePeriods();

  // Extract data from API responses
  const periods = Array.isArray(periodsData) ? periodsData : [];
  const timetableSlots = (timetableData as TimetableApiResponse)?.data?.slots || (timetableData as unknown as { slots: TimetableSlot[] })?.slots || [];

  // Sort periods by index
  const sortedPeriods = periods.length > 0 ? [...periods].sort((a: Period, b: Period) => (a.index || 0) - (b.index || 0)) : [];

  // Fetch teachers, courses, and rooms directly
  useEffect(() => {
    const fetchData = async () => {
      try {
        // Fetch teachers
        const teachersResponse = await http.get('/admin/teachers?size=100');
        const teachersData = teachersResponse?.data || teachersResponse || [];
        setTeachers(Array.isArray(teachersData) ? teachersData : []);

        // Fetch courses
        const coursesResponse = await http.get('/v1/courses?size=100');
        const coursesData = coursesResponse?.data || coursesResponse || [];
        setCourses(Array.isArray(coursesData) ? coursesData : []);

        // Fetch rooms
        const roomsResponse = await http.get('/v1/rooms?size=100');
        const roomsData = roomsResponse?.data || roomsResponse || [];
        setRooms(Array.isArray(roomsData) ? roomsData : []);

        console.log('Fetched data:', { 
          teachers: teachersData, 
          courses: coursesData, 
          rooms: roomsData 
        });
      } catch (error) {
        console.error('Error fetching data:', error);
        toast.error('Failed to load teachers, courses, or rooms');
      }
    };

    fetchData();
  }, []);

  // Create teacher color mapping
  useEffect(() => {
    const colorMap = new Map<number, typeof TEACHER_COLORS[0]>();
    teachers.forEach((teacher: Teacher, index: number) => {
      colorMap.set(teacher.id, TEACHER_COLORS[index % TEACHER_COLORS.length]);
    });
    setTeacherColorMap(colorMap);
  }, [teachers]);

  // Create a map of slots by day and period
  const slotMap = new Map<string, TimetableSlot>();
  if (Array.isArray(timetableSlots)) {
    timetableSlots.forEach((slot: TimetableSlot) => {
      if (slot.period?.id) {
        const key = `${slot.dayOfWeek}-${slot.period.id}`;
        slotMap.set(key, slot);
      }
    });
  }

  // Get color for a slot based on teacher
  const getSlotColor = (slot: TimetableSlot) => {
    if (slot.teacher?.id) {
      return teacherColorMap.get(slot.teacher.id) || TEACHER_COLORS[0];
    }
    return { bg: 'bg-gray-50', border: 'border-gray-200', text: 'text-gray-800', accent: 'bg-gray-400' };
  };

  // Handle slot click for editing
  const handleSlotClick = (dayOfWeek: DayOfWeek, period: Period, existingSlot?: TimetableSlot) => {
    console.log('Slot clicked:', { dayOfWeek, period, existingSlot });
    
    try {
      setEditingSlot({
        dayOfWeek,
        periodId: period.id,
        teacherId: existingSlot?.teacher?.id,
        courseId: existingSlot?.forCourse?.id,
        roomId: existingSlot?.room?.id,
        description: existingSlot?.description || ''
      });
      setIsDialogOpen(true);
    } catch (error) {
      console.error('Error handling slot click:', error);
      toast.error('Error opening slot editor');
    }
  };

  // Save slot changes
  const handleSaveSlot = async () => {
    if (!editingSlot) return;

    setIsLoading(true);
    try {
      const existingSlot = slotMap.get(`${editingSlot.dayOfWeek}-${editingSlot.periodId}`);
      
      const slotData = {
        dayOfWeek: editingSlot.dayOfWeek,
        periodId: editingSlot.periodId,
        forClassId: classId,
        forCourseId: editingSlot.courseId || null,
        teacherId: editingSlot.teacherId || null,
        roomId: editingSlot.roomId || null,
        description: editingSlot.description || null
      };

      console.log('Saving slot data:', slotData);

      if (existingSlot) {
        // Update existing slot
        await http.put(`/v1/timetables/slots/${existingSlot.id}`, slotData);
        toast.success('Slot updated successfully');
      } else {
        // Create new slot
        await http.post('/v1/timetables/slots', slotData);
        toast.success('Slot created successfully');
      }

      await refetchTimetable();
      setIsDialogOpen(false);
      setEditingSlot(null);
    } catch (error) {
      console.error('Error saving slot:', error);
      toast.error('Failed to save slot');
    } finally {
      setIsLoading(false);
    }
  };

  // Delete slot
  const handleDeleteSlot = async (slotId: number) => {
    if (!confirm('Are you sure you want to delete this slot?')) return;

    setIsLoading(true);
    try {
      await http.delete(`/v1/timetables/slots/${slotId}`);
      toast.success('Slot deleted successfully');
      await refetchTimetable();
    } catch (error) {
      console.error('Error deleting slot:', error);
      toast.error('Failed to delete slot');
    } finally {
      setIsLoading(false);
    }
  };

  // Auto-generate timetable
  const handleAutoGenerate = async () => {
    if (!confirm('This will replace the existing timetable. Are you sure?')) return;

    setIsLoading(true);
    try {
      // Use the debug endpoint that we know exists
      await http.post(`/v1/timetables/debug/class/${classId}/optimize`);
      toast.success('Timetable generated successfully');
      await refetchTimetable();
    } catch (error) {
      console.error('Error generating timetable:', error);
      // Try alternative endpoint
      try {
        await http.post(`/v1/timetables/class/${classId}/optimize`);
        toast.success('Timetable generated successfully');
        await refetchTimetable();
      } catch (altError) {
        console.error('Alternative endpoint also failed:', altError);
        toast.error('Failed to generate timetable. Please check if the optimization service is running.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  if (timetableLoading || periodsLoading) {
    return (
      <Card>
        <CardContent className="p-8">
          <div className="flex items-center justify-center">
            <RefreshCw className="w-6 h-6 animate-spin mr-2" />
            <span>Loading timetable data...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header with actions */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Class Timetable</h2>
          <p className="text-gray-600">
            Manage the weekly schedule for this class. Click on any slot to edit.
          </p>
        </div>
        <div className="flex gap-2">
          <Button 
            onClick={() => refetchTimetable()} 
            variant="outline"
            disabled={isLoading}
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
          <Button 
            onClick={handleAutoGenerate}
            disabled={isLoading}
          >
            <Calendar className="w-4 h-4 mr-2" />
            Auto Generate
          </Button>
        </div>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Total Slots</p>
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
                <p className="text-sm text-gray-600">Teachers</p>
                <p className="text-2xl font-bold text-green-600">{teachers.length}</p>
              </div>
              <User className="w-8 h-8 text-green-600" />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Courses</p>
                <p className="text-2xl font-bold text-purple-600">{courses.length}</p>
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
                <p className="text-2xl font-bold text-orange-600">{rooms.length}</p>
              </div>
              <MapPin className="w-8 h-8 text-orange-600" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Teacher Color Legend */}
      {teachers.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Palette className="w-5 h-5" />
              Teacher Color Legend
            </CardTitle>
          </CardHeader>
          <CardContent>
                         <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2">
               {teachers.map((teacher: Teacher) => {
                 const colors = teacherColorMap.get(teacher.id) || TEACHER_COLORS[0];
                 return (
                   <div key={teacher.id} className={`flex items-center gap-2 p-2 rounded ${colors.bg} ${colors.border} border`}>
                     <div className={`w-3 h-3 rounded-full ${colors.accent}`}></div>
                     <span className={`text-sm font-medium ${colors.text}`}>
                       {teacher.firstName} {teacher.lastName}
                     </span>
                   </div>
                 );
               })}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Timetable Grid */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="w-5 h-5" />
            Weekly Schedule
          </CardTitle>
        </CardHeader>
        <CardContent>
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
                {sortedPeriods.map(period => (
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
                      const colors = slot ? getSlotColor(slot) : null;

                      return (
                        <div
                          key={`${day}-${period.id}`}
                          className={`
                            p-3 border-2 rounded-lg min-h-[120px] cursor-pointer transition-all duration-200
                            ${slot && colors
                              ? `${colors.bg} ${colors.border} hover:opacity-80` 
                              : 'border-dashed border-gray-300 hover:border-gray-400 hover:bg-gray-50'
                            }
                          `}
                          onClick={() => handleSlotClick(day, period, slot)}
                        >
                          {slot ? (
                            <div className="space-y-2">
                              {/* Teacher with color indicator */}
                              {slot.teacher && colors && (
                                <div className="flex items-center gap-2">
                                  <div className={`w-3 h-3 rounded-full ${colors.accent}`}></div>
                                  <div className={`font-semibold text-sm ${colors.text}`}>
                                    {slot.teacher.firstName} {slot.teacher.lastName}
                                  </div>
                                </div>
                              )}

                              {/* Course */}
                              {slot.forCourse && (
                                <div className={`text-sm flex items-center gap-1 ${colors?.text || 'text-gray-700'}`}>
                                  <BookOpen className="w-3 h-3" />
                                  {slot.forCourse.name}
                                </div>
                              )}

                              {/* Room */}
                              {slot.room && (
                                <div className={`text-xs flex items-center gap-1 ${colors?.text || 'text-gray-700'}`}>
                                  <MapPin className="w-3 h-3" />
                                  {slot.room.name}
                                </div>
                              )}

                              {/* Description */}
                              {slot.description && (
                                <div className={`text-xs italic ${colors?.text || 'text-gray-600'}`}>
                                  {slot.description}
                                </div>
                              )}

                              {/* Actions */}
                              <div className="flex gap-1 mt-2">
                                <Button
                                  size="sm"
                                  variant="outline"
                                  className="h-6 px-2 text-xs"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleSlotClick(day, period, slot);
                                  }}
                                >
                                  <Edit className="w-3 h-3" />
                                </Button>
                                <Button
                                  size="sm"
                                  variant="outline"
                                  className="h-6 px-2 text-xs text-red-600 hover:text-red-700"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleDeleteSlot(slot.id);
                                  }}
                                >
                                  <Trash2 className="w-3 h-3" />
                                </Button>
                              </div>
                            </div>
                          ) : (
                            <div className="flex items-center justify-center h-full text-gray-400">
                              <Plus className="w-6 h-6" />
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
        </CardContent>
      </Card>

      {/* Edit Slot Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>
              {editingSlot && slotMap.get(`${editingSlot.dayOfWeek}-${editingSlot.periodId}`) 
                ? 'Edit Slot' 
                : 'Create Slot'
              }
            </DialogTitle>
            <DialogDescription>
              Configure the details for this time slot.
            </DialogDescription>
          </DialogHeader>

          {editingSlot && (
            <div className="space-y-4">
              {/* Slot Info */}
              <div className="p-3 bg-gray-50 rounded">
                                 <p className="text-sm font-medium">
                   {DAY_NAMES[editingSlot.dayOfWeek as keyof typeof DAY_NAMES]} - Period {
                     sortedPeriods.find((p: Period) => p.id === editingSlot.periodId)?.index
                   }
                 </p>
                <p className="text-xs text-gray-600">
                  {sortedPeriods.find(p => p.id === editingSlot.periodId)?.startTime} - {
                    sortedPeriods.find(p => p.id === editingSlot.periodId)?.endTime
                  }
                </p>
              </div>

              {/* Teacher Selection */}
              <div>
                <label className="text-sm font-medium text-gray-700 mb-2 block">
                  Teacher
                </label>
                                 <Select
                   value={editingSlot.teacherId?.toString() || ''}
                   onValueChange={(value: string) => 
                     setEditingSlot((prev: SlotEditData | null) => prev ? { ...prev, teacherId: value ? Number(value) : undefined } : null)
                   }
                 >
                   <SelectTrigger>
                     <SelectValue placeholder="Select a teacher" />
                   </SelectTrigger>
                   <SelectContent>
                     <SelectItem value="">No teacher</SelectItem>
                     {teachers.map((teacher: Teacher) => {
                       const colors = teacherColorMap.get(teacher.id);
                       return (
                         <SelectItem key={teacher.id} value={teacher.id.toString()}>
                           <div className="flex items-center gap-2">
                             {colors && <div className={`w-3 h-3 rounded-full ${colors.accent}`}></div>}
                             {teacher.firstName} {teacher.lastName}
                           </div>
                         </SelectItem>
                       );
                     })}
                   </SelectContent>
                 </Select>
              </div>

              {/* Course Selection */}
              <div>
                <label className="text-sm font-medium text-gray-700 mb-2 block">
                  Course
                </label>
                                 <Select
                   value={editingSlot.courseId?.toString() || ''}
                   onValueChange={(value: string) => 
                     setEditingSlot((prev: SlotEditData | null) => prev ? { ...prev, courseId: value ? Number(value) : undefined } : null)
                   }
                 >
                   <SelectTrigger>
                     <SelectValue placeholder="Select a course" />
                   </SelectTrigger>
                   <SelectContent>
                     <SelectItem value="">No course</SelectItem>
                     {courses.map((course: Course) => (
                       <SelectItem key={course.id} value={course.id.toString()}>
                         {course.name}
                       </SelectItem>
                     ))}
                   </SelectContent>
                 </Select>
              </div>

              {/* Room Selection */}
              <div>
                <label className="text-sm font-medium text-gray-700 mb-2 block">
                  Room
                </label>
                                 <Select
                   value={editingSlot.roomId?.toString() || ''}
                   onValueChange={(value: string) => 
                     setEditingSlot((prev: SlotEditData | null) => prev ? { ...prev, roomId: value ? Number(value) : undefined } : null)
                   }
                 >
                   <SelectTrigger>
                     <SelectValue placeholder="Select a room" />
                   </SelectTrigger>
                   <SelectContent>
                     <SelectItem value="">No room</SelectItem>
                     {rooms.map((room: Room) => (
                       <SelectItem key={room.id} value={room.id.toString()}>
                         {room.name}
                       </SelectItem>
                     ))}
                   </SelectContent>
                 </Select>
              </div>

              {/* Description */}
              <div>
                <label className="text-sm font-medium text-gray-700 mb-2 block">
                  Description (Optional)
                </label>
                                 <Input
                   value={editingSlot.description || ''}
                   onChange={(e: React.ChangeEvent<HTMLInputElement>) => 
                     setEditingSlot((prev: SlotEditData | null) => prev ? { ...prev, description: e.target.value } : null)
                   }
                   placeholder="Add a description..."
                 />
              </div>

              {/* Actions */}
              <div className="flex gap-2 pt-4">
                <Button
                  onClick={handleSaveSlot}
                  disabled={isLoading}
                  className="flex-1"
                >
                  {isLoading ? (
                    <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                  ) : (
                    <Save className="w-4 h-4 mr-2" />
                  )}
                  Save
                </Button>
                <Button
                  onClick={() => setIsDialogOpen(false)}
                  variant="outline"
                  disabled={isLoading}
                >
                  Cancel
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
