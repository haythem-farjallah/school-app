import { useQuery } from '@tanstack/react-query';
import { http } from '../../../lib/http';
import { Teacher } from '../../../types/teacher';
import { Course } from '../../../types/course';
import { Room } from '../../../types/room';

// Hook to test teachers endpoint
export function useTeachersForTimetable() {
  return useQuery({
    queryKey: ['timetable-teachers'],
    queryFn: async () => {
      console.log('🔍 Fetching teachers for timetable...');
      
      // Try multiple endpoints
      const endpoints = [
        '/admin/teachers?size=100',
        '/v1/teachers?size=100'
      ];
      
      for (const endpoint of endpoints) {
        try {
          console.log(`Trying endpoint: ${endpoint}`);
          const response = await http.get(endpoint);
          console.log(`Response from ${endpoint}:`, response);
          
          // Extract data from different response formats
          let teachers = response?.data || response;
          if (teachers?.content) {
            teachers = teachers.content;
          }
          
          if (Array.isArray(teachers) && teachers.length > 0) {
            console.log(`✅ Success! Found ${teachers.length} teachers from ${endpoint}`);
            return teachers as Teacher[];
          }
        } catch (error) {
          console.log(`❌ Failed to fetch from ${endpoint}:`, error);
        }
      }
      
      console.log('⚠️ No teachers found from any endpoint');
      return [];
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

// Hook to test courses endpoint
export function useCoursesForTimetable() {
  return useQuery({
    queryKey: ['timetable-courses'],
    queryFn: async () => {
      console.log('🔍 Fetching courses for timetable...');
      
      try {
        const response = await http.get('/v1/courses?size=100');
        console.log('Courses response:', response);
        
        let courses = response?.data || response;
        if (courses?.content) {
          courses = courses.content;
        }
        
        return Array.isArray(courses) ? courses as Course[] : [];
      } catch (error) {
        console.error('Error fetching courses:', error);
        return [];
      }
    },
    staleTime: 1000 * 60 * 5,
  });
}

// Hook to test rooms endpoint
export function useRoomsForTimetable() {
  return useQuery({
    queryKey: ['timetable-rooms'],
    queryFn: async () => {
      console.log('🔍 Fetching rooms for timetable...');
      
      try {
        const response = await http.get('/v1/rooms?size=100');
        console.log('Rooms response:', response);
        
        let rooms = response?.data || response;
        if (rooms?.content) {
          rooms = rooms.content;
        }
        
        return Array.isArray(rooms) ? rooms as Room[] : [];
      } catch (error) {
        console.error('Error fetching rooms:', error);
        return [];
      }
    },
    staleTime: 1000 * 60 * 5,
  });
}
