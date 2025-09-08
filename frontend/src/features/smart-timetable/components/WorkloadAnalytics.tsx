import React, { useState, useMemo } from 'react';
import { motion } from 'framer-motion';
import { 
  Users, 
  TrendingUp, 
  TrendingDown, 
  AlertTriangle, 
  CheckCircle, 
  Clock, 
  BarChart3,
  PieChart,
  Calendar,
  Target,
  Zap,
  Filter,
  Download,
  RefreshCw
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { 
  useAllTeacherWorkloads, 
  useTeacherWorkloadAnalysis,
  useBalanceTeacherWorkloads 
} from '../hooks/use-smart-timetable';
import type { 
  TeacherWorkloadAnalysis, 
  WorkloadStatus,
  DailyWorkload 
} from '@/types/smart-timetable';

interface WorkloadAnalyticsProps {
  timetableId: number;
  selectedTeacherId?: number;
  onTeacherSelect: (teacherId: number) => void;
}

const WORKLOAD_STATUS_CONFIG = {
  OPTIMAL: {
    color: 'bg-green-100 text-green-800 border-green-200',
    icon: CheckCircle,
    label: 'Optimal',
    description: '80-100% capacity utilization'
  },
  UNDERUTILIZED: {
    color: 'bg-blue-100 text-blue-800 border-blue-200',
    icon: TrendingDown,
    label: 'Underutilized',
    description: 'Less than 80% capacity'
  },
  OVERLOADED: {
    color: 'bg-orange-100 text-orange-800 border-orange-200',
    icon: TrendingUp,
    label: 'Overloaded',
    description: 'Over 100% capacity'
  },
  SEVERELY_OVERLOADED: {
    color: 'bg-red-100 text-red-800 border-red-200',
    icon: AlertTriangle,
    label: 'Severely Overloaded',
    description: 'Over 120% capacity'
  }
};

const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];

export function WorkloadAnalytics({ 
  timetableId, 
  selectedTeacherId, 
  onTeacherSelect 
}: WorkloadAnalyticsProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<WorkloadStatus | 'ALL'>('ALL');
  const [sortBy, setSortBy] = useState<'name' | 'workload' | 'efficiency'>('workload');

  // Data fetching
  const { data: allWorkloads, isLoading, refetch } = useAllTeacherWorkloads();
  const { data: selectedTeacherWorkload } = useTeacherWorkloadAnalysis(selectedTeacherId!);
  const balanceWorkloadsMutation = useBalanceTeacherWorkloads();

  // Filtered and sorted workloads
  const filteredWorkloads = useMemo(() => {
    if (!allWorkloads) return [];

    let filtered = allWorkloads.filter(workload => {
      const matchesSearch = searchQuery === '' || 
        `${workload.teacherFirstName} ${workload.teacherLastName}`.toLowerCase().includes(searchQuery.toLowerCase());
      const matchesStatus = statusFilter === 'ALL' || workload.status === statusFilter;
      return matchesSearch && matchesStatus;
    });

    // Sort
    filtered.sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return `${a.teacherFirstName} ${a.teacherLastName}`.localeCompare(`${b.teacherFirstName} ${b.teacherLastName}`);
        case 'workload':
          return b.workloadPercentage - a.workloadPercentage;
        case 'efficiency':
          return b.scheduleEfficiency - a.scheduleEfficiency;
        default:
          return 0;
      }
    });

    return filtered;
  }, [allWorkloads, searchQuery, statusFilter, sortBy]);

  // Summary statistics
  const summaryStats = useMemo(() => {
    if (!allWorkloads) return null;

    const totalTeachers = allWorkloads.length;
    const statusCounts = allWorkloads.reduce((acc, workload) => {
      acc[workload.status] = (acc[workload.status] || 0) + 1;
      return acc;
    }, {} as Record<WorkloadStatus, number>);

    const averageWorkload = allWorkloads.reduce((sum, w) => sum + w.workloadPercentage, 0) / totalTeachers;
    const averageEfficiency = allWorkloads.reduce((sum, w) => sum + w.scheduleEfficiency, 0) / totalTeachers;

    return {
      totalTeachers,
      statusCounts,
      averageWorkload,
      averageEfficiency,
      optimalCount: statusCounts.OPTIMAL || 0,
      issueCount: (statusCounts.OVERLOADED || 0) + (statusCounts.SEVERELY_OVERLOADED || 0) + (statusCounts.UNDERUTILIZED || 0)
    };
  }, [allWorkloads]);

  const handleBalanceWorkloads = async () => {
    try {
      await balanceWorkloadsMutation.mutateAsync(timetableId);
      refetch();
    } catch (error) {
      console.error('Failed to balance workloads:', error);
    }
  };

  if (isLoading) {
    return (
      <Card className="w-full">
        <CardContent className="p-6">
          <div className="flex items-center justify-center h-96">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card className="border-green-200/60 bg-gradient-to-br from-green-50/80 via-blue-50/40 to-teal-50/20 shadow-xl">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-gradient-to-br from-green-500 to-teal-600 rounded-lg">
                <Users className="h-6 w-6 text-white" />
              </div>
              <div>
                <CardTitle className="text-2xl font-bold bg-gradient-to-r from-green-900 via-teal-800 to-blue-700 bg-clip-text text-transparent">
                  Teacher Workload Analytics
                </CardTitle>
                <p className="text-green-700/80 mt-1">
                  Comprehensive workload analysis and optimization insights
                </p>
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <Button
                onClick={handleBalanceWorkloads}
                disabled={balanceWorkloadsMutation.isPending}
                className="bg-gradient-to-r from-green-600 to-teal-600 hover:from-green-700 hover:to-teal-700"
              >
                {balanceWorkloadsMutation.isPending ? (
                  <>
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                    Balancing...
                  </>
                ) : (
                  <>
                    <Zap className="h-4 w-4 mr-2" />
                    Balance Workloads
                  </>
                )}
              </Button>
              
              <Button variant="outline" onClick={() => refetch()}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Refresh
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Summary Statistics */}
      {summaryStats && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/60 to-indigo-50/30">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-blue-600 font-medium">Total Teachers</p>
                  <p className="text-2xl font-bold text-blue-900">{summaryStats.totalTeachers}</p>
                </div>
                <Users className="h-8 w-8 text-blue-500" />
              </div>
            </CardContent>
          </Card>

          <Card className="border-green-200/60 bg-gradient-to-br from-green-50/60 to-emerald-50/30">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-green-600 font-medium">Optimal Workload</p>
                  <p className="text-2xl font-bold text-green-900">{summaryStats.optimalCount}</p>
                </div>
                <CheckCircle className="h-8 w-8 text-green-500" />
              </div>
            </CardContent>
          </Card>

          <Card className="border-orange-200/60 bg-gradient-to-br from-orange-50/60 to-red-50/30">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-orange-600 font-medium">Need Attention</p>
                  <p className="text-2xl font-bold text-orange-900">{summaryStats.issueCount}</p>
                </div>
                <AlertTriangle className="h-8 w-8 text-orange-500" />
              </div>
            </CardContent>
          </Card>

          <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/60 to-pink-50/30">
            <CardContent className="p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-purple-600 font-medium">Avg. Efficiency</p>
                  <p className="text-2xl font-bold text-purple-900">
                    {(summaryStats.averageEfficiency * 100).toFixed(0)}%
                  </p>
                </div>
                <Target className="h-8 w-8 text-purple-500" />
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Teachers List */}
        <div className="lg:col-span-2 space-y-4">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2">
                  <BarChart3 className="h-5 w-5" />
                  Teacher Workloads
                </CardTitle>
                
                <div className="flex items-center gap-2">
                  <Input
                    placeholder="Search teachers..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-48"
                  />
                  
                  <Select value={statusFilter} onValueChange={(value: any) => setStatusFilter(value)}>
                    <SelectTrigger className="w-40">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">All Status</SelectItem>
                      <SelectItem value="OPTIMAL">Optimal</SelectItem>
                      <SelectItem value="UNDERUTILIZED">Underutilized</SelectItem>
                      <SelectItem value="OVERLOADED">Overloaded</SelectItem>
                      <SelectItem value="SEVERELY_OVERLOADED">Severely Overloaded</SelectItem>
                    </SelectContent>
                  </Select>
                  
                  <Select value={sortBy} onValueChange={(value: any) => setSortBy(value)}>
                    <SelectTrigger className="w-32">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="name">Name</SelectItem>
                      <SelectItem value="workload">Workload</SelectItem>
                      <SelectItem value="efficiency">Efficiency</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {filteredWorkloads.map((workload) => {
                  const statusConfig = WORKLOAD_STATUS_CONFIG[workload.status];
                  const StatusIcon = statusConfig.icon;
                  
                  return (
                    <motion.div
                      key={workload.teacherId}
                      className={`p-4 rounded-lg border cursor-pointer transition-all ${
                        selectedTeacherId === workload.teacherId
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300 hover:shadow-md'
                      }`}
                      onClick={() => onTeacherSelect(workload.teacherId)}
                      whileHover={{ scale: 1.01 }}
                      whileTap={{ scale: 0.99 }}
                    >
                      <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center text-white font-semibold">
                            {workload.teacherFirstName.charAt(0)}{workload.teacherLastName.charAt(0)}
                          </div>
                          <div>
                            <h3 className="font-semibold">
                              {workload.teacherFirstName} {workload.teacherLastName}
                            </h3>
                            <p className="text-sm text-gray-600">{workload.teacherEmail}</p>
                          </div>
                        </div>
                        
                        <div className="flex items-center gap-2">
                          <Badge className={statusConfig.color}>
                            <StatusIcon className="h-3 w-3 mr-1" />
                            {statusConfig.label}
                          </Badge>
                          <div className="text-right">
                            <div className="text-lg font-bold">
                              {workload.workloadPercentage.toFixed(0)}%
                            </div>
                            <div className="text-xs text-gray-500">
                              {workload.totalWeeklyHours}h / {workload.maxWeeklyCapacity}h
                            </div>
                          </div>
                        </div>
                      </div>
                      
                      <div className="space-y-2">
                        <div className="flex items-center justify-between text-sm">
                          <span>Workload</span>
                          <span>{workload.workloadPercentage.toFixed(1)}%</span>
                        </div>
                        <Progress 
                          value={Math.min(workload.workloadPercentage, 100)} 
                          className="h-2"
                        />
                        
                        <div className="grid grid-cols-3 gap-4 text-sm">
                          <div className="text-center">
                            <div className="font-medium">{workload.totalCourses}</div>
                            <div className="text-gray-500">Courses</div>
                          </div>
                          <div className="text-center">
                            <div className="font-medium">{workload.totalGaps}</div>
                            <div className="text-gray-500">Gaps</div>
                          </div>
                          <div className="text-center">
                            <div className="font-medium">{(workload.scheduleEfficiency * 100).toFixed(0)}%</div>
                            <div className="text-gray-500">Efficiency</div>
                          </div>
                        </div>
                      </div>
                      
                      {workload.scheduleIssues.length > 0 && (
                        <Alert className="mt-3">
                          <AlertTriangle className="h-4 w-4" />
                          <AlertDescription className="text-xs">
                            {workload.scheduleIssues.slice(0, 2).join(', ')}
                            {workload.scheduleIssues.length > 2 && ` +${workload.scheduleIssues.length - 2} more`}
                          </AlertDescription>
                        </Alert>
                      )}
                    </motion.div>
                  );
                })}
                
                {filteredWorkloads.length === 0 && (
                  <div className="text-center text-gray-500 py-8">
                    <Users className="h-12 w-12 mx-auto mb-4 opacity-50" />
                    <p>No teachers match your filters</p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Teacher Detail Panel */}
        <div className="space-y-4">
          {selectedTeacherWorkload ? (
            <>
              {/* Teacher Overview */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <User className="h-5 w-5" />
                    Teacher Details
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="text-center">
                      <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center text-white font-bold text-xl mx-auto mb-3">
                        {selectedTeacherWorkload.teacherFirstName.charAt(0)}{selectedTeacherWorkload.teacherLastName.charAt(0)}
                      </div>
                      <h3 className="font-semibold text-lg">
                        {selectedTeacherWorkload.teacherFirstName} {selectedTeacherWorkload.teacherLastName}
                      </h3>
                      <p className="text-sm text-gray-600">{selectedTeacherWorkload.teacherEmail}</p>
                    </div>
                    
                    <Separator />
                    
                    <div className="space-y-3">
                      <div className="flex justify-between">
                        <span className="text-sm">Weekly Hours</span>
                        <span className="font-medium">
                          {selectedTeacherWorkload.totalWeeklyHours} / {selectedTeacherWorkload.maxWeeklyCapacity}
                        </span>
                      </div>
                      
                      <div className="flex justify-between">
                        <span className="text-sm">Workload</span>
                        <span className="font-medium">
                          {selectedTeacherWorkload.workloadPercentage.toFixed(1)}%
                        </span>
                      </div>
                      
                      <div className="flex justify-between">
                        <span className="text-sm">Efficiency</span>
                        <span className="font-medium">
                          {(selectedTeacherWorkload.scheduleEfficiency * 100).toFixed(1)}%
                        </span>
                      </div>
                      
                      <div className="flex justify-between">
                        <span className="text-sm">Total Gaps</span>
                        <span className="font-medium">{selectedTeacherWorkload.totalGaps}</span>
                      </div>
                      
                      <div className="flex justify-between">
                        <span className="text-sm">Max Consecutive</span>
                        <span className="font-medium">{selectedTeacherWorkload.maxConsecutiveHours}h</span>
                      </div>
                    </div>
                    
                    <Badge className={WORKLOAD_STATUS_CONFIG[selectedTeacherWorkload.status].color}>
                      {WORKLOAD_STATUS_CONFIG[selectedTeacherWorkload.status].label}
                    </Badge>
                  </div>
                </CardContent>
              </Card>

              {/* Daily Schedule */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Calendar className="h-5 w-5" />
                    Weekly Schedule
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {DAYS_OF_WEEK.map(day => {
                      const dayWorkload = selectedTeacherWorkload.dailyWorkloads[day];
                      if (!dayWorkload) return null;
                      
                      return (
                        <div key={day} className="p-3 bg-gray-50 rounded-lg">
                          <div className="flex items-center justify-between mb-2">
                            <span className="font-medium text-sm">
                              {day.charAt(0) + day.slice(1).toLowerCase()}
                            </span>
                            <div className="flex items-center gap-2">
                              <Badge variant="outline" className="text-xs">
                                {dayWorkload.hoursScheduled}h
                              </Badge>
                              <Badge 
                                variant={dayWorkload.efficiency === 'HIGH' ? 'default' : 
                                        dayWorkload.efficiency === 'MEDIUM' ? 'secondary' : 'destructive'}
                                className="text-xs"
                              >
                                {dayWorkload.efficiency}
                              </Badge>
                            </div>
                          </div>
                          
                          <div className="grid grid-cols-3 gap-2 text-xs">
                            <div className="text-center">
                              <div className="font-medium">{dayWorkload.hoursScheduled}</div>
                              <div className="text-gray-500">Hours</div>
                            </div>
                            <div className="text-center">
                              <div className="font-medium">{dayWorkload.gaps}</div>
                              <div className="text-gray-500">Gaps</div>
                            </div>
                            <div className="text-center">
                              <div className="font-medium">{dayWorkload.consecutiveHours}</div>
                              <div className="text-gray-500">Consecutive</div>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </CardContent>
              </Card>

              {/* Recommendations */}
              {selectedTeacherWorkload.recommendations.length > 0 && (
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      <Target className="h-5 w-5" />
                      Recommendations
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      {selectedTeacherWorkload.recommendations.slice(0, 3).map((rec, index) => (
                        <div key={index} className="p-3 bg-blue-50 rounded-lg border border-blue-200">
                          <div className="flex items-start justify-between mb-2">
                            <h4 className="font-medium text-sm">{rec.title}</h4>
                            <Badge 
                              variant={rec.priority === 'HIGH' ? 'destructive' : 
                                      rec.priority === 'MEDIUM' ? 'default' : 'secondary'}
                              className="text-xs"
                            >
                              {rec.priority}
                            </Badge>
                          </div>
                          <p className="text-xs text-gray-600 mb-2">{rec.description}</p>
                          {rec.expectedImprovement > 0 && (
                            <div className="text-xs text-green-600">
                              Expected improvement: +{rec.expectedImprovement.toFixed(1)}%
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>
              )}
            </>
          ) : (
            <Card>
              <CardContent className="p-6">
                <div className="text-center text-gray-500 py-8">
                  <Users className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>Select a teacher to view detailed analytics</p>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
