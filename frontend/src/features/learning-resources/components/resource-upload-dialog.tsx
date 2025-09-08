import * as React from "react";
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle,
  DialogDescription,
  DialogFooter 
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { 
  Upload, 
  File, 
  X, 
  CheckCircle, 
  AlertCircle,
  FileText,
  Video,
  Image as ImageIcon,
  Volume2,
  Presentation,
  Link as LinkIcon,
  Cloud,
  Loader2
} from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import toast from "react-hot-toast";

import type { ResourceUploadRequest } from "@/types/learning-resource";
import { ResourceType } from "@/types/learning-resource";
import { useUploadLearningResource } from "../hooks/use-learning-resources";

interface ResourceUploadDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

interface FileWithPreview extends File {
  preview?: string;
  id: string;
}

interface UploadProgress {
  fileId: string;
  progress: number;
  status: 'uploading' | 'success' | 'error';
  error?: string;
}

// File type detection
const getResourceTypeFromFile = (file: File): ResourceType => {
  const extension = file.name.split('.').pop()?.toLowerCase();
  const mimeType = file.type.toLowerCase();

  if (mimeType.startsWith('video/') || ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm'].includes(extension || '')) {
    return ResourceType.VIDEO;
  }
  if (mimeType.startsWith('audio/') || ['mp3', 'wav', 'ogg', 'aac', 'flac'].includes(extension || '')) {
    return ResourceType.AUDIO;
  }
  if (mimeType.startsWith('image/') || ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(extension || '')) {
    return ResourceType.IMAGE;
  }
  if (['ppt', 'pptx', 'odp'].includes(extension || '')) {
    return ResourceType.PRESENTATION;
  }
  if (['pdf', 'doc', 'docx', 'txt', 'rtf', 'odt'].includes(extension || '')) {
    return ResourceType.DOCUMENT;
  }
  
  return ResourceType.OTHER;
};

// File size formatter
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// Resource type icon mapping
const getResourceTypeIcon = (type: ResourceType) => {
  switch (type) {
    case ResourceType.VIDEO:
      return Video;
    case ResourceType.DOCUMENT:
      return FileText;
    case ResourceType.PRESENTATION:
      return Presentation;
    case ResourceType.AUDIO:
      return Volume2;
    case ResourceType.IMAGE:
      return ImageIcon;
    case ResourceType.LINK:
      return LinkIcon;
    default:
      return FileText;
  }
};

export function ResourceUploadDialog({ open, onOpenChange, onSuccess }: ResourceUploadDialogProps) {
  const [files, setFiles] = React.useState<FileWithPreview[]>([]);
  const [uploadProgress, setUploadProgress] = React.useState<UploadProgress[]>([]);
  const [isDragOver, setIsDragOver] = React.useState(false);
  const [isUploading, setIsUploading] = React.useState(false);
  
  // Form state
  const [title, setTitle] = React.useState("");
  const [description, setDescription] = React.useState("");
  const [resourceType, setResourceType] = React.useState<ResourceType>(ResourceType.DOCUMENT);
  const [isPublic, setIsPublic] = React.useState(true);
  const [duration, setDuration] = React.useState<number | undefined>();
  const [thumbnailUrl, setThumbnailUrl] = React.useState("");

  const uploadMutation = useUploadLearningResource();

  // File input ref
  const fileInputRef = React.useRef<HTMLInputElement>(null);

  // Handle file selection
  const handleFileSelect = (selectedFiles: FileList | null) => {
    if (!selectedFiles) return;

    const newFiles: FileWithPreview[] = Array.from(selectedFiles).map(file => {
      const fileWithPreview = file as FileWithPreview;
      fileWithPreview.id = Math.random().toString(36).substr(2, 9);
      
      // Create preview for images
      if (file.type.startsWith('image/')) {
        fileWithPreview.preview = URL.createObjectURL(file);
      }
      
      return fileWithPreview;
    });

    setFiles(prev => [...prev, ...newFiles]);
    
    // Auto-detect resource type from first file
    if (newFiles.length > 0 && !title) {
      const detectedType = getResourceTypeFromFile(newFiles[0]);
      setResourceType(detectedType);
      setTitle(newFiles[0].name.replace(/\.[^/.]+$/, "")); // Remove extension
    }
  };

  // Handle drag and drop
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    handleFileSelect(e.dataTransfer.files);
  };

  // Remove file
  const removeFile = (fileId: string) => {
    setFiles(prev => {
      const fileToRemove = prev.find(f => f.id === fileId);
      if (fileToRemove?.preview) {
        URL.revokeObjectURL(fileToRemove.preview);
      }
      return prev.filter(f => f.id !== fileId);
    });
    setUploadProgress(prev => prev.filter(p => p.fileId !== fileId));
  };

  // Handle upload
  const handleUpload = async () => {
    if (!title.trim()) {
      toast.error("Please enter a title");
      return;
    }
    
    if (!description.trim()) {
      toast.error("Please enter a description");
      return;
    }

    if (files.length === 0) {
      toast.error("Please select at least one file");
      return;
    }

    setIsUploading(true);

    try {
      // For now, upload the first file (can be extended for multiple files)
      const file = files[0];
      
      // Initialize progress
      setUploadProgress([{
        fileId: file.id,
        progress: 0,
        status: 'uploading'
      }]);

      // Simulate upload progress
      const progressInterval = setInterval(() => {
        setUploadProgress(prev => 
          prev.map(p => 
            p.fileId === file.id && p.progress < 90
              ? { ...p, progress: p.progress + 10 }
              : p
          )
        );
      }, 200);

      const uploadRequest: ResourceUploadRequest = {
        file,
        title: title.trim(),
        description: description.trim(),
        type: resourceType,
        isPublic,
        duration,
        thumbnailUrl: thumbnailUrl.trim() || undefined,
      };

      await uploadMutation.mutateAsync(uploadRequest);

      // Complete progress
      clearInterval(progressInterval);
      setUploadProgress(prev => 
        prev.map(p => 
          p.fileId === file.id
            ? { ...p, progress: 100, status: 'success' }
            : p
        )
      );

      toast.success("Resource uploaded successfully!");
      
      // Reset form
      setTimeout(() => {
        handleClose();
        onSuccess?.();
      }, 1000);

    } catch (error) {
      setUploadProgress(prev => 
        prev.map(p => 
          p.fileId === files[0].id
            ? { ...p, status: 'error', error: 'Upload failed' }
            : p
        )
      );
      toast.error("Failed to upload resource");
    } finally {
      setIsUploading(false);
    }
  };

  // Handle close
  const handleClose = () => {
    // Clean up object URLs
    files.forEach(file => {
      if (file.preview) {
        URL.revokeObjectURL(file.preview);
      }
    });
    
    // Reset state
    setFiles([]);
    setUploadProgress([]);
    setTitle("");
    setDescription("");
    setResourceType(ResourceType.DOCUMENT);
    setIsPublic(true);
    setDuration(undefined);
    setThumbnailUrl("");
    setIsUploading(false);
    
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center">
            <Upload className="h-5 w-5 mr-2" />
            Upload Learning Resource
          </DialogTitle>
          <DialogDescription>
            Upload files and provide details for your learning resource
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 overflow-auto max-h-[60vh] px-1">
          {/* File Upload Area */}
          <div
            className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
              isDragOver
                ? 'border-blue-500 bg-blue-50'
                : 'border-slate-300 hover:border-slate-400'
            }`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              className="hidden"
              onChange={(e) => handleFileSelect(e.target.files)}
              accept="*/*"
            />
            
            <div className="space-y-4">
              <div className="h-16 w-16 rounded-full bg-gradient-to-r from-blue-100 to-indigo-100 border-2 border-blue-200/60 flex items-center justify-center mx-auto">
                <Cloud className="h-8 w-8 text-blue-600" />
              </div>
              <div>
                <p className="text-lg font-semibold text-slate-900">
                  Drop files here or click to browse
                </p>
                <p className="text-sm text-slate-500 mt-1">
                  Support for videos, documents, images, audio files and more
                </p>
              </div>
            </div>
          </div>

          {/* Selected Files */}
          {files.length > 0 && (
            <div className="space-y-3">
              <h3 className="font-semibold text-slate-900">Selected Files</h3>
              {files.map((file) => {
                const progress = uploadProgress.find(p => p.fileId === file.id);
                const IconComponent = getResourceTypeIcon(getResourceTypeFromFile(file));
                
                return (
                  <Card key={file.id}>
                    <CardContent className="p-4">
                      <div className="flex items-center space-x-4">
                        <div className="h-12 w-12 rounded-lg bg-gradient-to-r from-slate-100 to-blue-100 border border-slate-200 flex items-center justify-center">
                          {file.preview ? (
                            <img
                              src={file.preview}
                              alt={file.name}
                              className="h-full w-full object-cover rounded-lg"
                            />
                          ) : (
                            <IconComponent className="h-6 w-6 text-slate-600" />
                          )}
                        </div>
                        
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="font-medium text-slate-900 truncate">
                                {file.name}
                              </p>
                              <p className="text-sm text-slate-500">
                                {formatFileSize(file.size)}
                              </p>
                            </div>
                            
                            {!isUploading && (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => removeFile(file.id)}
                                className="text-slate-400 hover:text-red-600"
                              >
                                <X className="h-4 w-4" />
                              </Button>
                            )}
                          </div>
                          
                          {progress && (
                            <div className="mt-2 space-y-1">
                              <div className="flex items-center justify-between text-xs">
                                <span className="text-slate-600">
                                  {progress.status === 'uploading' && 'Uploading...'}
                                  {progress.status === 'success' && 'Upload complete'}
                                  {progress.status === 'error' && (progress.error || 'Upload failed')}
                                </span>
                                <span className="text-slate-600">{progress.progress}%</span>
                              </div>
                              <Progress value={progress.progress} className="h-1" />
                            </div>
                          )}
                        </div>
                        
                        {progress?.status === 'success' && (
                          <CheckCircle className="h-5 w-5 text-green-600" />
                        )}
                        {progress?.status === 'error' && (
                          <AlertCircle className="h-5 w-5 text-red-600" />
                        )}
                      </div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          )}

          {/* Resource Details Form */}
          <div className="space-y-4">
            <h3 className="font-semibold text-slate-900">Resource Details</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="title">Title *</Label>
                <Input
                  id="title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="Enter resource title"
                  disabled={isUploading}
                />
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="type">Resource Type</Label>
                <Select 
                  value={resourceType} 
                  onValueChange={(value) => setResourceType(value as ResourceType)}
                  disabled={isUploading}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.values(ResourceType).map((type) => (
                      <SelectItem key={type} value={type}>
                        {type}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description *</Label>
              <Textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Describe the resource content and purpose"
                className="min-h-[80px]"
                disabled={isUploading}
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="duration">Duration (minutes)</Label>
                <Input
                  id="duration"
                  type="number"
                  value={duration || ""}
                  onChange={(e) => setDuration(e.target.value ? parseInt(e.target.value) : undefined)}
                  placeholder="Optional"
                  min="0"
                  disabled={isUploading}
                />
              </div>
              
              <div className="space-y-2">
                <Label htmlFor="thumbnail">Thumbnail URL</Label>
                <Input
                  id="thumbnail"
                  value={thumbnailUrl}
                  onChange={(e) => setThumbnailUrl(e.target.value)}
                  placeholder="Optional thumbnail image URL"
                  disabled={isUploading}
                />
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <Switch
                id="public"
                checked={isPublic}
                onCheckedChange={setIsPublic}
                disabled={isUploading}
              />
              <Label htmlFor="public">Make this resource public</Label>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={handleClose}
            disabled={isUploading}
          >
            Cancel
          </Button>
          <Button
            onClick={handleUpload}
            disabled={isUploading || files.length === 0 || !title.trim() || !description.trim()}
            className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700"
          >
            {isUploading ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Uploading...
              </>
            ) : (
              <>
                <Upload className="h-4 w-4 mr-2" />
                Upload Resource
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
