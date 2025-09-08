import { useState } from 'react';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../../../components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../../components/ui/select';
import { Textarea } from '../../../components/ui/textarea';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Grade } from '../../../types/grade';
import { gradeFormSchema, GradeFormData, calculatePercentage, getGradeLevel } from '../gradeForm.definition';
import { getGradeLevelColor } from '../../../types/grade';
import { useCreateGrade, useUpdateGrade } from '../hooks/use-grades';

interface GradeSheetProps {
  isOpen: boolean;
  onClose: () => void;
  grade?: Grade;
  mode: 'create' | 'edit';
}

export function GradeSheet({ isOpen, onClose, grade, mode }: GradeSheetProps) {
  const createGradeMutation = useCreateGrade();
  const updateGradeMutation = useUpdateGrade();
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    setValue,
    watch,
  } = useForm<GradeFormData>({
    resolver: zodResolver(gradeFormSchema),
    defaultValues: grade
      ? {
          enrollmentId: grade.enrollment.id,
          courseId: grade.course.id,
          score: grade.score,
          maxScore: grade.maxScore,
          weight: grade.weight,
          content: grade.content,
          description: grade.description,
        }
      : {
          enrollmentId: 1,
          courseId: 1,
          score: 0,
          maxScore: 100,
          weight: 1.0,
          content: '',
          description: '',
        },
  });

  const watchedScore = watch('score');
  const watchedMaxScore = watch('maxScore');
  const percentage = calculatePercentage(watchedScore || 0, watchedMaxScore || 100);
  const gradeLevel = getGradeLevel(percentage);
  const gradeColor = getGradeLevelColor(percentage);

  const handleClose = () => {
    reset();
    onClose();
  };

  const onSubmit = async (data: GradeFormData) => {
    setIsSubmitting(true);
    try {
      if (mode === 'create') {
        await createGradeMutation.mutateAsync(data);
        console.log('Grade created successfully');
      } else if (grade) {
        await updateGradeMutation.mutateAsync({
          id: grade.id,
          data: data,
        });
        console.log('Grade updated successfully');
      }
      handleClose();
    } catch {
      console.error(
        mode === 'create' ? 'Failed to create grade' : 'Failed to update grade'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto bg-white">
        <DialogHeader className="pb-4">
          <DialogTitle className="text-gray-900 text-xl">
            {mode === 'create' ? 'Create New Grade' : 'Edit Grade'}
          </DialogTitle>
          <DialogDescription className="text-gray-600">
            {mode === 'create'
              ? 'Add a new grade for a student'
              : 'Update grade information'}
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto p-4">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6 bg-white p-6 rounded-lg border border-gray-200 shadow-lg">
            <div className="space-y-2">
              <Label htmlFor="enrollmentId" className="text-gray-700 font-medium">Student Enrollment</Label>
              <Select
                value={watch('enrollmentId')?.toString()}
                onValueChange={(value) => setValue('enrollmentId', parseInt(value))}
              >
              <SelectTrigger>
                <SelectValue placeholder="Select student enrollment" />
              </SelectTrigger>
              <SelectContent>
                {/* Will be populated with actual enrollment data */}
                <SelectItem value="1">John Doe - Class 10A</SelectItem>
                <SelectItem value="2">Jane Smith - Class 10B</SelectItem>
              </SelectContent>
            </Select>
            {errors.enrollmentId && (
              <p className="text-sm text-red-500">{errors.enrollmentId.message}</p>
            )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="courseId" className="text-gray-700 font-medium">Course</Label>
              <Select
                value={watch('courseId')?.toString()}
                onValueChange={(value) => setValue('courseId', parseInt(value))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select course" />
                </SelectTrigger>
                <SelectContent>
                  {/* Will be populated with actual course data */}
                  <SelectItem value="1">Mathematics</SelectItem>
                  <SelectItem value="2">Physics</SelectItem>
                  <SelectItem value="3">English</SelectItem>
                </SelectContent>
              </Select>
              {errors.courseId && (
                <p className="text-sm text-red-500">{errors.courseId.message}</p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="score" className="text-gray-700 font-medium">Score</Label>
                <Input
                  id="score"
                  type="number"
                  step="0.01"
                  {...register('score', { valueAsNumber: true })}
                  placeholder="Enter score"
                />
                {errors.score && (
                  <p className="text-sm text-red-500">{errors.score.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="maxScore" className="text-gray-700 font-medium">Maximum Score</Label>
                <Input
                  id="maxScore"
                  type="number"
                  step="0.01"
                  {...register('maxScore', { valueAsNumber: true })}
                  placeholder="Enter max score"
                />
                {errors.maxScore && (
                  <p className="text-sm text-red-500">{errors.maxScore.message}</p>
                )}
              </div>
            </div>

            {/* Grade Preview */}
            {watchedScore > 0 && watchedMaxScore > 0 && (
              <div className="p-3 bg-gray-50 rounded-lg">
                <div className="text-center">
                  <div className="text-2xl font-bold" style={{ color: gradeColor }}>
                    {gradeLevel}
                  </div>
                  <div className="text-sm text-gray-600">
                    {percentage ? percentage.toFixed(1) : '0.0'}% ({watchedScore}/{watchedMaxScore})
                  </div>
                </div>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="weight" className="text-gray-700 font-medium">Weight</Label>
              <Input
                id="weight"
                type="number"
                step="0.1"
                min="0.1"
                max="10"
                {...register('weight', { valueAsNumber: true })}
                placeholder="Enter weight (e.g., 1.0)"
              />
              {errors.weight && (
                <p className="text-sm text-red-500">{errors.weight.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="content" className="text-gray-700 font-medium">Grade Content</Label>
              <Input
                id="content"
                {...register('content')}
                placeholder="e.g., Midterm Exam, Final Project"
              />
              {errors.content && (
                <p className="text-sm text-red-500">{errors.content.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="description" className="text-gray-700 font-medium">Description (Optional)</Label>
              <Textarea
                id="description"
                {...register('description')}
                placeholder="Optional description or notes"
              />
              {errors.description && (
                <p className="text-sm text-red-500">{errors.description.message}</p>
              )}
            </div>

            <div className="flex justify-end space-x-2 pt-4">
              <Button type="button" variant="outline" onClick={handleClose}>
                Cancel
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting
                  ? mode === 'create'
                    ? 'Creating...'
                    : 'Updating...'
                  : mode === 'create'
                  ? 'Create Grade'
                  : 'Update Grade'}
              </Button>
            </div>
          </form>
        </div>
      </DialogContent>
    </Dialog>
  );
} 