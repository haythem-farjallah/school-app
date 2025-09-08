"use client";

import * as React from "react";
import { motion } from "framer-motion";
import { TrendingUp, TrendingDown, Minus, Target, Award } from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { cn } from "@/lib/utils";

export interface ProgressData {
  label: string;
  current: number;
  target: number;
  unit?: string;
  color?: 'blue' | 'green' | 'purple' | 'orange' | 'red' | 'indigo';
  trend?: {
    value: number;
    period: string;
  };
}

interface ProgressChartProps {
  title: string;
  data: ProgressData[];
  className?: string;
  showTrends?: boolean;
  animated?: boolean;
}

const colorConfig = {
  blue: {
    bg: "bg-blue-500",
    text: "text-blue-700",
    light: "bg-blue-100",
    border: "border-blue-200"
  },
  green: {
    bg: "bg-green-500",
    text: "text-green-700",
    light: "bg-green-100",
    border: "border-green-200"
  },
  purple: {
    bg: "bg-purple-500",
    text: "text-purple-700",
    light: "bg-purple-100",
    border: "border-purple-200"
  },
  orange: {
    bg: "bg-orange-500",
    text: "text-orange-700",
    light: "bg-orange-100",
    border: "border-orange-200"
  },
  red: {
    bg: "bg-red-500",
    text: "text-red-700",
    light: "bg-red-100",
    border: "border-red-200"
  },
  indigo: {
    bg: "bg-indigo-500",
    text: "text-indigo-700",
    light: "bg-indigo-100",
    border: "border-indigo-200"
  }
};

function ProgressItem({ 
  data, 
  animated = true 
}: { 
  data: ProgressData; 
  animated?: boolean;
}) {
  const percentage = Math.min((data.current / data.target) * 100, 100);
  const isCompleted = data.current >= data.target;
  const color = data.color || 'blue';
  const config = colorConfig[color];

  const getTrendIcon = (value: number) => {
    if (value > 0) return TrendingUp;
    if (value < 0) return TrendingDown;
    return Minus;
  };

  const getTrendColor = (value: number) => {
    if (value > 0) return "text-green-600 bg-green-50";
    if (value < 0) return "text-red-600 bg-red-50";
    return "text-gray-600 bg-gray-50";
  };

  const TrendIcon = data.trend ? getTrendIcon(data.trend.value) : null;

  return (
    <motion.div
      initial={animated ? { opacity: 0, y: 20 } : {}}
      animate={animated ? { opacity: 1, y: 0 } : {}}
      transition={{ duration: 0.3 }}
      className="space-y-3"
    >
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h4 className="font-medium text-slate-900">{data.label}</h4>
          {isCompleted && (
            <Badge className="bg-green-100 text-green-700 border-green-200">
              <Award className="h-3 w-3 mr-1" />
              Complete
            </Badge>
          )}
        </div>
        
        <div className="flex items-center gap-2 text-sm">
          <span className={cn("font-semibold", config.text)}>
            {data.current.toLocaleString()}{data.unit}
          </span>
          <span className="text-slate-500">
            / {data.target.toLocaleString()}{data.unit}
          </span>
        </div>
      </div>

      {/* Progress Bar */}
      <div className="space-y-2">
        <div className="relative">
          <Progress 
            value={percentage} 
            className={cn("h-3", config.border)}
          />
          {/* Target indicator */}
          <div className="absolute top-0 right-0 bottom-0 w-1 bg-slate-300 rounded-r-full" />
        </div>
        
        <div className="flex items-center justify-between text-xs">
          <span className={cn("font-medium", config.text)}>
            {percentage.toFixed(1)}% complete
          </span>
          
          {data.trend && TrendIcon && (
            <div className={cn(
              "flex items-center gap-1 px-2 py-1 rounded-full",
              getTrendColor(data.trend.value)
            )}>
              <TrendIcon className="h-3 w-3" />
              <span className="font-medium">
                {data.trend.value > 0 ? '+' : ''}{data.trend.value}%
              </span>
              <span className="text-slate-500">
                {data.trend.period}
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Additional info */}
      <div className="flex items-center justify-between text-xs text-slate-500">
        <span>
          {data.target - data.current > 0 
            ? `${(data.target - data.current).toLocaleString()}${data.unit} remaining`
            : 'Target achieved!'
          }
        </span>
        
        {percentage >= 100 && (
          <div className="flex items-center gap-1 text-green-600">
            <Target className="h-3 w-3" />
            <span className="font-medium">Goal reached</span>
          </div>
        )}
      </div>
    </motion.div>
  );
}

export function ProgressChart({
  title,
  data,
  className,
  showTrends = true,
  animated = true
}: ProgressChartProps) {
  const overallProgress = data.reduce((acc, item) => {
    const percentage = Math.min((item.current / item.target) * 100, 100);
    return acc + percentage;
  }, 0) / data.length;

  const completedItems = data.filter(item => item.current >= item.target).length;

  return (
    <Card className={cn(
      "bg-white/95 backdrop-blur-sm shadow-xl border-slate-200/60",
      className
    )}>
      <CardHeader className="pb-4">
        <div className="flex items-center justify-between">
          <CardTitle className="text-xl font-bold bg-gradient-to-r from-slate-900 to-blue-800 bg-clip-text text-transparent">
            {title}
          </CardTitle>
          
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
              {completedItems}/{data.length} Complete
            </Badge>
            <Badge 
              variant="outline" 
              className={cn(
                overallProgress >= 80 ? "bg-green-50 text-green-700 border-green-200" :
                overallProgress >= 60 ? "bg-yellow-50 text-yellow-700 border-yellow-200" :
                "bg-red-50 text-red-700 border-red-200"
              )}
            >
              {overallProgress.toFixed(1)}% Overall
            </Badge>
          </div>
        </div>
      </CardHeader>
      
      <CardContent className="space-y-6">
        {data.length === 0 ? (
          <div className="text-center py-8">
            <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Target className="h-8 w-8 text-slate-400" />
            </div>
            <h3 className="font-medium text-slate-900 mb-2">No progress data</h3>
            <p className="text-sm text-slate-500">
              Progress information will appear here when available.
            </p>
          </div>
        ) : (
          data.map((item, index) => (
            <ProgressItem 
              key={`${item.label}-${index}`} 
              data={item} 
              animated={animated}
            />
          ))
        )}
      </CardContent>
    </Card>
  );
}

// Specialized progress components for different contexts

interface AcademicProgressProps {
  gpa: number;
  targetGPA: number;
  creditsCompleted: number;
  totalCredits: number;
  coursesCompleted: number;
  totalCourses: number;
  className?: string;
}

export function AcademicProgress({
  gpa,
  targetGPA,
  creditsCompleted,
  totalCredits,
  coursesCompleted,
  totalCourses,
  className
}: AcademicProgressProps) {
  const progressData: ProgressData[] = [
    {
      label: "GPA Progress",
      current: gpa,
      target: targetGPA,
      unit: "",
      color: "green",
      trend: { value: 2.5, period: "this semester" }
    },
    {
      label: "Credits Earned",
      current: creditsCompleted,
      target: totalCredits,
      unit: " credits",
      color: "blue",
      trend: { value: 8.3, period: "this semester" }
    },
    {
      label: "Courses Completed",
      current: coursesCompleted,
      target: totalCourses,
      unit: " courses",
      color: "purple",
      trend: { value: 12.5, period: "this year" }
    }
  ];

  return (
    <ProgressChart
      title="Academic Progress"
      data={progressData}
      className={className}
    />
  );
}

interface TeachingProgressProps {
  classesManaged: number;
  targetClasses: number;
  studentsTeaching: number;
  targetStudents: number;
  assignmentsGraded: number;
  totalAssignments: number;
  className?: string;
}

export function TeachingProgress({
  classesManaged,
  targetClasses,
  studentsTeaching,
  targetStudents,
  assignmentsGraded,
  totalAssignments,
  className
}: TeachingProgressProps) {
  const progressData: ProgressData[] = [
    {
      label: "Classes Managed",
      current: classesManaged,
      target: targetClasses,
      unit: " classes",
      color: "indigo",
      trend: { value: 0, period: "this semester" }
    },
    {
      label: "Students Teaching",
      current: studentsTeaching,
      target: targetStudents,
      unit: " students",
      color: "green",
      trend: { value: 5.2, period: "this semester" }
    },
    {
      label: "Assignments Graded",
      current: assignmentsGraded,
      target: totalAssignments,
      unit: " assignments",
      color: "orange",
      trend: { value: -2.1, period: "this week" }
    }
  ];

  return (
    <ProgressChart
      title="Teaching Progress"
      data={progressData}
      className={className}
    />
  );
}
