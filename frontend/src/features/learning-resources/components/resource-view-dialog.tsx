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
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { 
  Download, 
  ExternalLink, 
  Eye, 
  EyeOff, 
  Clock, 
  Calendar,
  User,
  BookOpen,
  Users,
  MessageSquare,
  FileText,
  Video,
  Image,
  Volume2,
  Presentation,
  Link as LinkIcon,
  Share2,
  Heart,
  HeartOff
} from "lucide-react";
import { format } from "date-fns";
import toast from "react-hot-toast";

import type { LearningResource, ResourceComment } from "@/types/learning-resource";
import { ResourceType } from "@/types/learning-resource";
import { ResourceComments } from "./resource-comments";
import { ResourcePreview } from "./resource-preview";

interface ResourceViewDialogProps {
  resource: LearningResource | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDownload?: (resource: LearningResource) => void;
  onEdit?: (resource: LearningResource) => void;
  onDelete?: (resource: LearningResource) => void;
}

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
      return Image;
    case ResourceType.LINK:
      return LinkIcon;
    default:
      return FileText;
  }
};

// Resource type color mapping
const getResourceTypeColor = (type: ResourceType) => {
  switch (type) {
    case ResourceType.VIDEO:
      return "bg-gradient-to-r from-red-100 to-pink-100 text-red-800 border-red-200/60";
    case ResourceType.DOCUMENT:
      return "bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-800 border-blue-200/60";
    case ResourceType.PRESENTATION:
      return "bg-gradient-to-r from-orange-100 to-yellow-100 text-orange-800 border-orange-200/60";
    case ResourceType.AUDIO:
      return "bg-gradient-to-r from-purple-100 to-pink-100 text-purple-800 border-purple-200/60";
    case ResourceType.IMAGE:
      return "bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 border-green-200/60";
    case ResourceType.LINK:
      return "bg-gradient-to-r from-cyan-100 to-blue-100 text-cyan-800 border-cyan-200/60";
    default:
      return "bg-gradient-to-r from-gray-100 to-slate-100 text-gray-800 border-gray-200/60";
  }
};

export function ResourceViewDialog({ 
  resource, 
  open, 
  onOpenChange, 
  onDownload, 
  onEdit, 
  onDelete 
}: ResourceViewDialogProps) {
  const [isFavorited, setIsFavorited] = React.useState(false);
  const [showComments, setShowComments] = React.useState(false);

  if (!resource) return null;

  const IconComponent = getResourceTypeIcon(resource.type);
  const colorClasses = getResourceTypeColor(resource.type);

  const handleDownload = () => {
    if (onDownload) {
      onDownload(resource);
    } else {
      toast.success("Download started");
    }
  };

  const handleShare = async () => {
    try {
      if (navigator.share && resource.url) {
        await navigator.share({
          title: resource.title,
          text: resource.description,
          url: resource.url,
        });
      } else if (resource.url) {
        await navigator.clipboard.writeText(resource.url);
        toast.success("Resource link copied to clipboard");
      } else {
        toast.error("No shareable link available");
      }
    } catch (error) {
      toast.error("Failed to share resource");
    }
  };

  const handleFavorite = () => {
    setIsFavorited(!isFavorited);
    toast.success(isFavorited ? "Removed from favorites" : "Added to favorites");
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] p-0 overflow-hidden">
        <div className="flex flex-col h-full">
          {/* Header */}
          <DialogHeader className="p-6 pb-4 border-b bg-gradient-to-r from-slate-50 to-blue-50">
            <div className="flex items-start justify-between">
              <div className="flex items-start space-x-4">
                <div className="h-16 w-16 rounded-xl bg-gradient-to-r from-indigo-100 to-purple-100 border-2 border-indigo-200/60 flex items-center justify-center">
                  <IconComponent className="h-8 w-8 text-indigo-600" />
                </div>
                <div className="flex-1">
                  <DialogTitle className="text-2xl font-bold text-slate-900 mb-2">
                    {resource.title}
                  </DialogTitle>
                  <div className="flex items-center space-x-3 mb-3">
                    <Badge 
                      variant="outline" 
                      className={`${colorClasses} font-semibold`}
                    >
                      <IconComponent className="h-3 w-3 mr-1" />
                      {resource.type}
                    </Badge>
                    <Badge 
                      variant={resource.isPublic ? "default" : "secondary"}
                      className={
                        resource.isPublic 
                          ? "bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 border border-green-200/60"
                          : "bg-gradient-to-r from-orange-100 to-yellow-100 text-orange-800 border border-orange-200/60"
                      }
                    >
                      {resource.isPublic ? <Eye className="h-3 w-3 mr-1" /> : <EyeOff className="h-3 w-3 mr-1" />}
                      {resource.isPublic ? "Public" : "Private"}
                    </Badge>
                    {resource.duration && (
                      <Badge 
                        variant="outline" 
                        className="bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-800 border border-blue-200/60"
                      >
                        <Clock className="h-3 w-3 mr-1" />
                        {resource.duration}min
                      </Badge>
                    )}
                  </div>
                  <DialogDescription className="text-slate-600 text-base leading-relaxed">
                    {resource.description}
                  </DialogDescription>
                </div>
              </div>
              <div className="flex items-center space-x-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleFavorite}
                  className="text-slate-600 hover:text-red-600"
                >
                  {isFavorited ? (
                    <Heart className="h-4 w-4 fill-current text-red-600" />
                  ) : (
                    <HeartOff className="h-4 w-4" />
                  )}
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleShare}
                  className="text-slate-600 hover:text-blue-600"
                >
                  <Share2 className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </DialogHeader>

          {/* Content */}
          <div className="flex-1 overflow-hidden">
            <div className="grid grid-cols-1 lg:grid-cols-3 h-full">
              {/* Main Content - Preview */}
              <div className="lg:col-span-2 p-6 overflow-auto">
                <ResourcePreview resource={resource} />
              </div>

              {/* Sidebar - Details & Comments */}
              <div className="border-l bg-slate-50/50 flex flex-col">
                <div className="p-6 space-y-6">
                  {/* Resource Details */}
                  <Card>
                    <CardContent className="p-4 space-y-4">
                      <h3 className="font-semibold text-slate-900 flex items-center">
                        <BookOpen className="h-4 w-4 mr-2" />
                        Resource Details
                      </h3>
                      
                      <div className="space-y-3 text-sm">
                        <div className="flex items-center justify-between">
                          <span className="text-slate-600">Created:</span>
                          <div className="flex items-center text-slate-900">
                            <Calendar className="h-3 w-3 mr-1" />
                            {format(new Date(resource.createdAt), 'MMM dd, yyyy')}
                          </div>
                        </div>
                        
                        <div className="flex items-center justify-between">
                          <span className="text-slate-600">Updated:</span>
                          <div className="flex items-center text-slate-900">
                            <Calendar className="h-3 w-3 mr-1" />
                            {format(new Date(resource.updatedAt), 'MMM dd, yyyy')}
                          </div>
                        </div>

                        {resource.teacherIds.length > 0 && (
                          <div className="flex items-center justify-between">
                            <span className="text-slate-600">Teachers:</span>
                            <div className="flex items-center text-slate-900">
                              <User className="h-3 w-3 mr-1" />
                              {resource.teacherIds.length}
                            </div>
                          </div>
                        )}

                        {resource.classIds.length > 0 && (
                          <div className="flex items-center justify-between">
                            <span className="text-slate-600">Classes:</span>
                            <div className="flex items-center text-slate-900">
                              <Users className="h-3 w-3 mr-1" />
                              {resource.classIds.length}
                            </div>
                          </div>
                        )}

                        {resource.courseIds.length > 0 && (
                          <div className="flex items-center justify-between">
                            <span className="text-slate-600">Courses:</span>
                            <div className="flex items-center text-slate-900">
                              <BookOpen className="h-3 w-3 mr-1" />
                              {resource.courseIds.length}
                            </div>
                          </div>
                        )}
                      </div>
                    </CardContent>
                  </Card>

                  {/* Quick Actions */}
                  <Card>
                    <CardContent className="p-4">
                      <h3 className="font-semibold text-slate-900 mb-3">Quick Actions</h3>
                      <div className="space-y-2">
                        {resource.url && (
                          <Button
                            variant="outline"
                            size="sm"
                            className="w-full justify-start"
                            onClick={() => window.open(resource.url, '_blank')}
                          >
                            <ExternalLink className="h-4 w-4 mr-2" />
                            Open Link
                          </Button>
                        )}
                        
                        {resource.filename && (
                          <Button
                            variant="outline"
                            size="sm"
                            className="w-full justify-start"
                            onClick={handleDownload}
                          >
                            <Download className="h-4 w-4 mr-2" />
                            Download File
                          </Button>
                        )}

                        <Button
                          variant="outline"
                          size="sm"
                          className="w-full justify-start"
                          onClick={() => setShowComments(!showComments)}
                        >
                          <MessageSquare className="h-4 w-4 mr-2" />
                          {showComments ? 'Hide' : 'Show'} Comments
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                </div>

                {/* Comments Section */}
                {showComments && (
                  <div className="flex-1 border-t">
                    <ResourceComments resourceId={resource.id} />
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Footer */}
          <DialogFooter className="p-6 pt-4 border-t bg-slate-50/50">
            <div className="flex items-center justify-between w-full">
              <div className="flex items-center space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => onEdit?.(resource)}
                >
                  Edit Resource
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => onDelete?.(resource)}
                  className="text-red-600 hover:text-red-700 border-red-200 hover:border-red-300"
                >
                  Delete
                </Button>
              </div>
              <Button onClick={() => onOpenChange(false)}>
                Close
              </Button>
            </div>
          </DialogFooter>
        </div>
      </DialogContent>
    </Dialog>
  );
}
