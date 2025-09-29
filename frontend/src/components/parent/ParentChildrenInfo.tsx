import React, { useState } from "react";
import { User, TrendingUp, Calendar } from "lucide-react";
import { ChildInfo } from "@/types/parent-dashboard";
import ChildDetailsModal from "./ChildDetailsModal";

interface ParentChildrenInfoProps {
  children: ChildInfo[];
  onChildSelect?: (childId: number) => void;
}

export const ParentChildrenInfo: React.FC<ParentChildrenInfoProps> = ({
  children,
  onChildSelect
}) => {
  const [selectedChild, setSelectedChild] = useState<ChildInfo | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleViewDetails = (child: ChildInfo) => {
    setSelectedChild(child);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedChild(null);
  };
  const getAttendanceColor = (rate: number) => {
    if (rate >= 90) return "text-green-600 bg-green-100";
    if (rate >= 80) return "text-yellow-600 bg-yellow-100";
    return "text-red-600 bg-red-100";
  };

  const getAcademicColor = (standing: string) => {
    switch (standing.toLowerCase()) {
      case 'excellent': return "text-green-700 bg-green-100";
      case 'good': return "text-blue-700 bg-blue-100";
      case 'satisfactory': return "text-yellow-700 bg-yellow-100";
      default: return "text-red-700 bg-red-100";
    }
  };

  if (!children || children.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-8 text-center">
        <User className="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <h3 className="text-lg font-medium text-gray-900 mb-2">No Children Found</h3>
        <p className="text-gray-600">No children are associated with your account.</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {children.map((child) => (
        <div
          key={child.studentId}
          className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow cursor-pointer"
          onClick={() => onChildSelect?.(child.studentId)}
        >
          {/* Child Header */}
          <div className="flex items-center space-x-3 mb-4">
            <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
              <User className="h-6 w-6 text-blue-600" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900">{child.name}</h3>
              <p className="text-sm text-gray-600">{child.currentClass}</p>
            </div>
          </div>

          {/* Stats Grid */}
          <div className="space-y-4">
            {/* Attendance Rate */}
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Calendar className="h-4 w-4 text-gray-500" />
                <span className="text-sm text-gray-700">Attendance</span>
              </div>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${getAttendanceColor(child.attendanceRate)}`}>
                {child.attendanceRate}%
              </span>
            </div>

            {/* Academic Standing */}
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <TrendingUp className="h-4 w-4 text-gray-500" />
                <span className="text-sm text-gray-700">Standing</span>
              </div>
              <span className={`px-2 py-1 rounded-full text-xs font-medium ${getAcademicColor(child.academicStanding)}`}>
                {child.academicStanding}
              </span>
            </div>


            {/* Total Absences */}
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-700">Total Absences</span>
              <span className={`text-sm font-medium ${
                child.totalAbsences > 5 ? 'text-red-600' :
                child.totalAbsences > 2 ? 'text-yellow-600' :
                'text-green-600'
              }`}>
                {child.totalAbsences} / {child.totalDaysTracked} days
              </span>
            </div>
          </div>

          {/* Action Button */}
          <div className="mt-6 pt-4 border-t border-gray-100">
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleViewDetails(child);
              }}
              className="w-full bg-blue-600 text-white px-4 py-2 rounded-md text-sm font-medium hover:bg-blue-700 transition-colors"
            >
              View Details
            </button>
          </div>
        </div>
      ))}
      
      {/* Modal */}
      {selectedChild && (
        <ChildDetailsModal
          child={selectedChild}
          isOpen={isModalOpen}
          onClose={handleCloseModal}
        />
      )}
    </div>
  );
};

export default ParentChildrenInfo;
