import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet-async";
import { useAuth } from "@/hooks/useAuth";
import { usePaginated } from "@/hooks/usePaginated";
import { useLearningResources, useDownloadResource, usePreviewResource, useDeleteLearningResource, useUpdateLearningResource } from "@/features/learning-resources/hooks/use-learning-resources";
import toast from "react-hot-toast";
import { 
  BookOpen, 
  FileText, 
  Video, 
  Download, 
  Search, 
  Plus, 
  Grid3X3, 
  List, 
  Star, 
  Eye, 
  MoreVertical, 
  ChevronRight, 
  Share2,
  Edit,
  Trash2
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuTrigger 
} from "@/components/ui/dropdown-menu";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";

// Types - Use the actual LearningResource type from the API
type ClassResource = {
  id: number;
  title: string;
  description: string;
  type: string;
  url?: string;
  createdAt: string;
  // Actual API response structure
  classIds?: number[];
  teacherIds?: number[];
  courseIds?: number[];
  // View and download counters
  viewCount?: number;
  downloadCount?: number;
};

const LearningSpace = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isStudent = user?.role === 'STUDENT';
  const isTeacher = user?.role === 'TEACHER';
  
  // Helper function to check if current user can edit/delete a resource
  const canEditResource = (resource: ClassResource) => {
    return isTeacher && resource.teacherIds?.includes(user?.id || 0);
  };

  // State
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedClass, setSelectedClass] = useState<number | null>(null);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  // Get classes data
  const { data: classesData } = usePaginated(
    isStudent 
      ? '/v1/students/me/classes'
      : isTeacher 
      ? '/v1/classes/teacher/me'
      : '/v1/classes',
    "user-classes",
    50
  );

  // Add a refresh trigger to force cache invalidation
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  
  // Get learning resources with filters
  const resourcesQuery = useLearningResources({
    size: 20,
    search: searchQuery || undefined,
    classId: selectedClass || undefined,
    _refresh: refreshTrigger, // Force cache invalidation when this changes
  });

  const myClasses = classesData?.data || [];
  const classResources = resourcesQuery.data?.data || [];

  // Download hook
  // Download and preview mutations
  const downloadMutation = useDownloadResource();
  const previewMutation = usePreviewResource();
  
  // Delete and update mutations
  const deleteMutation = useDeleteLearningResource();
  const updateMutation = useUpdateLearningResource();
  
  // Edit modal state
  const [editingResource, setEditingResource] = useState<ClassResource | null>(null);
  const [editForm, setEditForm] = useState({
    title: '',
    description: '',
    classIds: [] as number[],
    isPublic: true
  });

  // No need for frontend filtering - backend already filters by classId
  // Only filter by search query since backend handles class filtering
  const filteredResources = classResources.filter((resource: ClassResource) => {
    const matchesSearch = searchQuery === '' || 
                         resource.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         resource.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesSearch;
  });

  // Helper function to get class name by ID
  const getClassNameById = (classId: number) => {
    const classInfo = myClasses.find((cls: any) => cls.id === classId);
    return classInfo?.name || `Class ${classId}`;
  };

  // Helper function to get class names for a resource
  const getResourceClassNames = (resource: ClassResource) => {
    if (!resource.classIds || resource.classIds.length === 0) {
      return ['All Classes'];
    }
    return resource.classIds.map(classId => getClassNameById(classId));
  };

  // Handle upload button click
  const handleUploadClick = () => {
    navigate('/teacher/upload-resource');
  };

  // Handle download
  const handleDownload = async (resource: ClassResource) => {
    if (!resource.url) {
      toast.error("No file available for download");
      return;
    }

    try {
      // Extract filename from URL
      const filename = resource.url.split('/').pop();
      if (!filename) {
        toast.error("Invalid file URL");
        return;
      }

      console.log('Download Debug:', {
        resourceUrl: resource.url,
        filename,
        downloadEndpoint: `/v1/learning-resources/files/${filename}`
      });

      // Show loading toast
      const loadingToast = toast.loading("Downloading file...");

      try {
        const blob = await downloadMutation.mutateAsync(filename);
        
        console.log('Download blob debug:', {
          blob,
          blobType: typeof blob,
          blobSize: blob?.size,
          blobConstructor: blob?.constructor?.name
        });
        
        // Ensure we have a valid blob
        if (!blob || !(blob instanceof Blob)) {
          throw new Error('Invalid blob received from server');
        }
        
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        toast.success(`Downloaded: ${resource.title}`);
        // Wait a bit for backend to process the increment, then refetch
        setTimeout(() => {
          setRefreshTrigger(prev => prev + 1); // Force cache invalidation
          resourcesQuery.refetch();
        }, 1000);
      } finally {
        toast.dismiss(loadingToast);
      }
    } catch (error) {
      console.error('Download error:', error);
      toast.error("Failed to download file. Make sure the backend server is running.");
    }
  };

  // Handle preview
  const handlePreview = async (resource: ClassResource) => {
    if (!resource.url) {
      toast.error("No file available for preview");
      return;
    }

    try {
      // Extract filename from URL
      const filename = resource.url.split('/').pop();
      if (!filename) {
        toast.error("Invalid file URL");
        return;
      }

      // Show loading toast
      const loadingToast = toast.loading("Loading preview...");

      try {
        // Preview the file as blob using authenticated request (increments view count)
        const blob = await previewMutation.mutateAsync(filename);
        
        console.log('Preview blob debug:', {
          blob,
          blobType: typeof blob,
          blobSize: blob?.size,
          blobConstructor: blob?.constructor?.name,
          isBlob: blob instanceof Blob
        });
        
        // Ensure we have a valid blob
        if (!blob || !(blob instanceof Blob)) {
          console.error('Invalid blob received:', blob);
          throw new Error(`Invalid blob received from server. Got: ${typeof blob}`);
        }
        
        // Create a blob URL for preview
        const blobUrl = window.URL.createObjectURL(blob);
        
        // Open the blob URL in a new tab
        const newWindow = window.open(blobUrl, '_blank');
        
        if (!newWindow) {
          toast.error("Please allow popups to preview files");
        } else {
          toast.success("Preview opened in new tab");
          // Wait a bit for backend to process the increment, then refetch
          setTimeout(() => {
            setRefreshTrigger(prev => prev + 1); // Force cache invalidation
            resourcesQuery.refetch();
          }, 1000);
        }
        
        // Clean up the blob URL after a delay to allow the browser to load it
        setTimeout(() => {
          window.URL.revokeObjectURL(blobUrl);
        }, 5000);
        
      } finally {
        toast.dismiss(loadingToast);
      }
      
    } catch (error) {
      console.error('Preview error:', error);
      toast.error("Failed to preview file. Make sure the backend server is running.");
    }
  };

  // Handle delete resource
  const handleDelete = async (resource: ClassResource) => {
    if (!window.confirm(`Are you sure you want to delete "${resource.title}"? This action cannot be undone.`)) {
      return;
    }

    try {
      await deleteMutation.mutateAsync(resource.id);
      toast.success(`Resource "${resource.title}" deleted successfully`);
      setRefreshTrigger(prev => prev + 1);
      resourcesQuery.refetch();
    } catch (error) {
      console.error('Delete error:', error);
      toast.error("Failed to delete resource. You can only delete resources you created.");
    }
  };

  // Handle edit resource
  const handleEdit = (resource: ClassResource) => {
    setEditingResource(resource);
    setEditForm({
      title: resource.title,
      description: resource.description,
      classIds: resource.classIds || [],
      isPublic: true // Default value, you might want to get this from the resource
    });
  };

  // Handle save edit
  const handleSaveEdit = async () => {
    if (!editingResource) return;

    try {
      await updateMutation.mutateAsync({
        id: editingResource.id,
        data: editForm
      });
      toast.success(`Resource "${editForm.title}" updated successfully`);
      setEditingResource(null);
      setRefreshTrigger(prev => prev + 1);
      resourcesQuery.refetch();
    } catch (error) {
      console.error('Update error:', error);
      toast.error("Failed to update resource. You can only edit resources you created.");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-indigo-50/20">
      <Helmet>
        <title>{isStudent ? "My Learning Space" : "Learning Space"} - School Management System</title>
        <meta name="description" content="Access learning resources, assignments, and course materials" />
      </Helmet>

      <div className="container mx-auto px-4 py-6 sm:px-6 lg:px-8 max-w-7xl">
        {/* Header */}
        <div className="mb-6 sm:mb-8">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <h1 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-slate-900 mb-2">
                {isStudent ? "My Learning Space" : "Learning Resources"}
              </h1>
              <p className="text-slate-600 text-sm sm:text-base">
                {isStudent 
                  ? "Access your course materials, assignments, and resources" 
                  : "Manage and share educational content with your students"
                }
              </p>
            </div>
            
            {isTeacher && (
              <Button 
                onClick={handleUploadClick}
                className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white shadow-lg hover:shadow-xl transition-all duration-200 flex items-center gap-2"
              >
                <Plus className="h-4 w-4" />
                Upload Resource
              </Button>
            )}
          </div>
        </div>

        {/* Search and Filters */}
        <div className="mb-6 sm:mb-8">
          <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
            <div className="flex-1 w-full sm:max-w-md">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                <Input
                  placeholder="Search resources, assignments..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10 bg-white/80 backdrop-blur-sm border-slate-200 focus:border-blue-300 focus:ring-blue-200"
                />
              </div>
            </div>
            
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1 bg-white/80 backdrop-blur-sm border border-slate-200 rounded-lg p-1">
                <Button
                  variant={viewMode === 'grid' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setViewMode('grid')}
                  className="h-8 w-8 p-0"
                >
                  <Grid3X3 className="h-4 w-4" />
                </Button>
                <Button
                  variant={viewMode === 'list' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setViewMode('list')}
                  className="h-8 w-8 p-0"
                >
                  <List className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>

        {/* All Resources Section */}
        <div className="space-y-4 sm:space-y-6">
            {/* Class Filter Pills */}
            <div className="flex items-center gap-2 flex-wrap">
              <Button
                variant={selectedClass === null ? "default" : "outline"}
                size="sm"
                onClick={() => setSelectedClass(null)}
                className={`rounded-full ${selectedClass === null ? 'bg-blue-600 text-white hover:bg-blue-700' : 'hover:bg-blue-50'}`}
              >
                All Classes
              </Button>
              {myClasses.map((classInfo: any) => (
                <Button
                  key={classInfo.id}
                  variant={selectedClass === classInfo.id ? "default" : "outline"}
                  size="sm"
                  onClick={() => setSelectedClass(classInfo.id)}
                  className={`rounded-full ${selectedClass === classInfo.id ? 'bg-blue-600 text-white hover:bg-blue-700' : 'hover:bg-blue-50'}`}
                >
                  {classInfo.name}
                </Button>
              ))}
            </div>

            {/* Resources Grid/List */}
            {viewMode === 'grid' ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
                {filteredResources.map((resource) => {
                  const IconComponent = resource.type === 'VIDEO' ? Video : FileText;
                  
                  return (
                    <Card key={resource.id} className="group hover:shadow-xl transition-all duration-300 border-slate-200/60 bg-white/95 backdrop-blur-sm hover:border-blue-300/60">
                      <CardContent className="p-4 sm:p-6">
                        <div className="space-y-4">
                          <div className="flex items-start justify-between">
                            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg">
                              <IconComponent className="h-6 w-6 text-white" />
                            </div>
                            
                            {isStudent ? (
                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <Button variant="ghost" size="sm" className="opacity-0 group-hover:opacity-100 transition-opacity">
                                    <MoreVertical className="h-4 w-4" />
                                  </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                  <DropdownMenuItem onClick={() => handlePreview(resource)}>
                                    <Eye className="h-4 w-4 mr-2" />
                                    Preview
                                  </DropdownMenuItem>
                                  <DropdownMenuItem onClick={() => handleDownload(resource)}>
                                    <Download className="h-4 w-4 mr-2" />
                                    Download
                                  </DropdownMenuItem>
                                  <DropdownMenuItem>
                                    <Star className="h-4 w-4 mr-2" />
                                    Bookmark
                                  </DropdownMenuItem>
                                  {canEditResource(resource) && (
                                    <>
                                      <DropdownMenuItem onClick={() => handleEdit(resource)}>
                                        <Edit className="h-4 w-4 mr-2" />
                                        Edit
                                      </DropdownMenuItem>
                                      <DropdownMenuItem onClick={() => handleDelete(resource)} className="text-red-600 hover:text-red-700">
                                        <Trash2 className="h-4 w-4 mr-2" />
                                        Delete
                                      </DropdownMenuItem>
                                    </>
                                  )}
                                </DropdownMenuContent>
                              </DropdownMenu>
                            ) : (
                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <Button variant="ghost" size="sm" className="opacity-0 group-hover:opacity-100 transition-opacity">
                                    <MoreVertical className="h-4 w-4" />
                                  </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                  <DropdownMenuItem onClick={() => handlePreview(resource)}>
                                    <Eye className="h-4 w-4 mr-2" />
                                    Preview
                                  </DropdownMenuItem>
                                  <DropdownMenuItem onClick={() => handleDownload(resource)}>
                                    <Download className="h-4 w-4 mr-2" />
                                    Download
                                  </DropdownMenuItem>
                                  <DropdownMenuItem>
                                    <Share2 className="h-4 w-4 mr-2" />
                                    Share
                                  </DropdownMenuItem>
                                  {canEditResource(resource) && (
                                    <>
                                      <DropdownMenuItem onClick={() => handleEdit(resource)}>
                                        <Edit className="h-4 w-4 mr-2" />
                                        Edit
                                      </DropdownMenuItem>
                                      <DropdownMenuItem onClick={() => handleDelete(resource)} className="text-red-600 hover:text-red-700">
                                        <Trash2 className="h-4 w-4 mr-2" />
                                        Delete
                                      </DropdownMenuItem>
                                    </>
                                  )}
                                </DropdownMenuContent>
                              </DropdownMenu>
                            )}
                          </div>
                          
                          <div className="space-y-2">
                            <CardTitle className="text-lg font-semibold text-slate-900 group-hover:text-blue-900 transition-colors line-clamp-2">
                              {resource.title}
                            </CardTitle>
                            <p className="text-sm text-slate-600 line-clamp-2">
                              {resource.description}
                            </p>
                          </div>
                          
                          <div className="space-y-3">
                            <div className="flex flex-wrap gap-1">
                              {getResourceClassNames(resource).map((className, index) => (
                                <Badge key={index} variant="outline" className="text-xs bg-blue-50 text-blue-700 border-blue-200">
                                  {className}
                                </Badge>
                              ))}
                            </div>
                            
                            {/* View and Download Counts */}
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-3 text-xs text-slate-500">
                                <div className="flex items-center gap-1">
                                  <Eye className="h-3 w-3" />
                                  <span>{resource.viewCount || 0}</span>
                                </div>
                                <div className="flex items-center gap-1">
                                  <Download className="h-3 w-3" />
                                  <span>{resource.downloadCount || 0}</span>
                                </div>
                              </div>
                              
                              {isStudent ? (
                                <Button 
                                  size="sm" 
                                  onClick={() => handlePreview(resource)}
                                  className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white shadow-md hover:shadow-lg transition-all duration-200"
                                >
                                  <Eye className="h-3 w-3 mr-1" />
                                  View
                                </Button>
                              ) : (
                                <Button 
                                  size="sm" 
                                  variant="ghost" 
                                  onClick={() => handlePreview(resource)}
                                  className="text-blue-600 hover:text-blue-700"
                                >
                                  Open
                                  <ChevronRight className="h-3 w-3 ml-1" />
                                </Button>
                              )}
                            </div>
                          </div>
                          
                          {/* Student-specific progress indicator */}
                          {isStudent && resource.type === 'DOCUMENT' && (
                            <div className="mt-3 pt-3 border-t border-slate-100">
                              <div className="flex items-center justify-between text-xs">
                                <span className="text-slate-500">Progress</span>
                                <span className="font-medium text-green-600">Completed</span>
                              </div>
                            </div>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            ) : (
              <div className="space-y-3">
                {filteredResources.map((resource) => {
                  const IconComponent = resource.type === 'VIDEO' ? Video : FileText;
                  
                  return (
                    <Card key={resource.id} className="group hover:shadow-lg transition-all duration-200 border-slate-200/60 bg-white/95 backdrop-blur-sm">
                      <CardContent className="p-4">
                        <div className="flex items-center gap-4">
                          <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-md flex-shrink-0">
                            <IconComponent className="h-6 w-6 text-white" />
                          </div>
                          
                          <div className="flex-1 min-w-0">
                            <h3 className="font-semibold text-slate-900 group-hover:text-blue-900 transition-colors line-clamp-1">
                              {resource.title}
                            </h3>
                            <p className="text-sm text-slate-600 line-clamp-1 mt-1">
                              {resource.description}
                            </p>
                            <div className="flex items-center gap-4 mt-2 text-xs text-slate-500">
                              <div className="flex flex-wrap gap-1">
                                {getResourceClassNames(resource).map((className, index) => (
                                  <span key={index} className="bg-blue-50 text-blue-700 px-2 py-1 rounded-full text-xs">
                                    {className}
                                  </span>
                                ))}
                              </div>
                              <span>•</span>
                              <span>Teacher</span>
                              <span>•</span>
                              <span>{new Date(resource.createdAt).toLocaleDateString()}</span>
                            </div>
                          </div>
                          
                          <div className="flex items-center gap-4 text-xs text-slate-500">
                            <div className="flex items-center gap-1">
                              <Eye className="h-3 w-3" />
                              {resource.viewCount || 0}
                            </div>
                            <div className="flex items-center gap-1">
                              <Download className="h-3 w-3" />
                              {resource.downloadCount || 0}
                            </div>
                          </div>
                          
                          {isStudent ? (
                            <div className="flex items-center gap-2">
                              <Button variant="ghost" size="sm" className="text-yellow-600 hover:text-yellow-700">
                                <Star className="h-4 w-4" />
                              </Button>
                              <Button 
                                size="sm" 
                                onClick={() => handlePreview(resource)}
                                className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white"
                              >
                                <Eye className="h-3 w-3 mr-1" />
                                View
                              </Button>
                            </div>
                          ) : (
                            <Button 
                              size="sm" 
                              variant="ghost" 
                              onClick={() => handlePreview(resource)}
                              className="text-blue-600 hover:text-blue-700"
                            >
                              Open
                            </Button>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            )}

            {/* Empty State */}
            {filteredResources.length === 0 && (
              <div className="text-center py-12">
                <div className="w-24 h-24 mx-auto mb-4 rounded-full bg-gradient-to-br from-slate-100 to-slate-200 flex items-center justify-center">
                  <BookOpen className="h-12 w-12 text-slate-400" />
                </div>
                <h3 className="text-lg font-semibold text-slate-900 mb-2">No resources found</h3>
                <p className="text-slate-600 mb-6">
                  {selectedClass 
                    ? "No resources available for the selected class." 
                    : "No resources match your search criteria."
                  }
                </p>
                {isTeacher && (
                  <Button 
                    onClick={handleUploadClick}
                    className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white"
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    Upload First Resource
                  </Button>
                )}
              </div>
            )}
        </div>
      </div>

      {/* Edit Resource Modal */}
      <Dialog open={!!editingResource} onOpenChange={(open) => !open && setEditingResource(null)}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto bg-white border border-gray-200 shadow-2xl">
          <DialogHeader className="pb-4 border-b border-gray-100">
            <DialogTitle className="text-xl font-semibold text-gray-900">Edit Resource</DialogTitle>
          </DialogHeader>
          
          <div className="space-y-6 py-4">
            <div className="space-y-2">
              <Label htmlFor="title" className="text-sm font-medium text-gray-700">Title</Label>
              <Input
                id="title"
                value={editForm.title}
                onChange={(e) => setEditForm(prev => ({ ...prev, title: e.target.value }))}
                placeholder="Resource title"
                className="w-full border-gray-300 focus:border-blue-500 focus:ring-blue-500"
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="description" className="text-sm font-medium text-gray-700">Description</Label>
              <Textarea
                id="description"
                value={editForm.description}
                onChange={(e) => setEditForm(prev => ({ ...prev, description: e.target.value }))}
                placeholder="Resource description"
                rows={3}
                className="w-full border-gray-300 focus:border-blue-500 focus:ring-blue-500"
              />
            </div>
            
            <div className="space-y-3">
              <Label className="text-sm font-medium text-gray-700">Assign to Classes</Label>
              <div className="border border-gray-200 rounded-lg p-4 bg-gray-50">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-40 overflow-y-auto">
                  {myClasses.map((cls: any) => (
                    <div key={cls.id} className="flex items-center space-x-3 p-2 rounded-md hover:bg-white transition-colors">
                      <Checkbox
                        id={`class-${cls.id}`}
                        checked={editForm.classIds.includes(cls.id)}
                        onCheckedChange={(checked) => {
                          if (checked) {
                            setEditForm(prev => ({
                              ...prev,
                              classIds: [...prev.classIds, cls.id]
                            }));
                          } else {
                            setEditForm(prev => ({
                              ...prev,
                              classIds: prev.classIds.filter(id => id !== cls.id)
                            }));
                          }
                        }}
                        className="border-gray-300"
                      />
                      <Label htmlFor={`class-${cls.id}`} className="text-sm text-gray-700 cursor-pointer">
                        {cls.name}
                      </Label>
                    </div>
                  ))}
                </div>
              </div>
            </div>
            
            <div className="flex items-center space-x-3 p-3 bg-blue-50 rounded-lg border border-blue-200">
              <Checkbox
                id="isPublic"
                checked={editForm.isPublic}
                onCheckedChange={(checked) => setEditForm(prev => ({ ...prev, isPublic: !!checked }))}
                className="border-blue-300"
              />
              <Label htmlFor="isPublic" className="text-sm text-blue-800 cursor-pointer">
                Make this resource public (visible to all students)
              </Label>
            </div>
          </div>
          
          <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100">
            <Button 
              variant="outline" 
              onClick={() => setEditingResource(null)}
              className="px-6 py-2 border-gray-300 text-gray-700 hover:bg-gray-50"
            >
              Cancel
            </Button>
            <Button 
              onClick={handleSaveEdit} 
              disabled={!editForm.title.trim()}
              className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Save Changes
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default LearningSpace;