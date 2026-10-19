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

/** Flattened response from the backend TimetableSlotResponseDto. */
export interface TimetableSlotResponseDto {
  id: number;
  dayOfWeek: DayOfWeek;
  description: string | null;
  periodId: number | null;
  periodIndex: number | null;
  periodStartTime: string | null;
  periodEndTime: string | null;
  forClassId: number | null;
  forClassName: string | null;
  forCourseId: number | null;
  forCourseName: string | null;
  forCourseCode: string | null;
  forCourseColor: string | null;
  teacherId: number | null;
  teacherFirstName: string | null;
  teacherLastName: string | null;
  teacherEmail: string | null;
  roomId: number | null;
  roomName: string | null;
  roomCapacity: number | null;
  createdAt: string | null;
  updatedAt: string | null;
}

/** /me returns slots without a single persisted timetable supplying metadata. */
export interface TimetableResponseDto {
  id: number | null;
  name: string | null;
  description: string | null;
  academicYear: string | null;
  semester: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  slots: TimetableSlotResponseDto[];
}
