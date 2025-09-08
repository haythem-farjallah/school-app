import { useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import { 
  Users, 
  CheckSquare, 
  Award,
  Bell,
  TrendingUp,
  Calendar,
  Loader2
} from "lucide-react";

// Dashboard Components
import TeacherSchedule from "@/components/Calendar/TeacherSchedule";
import MiniCalendar from "@/components/Calendar/MiniCalendar";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

// Hooks and utilities
import { useAuth } from "@/hooks/useAuth";
import { useTeacherClassStats } from "@/hooks/useTeacherClasses";

const TeacherDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  
  // Fetch teacher's class statistics
  const { data: stats, isLoading: statsLoading } = useTeacherClassStats();

  // Simplified teacher data - only essential information
  const teacherData = {
    myClasses: stats?.totalClasses || 0,
    totalStudents: stats?.totalStudents || 0,
    pendingGrades: 23, // This would need a separate API call
    averageGrade: stats?.averageGrade || 0,
    weeklyHours: 24, // This would need a separate API call
    upcomingDeadlines: 3 // This would need a separate API call
  };

  // Sample schedule events for teacher
  const scheduleEvents = [
    {
      id: "1",
      title: "Mathematics - Class 5A",
      subject: "Mathematics",
      startTime: "8:00 AM",
      endTime: "8:45 AM",
      day: "Monday",
      color: "bg-blue-50 border-blue-200 text-blue-700",
      room: "Room 5A",
      class: "5A",
      studentCount: 28
    },
    {
      id: "2",
      title: "Physics - Class 3B",
      subject: "Physics",
      startTime: "9:00 AM",
      endTime: "9:45 AM",
      day: "Tuesday",
      color: "bg-purple-50 border-purple-200 text-purple-700",
      room: "Room 3B",
      class: "3B",
      studentCount: 25
    }
  ];

  // Recent teaching activities
  const recentActivities = [
    {
      id: "1",
      title: "Mathematics Quiz Graded",
      message: "Completed grading for 28 students in Class 5A",
      date: new Date(Date.now() - 2 * 60 * 60 * 1000),
      type: "grading" as const,
      priority: "completed" as const
    },
    {
      id: "2",
      title: "Assignment Created",
      message: "New algebra assignment for Class 3B due Friday",
      date: new Date(Date.now() - 4 * 60 * 60 * 1000),
      type: "assignment" as const,
      priority: "medium" as const
    },
    {
      id: "3",
      title: "Parent Meeting Scheduled",
      message: "Meeting with Sarah's parents tomorrow at 2 PM",
      date: new Date(Date.now() - 6 * 60 * 60 * 1000),
      type: "meeting" as const,
      priority: "high" as const
    }
  ];

  const dashboardUser = {
    id: user?.id?.toString() || "1",
    name: user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : "Teacher Name",
    email: user?.email || "teacher@school.edu",
    role: "Teacher",
    avatar: undefined, // user?.avatar when available
    lastLogin: new Date(Date.now() - 3 * 60 * 60 * 1000)
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'high': return 'bg-red-100 text-red-800 border-red-200';
      case 'medium': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'completed': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'grading': return '📝';
      case 'assignment': return '📋';
      case 'meeting': return '👥';
      default: return '📄';
    }
  };

  return (
    <>
      <Helmet>
        <title>Teacher Dashboard - School Management System</title>
        <meta name="description" content="Teacher dashboard for managing classes and student progress" />
      </Helmet>

      <div className="min-h-screen bg-gray-50">
        {/* Header */}
        <div className="bg-white border-b border-gray-200 shadow-sm">
          <div className="container mx-auto px-6 py-4">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-2xl font-bold text-gray-900">Teacher Dashboard</h1>
                <p className="text-gray-600 mt-1">
                  Good {new Date().getHours() < 12 ? 'morning' : new Date().getHours() < 18 ? 'afternoon' : 'evening'}, {dashboardUser.name.split(' ')[0]}!
                </p>
              </div>
              <div className="text-right">
                <div className="text-sm text-gray-500">
                  {new Date().toLocaleDateString('en-US', { 
                    weekday: 'long', 
                    year: 'numeric', 
                    month: 'long', 
                    day: 'numeric' 
                  })}
                </div>
                <div className="text-lg font-semibold text-gray-900">
                  {new Date().toLocaleTimeString('en-US', { 
                    hour: '2-digit', 
                    minute: '2-digit' 
                  })}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Main Content */}
        <div className="container mx-auto px-6 py-8">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left Column - Teaching Schedule */}
            <div className="lg:col-span-2">
              <TeacherSchedule events={scheduleEvents} />
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
                    Teaching Overview
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">My Classes</span>
                    {statsLoading ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Badge variant="secondary">{teacherData.myClasses}</Badge>
                    )}
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Total Students</span>
                    {statsLoading ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Badge variant="secondary">{teacherData.totalStudents}</Badge>
                    )}
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Pending Grades</span>
                    <Badge variant="destructive">{teacherData.pendingGrades}</Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Class Average</span>
                    {statsLoading ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Badge variant="secondary">
                        {teacherData.averageGrade > 0 ? `${teacherData.averageGrade.toFixed(1)}%` : 'N/A'}
                      </Badge>
                    )}
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
                    onClick={() => navigate("/teacher/grades")}
                  >
                    <Award className="mr-2 h-4 w-4" />
                    Grade Students
                  </Button>
                  <Button 
                    variant="outline" 
                    className="w-full justify-start"
                    onClick={() => navigate("/teacher/attendance")}
                  >
                    <CheckSquare className="mr-2 h-4 w-4" />
                    Mark Attendance
                  </Button>
                  <Button 
                    variant="outline" 
                    className="w-full justify-start"
                    onClick={() => navigate("/teacher/schedule")}
                  >
                    <Calendar className="mr-2 h-4 w-4" />
                    My Schedule
                  </Button>
                  <Button 
                    variant="outline" 
                    className="w-full justify-start"
                    onClick={() => navigate("/teacher/classes")}
                  >
                    <Users className="mr-2 h-4 w-4" />
                    My Classes
                  </Button>
                </CardContent>
              </Card>

              {/* Recent Activities */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Bell className="h-5 w-5" />
                    Recent Activities
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-3">
                  {recentActivities.map((activity) => (
                    <div 
                      key={activity.id}
                      className="p-3 rounded-lg border cursor-pointer hover:bg-gray-50 transition-colors"
                    >
                      <div className="flex items-start justify-between mb-2">
                        <div className="flex items-center gap-2">
                          <span className="text-lg">{getActivityIcon(activity.type)}</span>
                          <h4 className="font-medium text-sm">{activity.title}</h4>
                        </div>
                        <Badge 
                          variant="outline" 
                          className={`text-xs ${getPriorityColor(activity.priority)}`}
                        >
                          {activity.priority}
                        </Badge>
                      </div>
                      <p className="text-xs text-gray-600 line-clamp-2 ml-7">
                        {activity.message}
                      </p>
                      <p className="text-xs text-gray-400 mt-1 ml-7">
                        {activity.date.toLocaleDateString()}
                      </p>
                    </div>
                  ))}
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    className="w-full"
                    onClick={() => navigate("/teacher/activities")}
                  >
                    View All Activities
                  </Button>
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default TeacherDashboard;