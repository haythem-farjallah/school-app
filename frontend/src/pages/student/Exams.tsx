import React from 'react';
import { Helmet } from 'react-helmet-async';
import { SimpleStudentLayout } from '@/components/Dashboard/SimpleStudentLayout';
import ExamCalendar from '@/components/Calendar/ExamCalendar';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Clock, MapPin, User, Calendar } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useNavigate } from 'react-router-dom';

const StudentExams = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const dashboardUser = {
    id: user?.id?.toString() || "1",
    name: user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : "Student Name",
    email: user?.email || "student@school.edu",
    role: "Student",
    avatar: undefined,
    lastLogin: new Date(Date.now() - 2 * 60 * 60 * 1000)
  };

  const notifications = [
    {
      id: "1",
      title: "Exam Reminder",
      message: "English midterm exam is tomorrow at 9:00 AM",
      type: "warning" as const,
      isRead: false,
      timestamp: new Date()
    }
  ];

  const upcomingExams = [
    {
      id: '1',
      subject: 'English',
      title: 'English Midterm Exam',
      date: 'December 20, 2024',
      time: '09:00 - 11:00',
      room: 'Room 3A',
      teacher: 'Noah',
      type: 'midterm',
      daysLeft: 5
    },
    {
      id: '2',
      subject: 'Biology',
      title: 'Biology Final Exam',
      date: 'December 22, 2024',
      time: '10:00 - 12:00',
      room: 'Room 2A',
      teacher: 'Daniel',
      type: 'final',
      daysLeft: 7
    },
    {
      id: '3',
      subject: 'Computer Science',
      title: 'Programming Quiz',
      date: 'December 25, 2024',
      time: '14:00 - 15:30',
      room: 'Lab 1',
      teacher: 'Chloe',
      type: 'quiz',
      daysLeft: 10
    }
  ];

  const getExamTypeBadge = (type: string) => {
    switch (type) {
      case 'final': return 'bg-red-100 text-red-800 border-red-200';
      case 'midterm': return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'quiz': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'test': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getDaysLeftColor = (days: number) => {
    if (days <= 2) return 'text-red-600 font-semibold';
    if (days <= 5) return 'text-orange-600 font-medium';
    return 'text-green-600';
  };

  const handleNotificationClick = (notification: any) => {
    console.log("Notification clicked:", notification);
  };

  return (
    <>
      <Helmet>
        <title>Exams - Student Dashboard</title>
        <meta name="description" content="View your upcoming exams and exam schedule" />
      </Helmet>
      
      <SimpleStudentLayout
        title="Exams"
        subtitle="View your exam schedule and upcoming tests"
        user={dashboardUser}
        notifications={notifications}
        onNotificationClick={handleNotificationClick}
        onProfileClick={() => navigate("/student/profile")}
        onSettingsClick={() => navigate("/student/settings")}
        onLogout={() => navigate("/login")}
      >
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column - Exam Calendar */}
          <div className="lg:col-span-2">
            <ExamCalendar />
          </div>

          {/* Right Column - Upcoming Exams */}
          <div className="space-y-6">
            {/* Upcoming Exams List */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Calendar className="h-5 w-5" />
                  Upcoming Exams
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {upcomingExams.map((exam) => (
                  <div 
                    key={exam.id}
                    className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
                  >
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="font-medium text-sm">{exam.subject}</h4>
                      <Badge 
                        variant="outline" 
                        className={`text-xs ${getExamTypeBadge(exam.type)}`}
                      >
                        {exam.type}
                      </Badge>
                    </div>
                    
                    <h5 className="text-xs text-gray-600 mb-3">{exam.title}</h5>
                    
                    <div className="space-y-2 text-xs text-gray-600">
                      <div className="flex items-center gap-2">
                        <Calendar className="h-3 w-3" />
                        <span>{exam.date}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <Clock className="h-3 w-3" />
                        <span>{exam.time}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <MapPin className="h-3 w-3" />
                        <span>{exam.room}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <User className="h-3 w-3" />
                        <span>{exam.teacher}</span>
                      </div>
                    </div>
                    
                    <div className="mt-3 pt-2 border-t border-gray-100">
                      <span className={`text-xs ${getDaysLeftColor(exam.daysLeft)}`}>
                        {exam.daysLeft} days left
                      </span>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>

            {/* Exam Statistics */}
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Exam Statistics</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">Total Exams</span>
                  <Badge variant="secondary">8</Badge>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">Completed</span>
                  <Badge variant="secondary">5</Badge>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">Upcoming</span>
                  <Badge variant="destructive">3</Badge>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">Average Score</span>
                  <Badge variant="secondary">87%</Badge>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </SimpleStudentLayout>
    </>
  );
};

export default StudentExams;
