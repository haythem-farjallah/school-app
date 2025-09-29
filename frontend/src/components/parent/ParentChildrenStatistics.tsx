import React from 'react';
import { TrendingUp, TrendingDown, AlertTriangle, Award, Users } from 'lucide-react';

interface ChildStatistics {
  studentId: number;
  studentName: string;
  currentClass: string;
  classId: number;
  attendanceRate: number;
  totalAbsences: number;
  totalLateArrivals: number;
  totalDaysTracked: number;
  averageGrade: number;
  academicStanding: string;
  alerts: string[];
}

interface ParentChildrenStatisticsProps {
  data: {
    children: ChildStatistics[];
    overallStatistics: {
      totalChildren: number;
      averageAttendanceRate: number;
      totalAbsences: number;
      totalLateArrivals: number;
      childrenWithGoodAttendance: number;
      childrenNeedingAttention: number;
    };
  };
}

export const ParentChildrenStatistics: React.FC<ParentChildrenStatisticsProps> = ({ data }) => {
  const { children, overallStatistics } = data;

  const getAttendanceColor = (rate: number) => {
    if (rate >= 95) return 'text-green-600';
    if (rate >= 90) return 'text-blue-600';
    if (rate >= 80) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getAttendanceBadge = (rate: number) => {
    if (rate >= 95) return 'bg-green-100 text-green-800';
    if (rate >= 90) return 'bg-blue-100 text-blue-800';
    if (rate >= 80) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  };

  return (
    <div className="space-y-6">
      {/* Overall Statistics */}
      <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Overall Family Statistics</h3>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-600">{overallStatistics.totalChildren}</div>
            <div className="text-sm text-gray-600">Total Children</div>
          </div>
          <div className="text-center">
            <div className={`text-2xl font-bold ${getAttendanceColor(overallStatistics.averageAttendanceRate)}`}>
              {overallStatistics.averageAttendanceRate}%
            </div>
            <div className="text-sm text-gray-600">Avg Attendance</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-red-600">{overallStatistics.totalAbsences}</div>
            <div className="text-sm text-gray-600">Total Absences</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-yellow-600">{overallStatistics.totalLateArrivals}</div>
            <div className="text-sm text-gray-600">Late Arrivals</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-green-600">{overallStatistics.childrenWithGoodAttendance}</div>
            <div className="text-sm text-gray-600">Good Attendance</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-orange-600">{overallStatistics.childrenNeedingAttention}</div>
            <div className="text-sm text-gray-600">Need Attention</div>
          </div>
        </div>
      </div>

      {/* Individual Child Statistics */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {children.map((child) => (
          <div key={child.studentId} className="bg-white rounded-lg shadow-sm border p-6">
            <div className="flex items-center justify-between mb-4">
              <h4 className="text-lg font-semibold text-gray-900">{child.studentName}</h4>
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${getAttendanceBadge(child.attendanceRate)}`}>
                {child.academicStanding}
              </span>
            </div>

            <div className="space-y-4">
              {/* Class Information */}
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Current Class:</span>
                <span className="font-medium">{child.currentClass}</span>
              </div>

              {/* Attendance Statistics */}
              <div className="bg-gray-50 rounded-lg p-4">
                <h5 className="font-medium text-gray-900 mb-3">Attendance Overview</h5>
                <div className="grid grid-cols-2 gap-4">
                  <div className="text-center">
                    <div className={`text-xl font-bold ${getAttendanceColor(child.attendanceRate)}`}>
                      {child.attendanceRate}%
                    </div>
                    <div className="text-xs text-gray-600">Attendance Rate</div>
                  </div>
                  <div className="text-center">
                    <div className="text-xl font-bold text-gray-700">{child.totalDaysTracked}</div>
                    <div className="text-xs text-gray-600">Days Tracked</div>
                  </div>
                </div>
                
                <div className="grid grid-cols-2 gap-4 mt-3">
                  <div className="text-center">
                    <div className="text-lg font-semibold text-red-600">{child.totalAbsences}</div>
                    <div className="text-xs text-gray-600">Absences</div>
                  </div>
                  <div className="text-center">
                    <div className="text-lg font-semibold text-yellow-600">{child.totalLateArrivals}</div>
                    <div className="text-xs text-gray-600">Late Arrivals</div>
                  </div>
                </div>
              </div>

              {/* Academic Performance */}
              <div className="bg-blue-50 rounded-lg p-4">
                <h5 className="font-medium text-gray-900 mb-2">Academic Performance</h5>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-gray-600">Average Grade:</span>
                  <span className="text-lg font-bold text-blue-600">
                    {Math.round(child.averageGrade)}%
                  </span>
                </div>
              </div>

              {/* Alerts */}
              {child.alerts.length > 0 && (
                <div className="bg-red-50 rounded-lg p-4">
                  <h5 className="font-medium text-red-900 mb-2 flex items-center">
                    <AlertTriangle className="h-4 w-4 mr-2" />
                    Alerts ({child.alerts.length})
                  </h5>
                  <ul className="space-y-1">
                    {child.alerts.map((alert, index) => (
                      <li key={index} className="text-sm text-red-700 flex items-start">
                        <span className="w-1 h-1 bg-red-500 rounded-full mt-2 mr-2 flex-shrink-0"></span>
                        {alert}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Performance Indicators */}
              <div className="flex justify-between items-center pt-2 border-t">
                <div className="flex items-center space-x-4">
                  {child.attendanceRate >= 95 && (
                    <div className="flex items-center text-green-600">
                      <Award className="h-4 w-4 mr-1" />
                      <span className="text-xs">Excellent</span>
                    </div>
                  )}
                  {child.attendanceRate >= 90 && child.attendanceRate < 95 && (
                    <div className="flex items-center text-blue-600">
                      <TrendingUp className="h-4 w-4 mr-1" />
                      <span className="text-xs">Good</span>
                    </div>
                  )}
                  {child.attendanceRate < 80 && (
                    <div className="flex items-center text-red-600">
                      <TrendingDown className="h-4 w-4 mr-1" />
                      <span className="text-xs">Needs Attention</span>
                    </div>
                  )}
                </div>
                
                <button className="text-blue-600 hover:text-blue-700 text-sm font-medium">
                  View Details →
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Performance Summary */}
      <div className="bg-white rounded-lg shadow-sm border p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Performance Summary</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center p-4 bg-green-50 rounded-lg">
            <Award className="h-8 w-8 text-green-600 mx-auto mb-2" />
            <div className="text-2xl font-bold text-green-600">{overallStatistics.childrenWithGoodAttendance}</div>
            <div className="text-sm text-green-700">Children with Good Attendance</div>
            <div className="text-xs text-gray-600 mt-1">(≥90% attendance rate)</div>
          </div>
          
          <div className="text-center p-4 bg-yellow-50 rounded-lg">
            <AlertTriangle className="h-8 w-8 text-yellow-600 mx-auto mb-2" />
            <div className="text-2xl font-bold text-yellow-600">{overallStatistics.childrenNeedingAttention}</div>
            <div className="text-sm text-yellow-700">Children Needing Attention</div>
            <div className="text-xs text-gray-600 mt-1">(&lt;80% attendance rate)</div>
          </div>
          
          <div className="text-center p-4 bg-blue-50 rounded-lg">
            <Users className="h-8 w-8 text-blue-600 mx-auto mb-2" />
            <div className="text-2xl font-bold text-blue-600">{overallStatistics.averageAttendanceRate}%</div>
            <div className="text-sm text-blue-700">Family Average</div>
            <div className="text-xs text-gray-600 mt-1">Overall attendance rate</div>
          </div>
        </div>
      </div>
    </div>
  );
};
