import React, { useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Download, 
  FileText, 
  Table, 
  File,
  Settings,
  Eye,
  Calendar,
  Users,
  MapPin,
  BookOpen,
  Palette,
  Layout,
  Filter,
  X,
  Check,
  AlertCircle,
  Info
} from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Separator } from '@/components/ui/separator';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Checkbox } from '@/components/ui/checkbox';
import { useExportTimetable } from '../hooks/use-smart-timetable';
import type { TimetableExportOptions } from '@/types/smart-timetable';

interface TimetableExportDialogProps {
  timetableId: number;
  timetableName: string;
  trigger?: React.ReactNode;
}

const EXPORT_FORMATS = [
  {
    value: 'PDF',
    label: 'PDF Document',
    icon: FileText,
    description: 'Professional PDF with formatting and styling',
    features: ['Print-ready', 'Professional layout', 'Color coding', 'Headers/Footers']
  },
  {
    value: 'EXCEL',
    label: 'Excel Spreadsheet',
    icon: Table,
    description: 'Editable Excel file with multiple sheets',
    features: ['Editable data', 'Multiple sheets', 'Formulas', 'Charts']
  },
  {
    value: 'CSV',
    label: 'CSV File',
    icon: File,
    description: 'Simple comma-separated values file',
    features: ['Universal format', 'Import anywhere', 'Lightweight', 'Data only']
  }
];

const TEMPLATES = [
  {
    id: 'professional',
    name: 'Professional',
    description: 'Clean, professional layout suitable for official documents',
    preview: '/templates/professional-preview.png'
  },
  {
    id: 'minimal',
    name: 'Minimal',
    description: 'Simple, clean layout with minimal styling',
    preview: '/templates/minimal-preview.png'
  },
  {
    id: 'colorful',
    name: 'Colorful',
    description: 'Vibrant colors with subject-based color coding',
    preview: '/templates/colorful-preview.png'
  }
];

export function TimetableExportDialog({ 
  timetableId, 
  timetableName, 
  trigger 
}: TimetableExportDialogProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedFormat, setSelectedFormat] = useState<'PDF' | 'EXCEL' | 'CSV'>('PDF');
  const [exportOptions, setExportOptions] = useState<TimetableExportOptions>({
    format: 'PDF',
    includeTeacherInfo: true,
    includeRoomInfo: true,
    includeConflicts: false,
    customTitle: '',
    orientation: 'landscape'
  });

  const exportMutation = useExportTimetable();

  const updateOptions = useCallback((updates: Partial<TimetableExportOptions>) => {
    setExportOptions(prev => ({ ...prev, ...updates }));
  }, []);

  const handleExport = useCallback(async () => {
    try {
      await exportMutation.mutateAsync({
        timetableId,
        format: selectedFormat,
        options: exportOptions
      });
      setIsOpen(false);
    } catch (error) {
      console.error('Export failed:', error);
    }
  }, [timetableId, selectedFormat, exportOptions, exportMutation]);

  const selectedFormatInfo = EXPORT_FORMATS.find(f => f.value === selectedFormat);

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        {trigger || (
          <Button variant="outline" className="flex items-center gap-2">
            <Download className="h-4 w-4" />
            Export Timetable
          </Button>
        )}
      </DialogTrigger>
      
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg">
              <Download className="h-5 w-5 text-white" />
            </div>
            Export Timetable: {timetableName}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Format Selection */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <File className="h-5 w-5" />
                Export Format
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {EXPORT_FORMATS.map((format) => {
                  const Icon = format.icon;
                  const isSelected = selectedFormat === format.value;
                  
                  return (
                    <motion.div
                      key={format.value}
                      className={`p-4 rounded-lg border-2 cursor-pointer transition-all ${
                        isSelected 
                          ? 'border-blue-500 bg-blue-50' 
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                      onClick={() => {
                        setSelectedFormat(format.value as any);
                        updateOptions({ format: format.value as any });
                      }}
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                    >
                      <div className="flex items-center gap-3 mb-3">
                        <Icon className={`h-6 w-6 ${isSelected ? 'text-blue-600' : 'text-gray-600'}`} />
                        <div>
                          <h3 className="font-semibold">{format.label}</h3>
                          {isSelected && <Check className="h-4 w-4 text-blue-600" />}
                        </div>
                      </div>
                      
                      <p className="text-sm text-gray-600 mb-3">{format.description}</p>
                      
                      <div className="flex flex-wrap gap-1">
                        {format.features.map((feature, index) => (
                          <Badge key={index} variant="secondary" className="text-xs">
                            {feature}
                          </Badge>
                        ))}
                      </div>
                    </motion.div>
                  );
                })}
              </div>
            </CardContent>
          </Card>

          {/* Export Options */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Settings className="h-5 w-5" />
                Export Options
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Tabs defaultValue="content" className="w-full">
                <TabsList className="grid w-full grid-cols-4">
                  <TabsTrigger value="content">Content</TabsTrigger>
                  <TabsTrigger value="formatting">Formatting</TabsTrigger>
                  <TabsTrigger value="filters">Filters</TabsTrigger>
                  <TabsTrigger value="advanced">Advanced</TabsTrigger>
                </TabsList>

                {/* Content Tab */}
                <TabsContent value="content" className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-4">
                      <div className="flex items-center justify-between">
                        <Label htmlFor="include-teacher" className="flex items-center gap-2">
                          <Users className="h-4 w-4" />
                          Include Teacher Information
                        </Label>
                        <Switch
                          id="include-teacher"
                          checked={exportOptions.includeTeacherInfo}
                          onCheckedChange={(checked) => updateOptions({ includeTeacherInfo: checked })}
                        />
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <Label htmlFor="include-room" className="flex items-center gap-2">
                          <MapPin className="h-4 w-4" />
                          Include Room Information
                        </Label>
                        <Switch
                          id="include-room"
                          checked={exportOptions.includeRoomInfo}
                          onCheckedChange={(checked) => updateOptions({ includeRoomInfo: checked })}
                        />
                      </div>
                      
                      <div className="flex items-center justify-between">
                        <Label htmlFor="include-conflicts" className="flex items-center gap-2">
                          <AlertCircle className="h-4 w-4" />
                          Include Conflicts
                        </Label>
                        <Switch
                          id="include-conflicts"
                          checked={exportOptions.includeConflicts}
                          onCheckedChange={(checked) => updateOptions({ includeConflicts: checked })}
                        />
                      </div>
                    </div>

                    <div className="space-y-4">
                      <div>
                        <Label htmlFor="custom-title" className="mb-2 block">Custom Title</Label>
                        <Input
                          id="custom-title"
                          placeholder="Enter custom title..."
                          value={exportOptions.customTitle || ''}
                          onChange={(e) => updateOptions({ customTitle: e.target.value })}
                        />
                      </div>
                      
                      <div>
                        <Label htmlFor="date-range" className="mb-2 block">Date Range (Optional)</Label>
                        <div className="grid grid-cols-2 gap-2">
                          <Input
                            type="date"
                            placeholder="Start date"
                            onChange={(e) => updateOptions({ 
                              dateRange: { 
                                ...exportOptions.dateRange, 
                                startDate: e.target.value 
                              } 
                            })}
                          />
                          <Input
                            type="date"
                            placeholder="End date"
                            onChange={(e) => updateOptions({ 
                              dateRange: { 
                                ...exportOptions.dateRange, 
                                endDate: e.target.value 
                              } 
                            })}
                          />
                        </div>
                      </div>
                    </div>
                  </div>
                </TabsContent>

                {/* Formatting Tab */}
                <TabsContent value="formatting" className="space-y-4">
                  <div className="grid grid-cols-2 gap-6">
                    <div className="space-y-4">
                      <div>
                        <Label className="mb-2 block">Page Orientation</Label>
                        <Select
                          value={exportOptions.orientation}
                          onValueChange={(value: any) => updateOptions({ orientation: value })}
                        >
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="portrait">Portrait</SelectItem>
                            <SelectItem value="landscape">Landscape</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>

                      {selectedFormat === 'PDF' && (
                        <div>
                          <Label className="mb-2 block">Template</Label>
                          <Select defaultValue="professional">
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {TEMPLATES.map(template => (
                                <SelectItem key={template.id} value={template.id}>
                                  {template.name}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        </div>
                      )}
                    </div>

                    <div className="space-y-4">
                      {selectedFormat === 'PDF' && (
                        <>
                          <Alert>
                            <Palette className="h-4 w-4" />
                            <AlertDescription>
                              PDF exports include professional styling with color coding and proper formatting.
                            </AlertDescription>
                          </Alert>
                        </>
                      )}
                      
                      {selectedFormat === 'EXCEL' && (
                        <>
                          <Alert>
                            <Table className="h-4 w-4" />
                            <AlertDescription>
                              Excel exports create structured spreadsheets with separate sheets for different views.
                            </AlertDescription>
                          </Alert>
                        </>
                      )}
                      
                      {selectedFormat === 'CSV' && (
                        <>
                          <Alert>
                            <File className="h-4 w-4" />
                            <AlertDescription>
                              CSV exports provide raw data that can be imported into any spreadsheet application.
                            </AlertDescription>
                          </Alert>
                        </>
                      )}
                    </div>
                  </div>
                </TabsContent>

                {/* Filters Tab */}
                <TabsContent value="filters" className="space-y-4">
                  <Alert>
                    <Filter className="h-4 w-4" />
                    <AlertDescription>
                      Apply filters to export only specific parts of your timetable.
                    </AlertDescription>
                  </Alert>
                  
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label className="mb-2 block">Days of Week</Label>
                      <div className="space-y-2">
                        {['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'].map(day => (
                          <div key={day} className="flex items-center space-x-2">
                            <Checkbox id={day} defaultChecked />
                            <Label htmlFor={day}>{day}</Label>
                          </div>
                        ))}
                      </div>
                    </div>
                    
                    <div className="space-y-4">
                      <div>
                        <Label className="mb-2 block">Specific Teachers (Optional)</Label>
                        <Input placeholder="Select teachers..." />
                      </div>
                      
                      <div>
                        <Label className="mb-2 block">Specific Classes (Optional)</Label>
                        <Input placeholder="Select classes..." />
                      </div>
                    </div>
                  </div>
                </TabsContent>

                {/* Advanced Tab */}
                <TabsContent value="advanced" className="space-y-4">
                  <div className="grid grid-cols-2 gap-6">
                    <div className="space-y-4">
                      <div>
                        <Label htmlFor="custom-header" className="mb-2 block">Custom Header</Label>
                        <Textarea
                          id="custom-header"
                          placeholder="Enter custom header text..."
                          rows={3}
                        />
                      </div>
                      
                      <div>
                        <Label htmlFor="custom-footer" className="mb-2 block">Custom Footer</Label>
                        <Textarea
                          id="custom-footer"
                          placeholder="Enter custom footer text..."
                          rows={3}
                        />
                      </div>
                    </div>
                    
                    <div className="space-y-4">
                      <Alert>
                        <Info className="h-4 w-4" />
                        <AlertDescription>
                          Advanced options allow you to customize headers, footers, and other export-specific settings.
                        </AlertDescription>
                      </Alert>
                      
                      {selectedFormat === 'CSV' && (
                        <div>
                          <Label className="mb-2 block">CSV Delimiter</Label>
                          <Select defaultValue=",">
                            <SelectTrigger>
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value=",">Comma (,)</SelectItem>
                              <SelectItem value=";">Semicolon (;)</SelectItem>
                              <SelectItem value="\t">Tab</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                      )}
                    </div>
                  </div>
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>

          {/* Preview & Export */}
          <Card>
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  {selectedFormatInfo && (
                    <>
                      <selectedFormatInfo.icon className="h-5 w-5 text-blue-600" />
                      <div>
                        <h3 className="font-semibold">{selectedFormatInfo.label}</h3>
                        <p className="text-sm text-gray-600">{selectedFormatInfo.description}</p>
                      </div>
                    </>
                  )}
                </div>
                
                <div className="flex items-center gap-3">
                  <Button variant="outline" className="flex items-center gap-2">
                    <Eye className="h-4 w-4" />
                    Preview
                  </Button>
                  
                  <Button
                    onClick={handleExport}
                    disabled={exportMutation.isPending}
                    className="flex items-center gap-2 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700"
                  >
                    {exportMutation.isPending ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                        Exporting...
                      </>
                    ) : (
                      <>
                        <Download className="h-4 w-4" />
                        Export {selectedFormat}
                      </>
                    )}
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </DialogContent>
    </Dialog>
  );
}
