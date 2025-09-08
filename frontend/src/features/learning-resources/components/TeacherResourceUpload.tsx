import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import toast from "react-hot-toast";
import {
  Upload,
  FileText,
  Video,
  Image,
  Link,
  BookOpen,
  Users,
  Tag,
  X,
  Plus,
  Check,
  AlertCircle,
  Loader2,
  Eye
} from "lucide-react";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Separator } from "@/components/ui/separator";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

import { useAuth } from "@/hooks/useAuth";
import { usePaginated } from "@/hooks/usePaginated";
import { useMutationApi } from "@/hooks/useMutationApi";
import { http } from "@/lib/http";

// Types
interface ClassInfo {
  id: number;
  name: string;
  gradeLevel: string;
  section: string;
  studentCount?: number;
}

interface ResourceUploadData {
  file?: File;
  title: string;
  description: string;
  type: 'DOCUMENT' | 'VIDEO' | 'PRESENTATION' | 'ASSIGNMENT' | 'QUIZ' | 'OTHER';
  url?: string;
  thumbnailUrl?: string;
  duration?: number;
  isPublic: boolean;
  classIds: number[];
  tags: string[];
}

// Schema
const uploadSchema = z.object({
  title: z.string().min(1, "Title is required").max(200, "Title must be less than 200 characters"),
  description: z.string().min(1, "Description is required").max(1000, "Description must be less than 1000 characters"),
  type: z.enum(['DOCUMENT', 'VIDEO', 'PRESENTATION', 'ASSIGNMENT', 'QUIZ', 'OTHER']),
  url: z.string().url("Please enter a valid URL").optional().or(z.literal("")),
  thumbnailUrl: z.string().url("Please enter a valid thumbnail URL").optional().or(z.literal("")),
  duration: z.number().min(0, "Duration must be positive").optional(),
  isPublic: z.boolean().default(true),
  classIds: z.array(z.number()).min(1, "Please select at least one class"),
  tags: z.array(z.string()).default([]),
});

type UploadFormData = z.infer<typeof uploadSchema>;

function TeacherResourceUpload() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [uploadMode, setUploadMode] = useState<'file' | 'url'>('file');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [newTag, setNewTag] = useState("");
  const [selectedClasses, setSelectedClasses] = useState<number[]>([]);

  // Get teacher's classes
  const { data: classesData, isLoading: classesLoading } = usePaginated<ClassInfo>(
    `/v1/classes/teacher/${user?.id}`,
    "teacher-classes",
    50
  );

  const teacherClasses = classesData?.data || [];

  // Form setup
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<UploadFormData>({
    resolver: zodResolver(uploadSchema),
    defaultValues: {
      isPublic: true,
      classIds: [],
      tags: [],
      type: 'DOCUMENT',
    },
  });

  const watchedTags = watch("tags") || [];
  const watchedType = watch("type");
  const watchedClassIds = watch("classIds") || [];

  // Keep selectedClasses in sync with form state
  React.useEffect(() => {
    setSelectedClasses(watchedClassIds);
  }, [watchedClassIds]);

  // Upload mutations
  const uploadFileMutation = useMutationApi<any, FormData>(
    async (formData) => {
      const response = await http.post("/v1/learning-resources/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      return response.data;
    }
  );

  const createUrlResourceMutation = useMutationApi<any, UploadFormData>(
    async (data) => {
      const response = await http.post("/v1/learning-resources", data);
      return response.data;
    }
  );

  // Handlers
  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const addTag = () => {
    if (newTag.trim() && !watchedTags.includes(newTag.trim())) {
      setValue("tags", [...watchedTags, newTag.trim()]);
      setNewTag("");
    }
  };

  const removeTag = (tagToRemove: string) => {
    setValue("tags", watchedTags.filter(tag => tag !== tagToRemove));
  };

  const toggleClass = (classId: number) => {
    const currentClasses = watchedClassIds;
    const newClasses = currentClasses.includes(classId)
      ? currentClasses.filter(id => id !== classId)
      : [...currentClasses, classId];
    
    setValue("classIds", newClasses);
  };

  const onSubmit = async (data: UploadFormData) => {
    try {
      if (uploadMode === 'file') {
        if (!selectedFile) {
          toast.error("Please select a file to upload");
          return;
        }

        const formData = new FormData();
        formData.append("file", selectedFile);
        formData.append("title", data.title);
        formData.append("description", data.description);
        formData.append("type", data.type);
        formData.append("isPublic", data.isPublic.toString());
        
        if (data.thumbnailUrl) formData.append("thumbnailUrl", data.thumbnailUrl);
        if (data.duration) formData.append("duration", data.duration.toString());
        if (data.classIds.length > 0) formData.append("classIds", JSON.stringify(data.classIds));
        if (data.tags.length > 0) formData.append("tags", JSON.stringify(data.tags));

        await uploadFileMutation.mutateAsync(formData);
        toast.success("File uploaded successfully!");
      } else {
        if (!data.url) {
          toast.error("Please enter a valid URL");
          return;
        }

        await createUrlResourceMutation.mutateAsync(data);
        toast.success("Resource created successfully!");
      }

      reset();
      setSelectedFile(null);
      setSelectedClasses([]);
      navigate("/learning-space");
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to upload resource");
    }
  };

  const getFileIcon = (type: string) => {
    switch (type) {
      case 'VIDEO': return Video;
      case 'PRESENTATION': return Image;
      case 'DOCUMENT': return FileText;
      default: return FileText;
    }
  };

  const isLoading = uploadFileMutation.isPending || createUrlResourceMutation.isPending || isSubmitting;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-indigo-50/20 p-4 sm:p-6">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-3 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-xl">
              <Upload className="h-8 w-8 text-blue-600" />
            </div>
            <div>
              <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-900 via-indigo-800 to-purple-700 bg-clip-text text-transparent">
                Upload Learning Resource
              </h1>
              <p className="text-slate-600 mt-1">
                Share materials with your classes
              </p>
            </div>
          </div>

          {/* Upload Mode Toggle */}
          <div className="flex items-center gap-2 p-1 bg-white rounded-lg border border-slate-200 w-fit">
            <Button
              type="button"
              variant={uploadMode === 'file' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setUploadMode('file')}
              className="flex items-center gap-2"
            >
              <Upload className="h-4 w-4" />
              Upload File
            </Button>
            <Button
              type="button"
              variant={uploadMode === 'url' ? 'default' : 'ghost'}
              size="sm"
              onClick={() => setUploadMode('url')}
              className="flex items-center gap-2"
            >
              <Link className="h-4 w-4" />
              Add URL
            </Button>
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Main Content */}
            <div className="lg:col-span-2 space-y-6">
              {/* File Upload / URL Input */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    {uploadMode === 'file' ? <Upload className="h-5 w-5" /> : <Link className="h-5 w-5" />}
                    {uploadMode === 'file' ? 'Upload File' : 'Resource URL'}
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {uploadMode === 'file' ? (
                    <div
                      className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                        dragActive
                          ? 'border-blue-400 bg-blue-50'
                          : selectedFile
                          ? 'border-green-400 bg-green-50'
                          : 'border-slate-300 hover:border-slate-400'
                      }`}
                      onDragEnter={handleDrag}
                      onDragLeave={handleDrag}
                      onDragOver={handleDrag}
                      onDrop={handleDrop}
                    >
                      {selectedFile ? (
                        <div className="space-y-3">
                          <div className="flex items-center justify-center">
                            <div className="p-3 bg-green-100 rounded-full">
                              <Check className="h-6 w-6 text-green-600" />
                            </div>
                          </div>
                          <div>
                            <p className="font-medium text-green-800">{selectedFile.name}</p>
                            <p className="text-sm text-green-600">
                              {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                            </p>
                          </div>
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setSelectedFile(null)}
                          >
                            Remove File
                          </Button>
                        </div>
                      ) : (
                        <div className="space-y-3">
                          <Upload className="h-12 w-12 text-slate-400 mx-auto" />
                          <div>
                            <p className="text-lg font-medium text-slate-700">
                              Drop your file here, or{" "}
                              <label className="text-blue-600 hover:text-blue-700 cursor-pointer underline">
                                browse
                                <input
                                  type="file"
                                  className="hidden"
                                  onChange={handleFileSelect}
                                  accept=".pdf,.doc,.docx,.ppt,.pptx,.mp4,.mov,.avi"
                                />
                              </label>
                            </p>
                            <p className="text-sm text-slate-500 mt-1">
                              Supports: PDF, DOC, PPT, MP4, MOV (Max 100MB)
                            </p>
                          </div>
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="space-y-3">
                      <Label htmlFor="url">Resource URL</Label>
                      <Input
                        id="url"
                        placeholder="https://example.com/resource"
                        {...register("url")}
                        className={errors.url ? "border-red-300" : ""}
                      />
                      {errors.url && (
                        <p className="text-sm text-red-600 flex items-center gap-1">
                          <AlertCircle className="h-4 w-4" />
                          {errors.url.message}
                        </p>
                      )}
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Resource Details */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <BookOpen className="h-5 w-5" />
                    Resource Details
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  {/* Title */}
                  <div className="space-y-2">
                    <Label htmlFor="title">Title *</Label>
                    <Input
                      id="title"
                      placeholder="Enter resource title"
                      {...register("title")}
                      className={errors.title ? "border-red-300" : ""}
                    />
                    {errors.title && (
                      <p className="text-sm text-red-600 flex items-center gap-1">
                        <AlertCircle className="h-4 w-4" />
                        {errors.title.message}
                      </p>
                    )}
                  </div>

                  {/* Description */}
                  <div className="space-y-2">
                    <Label htmlFor="description">Description *</Label>
                    <Textarea
                      id="description"
                      placeholder="Describe what this resource covers..."
                      rows={4}
                      {...register("description")}
                      className={errors.description ? "border-red-300" : ""}
                    />
                    {errors.description && (
                      <p className="text-sm text-red-600 flex items-center gap-1">
                        <AlertCircle className="h-4 w-4" />
                        {errors.description.message}
                      </p>
                    )}
                  </div>

                  {/* Type and Additional Fields */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="type">Resource Type *</Label>
                      <Select
                        value={watchedType}
                        onValueChange={(value) => setValue("type", value as any)}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select type" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="DOCUMENT">📄 Document</SelectItem>
                          <SelectItem value="VIDEO">🎥 Video</SelectItem>
                          <SelectItem value="PRESENTATION">📊 Presentation</SelectItem>
                          <SelectItem value="ASSIGNMENT">📝 Assignment</SelectItem>
                          <SelectItem value="QUIZ">❓ Quiz</SelectItem>
                          <SelectItem value="OTHER">📋 Other</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>

                    {watchedType === 'VIDEO' && (
                      <div className="space-y-2">
                        <Label htmlFor="duration">Duration (minutes)</Label>
                        <Input
                          id="duration"
                          type="number"
                          placeholder="15"
                          {...register("duration", { valueAsNumber: true })}
                        />
                      </div>
                    )}
                  </div>

                  {/* Thumbnail URL */}
                  <div className="space-y-2">
                    <Label htmlFor="thumbnailUrl">Thumbnail URL (optional)</Label>
                    <Input
                      id="thumbnailUrl"
                      placeholder="https://example.com/thumbnail.jpg"
                      {...register("thumbnailUrl")}
                    />
                  </div>

                  {/* Tags */}
                  <div className="space-y-3">
                    <Label>Tags</Label>
                    <div className="flex gap-2">
                      <Input
                        placeholder="Add a tag..."
                        value={newTag}
                        onChange={(e) => setNewTag(e.target.value)}
                        onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addTag())}
                        className="flex-1"
                      />
                      <Button type="button" onClick={addTag} size="sm">
                        <Plus className="h-4 w-4" />
                      </Button>
                    </div>
                    {watchedTags.length > 0 && (
                      <div className="flex flex-wrap gap-2">
                        {watchedTags.map((tag) => (
                          <Badge key={tag} variant="secondary" className="flex items-center gap-1">
                            <Tag className="h-3 w-3" />
                            {tag}
                            <button
                              type="button"
                              onClick={() => removeTag(tag)}
                              className="ml-1 hover:text-red-600"
                            >
                              <X className="h-3 w-3" />
                            </button>
                          </Badge>
                        ))}
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Sidebar */}
            <div className="space-y-6">
              {/* Class Selection */}
              <Card className="border-slate-200 shadow-sm">
                <CardHeader className="pb-4">
                  <CardTitle className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center">
                      <Users className="h-5 w-5 text-white" />
                    </div>
                    <div>
                      <div className="text-base font-semibold text-slate-900">Select Classes</div>
                      <div className="text-xs text-slate-500 font-normal">Choose which classes can access this resource</div>
                    </div>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {classesLoading ? (
                    <div className="flex flex-col items-center justify-center py-12">
                      <Loader2 className="h-8 w-8 animate-spin text-blue-600 mb-3" />
                      <p className="text-sm text-slate-500">Loading your classes...</p>
                    </div>
                  ) : teacherClasses.length === 0 ? (
                    <div className="text-center py-12">
                      <div className="w-16 h-16 mx-auto mb-4 bg-slate-100 rounded-full flex items-center justify-center">
                        <Users className="h-8 w-8 text-slate-400" />
                      </div>
                      <p className="text-sm text-slate-600 font-medium mb-1">No classes found</p>
                      <p className="text-xs text-slate-500">Please contact your administrator to assign classes.</p>
                    </div>
                  ) : (
                    <div className="space-y-2 max-h-80 overflow-y-auto pr-2">
                      <div className="text-xs font-medium text-slate-600 mb-3 px-1">
                        Select classes to share this resource with ({watchedClassIds.length} selected)
                      </div>
                      {teacherClasses.map((classInfo) => {
                        const isSelected = watchedClassIds.includes(classInfo.id);
                        return (
                          <div
                            key={classInfo.id}
                            className={`group relative flex items-center space-x-4 p-4 rounded-xl border-2 transition-all duration-200 cursor-pointer ${
                              isSelected 
                                ? 'border-blue-200 bg-blue-50/80 shadow-sm' 
                                : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/80'
                            }`}
                            onClick={() => toggleClass(classInfo.id)}
                          >
                            <div className={`flex-shrink-0 w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all duration-200 ${
                              isSelected 
                                ? 'border-blue-500 bg-blue-500' 
                                : 'border-slate-300 group-hover:border-slate-400'
                            }`}>
                              {isSelected && (
                                <Check className="h-3 w-3 text-white" />
                              )}
                            </div>
                            
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center space-x-2">
                                <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold ${
                                  isSelected 
                                    ? 'bg-blue-100 text-blue-700' 
                                    : 'bg-slate-100 text-slate-600 group-hover:bg-slate-200'
                                }`}>
                                  {classInfo.name}
                                </div>
                                <div className="flex-1 min-w-0">
                                  <p className={`font-semibold text-sm truncate ${
                                    isSelected ? 'text-blue-900' : 'text-slate-900'
                                  }`}>
                                    Class {classInfo.name}
                                  </p>
                                  <p className={`text-xs truncate ${
                                    isSelected ? 'text-blue-600' : 'text-slate-500'
                                  }`}>
                                    {classInfo.gradeLevel} • {classInfo.section} • {classInfo.studentCount || 0} students
                                  </p>
                                </div>
                              </div>
                            </div>
                            
                            {isSelected && (
                              <div className="flex-shrink-0">
                                <div className="w-2 h-2 rounded-full bg-blue-500"></div>
                              </div>
                            )}
                          </div>
                        );
                      })}
                      
                      {/* Quick Actions */}
                      <div className="flex items-center justify-between pt-3 mt-4 border-t border-slate-200">
                        <button
                          type="button"
                          onClick={() => setValue("classIds", teacherClasses.map(c => c.id))}
                          className="text-xs font-medium text-blue-600 hover:text-blue-700 transition-colors"
                        >
                          Select All
                        </button>
                        <button
                          type="button"
                          onClick={() => setValue("classIds", [])}
                          className="text-xs font-medium text-slate-500 hover:text-slate-700 transition-colors"
                        >
                          Clear All
                        </button>
                      </div>
                    </div>
                  )}
                  {errors.classIds && (
                    <p className="text-sm text-red-600 flex items-center gap-1 mt-2">
                      <AlertCircle className="h-4 w-4" />
                      {errors.classIds.message}
                    </p>
                  )}
                </CardContent>
              </Card>

              {/* Visibility Settings */}
              <Card className="border-slate-200 shadow-sm">
                <CardHeader className="pb-4">
                  <CardTitle className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-green-500 to-emerald-600 flex items-center justify-center">
                      <Eye className="h-5 w-5 text-white" />
                    </div>
                    <div>
                      <div className="text-base font-semibold text-slate-900">Visibility</div>
                      <div className="text-xs text-slate-500 font-normal">Control who can see this resource</div>
                    </div>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div 
                    className={`p-4 rounded-xl border-2 transition-all duration-200 cursor-pointer ${
                      watch("isPublic") 
                        ? 'border-green-200 bg-green-50/80' 
                        : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                    onClick={() => setValue("isPublic", !watch("isPublic"))}
                  >
                    <div className="flex items-center space-x-3">
                      <div className={`w-5 h-5 rounded-md border-2 flex items-center justify-center transition-all duration-200 ${
                        watch("isPublic") 
                          ? 'border-green-500 bg-green-500' 
                          : 'border-slate-300'
                      }`}>
                        {watch("isPublic") && (
                          <Check className="h-3 w-3 text-white" />
                        )}
                      </div>
                      <div className="flex-1">
                        <p className={`font-medium text-sm ${
                          watch("isPublic") ? 'text-green-900' : 'text-slate-900'
                        }`}>
                          Make this resource public
                        </p>
                        <p className={`text-xs mt-1 ${
                          watch("isPublic") ? 'text-green-600' : 'text-slate-500'
                        }`}>
                          {watch("isPublic") 
                            ? 'Visible to all teachers and students in the school'
                            : 'Only visible to selected classes and their teachers'
                          }
                        </p>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Actions */}
              <div className="space-y-4">
                <Button
                  type="submit"
                  disabled={isLoading || watchedClassIds.length === 0}
                  className="w-full h-12 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white font-semibold shadow-lg hover:shadow-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {isLoading ? (
                    <>
                      <Loader2 className="h-5 w-5 mr-2 animate-spin" />
                      Uploading Resource...
                    </>
                  ) : (
                    <>
                      <Upload className="h-5 w-5 mr-2" />
                      Upload Resource
                    </>
                  )}
                </Button>
                
                {watchedClassIds.length === 0 && !isLoading && (
                  <p className="text-xs text-amber-600 bg-amber-50 border border-amber-200 rounded-lg p-3 text-center">
                    <AlertCircle className="h-4 w-4 inline mr-1" />
                    Please select at least one class to continue
                  </p>
                )}
                
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => navigate("/learning-space")}
                  className="w-full h-10 border-slate-300 text-slate-700 hover:bg-slate-50 hover:border-slate-400 transition-all duration-200"
                >
                  Cancel
                </Button>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}

export default TeacherResourceUpload;
