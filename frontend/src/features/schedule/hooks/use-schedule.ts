import { useQuery } from '@tanstack/react-query';
import { http } from '../../../lib/http';
import { TimetableSlot } from '../../../types/timetable';
import { useAuth } from '../../../hooks/useAuth';

// Utility functions for safe API data extraction
const safeExtractData = <T>(response: unknown, path: string, fallback: T): T => {
  try {
    const keys = path.split('.');
    let current: unknown = response;
    for (const key of keys) {
      if (current && typeof current === 'object' && key in current) {
        current = (current as Record<string, unknown>)[key];
      } else {
        return fallback;
      }
    }
    return (current as T) || fallback;
  } catch {
    return fallback;
  }
};

// Type definitions for API responses
interface EnrollmentData {
  id: number;
  classEntity: { id: number; name: string };
  status: string;
}

interface ChildData {
  id: number;
  firstName: string;
  lastName: string;
}

interface ScheduleData {
  classId: number;
  className: string;
  slots: TimetableSlot[];
}

interface ChildScheduleData {
  child: ChildData;
  enrollments: EnrollmentData[];
  schedules: ScheduleData[];
  slots: (TimetableSlot & { childName: string; className: string })[];
}

/**
 * Hook to fetch teacher's schedule with assigned classes and time slots
 * @param teacherId - The ID of the teacher
 * @returns Query result with teacher's timetable slots
 */
export function useTeacherSchedule(teacherId?: number) {
  return useQuery({
    queryKey: ['teacher-schedule', teacherId],
    queryFn: async () => {
      try {
        const res = await http.get<{ status: string; data: TimetableSlot[] }>(`/v1/timetables/teacher/${teacherId}`);
        return res.data || [];
      } catch (error) {
        console.warn(`Error loading teacher schedule for ${teacherId}:`, error);
        return [];
      }
    },
    enabled: !!teacherId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

/**
 * Hook to fetch student's enrolled classes and their schedules
 * @param studentId - The ID of the student
 * @returns Query result with student's enrollments, schedules, and combined slots
 */
export function useStudentSchedule(studentId?: number) {
  return useQuery({
    queryKey: ['student-schedule', studentId],
    queryFn: async () => {
      try {
        // First get student's enrollments
        const enrollmentsRes = await http.get(`/v1/enrollments/student/${studentId}?size=100`);
        
        // Handle different response structures safely
        const enrollments: EnrollmentData[] = safeExtractData(enrollmentsRes, 'data.content', []);
      
      // Then get timetable for each enrolled class
      const schedulePromises = enrollments
        .filter((enrollment: EnrollmentData) => enrollment.status === 'ACTIVE')
        .map(async (enrollment: EnrollmentData): Promise<ScheduleData> => {
          try {
            const timetableRes = await http.get(`/v1/timetables/class/${enrollment.classEntity.id}`);
            
            // Handle different response structures safely
            const slots: TimetableSlot[] = safeExtractData(timetableRes, 'data.slots', []);
            
            return {
              classId: enrollment.classEntity.id,
              className: enrollment.classEntity.name,
              slots
            };
          } catch {
            console.warn(`No timetable found for class ${enrollment.classEntity.id}`);
            return {
              classId: enrollment.classEntity.id,
              className: enrollment.classEntity.name,
              slots: []
            };
          }
        });

      const schedules = await Promise.all(schedulePromises);
      
      // Flatten all slots with class information
      const allSlots: (TimetableSlot & { className: string })[] = [];
      schedules.forEach((schedule: ScheduleData) => {
        schedule.slots.forEach((slot: TimetableSlot) => {
          allSlots.push({
            ...slot,
            className: schedule.className
          });
        });
      });

        return {
          enrollments,
          schedules,
          allSlots
        };
      } catch (error) {
        console.warn(`Error loading student schedule for ${studentId}:`, error);
        return {
          enrollments: [],
          schedules: [],
          allSlots: []
        };
      }
    },
    enabled: !!studentId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

/**
 * Hook to fetch parent's children schedules with multi-child support
 * @param parentId - The ID of the parent
 * @returns Query result with children info, individual schedules, and combined slots
 */
export function useParentSchedule(parentId?: number) {
  return useQuery({
    queryKey: ['parent-schedule', parentId],
    queryFn: async () => {
      try {
        // Get parent's children via dashboard (which includes children info)
        const dashboardRes = await http.get(`/v1/dashboard/parent/${parentId}`);
        
        // Handle different response structures safely
        const children: ChildData[] = safeExtractData(dashboardRes, 'data.children', []);
      
      // Get schedule for each child
      const childSchedulePromises = children.map(async (child: ChildData): Promise<ChildScheduleData> => {
        try {
          // Get child's enrollments
          const enrollmentsRes = await http.get(`/v1/enrollments/student/${child.id}?size=100`);
          
          // Handle different response structures safely
          const enrollments: EnrollmentData[] = safeExtractData(enrollmentsRes, 'data.content', []);
          
          // Get timetables for each class
          const schedulePromises = enrollments
            .filter((enrollment: EnrollmentData) => enrollment.status === 'ACTIVE')
            .map(async (enrollment: EnrollmentData): Promise<ScheduleData> => {
              try {
                const timetableRes = await http.get(`/v1/timetables/class/${enrollment.classEntity.id}`);
                
                // Handle different response structures safely
                const slots: TimetableSlot[] = safeExtractData(timetableRes, 'data.slots', []);
                
                return {
                  classId: enrollment.classEntity.id,
                  className: enrollment.classEntity.name,
                  slots
                };
              } catch {
                return {
                  classId: enrollment.classEntity.id,
                  className: enrollment.classEntity.name,
                  slots: []
                };
              }
            });

          const schedules = await Promise.all(schedulePromises);
          
          // Flatten slots with child and class info
          const childSlots: (TimetableSlot & { childName: string; className: string })[] = [];
          schedules.forEach((schedule: ScheduleData) => {
            schedule.slots.forEach((slot: TimetableSlot) => {
              childSlots.push({
                ...slot,
                childName: `${child.firstName} ${child.lastName}`,
                className: schedule.className
              });
            });
          });

          return {
            child,
            enrollments,
            schedules,
            slots: childSlots
          };
        } catch (error) {
          console.warn(`Error loading schedule for child ${child.id}:`, error);
          return {
            child,
            enrollments: [],
            schedules: [],
            slots: []
          };
        }
      });

      const childrenSchedules = await Promise.all(childSchedulePromises);
      
      // Combine all slots
      const allSlots: (TimetableSlot & { childName: string; className: string })[] = [];
      childrenSchedules.forEach((childSchedule: ChildScheduleData) => {
        allSlots.push(...childSchedule.slots);
      });

        return {
          children,
          childrenSchedules,
          allSlots
        };
      } catch (error) {
        console.warn(`Error loading parent schedule for ${parentId}:`, error);
        return {
          children: [],
          childrenSchedules: [],
          allSlots: []
        };
      }
    },
    enabled: !!parentId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

/**
 * Hook to fetch current user's schedule based on their role
 * Automatically determines which schedule hook to use based on user role
 * @returns Combined query results for all possible user roles with loading/error states
 */
export function useMySchedule() {
  const { user } = useAuth();
  
  const teacherSchedule = useTeacherSchedule(
    user?.role === 'TEACHER' ? user.id : undefined
  );
  
  const studentSchedule = useStudentSchedule(
    user?.role === 'STUDENT' ? user.id : undefined
  );
  
  const parentSchedule = useParentSchedule(
    user?.role === 'PARENT' ? user.id : undefined
  );

  return {
    user,
    teacherSchedule,
    studentSchedule,
    parentSchedule,
    isLoading: teacherSchedule.isLoading || studentSchedule.isLoading || parentSchedule.isLoading,
    error: teacherSchedule.error || studentSchedule.error || parentSchedule.error
  };
}
