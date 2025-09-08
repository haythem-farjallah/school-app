import React from 'react';
import { Helmet } from 'react-helmet-async';
import { SimpleStudentLayout } from '@/components/Dashboard/SimpleStudentLayout';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Calendar, Clock, MapPin, Users, Star } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useNavigate } from 'react-router-dom';

const StudentEvents = () => {
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
      title: "Event Reminder",
      message: "Book Fair starts tomorrow at 8:00 AM",
      type: "info" as const,
      isRead: false,
      timestamp: new Date()
    }
  ];

  const events = [
    {
      id: '1',
      title: 'Book Fair',
      description: 'Browse and purchase books at our annual school Book Fair',
      class: '2A',
      date: '16/09/2024',
      startTime: '08:00',
      endTime: '10:00',
      location: 'School Library',
      type: 'academic',
      status: 'upcoming'
    },
    {
      id: '2',
      title: 'Sports Day',
      description: 'A fun-filled day of athletic events and team competitions',
      class: '3A',
      date: '16/09/2024',
      startTime: '10:00',
      endTime: '12:00',
      location: 'School Playground',
      type: 'sports',
      status: 'upcoming'
    },
    {
      id: '3',
      title: 'Spelling Bee',
      description: 'Annual spelling competition for all grade levels',
      class: 'General',
      date: '18/09/2024',
      startTime: '11:00',
      endTime: '13:00',
      location: 'Main Auditorium',
      type: 'competition',
      status: 'upcoming'
    },
    {
      id: '4',
      title: 'Spring Picnic',
      description: 'Family picnic in the school garden with games and activities',
      class: 'General',
      date: '20/09/2024',
      startTime: '08:00',
      endTime: '09:00',
      location: 'School Garden',
      type: 'social',
      status: 'upcoming'
    },
    {
      id: '5',
      title: 'Robotics Competition',
      description: 'Showcase your robotics projects and compete with other students',
      class: 'General',
      date: '20/09/2024',
      startTime: '11:00',
      endTime: '13:00',
      location: 'Computer Lab',
      type: 'competition',
      status: 'upcoming'
    },
    {
      id: '6',
      title: 'History Fair',
      description: 'Present historical projects and learn about different eras',
      class: 'General',
      date: '20/09/2024',
      startTime: '14:00',
      endTime: '15:00',
      location: 'History Classroom',
      type: 'academic',
      status: 'upcoming'
    }
  ];

  const getEventTypeColor = (type: string) => {
    switch (type) {
      case 'academic': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'sports': return 'bg-green-100 text-green-800 border-green-200';
      case 'competition': return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'social': return 'bg-orange-100 text-orange-800 border-orange-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getEventIcon = (type: string) => {
    switch (type) {
      case 'academic': return '📚';
      case 'sports': return '⚽';
      case 'competition': return '🏆';
      case 'social': return '🎉';
      default: return '📅';
    }
  };

  const handleNotificationClick = (notification: any) => {
    console.log("Notification clicked:", notification);
  };

  return (
    <>
      <Helmet>
        <title>Events - Student Dashboard</title>
        <meta name="description" content="View upcoming school events and activities" />
      </Helmet>
      
      <SimpleStudentLayout
        title="All Events"
        subtitle="Stay updated with school events and activities"
        user={dashboardUser}
        notifications={notifications}
        onNotificationClick={handleNotificationClick}
        onProfileClick={() => navigate("/student/profile")}
        onSettingsClick={() => navigate("/student/settings")}
        onLogout={() => navigate("/login")}
      >
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Left Column - Events List */}
          <div className="lg:col-span-3">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Calendar className="h-5 w-5" />
                  Upcoming Events
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-gray-200">
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Title</th>
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Class</th>
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Date</th>
                        <th className="text-left p-3 text-sm font-medium text-gray-600">Start Time</th>
                        <th className="text-left p-3 text-sm font-medium text-gray-600">End Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      {events.map((event) => (
                        <tr key={event.id} className="border-b border-gray-100 hover:bg-gray-50">
                          <td className="p-3">
                            <div className="flex items-center gap-3">
                              <span className="text-lg">{getEventIcon(event.type)}</span>
                              <div>
                                <div className="font-medium text-sm">{event.title}</div>
                                <div className="text-xs text-gray-500 mt-1">{event.description}</div>
                                <div className="flex items-center gap-2 mt-1">
                                  <Badge 
                                    variant="outline" 
                                    className={`text-xs ${getEventTypeColor(event.type)}`}
                                  >
                                    {event.type}
                                  </Badge>
                                  <div className="flex items-center gap-1 text-xs text-gray-500">
                                    <MapPin className="h-3 w-3" />
                                    <span>{event.location}</span>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </td>
                          <td className="p-3">
                            <div className="text-sm">{event.class}</div>
                          </td>
                          <td className="p-3">
                            <div className="text-sm">{event.date}</div>
                          </td>
                          <td className="p-3">
                            <div className="text-sm">{event.startTime}</div>
                          </td>
                          <td className="p-3">
                            <div className="text-sm">{event.endTime}</div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Right Column - Event Statistics & Quick Info */}
          <div className="space-y-6">
            {/* Event Statistics */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Star className="h-5 w-5" />
                  Event Summary
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="text-center">
                  <div className="text-2xl font-bold text-blue-600">{events.length}</div>
                  <div className="text-sm text-gray-600">Total Events</div>
                </div>
                
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Academic</span>
                    <Badge variant="secondary" className="bg-blue-100 text-blue-800">
                      {events.filter(e => e.type === 'academic').length}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Sports</span>
                    <Badge variant="secondary" className="bg-green-100 text-green-800">
                      {events.filter(e => e.type === 'sports').length}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Competitions</span>
                    <Badge variant="secondary" className="bg-purple-100 text-purple-800">
                      {events.filter(e => e.type === 'competition').length}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Social</span>
                    <Badge variant="secondary" className="bg-orange-100 text-orange-800">
                      {events.filter(e => e.type === 'social').length}
                    </Badge>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Today's Events */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Clock className="h-5 w-5" />
                  Today's Events
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                {events.filter(e => e.date === '16/09/2024').map((event) => (
                  <div key={event.id} className="p-3 bg-blue-50 rounded-lg border border-blue-200">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-lg">{getEventIcon(event.type)}</span>
                      <span className="font-medium text-sm">{event.title}</span>
                    </div>
                    <div className="text-xs text-gray-600 mb-2">{event.description}</div>
                    <div className="flex items-center gap-3 text-xs text-gray-500">
                      <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        <span>{event.startTime} - {event.endTime}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <MapPin className="h-3 w-3" />
                        <span>{event.location}</span>
                      </div>
                    </div>
                  </div>
                ))}
                
                {events.filter(e => e.date === '16/09/2024').length === 0 && (
                  <div className="text-sm text-gray-500 text-center py-4">
                    No events scheduled for today
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Event Categories */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <Users className="h-5 w-5" />
                  Categories
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <div className="flex items-center gap-2 p-2 rounded">
                  <span>📚</span>
                  <span className="text-sm">Academic Events</span>
                </div>
                <div className="flex items-center gap-2 p-2 rounded">
                  <span>⚽</span>
                  <span className="text-sm">Sports Activities</span>
                </div>
                <div className="flex items-center gap-2 p-2 rounded">
                  <span>🏆</span>
                  <span className="text-sm">Competitions</span>
                </div>
                <div className="flex items-center gap-2 p-2 rounded">
                  <span>🎉</span>
                  <span className="text-sm">Social Events</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </SimpleStudentLayout>
    </>
  );
};

export default StudentEvents;
