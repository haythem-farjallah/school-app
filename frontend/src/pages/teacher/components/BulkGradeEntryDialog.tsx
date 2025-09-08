import React, { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Users, Save, AlertCircle, CheckCircle } from "lucide-react";
import toast from "react-hot-toast";

// Mock data for students - in real app, fetch from API
const mockStudents = [
  { id: 1, firstName: "Alice", lastName: "Johnson", email: "alice.j@school.edu" },
  { id: 2, firstName: "Bob", lastName: "Smith", email: "bob.s@school.edu" },
  { id: 3, firstName: "Charlie", lastName: "Brown", email: "charlie.b@school.edu" },
  { id: 4, firstName: "Diana", lastName: "Wilson", email: "diana.w@school.edu" },
  { id: 5, firstName: "Edward", lastName: "Davis", email: "edward.d@school.edu" },
];

// Mock courses for the class
const mockCourses = [
  { id: 1, name: "Mathematics", code: "MATH101" },
  { id: 2, name: "Physics", code: "PHYS201" },
  { id: 3, name: "Chemistry", code: "CHEM101" },
];

const bulkGradeSchema = z.object({
  assessmentType: z.string().min(1, "Assessment type is required"),
  courseId: z.string().min(1, "Course is required"),
  term: z.string().optional(),
  description: z.string().optional(),
  grades: z.array(z.object({
    studentId: z.number(),
    value: z.number().min(0).max(20),
    comment: z.string().optional(),
    weight: z.number().min(0.1).max(10).default(1.0),
  })).min(1, "At least one grade is required"),
});

type BulkGradeFormData = z.infer<typeof bulkGradeSchema>;

interface BulkGradeEntryDialogProps {
  classId: number;
  className: string;
  isOpen: boolean;
  onClose: () => void;
}

const BulkGradeEntryDialog: React.FC<BulkGradeEntryDialogProps> = ({
  classId,
  className,
  isOpen,
  onClose,
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [students, setStudents] = useState(mockStudents);
  const [courses, setCourses] = useState(mockCourses);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    setValue,
    watch,
    getValues,
  } = useForm<BulkGradeFormData>({
    resolver: zodResolver(bulkGradeSchema),
    defaultValues: {
      assessmentType: "",
      courseId: "",
      term: "",
      description: "",
      grades: students.map(student => ({
        studentId: student.id,
        value: 0,
        comment: "",
        weight: 1.0,
      })),
    },
  });

  const watchedGrades = watch("grades");

  // Initialize grades when students change
  useEffect(() => {
    const initialGrades = students.map(student => ({
      studentId: student.id,
      value: 0,
      comment: "",
      weight: 1.0,
    }));
    setValue("grades", initialGrades);
  }, [students, setValue]);

  const handleClose = () => {
    reset();
    onClose();
  };

  const onSubmit = async (data: BulkGradeFormData) => {
    setIsLoading(true);
    try {
      // Filter out grades with 0 values (ungraded students)
      const validGrades = data.grades.filter(grade => grade.value > 0);
      
      if (validGrades.length === 0) {
        toast.error("Please enter at least one grade");
        return;
      }

      // Mock API call - replace with actual implementation
      const payload = {
        classId: classId,
        assessmentType: data.assessmentType,
        courseId: parseInt(data.courseId),
        term: data.term,
        description: data.description,
        grades: validGrades,
      };

      console.log("Submitting bulk grades:", payload);
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      toast.success(`Successfully entered grades for ${validGrades.length} students`);
      handleClose();
    } catch (error) {
      console.error("Failed to submit grades:", error);
      toast.error("Failed to submit grades. Please try again.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleGradeChange = (studentIndex: number, value: string) => {
    const numValue = parseFloat(value) || 0;
    if (numValue >= 0 && numValue <= 20) {
      setValue(`grades.${studentIndex}.value`, numValue);
    }
  };

  const setAllGrades = (value: number) => {
    students.forEach((_, index) => {
      setValue(`grades.${index}.value`, value);
    });
  };

  const clearAllGrades = () => {
    students.forEach((_, index) => {
      setValue(`grades.${index}.value`, 0);
      setValue(`grades.${index}.comment`, "");
    });
  };

  const calculateStats = () => {
    const validGrades = watchedGrades?.filter(g => g.value > 0) || [];
    if (validGrades.length === 0) return null;

    const values = validGrades.map(g => g.value);
    const average = values.reduce((sum, val) => sum + val, 0) / values.length;
    const min = Math.min(...values);
    const max = Math.max(...values);

    return { average: average.toFixed(2), min, max, count: validGrades.length };
  };

  const stats = calculateStats();

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Users className="h-5 w-5" />
            Bulk Grade Entry - {className}
          </DialogTitle>
          <DialogDescription>
            Enter grades for multiple students at once. Grades should be between 0 and 20.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Assessment Details */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Assessment Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="assessmentType">Assessment Type *</Label>
                  <Select 
                    onValueChange={(value) => setValue("assessmentType", value)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select assessment type" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="exam">Exam</SelectItem>
                      <SelectItem value="quiz">Quiz</SelectItem>
                      <SelectItem value="assignment">Assignment</SelectItem>
                      <SelectItem value="project">Project</SelectItem>
                      <SelectItem value="presentation">Presentation</SelectItem>
                      <SelectItem value="homework">Homework</SelectItem>
                    </SelectContent>
                  </Select>
                  {errors.assessmentType && (
                    <p className="text-sm text-red-600">{errors.assessmentType.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="courseId">Course *</Label>
                  <Select 
                    onValueChange={(value) => setValue("courseId", value)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select course" />
                    </SelectTrigger>
                    <SelectContent>
                      {courses.map((course) => (
                        <SelectItem key={course.id} value={course.id.toString()}>
                          {course.code} - {course.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {errors.courseId && (
                    <p className="text-sm text-red-600">{errors.courseId.message}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="term">Term/Period</Label>
                  <Input
                    {...register("term")}
                    placeholder="e.g., Midterm, Final, Quarter 1"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="description">Description</Label>
                  <Input
                    {...register("description")}
                    placeholder="Optional description"
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Quick Actions</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setAllGrades(20)}
                >
                  Set All to 20
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setAllGrades(15)}
                >
                  Set All to 15
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setAllGrades(10)}
                >
                  Set All to 10
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={clearAllGrades}
                >
                  Clear All
                </Button>
              </div>
            </CardContent>
          </Card>

          {/* Statistics */}
          {stats && (
            <Alert>
              <CheckCircle className="h-4 w-4" />
              <AlertDescription>
                <strong>Current Stats:</strong> Average: {stats.average}, 
                Min: {stats.min}, Max: {stats.max}, 
                Graded Students: {stats.count}/{students.length}
              </AlertDescription>
            </Alert>
          )}

          {/* Students Grid */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Student Grades</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {students.map((student, index) => (
                  <div key={student.id} className="grid grid-cols-1 md:grid-cols-12 gap-4 items-center p-4 border rounded-lg">
                    <div className="md:col-span-4">
                      <p className="font-medium">{student.firstName} {student.lastName}</p>
                      <p className="text-sm text-gray-500">{student.email}</p>
                    </div>
                    
                    <div className="md:col-span-2">
                      <Label htmlFor={`grade-${index}`} className="sr-only">
                        Grade for {student.firstName} {student.lastName}
                      </Label>
                      <Input
                        id={`grade-${index}`}
                        type="number"
                        min="0"
                        max="20"
                        step="0.1"
                        placeholder="0-20"
                        value={watchedGrades?.[index]?.value || 0}
                        onChange={(e) => handleGradeChange(index, e.target.value)}
                        className="w-full"
                      />
                    </div>
                    
                    <div className="md:col-span-2">
                      <Input
                        {...register(`grades.${index}.weight`)}
                        type="number"
                        min="0.1"
                        max="10"
                        step="0.1"
                        placeholder="Weight"
                        className="w-full"
                      />
                    </div>
                    
                    <div className="md:col-span-4">
                      <Input
                        {...register(`grades.${index}.comment`)}
                        placeholder="Optional comment"
                        className="w-full"
                      />
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Error Display */}
          {errors.grades && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                {errors.grades.message}
              </AlertDescription>
            </Alert>
          )}

          {/* Submit Actions */}
          <div className="flex justify-end space-x-2">
            <Button
              type="button"
              variant="outline"
              onClick={handleClose}
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isLoading}
            >
              {isLoading ? (
                <div className="flex items-center">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  Saving...
                </div>
              ) : (
                <div className="flex items-center">
                  <Save className="h-4 w-4 mr-2" />
                  Save Grades
                </div>
              )}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default BulkGradeEntryDialog;
