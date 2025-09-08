import * as React from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { 
  FileText, 
  Video, 
  Image as ImageIcon, 
  Volume2, 
  Presentation, 
  Link as LinkIcon,
  ExternalLink,
  Download,
  Play,
  Pause,
  Volume1,
  VolumeX,
  Maximize,
  Eye,
  AlertCircle
} from "lucide-react";

import type { LearningResource } from "@/types/learning-resource";
import { ResourceType } from "@/types/learning-resource";

interface ResourcePreviewProps {
  resource: LearningResource;
}

// PDF Preview Component
function PDFPreview({ url }: { url: string }) {
  return (
    <div className="w-full h-96 border rounded-lg overflow-hidden bg-slate-100">
      <iframe
        src={`${url}#toolbar=0&navpanes=0&scrollbar=0`}
        className="w-full h-full"
        title="PDF Preview"
      />
    </div>
  );
}

// Image Preview Component
function ImagePreview({ url, title }: { url: string; title: string }) {
  const [isLoading, setIsLoading] = React.useState(true);
  const [hasError, setHasError] = React.useState(false);

  return (
    <div className="w-full max-h-96 border rounded-lg overflow-hidden bg-slate-100 flex items-center justify-center">
      {hasError ? (
        <div className="text-center p-8">
          <AlertCircle className="h-12 w-12 text-slate-400 mx-auto mb-3" />
          <p className="text-slate-600">Failed to load image</p>
          <Button 
            variant="outline" 
            size="sm" 
            className="mt-2"
            onClick={() => window.open(url, '_blank')}
          >
            <ExternalLink className="h-4 w-4 mr-2" />
            View Original
          </Button>
        </div>
      ) : (
        <>
          {isLoading && (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          )}
          <img
            src={url}
            alt={title}
            className="max-w-full max-h-full object-contain"
            onLoad={() => setIsLoading(false)}
            onError={() => {
              setIsLoading(false);
              setHasError(true);
            }}
          />
        </>
      )}
    </div>
  );
}

// Video Preview Component
function VideoPreview({ url, title }: { url: string; title: string }) {
  const [isPlaying, setIsPlaying] = React.useState(false);
  const [isMuted, setIsMuted] = React.useState(false);
  const videoRef = React.useRef<HTMLVideoElement>(null);

  const togglePlay = () => {
    if (videoRef.current) {
      if (isPlaying) {
        videoRef.current.pause();
      } else {
        videoRef.current.play();
      }
      setIsPlaying(!isPlaying);
    }
  };

  const toggleMute = () => {
    if (videoRef.current) {
      videoRef.current.muted = !isMuted;
      setIsMuted(!isMuted);
    }
  };

  const toggleFullscreen = () => {
    if (videoRef.current) {
      if (videoRef.current.requestFullscreen) {
        videoRef.current.requestFullscreen();
      }
    }
  };

  return (
    <div className="w-full max-h-96 border rounded-lg overflow-hidden bg-black relative group">
      <video
        ref={videoRef}
        src={url}
        className="w-full h-full object-contain"
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        controls={false}
      />
      
      {/* Custom Controls */}
      <div className="absolute inset-0 bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity duration-200 flex items-center justify-center">
        <div className="flex items-center space-x-4">
          <Button
            variant="secondary"
            size="sm"
            onClick={togglePlay}
            className="bg-white/90 hover:bg-white text-black"
          >
            {isPlaying ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={toggleMute}
            className="bg-white/90 hover:bg-white text-black"
          >
            {isMuted ? <VolumeX className="h-4 w-4" /> : <Volume1 className="h-4 w-4" />}
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={toggleFullscreen}
            className="bg-white/90 hover:bg-white text-black"
          >
            <Maximize className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}

// Audio Preview Component
function AudioPreview({ url, title }: { url: string; title: string }) {
  const [isPlaying, setIsPlaying] = React.useState(false);
  const [currentTime, setCurrentTime] = React.useState(0);
  const [duration, setDuration] = React.useState(0);
  const audioRef = React.useRef<HTMLAudioElement>(null);

  const togglePlay = () => {
    if (audioRef.current) {
      if (isPlaying) {
        audioRef.current.pause();
      } else {
        audioRef.current.play();
      }
      setIsPlaying(!isPlaying);
    }
  };

  const handleTimeUpdate = () => {
    if (audioRef.current) {
      setCurrentTime(audioRef.current.currentTime);
    }
  };

  const handleLoadedMetadata = () => {
    if (audioRef.current) {
      setDuration(audioRef.current.duration);
    }
  };

  const formatTime = (time: number) => {
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time % 60);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  return (
    <Card className="w-full">
      <CardContent className="p-6">
        <div className="flex items-center space-x-4">
          <div className="h-16 w-16 rounded-full bg-gradient-to-r from-purple-100 to-pink-100 border-2 border-purple-200/60 flex items-center justify-center">
            <Volume2 className="h-8 w-8 text-purple-600" />
          </div>
          <div className="flex-1">
            <h3 className="font-semibold text-slate-900 mb-2">{title}</h3>
            <div className="flex items-center space-x-4">
              <Button
                variant="outline"
                size="sm"
                onClick={togglePlay}
                className="flex items-center space-x-2"
              >
                {isPlaying ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
                <span>{isPlaying ? 'Pause' : 'Play'}</span>
              </Button>
              <div className="flex items-center space-x-2 text-sm text-slate-600">
                <span>{formatTime(currentTime)}</span>
                <span>/</span>
                <span>{formatTime(duration)}</span>
              </div>
            </div>
          </div>
        </div>
        
        <audio
          ref={audioRef}
          src={url}
          onPlay={() => setIsPlaying(true)}
          onPause={() => setIsPlaying(false)}
          onTimeUpdate={handleTimeUpdate}
          onLoadedMetadata={handleLoadedMetadata}
          className="hidden"
        />
      </CardContent>
    </Card>
  );
}

// Document Preview Component
function DocumentPreview({ resource }: { resource: LearningResource }) {
  const getFileExtension = (url: string) => {
    return url.split('.').pop()?.toLowerCase() || '';
  };

  const extension = resource.url ? getFileExtension(resource.url) : '';
  const isPDF = extension === 'pdf';

  if (isPDF && resource.url) {
    return <PDFPreview url={resource.url} />;
  }

  return (
    <Card className="w-full">
      <CardContent className="p-8 text-center">
        <div className="h-24 w-24 rounded-full bg-gradient-to-r from-blue-100 to-indigo-100 border-2 border-blue-200/60 flex items-center justify-center mx-auto mb-4">
          <FileText className="h-12 w-12 text-blue-600" />
        </div>
        <h3 className="font-semibold text-slate-900 mb-2">{resource.title}</h3>
        <p className="text-slate-600 mb-4">Document preview not available</p>
        <div className="flex items-center justify-center space-x-3">
          {resource.url && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => window.open(resource.url, '_blank')}
            >
              <ExternalLink className="h-4 w-4 mr-2" />
              Open Document
            </Button>
          )}
          {resource.filename && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => {/* Download logic */}}
            >
              <Download className="h-4 w-4 mr-2" />
              Download
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

// Link Preview Component
function LinkPreview({ resource }: { resource: LearningResource }) {
  return (
    <Card className="w-full">
      <CardContent className="p-8 text-center">
        <div className="h-24 w-24 rounded-full bg-gradient-to-r from-cyan-100 to-blue-100 border-2 border-cyan-200/60 flex items-center justify-center mx-auto mb-4">
          <LinkIcon className="h-12 w-12 text-cyan-600" />
        </div>
        <h3 className="font-semibold text-slate-900 mb-2">{resource.title}</h3>
        <p className="text-slate-600 mb-4">{resource.description}</p>
        {resource.url && (
          <Button
            onClick={() => window.open(resource.url, '_blank')}
            className="bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-700 hover:to-blue-700"
          >
            <ExternalLink className="h-4 w-4 mr-2" />
            Visit Link
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

// Presentation Preview Component
function PresentationPreview({ resource }: { resource: LearningResource }) {
  return (
    <Card className="w-full">
      <CardContent className="p-8 text-center">
        <div className="h-24 w-24 rounded-full bg-gradient-to-r from-orange-100 to-yellow-100 border-2 border-orange-200/60 flex items-center justify-center mx-auto mb-4">
          <Presentation className="h-12 w-12 text-orange-600" />
        </div>
        <h3 className="font-semibold text-slate-900 mb-2">{resource.title}</h3>
        <p className="text-slate-600 mb-4">Presentation preview not available</p>
        <div className="flex items-center justify-center space-x-3">
          {resource.url && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => window.open(resource.url, '_blank')}
            >
              <Eye className="h-4 w-4 mr-2" />
              View Presentation
            </Button>
          )}
          {resource.filename && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => {/* Download logic */}}
            >
              <Download className="h-4 w-4 mr-2" />
              Download
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

export function ResourcePreview({ resource }: ResourcePreviewProps) {
  const renderPreview = () => {
    switch (resource.type) {
      case ResourceType.VIDEO:
        return resource.url ? (
          <VideoPreview url={resource.url} title={resource.title} />
        ) : (
          <DocumentPreview resource={resource} />
        );
      
      case ResourceType.AUDIO:
        return resource.url ? (
          <AudioPreview url={resource.url} title={resource.title} />
        ) : (
          <DocumentPreview resource={resource} />
        );
      
      case ResourceType.IMAGE:
        return resource.url ? (
          <ImagePreview url={resource.url} title={resource.title} />
        ) : (
          <DocumentPreview resource={resource} />
        );
      
      case ResourceType.DOCUMENT:
        return <DocumentPreview resource={resource} />;
      
      case ResourceType.PRESENTATION:
        return <PresentationPreview resource={resource} />;
      
      case ResourceType.LINK:
        return <LinkPreview resource={resource} />;
      
      default:
        return <DocumentPreview resource={resource} />;
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">Preview</h2>
        {resource.thumbnailUrl && (
          <Badge variant="outline" className="text-xs">
            <ImageIcon className="h-3 w-3 mr-1" />
            Has Thumbnail
          </Badge>
        )}
      </div>
      
      {renderPreview()}
      
      {resource.thumbnailUrl && (
        <div className="mt-4">
          <h3 className="text-sm font-medium text-slate-700 mb-2">Thumbnail</h3>
          <div className="w-32 h-20 border rounded-lg overflow-hidden bg-slate-100">
            <img
              src={resource.thumbnailUrl}
              alt={`${resource.title} thumbnail`}
              className="w-full h-full object-cover"
            />
          </div>
        </div>
      )}
    </div>
  );
}
