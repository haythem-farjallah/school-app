import React, { useState } from "react";
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
      ? `/v1/enrollments/student/${user?.id}`  // Use enrollments endpoint for students
      : isTeacher 
      ? '/v1/classes/teacher/me'
      : '/v1/classes',
    "user-classes",
    50
  );

  // Get all classes for mapping class names to IDs
  const { data: allClassesData } = usePaginated(
    '/v1/classes',
    "all-classes",
    100
  );

  // Transform enrollment data to class data for students
  const myClasses = React.useMemo(() => {
    if (isStudent) {
      return ((classesData?.data || []) as any[]).map((enrollment: any) => {
        // Find the class ID by matching class name from all classes
        const matchingClass = ((allClassesData?.data || []) as any[]).find((cls: any) => 
          cls.name === enrollment.className
        );
        return {
          id: matchingClass?.id || null,
          name: enrollment.className,
          level: matchingClass?.level || null
        };
      }).filter((cls: any) => cls.id !== null); // Filter out classes without ID
    }
    return (classesData?.data || []) as any[];
  }, [isStudent, classesData?.data, allClassesData?.data]);

  // For students, automatically set selectedClass to their enrolled class
  const studentClassId = isStudent && myClasses.length > 0 ? (myClasses[0] as any).id : null;
  
  // Debug logging for students
  if (isStudent) {
    console.log('🎓 Student Learning Space Debug:', {
      userId: user?.id,
      enrollmentData: classesData?.data,
      allClassesData: allClassesData?.data,
      myClasses,
      studentClassId,
      effectiveClassId: studentClassId
    });
  }

  // Get learning resources with filters
  // For students, always filter by their enrolled class
  const effectiveClassId = isStudent ? studentClassId : selectedClass;
  
  const resourcesQuery = useLearningResources({
    size: 100,
    search: searchQuery || undefined,
    classId: effectiveClassId || undefined,
  });
  
  const classResources = React.useMemo(() => {
    const resources = resourcesQuery.data?.data || [];
    console.log('🔍 Frontend received resources:', resources.map(r => ({
      id: r.id,
      title: r.title,
      viewCount: r.viewCount,
      downloadCount: r.downloadCount
    })));
    return resources;
  }, [resourcesQuery.data?.data]);

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

  // Frontend filtering - for students, only show resources that include their class
  const filteredResources = classResources.filter((resource: ClassResource) => {
    const matchesSearch = searchQuery === '' || 
                         resource.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         resource.description.toLowerCase().includes(searchQuery.toLowerCase());
    
    // For students, only show resources that include their class in the tags
    if (isStudent && studentClassId) {
      // Show resources that include the student's class (can have other classes too)
      const includesStudentClass = resource.classIds && 
                                  resource.classIds.includes(studentClassId);
      return matchesSearch && includesStudentClass;
    }
    
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
    
    // Always show all classes the resource belongs to (don't change the tags)
    return resource.classIds.map(classId => getClassNameById(classId));
  };

  // Debug logging for API call (after function definitions)
  React.useEffect(() => {
    if (isStudent && classResources.length > 0) {
      console.log('📚 Learning Resources Debug:', {
        classId: effectiveClassId,
        studentClassId,
        totalResources: classResources.length,
        filteredResources: filteredResources.length
      });
      
      // Debug each resource's class associations and filtering
      classResources.forEach((resource: any) => {
        const includesStudentClass = resource.classIds && 
                                    resource.classIds.includes(studentClassId);
        console.log(`📋 Resource "${resource.title}":`, {
          id: resource.id,
          originalClassIds: resource.classIds,
          classCount: resource.classIds?.length || 0,
          includesStudentClass,
          willShow: includesStudentClass
        });
      });
    }
  }, [isStudent, classResources, filteredResources, studentClassId, effectiveClassId]);

  // Handle upload button click
  const handleUploadClick = () => {
    navigate('/teacher/upload-resource');
  };

  // Handle open - works for both students and teachers
  const handleOpen = async (resource: ClassResource) => {
    if (!resource.url) {
      toast.error("No file available");
      return;
    }

    try {
      const filename = resource.url.split('/').pop();
      if (!filename) {
        toast.error("Invalid file URL");
        return;
      }

      console.log(`🔥 Opening resource: ${resource.title}, current view count: ${resource.viewCount}`);
      
      // Use preview endpoint to increment view count and get file
      const blob = await previewMutation.mutateAsync(filename);
      
      console.log(`✅ Preview API call completed for: ${filename}`);
      
      // Open the file in new tab
      const blobUrl = window.URL.createObjectURL(blob);
      window.open(blobUrl, '_blank');
      
      // Clean up
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 5000);
      
      // Force immediate refresh to show updated view count
      resourcesQuery.refetch();
      
    } catch (error) {
      console.error('Open error:', error);
      toast.error("Failed to open file");
    }
  };

  // Handle download
  const handleDownload = async (resource: ClassResource) => {
    console.log(`💾 Downloading resource: ${resource.title}, current download count: ${resource.downloadCount}`);
    
    if (!resource.url) {
      toast.error("No file available for download");
      return;
    }

    try {
      const filename = resource.url.split('/').pop();
      if (!filename) {
        toast.error("Invalid file URL");
        return;
      }

      console.log(`📁 Calling download endpoint: /v1/learning-resources/files/${filename}`);
      
      // Reset mutation state before calling to ensure it can be called multiple times
      downloadMutation.reset();
      
      const blob = await downloadMutation.mutateAsync(filename);
      
      console.log(`✅ Download API call completed for: ${filename}`);
      
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      toast.success("File downloaded");
      // Force immediate refresh to show updated download count
      console.log('🔄 Refreshing data to get updated download count...');
      await resourcesQuery.refetch();
      console.log('✅ Data refreshed after download');
      
    } catch (error) {
      console.error('Download error:', error);
      toast.error("Failed to download file");
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
      resourcesQuery.refetch();
    } catch (error) {
      console.error('Update error:', error);
      toast.error("Failed to update resource. You can only edit resources you created.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Helmet>
        <title>{isStudent ? "My Learning Space" : "Learning Resources"} - School Management System</title>
        <meta name="description" content="Access learning resources, assignments, and course materials" />
      </Helmet>

      <div className="container mx-auto px-4 py-6 sm:px-6 lg:px-8 max-w-7xl">
        {/* Header Section */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-6">
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">
                {isStudent ? "My Learning Space" : "Learning Resources"}
              </h1>
              <p className="text-gray-600 mt-1">
                {isStudent 
                  ? "Access your course materials and resources" 
                  : "Manage and share educational content"
                }
              </p>
            </div>
            
            {isTeacher && (
              <Button 
                onClick={handleUploadClick}
                className="bg-blue-600 hover:bg-blue-700 text-white shadow-sm hover:shadow transition-all flex items-center gap-2"
              >
                <Plus className="h-4 w-4" />
                Upload Resource
              </Button>
            )}
          </div>
          
          {/* Search and View Controls */}
          <div className="mt-6 flex flex-col sm:flex-row gap-3">
            <div className="flex-1 max-w-lg">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="Search resources..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10 bg-gray-50 border-gray-300 focus:border-blue-500 focus:ring-blue-500"
                />
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              {/* View Mode Toggle */}
              <div className="flex bg-gray-100 rounded-lg p-1">
                <Button
                  variant={viewMode === 'grid' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setViewMode('grid')}
                  className={`h-8 px-3 ${viewMode === 'grid' ? 'bg-white shadow-sm' : 'hover:bg-gray-200'}`}
                >
                  <Grid3X3 className="h-4 w-4" />
                </Button>
                <Button
                  variant={viewMode === 'list' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setViewMode('list')}
                  className={`h-8 px-3 ${viewMode === 'list' ? 'bg-white shadow-sm' : 'hover:bg-gray-200'}`}
                >
                  <List className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </div>

        {/* Main Content Section */}
        <div className="space-y-6">
            {/* Class Filter Pills - Only show for teachers and admins */}
            {!isStudent && myClasses.length > 0 && (
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
                <h3 className="text-sm font-medium text-gray-700 mb-3">Filter by Class</h3>
                <div className="flex items-center gap-2 flex-wrap">
                  <Button
                    variant={selectedClass === null ? "default" : "outline"}
                    size="sm"
                    onClick={() => setSelectedClass(null)}
                    className={`rounded-full ${selectedClass === null ? 'bg-blue-600 text-white hover:bg-blue-700' : 'bg-white hover:bg-gray-50'}`}
                  >
                    All Classes
                  </Button>
                  {myClasses.map((classInfo: any) => (
                    <Button
                      key={classInfo.id}
                      variant={selectedClass === classInfo.id ? "default" : "outline"}
                      size="sm"
                      onClick={() => setSelectedClass(classInfo.id)}
                      className={`rounded-full ${selectedClass === classInfo.id ? 'bg-blue-600 text-white hover:bg-blue-700' : 'bg-white hover:bg-gray-50'}`}
                    >
                      {classInfo.name}
                    </Button>
                  ))}
                </div>
              </div>
            )}

            {/* Student Class Info */}
            {isStudent && myClasses.length > 0 && (
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <div className="flex items-center gap-2">
                  <BookOpen className="h-5 w-5 text-blue-600" />
                  <span className="font-medium text-blue-900">
                    Resources for {(myClasses[0] as any).name}
                  </span>
                </div>
              </div>
            )}

            {/* Resources Grid/List */}
            {filteredResources.length === 0 ? (
              <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-12">
                <div className="text-center">
                  <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
                    <BookOpen className="h-8 w-8 text-gray-400" />
                  </div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">No Resources Found</h3>
                  <p className="text-gray-600 max-w-sm mx-auto">
                    {isStudent 
                      ? "No learning resources are currently available for your class." 
                      : searchQuery 
                        ? "No resources match your search criteria. Try different keywords."
                        : "No learning resources found. Upload your first resource to get started."
                    }
                  </p>
                  {isTeacher && !searchQuery && (
                    <Button 
                      onClick={handleUploadClick}
                      className="mt-6 bg-blue-600 hover:bg-blue-700 text-white"
                    >
                      <Plus className="h-4 w-4 mr-2" />
                      Upload First Resource
                    </Button>
                  )}
                </div>
              </div>
            ) : viewMode === 'grid' ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 sm:gap-6">
                {filteredResources.map((resource) => {
                  const IconComponent = resource.type === 'VIDEO' ? Video : FileText;
                  
                  return (
                    <Card key={resource.id} className="group hover:shadow-lg transition-all duration-300 border-gray-200 bg-white overflow-hidden">
                      <CardContent className="p-0">
                        <div className="p-5">
                          {/* Header with Icon and Menu */}
                          <div className="flex items-start justify-between mb-4">
                            <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-md">
                              <IconComponent className="h-6 w-6 text-white" />
                            </div>
                            
                            {/* Only show dropdown menu for teachers */}
                            {isTeacher && canEditResource(resource) && (
                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <Button variant="ghost" size="sm" className="opacity-0 group-hover:opacity-100 transition-opacity">
                                    <MoreVertical className="h-4 w-4" />
                                  </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                  <DropdownMenuItem onClick={() => handleEdit(resource)}>
                                    <Edit className="h-4 w-4 mr-2" />
                                    Edit
                                  </DropdownMenuItem>
                                  <DropdownMenuItem onClick={() => handleDelete(resource)} className="text-red-600 hover:text-red-700">
                                    <Trash2 className="h-4 w-4 mr-2" />
                                    Delete
                                  </DropdownMenuItem>
                                </DropdownMenuContent>
                              </DropdownMenu>
                            )}
                          </div>
                          
                          {/* Title and Description */}
                          <div className="mb-4">
                            <h3 className="text-base font-semibold text-gray-900 line-clamp-2 mb-1">
                              {resource.title}
                            </h3>
                            <p className="text-sm text-gray-600 line-clamp-2">
                              {resource.description}
                            </p>
                          </div>
                          
                          {/* Class Badges */}
                          <div className="flex flex-wrap gap-1.5 mb-4">
                            {getResourceClassNames(resource).map((className, index) => (
                              <Badge key={index} variant="secondary" className="text-xs bg-blue-50 text-blue-700 border-0">
                                {className}
                              </Badge>
                            ))}
                          </div>
                          
                          {/* Stats */}
                          <div className="flex items-center justify-between text-xs text-gray-500 mb-4">
                            <div className="flex items-center gap-3">
                              <span className="flex items-center gap-1">
                                <Eye className="h-3.5 w-3.5" />
                                {resource.viewCount || 0}
                              </span>
                              <span className="flex items-center gap-1">
                                <Download className="h-3.5 w-3.5" />
                                {resource.downloadCount || 0}
                              </span>
                            </div>
                            <span>{new Date(resource.createdAt).toLocaleDateString()}</span>
                          </div>
                        </div>
                        
                        {/* Action Buttons - Full Width Bottom Section */}
                        <div className="border-t border-gray-100 p-4 bg-gray-50">
                          {isStudent ? (
                            <div className="flex gap-2">
                              <Button 
                                size="sm" 
                                onClick={() => handleOpen(resource)}
                                className="flex-1 bg-blue-600 hover:bg-blue-700 text-white h-9"
                              >
                                <Eye className="h-4 w-4 mr-1.5" />
                                Open ({resource.viewCount || 0})
                              </Button>
                              <Button 
                                size="sm" 
                                variant="outline"
                                onClick={() => handleDownload(resource)}
                                className="flex-1 border-gray-300 hover:bg-white h-9"
                              >
                                <Download className="h-4 w-4 mr-1.5" />
                                Download ({resource.downloadCount || 0})
                              </Button>
                            </div>
                          ) : (
                            <Button 
                              size="sm" 
                              onClick={() => handleOpen(resource)}
                              className="w-full bg-blue-600 hover:bg-blue-700 text-white h-9"
                            >
                              Open Resource
                            </Button>
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
                    <Card key={resource.id} className="group hover:shadow-md transition-all duration-200 border-gray-200 bg-white">
                      <CardContent className="p-4">
                        <div className="flex items-center gap-4">
                          {/* Icon */}
                          <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-md flex-shrink-0">
                            <IconComponent className="h-6 w-6 text-white" />
                          </div>
                          
                          {/* Content */}
                          <div className="flex-1 min-w-0">
                            <h3 className="font-semibold text-gray-900 line-clamp-1">
                              {resource.title}
                            </h3>
                            <p className="text-sm text-gray-600 line-clamp-1 mt-1">
                              {resource.description}
                            </p>
                            <div className="flex items-center gap-2 mt-2">
                              {getResourceClassNames(resource).map((className, index) => (
                                <Badge key={index} variant="secondary" className="text-xs bg-blue-50 text-blue-700 border-0">
                                  {className}
                                </Badge>
                              ))}
                            </div>
                          </div>
                          
                          {/* Stats */}
                          <div className="flex items-center gap-4 text-sm text-gray-500">
                            <span className="flex items-center gap-1">
                              <Eye className="h-4 w-4" />
                              {resource.viewCount || 0}
                            </span>
                            <span className="flex items-center gap-1">
                              <Download className="h-4 w-4" />
                              {resource.downloadCount || 0}
                            </span>
                          </div>
                          
                          {/* Action Buttons */}
                          {isStudent ? (
                            <div className="flex items-center gap-2">
                              <Button 
                                size="sm" 
                                onClick={() => handleOpen(resource)}
                                className="bg-blue-600 hover:bg-blue-700 text-white"
                              >
                                <Eye className="h-4 w-4 mr-1.5" />
                                Open
                              </Button>
                              <Button 
                                size="sm" 
                                variant="outline"
                                onClick={() => handleDownload(resource)}
                                className="border-gray-300 hover:bg-gray-50"
                              >
                                <Download className="h-4 w-4" />
                              </Button>
                            </div>
                          ) : (
                            <div className="flex items-center gap-2">
                              <Button 
                                size="sm" 
                                onClick={() => handleOpen(resource)}
                                className="bg-blue-600 hover:bg-blue-700 text-white"
                              >
                                Open
                              </Button>
                              {canEditResource(resource) && (
                                <DropdownMenu>
                                  <DropdownMenuTrigger asChild>
                                    <Button variant="ghost" size="sm">
                                      <MoreVertical className="h-4 w-4" />
                                    </Button>
                                  </DropdownMenuTrigger>
                                  <DropdownMenuContent align="end">
                                    <DropdownMenuItem onClick={() => handleEdit(resource)}>
                                      <Edit className="h-4 w-4 mr-2" />
                                      Edit
                                    </DropdownMenuItem>
                                    <DropdownMenuItem onClick={() => handleDelete(resource)} className="text-red-600">
                                      <Trash2 className="h-4 w-4 mr-2" />
                                      Delete
                                    </DropdownMenuItem>
                                  </DropdownMenuContent>
                                </DropdownMenu>
                              )}
                            </div>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
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