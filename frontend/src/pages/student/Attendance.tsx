import React from "react";
import StudentAttendanceView from "@/components/student/StudentAttendanceView";

const StudentAttendance = () => {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                My Attendance
              </h1>
              <p className="text-gray-600 mt-1">
                Track your attendance and view detailed records
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Content Area */}
      <div className="max-w-7xl mx-auto px-6 py-4">
        <StudentAttendanceView />
      </div>
    </div>
  );
};

export default StudentAttendance;
