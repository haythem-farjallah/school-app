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
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { 
  Eye, 
  EyeOff, 
  Users, 
  BookOpen, 
  Search,
  Globe,
  Lock,
  CheckCircle,
  Circle,
  Loader2
} from "lucide-react";
import toast from "react-hot-toast";

import type { LearningResource } from "@/types/learning-resource";
import { useClasses } from "@/features/classes/hooks/use-classes";
import { useCourses } from "@/features/courses/hooks/use-courses";

interface ResourceVisibilityDialogProps {
  resource: LearningResource | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave?: (resourceId: number, settings: VisibilitySettings) => void;
}

interface VisibilitySettings {
  isPublic: boolean;
  classIds: number[];
  courseIds: number[];
}

interface ClassItem {
  id: number;
  name: string;
  level: string;
  studentCount?: number;
}

interface CourseItem {
  id: number;
  name: string;
  code: string;
  level?: string;
}

export function ResourceVisibilityDialog({ 
  resource, 
  open, 
  onOpenChange, 
  onSave 
}: ResourceVisibilityDialogProps) {
  const [isPublic, setIsPublic] = React.useState(true);
  const [selectedClassIds, setSelectedClassIds] = React.useState<number[]>([]);
  const [selectedCourseIds, setSelectedCourseIds] = React.useState<number[]>([]);
  const [classSearch, setClassSearch] = React.useState("");
  const [courseSearch, setCourseSearch] = React.useState("");
  const [isSaving, setIsSaving] = React.useState(false);

  // Fetch classes and courses
  const { data: classesData, isLoading: classesLoading } = useClasses({ size: 100 });
  const { data: coursesData, isLoading: coursesLoading } = useCourses({ size: 100 });

  const classes: ClassItem[] = classesData?.data || [];
  const courses: CourseItem[] = coursesData?.data || [];

  // Initialize form when resource changes
  React.useEffect(() => {
    if (resource) {
      setIsPublic(resource.isPublic);
      setSelectedClassIds(resource.classIds || []);
      setSelectedCourseIds(resource.courseIds || []);
    }
  }, [resource]);

  // Filter classes and courses based on search
  const filteredClasses = classes.filter(cls =>
    cls.name.toLowerCase().includes(classSearch.toLowerCase()) ||
    cls.level.toLowerCase().includes(classSearch.toLowerCase())
  );

  const filteredCourses = courses.filter(course =>
    course.name.toLowerCase().includes(courseSearch.toLowerCase()) ||
    course.code.toLowerCase().includes(courseSearch.toLowerCase())
  );

  // Handle class selection
  const handleClassToggle = (classId: number) => {
    setSelectedClassIds(prev => 
      prev.includes(classId)
        ? prev.filter(id => id !== classId)
        : [...prev, classId]
    );
  };

  // Handle course selection
  const handleCourseToggle = (courseId: number) => {
    setSelectedCourseIds(prev => 
      prev.includes(courseId)
        ? prev.filter(id => id !== courseId)
        : [...prev, courseId]
    );
  };

  // Handle select all classes
  const handleSelectAllClasses = () => {
    if (selectedClassIds.length === filteredClasses.length) {
      setSelectedClassIds([]);
    } else {
      setSelectedClassIds(filteredClasses.map(cls => cls.id));
    }
  };

  // Handle select all courses
  const handleSelectAllCourses = () => {
    if (selectedCourseIds.length === filteredCourses.length) {
      setSelectedCourseIds([]);
    } else {
      setSelectedCourseIds(filteredCourses.map(course => course.id));
    }
  };

  // Handle save
  const handleSave = async () => {
    if (!resource) return;

    setIsSaving(true);
    
    try {
      const settings: VisibilitySettings = {
        isPublic,
        classIds: selectedClassIds,
        courseIds: selectedCourseIds,
      };

      await onSave?.(resource.id, settings);
      toast.success("Visibility settings updated successfully");
      onOpenChange(false);
    } catch (error) {
      toast.error("Failed to update visibility settings");
    } finally {
      setIsSaving(false);
    }
  };

  // Handle close
  const handleClose = () => {
    if (resource) {
      setIsPublic(resource.isPublic);
      setSelectedClassIds(resource.classIds || []);
      setSelectedCourseIds(resource.courseIds || []);
    }
    setClassSearch("");
    setCourseSearch("");
    onOpenChange(false);
  };

  if (!resource) return null;

  const hasChanges = 
    isPublic !== resource.isPublic ||
    JSON.stringify(selectedClassIds.sort()) !== JSON.stringify((resource.classIds || []).sort()) ||
    JSON.stringify(selectedCourseIds.sort()) !== JSON.stringify((resource.courseIds || []).sort());

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center">
            <Eye className="h-5 w-5 mr-2" />
            Resource Visibility Settings
          </DialogTitle>
          <DialogDescription>
            Control who can access "{resource.title}"
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 overflow-auto max-h-[60vh] px-1">
          {/* Public/Private Toggle */}
          <Card>
            <CardHeader className="pb-4">
              <CardTitle className="text-base flex items-center">
                {isPublic ? (
                  <Globe className="h-4 w-4 mr-2 text-green-600" />
                ) : (
                  <Lock className="h-4 w-4 mr-2 text-orange-600" />
                )}
                General Visibility
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between p-4 border rounded-lg">
                <div className="flex items-center space-x-3">
                  <div className={`h-10 w-10 rounded-full flex items-center justify-center ${
                    isPublic 
                      ? 'bg-gradient-to-r from-green-100 to-emerald-100 border border-green-200'
                      : 'bg-gradient-to-r from-orange-100 to-yellow-100 border border-orange-200'
                  }`}>
                    {isPublic ? (
                      <Globe className="h-5 w-5 text-green-600" />
                    ) : (
                      <Lock className="h-5 w-5 text-orange-600" />
                    )}
                  </div>
                  <div>
                    <Label className="text-base font-semibold">
                      {isPublic ? 'Public Resource' : 'Private Resource'}
                    </Label>
                    <p className="text-sm text-slate-600">
                      {isPublic 
                        ? 'Anyone with access to the system can view this resource'
                        : 'Only selected classes and courses can access this resource'
                      }
                    </p>
                  </div>
                </div>
                <Switch
                  checked={isPublic}
                  onCheckedChange={setIsPublic}
                  disabled={isSaving}
                />
              </div>
              
              {!isPublic && (
                <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
                  <div className="flex items-start space-x-2">
                    <Lock className="h-4 w-4 text-orange-600 mt-0.5" />
                    <div>
                      <p className="text-sm font-medium text-orange-800">Private Resource</p>
                      <p className="text-xs text-orange-700 mt-1">
                        Select specific classes and courses below to grant access
                      </p>
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Class Selection */}
          {!isPublic && (
            <Card>
              <CardHeader className="pb-4">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-base flex items-center">
                    <Users className="h-4 w-4 mr-2" />
                    Classes ({selectedClassIds.length} selected)
                  </CardTitle>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleSelectAllClasses}
                    disabled={classesLoading || isSaving}
                  >
                    {selectedClassIds.length === filteredClasses.length ? 'Deselect All' : 'Select All'}
                  </Button>
                </div>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <Input
                    placeholder="Search classes..."
                    value={classSearch}
                    onChange={(e) => setClassSearch(e.target.value)}
                    className="pl-10"
                    disabled={isSaving}
                  />
                </div>
              </CardHeader>
              <CardContent>
                {classesLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
                  </div>
                ) : (
                  <ScrollArea className="h-48">
                    <div className="space-y-2">
                      {filteredClasses.map((cls) => (
                        <div
                          key={cls.id}
                          className="flex items-center space-x-3 p-3 border rounded-lg hover:bg-slate-50 cursor-pointer"
                          onClick={() => handleClassToggle(cls.id)}
                        >
                          <Checkbox
                            checked={selectedClassIds.includes(cls.id)}
                            onChange={() => handleClassToggle(cls.id)}
                          />
                          <div className="flex-1">
                            <div className="flex items-center justify-between">
                              <div>
                                <p className="font-medium text-slate-900">{cls.name}</p>
                                <p className="text-sm text-slate-600">{cls.level}</p>
                              </div>
                              {cls.studentCount && (
                                <Badge variant="outline" className="text-xs">
                                  {cls.studentCount} students
                                </Badge>
                              )}
                            </div>
                          </div>
                        </div>
                      ))}
                      
                      {filteredClasses.length === 0 && (
                        <div className="text-center py-8 text-slate-500">
                          <Users className="h-8 w-8 mx-auto mb-2 text-slate-300" />
                          <p>No classes found</p>
                        </div>
                      )}
                    </div>
                  </ScrollArea>
                )}
              </CardContent>
            </Card>
          )}

          {/* Course Selection */}
          {!isPublic && (
            <Card>
              <CardHeader className="pb-4">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-base flex items-center">
                    <BookOpen className="h-4 w-4 mr-2" />
                    Courses ({selectedCourseIds.length} selected)
                  </CardTitle>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleSelectAllCourses}
                    disabled={coursesLoading || isSaving}
                  >
                    {selectedCourseIds.length === filteredCourses.length ? 'Deselect All' : 'Select All'}
                  </Button>
                </div>
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                  <Input
                    placeholder="Search courses..."
                    value={courseSearch}
                    onChange={(e) => setCourseSearch(e.target.value)}
                    className="pl-10"
                    disabled={isSaving}
                  />
                </div>
              </CardHeader>
              <CardContent>
                {coursesLoading ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
                  </div>
                ) : (
                  <ScrollArea className="h-48">
                    <div className="space-y-2">
                      {filteredCourses.map((course) => (
                        <div
                          key={course.id}
                          className="flex items-center space-x-3 p-3 border rounded-lg hover:bg-slate-50 cursor-pointer"
                          onClick={() => handleCourseToggle(course.id)}
                        >
                          <Checkbox
                            checked={selectedCourseIds.includes(course.id)}
                            onChange={() => handleCourseToggle(course.id)}
                          />
                          <div className="flex-1">
                            <div className="flex items-center justify-between">
                              <div>
                                <p className="font-medium text-slate-900">{course.name}</p>
                                <p className="text-sm text-slate-600">{course.code}</p>
                              </div>
                              {course.level && (
                                <Badge variant="outline" className="text-xs">
                                  {course.level}
                                </Badge>
                              )}
                            </div>
                          </div>
                        </div>
                      ))}
                      
                      {filteredCourses.length === 0 && (
                        <div className="text-center py-8 text-slate-500">
                          <BookOpen className="h-8 w-8 mx-auto mb-2 text-slate-300" />
                          <p>No courses found</p>
                        </div>
                      )}
                    </div>
                  </ScrollArea>
                )}
              </CardContent>
            </Card>
          )}

          {/* Summary */}
          {hasChanges && (
            <Card className="bg-blue-50 border-blue-200">
              <CardContent className="p-4">
                <div className="flex items-start space-x-2">
                  <CheckCircle className="h-4 w-4 text-blue-600 mt-0.5" />
                  <div>
                    <p className="text-sm font-medium text-blue-800">Changes Summary</p>
                    <div className="text-xs text-blue-700 mt-1 space-y-1">
                      <p>• Visibility: {isPublic ? 'Public' : 'Private'}</p>
                      {!isPublic && (
                        <>
                          <p>• Classes: {selectedClassIds.length} selected</p>
                          <p>• Courses: {selectedCourseIds.length} selected</p>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={handleClose}
            disabled={isSaving}
          >
            Cancel
          </Button>
          <Button
            onClick={handleSave}
            disabled={isSaving || !hasChanges}
            className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700"
          >
            {isSaving ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Eye className="h-4 w-4 mr-2" />
                Save Settings
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
