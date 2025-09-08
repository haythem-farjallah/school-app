import * as React from "react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { BookOpen, GraduationCap, Users } from "lucide-react";
import { useCourses } from "@/features/courses/hooks/use-courses";
import { useClasses } from "@/features/classes/hooks/use-classes";
import type { Course } from "@/types/course";
import type { Class } from "@/types/class";

interface BulkCourseAssignmentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  selectedTeacherIds: number[];
  selectedTeacherNames: string[];
  onAssignCourses: (teacherIds: number[], courseIds: number[], classId: number) => Promise<void>;
  isLoading?: boolean;
}

export function BulkCourseAssignmentDialog({
  open,
  onOpenChange,
  selectedTeacherIds,
  selectedTeacherNames,
  onAssignCourses,
  isLoading = false,
}: BulkCourseAssignmentDialogProps) {
  const [selectedCourseIds, setSelectedCourseIds] = useState<number[]>([]);
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);

  // Fetch courses and classes
  const { data: coursesResponse, isLoading: coursesLoading } = useCourses({ size: 100 });
  const { data: classesResponse, isLoading: classesLoading } = useClasses({ size: 100 });

  const courses = coursesResponse?.data ?? [];
  const classes = classesResponse?.data ?? [];

  const handleCourseToggle = (courseId: number) => {
    setSelectedCourseIds(prev =>
      prev.includes(courseId)
        ? prev.filter(id => id !== courseId)
        : [...prev, courseId]
    );
  };

  const handleSelectAllCourses = () => {
    if (selectedCourseIds.length === courses.length) {
      setSelectedCourseIds([]);
    } else {
      setSelectedCourseIds(courses.map(course => course.id));
    }
  };

  const handleAssign = async () => {
    if (!selectedClassId || selectedCourseIds.length === 0) {
      return;
    }

    try {
      await onAssignCourses(selectedTeacherIds, selectedCourseIds, selectedClassId);
      onOpenChange(false);
      // Reset form
      setSelectedCourseIds([]);
      setSelectedClassId(null);
    } catch (error) {
      console.error("Failed to assign courses:", error);
    }
  };

  const handleClose = () => {
    onOpenChange(false);
    setSelectedCourseIds([]);
    setSelectedClassId(null);
  };

  const isFormValid = selectedClassId && selectedCourseIds.length > 0;

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-[600px] max-h-[80vh]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <BookOpen className="h-5 w-5 text-blue-600" />
            Bulk Assign Courses to Teachers
          </DialogTitle>
          <DialogDescription>
            Assign selected courses to {selectedTeacherIds.length} teacher{selectedTeacherIds.length !== 1 ? 's' : ''} 
            in a specific class.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Selected Teachers Summary */}
          <div className="space-y-2">
            <Label className="flex items-center gap-2">
              <Users className="h-4 w-4" />
              Selected Teachers ({selectedTeacherIds.length})
            </Label>
            <div className="flex flex-wrap gap-2 p-3 bg-blue-50 rounded-lg border">
              {selectedTeacherNames.slice(0, 3).map((name, index) => (
                <Badge key={index} variant="secondary">
                  {name}
                </Badge>
              ))}
              {selectedTeacherNames.length > 3 && (
                <Badge variant="outline">
                  +{selectedTeacherNames.length - 3} more
                </Badge>
              )}
            </div>
          </div>

          {/* Class Selection */}
          <div className="space-y-2">
            <Label className="flex items-center gap-2">
              <GraduationCap className="h-4 w-4" />
              Select Class *
            </Label>
            <Select 
              value={selectedClassId?.toString() ?? ""} 
              onValueChange={(value) => setSelectedClassId(parseInt(value))}
              disabled={classesLoading}
            >
              <SelectTrigger>
                <SelectValue placeholder={classesLoading ? "Loading classes..." : "Choose a class"} />
              </SelectTrigger>
              <SelectContent>
                {classes.map((clazz: Class) => (
                  <SelectItem key={clazz.id} value={clazz.id.toString()}>
                    <div className="flex items-center gap-2">
                      <span>{clazz.name}</span>
                      <Badge variant="outline" className="text-xs">
                        {clazz.level}
                      </Badge>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Course Selection */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <Label className="flex items-center gap-2">
                <BookOpen className="h-4 w-4" />
                Select Courses * ({selectedCourseIds.length} selected)
              </Label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handleSelectAllCourses}
                disabled={coursesLoading}
              >
                {selectedCourseIds.length === courses.length ? "Deselect All" : "Select All"}
              </Button>
            </div>

            <ScrollArea className="h-[200px] border rounded-lg p-3">
              {coursesLoading ? (
                <div className="text-center text-muted-foreground py-4">
                  Loading courses...
                </div>
              ) : courses.length === 0 ? (
                <div className="text-center text-muted-foreground py-4">
                  No courses available
                </div>
              ) : (
                <div className="space-y-2">
                  {courses.map((course: Course) => (
                    <div
                      key={course.id}
                      className="flex items-center space-x-3 p-2 rounded-lg hover:bg-gray-50 transition-colors"
                    >
                      <Checkbox
                        id={`course-${course.id}`}
                        checked={selectedCourseIds.includes(course.id)}
                        onCheckedChange={() => handleCourseToggle(course.id)}
                      />
                      <label
                        htmlFor={`course-${course.id}`}
                        className="flex-1 cursor-pointer"
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <span className="font-medium">{course.name}</span>
                            <div className="text-sm text-muted-foreground">
                              Code: {course.code}
                            </div>
                          </div>
                          <Badge variant="outline" className="text-xs">
                            {course.credits} credits
                          </Badge>
                        </div>
                      </label>
                    </div>
                  ))}
                </div>
              )}
            </ScrollArea>
          </div>

          {/* Assignment Summary */}
          {isFormValid && (
            <div className="p-3 bg-green-50 border border-green-200 rounded-lg">
              <div className="text-sm text-green-800">
                <strong>Assignment Summary:</strong> Assign {selectedCourseIds.length} course{selectedCourseIds.length !== 1 ? 's' : ''} 
                {" "}to {selectedTeacherIds.length} teacher{selectedTeacherIds.length !== 1 ? 's' : ''} 
                {" "}in the selected class.
              </div>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button 
            onClick={handleAssign} 
            disabled={!isFormValid || isLoading}
            className="bg-blue-600 hover:bg-blue-700"
          >
            {isLoading ? "Assigning..." : `Assign Courses`}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

