import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../../../components/ui/dialog';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { Label } from '../../../components/ui/label';
import { Textarea } from '../../../components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../../components/ui/select';
import { AttendanceDto, AttendanceStatus, UserType } from '../../../types/attendance';
import { useCreateAttendance, useUpdateAttendance } from '../hooks/use-attendance';
import { useAllTeacherClasses } from '../../../hooks/useTeacherClasses';
import { Calendar, User, MapPin, Clock, FileText, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const attendanceFormSchema = z.object({
  userId: z.number().min(1, 'Student is required'),
  date: z.string().min(1, 'Date is required'),
  status: z.nativeEnum(AttendanceStatus, { required_error: 'Status is required' }),
  classId: z.number().optional(),
  courseId: z.number().optional(),
  timetableSlotId: z.number().optional(),
  remarks: z.string().max(500, 'Remarks must be less than 500 characters').optional(),
  excuse: z.string().max(500, 'Excuse must be less than 500 characters').optional(),
  medicalNote: z.string().max(1000, 'Medical note must be less than 1000 characters').optional(),
});

type AttendanceFormData = z.infer<typeof attendanceFormSchema>;

interface AttendanceSheetProps {
  isOpen: boolean;
  onClose: () => void;
  attendance?: AttendanceDto;
  mode: 'create' | 'edit';
  onSuccess?: () => void;
}

export function AttendanceSheet({ 
  isOpen, 
  onClose, 
  attendance, 
  mode,
  onSuccess 
}: AttendanceSheetProps) {
  const createAttendanceMutation = useCreateAttendance();
  const updateAttendanceMutation = useUpdateAttendance();
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const { data: teacherClasses } = useAllTeacherClasses();

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    setValue,
    watch,
  } = useForm<AttendanceFormData>({
    resolver: zodResolver(attendanceFormSchema),
    defaultValues: attendance
      ? {
          userId: attendance.userId,
          date: attendance.date,
          status: attendance.status,
          classId: attendance.classId,
          courseId: attendance.courseId,
          timetableSlotId: attendance.timetableSlotId,
          remarks: attendance.remarks || '',
          excuse: attendance.excuse || '',
          medicalNote: attendance.medicalNote || '',
        }
      : {
          userId: 0,
          date: new Date().toISOString().split('T')[0],
          status: AttendanceStatus.PRESENT,
          remarks: '',
          excuse: '',
          medicalNote: '',
        },
  });

  const watchedStatus = watch('status');
  const watchedDate = watch('date');

  const handleClose = () => {
    reset();
    onClose();
  };

  const onSubmit = async (data: AttendanceFormData) => {
    setIsSubmitting(true);
    try {
      const attendanceData = {
        ...data,
        userType: UserType.STUDENT,
      };

      if (mode === 'create') {
        await createAttendanceMutation.mutateAsync(attendanceData);
        toast.success('Attendance recorded successfully');
      } else if (attendance) {
        await updateAttendanceMutation.mutateAsync({
          id: attendance.id!,
          data: attendanceData,
        });
        toast.success('Attendance updated successfully');
      }
      
      onSuccess?.();
      handleClose();
    } catch (error) {
      console.error(
        mode === 'create' ? 'Failed to create attendance' : 'Failed to update attendance',
        error
      );
      toast.error(
        mode === 'create' ? 'Failed to record attendance' : 'Failed to update attendance'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  // Reset form when attendance prop changes
  useEffect(() => {
    if (attendance && mode === 'edit') {
      reset({
        userId: attendance.userId,
        date: attendance.date,
        status: attendance.status,
        classId: attendance.classId,
        courseId: attendance.courseId,
        timetableSlotId: attendance.timetableSlotId,
        remarks: attendance.remarks || '',
        excuse: attendance.excuse || '',
        medicalNote: attendance.medicalNote || '',
      });
    } else if (mode === 'create') {
      reset({
        userId: 0,
        date: new Date().toISOString().split('T')[0],
        status: AttendanceStatus.PRESENT,
        remarks: '',
        excuse: '',
        medicalNote: '',
      });
    }
  }, [attendance, mode, reset]);

  const getStatusColor = (status: AttendanceStatus) => {
    switch (status) {
      case AttendanceStatus.PRESENT:
        return 'text-green-600 bg-green-50 border-green-200';
      case AttendanceStatus.ABSENT:
        return 'text-red-600 bg-red-50 border-red-200';
      case AttendanceStatus.LATE:
        return 'text-yellow-600 bg-yellow-50 border-yellow-200';
      case AttendanceStatus.EXCUSED:
        return 'text-blue-600 bg-blue-50 border-blue-200';
      default:
        return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <User className="h-5 w-5" />
            {mode === 'create' ? 'Record Attendance' : 'Edit Attendance'}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Status Preview */}
          <div className={`p-4 rounded-lg border ${getStatusColor(watchedStatus)}`}>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="font-medium">Status: {watchedStatus}</div>
              </div>
              <div className="text-sm">
                Date: {new Date(watchedDate || '').toLocaleDateString()}
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Student Selection */}
            <div className="space-y-2">
              <Label htmlFor="userId" className="flex items-center gap-2">
                <User className="h-4 w-4" />
                Student ID
              </Label>
              <Input
                id="userId"
                type="number"
                placeholder="Enter student ID"
                {...register('userId', { valueAsNumber: true })}
              />
              {errors.userId && (
                <p className="text-sm text-red-600">{errors.userId.message}</p>
              )}
            </div>

            {/* Date */}
            <div className="space-y-2">
              <Label htmlFor="date" className="flex items-center gap-2">
                <Calendar className="h-4 w-4" />
                Date
              </Label>
              <Input
                id="date"
                type="date"
                {...register('date')}
              />
              {errors.date && (
                <p className="text-sm text-red-600">{errors.date.message}</p>
              )}
            </div>

            {/* Status */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4" />
                Status
              </Label>
              <Select onValueChange={(value) => setValue('status', value as AttendanceStatus)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={AttendanceStatus.PRESENT}>Present</SelectItem>
                  <SelectItem value={AttendanceStatus.ABSENT}>Absent</SelectItem>
                  <SelectItem value={AttendanceStatus.LATE}>Late</SelectItem>
                  <SelectItem value={AttendanceStatus.EXCUSED}>Excused</SelectItem>
                </SelectContent>
              </Select>
              {errors.status && (
                <p className="text-sm text-red-600">{errors.status.message}</p>
              )}
            </div>

            {/* Class Selection */}
            <div className="space-y-2">
              <Label className="flex items-center gap-2">
                <MapPin className="h-4 w-4" />
                Class (Optional)
              </Label>
              <Select onValueChange={(value) => setValue('classId', parseInt(value))}>
                <SelectTrigger>
                  <SelectValue placeholder="Select class" />
                </SelectTrigger>
                <SelectContent>
                  {teacherClasses?.map((cls) => (
                    <SelectItem key={cls.id} value={cls.id.toString()}>
                      {cls.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Remarks */}
          <div className="space-y-2">
            <Label htmlFor="remarks" className="flex items-center gap-2">
              <FileText className="h-4 w-4" />
              Remarks
            </Label>
            <Textarea
              id="remarks"
              placeholder="Optional remarks about attendance..."
              {...register('remarks')}
              rows={2}
            />
            {errors.remarks && (
              <p className="text-sm text-red-600">{errors.remarks.message}</p>
            )}
          </div>

          {/* Excuse (only show if absent or excused) */}
          {(watchedStatus === AttendanceStatus.ABSENT || watchedStatus === AttendanceStatus.EXCUSED) && (
            <div className="space-y-2">
              <Label htmlFor="excuse" className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4" />
                Excuse/Reason
              </Label>
              <Textarea
                id="excuse"
                placeholder="Reason for absence or excuse..."
                {...register('excuse')}
                rows={2}
              />
              {errors.excuse && (
                <p className="text-sm text-red-600">{errors.excuse.message}</p>
              )}
            </div>
          )}

          {/* Medical Note (only show if excused) */}
          {watchedStatus === AttendanceStatus.EXCUSED && (
            <div className="space-y-2">
              <Label htmlFor="medicalNote" className="flex items-center gap-2">
                <Clock className="h-4 w-4" />
                Medical Note
              </Label>
              <Textarea
                id="medicalNote"
                placeholder="Medical note or documentation..."
                {...register('medicalNote')}
                rows={3}
              />
              {errors.medicalNote && (
                <p className="text-sm text-red-600">{errors.medicalNote.message}</p>
              )}
            </div>
          )}

          {/* Form Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <Button 
              type="button" 
              variant="outline" 
              onClick={handleClose}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button 
              type="submit"
              disabled={isSubmitting}
              className="bg-blue-600 hover:bg-blue-700"
            >
              {isSubmitting 
                ? (mode === 'create' ? 'Recording...' : 'Updating...') 
                : (mode === 'create' ? 'Record Attendance' : 'Update Attendance')
              }
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
