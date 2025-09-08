import React, { useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Brain, 
  Zap, 
  Target, 
  BarChart3, 
  Settings, 
  Play, 
  Pause, 
  RotateCcw,
  CheckCircle,
  AlertCircle,
  Clock,
  TrendingUp,
  Users,
  MapPin,
  BookOpen,
  Lightbulb
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Slider } from '@/components/ui/slider';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { 
  useOptimizeTimetable, 
  useReoptimizeTimetable, 
  useGenerateScenarios,
  useOptimizationStatus
} from '../hooks/use-smart-timetable';
import type { 
  TimetableOptimizationRequest, 
  TimetableOptimizationResult,
  OptimizationStatus 
} from '@/types/smart-timetable';

interface OptimizationDashboardProps {
  timetableId: number;
  currentOptimization?: TimetableOptimizationResult;
  onOptimizationComplete: (result: TimetableOptimizationResult) => void;
}

export function OptimizationDashboard({ 
  timetableId, 
  currentOptimization,
  onOptimizationComplete 
}: OptimizationDashboardProps) {
  const [optimizationRequest, setOptimizationRequest] = useState<TimetableOptimizationRequest>({
    timetableId,
    optimizationTimeSeconds: 30,
    teacherWorkloadWeight: 0.3,
    roomOptimizationWeight: 0.2,
    studentConvenienceWeight: 0.3,
    resourceEfficiencyWeight: 0.2,
    enableAIOptimization: true,
    enableWorkloadBalancing: true,
    enableConflictResolution: true,
    enableRoomOptimization: true,
    generateMultipleScenarios: false,
    scenarioCount: 3,
    minimumAcceptableScore: 0.7,
    maxIterationsWithoutImprovement: 100
  });

  const [activeOptimizationId, setActiveOptimizationId] = useState<string>();
  const [selectedScenario, setSelectedScenario] = useState<string>();

  // Mutations
  const optimizeMutation = useOptimizeTimetable();
  const reoptimizeMutation = useReoptimizeTimetable();
  const scenariosMutation = useGenerateScenarios();

  // Status polling
  const { data: optimizationStatus } = useOptimizationStatus(activeOptimizationId);

  const handleOptimize = useCallback(async () => {
    try {
      const result = await optimizeMutation.mutateAsync(optimizationRequest);
      setActiveOptimizationId(result.optimizationId);
      onOptimizationComplete(result);
    } catch (error) {
      console.error('Optimization failed:', error);
    }
  }, [optimizationRequest, optimizeMutation, onOptimizationComplete]);

  const handleReoptimize = useCallback(async () => {
    if (!optimizationRequest.hardConstraints?.length) return;
    
    try {
      const result = await reoptimizeMutation.mutateAsync({
        timetableId,
        additionalConstraints: optimizationRequest.hardConstraints
      });
      setActiveOptimizationId(result.optimizationId);
      onOptimizationComplete(result);
    } catch (error) {
      console.error('Re-optimization failed:', error);
    }
  }, [timetableId, optimizationRequest.hardConstraints, reoptimizeMutation, onOptimizationComplete]);

  const handleGenerateScenarios = useCallback(async () => {
    try {
      const result = await scenariosMutation.mutateAsync({
        request: { ...optimizationRequest, generateMultipleScenarios: true },
        scenarioCount: optimizationRequest.scenarioCount || 3
      });
      setActiveOptimizationId(result.optimizationId);
      onOptimizationComplete(result);
    } catch (error) {
      console.error('Scenario generation failed:', error);
    }
  }, [optimizationRequest, scenariosMutation, onOptimizationComplete]);

  const updateRequest = useCallback((updates: Partial<TimetableOptimizationRequest>) => {
    setOptimizationRequest(prev => ({ ...prev, ...updates }));
  }, []);

  const isOptimizing = optimizeMutation.isPending || reoptimizeMutation.isPending || scenariosMutation.isPending;
  const optimizationProgress = optimizationStatus?.status === 'RUNNING' ? 75 : 
                              optimizationStatus?.status === 'COMPLETED' ? 100 : 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/80 via-blue-50/40 to-indigo-50/20 shadow-xl">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-gradient-to-br from-purple-500 to-blue-600 rounded-lg">
                <Brain className="h-6 w-6 text-white" />
              </div>
              <div>
                <CardTitle className="text-2xl font-bold bg-gradient-to-r from-purple-900 via-blue-800 to-indigo-700 bg-clip-text text-transparent">
                  AI Optimization Dashboard
                </CardTitle>
                <p className="text-purple-700/80 mt-1">
                  Intelligent timetable optimization with machine learning
                </p>
              </div>
            </div>
            
            {/* Status Indicator */}
            <div className="flex items-center gap-2">
              {isOptimizing && (
                <div className="flex items-center gap-2 text-blue-600">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                  <span className="text-sm font-medium">Optimizing...</span>
                </div>
              )}
              {currentOptimization && !isOptimizing && (
                <Badge variant="secondary" className="bg-green-100 text-green-800">
                  <CheckCircle className="h-3 w-3 mr-1" />
                  Score: {(currentOptimization.finalScore * 100).toFixed(1)}%
                </Badge>
              )}
            </div>
          </div>
        </CardHeader>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Optimization Controls */}
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Settings className="h-5 w-5" />
                Optimization Parameters
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Tabs defaultValue="weights" className="w-full">
                <TabsList className="grid w-full grid-cols-4">
                  <TabsTrigger value="weights">Weights</TabsTrigger>
                  <TabsTrigger value="constraints">Constraints</TabsTrigger>
                  <TabsTrigger value="preferences">Preferences</TabsTrigger>
                  <TabsTrigger value="advanced">Advanced</TabsTrigger>
                </TabsList>

                {/* Weights Tab */}
                <TabsContent value="weights" className="space-y-6">
                  <div className="grid grid-cols-2 gap-6">
                    <div className="space-y-4">
                      <div>
                        <Label className="flex items-center gap-2 mb-2">
                          <Users className="h-4 w-4" />
                          Teacher Workload ({(optimizationRequest.teacherWorkloadWeight! * 100).toFixed(0)}%)
                        </Label>
                        <Slider
                          value={[optimizationRequest.teacherWorkloadWeight! * 100]}
                          onValueChange={([value]) => updateRequest({ teacherWorkloadWeight: value / 100 })}
                          max={100}
                          step={5}
                          className="w-full"
                        />
                      </div>
                      
                      <div>
                        <Label className="flex items-center gap-2 mb-2">
                          <MapPin className="h-4 w-4" />
                          Room Optimization ({(optimizationRequest.roomOptimizationWeight! * 100).toFixed(0)}%)
                        </Label>
                        <Slider
                          value={[optimizationRequest.roomOptimizationWeight! * 100]}
                          onValueChange={([value]) => updateRequest({ roomOptimizationWeight: value / 100 })}
                          max={100}
                          step={5}
                          className="w-full"
                        />
                      </div>
                    </div>

                    <div className="space-y-4">
                      <div>
                        <Label className="flex items-center gap-2 mb-2">
                          <BookOpen className="h-4 w-4" />
                          Student Convenience ({(optimizationRequest.studentConvenienceWeight! * 100).toFixed(0)}%)
                        </Label>
                        <Slider
                          value={[optimizationRequest.studentConvenienceWeight! * 100]}
                          onValueChange={([value]) => updateRequest({ studentConvenienceWeight: value / 100 })}
                          max={100}
                          step={5}
                          className="w-full"
                        />
                      </div>
                      
                      <div>
                        <Label className="flex items-center gap-2 mb-2">
                          <BarChart3 className="h-4 w-4" />
                          Resource Efficiency ({(optimizationRequest.resourceEfficiencyWeight! * 100).toFixed(0)}%)
                        </Label>
                        <Slider
                          value={[optimizationRequest.resourceEfficiencyWeight! * 100]}
                          onValueChange={([value]) => updateRequest({ resourceEfficiencyWeight: value / 100 })}
                          max={100}
                          step={5}
                          className="w-full"
                        />
                      </div>
                    </div>
                  </div>

                  <Alert>
                    <Lightbulb className="h-4 w-4" />
                    <AlertDescription>
                      Adjust weights to prioritize different aspects of optimization. Higher weights mean more importance.
                    </AlertDescription>
                  </Alert>
                </TabsContent>

                {/* Constraints Tab */}
                <TabsContent value="constraints" className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <Label htmlFor="ai-optimization">AI Optimization</Label>
                        <Switch
                          id="ai-optimization"
                          checked={optimizationRequest.enableAIOptimization}
                          onCheckedChange={(checked) => updateRequest({ enableAIOptimization: checked })}
                        />
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <Label htmlFor="workload-balancing">Workload Balancing</Label>
                        <Switch
                          id="workload-balancing"
                          checked={optimizationRequest.enableWorkloadBalancing}
                          onCheckedChange={(checked) => updateRequest({ enableWorkloadBalancing: checked })}
                        />
                      </div>
                    </div>

                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <Label htmlFor="conflict-resolution">Conflict Resolution</Label>
                        <Switch
                          id="conflict-resolution"
                          checked={optimizationRequest.enableConflictResolution}
                          onCheckedChange={(checked) => updateRequest({ enableConflictResolution: checked })}
                        />
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <Label htmlFor="room-optimization">Room Optimization</Label>
                        <Switch
                          id="room-optimization"
                          checked={optimizationRequest.enableRoomOptimization}
                          onCheckedChange={(checked) => updateRequest({ enableRoomOptimization: checked })}
                        />
                      </div>
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="hard-constraints" className="mb-2 block">Hard Constraints</Label>
                    <Textarea
                      id="hard-constraints"
                      placeholder="Enter hard constraints (one per line)..."
                      value={optimizationRequest.hardConstraints?.join('\n') || ''}
                      onChange={(e) => updateRequest({ 
                        hardConstraints: e.target.value.split('\n').filter(Boolean) 
                      })}
                      rows={3}
                    />
                  </div>
                </TabsContent>

                {/* Preferences Tab */}
                <TabsContent value="preferences" className="space-y-4">
                  <div className="text-center text-gray-500 py-8">
                    <Settings className="h-12 w-12 mx-auto mb-4 opacity-50" />
                    <p>Teacher and room preferences will be loaded here</p>
                    <p className="text-sm">Configure individual preferences in the respective management sections</p>
                  </div>
                </TabsContent>

                {/* Advanced Tab */}
                <TabsContent value="advanced" className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="optimization-time" className="mb-2 block">
                        Optimization Time ({optimizationRequest.optimizationTimeSeconds}s)
                      </Label>
                      <Slider
                        value={[optimizationRequest.optimizationTimeSeconds!]}
                        onValueChange={([value]) => updateRequest({ optimizationTimeSeconds: value })}
                        min={10}
                        max={300}
                        step={10}
                        className="w-full"
                      />
                    </div>

                    <div>
                      <Label htmlFor="min-score" className="mb-2 block">
                        Minimum Score ({(optimizationRequest.minimumAcceptableScore! * 100).toFixed(0)}%)
                      </Label>
                      <Slider
                        value={[optimizationRequest.minimumAcceptableScore! * 100]}
                        onValueChange={([value]) => updateRequest({ minimumAcceptableScore: value / 100 })}
                        min={50}
                        max={100}
                        step={5}
                        className="w-full"
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-between">
                    <Label htmlFor="generate-scenarios">Generate Multiple Scenarios</Label>
                    <Switch
                      id="generate-scenarios"
                      checked={optimizationRequest.generateMultipleScenarios}
                      onCheckedChange={(checked) => updateRequest({ generateMultipleScenarios: checked })}
                    />
                  </div>

                  {optimizationRequest.generateMultipleScenarios && (
                    <div>
                      <Label htmlFor="scenario-count" className="mb-2 block">
                        Number of Scenarios ({optimizationRequest.scenarioCount})
                      </Label>
                      <Slider
                        value={[optimizationRequest.scenarioCount!]}
                        onValueChange={([value]) => updateRequest({ scenarioCount: value })}
                        min={2}
                        max={10}
                        step={1}
                        className="w-full"
                      />
                    </div>
                  )}
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>

          {/* Action Buttons */}
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <Button
                    onClick={handleOptimize}
                    disabled={isOptimizing}
                    size="lg"
                    className="bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700"
                  >
                    {isOptimizing ? (
                      <>
                        <Pause className="h-4 w-4 mr-2" />
                        Optimizing...
                      </>
                    ) : (
                      <>
                        <Play className="h-4 w-4 mr-2" />
                        Start Optimization
                      </>
                    )}
                  </Button>

                  <Button
                    onClick={handleReoptimize}
                    disabled={isOptimizing || !optimizationRequest.hardConstraints?.length}
                    variant="outline"
                  >
                    <RotateCcw className="h-4 w-4 mr-2" />
                    Re-optimize
                  </Button>

                  <Button
                    onClick={handleGenerateScenarios}
                    disabled={isOptimizing}
                    variant="outline"
                  >
                    <Target className="h-4 w-4 mr-2" />
                    Generate Scenarios
                  </Button>
                </div>

                {isOptimizing && (
                  <div className="flex items-center gap-3">
                    <div className="text-sm text-gray-600">
                      Progress: {optimizationProgress}%
                    </div>
                    <Progress value={optimizationProgress} className="w-32" />
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Results Panel */}
        <div className="space-y-6">
          {/* Current Status */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BarChart3 className="h-5 w-5" />
                Optimization Status
              </CardTitle>
            </CardHeader>
            <CardContent>
              {currentOptimization ? (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Final Score</span>
                    <Badge variant="secondary" className="bg-green-100 text-green-800">
                      {(currentOptimization.finalScore * 100).toFixed(1)}%
                    </Badge>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Duration</span>
                    <span className="text-sm font-medium">
                      {(currentOptimization.durationMs / 1000).toFixed(1)}s
                    </span>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Conflicts Resolved</span>
                    <span className="text-sm font-medium">
                      {currentOptimization.resolvedConflicts?.length || 0}
                    </span>
                  </div>
                  
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Improvement</span>
                    <div className="flex items-center gap-1">
                      <TrendingUp className="h-3 w-3 text-green-500" />
                      <span className="text-sm font-medium text-green-600">
                        +{(currentOptimization.improvementPercentage || 0).toFixed(1)}%
                      </span>
                    </div>
                  </div>

                  <Separator />

                  <div className="space-y-2">
                    <h4 className="text-sm font-medium">Quality Metrics</h4>
                    <div className="space-y-1">
                      <div className="flex justify-between text-xs">
                        <span>Teacher Satisfaction</span>
                        <span>{(currentOptimization.qualityMetrics?.teacherSatisfaction * 100 || 0).toFixed(0)}%</span>
                      </div>
                      <div className="flex justify-between text-xs">
                        <span>Resource Efficiency</span>
                        <span>{(currentOptimization.qualityMetrics?.resourceEfficiency * 100 || 0).toFixed(0)}%</span>
                      </div>
                      <div className="flex justify-between text-xs">
                        <span>Schedule Compactness</span>
                        <span>{(currentOptimization.qualityMetrics?.scheduleCompactness * 100 || 0).toFixed(0)}%</span>
                      </div>
                    </div>
                  </div>
                </div>
              ) : (
                <div className="text-center text-gray-500 py-8">
                  <Clock className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>No optimization results yet</p>
                  <p className="text-sm">Start an optimization to see results</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Scenarios */}
          {currentOptimization?.alternativeScenarios && currentOptimization.alternativeScenarios.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Target className="h-5 w-5" />
                  Alternative Scenarios
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {currentOptimization.alternativeScenarios.map((scenario) => (
                    <motion.div
                      key={scenario.scenarioId}
                      className={`p-3 rounded-lg border cursor-pointer transition-all ${
                        selectedScenario === scenario.scenarioId
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                      onClick={() => setSelectedScenario(scenario.scenarioId)}
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                    >
                      <div className="flex items-center justify-between mb-2">
                        <h4 className="font-medium text-sm">{scenario.scenarioName}</h4>
                        <Badge variant="outline">
                          {(scenario.score * 100).toFixed(1)}%
                        </Badge>
                      </div>
                      <p className="text-xs text-gray-600 mb-2">{scenario.description}</p>
                      <div className="flex gap-1">
                        {scenario.advantages.slice(0, 2).map((advantage, index) => (
                          <Badge key={index} variant="secondary" className="text-xs">
                            {advantage}
                          </Badge>
                        ))}
                      </div>
                    </motion.div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Recommendations */}
          {currentOptimization?.recommendations && currentOptimization.recommendations.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Lightbulb className="h-5 w-5" />
                  Recommendations
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {currentOptimization.recommendations.slice(0, 3).map((rec, index) => (
                    <div key={index} className="p-3 bg-yellow-50 rounded-lg border border-yellow-200">
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
                      <p className="text-xs text-gray-600">{rec.description}</p>
                      {rec.expectedImprovement > 0 && (
                        <div className="mt-2 text-xs text-green-600">
                          Expected improvement: +{rec.expectedImprovement.toFixed(1)}%
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
