"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { 
  BookOpen, 
  Users, 
  FileText, 
  Video, 
  Download, 
  Calendar,
  Clock,
  Star,
  MessageSquare,
  Plus,
  Search,
  Filter,
  Grid3X3,
  List,
  ChevronRight,
  Play,
  Eye,
  Share2,
  MoreVertical,
  Upload,
  Bell,
  Pin,
  Archive,
  Edit,
  Trash2,
  ExternalLink
} from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { Progress } from "@/components/ui/progress";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";

interface ClassroomResource {
  id: string;
  title: string;
  type: 'document' | 'video' | 'assignment' | 'quiz' | 'presentation' | 'link';
  description: string;
  uploadedBy: string;
  uploadedAt: Date;
  size?: string;
  duration?: string;
  downloads: number;
  views: number;
  tags: string[];
  isPinned: boolean;
  isArchived: boolean;
  dueDate?: Date;
  submissionCount?: number;
  totalStudents?: number;
}

interface ClassroomAnnouncement {
  id: string;
  title: string;
  content: string;
  author: string;
  createdAt: Date;
  isPinned: boolean;
  attachments?: string[];
}

interface ClassroomViewProps {
  classId: string;
  className: string;
  subject: string;
  teacher: string;
  studentCount: number;
  color: string;
}

export function ClassroomView({ 
  classId, 
  className, 
  subject, 
  teacher, 
  studentCount, 
  color 
}: ClassroomViewProps) {
  const [activeTab, setActiveTab] = useState("stream");
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [searchQuery, setSearchQuery] = useState('');

  // Mock data for classroom resources
  const resources: ClassroomResource[] = [
    {
      id: "1",
      title: "Week 5: Advanced Calculus Problems",
      type: "document",
      description: "Practice problems for derivatives and integrals with step-by-step solutions",
      uploadedBy: teacher,
      uploadedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000),
      size: "3.2 MB",
      downloads: 45,
      views: 78,
      tags: ["calculus", "derivatives", "integrals"],
      isPinned: true,
      isArchived: false
    },
    {
      id: "2",
      title: "Midterm Assignment: Research Project",
      type: "assignment",
      description: "Research and analyze a mathematical concept of your choice. Due next Friday.",
      uploadedBy: teacher,
      uploadedAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000),
      downloads: 28,
      views: 56,
      tags: ["research", "midterm", "project"],
      isPinned: false,
      isArchived: false,
      dueDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      submissionCount: 18,
      totalStudents: studentCount
    },
    {
      id: "3",
      title: "Interactive Graphing Tutorial",
      type: "video",
      description: "Learn how to use graphing tools for complex mathematical functions",
      uploadedBy: teacher,
      uploadedAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000),
      duration: "24:15",
      downloads: 67,
      views: 134,
      tags: ["graphing", "tutorial", "functions"],
      isPinned: false,
      isArchived: false
    }
  ];

  // Mock data for announcements
  const announcements: ClassroomAnnouncement[] = [
    {
      id: "1",
      title: "Midterm Exam Schedule",
      content: "The midterm exam will be held on March 15th at 10:00 AM in Room A-201. Please bring your calculators and ID cards.",
      author: teacher,
      createdAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000),
      isPinned: true,
      attachments: ["exam_guidelines.pdf"]
    },
    {
      id: "2",
      title: "Office Hours Update",
      content: "My office hours for this week have been moved to Wednesday 2-4 PM due to a faculty meeting on Tuesday.",
      author: teacher,
      createdAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000),
      isPinned: false
    }
  ];

  const getResourceIcon = (type: string) => {
    switch (type) {
      case 'document': return FileText;
      case 'video': return Video;
      case 'assignment': return BookOpen;
      case 'quiz': return MessageSquare;
      case 'presentation': return Play;
      case 'link': return ExternalLink;
      default: return FileText;
    }
  };

  const getResourceColor = (type: string) => {
    switch (type) {
      case 'document': return 'from-blue-500 to-blue-600';
      case 'video': return 'from-red-500 to-red-600';
      case 'assignment': return 'from-green-500 to-green-600';
      case 'quiz': return 'from-purple-500 to-purple-600';
      case 'presentation': return 'from-orange-500 to-orange-600';
      case 'link': return 'from-indigo-500 to-indigo-600';
      default: return 'from-gray-500 to-gray-600';
    }
  };

  const filteredResources = resources.filter(resource => {
    const matchesSearch = resource.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         resource.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         resource.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesSearch && !resource.isArchived;
  });

  const pinnedResources = filteredResources.filter(r => r.isPinned);
  const regularResources = filteredResources.filter(r => !r.isPinned);

  return (
    <div className="space-y-6">
      {/* Classroom Header */}
      <div className={cn(
        "relative overflow-hidden rounded-2xl bg-gradient-to-br p-8 text-white shadow-xl",
        color
      )}>
        <div className="relative z-10">
          <div className="flex items-start justify-between mb-4">
            <div>
              <h1 className="text-3xl font-bold mb-2">{className}</h1>
              <p className="text-white/90 text-lg">{subject}</p>
              <p className="text-white/80 text-sm">Taught by {teacher}</p>
            </div>
            
            <div className="flex items-center gap-3">
              <div className="text-center">
                <div className="text-2xl font-bold">{studentCount}</div>
                <div className="text-white/80 text-sm">Students</div>
              </div>
              
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="sm" className="text-white hover:bg-white/20">
                    <MoreVertical className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem>
                    <Edit className="h-4 w-4 mr-2" />
                    Edit Class
                  </DropdownMenuItem>
                  <DropdownMenuItem>
                    <Users className="h-4 w-4 mr-2" />
                    Manage Students
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem>
                    <Archive className="h-4 w-4 mr-2" />
                    Archive Class
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            <Button variant="ghost" className="text-white hover:bg-white/20">
              <Upload className="h-4 w-4 mr-2" />
              Upload Resource
            </Button>
            <Button variant="ghost" className="text-white hover:bg-white/20">
              <Plus className="h-4 w-4 mr-2" />
              Create Assignment
            </Button>
          </div>
        </div>
        
        {/* Background Pattern */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-4 right-4 w-32 h-32 rounded-full border-2 border-white/30"></div>
          <div className="absolute bottom-4 left-4 w-24 h-24 rounded-full border-2 border-white/20"></div>
          <div className="absolute top-1/2 left-1/3 w-16 h-16 rounded-full border border-white/20"></div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <div className="flex items-center justify-between">
          <TabsList className="grid w-fit grid-cols-4 bg-white/80 backdrop-blur-sm border border-slate-200">
            <TabsTrigger value="stream" className="data-[state=active]:bg-blue-100 data-[state=active]:text-blue-700">
              Stream
            </TabsTrigger>
            <TabsTrigger value="classwork" className="data-[state=active]:bg-blue-100 data-[state=active]:text-blue-700">
              Classwork
            </TabsTrigger>
            <TabsTrigger value="people" className="data-[state=active]:bg-blue-100 data-[state=active]:text-blue-700">
              People
            </TabsTrigger>
            <TabsTrigger value="grades" className="data-[state=active]:bg-blue-100 data-[state=active]:text-blue-700">
              Grades
            </TabsTrigger>
          </TabsList>
          
          {activeTab === 'classwork' && (
            <div className="flex items-center gap-2">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                <Input
                  placeholder="Search resources..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10 w-64 border-slate-300"
                />
              </div>
              
              <div className="flex items-center border border-slate-200 rounded-lg bg-white">
                <Button
                  variant={viewMode === 'grid' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setViewMode('grid')}
                  className="rounded-r-none"
                >
                  <Grid3X3 className="h-4 w-4" />
                </Button>
                <Button
                  variant={viewMode === 'list' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setViewMode('list')}
                  className="rounded-l-none"
                >
                  <List className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </div>

        {/* Stream Tab */}
        <TabsContent value="stream" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Main Stream */}
            <div className="lg:col-span-2 space-y-6">
              {/* Announcements */}
              <Card className="border-slate-200/60 bg-white/95 backdrop-blur-sm">
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle className="flex items-center gap-2">
                      <Bell className="h-5 w-5 text-blue-600" />
                      Announcements
                    </CardTitle>
                    <Button size="sm" variant="outline">
                      <Plus className="h-4 w-4 mr-2" />
                      New
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <AnimatePresence>
                    {announcements.map((announcement) => (
                      <motion.div
                        key={announcement.id}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -20 }}
                        className={cn(
                          "p-4 rounded-lg border transition-all duration-200",
                          announcement.isPinned 
                            ? "bg-blue-50 border-blue-200" 
                            : "bg-slate-50 border-slate-200"
                        )}
                      >
                        <div className="flex items-start gap-3">
                          <Avatar className="w-10 h-10">
                            <AvatarFallback className="bg-blue-100 text-blue-700">
                              {announcement.author.split(' ').map(n => n[0]).join('')}
                            </AvatarFallback>
                          </Avatar>
                          
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-2">
                              <h4 className="font-semibold text-slate-900">{announcement.title}</h4>
                              {announcement.isPinned && (
                                <Pin className="h-4 w-4 text-blue-600" />
                              )}
                            </div>
                            <p className="text-slate-700 mb-2">{announcement.content}</p>
                            <div className="flex items-center justify-between text-xs text-slate-500">
                              <span>By {announcement.author}</span>
                              <span>{announcement.createdAt.toLocaleDateString()}</span>
                            </div>
                            {announcement.attachments && (
                              <div className="mt-2 flex gap-2">
                                {announcement.attachments.map((attachment, index) => (
                                  <Badge key={index} variant="outline" className="text-xs">
                                    <FileText className="h-3 w-3 mr-1" />
                                    {attachment}
                                  </Badge>
                                ))}
                              </div>
                            )}
                          </div>
                        </div>
                      </motion.div>
                    ))}
                  </AnimatePresence>
                </CardContent>
              </Card>

              {/* Recent Activity */}
              <Card className="border-slate-200/60 bg-white/95 backdrop-blur-sm">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Clock className="h-5 w-5 text-green-600" />
                    Recent Activity
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {resources.slice(0, 3).map((resource) => (
                      <div key={resource.id} className="flex items-center gap-3 p-3 rounded-lg hover:bg-slate-50 transition-colors">
                        <div className={cn(
                          "w-8 h-8 rounded-lg flex items-center justify-center bg-gradient-to-br",
                          getResourceColor(resource.type)
                        )}>
                          {React.createElement(getResourceIcon(resource.type), { className: "h-4 w-4 text-white" })}
                        </div>
                        <div className="flex-1">
                          <p className="font-medium text-slate-900">{resource.title}</p>
                          <p className="text-sm text-slate-600">
                            {resource.type === 'assignment' ? 'Assignment posted' : 'Resource uploaded'}
                          </p>
                        </div>
                        <div className="text-xs text-slate-500">
                          {resource.uploadedAt.toLocaleDateString()}
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Sidebar */}
            <div className="space-y-6">
              {/* Upcoming Assignments */}
              <Card className="border-slate-200/60 bg-white/95 backdrop-blur-sm">
                <CardHeader>
                  <CardTitle className="text-lg">Upcoming</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {resources.filter(r => r.dueDate).map((assignment) => (
                      <div key={assignment.id} className="p-3 rounded-lg bg-orange-50 border border-orange-200">
                        <h4 className="font-medium text-slate-900 mb-1">{assignment.title}</h4>
                        <p className="text-sm text-slate-600 mb-2">
                          Due: {assignment.dueDate?.toLocaleDateString()}
                        </p>
                        {assignment.submissionCount && assignment.totalStudents && (
                          <div className="space-y-1">
                            <div className="flex justify-between text-xs text-slate-600">
                              <span>Submissions</span>
                              <span>{assignment.submissionCount}/{assignment.totalStudents}</span>
                            </div>
                            <Progress 
                              value={(assignment.submissionCount / assignment.totalStudents) * 100} 
                              className="h-2"
                            />
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              {/* Quick Stats */}
              <Card className="border-slate-200/60 bg-white/95 backdrop-blur-sm">
                <CardHeader>
                  <CardTitle className="text-lg">Class Stats</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-2 gap-4 text-center">
                    <div>
                      <div className="text-2xl font-bold text-blue-600">{resources.length}</div>
                      <div className="text-xs text-slate-500">Resources</div>
                    </div>
                    <div>
                      <div className="text-2xl font-bold text-green-600">
                        {resources.filter(r => r.type === 'assignment').length}
                      </div>
                      <div className="text-xs text-slate-500">Assignments</div>
                    </div>
                    <div>
                      <div className="text-2xl font-bold text-purple-600">
                        {resources.reduce((sum, r) => sum + r.views, 0)}
                      </div>
                      <div className="text-xs text-slate-500">Total Views</div>
                    </div>
                    <div>
                      <div className="text-2xl font-bold text-orange-600">
                        {resources.reduce((sum, r) => sum + r.downloads, 0)}
                      </div>
                      <div className="text-xs text-slate-500">Downloads</div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            </div>
          </div>
        </TabsContent>

        {/* Classwork Tab */}
        <TabsContent value="classwork" className="space-y-6">
          {/* Pinned Resources */}
          {pinnedResources.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
                <Pin className="h-5 w-5 text-blue-600" />
                Pinned
              </h3>
              
              <div className={cn(
                viewMode === 'grid' 
                  ? "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
                  : "space-y-3"
              )}>
                {pinnedResources.map((resource) => (
                  <ResourceCard 
                    key={resource.id} 
                    resource={resource} 
                    viewMode={viewMode}
                    isPinned={true}
                  />
                ))}
              </div>
              
              <Separator />
            </div>
          )}

          {/* Regular Resources */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-slate-900">All Resources</h3>
            
            <div className={cn(
              viewMode === 'grid' 
                ? "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
                : "space-y-3"
            )}>
              {regularResources.map((resource) => (
                <ResourceCard 
                  key={resource.id} 
                  resource={resource} 
                  viewMode={viewMode}
                  isPinned={false}
                />
              ))}
            </div>
          </div>
        </TabsContent>

        {/* People Tab */}
        <TabsContent value="people" className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Teacher */}
            <Card className="border-slate-200/60 bg-white/95 backdrop-blur-sm">
              <CardHeader>
                <CardTitle>Teacher</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex items-center gap-4">
                  <Avatar className="w-16 h-16">
                    <AvatarFallback className="bg-blue-100 text-blue-700 text-lg">
                      {teacher.split(' ').map(n => n[0]).join('')}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <h3 className="font-semibold text-slate-900">{teacher}</h3>
                    <p className="text-slate-600">{subject} Teacher</p>
                    <Button variant="outline" size="sm" className="mt-2">
                      <MessageSquare className="h-4 w-4 mr-2" />
                      Message
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Students */}
            <Card className="border-slate-200/60 bg-white/95 backdrop-blur-sm">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle>Students ({studentCount})</CardTitle>
                  <Button variant="outline" size="sm">
                    <Users className="h-4 w-4 mr-2" />
                    Manage
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-center py-8 text-slate-500">
                  <Users className="h-12 w-12 mx-auto mb-4 text-slate-300" />
                  <p>Student list will be displayed here</p>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        {/* Grades Tab */}
        <TabsContent value="grades" className="space-y-6">
          <Card className="border-slate-200/60 bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle>Grade Management</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8 text-slate-500">
                <Star className="h-12 w-12 mx-auto mb-4 text-slate-300" />
                <p>Grade management interface will be displayed here</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

// Resource Card Component
function ResourceCard({ 
  resource, 
  viewMode, 
  isPinned 
}: { 
  resource: ClassroomResource; 
  viewMode: 'grid' | 'list';
  isPinned: boolean;
}) {
  const IconComponent = getResourceIcon(resource.type);
  
  if (viewMode === 'list') {
    return (
      <Card className="group hover:shadow-lg transition-all duration-200 border-slate-200/60 bg-white/95">
        <CardContent className="p-4">
          <div className="flex items-center gap-4">
            <div className={cn(
              "w-10 h-10 rounded-lg flex items-center justify-center bg-gradient-to-br flex-shrink-0",
              getResourceColor(resource.type)
            )}>
              <IconComponent className="h-5 w-5 text-white" />
            </div>
            
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <h3 className="font-semibold text-slate-900 group-hover:text-blue-900 transition-colors truncate">
                  {resource.title}
                </h3>
                {isPinned && <Pin className="h-4 w-4 text-blue-600 flex-shrink-0" />}
              </div>
              <p className="text-sm text-slate-600 truncate">
                {resource.description}
              </p>
              <div className="flex items-center gap-4 mt-1 text-xs text-slate-500">
                <span>By {resource.uploadedBy}</span>
                <span>•</span>
                <span>{resource.uploadedAt.toLocaleDateString()}</span>
                {resource.dueDate && (
                  <>
                    <span>•</span>
                    <span className="text-orange-600 font-medium">
                      Due: {resource.dueDate.toLocaleDateString()}
                    </span>
                  </>
                )}
              </div>
            </div>
            
            <div className="flex items-center gap-6 text-xs text-slate-500">
              <div className="flex items-center gap-1">
                <Eye className="h-3 w-3" />
                {resource.views}
              </div>
              <div className="flex items-center gap-1">
                <Download className="h-3 w-3" />
                {resource.downloads}
              </div>
            </div>
            
            <Button size="sm" variant="ghost" className="text-blue-600 hover:text-blue-700">
              Open
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="group hover:shadow-xl transition-all duration-300 border-slate-200/60 bg-white/95 backdrop-blur-sm">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className={cn(
            "w-12 h-12 rounded-xl flex items-center justify-center bg-gradient-to-br",
            getResourceColor(resource.type)
          )}>
            <IconComponent className="h-6 w-6 text-white" />
          </div>
          
          <div className="flex items-center gap-1">
            {isPinned && <Pin className="h-4 w-4 text-blue-600" />}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="sm" className="opacity-0 group-hover:opacity-100 transition-opacity">
                  <MoreVertical className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem>
                  <Eye className="h-4 w-4 mr-2" />
                  Preview
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <Download className="h-4 w-4 mr-2" />
                  Download
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <Share2 className="h-4 w-4 mr-2" />
                  Share
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem>
                  <Pin className="h-4 w-4 mr-2" />
                  {isPinned ? 'Unpin' : 'Pin'}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
        
        <div className="space-y-2">
          <CardTitle className="text-lg font-semibold text-slate-900 group-hover:text-blue-900 transition-colors line-clamp-2">
            {resource.title}
          </CardTitle>
          <p className="text-sm text-slate-600 line-clamp-2">
            {resource.description}
          </p>
        </div>
      </CardHeader>
      
      <CardContent className="pt-0">
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>By {resource.uploadedBy}</span>
            <span>{resource.uploadedAt.toLocaleDateString()}</span>
          </div>
          
          {resource.dueDate && (
            <div className="p-2 rounded-lg bg-orange-50 border border-orange-200">
              <div className="flex items-center justify-between text-xs">
                <span className="text-orange-700 font-medium">Due Date</span>
                <span className="text-orange-600">{resource.dueDate.toLocaleDateString()}</span>
              </div>
              {resource.submissionCount && resource.totalStudents && (
                <div className="mt-2 space-y-1">
                  <div className="flex justify-between text-xs text-orange-600">
                    <span>Submissions</span>
                    <span>{resource.submissionCount}/{resource.totalStudents}</span>
                  </div>
                  <Progress 
                    value={(resource.submissionCount / resource.totalStudents) * 100} 
                    className="h-1"
                  />
                </div>
              )}
            </div>
          )}
          
          <div className="flex items-center gap-4 text-xs text-slate-500">
            <div className="flex items-center gap-1">
              <Eye className="h-3 w-3" />
              {resource.views}
            </div>
            <div className="flex items-center gap-1">
              <Download className="h-3 w-3" />
              {resource.downloads}
            </div>
            {resource.size && (
              <span>{resource.size}</span>
            )}
            {resource.duration && (
              <span>{resource.duration}</span>
            )}
          </div>
          
          <div className="flex flex-wrap gap-1">
            {resource.tags.slice(0, 3).map((tag) => (
              <Badge key={tag} variant="secondary" className="text-xs">
                {tag}
              </Badge>
            ))}
          </div>
          
          <Separator />
          
          <div className="flex items-center justify-between">
            <Badge variant="outline" className="text-xs capitalize">
              {resource.type}
            </Badge>
            <Button size="sm" variant="ghost" className="text-blue-600 hover:text-blue-700">
              Open
              <ChevronRight className="h-3 w-3 ml-1" />
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function getResourceIcon(type: string) {
  switch (type) {
    case 'document': return FileText;
    case 'video': return Video;
    case 'assignment': return BookOpen;
    case 'quiz': return MessageSquare;
    case 'presentation': return Play;
    case 'link': return ExternalLink;
    default: return FileText;
  }
}

function getResourceColor(type: string) {
  switch (type) {
    case 'document': return 'from-blue-500 to-blue-600';
    case 'video': return 'from-red-500 to-red-600';
    case 'assignment': return 'from-green-500 to-green-600';
    case 'quiz': return 'from-purple-500 to-purple-600';
    case 'presentation': return 'from-orange-500 to-orange-600';
    case 'link': return 'from-indigo-500 to-indigo-600';
    default: return 'from-gray-500 to-gray-600';
  }
}
