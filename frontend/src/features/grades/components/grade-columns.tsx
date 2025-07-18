import { ColumnDef } from '@tanstack/react-table';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../../components/ui/dropdown-menu';
import { Edit, MoreHorizontal, Trash2, Eye } from 'lucide-react';
import { Grade } from '../../../types/grade';
import { calculatePercentage, getGradeLevel, getGradeLevelBadgeColor } from '../../../types/grade';

interface GradeColumnsProps {
  onEdit: (grade: Grade) => void;
  onDelete: (gradeId: number) => void;
  onView: (grade: Grade) => void;
}

export const createGradeColumns = ({
  onEdit,
  onDelete,
  onView,
}: GradeColumnsProps): ColumnDef<Grade>[] => [
  {
    accessorKey: 'enrollment.student',
    header: 'Student',
    cell: ({ row }) => {
      const student = row.original.enrollment.student;
      return (
        <div>
          <div className="font-medium">{`${student.firstName} ${student.lastName}`}</div>
          <div className="text-sm text-muted-foreground">{student.email}</div>
        </div>
      );
    },
  },
  {
    accessorKey: 'course',
    header: 'Course',
    cell: ({ row }) => {
      const course = row.original.course;
      return (
        <div>
          <div className="font-medium">{course.name}</div>
          <div className="text-sm text-muted-foreground">Credit: {course.credit}</div>
        </div>
      );
    },
  },
  {
    accessorKey: 'content',
    header: 'Grade Content',
    cell: ({ row }) => (
      <div className="max-w-32 truncate" title={row.original.content}>
        {row.original.content}
      </div>
    ),
  },
  {
    accessorKey: 'score',
    header: 'Score',
    cell: ({ row }) => {
      const grade = row.original;
      const percentage = calculatePercentage(grade.score, grade.maxScore);
      const gradeLevel = getGradeLevel(percentage);
      const badgeColor = getGradeLevelBadgeColor(percentage);
      
      return (
        <div className="text-center">
          <div className="flex items-center justify-center space-x-2">
            <span className="font-medium">{grade.score}</span>
            <span className="text-muted-foreground">/</span>
            <span className="text-muted-foreground">{grade.maxScore}</span>
          </div>
          <Badge className={`mt-1 ${badgeColor}`}>
            {gradeLevel} ({percentage ? percentage.toFixed(1) : '0.0'}%)
          </Badge>
        </div>
      );
    },
  },
  {
    accessorKey: 'weight',
    header: 'Weight',
    cell: ({ row }) => (
      <div className="text-center">
        <Badge variant="outline" className="font-medium">
          {row.original.weight}x
        </Badge>
      </div>
    ),
  },
  {
    accessorKey: 'teacher',
    header: 'Teacher',
    cell: ({ row }) => {
      const teacher = row.original.teacher;
      return (
        <div>
          <div className="font-medium">{`${teacher.firstName} ${teacher.lastName}`}</div>
          <div className="text-sm text-muted-foreground">{teacher.email}</div>
        </div>
      );
    },
  },
  {
    accessorKey: 'enrollment.class',
    header: 'Class',
    cell: ({ row }) => (
      <div className="font-medium">{row.original.enrollment.class.name}</div>
    ),
  },
  {
    accessorKey: 'gradedAt',
    header: 'Graded Date',
    cell: ({ row }) => (
      <div className="text-sm text-muted-foreground">
        {new Date(row.original.gradedAt).toLocaleDateString()}
      </div>
    ),
  },
  {
    id: 'actions',
    header: 'Actions',
    cell: ({ row }) => {
      const grade = row.original;
      return (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="h-8 w-8 p-0">
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => onView(grade)}>
              <Eye className="mr-2 h-4 w-4" />
              View Details
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onEdit(grade)}>
              <Edit className="mr-2 h-4 w-4" />
              Edit Grade
            </DropdownMenuItem>
            <DropdownMenuItem 
              onClick={() => onDelete(grade.id)}
              className="text-red-600 hover:text-red-700"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      );
    },
  },
]; 