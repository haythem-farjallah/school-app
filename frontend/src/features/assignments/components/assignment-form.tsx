// Assignment Form Component
// Comprehensive form for creating and editing assignments

import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { format } from 'date-fns';
import { 
  Calendar,
  FileText,
  Upload,
  Info,
  BookOpen,
  Users,
  Star,
  Flag
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { Switch } from '@/components/ui/switch';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { 
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import { Calendar as CalendarComponent } from '@/components/ui/calendar';
import { cn } from '@/lib/utils';

import type { 
  Assignment, 
  CreateAssignmentRequest, 
  UpdateAssignmentRequest,
  AssignmentType,
  Priority,
  SubmissionType
} from '@/types/assignment';

// Form Schema
const assignmentFormSchema = z.object({
  title: z.string().min(1, 'Title is required').max(200, 'Title must be less than 200 characters'),
  description: z.string().min(1, 'Description is required').max(2000, 'Description must be less than 2000 characters'),
  type: z.enum(['homework', 'quiz', 'exam', 'project', 'lab', 'presentation', 'essay', 'other']),
  priority: z.enum(['low', 'medium', 'high', 'urgent']),
  courseId: z.string().min(1, 'Course is required'),
  classId: z.string().min(1, 'Class is required'),
  dueDate: z.date({ required_error: 'Due date is required' }),
  availableFrom: z.date().optional(),
  availableUntil: z.date().optional(),
  totalPoints: z.number().min(1, 'Points must be at least 1').max(1000, 'Points must be less than 1000'),
  instructions: z.string().optional(),
  submissionTypes: z.array(z.enum(['text', 'file', 'url', 'media'])).min(1, 'At least one submission type is required'),
  allowLateSubmissions: z.boolean(),
  latePenalty: z.number().min(0).max(100).optional(),
  maxLateDays: z.number().min(1).max(30).optional(),
  requiresTextSubmission: z.boolean(),
  requiresFileSubmission: z.boolean(),
  allowedFileTypes: z.array(z.string()).optional(),
  maxFileSize: z.number().min(1).max(100).optional(),
  tags: z.string().optional(),
}).refine((data) => {
  if (data.availableFrom && data.availableUntil) {
    return data.availableFrom < data.availableUntil;
  }
  return true;
}, {
  message: "Available until date must be after available from date",
  path: ["availableUntil"],
}).refine((data) => {
  if (data.availableUntil) {
    return data.availableUntil <= data.dueDate;
  }
  return true;
}, {
  message: "Available until date must be before or equal to due date",
  path: ["availableUntil"],
});

type AssignmentFormData = z.infer<typeof assignmentFormSchema>;

// Props Interface
interface AssignmentFormProps {
  assignment?: Assignment;
  courses: Array<{ id: string; name: string }>;
  classes: Array<{ id: string; name: string }>;
  onSubmit: (data: CreateAssignmentRequest | UpdateAssignmentRequest) => void;
  onCancel: () => void;
  isLoading?: boolean;
  className?: string;
}

// Configuration
const assignmentTypes: Array<{ value: AssignmentType; label: string; description: string }> = [
  { value: 'homework', label: 'Homework', description: 'Regular homework assignment' },
  { value: 'quiz', label: 'Quiz', description: 'Short assessment or quiz' },
  { value: 'exam', label: 'Exam', description: 'Major examination' },
  { value: 'project', label: 'Project', description: 'Long-term project work' },
  { value: 'lab', label: 'Lab', description: 'Laboratory or practical work' },
  { value: 'presentation', label: 'Presentation', description: 'Oral presentation' },
  { value: 'essay', label: 'Essay', description: 'Written essay or paper' },
  { value: 'other', label: 'Other', description: 'Other type of assignment' },
];

const priorities: Array<{ value: Priority; label: string; color: string }> = [
  { value: 'low', label: 'Low', color: 'text-gray-600' },
  { value: 'medium', label: 'Medium', color: 'text-yellow-600' },
  { value: 'high', label: 'High', color: 'text-orange-600' },
  { value: 'urgent', label: 'Urgent', color: 'text-red-600' },
];

const submissionTypes: Array<{ value: SubmissionType; label: string; description: string }> = [
  { value: 'text', label: 'Text Entry', description: 'Students can enter text directly' },
  { value: 'file', label: 'File Upload', description: 'Students can upload files' },
  { value: 'url', label: 'Website URL', description: 'Students can submit website links' },
  { value: 'media', label: 'Media Upload', description: 'Students can upload images, videos, audio' },
];

const commonFileTypes = [
  { value: '.pdf', label: 'PDF Documents' },
  { value: '.doc,.docx', label: 'Word Documents' },
  { value: '.txt', label: 'Text Files' },
  { value: '.jpg,.jpeg,.png', label: 'Images' },
  { value: '.mp4,.mov', label: 'Videos' },
  { value: '.mp3,.wav', label: 'Audio Files' },
  { value: '.zip,.rar', label: 'Archive Files' },
];

// Main Component
export function AssignmentForm({
  assignment,
  courses,
  classes,
  onSubmit,
  onCancel,
  isLoading = false,
  className,
}: AssignmentFormProps) {
  const isEditing = !!assignment;
  
  const form = useForm<AssignmentFormData>({
    resolver: zodResolver(assignmentFormSchema),
    defaultValues: {
      title: assignment?.title || '',
      description: assignment?.description || '',
      type: assignment?.type || 'homework',
      priority: assignment?.priority || 'medium',
      courseId: assignment?.courseId || '',
      classId: assignment?.classId || '',
      dueDate: assignment ? new Date(assignment.dueDate) : new Date(),
      availableFrom: assignment?.availableFrom ? new Date(assignment.availableFrom) : undefined,
      availableUntil: assignment?.availableUntil ? new Date(assignment.availableUntil) : undefined,
      totalPoints: assignment?.totalPoints || 100,
      instructions: assignment?.instructions || '',
      submissionTypes: assignment?.submissionTypes || ['text'],
      allowLateSubmissions: assignment?.allowLateSubmissions || false,
      latePenalty: assignment?.latePenalty || 10,
      maxLateDays: assignment?.maxLateDays || 7,
      requiresTextSubmission: assignment?.requiresTextSubmission || false,
      requiresFileSubmission: assignment?.requiresFileSubmission || false,
      allowedFileTypes: assignment?.allowedFileTypes || [],
      maxFileSize: assignment?.maxFileSize || 10,
      tags: assignment?.tags?.join(', ') || '',
    },
  });

  const { control, handleSubmit, watch, formState: { errors } } = form;
  
  // Watch form values for dynamic behavior
  const watchedSubmissionTypes = watch('submissionTypes');
  const watchedAllowLateSubmissions = watch('allowLateSubmissions');

  // Handle form submission
  const onFormSubmit = (data: AssignmentFormData) => {
    const formattedData = {
      ...data,
      tags: data.tags ? data.tags.split(',').map(tag => tag.trim()).filter(Boolean) : [],
      resources: [], // Will be handled separately
    };

    if (isEditing && assignment) {
      onSubmit({ ...formattedData, id: assignment.id } as UpdateAssignmentRequest);
    } else {
      onSubmit(formattedData as CreateAssignmentRequest);
    }
  };

  return (
    <form onSubmit={handleSubmit(onFormSubmit)} className={cn('space-y-6', className)}>
      {/* Basic Information */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <FileText className="h-5 w-5" />
            <span>Basic Information</span>
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Title */}
          <div className="space-y-2">
            <Label htmlFor="title">Assignment Title *</Label>
            <Controller
              name="title"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  id="title"
                  placeholder="Enter assignment title"
                  className={errors.title ? 'border-red-500' : ''}
                />
              )}
            />
            {errors.title && (
              <p className="text-sm text-red-600">{errors.title.message}</p>
            )}
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="description">Description *</Label>
            <Controller
              name="description"
              control={control}
              render={({ field }) => (
                <Textarea
                  {...field}
                  id="description"
                  placeholder="Describe the assignment objectives and requirements"
                  rows={4}
                  className={errors.description ? 'border-red-500' : ''}
                />
              )}
            />
            {errors.description && (
              <p className="text-sm text-red-600">{errors.description.message}</p>
            )}
          </div>

          {/* Type and Priority */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="type">Assignment Type *</Label>
              <Controller
                name="type"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select assignment type" />
                    </SelectTrigger>
                    <SelectContent>
                      {assignmentTypes.map((type) => (
                        <SelectItem key={type.value} value={type.value}>
                          <div>
                            <div className="font-medium">{type.label}</div>
                            <div className="text-xs text-gray-500">{type.description}</div>
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="priority">Priority</Label>
              <Controller
                name="priority"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select priority" />
                    </SelectTrigger>
                    <SelectContent>
                      {priorities.map((priority) => (
                        <SelectItem key={priority.value} value={priority.value}>
                          <div className="flex items-center space-x-2">
                            <Flag className={cn('h-4 w-4', priority.color)} />
                            <span>{priority.label}</span>
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
            </div>
          </div>

          {/* Course and Class */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="courseId">Course *</Label>
              <Controller
                name="courseId"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger className={errors.courseId ? 'border-red-500' : ''}>
                      <SelectValue placeholder="Select course" />
                    </SelectTrigger>
                    <SelectContent>
                      {courses.map((course) => (
                        <SelectItem key={course.id} value={course.id}>
                          <div className="flex items-center space-x-2">
                            <BookOpen className="h-4 w-4" />
                            <span>{course.name}</span>
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.courseId && (
                <p className="text-sm text-red-600">{errors.courseId.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="classId">Class *</Label>
              <Controller
                name="classId"
                control={control}
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger className={errors.classId ? 'border-red-500' : ''}>
                      <SelectValue placeholder="Select class" />
                    </SelectTrigger>
                    <SelectContent>
                      {classes.map((classItem) => (
                        <SelectItem key={classItem.id} value={classItem.id}>
                          <div className="flex items-center space-x-2">
                            <Users className="h-4 w-4" />
                            <span>{classItem.name}</span>
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.classId && (
                <p className="text-sm text-red-600">{errors.classId.message}</p>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Scheduling */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Calendar className="h-5 w-5" />
            <span>Scheduling</span>
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Due Date */}
          <div className="space-y-2">
            <Label>Due Date *</Label>
            <Controller
              name="dueDate"
              control={control}
              render={({ field }) => (
                <Popover>
                  <PopoverTrigger asChild>
                    <Button
                      variant="outline"
                      className={cn(
                        'w-full justify-start text-left font-normal',
                        !field.value && 'text-muted-foreground',
                        errors.dueDate && 'border-red-500'
                      )}
                    >
                      <Calendar className="mr-2 h-4 w-4" />
                      {field.value ? format(field.value, 'PPP') : 'Pick a date'}
                    </Button>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0" align="start">
                    <CalendarComponent
                      mode="single"
                      selected={field.value}
                      onSelect={field.onChange}
                      initialFocus
                    />
                  </PopoverContent>
                </Popover>
              )}
            />
            {errors.dueDate && (
              <p className="text-sm text-red-600">{errors.dueDate.message}</p>
            )}
          </div>

          {/* Availability Window */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Available From</Label>
              <Controller
                name="availableFrom"
                control={control}
                render={({ field }) => (
                  <Popover>
                    <PopoverTrigger asChild>
                      <Button
                        variant="outline"
                        className={cn(
                          'w-full justify-start text-left font-normal',
                          !field.value && 'text-muted-foreground'
                        )}
                      >
                        <Calendar className="mr-2 h-4 w-4" />
                        {field.value ? format(field.value, 'PPP') : 'Pick a date'}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="start">
                      <CalendarComponent
                        mode="single"
                        selected={field.value}
                        onSelect={field.onChange}
                        initialFocus
                      />
                    </PopoverContent>
                  </Popover>
                )}
              />
            </div>

            <div className="space-y-2">
              <Label>Available Until</Label>
              <Controller
                name="availableUntil"
                control={control}
                render={({ field }) => (
                  <Popover>
                    <PopoverTrigger asChild>
                      <Button
                        variant="outline"
                        className={cn(
                          'w-full justify-start text-left font-normal',
                          !field.value && 'text-muted-foreground',
                          errors.availableUntil && 'border-red-500'
                        )}
                      >
                        <Calendar className="mr-2 h-4 w-4" />
                        {field.value ? format(field.value, 'PPP') : 'Pick a date'}
                      </Button>
                    </PopoverTrigger>
                    <PopoverContent className="w-auto p-0" align="start">
                      <CalendarComponent
                        mode="single"
                        selected={field.value}
                        onSelect={field.onChange}
                        initialFocus
                      />
                    </PopoverContent>
                  </Popover>
                )}
              />
              {errors.availableUntil && (
                <p className="text-sm text-red-600">{errors.availableUntil.message}</p>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Grading */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Star className="h-5 w-5" />
            <span>Grading</span>
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="totalPoints">Total Points *</Label>
            <Controller
              name="totalPoints"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  type="number"
                  min="1"
                  max="1000"
                  onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
                  className={errors.totalPoints ? 'border-red-500' : ''}
                />
              )}
            />
            {errors.totalPoints && (
              <p className="text-sm text-red-600">{errors.totalPoints.message}</p>
            )}
          </div>

          {/* Late Submission Settings */}
          <div className="space-y-4">
            <div className="flex items-center space-x-2">
              <Controller
                name="allowLateSubmissions"
                control={control}
                render={({ field }) => (
                  <Switch
                    checked={field.value}
                    onCheckedChange={field.onChange}
                  />
                )}
              />
              <Label>Allow late submissions</Label>
            </div>

            {watchedAllowLateSubmissions && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 ml-6">
                <div className="space-y-2">
                  <Label htmlFor="latePenalty">Late Penalty (%)</Label>
                  <Controller
                    name="latePenalty"
                    control={control}
                    render={({ field }) => (
                      <Input
                        {...field}
                        type="number"
                        min="0"
                        max="100"
                        onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
                      />
                    )}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="maxLateDays">Max Late Days</Label>
                  <Controller
                    name="maxLateDays"
                    control={control}
                    render={({ field }) => (
                      <Input
                        {...field}
                        type="number"
                        min="1"
                        max="30"
                        onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
                      />
                    )}
                  />
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Submission Settings */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Upload className="h-5 w-5" />
            <span>Submission Settings</span>
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Submission Types */}
          <div className="space-y-2">
            <Label>Submission Types *</Label>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {submissionTypes.map((type) => (
                <Controller
                  key={type.value}
                  name="submissionTypes"
                  control={control}
                  render={({ field }) => (
                    <div className="flex items-start space-x-2">
                      <Checkbox
                        checked={field.value.includes(type.value)}
                        onCheckedChange={(checked) => {
                          if (checked) {
                            field.onChange([...field.value, type.value]);
                          } else {
                            field.onChange(field.value.filter((t) => t !== type.value));
                          }
                        }}
                      />
                      <div className="flex-1">
                        <Label className="text-sm font-medium">{type.label}</Label>
                        <p className="text-xs text-gray-500">{type.description}</p>
                      </div>
                    </div>
                  )}
                />
              ))}
            </div>
            {errors.submissionTypes && (
              <p className="text-sm text-red-600">{errors.submissionTypes.message}</p>
            )}
          </div>

          {/* File Upload Settings */}
          {watchedSubmissionTypes.includes('file') && (
            <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
              <h4 className="font-medium">File Upload Settings</h4>
              
              <div className="space-y-2">
                <Label htmlFor="maxFileSize">Max File Size (MB)</Label>
                <Controller
                  name="maxFileSize"
                  control={control}
                  render={({ field }) => (
                    <Input
                      {...field}
                      type="number"
                      min="1"
                      max="100"
                      onChange={(e) => field.onChange(parseInt(e.target.value) || 0)}
                    />
                  )}
                />
              </div>

              <div className="space-y-2">
                <Label>Allowed File Types</Label>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                  {commonFileTypes.map((fileType) => (
                    <Controller
                      key={fileType.value}
                      name="allowedFileTypes"
                      control={control}
                      render={({ field }) => (
                        <div className="flex items-center space-x-2">
                          <Checkbox
                            checked={field.value?.includes(fileType.value) || false}
                            onCheckedChange={(checked) => {
                              const currentTypes = field.value || [];
                              if (checked) {
                                field.onChange([...currentTypes, fileType.value]);
                              } else {
                                field.onChange(currentTypes.filter((t) => t !== fileType.value));
                              }
                            }}
                          />
                          <Label className="text-sm">{fileType.label}</Label>
                        </div>
                      )}
                    />
                  ))}
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Instructions and Tags */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <Info className="h-5 w-5" />
            <span>Additional Information</span>
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Instructions */}
          <div className="space-y-2">
            <Label htmlFor="instructions">Detailed Instructions</Label>
            <Controller
              name="instructions"
              control={control}
              render={({ field }) => (
                <Textarea
                  {...field}
                  id="instructions"
                  placeholder="Provide detailed instructions for students"
                  rows={6}
                />
              )}
            />
          </div>

          {/* Tags */}
          <div className="space-y-2">
            <Label htmlFor="tags">Tags</Label>
            <Controller
              name="tags"
              control={control}
              render={({ field }) => (
                <Input
                  {...field}
                  id="tags"
                  placeholder="Enter tags separated by commas (e.g., math, algebra, homework)"
                />
              )}
            />
            <p className="text-xs text-gray-500">
              Tags help organize and filter assignments. Separate multiple tags with commas.
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Form Actions */}
      <div className="flex items-center justify-end space-x-4 pt-6">
        <Button
          type="button"
          variant="outline"
          onClick={onCancel}
          disabled={isLoading}
        >
          Cancel
        </Button>
        <Button
          type="submit"
          disabled={isLoading}
          className="min-w-[120px]"
        >
          {isLoading ? (
            <div className="flex items-center space-x-2">
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              <span>{isEditing ? 'Updating...' : 'Creating...'}</span>
            </div>
          ) : (
            <span>{isEditing ? 'Update Assignment' : 'Create Assignment'}</span>
          )}
        </Button>
      </div>
    </form>
  );
}

export default AssignmentForm;
