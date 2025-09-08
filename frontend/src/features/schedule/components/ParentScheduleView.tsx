import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../../components/ui/tabs';
import { 
  Clock, 
  Users, 
  BookOpen, 
  MapPin, 
  Calendar,
  Download,
  RefreshCw,
  GraduationCap,
  User,
  AlertCircle,
  Baby,
  Phone,
  Mail
} from 'lucide-react';
import { useParentSchedule } from '../hooks/use-schedule';
import { usePeriods } from '../../timetable/hooks';
import { ScheduleGrid } from './ScheduleGrid';
import { useAuth } from '../../../hooks/useAuth';
import { TimetableSlot } from '../../../types/timetable';

interface ChildScheduleProps {
  childName: string;
  slots: (TimetableSlot & { childName: string; className: string })[];
  periods: any[];
}

function ChildScheduleCard({ childName, slots, periods }: ChildScheduleProps) {
  const childSlots = slots.filter(slot => slot.childName === childName);
  const totalClasses = new Set(childSlots.map(slot => slot.className)).size;
  const totalCourses = new Set(childSlots.map(slot => slot.forCourse?.name).filter(Boolean)).size;
  const weeklyHours = childSlots.length;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Baby className="w-5 h-5 text-blue-600" />
          {childName}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-3 gap-4 mb-4">
          <div className="text-center">
            <p className="text-2xl font-bold text-blue-600">{totalClasses}</p>
            <p className="text-xs text-gray-600">Classes</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-bold text-green-600">{totalCourses}</p>
            <p className="text-xs text-gray-600">Courses</p>
          </div>
          <div className="text-center">
            <p className="text-2xl font-bold text-purple-600">{weeklyHours}</p>
            <p className="text-xs text-gray-600">Hours/Week</p>
          </div>
        </div>

        <div className="space-y-2">
          <p className="text-sm font-medium text-gray-700">This Week's Classes:</p>
          <div className="space-y-1 max-h-32 overflow-y-auto">
            {[...new Set(childSlots.map(slot => slot.className))].map(className => (
              <div key={className} className="flex items-center gap-2 text-sm">
                <Users className="w-3 h-3 text-gray-400" />
                <span>{className}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="pt-3 border-t mt-4">
          <div className="flex gap-2">
            <Button size="sm" variant="outline" className="flex-1">
              <BookOpen className="w-4 h-4 mr-1" />
              Grades
            </Button>
            <Button size="sm" variant="outline" className="flex-1">
              <Calendar className="w-4 h-4 mr-1" />
              Attendance
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export function ParentScheduleView() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('overview');
  const [selectedChild, setSelectedChild] = useState<string>('all');
  
  const { data: scheduleData, isLoading, error, refetch } = useParentSchedule(user?.id);
  const { data: periods, isLoading: periodsLoading } = usePeriods();

  if (isLoading || periodsLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Children's Schedules</h1>
            <p className="text-gray-600">View your children's class schedules and academic information</p>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1, 2, 3].map(i => (
            <Card key={i} className="animate-pulse">
              <CardContent className="p-6">
                <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                <div className="h-8 bg-gray-200 rounded w-1/2"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Children's Schedules</h1>
            <p className="text-gray-600">View your children's class schedules and academic information</p>
          </div>
        </div>
        <Card>
          <CardContent className="p-6 text-center">
            <div className="text-red-500 mb-4">
              <AlertCircle className="w-12 h-12 mx-auto mb-2" />
              <p>Error loading children's schedules</p>
              <p className="text-sm text-gray-600 mt-2">Please try refreshing the page</p>
            </div>
            <Button onClick={() => refetch()} variant="outline">
              <RefreshCw className="w-4 h-4 mr-2" />
              Retry
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const { children = [], childrenSchedules = [], allSlots = [] } = scheduleData || {};
  const periodsData = periods || [];

  // Calculate overall statistics
  const totalChildren = children.length;
  const totalClasses = new Set(allSlots.map(slot => slot.className)).size;
  const totalCourses = new Set(allSlots.map(slot => slot.forCourse?.name).filter(Boolean)).size;
  const totalWeeklyHours = allSlots.length;

  // Get unique child names for filter
  const childNames = [...new Set(allSlots.map(slot => slot.childName))];

  // Filter slots by selected child
  const filteredSlots = selectedChild === 'all' 
    ? allSlots 
    : allSlots.filter(slot => slot.childName === selectedChild);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Children's Schedules</h1>
          <p className="text-gray-600">
            Welcome, {user?.firstName}! Here's an overview of your children's academic schedules.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => refetch()}>
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
          <Button variant="outline">
            <Download className="w-4 h-4 mr-2" />
            Export PDF
          </Button>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Children</p>
                <p className="text-2xl font-bold text-blue-600">{totalChildren}</p>
              </div>
              <div className="p-3 bg-blue-100 rounded-full">
                <Baby className="w-6 h-6 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Classes</p>
                <p className="text-2xl font-bold text-green-600">{totalClasses}</p>
              </div>
              <div className="p-3 bg-green-100 rounded-full">
                <Users className="w-6 h-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Courses</p>
                <p className="text-2xl font-bold text-purple-600">{totalCourses}</p>
              </div>
              <div className="p-3 bg-purple-100 rounded-full">
                <BookOpen className="w-6 h-6 text-purple-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Weekly Hours</p>
                <p className="text-2xl font-bold text-orange-600">{totalWeeklyHours}</p>
              </div>
              <div className="p-3 bg-orange-100 rounded-full">
                <Clock className="w-6 h-6 text-orange-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="schedule">Combined Schedule</TabsTrigger>
          <TabsTrigger value="individual">Individual Schedules</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {childNames.map(childName => (
              <ChildScheduleCard
                key={childName}
                childName={childName}
                slots={allSlots}
                periods={periodsData}
              />
            ))}
          </div>

          {children.length === 0 && (
            <Card>
              <CardContent className="p-8 text-center">
                <Baby className="w-12 h-12 mx-auto mb-4 text-gray-400" />
                <h3 className="text-lg font-semibold text-gray-900 mb-2">No Children Found</h3>
                <p className="text-gray-600">
                  No children are associated with your account. Contact the administration if this is incorrect.
                </p>
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="schedule" className="space-y-6">
          {/* Child Filter */}
          {childNames.length > 1 && (
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center gap-4">
                  <label className="text-sm font-medium text-gray-700">View schedule for:</label>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      variant={selectedChild === 'all' ? 'default' : 'outline'}
                      onClick={() => setSelectedChild('all')}
                    >
                      All Children
                    </Button>
                    {childNames.map(childName => (
                      <Button
                        key={childName}
                        size="sm"
                        variant={selectedChild === childName ? 'default' : 'outline'}
                        onClick={() => setSelectedChild(childName)}
                      >
                        {childName}
                      </Button>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          <ScheduleGrid
            slots={filteredSlots}
            periods={periodsData}
            title={selectedChild === 'all' ? "Combined Family Schedule" : `${selectedChild}'s Schedule`}
            showTeacher={true}
            showRoom={true}
            showClass={true}
            emptyMessage="No classes scheduled for the selected child(ren)."
          />
        </TabsContent>

        <TabsContent value="individual" className="space-y-6">
          {childrenSchedules.map(childSchedule => (
            <div key={childSchedule.child.id} className="space-y-4">
              <div className="flex items-center gap-3">
                <Baby className="w-6 h-6 text-blue-600" />
                <h3 className="text-xl font-semibold text-gray-900">
                  {childSchedule.child.firstName} {childSchedule.child.lastName}
                </h3>
                <Badge variant="secondary">
                  {childSchedule.slots.length} weekly hours
                </Badge>
              </div>

              <ScheduleGrid
                slots={childSchedule.slots}
                periods={periodsData}
                showTeacher={true}
                showRoom={true}
                showClass={true}
                emptyMessage={`No classes scheduled for ${childSchedule.child.firstName}.`}
              />
            </div>
          ))}

          {childrenSchedules.length === 0 && (
            <Card>
              <CardContent className="p-8 text-center">
                <Baby className="w-12 h-12 mx-auto mb-4 text-gray-400" />
                <h3 className="text-lg font-semibold text-gray-900 mb-2">No Children Schedules</h3>
                <p className="text-gray-600">
                  No schedule information is available for your children at this time.
                </p>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
