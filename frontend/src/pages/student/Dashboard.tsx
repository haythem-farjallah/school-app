import { useNavigate } from "react-router-dom";
import { 
  BookOpen, 
  FileText, 
  Calendar, 
  Award, 
  Clock,
  Target,
  CheckCircle,
  Bell,
  MessageSquare,
  TrendingUp
} from "lucide-react";

// Simple Dashboard Components
import { SimpleStudentLayout } from "@/components/Dashboard/SimpleStudentLayout";
import WeeklySchedule from "@/components/Calendar/WeeklySchedule";
import MiniCalendar from "@/components/Calendar/MiniCalendar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

// Hooks and utilities
import { useAuth } from "@/hooks/useAuth";

const StudentDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  // Simplified student data - only essential information
  const studentData = {
    assignmentsDue: 3,
    averageGrade: 85.4,
    attendanceRate: 92,
    upcomingExams: 2
  };

  // Sample schedule events for weekly view
  const scheduleEvents = [
    {
      id: "1",
      title: "4A - Physics",
      subject: "Physics",
      startTime: "8:00 AM",
      endTime: "8:45 AM",
      day: "Monday",
      color: "bg-blue-50 border-blue-200 text-blue-700",
      room: "Room 4A"
    },
    {
      id: "2",
      title: "1A - Chemistry",
      subject: "Chemistry", 
      startTime: "8:00 AM",
      endTime: "8:45 AM",
      day: "Tuesday",
      color: "bg-green-100 border-green-300 text-green-800",
      room: "Room 1A"
    },
    {
      id: "3",
      title: "Mathematics",
      subject: "Mathematics",
      startTime: "10:00 AM", 
      endTime: "10:45 AM",
      day: "Wednesday",
      color: "bg-purple-100 border-purple-300 text-purple-800",
      room: "Room 2A"
    }
  ];

  // Recent announcements
  const recentAnnouncements = [
    {
      id: "1",
      title: "Midterm Schedule Released",
      message: "Check your schedule for upcoming midterm exams",
      date: new Date(Date.now() - 2 * 60 * 60 * 1000),
      priority: "high" as const
    },
    {
      id: "2",
      title: "Library Hours Extended",
      message: "Library will be open until 10 PM during exam week",
      date: new Date(Date.now() - 4 * 60 * 60 * 1000),
      priority: "medium" as const
    },
    {
      id: "3",
      title: "Science Fair Registration",
      message: "Register for the annual science fair by Friday",
      date: new Date(Date.now() - 6 * 60 * 60 * 1000),
      priority: "low" as const
    }
  ];

  const handleNotificationClick = (notification: { id: string; title: string; message: string }) => {
    console.log("Notification clicked:", notification);
  };

  const dashboardUser = {
    id: user?.id?.toString() || "1",
    name: user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : "Student Name",
    email: user?.email || "student@school.edu",
    role: "Student",
    avatar: undefined, // user?.avatar when available
    lastLogin: new Date(Date.now() - 2 * 60 * 60 * 1000)
  };

  const notifications = [
    {
      id: "1",
      title: "Assignment Due Tomorrow",
      message: "Physics lab report is due tomorrow at 11:59 PM",
      type: "warning" as const,
      isRead: false,
      timestamp: new Date()
    },
    {
      id: "2",
      title: "New Grade Posted",
      message: "Your Mathematics quiz grade has been posted",
      type: "success" as const,
      isRead: false,
      timestamp: new Date()
    }
  ];

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high': return 'bg-red-100 text-red-800 border-red-200';
      case 'medium': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'low': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  return (
    <SimpleStudentLayout
      title="Student Dashboard"
      subtitle="Your academic overview"
      user={dashboardUser}
      notifications={notifications}
      onNotificationClick={handleNotificationClick}
      onProfileClick={() => navigate("/student/profile")}
      onSettingsClick={() => navigate("/student/settings")}
      onLogout={() => navigate("/login")}
    >
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Weekly Schedule */}
        <div className="lg:col-span-2">
          <WeeklySchedule events={scheduleEvents} />
        </div>

        {/* Right Column - Quick Info & Actions */}
        <div className="space-y-6">
          {/* Mini Calendar */}
          <MiniCalendar />
          
          {/* Quick Stats */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TrendingUp className="h-5 w-5" />
                Quick Overview
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Assignments Due</span>
                <Badge variant="destructive">{studentData.assignmentsDue}</Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Average Grade</span>
                <Badge variant="secondary">{studentData.averageGrade}%</Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Attendance Rate</span>
                <Badge variant="secondary">{studentData.attendanceRate}%</Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Upcoming Exams</span>
                <Badge variant="outline">{studentData.upcomingExams}</Badge>
              </div>
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle>Quick Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Button 
                variant="outline" 
                className="w-full justify-start"
                onClick={() => navigate("/student/schedule")}
              >
                <Calendar className="mr-2 h-4 w-4" />
                My Schedule
              </Button>
              <Button 
                variant="outline" 
                className="w-full justify-start"
                onClick={() => navigate("/student/assignments")}
              >
                <FileText className="mr-2 h-4 w-4" />
                Assignments
              </Button>
              <Button 
                variant="outline" 
                className="w-full justify-start"
                onClick={() => navigate("/student/grades")}
              >
                <Award className="mr-2 h-4 w-4" />
                Grades
              </Button>
              <Button 
                variant="outline" 
                className="w-full justify-start"
                onClick={() => navigate("/student/attendance")}
              >
                <CheckCircle className="mr-2 h-4 w-4" />
                Attendance
              </Button>
              <Button 
                variant="outline" 
                className="w-full justify-start"
                onClick={() => navigate("/student/learning-resources")}
              >
                <BookOpen className="mr-2 h-4 w-4" />
                Learning Resources
              </Button>
            </CardContent>
          </Card>

          {/* Recent Announcements */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Bell className="h-5 w-5" />
                Announcements
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {recentAnnouncements.map((announcement) => (
                <div 
                  key={announcement.id}
                  className="p-3 rounded-lg border cursor-pointer hover:bg-gray-50 transition-colors"
                  onClick={() => navigate("/student/announcements")}
                >
                  <div className="flex items-start justify-between mb-2">
                    <h4 className="font-medium text-sm">{announcement.title}</h4>
                    <Badge 
                      variant="outline" 
                      className={`text-xs ${getPriorityColor(announcement.priority)}`}
                    >
                      {announcement.priority}
                    </Badge>
                  </div>
                  <p className="text-xs text-gray-600 line-clamp-2">
                    {announcement.message}
                  </p>
                  <p className="text-xs text-gray-400 mt-1">
                    {announcement.date.toLocaleDateString()}
                  </p>
                </div>
              ))}
              <Button 
                variant="ghost" 
                size="sm" 
                className="w-full"
                onClick={() => navigate("/student/announcements")}
              >
                View All Announcements
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </SimpleStudentLayout>
  );
};

export default StudentDashboard;