import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { Input } from '../../../components/ui/input';
import { 
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '../../../components/ui/dialog';
import { 
  Clock, 
  Plus,
  Edit,
  Trash2,
  Save,
  RefreshCw,
  AlertTriangle
} from 'lucide-react';
import { usePeriods } from '../hooks';
import { Period } from '../../../types/period';
import { http } from '../../../lib/http';
import toast from 'react-hot-toast';

export function PeriodManagement() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingPeriod, setEditingPeriod] = useState<Period | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [formData, setFormData] = useState({
    index: '',
    startTime: '',
    endTime: ''
  });

  const { data: periods, isLoading: periodsLoading, refetch } = usePeriods();

  // Sort periods by index
  const sortedPeriods = periods ? [...periods].sort((a, b) => a.index - b.index) : [];

  // Check for duplicate periods
  const duplicates = periods ? periods.reduce((acc, period, index, arr) => {
    const duplicateIndex = arr.findIndex((p, i) => i !== index && p.index === period.index);
    if (duplicateIndex !== -1 && !acc.includes(period.index)) {
      acc.push(period.index);
    }
    return acc;
  }, [] as number[]) : [];

  const handleEdit = (period: Period) => {
    setEditingPeriod(period);
    setFormData({
      index: period.index.toString(),
      startTime: period.startTime,
      endTime: period.endTime
    });
    setIsDialogOpen(true);
  };

  const handleCreate = () => {
    setEditingPeriod(null);
    setFormData({
      index: '',
      startTime: '',
      endTime: ''
    });
    setIsDialogOpen(true);
  };

  const handleSave = async () => {
    if (!formData.index || !formData.startTime || !formData.endTime) {
      toast.error('Please fill in all fields');
      return;
    }

    setIsLoading(true);
    try {
      const periodData = {
        index: parseInt(formData.index),
        startTime: formData.startTime,
        endTime: formData.endTime
      };

      if (editingPeriod) {
        await http.put(`/v1/periods/${editingPeriod.id}`, periodData);
        toast.success('Period updated successfully');
      } else {
        await http.post('/v1/periods', periodData);
        toast.success('Period created successfully');
      }

      await refetch();
      setIsDialogOpen(false);
    } catch (error) {
      console.error('Error saving period:', error);
      toast.error('Failed to save period');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (periodId: number) => {
    if (!confirm('Are you sure you want to delete this period? This may affect existing timetable slots.')) {
      return;
    }

    setIsLoading(true);
    try {
      await http.delete(`/v1/periods/${periodId}`);
      toast.success('Period deleted successfully');
      await refetch();
    } catch (error) {
      console.error('Error deleting period:', error);
      toast.error('Failed to delete period');
    } finally {
      setIsLoading(false);
    }
  };

  const handleFixDuplicates = async () => {
    if (!confirm('This will remove duplicate periods. Are you sure?')) {
      return;
    }

    setIsLoading(true);
    try {
      await http.post('/v1/periods/fix-duplicates');
      toast.success('Duplicate periods fixed successfully');
      await refetch();
    } catch (error) {
      console.error('Error fixing duplicates:', error);
      toast.error('Failed to fix duplicate periods');
    } finally {
      setIsLoading(false);
    }
  };

  if (periodsLoading) {
    return (
      <Card>
        <CardContent className="p-8">
          <div className="flex items-center justify-center">
            <RefreshCw className="w-6 h-6 animate-spin mr-2" />
            <span>Loading periods...</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Period Management</h2>
          <p className="text-gray-600">
            Manage time periods for the school timetable
          </p>
        </div>
        <div className="flex gap-2">
          <Button onClick={() => refetch()} variant="outline" disabled={isLoading}>
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
          <Button onClick={handleCreate} disabled={isLoading}>
            <Plus className="w-4 h-4 mr-2" />
            Add Period
          </Button>
        </div>
      </div>

      {/* Duplicate Warning */}
      {duplicates.length > 0 && (
        <Card className="border-orange-200 bg-orange-50">
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-orange-600" />
                <div>
                  <p className="font-medium text-orange-800">Duplicate Periods Detected</p>
                  <p className="text-sm text-orange-700">
                    Found duplicate periods with indices: {duplicates.join(', ')}
                  </p>
                </div>
              </div>
              <Button 
                onClick={handleFixDuplicates} 
                variant="outline" 
                className="border-orange-300 text-orange-700 hover:bg-orange-100"
                disabled={isLoading}
              >
                Fix Duplicates
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Periods List */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Clock className="w-5 h-5" />
            Time Periods ({sortedPeriods.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            {sortedPeriods.map(period => (
              <div
                key={period.id}
                className={`
                  flex items-center justify-between p-4 border rounded-lg
                  ${duplicates.includes(period.index) 
                    ? 'border-orange-300 bg-orange-50' 
                    : 'border-gray-200 hover:bg-gray-50'
                  }
                `}
              >
                <div className="flex items-center gap-4">
                  <Badge variant={duplicates.includes(period.index) ? "destructive" : "secondary"}>
                    Period {period.index}
                  </Badge>
                  <div>
                    <p className="font-medium">
                      {period.startTime} - {period.endTime}
                    </p>
                    <p className="text-sm text-gray-600">
                      Duration: {
                        Math.round(
                          (new Date(`1970-01-01T${period.endTime}`).getTime() - 
                           new Date(`1970-01-01T${period.startTime}`).getTime()) / 60000
                        )
                      } minutes
                    </p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleEdit(period)}
                    disabled={isLoading}
                  >
                    <Edit className="w-4 h-4" />
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="text-red-600 hover:text-red-700"
                    onClick={() => handleDelete(period.id)}
                    disabled={isLoading}
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            ))}

            {sortedPeriods.length === 0 && (
              <div className="text-center py-8 text-gray-500">
                <Clock className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>No periods found. Create your first period to get started.</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Edit/Create Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {editingPeriod ? 'Edit Period' : 'Create Period'}
            </DialogTitle>
            <DialogDescription>
              Configure the time period details.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium text-gray-700 mb-2 block">
                Period Index
              </label>
              <Input
                type="number"
                value={formData.index}
                onChange={(e) => setFormData(prev => ({ ...prev, index: e.target.value }))}
                placeholder="e.g., 1"
                min="1"
              />
            </div>

            <div>
              <label className="text-sm font-medium text-gray-700 mb-2 block">
                Start Time
              </label>
              <Input
                type="time"
                value={formData.startTime}
                onChange={(e) => setFormData(prev => ({ ...prev, startTime: e.target.value }))}
              />
            </div>

            <div>
              <label className="text-sm font-medium text-gray-700 mb-2 block">
                End Time
              </label>
              <Input
                type="time"
                value={formData.endTime}
                onChange={(e) => setFormData(prev => ({ ...prev, endTime: e.target.value }))}
              />
            </div>

            <div className="flex gap-2 pt-4">
              <Button
                onClick={handleSave}
                disabled={isLoading}
                className="flex-1"
              >
                {isLoading ? (
                  <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                ) : (
                  <Save className="w-4 h-4 mr-2" />
                )}
                {editingPeriod ? 'Update' : 'Create'}
              </Button>
              <Button
                onClick={() => setIsDialogOpen(false)}
                variant="outline"
                disabled={isLoading}
              >
                Cancel
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
