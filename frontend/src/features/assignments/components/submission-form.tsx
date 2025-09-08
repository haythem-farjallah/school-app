// Assignment Submission Form Component
// Interface for students to submit their assignments

import { useState, useCallback } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { 
  Upload,
  File,
  X,
  AlertCircle,
  CheckCircle,
  Clock,
  FileText,
  Image,
  Video,
  Music,
  Archive,
  Link,
  Save,
  Send
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { cn } from '@/lib/utils';

import type { Assignment, Submission } from '@/types/assignment';

// Form Schema
const submissionFormSchema = z.object({
  textContent: z.string().optional(),
  urls: z.array(z.string().url('Please enter a valid URL')).optional(),
});

type SubmissionFormData = z.infer<typeof submissionFormSchema>;

// Props Interface
interface SubmissionFormProps {
  assignment: Assignment;
  existingSubmission?: Submission;
  onSubmit: (data: {
    textContent?: string;
    files?: File[];
    urls?: string[];
  }) => void;
  onSaveDraft?: (data: {
    textContent?: string;
    files?: File[];
    urls?: string[];
  }) => void;
  isLoading?: boolean;
  className?: string;
}

// File type icons
const getFileIcon = (mimeType: string) => {
  if (mimeType.startsWith('image/')) return Image;
  if (mimeType.startsWith('video/')) return Video;
  if (mimeType.startsWith('audio/')) return Music;
  if (mimeType.includes('zip') || mimeType.includes('rar')) return Archive;
  if (mimeType.includes('pdf') || mimeType.includes('document')) return FileText;
  return File;
};

// Format file size
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// Main Component
export function SubmissionForm({
  assignment,
  existingSubmission,
  onSubmit,
  onSaveDraft,
  isLoading = false,
  className,
}: SubmissionFormProps) {
  const [files, setFiles] = useState<File[]>([]);
  const [urlInputs, setUrlInputs] = useState<string[]>(
    existingSubmission?.urls || ['']
  );

  const form = useForm<SubmissionFormData>({
    resolver: zodResolver(submissionFormSchema),
    defaultValues: {
      textContent: existingSubmission?.textContent || '',
      urls: existingSubmission?.urls || [],
    },
  });

  const { control, handleSubmit, formState: { errors } } = form;

  // Check if assignment allows specific submission types
  const allowsText = assignment.submissionTypes.includes('text');
  const allowsFile = assignment.submissionTypes.includes('file');
  const allowsUrl = assignment.submissionTypes.includes('url');
  const allowsMedia = assignment.submissionTypes.includes('media');

  // File input handler
  const handleFileChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(event.target.files || []);
    
    // Validate file size
    const maxSize = (assignment.maxFileSize || 10) * 1024 * 1024; // Convert MB to bytes
    const validFiles = selectedFiles.filter(file => {
      if (file.size > maxSize) {
        alert(`File ${file.name} is too large. Maximum size is ${assignment.maxFileSize || 10}MB`);
        return false;
      }
      return true;
    });

    // Validate file types if specified
    if (assignment.allowedFileTypes && assignment.allowedFileTypes.length > 0) {
      const allowedExtensions = assignment.allowedFileTypes.flatMap(type => 
        type.split(',').map(ext => ext.trim().toLowerCase())
      );
      
      const validTypeFiles = validFiles.filter(file => {
        const fileExtension = '.' + file.name.split('.').pop()?.toLowerCase();
        const isAllowed = allowedExtensions.some(ext => 
          ext === fileExtension || file.type.includes(ext.replace('.', ''))
        );
        
        if (!isAllowed) {
          alert(`File ${file.name} type is not allowed. Allowed types: ${assignment.allowedFileTypes?.join(', ')}`);
          return false;
        }
        return true;
      });

      setFiles(prev => [...prev, ...validTypeFiles]);
    } else {
      setFiles(prev => [...prev, ...validFiles]);
    }
  }, [assignment.maxFileSize, assignment.allowedFileTypes]);

  // Remove file
  const removeFile = (index: number) => {
    setFiles(prev => prev.filter((_, i) => i !== index));
  };

  // URL management
  const addUrlInput = () => {
    setUrlInputs(prev => [...prev, '']);
  };

  const removeUrlInput = (index: number) => {
    setUrlInputs(prev => prev.filter((_, i) => i !== index));
  };

  const updateUrl = (index: number, value: string) => {
    setUrlInputs(prev => prev.map((url, i) => i === index ? value : url));
  };

  // Form submission
  const onFormSubmit = (data: SubmissionFormData) => {
    const submissionData = {
      textContent: data.textContent,
      files: files.length > 0 ? files : undefined,
      urls: urlInputs.filter(url => url.trim() !== ''),
    };

    onSubmit(submissionData);
  };

  const onSaveDraftClick = () => {
    if (onSaveDraft) {
      const data = form.getValues();
      const submissionData = {
        textContent: data.textContent,
        files: files.length > 0 ? files : undefined,
        urls: urlInputs.filter(url => url.trim() !== ''),
      };

      onSaveDraft(submissionData);
    }
  };

  // Check if submission is late
  const isLate = new Date() > new Date(assignment.dueDate);
  const canSubmit = assignment.allowLateSubmissions || !isLate;

  return (
    <form onSubmit={handleSubmit(onFormSubmit)} className={cn('space-y-6', className)}>
      {/* Assignment Info */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center space-x-2">
            <FileText className="h-5 w-5" />
            <span>{assignment.title}</span>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <p className="text-gray-600">{assignment.description}</p>
            
            <div className="flex items-center justify-between text-sm">
              <div className="flex items-center space-x-4">
                <Badge variant="outline">{assignment.type}</Badge>
                <span className="text-gray-600">
                  Due: {new Date(assignment.dueDate).toLocaleDateString()}
                </span>
                <span className="text-gray-600">
                  Points: {assignment.totalPoints}
                </span>
              </div>
              
              {isLate && (
                <Badge variant="destructive" className="flex items-center space-x-1">
                  <Clock className="h-3 w-3" />
                  <span>Late</span>
                </Badge>
              )}
            </div>

            {assignment.instructions && (
              <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                <h4 className="font-medium text-blue-900 mb-2">Instructions</h4>
                <p className="text-blue-800 whitespace-pre-wrap">{assignment.instructions}</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Submission Status Alert */}
      {!canSubmit && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            This assignment is past due and late submissions are not allowed.
          </AlertDescription>
        </Alert>
      )}

      {existingSubmission && (
        <Alert>
          <CheckCircle className="h-4 w-4" />
          <AlertDescription>
            You have already submitted this assignment. You can update your submission below.
          </AlertDescription>
        </Alert>
      )}

      {/* Text Submission */}
      {allowsText && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <FileText className="h-5 w-5" />
              <span>Text Entry</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              <Label htmlFor="textContent">Your Response</Label>
              <Controller
                name="textContent"
                control={control}
                render={({ field }) => (
                  <Textarea
                    {...field}
                    id="textContent"
                    placeholder="Enter your response here..."
                    rows={8}
                    className={errors.textContent ? 'border-red-500' : ''}
                    disabled={!canSubmit}
                  />
                )}
              />
              {errors.textContent && (
                <p className="text-sm text-red-600">{errors.textContent.message}</p>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* File Upload */}
      {(allowsFile || allowsMedia) && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Upload className="h-5 w-5" />
              <span>File Upload</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {/* File Input */}
              <div className="border-2 border-dashed rounded-lg p-8 text-center">
                <Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <div>
                  <p className="text-gray-600 mb-2">
                    Select files to upload
                  </p>
                  <p className="text-sm text-gray-500">
                    Max file size: {assignment.maxFileSize || 10}MB
                  </p>
                  {assignment.allowedFileTypes && assignment.allowedFileTypes.length > 0 && (
                    <p className="text-sm text-gray-500 mt-1">
                      Allowed types: {assignment.allowedFileTypes.join(', ')}
                    </p>
                  )}
                  <input
                    type="file"
                    multiple
                    onChange={handleFileChange}
                    disabled={!canSubmit}
                    className="mt-4"
                    accept={assignment.allowedFileTypes?.join(',')}
                  />
                </div>
              </div>

              {/* File List */}
              {files.length > 0 && (
                <div className="space-y-2">
                  <Label>Selected Files</Label>
                  <div className="space-y-2">
                    {files.map((file, index) => {
                      const FileIcon = getFileIcon(file.type);
                      return (
                        <div
                          key={index}
                          className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                        >
                          <div className="flex items-center space-x-3">
                            <FileIcon className="h-5 w-5 text-gray-500" />
                            <div>
                              <p className="font-medium text-sm">{file.name}</p>
                              <p className="text-xs text-gray-500">{formatFileSize(file.size)}</p>
                            </div>
                          </div>
                          
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => removeFile(index)}
                            disabled={!canSubmit}
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* URL Submission */}
      {allowsUrl && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Link className="h-5 w-5" />
              <span>Website URLs</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {urlInputs.map((url, index) => (
                <div key={index} className="flex items-center space-x-2">
                  <Input
                    value={url}
                    onChange={(e) => updateUrl(index, e.target.value)}
                    placeholder="https://example.com"
                    disabled={!canSubmit}
                    className="flex-1"
                  />
                  {urlInputs.length > 1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => removeUrlInput(index)}
                      disabled={!canSubmit}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              ))}
              
              <Button
                type="button"
                variant="outline"
                onClick={addUrlInput}
                disabled={!canSubmit}
                className="w-full"
              >
                Add Another URL
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Existing Submission Files */}
      {existingSubmission && existingSubmission.files.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Previously Submitted Files</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {existingSubmission.files.map((file, index) => {
                const FileIcon = getFileIcon(file.mimeType);
                return (
                  <div
                    key={index}
                    className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                  >
                    <div className="flex items-center space-x-3">
                      <FileIcon className="h-5 w-5 text-gray-500" />
                      <div>
                        <p className="font-medium text-sm">{file.originalName}</p>
                        <p className="text-xs text-gray-500">{formatFileSize(file.size)}</p>
                      </div>
                    </div>
                    
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => window.open(file.url, '_blank')}
                    >
                      View
                    </Button>
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Form Actions */}
      {canSubmit && (
        <div className="flex items-center justify-between pt-6">
          <div className="text-sm text-gray-600">
            {isLate && assignment.allowLateSubmissions && (
              <div className="flex items-center space-x-1 text-orange-600">
                <AlertCircle className="h-4 w-4" />
                <span>
                  Late submission penalty: {assignment.latePenalty || 0}% per day
                </span>
              </div>
            )}
          </div>
          
          <div className="flex items-center space-x-3">
            {onSaveDraft && (
              <Button
                type="button"
                variant="outline"
                onClick={onSaveDraftClick}
                disabled={isLoading}
              >
                <Save className="mr-2 h-4 w-4" />
                Save Draft
              </Button>
            )}
            
            <Button
              type="submit"
              disabled={isLoading}
              className="min-w-[120px]"
            >
              {isLoading ? (
                <div className="flex items-center space-x-2">
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  <span>Submitting...</span>
                </div>
              ) : (
                <div className="flex items-center space-x-2">
                  <Send className="h-4 w-4" />
                  <span>{existingSubmission ? 'Update Submission' : 'Submit Assignment'}</span>
                </div>
              )}
            </Button>
          </div>
        </div>
      )}
    </form>
  );
}

export default SubmissionForm;
