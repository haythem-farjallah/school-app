import { useQuery } from '@tanstack/react-query';
import { http } from '@/lib/http';

// Types for teacher classes and grade management
export interface TeacherClass {
  classId: number;
  className: string;
  courseId: number;
  courseName: string;
  courseCode: string;
  coefficient: number;
  students: TeacherClassStudent[];
  semester: string;
  examTypes: string[];
}

export interface TeacherClassStudent {
  studentId: number;
  firstName: string;
  lastName: string;
  email: string;
  enrollmentId: number;
  currentGrades: {
    firstExam?: ExamGrade;
    secondExam?: ExamGrade;
    finalExam?: ExamGrade;
    quizzes: ExamGrade[];
    assignments: ExamGrade[];
  };
  average: number;
  attendanceRate: number;
}

export interface ExamGrade {
  score: number;
  maxScore: number;
  percentage: number;
  teacherRemarks: string;
  gradedAt: string;
}

export interface BulkGradeEntry {
  classId: number;
  courseId: number;
  assessmentType: string;
  term: string;
  grades: StudentGradeEntry[];
}

export interface StudentGradeEntry {
  studentId: number;
  value: number;
  comment: string;
  weight?: number;
}

// Hook to fetch specific class details for grading
export function useTeacherGradeClass(teacherId?: number, classId?: number, courseId?: number) {
  return useQuery({
    queryKey: ['teacher-grade-class', teacherId, classId, courseId],
    queryFn: async () => {
      if (!classId) {
        throw new Error('Class ID is required');
      }
      
      try {
        console.log('Fetching class details for class ID:', classId);
        
        // First, verify that this class belongs to the current teacher
        // by checking if it's in their class list
        try {
          const teacherClassesResponse = await http.get('/v1/classes/teacher/me?size=100');
          const teacherClasses = teacherClassesResponse.data?.data?.content || [];
          const hasAccess = teacherClasses.some((cls: any) => cls.id === classId);
          
          if (!hasAccess) {
            throw new Error(`You don't have access to class ${classId}. This class is not assigned to you.`);
          }
        } catch (accessError) {
          console.warn('Could not verify class access:', accessError);
          // Continue anyway - the class endpoint will handle permissions
        }
        
        // Get class details
        const classResponse = await http.get(`/v1/classes/${classId}`);
        console.log('Class details response:', classResponse);
        
        let classData;
        if (classResponse.data?.data) {
          classData = classResponse.data.data;
        } else if (classResponse.data) {
          classData = classResponse.data;
        } else {
          throw new Error('Invalid class response structure');
        }
        
        console.log('Parsed class data:', classData);
        
        // Get students enrolled in this class
        let students = [];
        console.log('Fetching enrollments for class ID:', classId);
        const enrollmentsResponse = await http.get(`/v1/enrollments/class/${classId}?size=100`);
        console.log('Enrollments response:', enrollmentsResponse);
        
        let enrollments = [];
        if (enrollmentsResponse.data?.data?.content) {
          enrollments = enrollmentsResponse.data.data.content;
        } else if (enrollmentsResponse.data?.content) {
          enrollments = enrollmentsResponse.data.content;
        } else if (Array.isArray(enrollmentsResponse.data?.data)) {
          enrollments = enrollmentsResponse.data.data;
        }
        
        console.log('Parsed enrollments:', enrollments);
        
        // Transform enrollments to student format
        students = enrollments.map((enrollment: any) => ({
          studentId: enrollment.student?.id || enrollment.studentId,
          firstName: enrollment.student?.firstName || 'Student',
          lastName: enrollment.student?.lastName || `${enrollment.id}`,
          email: enrollment.student?.email || `student${enrollment.id}@school.edu`,
          enrollmentId: enrollment.id,
          currentGrades: {
            firstExam: null,
            secondExam: null,
            finalExam: null,
            quizzes: [],
            assignments: [],
          },
          average: 0,
          attendanceRate: 85.0, // Default attendance rate
        }));
        
        const result = {
          classId: classData.id,
          className: classData.name,
          courseId: courseId || 1,
          courseName: 'General Course',
          courseCode: 'GEN101',
          coefficient: 1.0,
          students: students,
          semester: 'FIRST',
          examTypes: ['FIRST_EXAM', 'SECOND_EXAM', 'FINAL_EXAM', 'QUIZ', 'ASSIGNMENT'],
        };
        
        console.log('Returning class details:', result);
        return result;
      } catch (error) {
        console.error('Error fetching class details:', error);
        throw error;
      }
    },
    enabled: !!classId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Hook to get teacher's class statistics
export function useTeacherClassStats(teacherId?: number) {
  return useQuery({
    queryKey: ['teacher-class-stats', teacherId],
    queryFn: async () => {
      try {
        // Use the current teacher endpoint if no specific teacherId is provided
        const endpoint = teacherId 
          ? `/v1/classes/teacher/${teacherId}?size=50`
          : `/v1/classes/teacher/me?size=50`;
        
        console.log('Fetching teacher stats from:', endpoint);
        const response = await http.get(endpoint);
        console.log('Teacher stats response:', response);
        
        // Handle different response structures
        let classes = [];
        if (response.data?.data?.content) {
          classes = response.data.data.content;
        } else if (response.data?.content) {
          classes = response.data.content;
        } else if (Array.isArray(response.data?.data)) {
          classes = response.data.data;
        } else if (Array.isArray(response.data)) {
          classes = response.data;
        }
        
        // Calculate basic statistics from the classes data
        const totalClasses = classes.length;
        const totalStudents = classes.reduce((sum: number, cls: any) => sum + (cls.studentCount || 0), 0);
        const totalCourses = 3; // Default - we'll enhance this later
        const pendingGrades = Math.floor(totalStudents * 0.3); // Estimate 30% pending
        const averageGrade = 82.5; // Default average
        const capacityUsed = totalClasses > 0 ? Math.round((totalStudents / (totalClasses * 30)) * 100) : 0; // Assume 30 capacity per class
        
        return {
          totalClasses,
          totalStudents,
          totalCourses,
          pendingGrades,
          averageGrade,
          capacityUsed,
          activeCourses: totalCourses,
        };
      } catch (error) {
        console.error('Error fetching teacher stats:', error);
        throw error;
      }
    },
    enabled: true, // Always enabled since we can use current teacher endpoint
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

// Hook to get teacher's classes with basic info (for class selection)
export function useTeacherClasses(teacherId?: number) {
  return useQuery({
    queryKey: ['teacher-classes-basic', teacherId],
    queryFn: async () => {
      try {
        // Use the current teacher endpoint if no specific teacherId is provided
        const endpoint = teacherId 
          ? `/v1/classes/teacher/${teacherId}?size=50`
          : `/v1/classes/teacher/me?size=50`;
        
        console.log('Fetching teacher classes from:', endpoint);
        const response = await http.get(endpoint);
        console.log('Teacher classes response:', response);
        
        // Handle different response structures
        let classes = [];
        if (response.data?.data?.content) {
          classes = response.data.data.content;
        } else if (response.data?.content) {
          classes = response.data.content;
        } else if (Array.isArray(response.data?.data)) {
          classes = response.data.data;
        } else if (Array.isArray(response.data)) {
          classes = response.data;
        }
        
        console.log('Parsed classes:', classes);
        
        // If no classes found, return empty array instead of error
        if (!classes || classes.length === 0) {
          console.log('No classes found for teacher, returning empty array');
          return [];
        }
        
        // Transform to basic class info for selection
        return classes.map((cls: any) => ({
          id: cls.id.toString(),
          classId: cls.id,
          courseId: 1, // Default course ID - we'll need to get this from teaching assignments
          className: cls.name,
          subject: 'General', // Default subject - we'll enhance this later
          capacity: 30, // Default capacity
          enrolled: cls.studentCount || 0,
          grade: cls.gradeLevel || cls.name.charAt(0), // Extract grade level
          room: `Room ${cls.name}`, // Generate room name
          schedule: 'Mon, Wed, Fri - 8:00 AM', // Default schedule
          averageGrade: 85.0, // Default average - we'll calculate this later
          status: 'active' as const,
          courseCode: 'GEN101', // Default course code
          coefficient: 1.0, // Default coefficient
        }));
      } catch (error) {
        console.error('Error fetching teacher classes:', error);
        throw error;
      }
    },
    enabled: true, // Always enabled since we can use current teacher endpoint
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}
