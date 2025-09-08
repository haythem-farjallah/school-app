// Assignment List Component
// Comprehensive list component with filtering, sorting, and actions

import React, { useState } from 'react';
import { 
  Search,
  Filter,
  SortAsc,
  SortDesc,
  Grid,
  List,
  Plus,
  Download,
  Upload,
  MoreHorizontal,
  Calendar,
  Clock,
  Users,
  BookOpen,
  Star,
  Flag,
  Eye,
  Edit,
  Trash2,
  CheckCircle,
  XCircle,
  Archive
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { 
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  DropdownMenuCheckboxItem,
  DropdownMenuLabel,
} from '@/components/ui/dropdown-menu';
import { Checkbox } from '@/components/ui/checkbox';
import { Separator } from '@/components/ui/separator';
import { cn } from '@/lib/utils';
import { getRoleClasses } from '@/lib/theme';

import { AssignmentCard, CompactAssignmentCard } from './assignment-card';
import type { 
  Assignment, 
  AssignmentFilters, 
  AssignmentType, 
  AssignmentStatus, 
  Priority 
} from '@/types/assignment';

// Props Interface
interface AssignmentListProps {
  assignments: Assignment[];
  totalCount: number;
  isLoading?: boolean;
  filters?: AssignmentFilters;
  onFiltersChange?: (filters: AssignmentFilters) => void;
  onAssignmentSelect?: (assignment: Assignment) => void;
  onAssignmentView?: (assignment: Assignment) => void;
  onAssignmentEdit?: (assignment: Assignment) => void;
  onAssignmentDelete?: (assignment: Assignment) => void;
  onAssignmentPublish?: (assignment: Assignment) => void;
  onAssignmentClose?: (assignment: Assignment) => void;
  onAssignmentArchive?: (assignment: Assignment) => void;
  onBulkAction?: (action: string, assignmentIds: string[]) => void;
  onCreateNew?: () => void;
  onExport?: () => void;
  onImport?: () => void;
  showBulkActions?: boolean;
  showCreateButton?: boolean;
  showFilters?: boolean;
  className?: string;
}

// Sort options
const sortOptions = [
  { value: 'dueDate_asc', label: 'Due Date (Earliest First)', icon: SortAsc },
  { value: 'dueDate_desc', label: 'Due Date (Latest First)', icon: SortDesc },
  { value: 'created_desc', label: 'Recently Created', icon: SortDesc },
  { value: 'created_asc', label: 'Oldest First', icon: SortAsc },
  { value: 'title_asc', label: 'Title (A-Z)', icon: SortAsc },
  { value: 'title_desc', label: 'Title (Z-A)', icon: SortDesc },
  { value: 'priority_desc', label: 'Priority (High to Low)', icon: SortDesc },
  { value: 'submissions_desc', label: 'Most Submissions', icon: SortDesc },
];

// Filter options
const typeOptions: Array<{ value: AssignmentType; label: string }> = [
  { value: 'homework', label: 'Homework' },
  { value: 'quiz', label: 'Quiz' },
  { value: 'exam', label: 'Exam' },
  { value: 'project', label: 'Project' },
  { value: 'lab', label: 'Lab' },
  { value: 'presentation', label: 'Presentation' },
  { value: 'essay', label: 'Essay' },
  { value: 'other', label: 'Other' },
];

const statusOptions: Array<{ value: AssignmentStatus; label: string }> = [
  { value: 'draft', label: 'Draft' },
  { value: 'published', label: 'Published' },
  { value: 'active', label: 'Active' },
  { value: 'closed', label: 'Closed' },
  { value: 'archived', label: 'Archived' },
];

const priorityOptions: Array<{ value: Priority; label: string }> = [
  { value: 'low', label: 'Low' },
  { value: 'medium', label: 'Medium' },
  { value: 'high', label: 'High' },
  { value: 'urgent', label: 'Urgent' },
];

// Main Component
export function AssignmentList({
  assignments,
  totalCount,
  isLoading = false,
  filters = {},
  onFiltersChange,
  onAssignmentSelect,
  onAssignmentView,
  onAssignmentEdit,
  onAssignmentDelete,
  onAssignmentPublish,
  onAssignmentClose,
  onAssignmentArchive,
  onBulkAction,
  onCreateNew,
  onExport,
  onImport,
  showBulkActions = true,
  showCreateButton = true,
  showFilters = true,
  className,
}: AssignmentListProps) {
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [selectedAssignments, setSelectedAssignments] = useState<string[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('dueDate_asc');
  const roleClasses = getRoleClasses('TEACHER'); // Assuming teacher context for now

  // Handle selection
  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedAssignments(assignments.map(a => a.id));
    } else {
      setSelectedAssignments([]);
    }
  };

  const handleSelectAssignment = (assignmentId: string, checked: boolean) => {
    if (checked) {
      setSelectedAssignments([...selectedAssignments, assignmentId]);
    } else {
      setSelectedAssignments(selectedAssignments.filter(id => id !== assignmentId));
    }
  };

  // Handle bulk actions
  const handleBulkAction = (action: string) => {
    if (onBulkAction && selectedAssignments.length > 0) {
      onBulkAction(action, selectedAssignments);
      setSelectedAssignments([]);
    }
  };

  // Filter assignments based on search query
  const filteredAssignments = assignments.filter(assignment => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return (
      assignment.title.toLowerCase().includes(query) ||
      assignment.description.toLowerCase().includes(query) ||
      assignment.courseName.toLowerCase().includes(query) ||
      assignment.className.toLowerCase().includes(query) ||
      assignment.teacherName.toLowerCase().includes(query) ||
      assignment.tags?.some(tag => tag.toLowerCase().includes(query))
    );
  });

  // Sort assignments
  const sortedAssignments = [...filteredAssignments].sort((a, b) => {
    switch (sortBy) {
      case 'dueDate_asc':
        return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
      case 'dueDate_desc':
        return new Date(b.dueDate).getTime() - new Date(a.dueDate).getTime();
      case 'created_desc':
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      case 'created_asc':
        return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      case 'title_asc':
        return a.title.localeCompare(b.title);
      case 'title_desc':
        return b.title.localeCompare(a.title);
      case 'priority_desc':
        const priorityOrder = { urgent: 4, high: 3, medium: 2, low: 1 };
        return priorityOrder[b.priority] - priorityOrder[a.priority];
      case 'submissions_desc':
        return b.submissionCount - a.submissionCount;
      default:
        return 0;
    }
  });

  return (
    <div className={cn('space-y-6', className)}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Assignments</h2>
          <p className="text-gray-600">
            {totalCount} assignment{totalCount !== 1 ? 's' : ''} total
            {filteredAssignments.length !== totalCount && (
              <span> • {filteredAssignments.length} shown</span>
            )}
          </p>
        </div>

        <div className="flex items-center space-x-2">
          {onImport && (
            <Button variant="outline" onClick={onImport}>
              <Upload className="mr-2 h-4 w-4" />
              Import
            </Button>
          )}
          
          {onExport && (
            <Button variant="outline" onClick={onExport}>
              <Download className="mr-2 h-4 w-4" />
              Export
            </Button>
          )}
          
          {showCreateButton && onCreateNew && (
            <Button onClick={onCreateNew}>
              <Plus className="mr-2 h-4 w-4" />
              Create Assignment
            </Button>
          )}
        </div>
      </div>

      {/* Filters and Controls */}
      {showFilters && (
        <Card>
          <CardContent className="p-4">
            <div className="flex flex-col lg:flex-row gap-4">
              {/* Search */}
              <div className="flex-1">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <Input
                    placeholder="Search assignments..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-10"
                  />
                </div>
              </div>

              {/* Sort */}
              <Select value={sortBy} onValueChange={setSortBy}>
                <SelectTrigger className="w-full lg:w-[200px]">
                  <SelectValue placeholder="Sort by" />
                </SelectTrigger>
                <SelectContent>
                  {sortOptions.map((option) => {
                    const Icon = option.icon;
                    return (
                      <SelectItem key={option.value} value={option.value}>
                        <div className="flex items-center space-x-2">
                          <Icon className="h-4 w-4" />
                          <span>{option.label}</span>
                        </div>
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>

              {/* Filters */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" className={`border-gray-300 hover:${roleClasses.primaryLight}`}>
                    <Filter className="mr-2 h-4 w-4" />
                    Filters
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent className="w-56">
                  <DropdownMenuLabel>Filter by Status</DropdownMenuLabel>
                  {statusOptions.map((status) => (
                    <DropdownMenuCheckboxItem
                      key={status.value}
                      checked={filters.status?.includes(status.value) || false}
                      onCheckedChange={(checked) => {
                        if (onFiltersChange) {
                          const currentStatus = filters.status || [];
                          const newStatus = checked
                            ? [...currentStatus, status.value]
                            : currentStatus.filter(s => s !== status.value);
                          onFiltersChange({ ...filters, status: newStatus });
                        }
                      }}
                    >
                      {status.label}
                    </DropdownMenuCheckboxItem>
                  ))}
                  
                  <DropdownMenuSeparator />
                  
                  <DropdownMenuLabel>Filter by Type</DropdownMenuLabel>
                  {typeOptions.map((type) => (
                    <DropdownMenuCheckboxItem
                      key={type.value}
                      checked={filters.type?.includes(type.value) || false}
                      onCheckedChange={(checked) => {
                        if (onFiltersChange) {
                          const currentType = filters.type || [];
                          const newType = checked
                            ? [...currentType, type.value]
                            : currentType.filter(t => t !== type.value);
                          onFiltersChange({ ...filters, type: newType });
                        }
                      }}
                    >
                      {type.label}
                    </DropdownMenuCheckboxItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>

              {/* View Mode */}
              <div className="flex items-center border rounded-md">
                <Button
                  variant={viewMode === 'grid' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setViewMode('grid')}
                  className={`rounded-r-none ${viewMode === 'grid' ? roleClasses.button : ''}`}
                >
                  <Grid className="h-4 w-4" />
                </Button>
                <Button
                  variant={viewMode === 'list' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setViewMode('list')}
                  className={`rounded-l-none ${viewMode === 'list' ? roleClasses.button : ''}`}
                >
                  <List className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Bulk Actions */}
      {showBulkActions && selectedAssignments.length > 0 && (
        <Card className={`${roleClasses.primaryLight} ${roleClasses.primaryBorder}`}>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <Checkbox
                  checked={selectedAssignments.length === assignments.length}
                  onCheckedChange={handleSelectAll}
                  className={`data-[state=checked]:${roleClasses.primaryBg}`}
                />
                <span className="text-sm font-medium">
                  {selectedAssignments.length} assignment{selectedAssignments.length !== 1 ? 's' : ''} selected
                </span>
              </div>

              <div className="flex items-center space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBulkAction('publish')}
                >
                  <CheckCircle className="mr-2 h-4 w-4" />
                  Publish
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBulkAction('close')}
                >
                  <XCircle className="mr-2 h-4 w-4" />
                  Close
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBulkAction('archive')}
                >
                  <Archive className="mr-2 h-4 w-4" />
                  Archive
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBulkAction('delete')}
                  className="text-red-600 hover:text-red-700"
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  Delete
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Assignment List */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {Array.from({ length: 6 }).map((_, index) => (
            <Card key={index} className="animate-pulse">
              <CardHeader>
                <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                <div className="h-3 bg-gray-200 rounded w-1/2"></div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  <div className="h-3 bg-gray-200 rounded"></div>
                  <div className="h-3 bg-gray-200 rounded w-2/3"></div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : sortedAssignments.length === 0 ? (
        <Card>
          <CardContent className="p-12 text-center">
            <BookOpen className="h-12 w-12 text-gray-400 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No assignments found</h3>
            <p className="text-gray-600 mb-4">
              {searchQuery || Object.keys(filters).length > 0
                ? 'Try adjusting your search or filters'
                : 'Get started by creating your first assignment'
              }
            </p>
            {showCreateButton && onCreateNew && (
              <Button onClick={onCreateNew}>
                <Plus className="mr-2 h-4 w-4" />
                Create Assignment
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className={cn(
          viewMode === 'grid'
            ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6'
            : 'space-y-4'
        )}>
          {sortedAssignments.map((assignment) => {
            const isSelected = selectedAssignments.includes(assignment.id);
            
            const cardProps = {
              assignment,
              onView: onAssignmentView,
              onEdit: onAssignmentEdit,
              onDelete: onAssignmentDelete,
              onPublish: onAssignmentPublish,
              onClose: onAssignmentClose,
              onArchive: onAssignmentArchive,
              className: cn(
                'cursor-pointer transition-all duration-200',
                isSelected && `ring-2 ${roleClasses.primary.replace('text-','ring-')} ring-offset-2`
              ),
            };

            return (
              <div key={assignment.id} className="relative">
                {showBulkActions && (
                  <div className="absolute top-3 left-3 z-10">
                    <Checkbox
                      checked={isSelected}
                      onCheckedChange={(checked) => 
                        handleSelectAssignment(assignment.id, checked as boolean)
                      }
                      className="bg-white shadow-sm"
                    />
                  </div>
                )}
                
                <div
                  onClick={() => onAssignmentSelect?.(assignment)}
                  className={cn(showBulkActions && 'pl-8')}
                >
                  {viewMode === 'grid' ? (
                    <AssignmentCard {...cardProps} variant="detailed" />
                  ) : (
                    <CompactAssignmentCard {...cardProps} />
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default AssignmentList;
