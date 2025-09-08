import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../../components/ui/tabs';
import { Input } from '../../../components/ui/input';
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
  Search,
  Settings,
  Eye,
  Edit
} from 'lucide-react';
import { useTimetables } from '../../timetable/hooks';
import { usePeriods } from '../../timetable/hooks';
import { ScheduleGrid } from './ScheduleGrid';
import { useAuth } from '../../../hooks/useAuth';
import { useClasses } from '../../classes/hooks/use-classes';
import { useTeachers } from '../../teachers/hooks/use-teachers';

export function StaffScheduleView() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('overview');
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedClass, setSelectedClass] = useState<number | null>(null);
  
  const { data: timetablesData, isLoading: timetablesLoading, error: timetablesError, refetch } = useTimetables();
  const { data: periodsData, isLoading: periodsLoading } = usePeriods();
  const { data: classesData, isLoading: classesLoading } = useClasses();
  const { data: teachersData, isLoading: teachersLoading } = useTeachers();

  const isLoading = timetablesLoading || periodsLoading || classesLoading || teachersLoading;

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Schedule Management</h1>
            <p className="text-gray-600">Manage and oversee all school schedules</p>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map(i => (
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

  if (timetablesError) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Schedule Management</h1>
            <p className="text-gray-600">Manage and oversee all school schedules</p>
          </div>
        </div>
        <Card>
          <CardContent className="p-6 text-center">
            <div className="text-red-500 mb-4">
              <AlertCircle className="w-12 h-12 mx-auto mb-2" />
              <p>Error loading schedule data</p>
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

  const timetables = timetablesData || [];
  const periods = periodsData || [];
  const classes = classesData?.data || [];
  const teachers = teachersData?.data || [];

  // Calculate statistics
  const totalTimetables = timetables.length;
  const totalSlots = timetables.reduce((sum, tt) => sum + (tt.slots?.length || 0), 0);
  const totalClasses = classes.length;
  const totalTeachers = teachers.length;

  // Filter classes based on search
  const filteredClasses = classes.filter(cls => 
    cls.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Get selected class timetable
  const selectedClassTimetable = selectedClass 
    ? timetables.find(tt => tt.classIds?.includes(selectedClass))
    : null;

  const selectedClassSlots = selectedClassTimetable?.slots || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Schedule Management</h1>
          <p className="text-gray-600">
            Welcome, {user?.firstName}! Manage and oversee all school schedules and timetables.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => refetch()}>
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
          <Button variant="outline">
            <Settings className="w-4 h-4 mr-2" />
            Settings
          </Button>
          <Button variant="outline">
            <Download className="w-4 h-4 mr-2" />
            Export All
          </Button>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Classes</p>
                <p className="text-2xl font-bold text-blue-600">{totalClasses}</p>
              </div>
              <div className="p-3 bg-blue-100 rounded-full">
                <Users className="w-6 h-6 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Active Timetables</p>
                <p className="text-2xl font-bold text-green-600">{totalTimetables}</p>
              </div>
              <div className="p-3 bg-green-100 rounded-full">
                <Calendar className="w-6 h-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Slots</p>
                <p className="text-2xl font-bold text-purple-600">{totalSlots}</p>
              </div>
              <div className="p-3 bg-purple-100 rounded-full">
                <Clock className="w-6 h-6 text-purple-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Teachers</p>
                <p className="text-2xl font-bold text-orange-600">{totalTeachers}</p>
              </div>
              <div className="p-3 bg-orange-100 rounded-full">
                <GraduationCap className="w-6 h-6 text-orange-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="classes">Class Schedules</TabsTrigger>
          <TabsTrigger value="teachers">Teacher Schedules</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Recent Timetables */}
            <Card>
              <CardHeader>
                <CardTitle>Recent Timetables</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {timetables.slice(0, 5).map(timetable => (
                    <div key={timetable.id} className="flex items-center justify-between p-3 border rounded-lg">
                      <div>
                        <p className="font-medium">{timetable.name}</p>
                        <p className="text-sm text-gray-600">
                          {timetable.academicYear} - {timetable.semester}
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge variant="secondary">{timetable.slots?.length || 0} slots</Badge>
                        <Button size="sm" variant="outline">
                          <Eye className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>

            {/* Quick Actions */}
            <Card>
              <CardHeader>
                <CardTitle>Quick Actions</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 gap-3">
                  <Button className="h-16 flex-col">
                    <Calendar className="w-6 h-6 mb-2" />
                    <span className="text-sm">Create Timetable</span>
                  </Button>
                  <Button variant="outline" className="h-16 flex-col">
                    <Settings className="w-6 h-6 mb-2" />
                    <span className="text-sm">Auto Generate</span>
                  </Button>
                  <Button variant="outline" className="h-16 flex-col">
                    <Download className="w-6 h-6 mb-2" />
                    <span className="text-sm">Export Reports</span>
                  </Button>
                  <Button variant="outline" className="h-16 flex-col">
                    <Users className="w-6 h-6 mb-2" />
                    <span className="text-sm">Manage Classes</span>
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="classes" className="space-y-6">
          {/* Search and Filter */}
          <Card>
            <CardContent className="p-4">
              <div className="flex items-center gap-4">
                <div className="relative flex-1">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
                  <Input
                    placeholder="Search classes..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <Button variant="outline">
                  <Settings className="w-4 h-4 mr-2" />
                  Filters
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Class List */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredClasses.map(classItem => {
              const classTimetable = timetables.find(tt => tt.classIds?.includes(classItem.id));
              const slotCount = classTimetable?.slots?.length || 0;
              
              return (
                <Card 
                  key={classItem.id} 
                  className={`hover:shadow-lg transition-shadow cursor-pointer ${
                    selectedClass === classItem.id ? 'ring-2 ring-blue-500' : ''
                  }`}
                  onClick={() => setSelectedClass(classItem.id)}
                >
                  <CardHeader>
                    <CardTitle className="flex items-center justify-between">
                      <span className="flex items-center gap-2">
                        <Users className="w-5 h-5 text-blue-600" />
                        {classItem.name}
                      </span>
                      <Badge variant={slotCount > 0 ? "default" : "secondary"}>
                        {slotCount} slots
                      </Badge>
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2">
                      <div className="flex items-center gap-2 text-sm text-gray-600">
                        <GraduationCap className="w-4 h-4" />
                        <span>Level: {classItem.level?.name || 'N/A'}</span>
                      </div>
                      <div className="flex items-center gap-2 text-sm text-gray-600">
                        <BookOpen className="w-4 h-4" />
                        <span>Capacity: {classItem.capacity || 'N/A'}</span>
                      </div>
                      <div className="flex items-center gap-2 text-sm text-gray-600">
                        <Calendar className="w-4 h-4" />
                        <span>Status: {slotCount > 0 ? 'Scheduled' : 'Pending'}</span>
                      </div>
                    </div>
                    
                    <div className="pt-3 border-t mt-4">
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline" className="flex-1">
                          <Eye className="w-4 h-4 mr-1" />
                          View
                        </Button>
                        <Button size="sm" variant="outline" className="flex-1">
                          <Edit className="w-4 h-4 mr-1" />
                          Edit
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>

          {/* Selected Class Schedule */}
          {selectedClass && (
            <ScheduleGrid
              slots={selectedClassSlots}
              periods={periods}
              title={`Schedule for ${classes.find(c => c.id === selectedClass)?.name}`}
              showTeacher={true}
              showRoom={true}
              showClass={false}
              emptyMessage="No schedule created for this class yet."
            />
          )}
        </TabsContent>

        <TabsContent value="teachers" className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {teachers.map(teacher => {
              // Count slots assigned to this teacher across all timetables
              const teacherSlots = timetables.reduce((slots, tt) => {
                const teacherSlotsInTimetable = tt.slots?.filter(slot => slot.teacher?.id === teacher.id) || [];
                return slots.concat(teacherSlotsInTimetable);
              }, [] as any[]);

              const weeklyHours = teacherSlots.length;
              const classesTeaching = new Set(teacherSlots.map(slot => slot.forClass?.name).filter(Boolean)).size;

              return (
                <Card key={teacher.id} className="hover:shadow-lg transition-shadow">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <User className="w-5 h-5 text-green-600" />
                      {teacher.firstName} {teacher.lastName}
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="text-center">
                          <p className="text-2xl font-bold text-blue-600">{classesTeaching}</p>
                          <p className="text-xs text-gray-600">Classes</p>
                        </div>
                        <div className="text-center">
                          <p className="text-2xl font-bold text-purple-600">{weeklyHours}</p>
                          <p className="text-xs text-gray-600">Hours/Week</p>
                        </div>
                      </div>

                      <div className="space-y-1">
                        <p className="text-sm font-medium text-gray-700">Specialization:</p>
                        <p className="text-sm text-gray-600">{teacher.qualifications || 'N/A'}</p>
                      </div>

                      <div className="pt-3 border-t">
                        <div className="flex gap-2">
                          <Button size="sm" variant="outline" className="flex-1">
                            <Eye className="w-4 h-4 mr-1" />
                            Schedule
                          </Button>
                          <Button size="sm" variant="outline" className="flex-1">
                            <Users className="w-4 h-4 mr-1" />
                            Classes
                          </Button>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>

          {teachers.length === 0 && (
            <Card>
              <CardContent className="p-8 text-center">
                <User className="w-12 h-12 mx-auto mb-4 text-gray-400" />
                <h3 className="text-lg font-semibold text-gray-900 mb-2">No Teachers Found</h3>
                <p className="text-gray-600">
                  No teachers are registered in the system yet.
                </p>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
