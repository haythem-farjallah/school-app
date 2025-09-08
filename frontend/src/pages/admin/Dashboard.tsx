import { useTranslation } from 'react-i18next';
import { AdminFeed } from '@/components/Admin/AdminFeed';
import { WebSocketTestPanel } from '@/components/Admin/WebSocketTestPanel';
import { RecentActivity } from '@/components/Dashboard/RecentActivity';
import { useDashboardStats } from '@/hooks/use-dashboard-stats';
import { Loader2, TrendingUp, TrendingDown } from 'lucide-react';

const AdminDashboard = () => {
  const { t } = useTranslation();
  const { data: stats, isLoading, error } = useDashboardStats();

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">{t('Admin Dashboard')}</h1>
        <p className="text-gray-600">Welcome back! Here's an overview of your school management system.</p>
      </div>
      
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
          <span className="ml-2 text-gray-600">Loading dashboard statistics...</span>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-8">
          <p className="text-red-600">Failed to load dashboard statistics. Please try again later.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-8">
          <div className="bg-gradient-to-br from-blue-50 to-blue-100 p-6 rounded-xl border border-blue-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-lg">👥</span>
              </div>
              <span className="text-xs text-blue-600 font-semibold uppercase tracking-wide">2024/25</span>
            </div>
            <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Students')}</h2>
            <p className="text-3xl font-bold text-blue-600">{stats?.totalStudents?.toLocaleString() || '0'}</p>
            <div className="flex items-center mt-2">
              <TrendingUp className="h-3 w-3 text-green-500 mr-1" />
              <p className="text-sm text-blue-500">Active students</p>
            </div>
          </div>

          <div className="bg-gradient-to-br from-green-50 to-green-100 p-6 rounded-xl border border-green-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-green-500 to-green-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-lg">🎓</span>
              </div>
              <span className="text-xs text-green-600 font-semibold uppercase tracking-wide">2024/25</span>
            </div>
            <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Teachers')}</h2>
            <p className="text-3xl font-bold text-green-600">{stats?.totalTeachers?.toLocaleString() || '0'}</p>
            <div className="flex items-center mt-2">
              <TrendingUp className="h-3 w-3 text-green-500 mr-1" />
              <p className="text-sm text-green-500">Active faculty</p>
            </div>
          </div>

          <div className="bg-gradient-to-br from-purple-50 to-purple-100 p-6 rounded-xl border border-purple-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-lg">👨‍👩‍👧‍👦</span>
              </div>
              <span className="text-xs text-purple-600 font-semibold uppercase tracking-wide">2024/25</span>
            </div>
            <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Parents')}</h2>
            <p className="text-3xl font-bold text-purple-600">{stats?.totalParents?.toLocaleString() || '0'}</p>
            <div className="flex items-center mt-2">
              <TrendingUp className="h-3 w-3 text-purple-500 mr-1" />
              <p className="text-sm text-purple-500">Engaged families</p>
            </div>
          </div>

          <div className="bg-gradient-to-br from-orange-50 to-orange-100 p-6 rounded-xl border border-orange-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-orange-500 to-orange-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-lg">📚</span>
              </div>
              <span className="text-xs text-orange-600 font-semibold uppercase tracking-wide">2024/25</span>
            </div>
            <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Classes')}</h2>
            <p className="text-3xl font-bold text-orange-600">{stats?.totalClasses?.toLocaleString() || '0'}</p>
            <div className="flex items-center mt-2">
              <TrendingUp className="h-3 w-3 text-orange-500 mr-1" />
              <p className="text-sm text-orange-500">Active classes</p>
            </div>
          </div>

          <div className="bg-gradient-to-br from-indigo-50 to-indigo-100 p-6 rounded-xl border border-indigo-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
            <div className="flex items-center justify-between mb-4">
              <div className="w-12 h-12 bg-gradient-to-br from-indigo-500 to-indigo-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-lg">📝</span>
              </div>
              <span className="text-xs text-indigo-600 font-semibold uppercase tracking-wide">2024/25</span>
            </div>
            <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Enrollments')}</h2>
            <p className="text-3xl font-bold text-indigo-600">{stats?.activeEnrollments?.toLocaleString() || '0'}</p>
            <div className="flex items-center mt-2">
              <TrendingUp className="h-3 w-3 text-indigo-500 mr-1" />
              <p className="text-sm text-indigo-500">Active enrollments</p>
            </div>
          </div>
        </div>
      )}

      {/* Real-time Admin Feed */}
      <div className="mb-6">
        <AdminFeed />
      </div>

      {/* Additional sections */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RecentActivity />

        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
          <h3 className="text-xl font-semibold text-gray-900 mb-4">Quick Actions</h3>
          <div className="grid grid-cols-2 gap-3">
            <button className="p-3 rounded-lg border border-gray-200 hover:border-blue-300 hover:bg-blue-50 transition-all duration-200 text-left">
              <span className="text-sm font-medium text-gray-700">Add Student</span>
            </button>
            <button className="p-3 rounded-lg border border-gray-200 hover:border-green-300 hover:bg-green-50 transition-all duration-200 text-left">
              <span className="text-sm font-medium text-gray-700">Add Teacher</span>
            </button>
            <button className="p-3 rounded-lg border border-gray-200 hover:border-purple-300 hover:bg-purple-50 transition-all duration-200 text-left">
              <span className="text-sm font-medium text-gray-700">Create Class</span>
            </button>
            <button className="p-3 rounded-lg border border-gray-200 hover:border-orange-300 hover:bg-orange-50 transition-all duration-200 text-left">
              <span className="text-sm font-medium text-gray-700">Send Notice</span>
            </button>
            <a href="/admin/timetable" className="p-3 rounded-lg border border-gray-200 hover:border-indigo-300 hover:bg-indigo-50 transition-all duration-200 text-left block">
              <span className="text-sm font-medium text-gray-700">Manage Timetables</span>
            </a>
          </div>
        </div>
      </div>
      
      {/* Development WebSocket Test Panel */}
      <WebSocketTestPanel />
    </div>
  );
}; 

export default AdminDashboard; 