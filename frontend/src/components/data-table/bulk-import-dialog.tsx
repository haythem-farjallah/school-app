import * as React from "react";
import { useState, useCallback } from "react";
import toast from "react-hot-toast";
import { 
  Upload, 
  FileText, 
  CheckCircle, 
  XCircle, 
  Clock, 
  Download,
  AlertCircle,
  X
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogPortal,
  DialogOverlay,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export type UserType = 'students' | 'teachers' | 'parents' | 'staff';

export interface ImportResult {
  id: string;
  row: number;
  data: Record<string, string | number>;
  status: 'pending' | 'processing' | 'success' | 'error';
  error?: string;
}

export interface BulkImportDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  userType: UserType;
  onImport: (file: File) => Promise<ImportResult[]>;
}

const FileDropZone = ({ 
  onDrop, 
  isDragActive, 
  userType 
}: { 
  onDrop: (file: File) => void;
  isDragActive: boolean;
  userType: UserType;
}) => {
  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    const files = Array.from(e.dataTransfer.files);
    const file = files[0];
    if (file && (file.type.includes('csv') || file.type.includes('sheet') || file.name.endsWith('.csv') || file.name.endsWith('.xlsx'))) {
      onDrop(file);
    }
  }, [onDrop]);

  const handleFileInput = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onDrop(file);
    }
  }, [onDrop]);

  return (
    <div
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      className={cn(
        "relative border-2 border-dashed rounded-2xl p-12 text-center transition-all duration-300 cursor-pointer group",
        isDragActive
          ? "border-blue-500 bg-gradient-to-br from-blue-50 to-indigo-50 scale-105"
          : "border-gray-300 hover:border-blue-400 hover:bg-gray-50/50"
      )}
    >
      <input
        type="file"
        accept=".csv,.xlsx,.xls"
        onChange={handleFileInput}
        className="hidden"
        id="file-upload"
      />
      
      <motion.div
        animate={isDragActive ? { scale: 1.02 } : { scale: 1 }}
        className="space-y-6"
      >
        <div className="flex justify-center">
          <div className={cn(
            "flex h-20 w-20 items-center justify-center rounded-full transition-all duration-300",
            isDragActive 
              ? "bg-gradient-to-r from-blue-500 to-purple-600 text-white shadow-lg" 
              : "bg-gray-100 text-gray-400 group-hover:bg-blue-100 group-hover:text-blue-500"
          )}>
            <Upload className="h-10 w-10" />
          </div>
        </div>
        
        <div className="space-y-2">
          <h3 className="text-xl font-semibold text-gray-900">
            Drop your {userType} file here
          </h3>
          <p className="text-gray-600 max-w-md mx-auto">
            Drag and drop your CSV or Excel file here, or click the button below to browse your computer
          </p>
        </div>
        
        <Button 
          asChild 
          size="lg"
          className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300"
        >
          <label htmlFor="file-upload" className="cursor-pointer">
            <Upload className="h-5 w-5 mr-2" />
            Choose File
          </label>
        </Button>
        
        <div className="flex items-center justify-center gap-8 text-sm text-gray-500 pt-4">
          <div className="flex items-center gap-2">
            <div className="h-2 w-2 rounded-full bg-green-500"></div>
            <span>CSV, XLSX, XLS</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-2 w-2 rounded-full bg-blue-500"></div>
            <span>Max 10MB</span>
          </div>
        </div>
      </motion.div>

      {/* Decorative background pattern */}
      <div className="absolute inset-0 opacity-5 pointer-events-none">
        <div className="absolute top-4 left-4 h-8 w-8 rounded-full bg-blue-500"></div>
        <div className="absolute top-8 right-8 h-4 w-4 rounded-full bg-purple-500"></div>
        <div className="absolute bottom-8 left-8 h-6 w-6 rounded-full bg-indigo-500"></div>
        <div className="absolute bottom-4 right-4 h-3 w-3 rounded-full bg-blue-400"></div>
      </div>
    </div>
  );
};

const ProcessingProgress = ({
  results,
  fileName,
  onClose
}: {
  results: ImportResult[];
  fileName: string;
  onClose: () => void;
}) => {
  const total = results.length;
  const processed = results.filter(r => r.status !== 'pending').length;
  const successful = results.filter(r => r.status === 'success').length;
  const errors = results.filter(r => r.status === 'error').length;
  const processing = results.filter(r => r.status === 'processing').length;
  
  const progress = total > 0 ? (processed / total) * 100 : 0;
  const isComplete = processed === total;

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-6 border border-blue-200">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-r from-blue-500 to-purple-600">
            <FileText className="h-6 w-6 text-white" />
          </div>
          <div>
            <h3 className="text-xl font-semibold text-gray-900">Processing Import</h3>
            <p className="text-sm text-gray-600 font-medium">{fileName}</p>
          </div>
        </div>
        {isComplete && (
          <Button variant="outline" size="sm" onClick={onClose} className="border-blue-300 text-blue-700 hover:bg-blue-100">
            <X className="h-4 w-4" />
          </Button>
        )}
      </div>

      {/* Progress Bar */}
      <div className="space-y-4 bg-white rounded-xl p-6 border border-gray-200 shadow-sm">
        <div className="flex justify-between items-center">
          <span className="text-lg font-medium text-gray-900">Import Progress</span>
          <span className="text-sm font-semibold text-blue-600 bg-blue-100 px-3 py-1 rounded-full">
            {processed}/{total} processed
          </span>
        </div>
        <Progress value={progress} className="h-4" />
        <div className="flex justify-between text-sm text-gray-600">
          <span>{progress.toFixed(1)}% complete</span>
          <span>{isComplete ? 'Import completed!' : 'Processing...'}</span>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
        <Card className="border-blue-200 bg-gradient-to-br from-blue-50 to-blue-100 shadow-sm hover:shadow-md transition-shadow">
          <CardContent className="p-6 text-center">
            <div className="text-3xl font-bold text-blue-700 mb-1">{total}</div>
            <div className="text-sm font-medium text-blue-600">Total Records</div>
          </CardContent>
        </Card>
        
        <Card className="border-green-200 bg-gradient-to-br from-green-50 to-green-100 shadow-sm hover:shadow-md transition-shadow">
          <CardContent className="p-6 text-center">
            <div className="text-3xl font-bold text-green-700 mb-1">{successful}</div>
            <div className="text-sm font-medium text-green-600">Successful</div>
          </CardContent>
        </Card>
        
        <Card className="border-red-200 bg-gradient-to-br from-red-50 to-red-100 shadow-sm hover:shadow-md transition-shadow">
          <CardContent className="p-6 text-center">
            <div className="text-3xl font-bold text-red-700 mb-1">{errors}</div>
            <div className="text-sm font-medium text-red-600">Failed</div>
          </CardContent>
        </Card>
        
        <Card className="border-orange-200 bg-gradient-to-br from-orange-50 to-orange-100 shadow-sm hover:shadow-md transition-shadow">
          <CardContent className="p-6 text-center">
            <div className="text-3xl font-bold text-orange-700 mb-1">{processing}</div>
            <div className="text-sm font-medium text-orange-600">Processing</div>
          </CardContent>
        </Card>
      </div>

      {/* Processing Items */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h4 className="text-lg font-semibold text-gray-900">Processing Details</h4>
          <div className="text-sm text-gray-500">
            Real-time status for each record
          </div>
        </div>
        
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="max-h-80 overflow-y-auto">
            <AnimatePresence>
              {results.map((result, index) => (
                <motion.div
                  key={result.id}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  transition={{ delay: index * 0.05 }}
                  className={cn(
                    "flex items-center justify-between p-4 border-b border-gray-100 last:border-b-0 transition-all duration-200",
                    result.status === 'success' && "bg-gradient-to-r from-green-50/50 to-green-50/20 hover:from-green-50 hover:to-green-50/40",
                    result.status === 'error' && "bg-gradient-to-r from-red-50/50 to-red-50/20 hover:from-red-50 hover:to-red-50/40",
                    result.status === 'processing' && "bg-gradient-to-r from-orange-50/50 to-orange-50/20 hover:from-orange-50 hover:to-orange-50/40",
                    result.status === 'pending' && "bg-gradient-to-r from-gray-50/50 to-gray-50/20 hover:from-gray-50 hover:to-gray-50/40"
                  )}
                >
                  <div className="flex items-center gap-4">
                    <div className="flex-shrink-0">
                      {result.status === 'success' && (
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-green-100">
                          <CheckCircle className="h-5 w-5 text-green-600" />
                        </div>
                      )}
                      {result.status === 'error' && (
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-red-100">
                          <XCircle className="h-5 w-5 text-red-600" />
                        </div>
                      )}
                      {result.status === 'processing' && (
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-orange-100">
                          <motion.div
                            animate={{ rotate: 360 }}
                            transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                          >
                            <Clock className="h-5 w-5 text-orange-600" />
                          </motion.div>
                        </div>
                      )}
                      {result.status === 'pending' && (
                        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-100">
                          <Clock className="h-5 w-5 text-gray-400" />
                        </div>
                      )}
                    </div>
                    
                    <div className="min-w-0 flex-1">
                      <div className="text-sm font-semibold text-gray-900">
                        Row {result.row}
                        {result.data.firstName && result.data.lastName && (
                          <span className="ml-2 font-normal text-gray-600">
                            - {result.data.firstName} {result.data.lastName}
                          </span>
                        )}
                      </div>
                      {result.data.email && (
                        <div className="text-xs text-gray-500 mt-1">{result.data.email}</div>
                      )}
                      {result.error && (
                        <div className="text-xs text-red-600 mt-1 font-medium">{result.error}</div>
                      )}
                    </div>
                  </div>
                  
                  <Badge
                    className={cn(
                      "font-medium",
                      result.status === 'success' && "bg-green-100 text-green-700 border-green-200",
                      result.status === 'error' && "bg-red-100 text-red-700 border-red-200",
                      result.status === 'processing' && "bg-orange-100 text-orange-700 border-orange-200",
                      result.status === 'pending' && "bg-gray-100 text-gray-600 border-gray-200"
                    )}
                    variant="outline"
                  >
                    {result.status === 'success' && '✓ Success'}
                    {result.status === 'error' && '✗ Failed'}
                    {result.status === 'processing' && '⟳ Processing'}
                    {result.status === 'pending' && '◷ Pending'}
                  </Badge>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      {isComplete && (
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-6 border border-blue-200">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-r from-green-500 to-emerald-600">
                <CheckCircle className="h-5 w-5 text-white" />
              </div>
              <div>
                <div className="font-semibold text-gray-900">
                  Import Completed Successfully!
                </div>
                <div className="text-sm text-gray-600">
                  {successful} records imported, {errors} failed
                </div>
              </div>
            </div>
            <div className="flex gap-3">
              {errors > 0 && (
                <Button variant="outline" size="sm" className="border-red-300 text-red-700 hover:bg-red-50">
                  <Download className="h-4 w-4 mr-2" />
                  Download Error Report
                </Button>
              )}
              <Button 
                onClick={onClose}
                className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300"
              >
                Close & Continue
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export function BulkImportDialog({
  open,
  onOpenChange,
  userType,
  onImport
}: BulkImportDialogProps) {
  const [dragActive, setDragActive] = useState(false);
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [importResults, setImportResults] = useState<ImportResult[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);

  const getTemplateData = () => {
    switch (userType) {
      case 'students':
        return [
          { firstName: 'John', lastName: 'Doe', email: 'john.doe@school.edu', gradeLevel: '10', enrollmentYear: '2024' },
          { firstName: 'Jane', lastName: 'Smith', email: 'jane.smith@school.edu', gradeLevel: '11', enrollmentYear: '2024' },
          { firstName: 'Mike', lastName: 'Johnson', email: 'mike.johnson@school.edu', gradeLevel: '9', enrollmentYear: '2024' }
        ];
      case 'teachers':
        return [
          { firstName: 'Sarah', lastName: 'Wilson', email: 'sarah.wilson@school.edu', department: 'Mathematics', phoneNumber: '+1234567890' },
          { firstName: 'David', lastName: 'Brown', email: 'david.brown@school.edu', department: 'Science', phoneNumber: '+1234567891' },
          { firstName: 'Emily', lastName: 'Davis', email: 'emily.davis@school.edu', department: 'English', phoneNumber: '+1234567892' }
        ];
      case 'parents':
        return [
          { firstName: 'Robert', lastName: 'Miller', email: 'robert.miller@gmail.com', phoneNumber: '+1234567893', address: '123 Main St' },
          { firstName: 'Lisa', lastName: 'Anderson', email: 'lisa.anderson@gmail.com', phoneNumber: '+1234567894', address: '456 Oak Ave' },
          { firstName: 'Mark', lastName: 'Taylor', email: 'mark.taylor@gmail.com', phoneNumber: '+1234567895', address: '789 Pine Rd' }
        ];
      case 'staff':
        return [
          { firstName: 'Karen', lastName: 'White', email: 'karen.white@school.edu', department: 'Administration', position: 'Secretary' },
          { firstName: 'James', lastName: 'Garcia', email: 'james.garcia@school.edu', department: 'IT', position: 'Technician' },
          { firstName: 'Maria', lastName: 'Rodriguez', email: 'maria.rodriguez@school.edu', department: 'Maintenance', position: 'Custodian' }
        ];
      default:
        return [];
    }
  };

  const downloadTemplate = () => {
    try {
      const data = getTemplateData();
      
      if (data.length === 0) {
        toast.error('Template not available for this user type');
        return;
      }

      // Convert to CSV format
      const headers = Object.keys(data[0]);
      const csvContent = [
        headers.join(','), // Header row
        ...data.map(row => headers.map(header => `"${row[header as keyof typeof row]}"`).join(','))
      ].join('\n');

      // Create and download the file
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const link = document.createElement('a');
      const url = URL.createObjectURL(blob);
      
      link.setAttribute('href', url);
      link.setAttribute('download', `${userType}_import_template.csv`);
      link.style.visibility = 'hidden';
      
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      URL.revokeObjectURL(url);

      toast.success(`📄 Template downloaded: ${userType}_import_template.csv`);
      console.log(`📄 BulkImportDialog - Downloaded template for ${userType}`);
    } catch (error) {
      console.error('❌ Error downloading template:', error);
      toast.error('Failed to download template');
    }
  };

  const handleFileSelect = useCallback(async (file: File) => {
    setUploadedFile(file);
    setIsProcessing(true);
    
    try {
      console.log(`📁 BulkImportDialog - Processing ${userType} file:`, file.name);
      const results = await onImport(file);
      setImportResults(results);
    } catch (error) {
      console.error("❌ BulkImportDialog - Import failed:", error);
      // Handle error - could show error state
    }
  }, [onImport, userType]);

  const handleClose = useCallback(() => {
    setUploadedFile(null);
    setImportResults([]);
    setIsProcessing(false);
    onOpenChange(false);
  }, [onOpenChange]);

  const handleDragEnter = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
  }, []);

  const getUserTypeLabel = () => {
    return userType.charAt(0).toUpperCase() + userType.slice(1);
  };

  const getRequiredColumns = () => {
    const data = getTemplateData();
    if (data.length === 0) return ['firstName', 'lastName', 'email'];
    return Object.keys(data[0]);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPortal>
        <DialogOverlay className="fixed inset-0 z-50 bg-white/40 backdrop-blur-sm data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0" />
        <DialogContent 
          className="fixed left-[50%] top-[50%] z-50 max-w-5xl max-h-[95vh] translate-x-[-50%] translate-y-[-50%] overflow-hidden flex flex-col bg-white border-2 border-blue-200 shadow-2xl rounded-2xl"
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
        >
        <DialogHeader className="space-y-6 pb-6 border-b border-blue-100 bg-gradient-to-r from-blue-50/80 to-indigo-50/80 p-6 -m-6 mb-0 rounded-t-2xl">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-full bg-gradient-to-r from-blue-500 to-purple-600 shadow-lg">
              <Upload className="h-7 w-7 text-white" />
            </div>
            <div>
              <DialogTitle className="text-3xl font-bold text-gray-900 mb-2">
                Bulk Import {getUserTypeLabel()}
              </DialogTitle>
              <DialogDescription className="text-lg text-gray-700 font-medium">
                Upload CSV or Excel files to import multiple {userType} at once
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto py-8 px-6">
          {!uploadedFile && !isProcessing && (
            <div className="space-y-8">
              <FileDropZone
                onDrop={handleFileSelect}
                isDragActive={dragActive}
                userType={userType}
              />

              {/* Help Section */}
              <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl p-8 border border-blue-200 shadow-sm">
                <div className="flex items-start gap-5">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-500 shadow-md">
                    <AlertCircle className="h-5 w-5 text-white" />
                  </div>
                  <div className="space-y-4">
                    <h4 className="text-xl font-bold text-gray-900">Required Columns</h4>
                    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 text-sm">
                      {getRequiredColumns().map((column) => (
                        <div key={column} className="bg-white rounded-lg px-4 py-3 border border-blue-300 shadow-sm">
                          <code className="text-blue-700 font-bold text-base">{column}</code>
                        </div>
                      ))}
                    </div>
                    <p className="text-base text-gray-700 font-medium">
                      Make sure your file includes these columns for successful import. Download the template below for the exact format.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {(uploadedFile || importResults.length > 0) && (
            <ProcessingProgress
              results={importResults}
              fileName={uploadedFile?.name || ''}
              onClose={handleClose}
            />
          )}
        </div>

        {/* Footer */}
        <div className="border-t border-blue-100 pt-6 bg-gradient-to-r from-blue-50/50 to-indigo-50/50 px-6 -mx-6 mt-6 rounded-b-2xl">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100">
                <FileText className="h-4 w-4 text-blue-600" />
              </div>
              <span className="text-base font-medium text-gray-800">
                Need a template to get started?
              </span>
            </div>
            <Button 
              variant="outline" 
              size="lg" 
              onClick={downloadTemplate}
              className="border-blue-300 text-blue-700 hover:bg-blue-100 font-semibold shadow-sm"
            >
              <Download className="h-5 w-5 mr-2" />
              Download Template
            </Button>
          </div>
        </div>
        </DialogContent>
      </DialogPortal>
    </Dialog>
  );
}
