import { useState, useMemo } from 'react';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { DataTable } from '../../../components/data-table/data-table';
import { DataTableToolbar } from '../../../components/data-table/data-table-toolbar';
import { useDataTable } from '../../../hooks/use-data-table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../../components/ui/select';
import { Plus, Search, GraduationCap, TrendingUp, Calculator, BarChart3 } from 'lucide-react';
import { Grade } from '../../../types/grade';
import { useGrades, useDeleteGrade } from '../hooks/use-grades';
import { createGradeColumns } from './grade-columns';
import { GradeSheet } from './grade-sheet';
import { calculatePercentage, getGradeLevel, getGradeLevelBadgeColor } from '../../../types/grade';

export function GradesTable() {
  const [search, setSearch] = useState('');
  const [courseFilter, setCourseFilter] = useState('all');
  const [isSheetOpen, setIsSheetOpen] = useState(false);
  const [selectedGrade, setSelectedGrade] = useState<Grade | undefined>();
  const [sheetMode, setSheetMode] = useState<'create' | 'edit'>('create');

  const {
    data,
    refetch,
  } = useGrades({ 
    search,
    courseId: courseFilter !== 'all' ? parseInt(courseFilter) : undefined,
  });


  const deleteGradeMutation = useDeleteGrade();

  const handleCreateGrade = () => {
    setSelectedGrade(undefined);
    setSheetMode('create');
    setIsSheetOpen(true);
  };

  const handleEditGrade = (grade: Grade) => {
    setSelectedGrade(grade);
    setSheetMode('edit');
    setIsSheetOpen(true);
  };

  const handleViewGrade = (grade: Grade) => {
    setSelectedGrade(grade);
    setSheetMode('edit');
    setIsSheetOpen(true);
  };

  const handleDeleteGrade = async (gradeId: number) => {
    if (window.confirm('Are you sure you want to delete this grade?')) {
      try {
        await deleteGradeMutation.mutateAsync(gradeId);
        refetch();
      } catch {
        console.error('Failed to delete grade');
      }
    }
  };

  const columns = useMemo(
    () => createGradeColumns({
      onEdit: handleEditGrade,
      onDelete: handleDeleteGrade,
      onView: handleViewGrade,
    }),
    [handleEditGrade, handleDeleteGrade, handleViewGrade]
  );

  const grades = data?.data || [];
  const totalElements = data?.totalItems || 0;
  const totalPages = data?.totalPages || 0;

  const { table } = useDataTable({
    data: grades,
    columns,
    pageCount: totalPages,
    enableAdvancedFilter: false,
  });

  // Calculate grade distribution
  const gradeDistribution = useMemo(() => {
    const distribution = { A: 0, B: 0, C: 0, D: 0, F: 0 };
    grades.forEach(grade => {
      const percentage = calculatePercentage(grade.score, grade.maxScore);
      const level = getGradeLevel(percentage);
      distribution[level as keyof typeof distribution]++;
    });
    return distribution;
  }, [grades]);

  // Calculate average score
  const averageScore = useMemo(() => {
    if (grades.length === 0) return 0;
    const total = grades.reduce((sum, grade) => {
      return sum + calculatePercentage(grade.score, grade.maxScore);
    }, 0);
    return total / grades.length;
  }, [grades]);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Grade Management</h2>
          <p className="text-gray-600">
            Manage student grades, track academic performance, and generate reports
          </p>
        </div>
        <Button onClick={handleCreateGrade} className="bg-purple-600 hover:bg-purple-700">
          <Plus className="w-4 h-4 mr-2" />
          Add Grade
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <div className="bg-white p-4 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <GraduationCap className="w-8 h-8 text-purple-600" />
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-600">Total Grades</p>
              <p className="text-xl font-bold text-gray-900">{totalElements}</p>
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <Calculator className="w-8 h-8 text-green-600" />
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-600">Average Score</p>
              <p className="text-xl font-bold text-gray-900">{averageScore ? averageScore.toFixed(1) : '0.0'}%</p>
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <TrendingUp className="w-8 h-8 text-blue-600" />
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-600">A Grades</p>
              <p className="text-xl font-bold text-gray-900">{gradeDistribution.A}</p>
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <BarChart3 className="w-8 h-8 text-yellow-600" />
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-600">B Grades</p>
              <p className="text-xl font-bold text-gray-900">{gradeDistribution.B}</p>
            </div>
          </div>
        </div>
        <div className="bg-white p-4 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <BarChart3 className="w-8 h-8 text-orange-600" />
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-600">C & Below</p>
              <p className="text-xl font-bold text-gray-900">
                {gradeDistribution.C + gradeDistribution.D + gradeDistribution.F}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Grade Distribution Chart */}
      <div className="bg-white p-4 rounded-lg shadow-sm border">
        <h3 className="text-lg font-semibold mb-4">Grade Distribution</h3>
        <div className="flex items-center space-x-4">
          {Object.entries(gradeDistribution).map(([grade, count]) => (
            <div key={grade} className="flex-1 text-center">
              <div className="text-2xl font-bold" style={{ color: getGradeLevelBadgeColor(grade === 'A' ? 95 : grade === 'B' ? 85 : grade === 'C' ? 75 : grade === 'D' ? 65 : 55).replace('bg-', 'text-').replace('-100', '-600') }}>
                {grade}
              </div>
              <div className="text-sm text-gray-600">{count} grades</div>
              <div className="text-xs text-gray-400">
                {totalElements > 0 ? ((count / totalElements) * 100).toFixed(1) : 0}%
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-lg shadow-sm border">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                placeholder="Search grades..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>
          <Select value={courseFilter} onValueChange={setCourseFilter}>
            <SelectTrigger className="w-full sm:w-48">
              <SelectValue placeholder="Filter by course" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Courses</SelectItem>
              <SelectItem value="1">Mathematics</SelectItem>
              <SelectItem value="2">Physics</SelectItem>
              <SelectItem value="3">English</SelectItem>
            </SelectContent>
          </Select>
          {(search || courseFilter !== 'all') && (
            <Button
              variant="outline"
              onClick={() => {
                setSearch('');
                setCourseFilter('all');
              }}
            >
              Clear Filters
            </Button>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg shadow-sm border">
        <DataTable table={table}>
          <DataTableToolbar table={table} />
        </DataTable>
      </div>

      {/* Grade Sheet */}
      <GradeSheet
        isOpen={isSheetOpen}
        onClose={() => setIsSheetOpen(false)}
        grade={selectedGrade}
        mode={sheetMode}
      />
    </div>
  );
} 