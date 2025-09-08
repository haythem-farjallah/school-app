import React from "react";
import { Helmet } from "react-helmet-async";
import { TeachingAssignmentsTable } from "@/features/teaching-assignments/components/teaching-assignments-table";

export default function TeachingAssignments() {
  console.log("🎓 TeachingAssignments - Page rendered");

  return (
    <>
      <Helmet>
        <title>Teaching Assignments | School Management</title>
        <meta name="description" content="Manage teacher-course assignments and class schedules" />
      </Helmet>
      
      <div className="flex-1 space-y-4 p-4 md:p-8 pt-6">
        <TeachingAssignmentsTable />
      </div>
    </>
  );
}

