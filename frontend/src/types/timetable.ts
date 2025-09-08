import { Teacher } from './teacher';
import { Period } from './period';
import { Course } from './course';
import { Room } from './room';

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface TimetableSlot {
  id: number;
  dayOfWeek: DayOfWeek; // Backend sends enum values like 'MONDAY', 'TUESDAY', etc.
  period?: Period; // For nested period object (when creating)
  periodId?: number; // For flat structure (when retrieving)
  periodIndex?: number; // Additional period info from API
  teacher?: Teacher;
  forClass?: { id: number; name: string }; // Backend sends the full class object
  forCourse?: Course;
  room?: Room;
  description?: string;
  timetable?: { id: number }; // Backend includes timetable reference
}

export interface Timetable {
  id: number;
  name: string;
  academicYear: string;
  semester: string;
  slots: TimetableSlot[];
  classIds: number[];
  teacherIds: number[];
  roomIds: number[];
} 