import * as React from "react";
import { Helmet } from "react-helmet-async";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { 
  Target, 
  List, 
  ArrowLeft
} from "lucide-react";
import { Link } from "react-router-dom";

import { TeacherCourseLinkingInterface } from "@/features/teaching-assignments/components/teacher-course-linking-interface";
import { TeachingAssignmentsTable } from "@/features/teaching-assignments/components/teaching-assignments-table";

export default function TeachingAssignmentsLinkingPage() {
  const [activeView, setActiveView] = React.useState<"linking" | "management">("linking");

  return (
    <>
      <Helmet>
        <title>Teaching Assignments - School Management</title>
        <meta name="description" content="Manage teacher-course assignments and teaching schedules" />
      </Helmet>
      
      <div className="container mx-auto py-6 space-y-8">
        {/* Navigation */}
        <div className="flex items-center space-x-4">
          <Button variant="ghost" asChild>
            <Link to="/admin/dashboard">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Dashboard
            </Link>
          </Button>
        </div>

        {/* Header */}
        <Card className="border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-blue-50/40">
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-3xl font-bold text-slate-900">
                  Teaching Assignments
                </h1>
                <p className="text-slate-600 mt-2">
                  Manage teacher-course assignments and teaching schedules
                </p>
              </div>
              
                               <Tabs value={activeView} onValueChange={(value) => setActiveView(value as "linking" | "management")}>
                <TabsList className="grid w-full grid-cols-2">
                  <TabsTrigger value="linking" className="flex items-center space-x-2">
                    <Target className="h-4 w-4" />
                    <span>Assignment Interface</span>
                  </TabsTrigger>
                  <TabsTrigger value="management" className="flex items-center space-x-2">
                    <List className="h-4 w-4" />
                    <span>Manage Assignments</span>
                  </TabsTrigger>
                </TabsList>
              </Tabs>
            </div>
          </CardContent>
        </Card>

        {/* Content */}
        {activeView === "linking" ? (
          <TeacherCourseLinkingInterface />
        ) : (
          <TeachingAssignmentsTable />
        )}
      </div>
    </>
  );
}
