import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
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
  Pin,
  PinOff,
  Share2,
  Bell,
  Mail,
  MessageSquare,
  Smartphone
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

interface Announcement {
  id: string;
  title: string;
  content: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  status: 'DRAFT' | 'SCHEDULED' | 'PUBLISHED' | 'ARCHIVED';
  targetAudience: {
    roles: string[];
    classes: string[];
    individuals: string[];
  };
  channels: ('EMAIL' | 'SMS' | 'PUSH' | 'IN_APP')[];
  scheduledAt?: string;
  publishedAt?: string;
  expiresAt?: string;
  isPinned: boolean;
  author: {
    id: string;
    name: string;
    avatar?: string;
  };
  stats: {
    views: number;
    sent: number;
    delivered: number;
    opened: number;
  };
  createdAt: string;
  updatedAt: string;
}

interface EnhancedAnnouncementSystemProps {
  className?: string;
}

export function EnhancedAnnouncementSystem({ className }: EnhancedAnnouncementSystemProps) {
  const [announcements, setAnnouncements] = useState<Announcement[]>([
    {
      id: '1',
      title: 'School Closure Due to Weather',
      content: 'Due to severe weather conditions, the school will be closed tomorrow, March 15th. All classes and activities are cancelled. Please stay safe and warm.',
      priority: 'URGENT',
      status: 'PUBLISHED',
      targetAudience: {
        roles: ['STUDENT', 'TEACHER', 'PARENT', 'STAFF'],
        classes: [],
        individuals: []
      },
      channels: ['EMAIL', 'SMS', 'PUSH', 'IN_APP'],
      publishedAt: '2024-03-14T08:00:00Z',
      expiresAt: '2024-03-16T23:59:59Z',
      isPinned: true,
      author: {
        id: 'admin1',
        name: 'Principal Johnson',
        avatar: '/avatars/principal.jpg'
      },
      stats: {
        views: 1247,
        sent: 1500,
        delivered: 1485,
        opened: 1247
      },
      createdAt: '2024-03-14T07:30:00Z',
      updatedAt: '2024-03-14T08:00:00Z'
    },
    {
      id: '2',
      title: 'Parent-Teacher Conference Schedule',
      content: 'Parent-teacher conferences are scheduled for next week. Please check your email for your assigned time slot and meeting details.',
      priority: 'HIGH',
      status: 'PUBLISHED',
      targetAudience: {
        roles: ['PARENT'],
        classes: [],
        individuals: []
      },
      channels: ['EMAIL', 'IN_APP'],
      publishedAt: '2024-03-13T14:00:00Z',
      isPinned: false,
      author: {
        id: 'staff1',
        name: 'Ms. Anderson',
        avatar: '/avatars/anderson.jpg'
      },
      stats: {
        views: 342,
        sent: 450,
        delivered: 445,
        opened: 342
      },
      createdAt: '2024-03-13T13:30:00Z',
      updatedAt: '2024-03-13T14:00:00Z'
    }
  ]);

  const [selectedAnnouncement, setSelectedAnnouncement] = useState<Announcement | null>(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [priorityFilter, setPriorityFilter] = useState('all');

  const [newAnnouncement, setNewAnnouncement] = useState({
    title: '',
    content: '',
    priority: 'MEDIUM' as const,
    targetAudience: {
      roles: [] as string[],
      classes: [] as string[],
      individuals: [] as string[]
    },
    channels: ['IN_APP'] as ('EMAIL' | 'SMS' | 'PUSH' | 'IN_APP')[],
    scheduledAt: '',
    expiresAt: '',
    isPinned: false
  });

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

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PUBLISHED':
        return 'bg-green-100 text-green-800';
      case 'SCHEDULED':
        return 'bg-yellow-100 text-yellow-800';
      case 'DRAFT':
        return 'bg-gray-100 text-gray-800';
      case 'ARCHIVED':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getChannelIcon = (channel: string) => {
    switch (channel) {
      case 'EMAIL':
        return <Mail className="h-3 w-3" />;
      case 'SMS':
        return <MessageSquare className="h-3 w-3" />;
      case 'PUSH':
        return <Smartphone className="h-3 w-3" />;
      case 'IN_APP':
        return <Bell className="h-3 w-3" />;
      default:
        return <Bell className="h-3 w-3" />;
    }
  };

  const handleCreateAnnouncement = () => {
    const announcement: Announcement = {
      id: Date.now().toString(),
      ...newAnnouncement,
      status: newAnnouncement.scheduledAt ? 'SCHEDULED' : 'PUBLISHED',
      publishedAt: newAnnouncement.scheduledAt || new Date().toISOString(),
      author: {
        id: 'current-user',
        name: 'Current User',
        avatar: '/avatars/default.jpg'
      },
      stats: {
        views: 0,
        sent: 0,
        delivered: 0,
        opened: 0
      },
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    setAnnouncements(prev => [announcement, ...prev]);
    setIsCreateDialogOpen(false);
    setNewAnnouncement({
      title: '',
      content: '',
      priority: 'MEDIUM',
      targetAudience: { roles: [], classes: [], individuals: [] },
      channels: ['IN_APP'],
      scheduledAt: '',
      expiresAt: '',
      isPinned: false
    });
    toast.success('Announcement created successfully');
  };

  const handleTogglePin = (id: string) => {
    setAnnouncements(prev => 
      prev.map(announcement => 
        announcement.id === id 
          ? { ...announcement, isPinned: !announcement.isPinned }
          : announcement
      )
    );
    toast.success('Announcement pin status updated');
  };

  const handleDeleteAnnouncement = (id: string) => {
    setAnnouncements(prev => prev.filter(announcement => announcement.id !== id));
    toast.success('Announcement deleted successfully');
  };

  const filteredAnnouncements = announcements.filter(announcement => {
    const matchesSearch = announcement.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         announcement.content.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === 'all' || announcement.status === statusFilter;
    const matchesPriority = priorityFilter === 'all' || announcement.priority === priorityFilter;
    
    return matchesSearch && matchesStatus && matchesPriority;
  });

  const pinnedAnnouncements = filteredAnnouncements.filter(a => a.isPinned);
  const regularAnnouncements = filteredAnnouncements.filter(a => !a.isPinned);

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-2">
            <Megaphone className="h-8 w-8 text-blue-600" />
            Announcements
          </h1>
          <p className="text-gray-600 mt-1">Create and manage school announcements with targeted delivery</p>
        </div>
        
        <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
          <DialogTrigger asChild>
            <Button className="bg-blue-600 hover:bg-blue-700">
              <Plus className="h-4 w-4 mr-2" />
              Create Announcement
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Create New Announcement</DialogTitle>
              <DialogDescription>
                Create a targeted announcement with rich content and delivery options
              </DialogDescription>
            </DialogHeader>
            
            <div className="space-y-6">
              <Tabs defaultValue="content" className="w-full">
                <TabsList className="grid w-full grid-cols-3">
                  <TabsTrigger value="content">Content</TabsTrigger>
                  <TabsTrigger value="targeting">Targeting</TabsTrigger>
                  <TabsTrigger value="delivery">Delivery</TabsTrigger>
                </TabsList>

                <TabsContent value="content" className="space-y-4">
                  <div>
                    <Label htmlFor="title">Title</Label>
                    <Input
                      id="title"
                      value={newAnnouncement.title}
                      onChange={(e) => setNewAnnouncement(prev => ({ ...prev, title: e.target.value }))}
                      placeholder="Enter announcement title..."
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="content">Content</Label>
                    <Textarea
                      id="content"
                      value={newAnnouncement.content}
                      onChange={(e) => setNewAnnouncement(prev => ({ ...prev, content: e.target.value }))}
                      placeholder="Enter announcement content..."
                      rows={6}
                    />
                  </div>
                  
                  <div>
                    <Label htmlFor="priority">Priority</Label>
                    <Select
                      value={newAnnouncement.priority}
                      onValueChange={(value: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT') => 
                        setNewAnnouncement(prev => ({ ...prev, priority: value }))
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
                </TabsContent>

                <TabsContent value="targeting" className="space-y-4">
                  <div>
                    <Label className="text-base font-medium mb-3 block">Target Audience</Label>
                    <div className="space-y-3">
                      <div>
                        <Label className="text-sm font-medium mb-2 block">Roles</Label>
                        <div className="grid grid-cols-2 gap-2">
                          {['STUDENT', 'TEACHER', 'PARENT', 'STAFF'].map(role => (
                            <div key={role} className="flex items-center space-x-2">
                              <Checkbox
                                id={role}
                                checked={newAnnouncement.targetAudience.roles.includes(role)}
                                onCheckedChange={(checked) => {
                                  if (checked) {
                                    setNewAnnouncement(prev => ({
                                      ...prev,
                                      targetAudience: {
                                        ...prev.targetAudience,
                                        roles: [...prev.targetAudience.roles, role]
                                      }
                                    }));
                                  } else {
                                    setNewAnnouncement(prev => ({
                                      ...prev,
                                      targetAudience: {
                                        ...prev.targetAudience,
                                        roles: prev.targetAudience.roles.filter(r => r !== role)
                                      }
                                    }));
                                  }
                                }}
                              />
                              <Label htmlFor={role} className="text-sm">
                                {role.charAt(0) + role.slice(1).toLowerCase()}s
                              </Label>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                </TabsContent>

                <TabsContent value="delivery" className="space-y-4">
                  <div>
                    <Label className="text-base font-medium mb-3 block">Delivery Channels</Label>
                    <div className="grid grid-cols-2 gap-3">
                      {[
                        { key: 'IN_APP', label: 'In-App', icon: Bell },
                        { key: 'EMAIL', label: 'Email', icon: Mail },
                        { key: 'SMS', label: 'SMS', icon: MessageSquare },
                        { key: 'PUSH', label: 'Push', icon: Smartphone }
                      ].map(({ key, label, icon: Icon }) => (
                        <div key={key} className="flex items-center space-x-2 p-3 border rounded-lg">
                          <Checkbox
                            id={key}
                            checked={newAnnouncement.channels.includes(key as any)}
                            onCheckedChange={(checked) => {
                              if (checked) {
                                setNewAnnouncement(prev => ({
                                  ...prev,
                                  channels: [...prev.channels, key as any]
                                }));
                              } else {
                                setNewAnnouncement(prev => ({
                                  ...prev,
                                  channels: prev.channels.filter(c => c !== key)
                                }));
                              }
                            }}
                          />
                          <Icon className="h-4 w-4" />
                          <Label htmlFor={key} className="text-sm">{label}</Label>
                        </div>
                      ))}
                    </div>
                  </div>
                  
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="scheduledAt">Schedule For Later (Optional)</Label>
                      <Input
                        id="scheduledAt"
                        type="datetime-local"
                        value={newAnnouncement.scheduledAt}
                        onChange={(e) => setNewAnnouncement(prev => ({ ...prev, scheduledAt: e.target.value }))}
                      />
                    </div>
                    <div>
                      <Label htmlFor="expiresAt">Expires At (Optional)</Label>
                      <Input
                        id="expiresAt"
                        type="datetime-local"
                        value={newAnnouncement.expiresAt}
                        onChange={(e) => setNewAnnouncement(prev => ({ ...prev, expiresAt: e.target.value }))}
                      />
                    </div>
                  </div>
                  
                  <div className="flex items-center space-x-2">
                    <Switch
                      id="isPinned"
                      checked={newAnnouncement.isPinned}
                      onCheckedChange={(checked) => setNewAnnouncement(prev => ({ ...prev, isPinned: checked }))}
                    />
                    <Label htmlFor="isPinned">Pin this announcement</Label>
                  </div>
                </TabsContent>
              </Tabs>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                Cancel
              </Button>
              <Button 
                onClick={handleCreateAnnouncement}
                disabled={!newAnnouncement.title || !newAnnouncement.content}
              >
                <Send className="h-4 w-4 mr-2" />
                {newAnnouncement.scheduledAt ? 'Schedule' : 'Publish'} Announcement
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
                <SelectItem value="PUBLISHED">Published</SelectItem>
                <SelectItem value="SCHEDULED">Scheduled</SelectItem>
                <SelectItem value="DRAFT">Draft</SelectItem>
                <SelectItem value="ARCHIVED">Archived</SelectItem>
              </SelectContent>
            </Select>
            <Select value={priorityFilter} onValueChange={setPriorityFilter}>
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

      {/* Pinned Announcements */}
      {pinnedAnnouncements.length > 0 && (
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Pin className="h-5 w-5 text-blue-600" />
            Pinned Announcements
          </h2>
          <div className="space-y-4">
            {pinnedAnnouncements.map((announcement) => (
              <AnnouncementCard
                key={announcement.id}
                announcement={announcement}
                onTogglePin={handleTogglePin}
                onDelete={handleDeleteAnnouncement}
                onView={setSelectedAnnouncement}
              />
            ))}
          </div>
        </div>
      )}

      {/* Regular Announcements */}
      <div>
        {pinnedAnnouncements.length > 0 && (
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Recent Announcements</h2>
        )}
        <div className="space-y-4">
          {regularAnnouncements.length === 0 ? (
            <Card className="text-center py-12">
              <CardContent>
                <Megaphone className="h-12 w-12 text-gray-300 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">No announcements found</h3>
                <p className="text-gray-500 mb-4">
                  {searchQuery || statusFilter !== 'all' || priorityFilter !== 'all'
                    ? 'Try adjusting your filters to see more results.'
                    : 'Create your first announcement to get started.'
                  }
                </p>
                {!searchQuery && statusFilter === 'all' && priorityFilter === 'all' && (
                  <Button onClick={() => setIsCreateDialogOpen(true)}>
                    <Plus className="h-4 w-4 mr-2" />
                    Create Announcement
                  </Button>
                )}
              </CardContent>
            </Card>
          ) : (
            regularAnnouncements.map((announcement) => (
              <AnnouncementCard
                key={announcement.id}
                announcement={announcement}
                onTogglePin={handleTogglePin}
                onDelete={handleDeleteAnnouncement}
                onView={setSelectedAnnouncement}
              />
            ))
          )}
        </div>
      </div>

      {/* Announcement Detail Dialog */}
      <Dialog open={!!selectedAnnouncement} onOpenChange={() => setSelectedAnnouncement(null)}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          {selectedAnnouncement && (
            <>
              <DialogHeader>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <DialogTitle className="text-xl">{selectedAnnouncement.title}</DialogTitle>
                    <div className="flex items-center gap-2 mt-2">
                      <Badge className={getPriorityColor(selectedAnnouncement.priority)}>
                        {selectedAnnouncement.priority}
                      </Badge>
                      <Badge variant="secondary" className={getStatusColor(selectedAnnouncement.status)}>
                        {selectedAnnouncement.status}
                      </Badge>
                      {selectedAnnouncement.isPinned && (
                        <Badge variant="outline" className="border-blue-200 text-blue-800">
                          <Pin className="h-3 w-3 mr-1" />
                          Pinned
                        </Badge>
                      )}
                    </div>
                  </div>
                </div>
              </DialogHeader>
              
              <div className="space-y-6">
                <div>
                  <h4 className="font-medium mb-2">Content</h4>
                  <p className="text-gray-700 whitespace-pre-wrap">{selectedAnnouncement.content}</p>
                </div>
                
                <div className="grid grid-cols-2 gap-6">
                  <div>
                    <h4 className="font-medium mb-2">Delivery Channels</h4>
                    <div className="flex gap-2">
                      {selectedAnnouncement.channels.map(channel => (
                        <Badge key={channel} variant="outline" className="flex items-center gap-1">
                          {getChannelIcon(channel)}
                          {channel}
                        </Badge>
                      ))}
                    </div>
                  </div>
                  
                  <div>
                    <h4 className="font-medium mb-2">Target Audience</h4>
                    <div className="flex flex-wrap gap-1">
                      {selectedAnnouncement.targetAudience.roles.map(role => (
                        <Badge key={role} variant="secondary">
                          {role.charAt(0) + role.slice(1).toLowerCase()}s
                        </Badge>
                      ))}
                    </div>
                  </div>
                </div>
                
                <div>
                  <h4 className="font-medium mb-2">Statistics</h4>
                  <div className="grid grid-cols-4 gap-4">
                    <div className="text-center p-3 bg-blue-50 rounded-lg">
                      <div className="text-2xl font-bold text-blue-900">{selectedAnnouncement.stats.sent}</div>
                      <div className="text-sm text-blue-600">Sent</div>
                    </div>
                    <div className="text-center p-3 bg-green-50 rounded-lg">
                      <div className="text-2xl font-bold text-green-900">{selectedAnnouncement.stats.delivered}</div>
                      <div className="text-sm text-green-600">Delivered</div>
                    </div>
                    <div className="text-center p-3 bg-purple-50 rounded-lg">
                      <div className="text-2xl font-bold text-purple-900">{selectedAnnouncement.stats.opened}</div>
                      <div className="text-sm text-purple-600">Opened</div>
                    </div>
                    <div className="text-center p-3 bg-orange-50 rounded-lg">
                      <div className="text-2xl font-bold text-orange-900">{selectedAnnouncement.stats.views}</div>
                      <div className="text-sm text-orange-600">Views</div>
                    </div>
                  </div>
                </div>
                
                <div className="flex items-center justify-between text-sm text-gray-500 pt-4 border-t">
                  <div className="flex items-center gap-2">
                    <Avatar className="h-6 w-6">
                      <AvatarImage src={selectedAnnouncement.author.avatar} />
                      <AvatarFallback>{selectedAnnouncement.author.name.charAt(0)}</AvatarFallback>
                    </Avatar>
                    <span>By {selectedAnnouncement.author.name}</span>
                  </div>
                  <div>
                    Published {formatDistanceToNow(new Date(selectedAnnouncement.publishedAt || selectedAnnouncement.createdAt), { addSuffix: true })}
                  </div>
                </div>
              </div>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}

// Announcement Card Component
function AnnouncementCard({ 
  announcement, 
  onTogglePin, 
  onDelete, 
  onView 
}: {
  announcement: Announcement;
  onTogglePin: (id: string) => void;
  onDelete: (id: string) => void;
  onView: (announcement: Announcement) => void;
}) {
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

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PUBLISHED':
        return 'bg-green-100 text-green-800';
      case 'SCHEDULED':
        return 'bg-yellow-100 text-yellow-800';
      case 'DRAFT':
        return 'bg-gray-100 text-gray-800';
      case 'ARCHIVED':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getChannelIcon = (channel: string) => {
    switch (channel) {
      case 'EMAIL':
        return <Mail className="h-3 w-3" />;
      case 'SMS':
        return <MessageSquare className="h-3 w-3" />;
      case 'PUSH':
        return <Smartphone className="h-3 w-3" />;
      case 'IN_APP':
        return <Bell className="h-3 w-3" />;
      default:
        return <Bell className="h-3 w-3" />;
    }
  };

  return (
    <Card className={`hover:shadow-lg transition-all duration-200 ${announcement.isPinned ? 'border-blue-200 bg-blue-50/30' : ''}`}>
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              {announcement.isPinned && <Pin className="h-4 w-4 text-blue-600" />}
              <CardTitle className="text-lg line-clamp-1">{announcement.title}</CardTitle>
            </div>
            <div className="flex items-center gap-2">
              <Badge className={getPriorityColor(announcement.priority)}>
                {announcement.priority}
              </Badge>
              <Badge variant="secondary" className={getStatusColor(announcement.status)}>
                {announcement.status}
              </Badge>
              <div className="flex items-center gap-1">
                {announcement.channels.slice(0, 3).map(channel => (
                  <div key={channel} className="p-1 bg-gray-100 rounded">
                    {getChannelIcon(channel)}
                  </div>
                ))}
                {announcement.channels.length > 3 && (
                  <Badge variant="outline" className="text-xs">
                    +{announcement.channels.length - 3}
                  </Badge>
                )}
              </div>
            </div>
          </div>
          
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm">
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => onView(announcement)}>
                <Eye className="h-4 w-4 mr-2" />
                View Details
              </DropdownMenuItem>
              <DropdownMenuItem>
                <Edit className="h-4 w-4 mr-2" />
                Edit
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onTogglePin(announcement.id)}>
                {announcement.isPinned ? (
                  <>
                    <PinOff className="h-4 w-4 mr-2" />
                    Unpin
                  </>
                ) : (
                  <>
                    <Pin className="h-4 w-4 mr-2" />
                    Pin
                  </>
                )}
              </DropdownMenuItem>
              <DropdownMenuItem>
                <Share2 className="h-4 w-4 mr-2" />
                Share
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem 
                onClick={() => onDelete(announcement.id)}
                className="text-red-600 focus:text-red-600"
              >
                <Trash2 className="h-4 w-4 mr-2" />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>
      
      <CardContent>
        <p className="text-gray-600 line-clamp-2 mb-4">{announcement.content}</p>
        
        <div className="flex items-center justify-between text-sm text-gray-500">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1">
              <Eye className="h-3 w-3" />
              {announcement.stats.views}
            </div>
            <div className="flex items-center gap-1">
              <Send className="h-3 w-3" />
              {announcement.stats.sent}
            </div>
            <div className="flex items-center gap-1">
              <Users className="h-3 w-3" />
              {announcement.targetAudience.roles.join(', ')}
            </div>
          </div>
          <div>
            {formatDistanceToNow(new Date(announcement.publishedAt || announcement.createdAt), { addSuffix: true })}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
