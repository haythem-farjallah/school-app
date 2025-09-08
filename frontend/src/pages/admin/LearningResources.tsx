import * as React from "react";
import { Helmet } from "react-helmet-async";

import { LearningResourcesTable } from "@/features/learning-resources/components/learning-resources-table";

export default function LearningResourcesPage() {
  return (
    <>
      <Helmet>
        <title>Learning Resources - School Management</title>
        <meta name="description" content="Manage educational content, videos, documents, and learning materials" />
      </Helmet>
      
      <div className="container mx-auto py-6 space-y-8">
        <LearningResourcesTable />
      </div>
    </>
  );
} 