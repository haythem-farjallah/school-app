import React, { useState, useCallback } from 'react';
import { Helmet } from 'react-helmet-async';
import { motion } from 'framer-motion';
import { 
  Brain, 
  Calendar, 
  Users, 
  AlertTriangle, 
  BarChart3,
  Settings,
  Download,
  RefreshCw,
  Zap
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { TimetableGrid } from '@/features/smart-timetable/components/TimetableGrid';
import { OptimizationDashboard } from '@/features/smart-timetable/components/OptimizationDashboard';
import { WorkloadAnalytics } from '@/features/smart-timetable/components/WorkloadAnalytics';
import { ConflictResolution } from '@/features/smart-timetable/components/ConflictResolution';
import { TimetableExportDialog } from '@/features/smart-timetable/components/TimetableExportDialog';
import { 
  useEnhancedTimetableSlots,
  useTimetableAnalytics,
  useApplyScheduleChange,
  useValidateScheduleChange,
  useExportTimetable
} from '@/features/smart-timetable/hooks/use-smart-timetable';
import { useTimetables } from '@/features/timetable/hooks';
import type { 
  EnhancedTimetableSlot, 
  TimetableGridConfig, 
  TimetableFilters,
  TimetableOptimizationResult,
  DragItem,
  DropResult
} from '@/types/smart-timetable';
import toast from 'react-hot-toast';

export default function SmartTimetable() {
  const [selectedTimetableId, setSelectedTimetableId] = useState<number>(1); // Default to first timetable
  const [selectedTeacherId, setSelectedTeacherId] = useState<number>();
  const [currentOptimization, setCurrentOptimization] = useState<TimetableOptimizationResult>();
  
  // Grid configuration
  const [gridConfig, setGridConfig] = useState<TimetableGridConfig>({
    showWeekends: false,
    startHour: 8,
    endHour: 17,
    slotDuration: 60,
    showRoomInfo: true,
    showTeacherInfo: true,
    colorScheme: 'subject',
    enableDragDrop: true,
    enableConflictHighlight: true,
    compactView: false
  });

  // Filters
  const [filters, setFilters] = useState<TimetableFilters>({
    teacherIds: [],
    classIds: [],
    courseIds: [],
    roomIds: [],
    dayOfWeek: [],
    conflictsOnly: false,
    unassignedOnly: false,
    searchQuery: ''
  });

  // Data fetching
  const { data: timetables } = useTimetables({ page: 1, size: 50 });
  const { data: timetableSlots, isLoading: slotsLoading, refetch: refetchSlots } = useEnhancedTimetableSlots(selectedTimetableId);
  const { data: analytics } = useTimetableAnalytics(selectedTimetableId);
  
  // Mutations
  const validateChangeMutation = useValidateScheduleChange();
  const applyChangeMutation = useApplyScheduleChange();
  const exportMutation = useExportTimetable();

  // Event handlers
  const handleSlotClick = useCallback((slot: EnhancedTimetableSlot) => {
    console.log('Slot clicked:', slot);
    // Could open a slot details modal here
  }, []);

  const handleSlotDrop = useCallback(async (dragItem: DragItem, dropResult: DropResult) => {
    console.log('Slot drop:', { dragItem, dropResult });
    
    try {
      // First validate the change
      const isValid = await validateChangeMutation.mutateAsync({
        timetableId: selectedTimetableId,
        slotId: dragItem.slot.id,
        newTeacherId: dropResult.targetTeacherId,
        newRoomId: dropResult.targetRoomId
      });

      if (!isValid) {
        toast.error('This change would create conflicts');
        return;
      }

      // Apply the change
      await applyChangeMutation.mutateAsync({
        timetableId: selectedTimetableId,
        slotId: dragItem.slot.id,
        newTeacherId: dropResult.targetTeacherId,
        newRoomId: dropResult.targetRoomId
      });

      // Refresh the slots
      refetchSlots();
      
    } catch (error) {
      console.error('Failed to apply slot change:', error);
    }
  }, [selectedTimetableId, validateChangeMutation, applyChangeMutation, refetchSlots]);

  const handleConfigChange = useCallback((updates: Partial<TimetableGridConfig>) => {
    setGridConfig(prev => ({ ...prev, ...updates }));
  }, []);

  const handleFiltersChange = useCallback((updates: Partial<TimetableFilters>) => {
    setFilters(prev => ({ ...prev, ...updates }));
  }, []);

  const handleOptimizationComplete = useCallback((result: TimetableOptimizationResult) => {
    setCurrentOptimization(result);
    refetchSlots();
  }, [refetchSlots]);

  const handleConflictResolved = useCallback(() => {
    refetchSlots();
  }, [refetchSlots]);

  const handleExport = useCallback(async (format: 'PDF' | 'EXCEL' | 'CSV') => {
    try {
      await exportMutation.mutateAsync({
        timetableId: selectedTimetableId,
        format,
        options: {
          includeTeacherInfo: gridConfig.showTeacherInfo,
          includeRoomInfo: gridConfig.showRoomInfo,
          includeConflicts: gridConfig.enableConflictHighlight
        }
      });
    } catch (error) {
      console.error('Export failed:', error);
    }
  }, [selectedTimetableId, gridConfig, exportMutation]);

  return (
    <>
      <Helmet>
        <title>Smart Timetable - AI-Powered Scheduling | School Management</title>
        <meta name="description" content="Intelligent timetable optimization with AI-powered conflict resolution and workload balancing" />
      </Helmet>

      <div className="container mx-auto p-6 space-y-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between"
        >
          <div className="flex items-center gap-4">
            <div className="p-3 bg-gradient-to-br from-purple-500 via-blue-500 to-indigo-600 rounded-xl shadow-lg">
              <Brain className="h-8 w-8 text-white" />
            </div>
            <div>
              <h1 className="text-4xl font-bold bg-gradient-to-r from-purple-900 via-blue-800 to-indigo-700 bg-clip-text text-transparent">
                Smart Timetable System
              </h1>
              <p className="text-lg text-gray-600 mt-1">
                AI-powered intelligent scheduling and optimization
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Timetable Selector */}
            <Select
              value={selectedTimetableId.toString()}
              onValueChange={(value) => setSelectedTimetableId(parseInt(value))}
            >
              <SelectTrigger className="w-48">
                <SelectValue placeholder="Select Timetable" />
              </SelectTrigger>
              <SelectContent>
                {timetables?.data?.map((timetable: any) => (
                  <SelectItem key={timetable.id} value={timetable.id.toString()}>
                    {timetable.name} ({timetable.academicYear})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            {/* Export Options */}
            <TimetableExportDialog
              timetableId={selectedTimetableId}
              timetableName={timetables?.data?.find((t: any) => t.id === selectedTimetableId)?.name || 'Timetable'}
              trigger={
                <Button variant="outline" className="flex items-center gap-2">
                  <Download className="h-4 w-4" />
                  Export
                </Button>
              }
            />

            <Button
              variant="outline"
              onClick={() => refetchSlots()}
              disabled={slotsLoading}
            >
              <RefreshCw className={`h-4 w-4 mr-2 ${slotsLoading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
          </div>
        </motion.div>

        {/* Analytics Summary */}
        {analytics && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
          >
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <Card className="border-green-200/60 bg-gradient-to-br from-green-50/60 to-emerald-50/30">
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-green-600 font-medium">Overall Health</p>
                      <p className="text-xl font-bold text-green-900">{analytics.overallHealth}</p>
                    </div>
                    <BarChart3 className="h-6 w-6 text-green-500" />
                  </div>
                </CardContent>
              </Card>

              <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/60 to-indigo-50/30">
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-blue-600 font-medium">Conflicts</p>
                      <p className="text-xl font-bold text-blue-900">
                        {analytics.conflictReport.totalConflicts}
                      </p>
                    </div>
                    <AlertTriangle className="h-6 w-6 text-blue-500" />
                  </div>
                </CardContent>
              </Card>

              <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/60 to-pink-50/30">
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-purple-600 font-medium">Teacher Utilization</p>
                      <p className="text-xl font-bold text-purple-900">
                        {(analytics.utilizationStats.averageTeacherUtilization * 100).toFixed(0)}%
                      </p>
                    </div>
                    <Users className="h-6 w-6 text-purple-500" />
                  </div>
                </CardContent>
              </Card>

              <Card className="border-orange-200/60 bg-gradient-to-br from-orange-50/60 to-red-50/30">
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm text-orange-600 font-medium">Efficiency Score</p>
                      <p className="text-xl font-bold text-orange-900">
                        {(analytics.utilizationStats.efficiencyScore * 100).toFixed(0)}%
                      </p>
                    </div>
                    <Zap className="h-6 w-6 text-orange-500" />
                  </div>
                </CardContent>
              </Card>
            </div>
          </motion.div>
        )}

        {/* Main Content */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          <Tabs defaultValue="timetable" className="w-full">
            <TabsList className="grid w-full grid-cols-4 mb-6">
              <TabsTrigger value="timetable" className="flex items-center gap-2">
                <Calendar className="h-4 w-4" />
                Interactive Timetable
              </TabsTrigger>
              <TabsTrigger value="optimization" className="flex items-center gap-2">
                <Brain className="h-4 w-4" />
                AI Optimization
              </TabsTrigger>
              <TabsTrigger value="workload" className="flex items-center gap-2">
                <Users className="h-4 w-4" />
                Workload Analytics
              </TabsTrigger>
              <TabsTrigger value="conflicts" className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4" />
                Conflict Resolution
              </TabsTrigger>
            </TabsList>

            {/* Interactive Timetable Tab */}
            <TabsContent value="timetable" className="space-y-6">
              {timetableSlots && (
                <TimetableGrid
                  timetableId={selectedTimetableId}
                  slots={timetableSlots}
                  config={gridConfig}
                  filters={filters}
                  onSlotClick={handleSlotClick}
                  onSlotDrop={handleSlotDrop}
                  onConfigChange={handleConfigChange}
                  onFiltersChange={handleFiltersChange}
                  isLoading={slotsLoading}
                />
              )}
            </TabsContent>

            {/* AI Optimization Tab */}
            <TabsContent value="optimization" className="space-y-6">
              <OptimizationDashboard
                timetableId={selectedTimetableId}
                currentOptimization={currentOptimization}
                onOptimizationComplete={handleOptimizationComplete}
              />
            </TabsContent>

            {/* Workload Analytics Tab */}
            <TabsContent value="workload" className="space-y-6">
              <WorkloadAnalytics
                timetableId={selectedTimetableId}
                selectedTeacherId={selectedTeacherId}
                onTeacherSelect={setSelectedTeacherId}
              />
            </TabsContent>

            {/* Conflict Resolution Tab */}
            <TabsContent value="conflicts" className="space-y-6">
              <ConflictResolution
                timetableId={selectedTimetableId}
                onConflictResolved={handleConflictResolved}
              />
            </TabsContent>
          </Tabs>
        </motion.div>

        {/* Quick Actions Floating Panel */}
        {currentOptimization && (
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="fixed bottom-6 left-6 z-50"
          >
            <Card className="bg-gradient-to-r from-purple-600 to-blue-600 text-white shadow-2xl">
              <CardContent className="p-4">
                <div className="flex items-center gap-3">
                  <div className="p-2 bg-white/20 rounded-lg">
                    <Zap className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="font-semibold">Last Optimization</div>
                    <div className="text-sm opacity-90">
                      Score: {(currentOptimization.finalScore * 100).toFixed(1)}% 
                      ({currentOptimization.resolvedConflicts?.length || 0} conflicts resolved)
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}

        {/* High Priority Alerts */}
        {analytics && analytics.conflictReport.criticalConflicts > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="fixed top-20 right-6 z-50 max-w-sm"
          >
            <Alert className="border-red-200 bg-red-50">
              <AlertTriangle className="h-4 w-4 text-red-600" />
              <AlertDescription className="text-red-800">
                <strong>{analytics.conflictReport.criticalConflicts} critical conflicts</strong> detected. 
                Immediate attention required to prevent scheduling issues.
              </AlertDescription>
            </Alert>
          </motion.div>
        )}
      </div>
    </>
  );
}
