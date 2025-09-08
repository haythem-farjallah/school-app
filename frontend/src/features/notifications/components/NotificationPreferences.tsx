import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Bell, 
  Mail, 
  MessageSquare, 
  Smartphone,
  Clock,
  Volume2,
  VolumeX,
  Moon,
  Sun,
  Settings,
  Save,
  RotateCcw,
  CheckCircle,
  AlertCircle,
  GraduationCap,
  Calendar,
  DollarSign,
  Megaphone,
  UserCheck,
  AlertTriangle
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useNotificationPreferences, useUpdateNotificationPreferences } from '../hooks/use-notifications';
import type { NotificationPreferences } from '@/types/notification';
import toast from 'react-hot-toast';

interface NotificationPreferencesProps {
  userId: string;
  className?: string;
}

export function NotificationPreferences({ userId, className }: NotificationPreferencesProps) {
  const [preferences, setPreferences] = useState<NotificationPreferences>({
    emailEnabled: true,
    smsEnabled: true,
    pushEnabled: true,
    inAppEnabled: true,
    gradeNotifications: true,
    assignmentNotifications: true,
    attendanceNotifications: true,
    announcementNotifications: true,
    scheduleNotifications: true,
    paymentNotifications: true,
    systemNotifications: true,
    quietHoursEnabled: false,
    quietHoursStart: '22:00',
    quietHoursEnd: '07:00',
    digestFrequency: 'NONE'
  });

  const [hasChanges, setHasChanges] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Hooks
  const { data: currentPreferences, isLoading } = useNotificationPreferences(userId);
  const updatePreferencesMutation = useUpdateNotificationPreferences();

  // Load current preferences
  useEffect(() => {
    if (currentPreferences) {
      setPreferences(currentPreferences);
    }
  }, [currentPreferences]);

  const handlePreferenceChange = (key: keyof NotificationPreferences, value: any) => {
    setPreferences(prev => ({
      ...prev,
      [key]: value
    }));
    setHasChanges(true);
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await updatePreferencesMutation.mutateAsync({
        userId,
        preferences
      });
      setHasChanges(false);
      toast.success('Notification preferences saved successfully');
    } catch (error) {
      toast.error('Failed to save notification preferences');
    } finally {
      setIsSaving(false);
    }
  };

  const handleReset = () => {
    if (currentPreferences) {
      setPreferences(currentPreferences);
      setHasChanges(false);
      toast.success('Preferences reset to saved values');
    }
  };

  const ChannelSection = ({ 
    title, 
    description, 
    icon: Icon, 
    enabled, 
    onToggle, 
    color = 'blue' 
  }: {
    title: string;
    description: string;
    icon: React.ElementType;
    enabled: boolean;
    onToggle: (enabled: boolean) => void;
    color?: string;
  }) => (
    <Card className={`transition-all duration-200 ${enabled ? `border-${color}-200 bg-${color}-50/30` : 'border-gray-200'}`}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={`p-2 rounded-lg ${enabled ? `bg-${color}-100` : 'bg-gray-100'}`}>
              <Icon className={`h-5 w-5 ${enabled ? `text-${color}-600` : 'text-gray-500'}`} />
            </div>
            <div>
              <CardTitle className="text-base">{title}</CardTitle>
              <CardDescription className="text-sm">{description}</CardDescription>
            </div>
          </div>
          <Switch
            checked={enabled}
            onCheckedChange={onToggle}
          />
        </div>
      </CardHeader>
    </Card>
  );

  const NotificationTypeSection = ({ 
    title, 
    description, 
    icon: Icon, 
    enabled, 
    onToggle, 
    color = 'blue' 
  }: {
    title: string;
    description: string;
    icon: React.ElementType;
    enabled: boolean;
    onToggle: (enabled: boolean) => void;
    color?: string;
  }) => (
    <div className="flex items-center justify-between p-4 rounded-lg border hover:bg-gray-50 transition-colors">
      <div className="flex items-center gap-3">
        <Icon className={`h-5 w-5 text-${color}-600`} />
        <div>
          <Label className="text-sm font-medium">{title}</Label>
          <p className="text-xs text-gray-500 mt-1">{description}</p>
        </div>
      </div>
      <Switch
        checked={enabled}
        onCheckedChange={onToggle}
      />
    </div>
  );

  if (isLoading) {
    return (
      <div className="space-y-6 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/3" />
        <div className="space-y-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="h-20 bg-gray-200 rounded" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <Settings className="h-6 w-6" />
            Notification Preferences
          </h1>
          <p className="text-gray-600 mt-1">Customize how and when you receive notifications</p>
        </div>
        
        {/* Action Buttons */}
        <div className="flex items-center gap-3">
          {hasChanges && (
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="flex items-center gap-2"
            >
              <Badge variant="secondary" className="bg-yellow-100 text-yellow-800">
                <AlertCircle className="h-3 w-3 mr-1" />
                Unsaved Changes
              </Badge>
              <Button
                variant="outline"
                size="sm"
                onClick={handleReset}
                disabled={isSaving}
              >
                <RotateCcw className="h-4 w-4 mr-2" />
                Reset
              </Button>
            </motion.div>
          )}
          <Button
            onClick={handleSave}
            disabled={!hasChanges || isSaving}
            className="min-w-24"
          >
            {isSaving ? (
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
              >
                <RotateCcw className="h-4 w-4 mr-2" />
              </motion.div>
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            {isSaving ? 'Saving...' : 'Save Changes'}
          </Button>
        </div>
      </div>

      <Tabs defaultValue="channels" className="w-full">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="channels">Channels</TabsTrigger>
          <TabsTrigger value="types">Notification Types</TabsTrigger>
          <TabsTrigger value="schedule">Schedule</TabsTrigger>
          <TabsTrigger value="digest">Digest</TabsTrigger>
        </TabsList>

        {/* Notification Channels */}
        <TabsContent value="channels" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Communication Channels</CardTitle>
              <CardDescription>
                Choose how you want to receive notifications across different channels
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <ChannelSection
                title="Email Notifications"
                description="Receive notifications via email"
                icon={Mail}
                enabled={preferences.emailEnabled}
                onToggle={(enabled) => handlePreferenceChange('emailEnabled', enabled)}
                color="blue"
              />
              
              <ChannelSection
                title="SMS Notifications"
                description="Receive notifications via text message"
                icon={MessageSquare}
                enabled={preferences.smsEnabled}
                onToggle={(enabled) => handlePreferenceChange('smsEnabled', enabled)}
                color="green"
              />
              
              <ChannelSection
                title="Push Notifications"
                description="Receive notifications on your device"
                icon={Smartphone}
                enabled={preferences.pushEnabled}
                onToggle={(enabled) => handlePreferenceChange('pushEnabled', enabled)}
                color="purple"
              />
              
              <ChannelSection
                title="In-App Notifications"
                description="Receive notifications within the application"
                icon={Bell}
                enabled={preferences.inAppEnabled}
                onToggle={(enabled) => handlePreferenceChange('inAppEnabled', enabled)}
                color="orange"
              />
            </CardContent>
          </Card>
        </TabsContent>

        {/* Notification Types */}
        <TabsContent value="types" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Notification Types</CardTitle>
              <CardDescription>
                Control which types of notifications you want to receive
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <NotificationTypeSection
                title="Grade Notifications"
                description="New grades and grade updates"
                icon={GraduationCap}
                enabled={preferences.gradeNotifications}
                onToggle={(enabled) => handlePreferenceChange('gradeNotifications', enabled)}
                color="green"
              />
              
              <NotificationTypeSection
                title="Assignment Notifications"
                description="Assignment due dates and updates"
                icon={Calendar}
                enabled={preferences.assignmentNotifications}
                onToggle={(enabled) => handlePreferenceChange('assignmentNotifications', enabled)}
                color="blue"
              />
              
              <NotificationTypeSection
                title="Attendance Notifications"
                description="Attendance alerts and reports"
                icon={UserCheck}
                enabled={preferences.attendanceNotifications}
                onToggle={(enabled) => handlePreferenceChange('attendanceNotifications', enabled)}
                color="yellow"
              />
              
              <NotificationTypeSection
                title="Announcements"
                description="School announcements and news"
                icon={Megaphone}
                enabled={preferences.announcementNotifications}
                onToggle={(enabled) => handlePreferenceChange('announcementNotifications', enabled)}
                color="purple"
              />
              
              <NotificationTypeSection
                title="Schedule Changes"
                description="Timetable and schedule updates"
                icon={Clock}
                enabled={preferences.scheduleNotifications}
                onToggle={(enabled) => handlePreferenceChange('scheduleNotifications', enabled)}
                color="indigo"
              />
              
              <NotificationTypeSection
                title="Payment Reminders"
                description="Fee payments and financial alerts"
                icon={DollarSign}
                enabled={preferences.paymentNotifications}
                onToggle={(enabled) => handlePreferenceChange('paymentNotifications', enabled)}
                color="orange"
              />
              
              <NotificationTypeSection
                title="System Notifications"
                description="System updates and maintenance alerts"
                icon={AlertTriangle}
                enabled={preferences.systemNotifications}
                onToggle={(enabled) => handlePreferenceChange('systemNotifications', enabled)}
                color="red"
              />
            </CardContent>
          </Card>
        </TabsContent>

        {/* Schedule Settings */}
        <TabsContent value="schedule" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Quiet Hours</CardTitle>
              <CardDescription>
                Set times when you don't want to receive notifications
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between p-4 rounded-lg border">
                <div className="flex items-center gap-3">
                  <Moon className="h-5 w-5 text-indigo-600" />
                  <div>
                    <Label className="text-sm font-medium">Enable Quiet Hours</Label>
                    <p className="text-xs text-gray-500 mt-1">
                      Pause non-urgent notifications during specified hours
                    </p>
                  </div>
                </div>
                <Switch
                  checked={preferences.quietHoursEnabled}
                  onCheckedChange={(enabled) => handlePreferenceChange('quietHoursEnabled', enabled)}
                />
              </div>

              {preferences.quietHoursEnabled && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="space-y-4 pl-4 border-l-2 border-indigo-200"
                >
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label className="text-sm font-medium mb-2 block">Start Time</Label>
                      <Select
                        value={preferences.quietHoursStart}
                        onValueChange={(value) => handlePreferenceChange('quietHoursStart', value)}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {Array.from({ length: 24 }, (_, i) => {
                            const hour = i.toString().padStart(2, '0');
                            return (
                              <SelectItem key={`${hour}:00`} value={`${hour}:00`}>
                                {`${hour}:00`}
                              </SelectItem>
                            );
                          })}
                        </SelectContent>
                      </Select>
                    </div>
                    <div>
                      <Label className="text-sm font-medium mb-2 block">End Time</Label>
                      <Select
                        value={preferences.quietHoursEnd}
                        onValueChange={(value) => handlePreferenceChange('quietHoursEnd', value)}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {Array.from({ length: 24 }, (_, i) => {
                            const hour = i.toString().padStart(2, '0');
                            return (
                              <SelectItem key={`${hour}:00`} value={`${hour}:00`}>
                                {`${hour}:00`}
                              </SelectItem>
                            );
                          })}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-gray-600 bg-blue-50 p-3 rounded-lg">
                    <Sun className="h-4 w-4" />
                    <span>
                      Quiet hours: {preferences.quietHoursStart} - {preferences.quietHoursEnd}
                    </span>
                  </div>
                </motion.div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Digest Settings */}
        <TabsContent value="digest" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Notification Digest</CardTitle>
              <CardDescription>
                Receive a summary of notifications instead of individual alerts
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label className="text-sm font-medium mb-3 block">Digest Frequency</Label>
                <Select
                  value={preferences.digestFrequency}
                  onValueChange={(value: 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY') => 
                    handlePreferenceChange('digestFrequency', value)
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="NONE">No Digest (Individual notifications)</SelectItem>
                    <SelectItem value="DAILY">Daily Digest</SelectItem>
                    <SelectItem value="WEEKLY">Weekly Digest</SelectItem>
                    <SelectItem value="MONTHLY">Monthly Digest</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {preferences.digestFrequency !== 'NONE' && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  className="p-4 bg-blue-50 rounded-lg border border-blue-200"
                >
                  <div className="flex items-start gap-3">
                    <CheckCircle className="h-5 w-5 text-blue-600 mt-0.5" />
                    <div>
                      <h4 className="text-sm font-medium text-blue-900">
                        {preferences.digestFrequency.charAt(0) + preferences.digestFrequency.slice(1).toLowerCase()} Digest Enabled
                      </h4>
                      <p className="text-xs text-blue-700 mt-1">
                        You'll receive a summary of all notifications {preferences.digestFrequency.toLowerCase()} 
                        instead of individual alerts. Urgent notifications will still be delivered immediately.
                      </p>
                    </div>
                  </div>
                </motion.div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
