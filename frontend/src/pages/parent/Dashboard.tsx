import { useNavigate } from "react-router-dom";
import { 
  Users, 
  Calendar, 
  MessageSquare, 
  CheckSquare, 
  Phone,
  Award,
  AlertTriangle,
  Star
} from "lucide-react";

// Dashboard Components
import { StatCard, StatGrid } from "@/components/Dashboard/StatCard";
import { EnhancedQuickActions, parentQuickActions } from "@/components/Dashboard/EnhancedQuickActions";
import { EnhancedActivityFeed, type ActivityItem } from "@/components/Dashboard/EnhancedActivityFeed";

// Hooks and utilities
import { useAuth } from "@/hooks/useAuth";

const ParentDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  // Mock parent data (replace with actual API calls)
  const parentData = {
    totalChildren: 2,
    upcomingEvents: 4,
    unreadMessages: 3,
    avgGradeAllChildren: 87.2,
    totalAttendanceRate: 94.5,
    pendingPermissions: 1,
    teacherMeetings: 2,
    schoolNotifications: 5
  };

  // Mock children data
  const childrenData = [
    {
      id: 1,
      name: "Emma Johnson",
      grade: "Grade 10",
      averageGrade: 89.5,
      attendanceRate: 96,
      recentGrades: [
        { subject: "Mathematics", grade: 92, date: "2024-01-15" },
        { subject: "Physics", grade: 87, date: "2024-01-14" },
        { subject: "English", grade: 91, date: "2024-01-13" }
      ]
    },
    {
      id: 2,
      name: "Michael Johnson",
      grade: "Grade 7",
      averageGrade: 84.8,
      attendanceRate: 93,
      recentGrades: [
        { subject: "Science", grade: 88, date: "2024-01-15" },
        { subject: "History", grade: 82, date: "2024-01-14" },
        { subject: "Art", grade: 94, date: "2024-01-13" }
      ]
    }
  ];

  // Sample activity data for parents
  const recentActivities: ActivityItem[] = [
    {
      id: "1",
      type: "grade",
      title: "Emma's Mathematics Grade",
      description: "Emma received 92% on her Algebra test - Great improvement!",
      timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000),
      metadata: {
        grade: 92,
        subject: "Mathematics",
        className: "Grade 10-A",
        status: "completed"
      },
      actionUrl: "/parent/grades"
    },
    {
      id: "2",
      type: "attendance",
      title: "Perfect Attendance Week",
      description: "Both children maintained perfect attendance this week",
      timestamp: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000),
      metadata: {
        status: "completed"
      },
      actionUrl: "/parent/attendance"
    },
    {
      id: "3",
      type: "message",
      title: "Teacher Message",
      description: "Ms. Smith sent a message about Michael's science project",
      timestamp: new Date(Date.now() - 3 * 60 * 60 * 1000),
      metadata: {
        teacherName: "Ms. Smith",
        subject: "Science",
        priority: "medium",
        status: "pending"
      },
      actionUrl: "/parent/messages"
    },
    {
      id: "4",
      type: "event",
      title: "Parent-Teacher Conference",
      description: "Scheduled meeting with Emma's homeroom teacher next week",
      timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000),
      metadata: {
        priority: "high"
      },
      actionUrl: "/parent/events"
    },
    {
      id: "5",
      type: "achievement",
      title: "Honor Roll Recognition",
      description: "Emma has been selected for the honor roll this semester",
      timestamp: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
      metadata: {
        status: "completed"
      },
      actionUrl: "/parent/achievements"
    },
    {
      id: "6",
      type: "announcement",
      title: "School Field Trip",
      description: "Science museum field trip permission slip required",
      timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000),
      metadata: {
        priority: "medium",
        status: "pending"
      },
      actionUrl: "/parent/announcements"
    }
  ];

  // Enhanced quick actions with navigation
  const enhancedParentActions = parentQuickActions.map(action => ({
    ...action,
    onClick: () => {
      switch (action.id) {
        case "children-overview":
          navigate("/parent/children");
          break;
        case "grades-reports":
          navigate("/parent/grades");
          break;
        case "attendance-record":
          navigate("/parent/attendance");
          break;
        case "teacher-communication":
          navigate("/parent/messages");
          break;
        case "school-events":
          navigate("/parent/events");
          break;
        case "notifications":
          navigate("/parent/notifications");
          break;
        default:
          console.log(`Action: ${action.id}`);
      }
    }
  }));

  const handleActivityClick = (activity: ActivityItem) => {
    if (activity.actionUrl) {
      navigate(activity.actionUrl);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold bg-gradient-to-r from-slate-900 to-blue-800 bg-clip-text text-transparent">
              Parent Dashboard
            </h1>
            <p className="text-slate-600 mt-2">Stay connected with your children's academic journey</p>
          </div>
          <div className="text-right">
            <div className="text-sm text-slate-500 mb-1">
              {new Date().toLocaleDateString('en-US', { 
                weekday: 'long', 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
              })}
            </div>
            <div className="text-lg font-semibold text-slate-700">
              {new Date().toLocaleTimeString('en-US', { 
                hour: '2-digit', 
                minute: '2-digit' 
              })}
            </div>
          </div>
        </div>
      </div>

      <div className="space-y-8">
        {/* Statistics Cards */}
        <StatGrid columns={4}>
          <StatCard
            title="My Children"
            value={parentData.totalChildren}
            subtitle="Students enrolled"
            icon={<Users className="h-5 w-5" />}
            variant="blue"
            onClick={() => navigate("/parent/children")}
          />
          
          <StatCard
            title="Unread Messages"
            value={parentData.unreadMessages}
            subtitle="From teachers"
            icon={<MessageSquare className="h-5 w-5" />}
            variant="orange"
            onClick={() => navigate("/parent/messages")}
          />
          
          <StatCard
            title="Average Grade"
            value={`${parentData.avgGradeAllChildren}%`}
            subtitle="All children combined"
            icon={<Award className="h-5 w-5" />}
            variant="green"
            trend={{ value: 2.8, isPositive: true, label: "this semester" }}
            onClick={() => navigate("/parent/grades")}
          />
          
          <StatCard
            title="Attendance Rate"
            value={`${parentData.totalAttendanceRate}%`}
            subtitle="Combined attendance"
            icon={<CheckSquare className="h-5 w-5" />}
            variant="purple"
            trend={{ value: 1.2, isPositive: true, label: "this month" }}
            onClick={() => navigate("/parent/attendance")}
          />
        </StatGrid>

        {/* Children Overview Cards */}
        <div className="space-y-4">
          <h2 className="text-2xl font-bold bg-gradient-to-r from-slate-900 to-blue-800 bg-clip-text text-transparent">
            Children Overview
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {childrenData.map((child) => (
              <div
                key={child.id}
                className="bg-white/95 backdrop-blur-sm shadow-xl border border-slate-200/60 rounded-xl p-6 hover:shadow-2xl transition-all duration-300 cursor-pointer"
                onClick={() => navigate(`/parent/child/${child.id}`)}
              >
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-full flex items-center justify-center">
                      <span className="text-white font-bold text-lg">
                        {child.name.split(' ').map(n => n[0]).join('')}
                      </span>
                    </div>
                    <div>
                      <h3 className="font-bold text-slate-900">{child.name}</h3>
                      <p className="text-slate-600 text-sm">{child.grade}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-2xl font-bold text-green-600">{child.averageGrade}%</div>
                    <div className="text-xs text-slate-500">Average Grade</div>
                  </div>
                </div>
                
                <div className="grid grid-cols-2 gap-4 mb-4">
                  <div className="text-center p-3 bg-blue-50 rounded-lg">
                    <div className="text-lg font-bold text-blue-700">{child.attendanceRate}%</div>
                    <div className="text-xs text-blue-600">Attendance</div>
                  </div>
                  <div className="text-center p-3 bg-green-50 rounded-lg">
                    <div className="text-lg font-bold text-green-700">{child.recentGrades.length}</div>
                    <div className="text-xs text-green-600">Recent Grades</div>
                  </div>
                </div>
                
                <div className="space-y-2">
                  <h4 className="font-medium text-slate-700 text-sm">Recent Grades:</h4>
                  {child.recentGrades.slice(0, 3).map((grade, index) => (
                    <div key={index} className="flex justify-between items-center text-sm">
                      <span className="text-slate-600">{grade.subject}</span>
                      <span className={`font-medium ${
                        grade.grade >= 90 ? 'text-green-600' : 
                        grade.grade >= 80 ? 'text-blue-600' : 
                        grade.grade >= 70 ? 'text-orange-600' : 'text-red-600'
                      }`}>
                        {grade.grade}%
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column - Quick Actions */}
          <div className="lg:col-span-1">
            <EnhancedQuickActions
              title="Quick Actions"
              actions={enhancedParentActions}
              columns={2}
              showShortcuts={true}
            />
          </div>

          {/* Right Column - Activity Feed */}
          <div className="lg:col-span-2">
            <EnhancedActivityFeed
              title="Family Academic Activity"
              activities={recentActivities}
              maxItems={8}
              onItemClick={handleActivityClick}
              onViewAll={() => navigate("/parent/activity")}
            />
          </div>
        </div>

        {/* Additional Stats Row */}
        <StatGrid columns={4}>
          <StatCard
            title="Upcoming Events"
            value={parentData.upcomingEvents}
            subtitle="This month"
            icon={<Calendar className="h-5 w-5" />}
            variant="blue"
            onClick={() => navigate("/parent/events")}
          />
          
          <StatCard
            title="Teacher Meetings"
            value={parentData.teacherMeetings}
            subtitle="Scheduled"
            icon={<Phone className="h-5 w-5" />}
            variant="green"
            onClick={() => navigate("/parent/meetings")}
          />
          
          <StatCard
            title="Pending Actions"
            value={parentData.pendingPermissions}
            subtitle="Permission slips"
            icon={<AlertTriangle className="h-5 w-5" />}
            variant="orange"
            onClick={() => navigate("/parent/permissions")}
          />
          
          <StatCard
            title="School Notifications"
            value={parentData.schoolNotifications}
            subtitle="This week"
            icon={<Star className="h-5 w-5" />}
            variant="purple"
            onClick={() => navigate("/parent/notifications")}
          />
        </StatGrid>
      </div>
    </div>
  );
};

export default ParentDashboard;