import React from 'react';
import { Helmet } from 'react-helmet-async';
import { SimpleStudentLayout } from '@/components/Dashboard/SimpleStudentLayout';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { TrendingUp, Award, Calendar, FileText } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useNavigate } from 'react-router-dom';

const StudentResults = () => {
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
      title: "New Result Posted",
      message: "Your Mathematics quiz result has been posted",
      type: "success" as const,
      isRead: false,
      timestamp: new Date()
    }
  ];

  const recentResults = [
    {
      id: '1',
      subject: 'Geography',
      title: 'Geography Assignment',
      score: 90,
      maxScore: 100,
      grade: 'A',
      teacher: 'James Green',
      class: '5A',
      date: '14/09/2024',
      type: 'assignment'
    },
    {
      id: '2',
      subject: 'English',
      title: 'English Midterm Exam',
      score: 85,
      maxScore: 100,
      grade: 'B+',
      teacher: 'Noah',
      class: '3A',
      date: '12/09/2024',
      type: 'exam'
    },
    {
      id: '3',
      subject: 'Biology',
      title: 'Biology Quiz',
      score: 92,
      maxScore: 100,
      grade: 'A',
      teacher: 'Daniel',
      class: '2A',
      date: '10/09/2024',
      type: 'quiz'
    },
    {
      id: '4',
      subject: 'Computer Science',
      title: 'Programming Test',
      score: 88,
      maxScore: 100,
      grade: 'B+',
      teacher: 'Chloe',
      class: '3A',
      date: '08/09/2024',
      type: 'test'
    }
  ];

  const getGradeColor = (grade: string) => {
    if (grade.startsWith('A')) return 'bg-green-100 text-green-800 border-green-200';
    if (grade.startsWith('B')) return 'bg-blue-100 text-blue-800 border-blue-200';
    if (grade.startsWith('C')) return 'bg-yellow-100 text-yellow-800 border-yellow-200';
    if (grade.startsWith('D')) return 'bg-orange-100 text-orange-800 border-orange-200';
    return 'bg-red-100 text-red-800 border-red-200';
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'exam': return '📝';
      case 'quiz': return '❓';
      case 'assignment': return '📋';
      case 'test': return '🧪';
      default: return '📄';
    }
  };

  const calculateAverage = () => {
    const total = recentResults.reduce((sum, result) => sum + result.score, 0);
    return Math.round(total / recentResults.length);
  };

  const handleNotificationClick = (notification: any) => {
    console.log("Notification clicked:", notification);
  };

  return (
    <>
      <Helmet>
        <title>Results - Student Dashboard</title>
        <meta name="description" content="View your exam results and grades" />
      </Helmet>
      
      <SimpleStudentLayout
        title="All Results"
        subtitle="View your academic performance and grades"
        user={dashboardUser}
        notifications={notifications}
        onNotificationClick={handleNotificationClick}
        onProfileClick={() => navigate("/student/profile")}
        onSettingsClick={() => navigate("/student/settings")}
        onLogout={() => navigate("/login")}
      >
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Left Column - Results List */}
          <div className="lg:col-span-3">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FileText className="h-5 w-5" />
                  Recent Results
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-gray-200">
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Title</th>
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Student</th>
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Score</th>
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Teacher</th>
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Class</th>
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Date</th>
                      </tr>
                    </thead>
                    <tbody>
                      {recentResults.map((result) => (
                        <tr key={result.id} className="border-b border-gray-100 hover:bg-gray-50">
                          <td className="p-3">
                            <div className="flex items-center gap-2">
                              <span className="text-lg">{getTypeIcon(result.type)}</span>
                              <div>
                                <div className="font-medium text-sm">{result.title}</div>
                                <div className="text-xs text-gray-500">{result.subject}</div>
                              </div>
                            </div>
                          </td>
                          <td className="p-3">
                            <div className="text-sm">Stella</div>
                          </td>
                          <td className="p-3">
                            <div className="flex items-center gap-2">
                              <Badge 
                                variant="outline" 
                                className={`${getGradeColor(result.grade)} font-semibold`}
                              >
                                {result.score}
                              </Badge>
                              <Badge 
                                variant="outline" 
                                className={getGradeColor(result.grade)}
                              >
                                {result.grade}
                              </Badge>
                            </div>
                          </td>
                          <td className="p-3">
                            <div className="text-sm">{result.teacher}</div>
                          </td>
                          <td className="p-3">
                            <div className="text-sm">{result.class}</div>
                          </td>
                          <td className="p-3">
                            <div className="text-sm text-gray-600">{result.date}</div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Right Column - Statistics */}
          <div className="space-y-6">
            {/* Performance Overview */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <TrendingUp className="h-5 w-5" />
                  Performance
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="text-center">
                  <div className="text-2xl font-bold text-blue-600">{calculateAverage()}%</div>
                  <div className="text-sm text-gray-600">Average Score</div>
                </div>
                
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Total Results</span>
                    <Badge variant="secondary">{recentResults.length}</Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">A Grades</span>
                    <Badge variant="secondary" className="bg-green-100 text-green-800">
                      {recentResults.filter(r => r.grade.startsWith('A')).length}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">B Grades</span>
                    <Badge variant="secondary" className="bg-blue-100 text-blue-800">
                      {recentResults.filter(r => r.grade.startsWith('B')).length}
                    </Badge>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Recent Achievements */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Award className="h-5 w-5" />
                  Achievements
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="p-3 bg-yellow-50 rounded-lg border border-yellow-200">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-lg">🏆</span>
                    <span className="font-medium text-sm">Top Performer</span>
                  </div>
                  <div className="text-xs text-gray-600">Scored 90+ in Geography</div>
                </div>
                
                <div className="p-3 bg-blue-50 rounded-lg border border-blue-200">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-lg">📚</span>
                    <span className="font-medium text-sm">Consistent Student</span>
                  </div>
                  <div className="text-xs text-gray-600">4 consecutive good grades</div>
                </div>
                
                <div className="p-3 bg-green-50 rounded-lg border border-green-200">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-lg">⭐</span>
                    <span className="font-medium text-sm">Improvement</span>
                  </div>
                  <div className="text-xs text-gray-600">Score improved by 15%</div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </SimpleStudentLayout>
    </>
  );
};

export default StudentResults;
