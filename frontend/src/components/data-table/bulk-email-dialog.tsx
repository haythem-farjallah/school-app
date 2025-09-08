import * as React from "react";
import { useState } from "react";
import toast from "react-hot-toast";
import { Send, Mail, MessageSquare, Link as LinkIcon, X } from "lucide-react";
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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export interface BulkEmailDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  userType: 'students' | 'teachers' | 'staff' | 'parents';
  selectedCount: number;
  onSendEmail: (emailData: {
    subject: string;
    message: string;
    actionUrl?: string;
    actionText?: string;
  }) => Promise<void>;
  isLoading?: boolean;
}

const EMAIL_TEMPLATES = {
  welcome: {
    subject: "Welcome to Our School System",
    message: "Welcome! We're excited to have you as part of our school community. Please log in to access your dashboard and explore the available features.",
    actionText: "Login Now",
    actionUrl: "/login"
  },
  reminder: {
    subject: "Important Reminder",
    message: "This is a friendly reminder about upcoming deadlines and important announcements. Please check your dashboard for the latest updates.",
    actionText: "View Dashboard",
    actionUrl: "/dashboard"
  },
  notification: {
    subject: "System Notification",
    message: "We have important updates to share with you. Please review the information in your account.",
    actionText: "View Details",
    actionUrl: "/notifications"
  },
  custom: {
    subject: "",
    message: "",
    actionText: "",
    actionUrl: ""
  }
};

export function BulkEmailDialog({
  open,
  onOpenChange,
  userType,
  selectedCount,
  onSendEmail,
  isLoading = false
}: BulkEmailDialogProps) {
  const [selectedTemplate, setSelectedTemplate] = useState<keyof typeof EMAIL_TEMPLATES>('custom');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [actionUrl, setActionUrl] = useState('');
  const [actionText, setActionText] = useState('');
  const [showAdvanced, setShowAdvanced] = useState(false);

  // Reset form when dialog opens/closes
  React.useEffect(() => {
    if (open) {
      setSelectedTemplate('custom');
      setSubject('');
      setMessage('');
      setActionUrl('');
      setActionText('');
      setShowAdvanced(false);
    }
  }, [open]);

  // Update form when template changes
  React.useEffect(() => {
    if (selectedTemplate !== 'custom') {
      const template = EMAIL_TEMPLATES[selectedTemplate];
      setSubject(template.subject);
      setMessage(template.message);
      setActionUrl(template.actionUrl || '');
      setActionText(template.actionText || '');
    }
  }, [selectedTemplate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!subject.trim() || !message.trim()) {
      toast.error('Please fill in both subject and message');
      return;
    }

    try {
      await onSendEmail({
        subject: subject.trim(),
        message: message.trim(),
        actionUrl: actionUrl.trim() || undefined,
        actionText: actionText.trim() || undefined,
      });
      onOpenChange(false);
    } catch (error) {
      console.error('Failed to send email:', error);
    }
  };

  const userDisplayName = userType.charAt(0).toUpperCase() + userType.slice(1);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogPortal>
        <DialogOverlay className="bg-black/50" />
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Mail className="h-5 w-5" />
              Send Bulk Email
            </DialogTitle>
            <DialogDescription>
              Send an email to {selectedCount} selected {userDisplayName.toLowerCase()}
              {selectedCount !== 1 ? 's' : ''}
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Template Selection */}
            <div className="space-y-3">
              <Label className="text-sm font-medium">Email Template</Label>
              <div className="grid grid-cols-2 gap-2">
                {Object.entries(EMAIL_TEMPLATES).map(([key, template]) => (
                  <button
                    key={key}
                    type="button"
                    onClick={() => setSelectedTemplate(key as keyof typeof EMAIL_TEMPLATES)}
                    className={cn(
                      "p-3 text-left border rounded-lg transition-all",
                      selectedTemplate === key
                        ? "border-blue-500 bg-blue-50 text-blue-900"
                        : "border-gray-200 hover:border-gray-300"
                    )}
                  >
                    <div className="font-medium text-sm">
                      {key === 'custom' ? 'Custom Email' : template.subject || 'Template'}
                    </div>
                    <div className="text-xs text-gray-500 mt-1">
                      {key === 'custom' 
                        ? 'Create your own email'
                        : template.message.substring(0, 60) + '...'
                      }
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* Recipients Info */}
            <Card>
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <MessageSquare className="h-4 w-4 text-gray-500" />
                    <span className="text-sm text-gray-600">Recipients:</span>
                  </div>
                  <Badge variant="secondary">
                    {selectedCount} {userDisplayName.toLowerCase()}
                    {selectedCount !== 1 ? 's' : ''}
                  </Badge>
                </div>
              </CardContent>
            </Card>

            {/* Email Form */}
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="subject">Subject *</Label>
                <Input
                  id="subject"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  placeholder="Enter email subject"
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="message">Message *</Label>
                <Textarea
                  id="message"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder="Enter your message here..."
                  className="min-h-[120px]"
                  required
                />
                <p className="text-xs text-gray-500">
                  Tip: Use {"{firstName}"} and {"{lastName}"} for personalization
                </p>
              </div>

              {/* Advanced Options */}
              <div className="border-t pt-4">
                <button
                  type="button"
                  onClick={() => setShowAdvanced(!showAdvanced)}
                  className="flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900"
                >
                  <LinkIcon className="h-4 w-4" />
                  Advanced Options
                  <span className="text-xs">(Optional action button)</span>
                </button>

                <AnimatePresence>
                  {showAdvanced && (
                    <motion.div
                      initial={{ height: 0, opacity: 0 }}
                      animate={{ height: "auto", opacity: 1 }}
                      exit={{ height: 0, opacity: 0 }}
                      className="overflow-hidden"
                    >
                      <div className="space-y-4 mt-4 pt-4 border-t">
                        <div className="space-y-2">
                          <Label htmlFor="actionText">Action Button Text</Label>
                          <Input
                            id="actionText"
                            value={actionText}
                            onChange={(e) => setActionText(e.target.value)}
                            placeholder="e.g., View Dashboard, Login Now"
                          />
                        </div>

                        <div className="space-y-2">
                          <Label htmlFor="actionUrl">Action URL</Label>
                          <Input
                            id="actionUrl"
                            value={actionUrl}
                            onChange={(e) => setActionUrl(e.target.value)}
                            placeholder="e.g., /dashboard, https://example.com"
                          />
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-between pt-4 border-t">
              <Button 
                type="button" 
                variant="outline" 
                onClick={() => onOpenChange(false)}
                disabled={isLoading}
              >
                Cancel
              </Button>
              <Button 
                type="submit" 
                disabled={isLoading || !subject.trim() || !message.trim()}
                className="flex items-center gap-2"
              >
                {isLoading ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white" />
                    Sending...
                  </>
                ) : (
                  <>
                    <Send className="h-4 w-4" />
                    Send Email
                  </>
                )}
              </Button>
            </div>
          </form>
        </DialogContent>
      </DialogPortal>
    </Dialog>
  );
}
