import { useTranslation } from 'react-i18next';

const AdminDashboard = () => {
  const { t } = useTranslation();

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">{t('Admin Dashboard')}</h1>
        <p className="text-gray-600">Welcome back! Here's an overview of your school management system.</p>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-gradient-to-br from-blue-50 to-blue-100 p-6 rounded-xl border border-blue-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-lg">👥</span>
            </div>
            <span className="text-xs text-blue-600 font-semibold uppercase tracking-wide">2024/25</span>
          </div>
          <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Students')}</h2>
          <p className="text-3xl font-bold text-blue-600">1,218</p>
          <p className="text-sm text-blue-500 mt-2">+12 new this month</p>
        </div>

        <div className="bg-gradient-to-br from-green-50 to-green-100 p-6 rounded-xl border border-green-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 bg-gradient-to-br from-green-500 to-green-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-lg">🎓</span>
            </div>
            <span className="text-xs text-green-600 font-semibold uppercase tracking-wide">2024/25</span>
          </div>
          <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Teachers')}</h2>
          <p className="text-3xl font-bold text-green-600">124</p>
          <p className="text-sm text-green-500 mt-2">Active faculty</p>
        </div>

        <div className="bg-gradient-to-br from-purple-50 to-purple-100 p-6 rounded-xl border border-purple-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-lg">👨‍👩‍👧‍👦</span>
            </div>
            <span className="text-xs text-purple-600 font-semibold uppercase tracking-wide">2024/25</span>
          </div>
          <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Parents')}</h2>
          <p className="text-3xl font-bold text-purple-600">960</p>
          <p className="text-sm text-purple-500 mt-2">Engaged families</p>
        </div>

        <div className="bg-gradient-to-br from-orange-50 to-orange-100 p-6 rounded-xl border border-orange-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 bg-gradient-to-br from-orange-500 to-orange-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-lg">👤</span>
            </div>
            <span className="text-xs text-orange-600 font-semibold uppercase tracking-wide">2024/25</span>
          </div>
          <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Staff')}</h2>
          <p className="text-3xl font-bold text-orange-600">30</p>
          <p className="text-sm text-orange-500 mt-2">Support team</p>
        </div>
      </div>

      {/* Additional sections can be added here */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
          <h3 className="text-xl font-semibold text-gray-900 mb-4">Recent Activity</h3>
          <div className="space-y-3">
            <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
              <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
              <span className="text-gray-700">New student registration: Sarah Johnson</span>
            </div>
            <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
              <div className="w-2 h-2 bg-green-500 rounded-full"></div>
              <span className="text-gray-700">Teacher joined: Prof. Michael Chen</span>
            </div>
            <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
              <div className="w-2 h-2 bg-purple-500 rounded-full"></div>
              <span className="text-gray-700">Class schedule updated</span>
            </div>
          </div>
        </div>

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
          </div>
        </div>
      </div>
    </div>
  );
}; 

export default AdminDashboard; 