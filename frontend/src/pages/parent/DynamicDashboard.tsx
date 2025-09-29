import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { 
  Users, 
  Calendar, 
  MessageSquare, 
  CheckSquare, 
  Phone,
  Award,
  AlertTriangle,
  Star,
  Clock,
  TrendingUp,
  BookOpen,
  UserCheck
} from "lucide-react";

// Dashboard Components
import { StatCard, StatGrid } from "@/components/Dashboard/StatCard";
import { EnhancedQuickActions, parentQuickActions } from "@/components/Dashboard/EnhancedQuickActions";
import { EnhancedActivityFeed, type ActivityItem } from "@/components/Dashboard/EnhancedActivityFeed";

// Hooks and utilities
import { useAuth } from "@/hooks/useAuth";
import { http } from "@/lib/http";

// New Components
import { ParentChildrenTimetable } from "@/components/parent/ParentChildrenTimetable";
import { ParentChildrenAttendance } from "@/components/parent/ParentChildrenAttendance";
import { ParentChildrenStatistics } from "@/components/parent/ParentChildrenStatistics";

const DynamicParentDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [selectedChild, setSelectedChild] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'timetables' | 'attendance' | 'statistics'>('overview');

  // Fetch parent dashboard data
  const { data: dashboardData, isLoading: isDashboardLoading } = useQuery({
    queryKey: ['parent-dashboard', user?.id],
    queryFn: async () => {
      const response = await http.get(`/v1/dashboard/parent/${user?.id}`);
      return response.data.data;
    },
    enabled: !!user?.id
  });

  // Fetch children statistics
  const { data: childrenStats, isLoading: isStatsLoading } = useQuery({
    queryKey: ['parent-children-statistics', user?.id],
    queryFn: async () => {
      const response = await http.get(`/v1/dashboard/parent/children-statistics/${user?.id}`);
      return response.data.data;
    },
    enabled: !!user?.id
  });

  // Fetch children timetables
  const { data: childrenTimetables, isLoading: isTimetablesLoading } = useQuery({
    queryKey: ['parent-children-timetables', user?.id],
    queryFn: async () => {
      const response = await http.get(`/v1/dashboard/parent/children-timetables/${user?.id}`);
      return response.data.data;
    },
    enabled: !!user?.id
  });

  // Fetch children attendance
  const { data: childrenAttendance, isLoading: isAttendanceLoading } = useQuery({
    queryKey: ['parent-children-attendance', user?.id],
    queryFn: async () => {
      const response = await http.get(`/v1/dashboard/parent/children-attendance/${user?.id}`);
      return response.data.data;
    },
    enabled: !!user?.id
  });

  if (isDashboardLoading || isStatsLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const children = childrenStats?.children || [];
  const overallStats = childrenStats?.overallStatistics || {};

  // Create dynamic stat cards based on real data
  const statCards = [
    {
      title: "Total Children",
      value: overallStats.totalChildren || 0,
      icon: Users,
      color: "blue",
      trend: { value: 0, isPositive: true }
    },
    {
      title: "Average Attendance",
      value: `${overallStats.averageAttendanceRate || 0}%`,
      icon: UserCheck,
      color: overallStats.averageAttendanceRate > 90 ? "green" : 
             overallStats.averageAttendanceRate > 80 ? "yellow" : "red",
      trend: { value: 2.5, isPositive: true }
    },
    {
      title: "Total Absences",
      value: overallStats.totalAbsences || 0,
      icon: AlertTriangle,
      color: overallStats.totalAbsences > 10 ? "red" : 
             overallStats.totalAbsences > 5 ? "yellow" : "green",
      trend: { value: -1.2, isPositive: false }
    },
    {
      title: "Children with Good Attendance",
      value: overallStats.childrenWithGoodAttendance || 0,
      icon: Award,
      color: "green",
      trend: { value: 1, isPositive: true }
    }
  ];

  // Generate recent activities from real data
  const recentActivities: ActivityItem[] = children.flatMap((child: any, index: number) => [
    {
      id: `attendance-${child.studentId}`,
      type: "attendance",
      title: `${child.studentName}'s Attendance`,
      description: `Current attendance rate: ${child.attendanceRate}% ${
        child.attendanceRate > 90 ? "- Excellent!" : 
        child.attendanceRate > 80 ? "- Good" : "- Needs attention"
      }`,
      timestamp: new Date(Date.now() - index * 60 * 60 * 1000),
      metadata: {
        studentId: child.studentId,
        attendanceRate: child.attendanceRate,
        status: child.attendanceRate > 90 ? "excellent" : 
                child.attendanceRate > 80 ? "good" : "attention"
      },
      actionUrl: `/parent/attendance/${child.studentId}`
    },
    ...(child.alerts?.length > 0 ? [{
      id: `alert-${child.studentId}`,
      type: "alert",
      title: `Alert for ${child.studentName}`,
      description: child.alerts[0],
      timestamp: new Date(Date.now() - (index + 1) * 30 * 60 * 1000),
      metadata: {
        studentId: child.studentId,
        priority: "high"
      },
      actionUrl: `/parent/student/${child.studentId}`
    }] : [])
  ]).slice(0, 10);

  const handleActivityClick = (activity: ActivityItem) => {
    if (activity.actionUrl) {
      navigate(activity.actionUrl);
    }
  };

  return (
    <div className="space-y-6 p-6 bg-gray-50 min-h-screen">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">
              Welcome back, {user?.firstName}!
            </h1>
            <p className="text-gray-600 mt-2">
              Here's an overview of your {overallStats.totalChildren || 0} child{overallStats.totalChildren !== 1 ? 'ren' : ''}'s progress
            </p>
          </div>
          <div className="text-right">
            <p className="text-sm text-gray-500">Last updated</p>
            <p className="text-lg font-semibold text-gray-900">
              {new Date().toLocaleDateString()}
            </p>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="bg-white rounded-lg shadow-sm p-4">
        <div className="flex space-x-1 bg-gray-100 p-1 rounded-lg">
          {[
            { id: 'overview', label: 'Overview', icon: Users },
            { id: 'timetables', label: 'Timetables', icon: Calendar },
            { id: 'attendance', label: 'Attendance', icon: CheckSquare },
            { id: 'statistics', label: 'Statistics', icon: TrendingUp }
          ].map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex-1 flex items-center justify-center space-x-2 px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                  activeTab === tab.id
                    ? 'bg-white text-blue-600 shadow-sm'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
              >
                <Icon className="h-4 w-4" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <>
          {/* Statistics Overview */}
          <StatGrid>
            {statCards.map((stat, index) => (
              <StatCard key={index} {...stat} />
            ))}
          </StatGrid>

          {/* Children Overview Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {children.map((child: any) => (
              <div key={child.studentId} className="bg-white rounded-lg shadow-sm p-6 hover:shadow-md transition-shadow">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900">{child.studentName}</h3>
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    child.attendanceRate > 90 ? 'bg-green-100 text-green-800' :
                    child.attendanceRate > 80 ? 'bg-yellow-100 text-yellow-800' :
                    'bg-red-100 text-red-800'
                  }`}>
                    {child.academicStanding}
                  </span>
                </div>
                
                <div className="space-y-3">
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-600">Class:</span>
                    <span className="font-medium">{child.currentClass}</span>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-600">Attendance Rate:</span>
                    <span className={`font-medium ${
                      child.attendanceRate > 90 ? 'text-green-600' :
                      child.attendanceRate > 80 ? 'text-yellow-600' :
                      'text-red-600'
                    }`}>
                      {child.attendanceRate}%
                    </span>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-600">Total Absences:</span>
                    <span className={`font-medium ${
                      child.totalAbsences > 5 ? 'text-red-600' :
                      child.totalAbsences > 2 ? 'text-yellow-600' :
                      'text-green-600'
                    }`}>
                      {child.totalAbsences}
                    </span>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <span className="text-sm text-gray-600">Average Grade:</span>
                    <span className="font-medium text-blue-600">
                      {Math.round(child.averageGrade)}%
                    </span>
                  </div>
                </div>

                {child.alerts?.length > 0 && (
                  <div className="mt-4 p-3 bg-red-50 rounded-lg">
                    <div className="flex items-center">
                      <AlertTriangle className="h-4 w-4 text-red-500 mr-2" />
                      <span className="text-sm text-red-700">
                        {child.alerts.length} alert{child.alerts.length !== 1 ? 's' : ''}
                      </span>
                    </div>
                  </div>
                )}

                <div className="mt-4 flex space-x-2">
                  <button
                    onClick={() => navigate(`/parent/student/${child.studentId}`)}
                    className="flex-1 bg-blue-600 text-white px-3 py-2 rounded-md text-sm font-medium hover:bg-blue-700 transition-colors"
                  >
                    View Details
                  </button>
                  <button
                    onClick={() => setSelectedChild(child.studentId)}
                    className="flex-1 bg-gray-100 text-gray-700 px-3 py-2 rounded-md text-sm font-medium hover:bg-gray-200 transition-colors"
                  >
                    Quick View
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Quick Actions and Recent Activity */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-6">Quick Actions</h2>
              <EnhancedQuickActions actions={parentQuickActions} />
            </div>

            <div className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-6">Recent Activity</h2>
              <EnhancedActivityFeed activities={recentActivities} />
            </div>
          </div>
        </>
      )}

      {activeTab === 'timetables' && (
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-gray-900 flex items-center">
              <Calendar className="h-5 w-5 mr-2" />
              Children's Timetables
            </h2>
            <button
              onClick={() => navigate('/parent/timetables')}
              className="text-blue-600 hover:text-blue-700 font-medium"
            >
              View All →
            </button>
          </div>
          
          {!isTimetablesLoading && childrenTimetables && (
            <ParentChildrenTimetable 
              data={childrenTimetables} 
              selectedChild={selectedChild}
              onChildSelect={setSelectedChild}
            />
          )}
        </div>
      )}

      {activeTab === 'attendance' && (
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-gray-900 flex items-center">
              <UserCheck className="h-5 w-5 mr-2" />
              Attendance Overview
            </h2>
            <button
              onClick={() => navigate('/parent/attendance')}
              className="text-blue-600 hover:text-blue-700 font-medium"
            >
              View Details →
            </button>
          </div>
          
          {!isAttendanceLoading && childrenAttendance && (
            <ParentChildrenAttendance 
              data={childrenAttendance}
              selectedChild={selectedChild}
              onChildSelect={setSelectedChild}
            />
          )}
        </div>
      )}

      {activeTab === 'statistics' && (
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-gray-900 flex items-center">
              <TrendingUp className="h-5 w-5 mr-2" />
              Performance Statistics
            </h2>
            <button
              onClick={() => navigate('/parent/statistics')}
              className="text-blue-600 hover:text-blue-700 font-medium"
            >
              View All →
            </button>
          </div>
          
          {childrenStats && (
            <ParentChildrenStatistics data={childrenStats} />
          )}
        </div>
      )}
    </div>
  );
};

export default DynamicParentDashboard;
