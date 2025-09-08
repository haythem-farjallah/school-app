// Assignment Card Component
// Reusable card component for displaying assignment information

import { format, formatDistanceToNow, isPast, isToday, isTomorrow } from 'date-fns';
import { 
  Calendar, 
  Clock, 
  Users, 
  FileText, 
  CheckCircle, 
  AlertCircle, 
  XCircle,
  Eye,
  Edit,
  Trash2,
  MoreHorizontal,
  BookOpen,
  GraduationCap,
  Star,
  Flag
} from 'lucide-react';

import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { 
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Progress } from '@/components/ui/progress';
import { cn } from '@/lib/utils';

import type { Assignment, AssignmentType, AssignmentStatus, Priority } from '@/types/assignment';

// Type and Status Configuration
const typeConfig: Record<AssignmentType, { icon: React.ComponentType<{ className?: string }>; color: string; label: string }> = {
  homework: { icon: FileText, color: 'bg-blue-500', label: 'Homework' },
  quiz: { icon: CheckCircle, color: 'bg-green-500', label: 'Quiz' },
  exam: { icon: AlertCircle, color: 'bg-red-500', label: 'Exam' },
  project: { icon: Star, color: 'bg-purple-500', label: 'Project' },
  lab: { icon: GraduationCap, color: 'bg-orange-500', label: 'Lab' },
  presentation: { icon: Users, color: 'bg-indigo-500', label: 'Presentation' },
  essay: { icon: BookOpen, color: 'bg-pink-500', label: 'Essay' },
  other: { icon: FileText, color: 'bg-gray-500', label: 'Other' },
};

const statusConfig: Record<AssignmentStatus, { color: string; label: string }> = {
  draft: { color: 'bg-gray-100 text-gray-800', label: 'Draft' },
  published: { color: 'bg-blue-100 text-blue-800', label: 'Published' },
  active: { color: 'bg-green-100 text-green-800', label: 'Active' },
  closed: { color: 'bg-red-100 text-red-800', label: 'Closed' },
  archived: { color: 'bg-gray-100 text-gray-600', label: 'Archived' },
};

const priorityConfig: Record<Priority, { color: string; icon: React.ComponentType<{ className?: string }> }> = {
  low: { color: 'text-gray-500', icon: Flag },
  medium: { color: 'text-yellow-500', icon: Flag },
  high: { color: 'text-orange-500', icon: Flag },
  urgent: { color: 'text-red-500', icon: Flag },
};

// Props Interface
interface AssignmentCardProps {
  assignment: Assignment;
  variant?: 'default' | 'compact' | 'detailed';
  showActions?: boolean;
  showProgress?: boolean;
  showTeacher?: boolean;
  showCourse?: boolean;
  className?: string;
  onView?: (assignment: Assignment) => void;
  onEdit?: (assignment: Assignment) => void;
  onDelete?: (assignment: Assignment) => void;
  onPublish?: (assignment: Assignment) => void;
  onClose?: (assignment: Assignment) => void;
  onArchive?: (assignment: Assignment) => void;
}

// Utility Functions
function getDueDateStatus(dueDate: Date) {
  const now = new Date();
  
  if (isPast(dueDate)) {
    return { status: 'overdue', color: 'text-red-600', label: 'Overdue' };
  }
  
  if (isToday(dueDate)) {
    return { status: 'today', color: 'text-orange-600', label: 'Due Today' };
  }
  
  if (isTomorrow(dueDate)) {
    return { status: 'tomorrow', color: 'text-yellow-600', label: 'Due Tomorrow' };
  }
  
  const daysUntilDue = Math.ceil((dueDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
  
  if (daysUntilDue <= 7) {
    return { status: 'soon', color: 'text-yellow-600', label: `Due in ${daysUntilDue} days` };
  }
  
  return { status: 'future', color: 'text-gray-600', label: formatDistanceToNow(dueDate, { addSuffix: true }) };
}

function getSubmissionProgress(assignment: Assignment) {
  if (assignment.totalStudents === 0) return 0;
  return Math.round((assignment.submissionCount / assignment.totalStudents) * 100);
}

function getGradingProgress(assignment: Assignment) {
  if (assignment.submissionCount === 0) return 0;
  return Math.round((assignment.gradedCount / assignment.submissionCount) * 100);
}

// Main Component
export function AssignmentCard({
  assignment,
  variant = 'default',
  showActions = true,
  showProgress = true,
  showTeacher = false,
  showCourse = true,
  className,
  onView,
  onEdit,
  onDelete,
  onPublish,
  onClose,
  onArchive,
}: AssignmentCardProps) {
  const typeInfo = typeConfig[assignment.type];
  const statusInfo = statusConfig[assignment.status];
  const priorityInfo = priorityConfig[assignment.priority];
  const dueDateStatus = getDueDateStatus(new Date(assignment.dueDate));
  const submissionProgress = getSubmissionProgress(assignment);
  const gradingProgress = getGradingProgress(assignment);

  const TypeIcon = typeInfo.icon;
  const PriorityIcon = priorityInfo.icon;

  // Compact variant
  if (variant === 'compact') {
    return (
      <Card className={cn('hover:shadow-md transition-shadow duration-200', className)}>
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3 flex-1 min-w-0">
              <div className={cn('w-8 h-8 rounded-full flex items-center justify-center', typeInfo.color)}>
                <TypeIcon className="h-4 w-4 text-white" />
              </div>
              
              <div className="flex-1 min-w-0">
                <h3 className="font-medium text-sm truncate">{assignment.title}</h3>
                <div className="flex items-center space-x-2 text-xs text-gray-500 mt-1">
                  <span>{assignment.courseName}</span>
                  <span>•</span>
                  <span className={dueDateStatus.color}>{dueDateStatus.label}</span>
                </div>
              </div>
            </div>
            
            <div className="flex items-center space-x-2">
              <Badge variant="secondary" className={cn('text-xs', statusInfo.color)}>
                {statusInfo.label}
              </Badge>
              
              {showActions && (
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    {onView && (
                      <DropdownMenuItem onClick={() => onView(assignment)}>
                        <Eye className="mr-2 h-4 w-4" />
                        View
                      </DropdownMenuItem>
                    )}
                    {onEdit && (
                      <DropdownMenuItem onClick={() => onEdit(assignment)}>
                        <Edit className="mr-2 h-4 w-4" />
                        Edit
                      </DropdownMenuItem>
                    )}
                    <DropdownMenuSeparator />
                    {onDelete && (
                      <DropdownMenuItem 
                        onClick={() => onDelete(assignment)}
                        className="text-red-600 focus:text-red-600"
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        Delete
                      </DropdownMenuItem>
                    )}
                  </DropdownMenuContent>
                </DropdownMenu>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  // Default and Detailed variants
  return (
    <Card className={cn(
      'hover:shadow-lg transition-all duration-200 border-l-4',
      typeInfo.color.replace('bg-', 'border-l-'),
      className
    )}>
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex items-start space-x-3 flex-1 min-w-0">
            <div className={cn('w-10 h-10 rounded-lg flex items-center justify-center', typeInfo.color)}>
              <TypeIcon className="h-5 w-5 text-white" />
            </div>
            
            <div className="flex-1 min-w-0">
              <div className="flex items-center space-x-2 mb-1">
                <h3 className="font-semibold text-lg truncate">{assignment.title}</h3>
                {assignment.priority !== 'low' && (
                  <PriorityIcon className={cn('h-4 w-4', priorityInfo.color)} />
                )}
              </div>
              
              <div className="flex items-center space-x-4 text-sm text-gray-600">
                {showCourse && (
                  <div className="flex items-center space-x-1">
                    <BookOpen className="h-4 w-4" />
                    <span>{assignment.courseName}</span>
                  </div>
                )}
                
                <div className="flex items-center space-x-1">
                  <Users className="h-4 w-4" />
                  <span>{assignment.className}</span>
                </div>
                
                {showTeacher && (
                  <div className="flex items-center space-x-1">
                    <GraduationCap className="h-4 w-4" />
                    <span>{assignment.teacherName}</span>
                  </div>
                )}
              </div>
            </div>
          </div>
          
          <div className="flex items-center space-x-2">
            <Badge variant="secondary" className={cn('text-xs', statusInfo.color)}>
              {statusInfo.label}
            </Badge>
            
            {showActions && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                    <MoreHorizontal className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  {onView && (
                    <DropdownMenuItem onClick={() => onView(assignment)}>
                      <Eye className="mr-2 h-4 w-4" />
                      View Details
                    </DropdownMenuItem>
                  )}
                  {onEdit && (
                    <DropdownMenuItem onClick={() => onEdit(assignment)}>
                      <Edit className="mr-2 h-4 w-4" />
                      Edit Assignment
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuSeparator />
                  {assignment.status === 'draft' && onPublish && (
                    <DropdownMenuItem onClick={() => onPublish(assignment)}>
                      <CheckCircle className="mr-2 h-4 w-4" />
                      Publish
                    </DropdownMenuItem>
                  )}
                  {assignment.status === 'active' && onClose && (
                    <DropdownMenuItem onClick={() => onClose(assignment)}>
                      <XCircle className="mr-2 h-4 w-4" />
                      Close
                    </DropdownMenuItem>
                  )}
                  {onArchive && (
                    <DropdownMenuItem onClick={() => onArchive(assignment)}>
                      <FileText className="mr-2 h-4 w-4" />
                      Archive
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuSeparator />
                  {onDelete && (
                    <DropdownMenuItem 
                      onClick={() => onDelete(assignment)}
                      className="text-red-600 focus:text-red-600"
                    >
                      <Trash2 className="mr-2 h-4 w-4" />
                      Delete
                    </DropdownMenuItem>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0">
        {/* Description */}
        {assignment.description && (
          <p className="text-gray-600 text-sm mb-4 line-clamp-2">
            {assignment.description}
          </p>
        )}
        
        {/* Due Date and Points */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-4 text-sm">
            <div className="flex items-center space-x-1">
              <Calendar className="h-4 w-4 text-gray-400" />
              <span className="text-gray-600">Due:</span>
              <span className={cn('font-medium', dueDateStatus.color)}>
                {format(new Date(assignment.dueDate), 'MMM d, yyyy')}
              </span>
            </div>
            
            <div className="flex items-center space-x-1">
              <Star className="h-4 w-4 text-gray-400" />
              <span className="text-gray-600">{assignment.totalPoints} points</span>
            </div>
          </div>
          
          <div className={cn('text-sm font-medium', dueDateStatus.color)}>
            {dueDateStatus.label}
          </div>
        </div>
        
        {/* Progress Indicators */}
        {showProgress && variant === 'detailed' && (
          <div className="space-y-3">
            <div>
              <div className="flex items-center justify-between text-sm mb-1">
                <span className="text-gray-600">Submissions</span>
                <span className="font-medium">
                  {assignment.submissionCount} / {assignment.totalStudents} ({submissionProgress}%)
                </span>
              </div>
              <Progress value={submissionProgress} className="h-2" />
            </div>
            
            {assignment.submissionCount > 0 && (
              <div>
                <div className="flex items-center justify-between text-sm mb-1">
                  <span className="text-gray-600">Graded</span>
                  <span className="font-medium">
                    {assignment.gradedCount} / {assignment.submissionCount} ({gradingProgress}%)
                  </span>
                </div>
                <Progress value={gradingProgress} className="h-2" />
              </div>
            )}
          </div>
        )}
        
        {/* Tags */}
        {assignment.tags && assignment.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-3">
            {assignment.tags.slice(0, 3).map((tag) => (
              <Badge key={tag} variant="outline" className="text-xs">
                {tag}
              </Badge>
            ))}
            {assignment.tags.length > 3 && (
              <Badge variant="outline" className="text-xs">
                +{assignment.tags.length - 3} more
              </Badge>
            )}
          </div>
        )}
        
        {/* Footer with timestamps */}
        <div className="flex items-center justify-between text-xs text-gray-500 mt-4 pt-3 border-t">
          <div className="flex items-center space-x-1">
            <Clock className="h-3 w-3" />
            <span>Created {formatDistanceToNow(new Date(assignment.createdAt), { addSuffix: true })}</span>
          </div>
          
          {assignment.averageScore && (
            <div className="flex items-center space-x-1">
              <span>Avg: {assignment.averageScore.toFixed(1)}%</span>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

// Export variants for specific use cases
export const CompactAssignmentCard = (props: Omit<AssignmentCardProps, 'variant'>) => (
  <AssignmentCard {...props} variant="compact" />
);

export const DetailedAssignmentCard = (props: Omit<AssignmentCardProps, 'variant'>) => (
  <AssignmentCard {...props} variant="detailed" />
);

export default AssignmentCard;
