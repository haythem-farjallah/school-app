import { Teacher } from './teacher';
import { Period } from './period';
import { Course } from './course';
import { Room } from './room';

export interface TimetableSlot {
  id: number;
  dayOfWeek: string; // e.g. 'Monday'
  period: Period;
  teacher?: Teacher;
  forClassId: number;
  forCourse?: Course;
  room?: Room;
  description?: string;
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