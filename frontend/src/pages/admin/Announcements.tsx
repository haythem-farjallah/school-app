import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { 
  Megaphone, 
  Plus, 
  Send, 
  Users, 
  Calendar, 
  Eye, 
  Edit, 
  Trash2,
  Target,
  Clock,
  AlertTriangle,
  Info,
  CheckCircle,
  Filter,
  Search,
  MoreVertical,
  Bell,
  Mail,
  MessageSquare,
  Smartphone,
  School,
  UserCheck,
  GraduationCap,
  Shield
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
import { CreateAnnouncementRequest } from '../../types/announcement';

const Announcements = () => {
  const { t } = useTranslation();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [importanceFilter, setImportanceFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  
  // API hooks
  const { data: announcementsData, isLoading } = useAnnouncements({ page: 0, size: 50 });
  const createAnnouncementMutation = useCreateAnnouncement();
  const deleteAnnouncementMutation = useDeleteAnnouncement();

  const [newAnnouncement, setNewAnnouncement] = useState<CreateAnnouncementRequest>({
    title: '',
    body: '',
    isPublic: true,
    importance: 'MEDIUM',
    targetType: 'WHOLE_SCHOOL',
    sendNotifications: true
  });

  const announcements = announcementsData?.content || [];

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

  const getTargetIcon = (targetType: string) => {
    switch (targetType) {
      case 'WHOLE_SCHOOL':
        return <School className="h-4 w-4" />;
      case 'ALL_STAFF':
        return <Shield className="h-4 w-4" />;
      case 'ALL_TEACHERS':
        return <GraduationCap className="h-4 w-4" />;
      case 'ALL_STUDENTS':
        return <UserCheck className="h-4 w-4" />;
      default:
        return <Users className="h-4 w-4" />;
    }
  };

  const handleCreateAnnouncement = async () => {
    if (!newAnnouncement.title || !newAnnouncement.body) {
      toast.error('Please fill in all required fields');
      return;
    }

    try {
      await createAnnouncementMutation.mutateAsync(newAnnouncement);
      setIsCreateDialogOpen(false);
      setNewAnnouncement({
        title: '',
        body: '',
        isPublic: true,
        importance: 'MEDIUM',
        targetType: 'WHOLE_SCHOOL',
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

  const filteredAnnouncements = announcements.filter(announcement => {
    const matchesSearch = announcement.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         announcement.body.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesImportance = importanceFilter === 'all' || announcement.importance === importanceFilter;
    const matchesStatus = statusFilter === 'all' || 
                         (statusFilter === 'public' && announcement.isPublic) ||
                         (statusFilter === 'private' && !announcement.isPublic);
    
    return matchesSearch && matchesImportance && matchesStatus;
  });

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-2">
            <Megaphone className="h-8 w-8 text-blue-600" />
            {t('Announcements Management')}
          </h1>
          <p className="text-gray-600 mt-1">Create and manage school-wide announcements</p>
        </div>
        
        <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
          <DialogTrigger asChild>
            <Button className="bg-blue-600 hover:bg-blue-700">
              <Plus className="h-4 w-4 mr-2" />
              Create Announcement
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Create New Announcement</DialogTitle>
              <DialogDescription>
                Send an announcement to the entire school or specific groups
              </DialogDescription>
            </DialogHeader>
            
            <div className="space-y-4">
              <div>
                <Label htmlFor="title">Title *</Label>
                <Input
                  id="title"
                  value={newAnnouncement.title}
                  onChange={(e) => setNewAnnouncement(prev => ({ ...prev, title: e.target.value }))}
                  placeholder="Enter announcement title..."
                />
              </div>
              
              <div>
                <Label htmlFor="content">Content *</Label>
                <Textarea
                  id="content"
                  value={newAnnouncement.body}
                  onChange={(e) => setNewAnnouncement(prev => ({ ...prev, body: e.target.value }))}
                  placeholder="Enter announcement content..."
                  rows={4}
                />
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="priority">Priority</Label>
                  <Select
                    value={newAnnouncement.importance}
                    onValueChange={(value: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT') => 
                      setNewAnnouncement(prev => ({ ...prev, importance: value }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="LOW">Low Priority</SelectItem>
                      <SelectItem value="MEDIUM">Medium Priority</SelectItem>
                      <SelectItem value="HIGH">High Priority</SelectItem>
                      <SelectItem value="URGENT">Urgent</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <Label htmlFor="targetType">Target Audience</Label>
                  <Select
                    value={newAnnouncement.targetType}
                    onValueChange={(value: 'WHOLE_SCHOOL' | 'ALL_STAFF' | 'ALL_TEACHERS' | 'ALL_STUDENTS') => 
                      setNewAnnouncement(prev => ({ ...prev, targetType: value }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="WHOLE_SCHOOL">
                        <div className="flex items-center gap-2">
                          <School className="h-4 w-4" />
                          Whole School
                        </div>
                      </SelectItem>
                      <SelectItem value="ALL_STAFF">
                        <div className="flex items-center gap-2">
                          <Shield className="h-4 w-4" />
                          All Staff
                        </div>
                      </SelectItem>
                      <SelectItem value="ALL_TEACHERS">
                        <div className="flex items-center gap-2">
                          <GraduationCap className="h-4 w-4" />
                          All Teachers
                        </div>
                      </SelectItem>
                      <SelectItem value="ALL_STUDENTS">
                        <div className="flex items-center gap-2">
                          <UserCheck className="h-4 w-4" />
                          All Students
                        </div>
                      </SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              
              <div className="flex items-center space-x-4">
                <div className="flex items-center space-x-2">
                  <Switch
                    id="isPublic"
                    checked={newAnnouncement.isPublic || false}
                    onCheckedChange={(checked) => setNewAnnouncement(prev => ({ ...prev, isPublic: checked }))}
                  />
                  <Label htmlFor="isPublic">Make public (visible to all)</Label>
                </div>

                <div className="flex items-center space-x-2">
                  <Switch
                    id="sendNotifications"
                    checked={newAnnouncement.sendNotifications || false}
                    onCheckedChange={(checked) => setNewAnnouncement(prev => ({ ...prev, sendNotifications: checked }))}
                  />
                  <Label htmlFor="sendNotifications">Send notifications</Label>
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                Cancel
              </Button>
              <Button 
                onClick={handleCreateAnnouncement}
                disabled={createAnnouncementMutation.isPending || !newAnnouncement.title || !newAnnouncement.body}
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
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="public">Public</SelectItem>
                <SelectItem value="private">Private</SelectItem>
              </SelectContent>
            </Select>
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
                {searchQuery || importanceFilter !== 'all' || statusFilter !== 'all'
                  ? 'Try adjusting your filters to see more results.'
                  : 'Create your first announcement to get started.'
                }
              </p>
              {!searchQuery && importanceFilter === 'all' && statusFilter === 'all' && (
                <Button onClick={() => setIsCreateDialogOpen(true)}>
                  <Plus className="h-4 w-4 mr-2" />
                  Create Announcement
                </Button>
              )}
            </CardContent>
          </Card>
        ) : (
          filteredAnnouncements.map((announcement) => (
            <Card key={announcement.id} className="hover:shadow-lg transition-all duration-200">
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className="text-lg line-clamp-1">{announcement.title}</CardTitle>
                    <div className="flex items-center gap-2 mt-2">
                      <Badge className={getPriorityColor(announcement.importance)}>
                        {announcement.importance}
                      </Badge>
                      <Badge variant={announcement.isPublic ? "secondary" : "outline"}>
                        {announcement.isPublic ? 'Public' : 'Private'}
                      </Badge>
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
                
                <div className="flex items-center justify-between text-sm text-gray-500">
                  <div className="flex items-center gap-4">
                    <div className="flex items-center gap-1">
                      <Calendar className="h-3 w-3" />
                      {formatDistanceToNow(new Date(announcement.createdAt), { addSuffix: true })}
                    </div>
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

export default Announcements; 