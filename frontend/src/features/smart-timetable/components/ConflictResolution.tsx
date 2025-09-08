import React, { useState, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  AlertTriangle, 
  CheckCircle, 
  Clock, 
  Users, 
  MapPin, 
  BookOpen,
  Zap,
  Target,
  Filter,
  Search,
  RefreshCw,
  ArrowRight,
  X,
  Info,
  AlertCircle,
  TrendingUp
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Separator } from '@/components/ui/separator';
import { Checkbox } from '@/components/ui/checkbox';
import { Progress } from '@/components/ui/progress';
import { 
  useTimetableConflicts, 
  useResolveConflicts 
} from '../hooks/use-smart-timetable';
import type { 
  TimetableConflictReport, 
  ConflictSeverity,
  TeacherConflict,
  RoomConflict,
  ClassConflict,
  ConflictResolutionSuggestion
} from '@/types/smart-timetable';

interface ConflictResolutionProps {
  timetableId: number;
  onConflictResolved: () => void;
}

const SEVERITY_CONFIG = {
  CRITICAL: {
    color: 'bg-red-100 text-red-800 border-red-300',
    icon: AlertTriangle,
    label: 'Critical',
    priority: 5
  },
  HIGH: {
    color: 'bg-orange-100 text-orange-800 border-orange-300',
    icon: AlertCircle,
    label: 'High',
    priority: 4
  },
  MEDIUM: {
    color: 'bg-yellow-100 text-yellow-800 border-yellow-300',
    icon: Info,
    label: 'Medium',
    priority: 3
  },
  LOW: {
    color: 'bg-blue-100 text-blue-800 border-blue-300',
    icon: Info,
    label: 'Low',
    priority: 2
  },
  NONE: {
    color: 'bg-green-100 text-green-800 border-green-300',
    icon: CheckCircle,
    label: 'None',
    priority: 1
  }
};

const CONFLICT_TYPE_ICONS = {
  DOUBLE_BOOKING: Users,
  OVERLOAD: TrendingUp,
  PREFERENCE_VIOLATION: Target,
  CAPACITY_EXCEEDED: MapPin,
  EQUIPMENT_MISMATCH: MapPin,
  EXCESSIVE_HOURS: Clock,
  INSUFFICIENT_BREAKS: Clock
};

export function ConflictResolution({ timetableId, onConflictResolved }: ConflictResolutionProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [severityFilter, setSeverityFilter] = useState<ConflictSeverity | 'ALL'>('ALL');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [selectedConflicts, setSelectedConflicts] = useState<string[]>([]);
  const [selectedSuggestion, setSelectedSuggestion] = useState<string>();

  // Data fetching
  const { data: conflictReport, isLoading, refetch } = useTimetableConflicts(timetableId);
  const resolveConflictsMutation = useResolveConflicts();

  // All conflicts combined
  const allConflicts = useMemo(() => {
    if (!conflictReport) return [];

    const conflicts = [
      ...conflictReport.teacherConflicts.map(c => ({ ...c, category: 'teacher' as const })),
      ...conflictReport.roomConflicts.map(c => ({ ...c, category: 'room' as const })),
      ...conflictReport.classConflicts.map(c => ({ ...c, category: 'class' as const }))
    ];

    return conflicts.sort((a, b) => 
      SEVERITY_CONFIG[b.severity].priority - SEVERITY_CONFIG[a.severity].priority
    );
  }, [conflictReport]);

  // Filtered conflicts
  const filteredConflicts = useMemo(() => {
    return allConflicts.filter(conflict => {
      const matchesSearch = searchQuery === '' || 
        conflict.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (conflict.category === 'teacher' && (conflict as any).teacherName.toLowerCase().includes(searchQuery.toLowerCase()));
      
      const matchesSeverity = severityFilter === 'ALL' || conflict.severity === severityFilter;
      const matchesType = typeFilter === 'ALL' || conflict.conflictType === typeFilter;
      
      return matchesSearch && matchesSeverity && matchesType;
    });
  }, [allConflicts, searchQuery, severityFilter, typeFilter]);

  // Conflict types for filter
  const conflictTypes = useMemo(() => {
    const types = new Set(allConflicts.map(c => c.conflictType));
    return Array.from(types);
  }, [allConflicts]);

  const handleResolveSelected = async () => {
    if (selectedConflicts.length === 0) return;
    
    try {
      await resolveConflictsMutation.mutateAsync({
        timetableId,
        conflictTypes: selectedConflicts
      });
      setSelectedConflicts([]);
      refetch();
      onConflictResolved();
    } catch (error) {
      console.error('Failed to resolve conflicts:', error);
    }
  };

  const handleSelectAll = () => {
    if (selectedConflicts.length === filteredConflicts.length) {
      setSelectedConflicts([]);
    } else {
      setSelectedConflicts(filteredConflicts.map(c => c.conflictType));
    }
  };

  const toggleConflictSelection = (conflictType: string) => {
    setSelectedConflicts(prev => 
      prev.includes(conflictType)
        ? prev.filter(t => t !== conflictType)
        : [...prev, conflictType]
    );
  };

  if (isLoading) {
    return (
      <Card className="w-full">
        <CardContent className="p-6">
          <div className="flex items-center justify-center h-96">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600"></div>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!conflictReport) {
    return (
      <Card className="w-full">
        <CardContent className="p-6">
          <div className="text-center text-gray-500 py-8">
            <AlertTriangle className="h-12 w-12 mx-auto mb-4 opacity-50" />
            <p>Unable to load conflict report</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card className="border-red-200/60 bg-gradient-to-br from-red-50/80 via-orange-50/40 to-yellow-50/20 shadow-xl">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-gradient-to-br from-red-500 to-orange-600 rounded-lg">
                <AlertTriangle className="h-6 w-6 text-white" />
              </div>
              <div>
                <CardTitle className="text-2xl font-bold bg-gradient-to-r from-red-900 via-orange-800 to-yellow-700 bg-clip-text text-transparent">
                  Conflict Resolution Center
                </CardTitle>
                <p className="text-red-700/80 mt-1">
                  Detect, analyze, and resolve timetable conflicts automatically
                </p>
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <Badge className={SEVERITY_CONFIG[conflictReport.overallSeverity].color}>
                {SEVERITY_CONFIG[conflictReport.overallSeverity].label} Severity
              </Badge>
              
              <Button
                onClick={handleResolveSelected}
                disabled={selectedConflicts.length === 0 || resolveConflictsMutation.isPending}
                className="bg-gradient-to-r from-red-600 to-orange-600 hover:from-red-700 hover:to-orange-700"
              >
                {resolveConflictsMutation.isPending ? (
                  <>
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                    Resolving...
                  </>
                ) : (
                  <>
                    <Zap className="h-4 w-4 mr-2" />
                    Resolve Selected ({selectedConflicts.length})
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
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className="border-red-200/60 bg-gradient-to-br from-red-50/60 to-orange-50/30">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-red-600 font-medium">Total Conflicts</p>
                <p className="text-2xl font-bold text-red-900">{conflictReport.totalConflicts}</p>
              </div>
              <AlertTriangle className="h-8 w-8 text-red-500" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-orange-200/60 bg-gradient-to-br from-orange-50/60 to-yellow-50/30">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-orange-600 font-medium">Critical</p>
                <p className="text-2xl font-bold text-orange-900">{conflictReport.criticalConflicts}</p>
              </div>
              <AlertCircle className="h-8 w-8 text-orange-500" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-yellow-200/60 bg-gradient-to-br from-yellow-50/60 to-amber-50/30">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-yellow-600 font-medium">Major</p>
                <p className="text-2xl font-bold text-yellow-900">{conflictReport.majorConflicts}</p>
              </div>
              <Info className="h-8 w-8 text-yellow-500" />
            </div>
          </CardContent>
        </Card>

        <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/60 to-indigo-50/30">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-blue-600 font-medium">Minor</p>
                <p className="text-2xl font-bold text-blue-900">{conflictReport.minorConflicts}</p>
              </div>
              <Info className="h-8 w-8 text-blue-500" />
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Conflicts List */}
        <div className="lg:col-span-2 space-y-4">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2">
                  <AlertTriangle className="h-5 w-5" />
                  Active Conflicts
                </CardTitle>
                
                <div className="flex items-center gap-2">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <Input
                      placeholder="Search conflicts..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="pl-10 w-48"
                    />
                  </div>
                  
                  <Select value={severityFilter} onValueChange={(value: any) => setSeverityFilter(value)}>
                    <SelectTrigger className="w-32">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">All Severity</SelectItem>
                      <SelectItem value="CRITICAL">Critical</SelectItem>
                      <SelectItem value="HIGH">High</SelectItem>
                      <SelectItem value="MEDIUM">Medium</SelectItem>
                      <SelectItem value="LOW">Low</SelectItem>
                    </SelectContent>
                  </Select>
                  
                  <Select value={typeFilter} onValueChange={setTypeFilter}>
                    <SelectTrigger className="w-40">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">All Types</SelectItem>
                      {conflictTypes.map(type => (
                        <SelectItem key={type} value={type}>
                          {type.replace('_', ' ')}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              
              {filteredConflicts.length > 0 && (
                <div className="flex items-center gap-2">
                  <Checkbox
                    checked={selectedConflicts.length === filteredConflicts.length}
                    onCheckedChange={handleSelectAll}
                  />
                  <span className="text-sm text-gray-600">
                    Select All ({filteredConflicts.length} conflicts)
                  </span>
                </div>
              )}
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <AnimatePresence>
                  {filteredConflicts.map((conflict, index) => {
                    const severityConfig = SEVERITY_CONFIG[conflict.severity];
                    const SeverityIcon = severityConfig.icon;
                    const ConflictIcon = CONFLICT_TYPE_ICONS[conflict.conflictType as keyof typeof CONFLICT_TYPE_ICONS] || AlertTriangle;
                    
                    return (
                      <motion.div
                        key={`${conflict.category}-${index}`}
                        className="p-4 border rounded-lg hover:shadow-md transition-all"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -20 }}
                        transition={{ duration: 0.2, delay: index * 0.05 }}
                      >
                        <div className="flex items-start gap-3">
                          <Checkbox
                            checked={selectedConflicts.includes(conflict.conflictType)}
                            onCheckedChange={() => toggleConflictSelection(conflict.conflictType)}
                          />
                          
                          <div className="flex-1">
                            <div className="flex items-center justify-between mb-2">
                              <div className="flex items-center gap-2">
                                <ConflictIcon className="h-4 w-4 text-gray-600" />
                                <span className="font-medium">
                                  {conflict.conflictType.replace('_', ' ')}
                                </span>
                                <Badge variant="outline" className="text-xs">
                                  {conflict.category}
                                </Badge>
                              </div>
                              
                              <Badge className={severityConfig.color}>
                                <SeverityIcon className="h-3 w-3 mr-1" />
                                {severityConfig.label}
                              </Badge>
                            </div>
                            
                            <p className="text-sm text-gray-700 mb-3">{conflict.description}</p>
                            
                            {conflict.conflictingSlots && conflict.conflictingSlots.length > 0 && (
                              <div className="mb-3">
                                <p className="text-xs text-gray-500 mb-1">Affected Slots:</p>
                                <div className="flex flex-wrap gap-1">
                                  {conflict.conflictingSlots.slice(0, 3).map((slot, slotIndex) => (
                                    <Badge key={slotIndex} variant="secondary" className="text-xs">
                                      {slot.dayOfWeek} {slot.period}
                                    </Badge>
                                  ))}
                                  {conflict.conflictingSlots.length > 3 && (
                                    <Badge variant="secondary" className="text-xs">
                                      +{conflict.conflictingSlots.length - 3} more
                                    </Badge>
                                  )}
                                </div>
                              </div>
                            )}
                            
                            <div className="text-xs text-gray-600">
                              <strong>Impact:</strong> {conflict.impact}
                            </div>
                            
                            {conflict.resolutionOptions && conflict.resolutionOptions.length > 0 && (
                              <div className="mt-2">
                                <p className="text-xs text-gray-500 mb-1">Resolution Options:</p>
                                <ul className="text-xs text-gray-600 list-disc list-inside">
                                  {conflict.resolutionOptions.slice(0, 2).map((option, optionIndex) => (
                                    <li key={optionIndex}>{option}</li>
                                  ))}
                                </ul>
                              </div>
                            )}
                          </div>
                        </div>
                      </motion.div>
                    );
                  })}
                </AnimatePresence>
                
                {filteredConflicts.length === 0 && (
                  <div className="text-center text-gray-500 py-8">
                    {conflictReport.totalConflicts === 0 ? (
                      <>
                        <CheckCircle className="h-12 w-12 mx-auto mb-4 text-green-500" />
                        <p className="text-green-600 font-medium">No conflicts detected!</p>
                        <p className="text-sm">Your timetable is optimally scheduled</p>
                      </>
                    ) : (
                      <>
                        <Filter className="h-12 w-12 mx-auto mb-4 opacity-50" />
                        <p>No conflicts match your filters</p>
                      </>
                    )}
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Resolution Suggestions */}
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Target className="h-5 w-5" />
                Resolution Suggestions
              </CardTitle>
            </CardHeader>
            <CardContent>
              {conflictReport.resolutionSuggestions && conflictReport.resolutionSuggestions.length > 0 ? (
                <div className="space-y-3">
                  {conflictReport.resolutionSuggestions.slice(0, 5).map((suggestion) => (
                    <motion.div
                      key={suggestion.suggestionId}
                      className={`p-3 rounded-lg border cursor-pointer transition-all ${
                        selectedSuggestion === suggestion.suggestionId
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                      onClick={() => setSelectedSuggestion(suggestion.suggestionId)}
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                    >
                      <div className="flex items-start justify-between mb-2">
                        <h4 className="font-medium text-sm">{suggestion.title}</h4>
                        <div className="flex items-center gap-1">
                          <Badge 
                            variant={suggestion.resolutionType === 'AUTOMATIC' ? 'default' : 'secondary'}
                            className="text-xs"
                          >
                            {suggestion.resolutionType}
                          </Badge>
                          <Badge variant="outline" className="text-xs">
                            {suggestion.estimatedTimeMinutes}min
                          </Badge>
                        </div>
                      </div>
                      
                      <p className="text-xs text-gray-600 mb-2">{suggestion.description}</p>
                      
                      <div className="flex items-center justify-between">
                        <div className="text-xs text-green-600">
                          +{suggestion.expectedImprovement.toFixed(1)}% improvement
                        </div>
                        <ArrowRight className="h-3 w-3 text-gray-400" />
                      </div>
                    </motion.div>
                  ))}
                </div>
              ) : (
                <div className="text-center text-gray-500 py-8">
                  <Target className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>No resolution suggestions available</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Impact Analysis */}
          {conflictReport.impactAnalysis && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <TrendingUp className="h-5 w-5" />
                  Impact Analysis
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div className="flex justify-between text-sm">
                    <span>Affected Teachers</span>
                    <span className="font-medium">{conflictReport.impactAnalysis.affectedTeachers}</span>
                  </div>
                  
                  <div className="flex justify-between text-sm">
                    <span>Affected Classes</span>
                    <span className="font-medium">{conflictReport.impactAnalysis.affectedClasses}</span>
                  </div>
                  
                  <div className="flex justify-between text-sm">
                    <span>Affected Rooms</span>
                    <span className="font-medium">{conflictReport.impactAnalysis.affectedRooms}</span>
                  </div>
                  
                  <div className="flex justify-between text-sm">
                    <span>Estimated Students</span>
                    <span className="font-medium">{conflictReport.impactAnalysis.affectedStudents}</span>
                  </div>
                  
                  <Separator />
                  
                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span>Schedule Quality</span>
                      <span className="font-medium">
                        {(conflictReport.impactAnalysis.scheduleQualityScore * 100).toFixed(0)}%
                      </span>
                    </div>
                    <Progress 
                      value={conflictReport.impactAnalysis.scheduleQualityScore * 100} 
                      className="h-2"
                    />
                  </div>
                  
                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span>Teacher Satisfaction</span>
                      <span className="font-medium">
                        {(conflictReport.impactAnalysis.teacherSatisfactionScore * 100).toFixed(0)}%
                      </span>
                    </div>
                    <Progress 
                      value={conflictReport.impactAnalysis.teacherSatisfactionScore * 100} 
                      className="h-2"
                    />
                  </div>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
