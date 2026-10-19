import type { ApiResponse } from "@/types/level";
import type { TimetableResponseDto, TimetableSlotResponseDto } from "@/types/timetable";

const mathematics: TimetableSlotResponseDto = {
  id: 41,
  dayOfWeek: "MONDAY",
  description: null,
  periodId: 12,
  periodIndex: 2,
  periodStartTime: "09:15:00",
  periodEndTime: "10:00:00",
  forClassId: 4,
  forClassName: "7A",
  forCourseId: 8,
  forCourseName: "Mathematics",
  forCourseCode: "MATH",
  forCourseColor: "#123456",
  teacherId: 9,
  teacherFirstName: "Nadia",
  teacherLastName: "Ben Ali",
  teacherEmail: "nadia@fixtures.school.test",
  roomId: 3,
  roomName: "Room 12",
  roomCapacity: 28,
  createdAt: "2026-09-01T08:00:00",
  updatedAt: "2026-09-01T08:00:00",
};

// /me is a class-slot view: no single persisted timetable supplies its metadata.
export const studentTimetable: ApiResponse<TimetableResponseDto> = {
  status: "Student timetable retrieved successfully",
  data: {
    id: null, name: null, description: null, academicYear: null, semester: null, createdAt: null, updatedAt: null,
    slots: [
      mathematics,
      { ...mathematics, id: 42, dayOfWeek: "SUNDAY", forCourseId: 10, forCourseName: "History" },
      { ...mathematics, id: 43, dayOfWeek: "MONDAY", periodId: 11, periodIndex: 1,
        periodStartTime: "08:15:00", periodEndTime: "09:00:00", forCourseId: 11, forCourseName: "Physics",
        forClassName: null, teacherFirstName: null, teacherLastName: null, roomName: null },
      { ...mathematics, id: 44, dayOfWeek: "SATURDAY", forCourseId: 12, forCourseName: "Art" },
    ],
  },
};
