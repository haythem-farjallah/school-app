import { useNavigate } from "react-router-dom";
import { Calendar, CheckCircle, BookOpen, Clock, User } from "lucide-react";

// Components
import { SimpleStudentLayout } from "@/components/Dashboard/SimpleStudentLayout";
import StudentWeeklyTimetable from "@/components/student/StudentWeeklyTimetable";
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
    attendanceRate: 92,
    totalClasses: 25,
    presentDays: 23,
    upcomingClasses: 5
  };

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
      title: "Class Starting Soon",
      message: "Mathematics class starts in 30 minutes",
      type: "info" as const,
      isRead: false,
      timestamp: new Date()
    },
    {
      id: "2",
      title: "Attendance Reminder",
      message: "Don't forget to check in for today's classes",
      type: "warning" as const,
      isRead: false,
      timestamp: new Date()
    }
  ];

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
      <div className="space-y-8">
        {/* Welcome Section */}
        <div className="bg-gradient-to-r from-blue-600 via-blue-700 to-indigo-700 rounded-xl p-6 text-white">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold mb-2">
                Welcome back, {user?.firstName}! 👋
              </h2>
              <p className="text-blue-100">
                Ready for another great day of learning? Here's your schedule overview.
              </p>
            </div>
            <div className="hidden md:flex items-center space-x-4">
              <div className="text-center">
                <div className="text-3xl font-bold">{studentData.attendanceRate}%</div>
                <div className="text-sm text-blue-100">Attendance</div>
              </div>
              <div className="w-px h-12 bg-blue-400"></div>
              <div className="text-center">
                <div className="text-3xl font-bold">{studentData.totalClasses}</div>
                <div className="text-sm text-blue-100">Total Classes</div>
              </div>
            </div>
          </div>
        </div>

        {/* Quick Stats Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Card className="hover:shadow-lg transition-shadow duration-200">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Attendance Rate</p>
                  <p className="text-3xl font-bold text-green-600 mt-1">{studentData.attendanceRate}%</p>
                </div>
                <div className="p-3 bg-green-100 rounded-full">
                  <CheckCircle className="w-6 h-6 text-green-600" />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="hover:shadow-lg transition-shadow duration-200">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Total Classes</p>
                  <p className="text-3xl font-bold text-blue-600 mt-1">{studentData.totalClasses}</p>
                </div>
                <div className="p-3 bg-blue-100 rounded-full">
                  <Calendar className="w-6 h-6 text-blue-600" />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="hover:shadow-lg transition-shadow duration-200">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Present Days</p>
                  <p className="text-3xl font-bold text-purple-600 mt-1">{studentData.presentDays}</p>
                </div>
                <div className="p-3 bg-purple-100 rounded-full">
                  <User className="w-6 h-6 text-purple-600" />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="hover:shadow-lg transition-shadow duration-200">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600">Upcoming Classes</p>
                  <p className="text-3xl font-bold text-orange-600 mt-1">{studentData.upcomingClasses}</p>
                </div>
                <div className="p-3 bg-orange-100 rounded-full">
                  <Clock className="w-6 h-6 text-orange-600" />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Main Content - Only Timetable */}
        <div>
          <StudentWeeklyTimetable />
        </div>

      </div>
    </SimpleStudentLayout>
  );
};

export default StudentDashboard;