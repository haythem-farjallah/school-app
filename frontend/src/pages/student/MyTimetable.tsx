import { useState, useEffect } from 'react';
import { StudentTimetableView } from '../../features/timetable/components/StudentTimetableView';
import { Card, CardContent } from '../../components/ui/card';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { http } from '../../lib/http';
import toast from 'react-hot-toast';

interface StudentClass {
  id: number;
  name: string;
  level: string;
}

export default function MyTimetable() {
  const [studentClass, setStudentClass] = useState<StudentClass | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchStudentClass = async () => {
      try {
        // Try to get current student's class information
        // This would typically come from authentication context
        // For now, we'll try to get it from the API or use a default
        
        let classInfo = null;
        
        try {
          // Try to get student dashboard first
          const dashboardResponse = await http.get('/v1/dashboard/current-user');
          if (dashboardResponse?.data?.enrolledClasses?.[0]) {
            const enrolledClass = dashboardResponse.data.enrolledClasses[0];
            classInfo = {
              id: enrolledClass.classId,
              name: enrolledClass.className,
              level: enrolledClass.className // Use className as level fallback
            };
          }
        } catch (error) {
          console.log('No student dashboard found, trying alternative methods...');
        }
        
        if (!classInfo) {
          try {
            // Fallback: get first available class
            const classesResponse = await http.get('/v1/classes?size=1');
            if (classesResponse?.data?.content?.[0]) {
              classInfo = classesResponse.data.content[0];
              toast.info('Using default class for demonstration');
            } else if (classesResponse?.data?.[0]) {
              classInfo = classesResponse.data[0];
              toast.info('Using default class for demonstration');
            }
          } catch (error) {
            console.error('Failed to fetch classes:', error);
          }
        }
        
        if (classInfo) {
          setStudentClass(classInfo);
        } else {
          toast.error('No class information found. Please contact your administrator.');
        }
        
      } catch (error) {
        console.error('Error fetching student class:', error);
        toast.error('Failed to load class information');
      } finally {
        setIsLoading(false);
      }
    };

    fetchStudentClass();
  }, []);

  if (isLoading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <Card>
          <CardContent className="p-8">
            <div className="flex items-center justify-center">
              <RefreshCw className="w-6 h-6 animate-spin mr-2" />
              <span>Loading your class information...</span>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!studentClass) {
    return (
      <div className="container mx-auto px-4 py-8">
        <Card>
          <CardContent className="p-8">
            <div className="text-center">
              <AlertCircle className="w-12 h-12 mx-auto mb-4 text-red-500" />
              <h3 className="text-lg font-semibold text-red-600 mb-2">No Class Information</h3>
              <p className="text-gray-600">
                We couldn't find your class information. Please contact your administrator to get access to your timetable.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <StudentTimetableView 
        classId={studentClass.id} 
        className={studentClass.name}
      />
    </div>
  );
}
