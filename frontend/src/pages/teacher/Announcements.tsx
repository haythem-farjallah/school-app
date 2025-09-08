import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { motion } from 'framer-motion';
import { 
  Megaphone, 
  Plus, 
  Send, 
  Users, 
  Calendar, 
  Edit, 
  Trash2,
  Search,
  MoreVertical
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, DropdownMenuSeparator } from '@/components/ui/dropdown-menu';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Checkbox } from '@/components/ui/checkbox';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { formatDistanceToNow } from 'date-fns';
import toast from 'react-hot-toast';
import { useAnnouncements, useCreateAnnouncement, useDeleteAnnouncement } from '../../features/announcements/hooks/use-announcements';
import { useAllTeacherClasses } from '@/hooks/useTeacherClasses';
import { CreateAnnouncementRequest } from '../../types/announcement';

const TeacherAnnouncements = () => {
  const { t } = useTranslation();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [importanceFilter, setImportanceFilter] = useState('all');
  
  // API hooks
  const { data: announcementsData, isLoading } = useAnnouncements({ page: 0, size: 50 });
  const { data: teacherClasses, isLoading: isLoadingClasses, error: classesError } = useAllTeacherClasses();
  const createAnnouncementMutation = useCreateAnnouncement();
  const deleteAnnouncementMutation = useDeleteAnnouncement();

  // Debug logging
  console.log('Announcements Debug:', { 
    announcementsData, 
    isLoading, 
    content: (announcementsData as any)?.content,
    contentLength: (announcementsData as any)?.content?.length 
  });
  
  console.log('Teacher Classes Debug:', { 
    teacherClasses, 
    isLoadingClasses, 
    classesError,
    isArray: Array.isArray(teacherClasses),
    length: teacherClasses?.length 
  });

  const [newAnnouncement, setNewAnnouncement] = useState<CreateAnnouncementRequest>({
    title: '',
    body: '',
    isPublic: true, // Temporarily set to true for testing
    importance: 'MEDIUM',
    targetType: 'CLASSES',
    targetClassIds: [],
    sendNotifications: true
  });

  const announcements = (announcementsData as any)?.content || [];

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'URGENT':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'HIGH':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'MEDIUM':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'LOW':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const handleCreateAnnouncement = async () => {
    if (!newAnnouncement.title || !newAnnouncement.body) {
      toast.error('Please fill in all required fields');
      return;
    }

    if (newAnnouncement.targetType === 'CLASSES' && (!newAnnouncement.targetClassIds || newAnnouncement.targetClassIds.length === 0)) {
      toast.error('Please select at least one class');
      return;
    }

    try {
      await createAnnouncementMutation.mutateAsync(newAnnouncement);
      setIsCreateDialogOpen(false);
      setNewAnnouncement({
        title: '',
        body: '',
        isPublic: false,
        importance: 'MEDIUM',
        targetType: 'CLASSES',
        targetClassIds: [],
        sendNotifications: true
      });
    } catch (error) {
      // Error is handled by the mutation
    }
  };

  const handleDeleteAnnouncement = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this announcement?')) {
      await deleteAnnouncementMutation.mutateAsync(id);
    }
  };

  const filteredAnnouncements = announcements.filter((announcement: any) => {
    const matchesSearch = announcement.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         announcement.body.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesImportance = importanceFilter === 'all' || announcement.importance === importanceFilter;
    
    return matchesSearch && matchesImportance;
  });

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-2">
            <Megaphone className="h-8 w-8 text-blue-600" />
            {t('Announcements')}
          </h1>
          <p className="text-gray-600 mt-1">Send announcements to your classes</p>
        </div>
        
        <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
          <DialogTrigger asChild>
            <Button className="bg-blue-600 hover:bg-blue-700">
              <Plus className="h-4 w-4 mr-2" />
              Create Announcement
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto bg-white border shadow-lg">
            <DialogHeader>
              <DialogTitle className="text-gray-900 text-xl font-semibold">Create New Announcement</DialogTitle>
              <DialogDescription className="text-gray-600">
                Send an announcement to students in your classes
              </DialogDescription>
            </DialogHeader>
            
            <div className="space-y-6">
              <div>
                <Label htmlFor="title" className="text-base font-medium mb-2 block text-gray-900">Title *</Label>
                <Input
                  id="title"
                  value={newAnnouncement.title}
                  onChange={(e) => setNewAnnouncement(prev => ({ ...prev, title: e.target.value }))}
                  placeholder="Enter announcement title..."
                  className="bg-white border-gray-300 text-gray-900"
                />
              </div>
              
              <div>
                <Label htmlFor="content" className="text-base font-medium mb-2 block text-gray-900">Content *</Label>
                <Textarea
                  id="content"
                  value={newAnnouncement.body}
                  onChange={(e) => setNewAnnouncement(prev => ({ ...prev, body: e.target.value }))}
                  placeholder="Enter announcement content..."
                  rows={4}
                  className="bg-white border-gray-300 text-gray-900"
                />
              </div>
              
              <div>
                <Label htmlFor="priority" className="text-base font-medium mb-2 block text-gray-900">Priority</Label>
                <Select
                  value={newAnnouncement.importance}
                  onValueChange={(value: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT') => 
                    setNewAnnouncement(prev => ({ ...prev, importance: value }))
                  }
                >
                  <SelectTrigger className="bg-white border-gray-300 text-gray-900">
                    <SelectValue placeholder="Select priority" />
                  </SelectTrigger>
                  <SelectContent className="bg-white border-gray-300">
                    <SelectItem value="LOW">Low Priority</SelectItem>
                    <SelectItem value="MEDIUM">Medium Priority</SelectItem>
                    <SelectItem value="HIGH">High Priority</SelectItem>
                    <SelectItem value="URGENT">Urgent</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div>
                <div className="flex items-center justify-between mb-3">
                  <Label className="text-base font-medium text-gray-900">Select Classes *</Label>
                  {newAnnouncement.targetClassIds && newAnnouncement.targetClassIds.length > 0 && (
                    <span className="text-sm text-blue-600 bg-blue-50 px-2 py-1 rounded-md">
                      {newAnnouncement.targetClassIds.length} selected
                    </span>
                  )}
                </div>
                {isLoadingClasses ? (
                  <div className="text-gray-600 p-4 text-center bg-gray-50 rounded-md border border-gray-200">
                    Loading classes...
                  </div>
                ) : teacherClasses && Array.isArray(teacherClasses) && teacherClasses.length > 0 ? (
                  <div className="space-y-3 max-h-40 overflow-y-auto p-4 bg-gray-50 rounded-md border border-gray-200">
                    {teacherClasses.map(classItem => (
                      <div key={classItem.id} className="flex items-center space-x-3 p-3 rounded-md hover:bg-gray-100 transition-colors border border-gray-200 bg-white">
                        <Checkbox
                          id={`class-${classItem.id}`}
                          checked={newAnnouncement.targetClassIds?.includes(classItem.id) || false}
                          onCheckedChange={(checked) => {
                            console.log('Checkbox changed:', { classId: classItem.id, checked, currentIds: newAnnouncement.targetClassIds });
                            if (checked) {
                              setNewAnnouncement(prev => ({
                                ...prev,
                                targetClassIds: [...(prev.targetClassIds || []), classItem.id]
                              }));
                            } else {
                              setNewAnnouncement(prev => ({
                                ...prev,
                                targetClassIds: prev.targetClassIds?.filter(id => id !== classItem.id) || []
                              }));
                            }
                          }}
                        />
                        <Label htmlFor={`class-${classItem.id}`} className="text-sm font-medium text-gray-900 cursor-pointer flex-1">
                          <div className="flex flex-col">
                            <span className="font-semibold text-gray-900">{classItem.name}</span>
                            <span className="text-xs text-gray-600">
                              Grade {classItem.grade} • {classItem.enrolled}/{classItem.capacity} students • {classItem.courses?.length || 0} courses
                            </span>
                          </div>
                        </Label>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="text-gray-600 p-4 text-center bg-gray-50 rounded-md border border-gray-200">
                    <p className="mb-2">No classes assigned to you</p>
                    <p className="text-xs text-gray-500">Contact your administrator to assign classes to your account.</p>
                  </div>
                )}
              </div>
              

            </div>

            <DialogFooter className="gap-2">
              <Button 
                variant="outline" 
                onClick={() => setIsCreateDialogOpen(false)}
                className="bg-white border-gray-300 text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </Button>
              <Button 
                onClick={handleCreateAnnouncement}
                disabled={
                  createAnnouncementMutation.isPending || 
                  !newAnnouncement.title || 
                  !newAnnouncement.body ||
                  !newAnnouncement.targetClassIds ||
                  newAnnouncement.targetClassIds.length === 0
                }
                className="bg-blue-600 hover:bg-blue-700 text-white disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Send className="h-4 w-4 mr-2" />
                {createAnnouncementMutation.isPending ? 'Publishing...' : 'Publish Announcement'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="Search announcements..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <Select value={importanceFilter} onValueChange={setImportanceFilter}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="Priority" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Priorities</SelectItem>
                <SelectItem value="URGENT">Urgent</SelectItem>
                <SelectItem value="HIGH">High</SelectItem>
                <SelectItem value="MEDIUM">Medium</SelectItem>
                <SelectItem value="LOW">Low</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Announcements List */}
      <div className="space-y-4">
        {isLoading ? (
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <p className="text-gray-500 mt-2">Loading announcements...</p>
          </div>
        ) : filteredAnnouncements.length === 0 ? (
          <Card className="text-center py-12">
            <CardContent>
              <Megaphone className="h-12 w-12 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No announcements found</h3>
              <p className="text-gray-500 mb-4">
                {searchQuery || importanceFilter !== 'all'
                  ? 'Try adjusting your filters to see more results.'
                  : 'Create your first announcement to get started.'
                }
              </p>
              {!searchQuery && importanceFilter === 'all' && (
                <Button onClick={() => setIsCreateDialogOpen(true)}>
                  <Plus className="h-4 w-4 mr-2" />
                  Create Announcement
                </Button>
              )}
            </CardContent>
          </Card>
        ) : (
          filteredAnnouncements.map((announcement: any) => (
            <Card key={announcement.id} className="hover:shadow-lg transition-all duration-200">
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className="text-lg line-clamp-1">{announcement.title}</CardTitle>
                    <div className="flex items-center gap-2 mt-2">
                      <Badge className={getPriorityColor(announcement.importance)}>
                        {announcement.importance}
                      </Badge>
                      {announcement.isPublic && (
                        <Badge variant="secondary">Public</Badge>
                      )}
                    </div>
                  </div>
                  
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="sm">
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem>
                        <Edit className="h-4 w-4 mr-2" />
                        Edit
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem 
                        onClick={() => handleDeleteAnnouncement(announcement.id)}
                        className="text-red-600 focus:text-red-600"
                        disabled={deleteAnnouncementMutation.isPending}
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        Delete
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </CardHeader>
              
              <CardContent>
                <p className="text-gray-600 line-clamp-3 mb-4">{announcement.body}</p>
                
                {/* Target Classes Information */}
                {announcement.targetType === 'CLASSES' && announcement.targetClassNames && announcement.targetClassNames.length > 0 && (
                  <div className="mb-3">
                    <div className="flex items-center gap-2 text-sm text-blue-600">
                      <Users className="h-3 w-3" />
                      <span className="font-medium">Target Classes:</span>
                      <div className="flex flex-wrap gap-1">
                        {announcement.targetClassNames.map((className, index) => (
                          <Badge key={index} variant="outline" className="text-xs">
                            {className}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </div>
                )}
                
                {/* Target Type for non-class announcements */}
                {announcement.targetType && announcement.targetType !== 'CLASSES' && (
                  <div className="mb-3">
                    <div className="flex items-center gap-2 text-sm text-green-600">
                      <Users className="h-3 w-3" />
                      <span className="font-medium">Target:</span>
                      <Badge variant="outline" className="text-xs">
                        {announcement.targetType.replace('_', ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                      </Badge>
                    </div>
                  </div>
                )}
                
                <div className="flex items-center justify-between text-sm text-gray-500">
                  <div className="flex items-center gap-4">
                    <div className="flex items-center gap-1">
                      <Calendar className="h-3 w-3" />
                      {formatDistanceToNow(new Date(announcement.createdAt), { addSuffix: true })}
                    </div>
                    {announcement.createdByName && (
                      <div className="flex items-center gap-1">
                        <Users className="h-3 w-3" />
                        <span>by {announcement.createdByName}</span>
                      </div>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>
    </div>
  );
};

export default TeacherAnnouncements; 