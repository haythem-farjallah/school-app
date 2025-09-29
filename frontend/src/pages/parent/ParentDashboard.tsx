import { useState } from "react";
import { AlertTriangle } from "lucide-react";
// TypeScript imports fixed
import { useQuery } from "@tanstack/react-query";

// Hooks and utilities
import { useAuth } from "@/hooks/useAuth";
import { http } from "@/lib/http";

// Types
import { ParentDashboardData, ChildInfo } from "@/types/parent-dashboard";

// Components
import ParentChildrenInfo from "@/components/parent/ParentChildrenInfo";

const ParentDashboard = () => {
  const { user } = useAuth();
  const [selectedChild, setSelectedChild] = useState<number | null>(null);

  // Fetch parent dashboard data (includes children info)
  const { data: dashboardData, isLoading: isChildrenLoading, error } = useQuery<ParentDashboardData | null>({
    queryKey: ['parent-dashboard', user?.id],
    queryFn: async (): Promise<ParentDashboardData | null> => {
      if (!user?.id) return null;
      const response = await http.get(`/v1/dashboard/parent/${user?.id}`);
      return response.data || null;
    },
    enabled: !!user?.id,
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: 3
  });


  if (isChildrenLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen">
        <AlertTriangle className="h-12 w-12 text-red-500 mb-4" />
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Unable to load dashboard</h2>
        <p className="text-gray-600 mb-4">Please try refreshing the page or contact support if the issue persists.</p>
        <button 
          onClick={() => window.location.reload()} 
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Refresh Page
        </button>
      </div>
    );
  }

  const children: ChildInfo[] = dashboardData?.children || [];


  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                Parent Dashboard
              </h1>
              <p className="text-gray-600 mt-1">
                Welcome back, {user?.firstName}! Monitor your {children.length} child{children.length !== 1 ? 'ren' : ''}'s progress
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Content Area */}
      <div className="max-w-7xl mx-auto px-6 py-4">
        <ParentChildrenInfo
          children={children}
          onChildSelect={setSelectedChild}
        />
      </div>
    </div>
  );
};

export default ParentDashboard;
