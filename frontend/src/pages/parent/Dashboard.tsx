import { useTranslation } from 'react-i18next';

const ParentDashboard = () => {
  const { t } = useTranslation();

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">{t('Parent Dashboard')}</h1>
        <p className="text-gray-600">Stay connected with your child's educational journey.</p>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
        <div className="bg-gradient-to-br from-blue-50 to-blue-100 p-6 rounded-xl border border-blue-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-lg">👶</span>
            </div>
            <span className="text-xs text-blue-600 font-semibold uppercase tracking-wide">Family</span>
          </div>
          <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('My Children')}</h2>
          <p className="text-3xl font-bold text-blue-600">2</p>
          <p className="text-sm text-blue-500 mt-2">Active students</p>
        </div>

        <div className="bg-gradient-to-br from-green-50 to-green-100 p-6 rounded-xl border border-green-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 bg-gradient-to-br from-green-500 to-green-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-lg">📅</span>
            </div>
            <span className="text-xs text-green-600 font-semibold uppercase tracking-wide">This Week</span>
          </div>
          <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Upcoming Events')}</h2>
          <p className="text-3xl font-bold text-green-600">4</p>
          <p className="text-sm text-green-500 mt-2">School activities</p>
        </div>

        <div className="bg-gradient-to-br from-purple-50 to-purple-100 p-6 rounded-xl border border-purple-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center justify-between mb-4">
            <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-lg">💬</span>
            </div>
            <span className="text-xs text-purple-600 font-semibold uppercase tracking-wide">Unread</span>
          </div>
          <h2 className="text-lg font-semibold mb-1 text-gray-800">{t('Messages')}</h2>
          <p className="text-3xl font-bold text-purple-600">3</p>
          <p className="text-sm text-purple-500 mt-2">From teachers</p>
        </div>
      </div>

      {/* Children Progress Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
          <h3 className="text-xl font-semibold text-gray-900 mb-4">Children's Progress</h3>
          <div className="space-y-4">
            <div className="p-4 rounded-lg bg-gradient-to-r from-blue-50 to-blue-100 border border-blue-200">
              <div className="flex items-center justify-between mb-2">
                <span className="font-medium text-gray-800">Emma Johnson</span>
                <span className="text-sm text-blue-600 font-semibold">Grade 8</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="flex-1 bg-blue-200 rounded-full h-2">
                  <div className="bg-blue-600 h-2 rounded-full" style={{ width: '85%' }}></div>
                </div>
                <span className="text-sm font-medium text-blue-600">85%</span>
              </div>
              <p className="text-xs text-blue-500 mt-1">Overall performance</p>
            </div>
            
            <div className="p-4 rounded-lg bg-gradient-to-r from-green-50 to-green-100 border border-green-200">
              <div className="flex items-center justify-between mb-2">
                <span className="font-medium text-gray-800">Alex Johnson</span>
                <span className="text-sm text-green-600 font-semibold">Grade 5</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="flex-1 bg-green-200 rounded-full h-2">
                  <div className="bg-green-600 h-2 rounded-full" style={{ width: '92%' }}></div>
                </div>
                <span className="text-sm font-medium text-green-600">92%</span>
              </div>
              <p className="text-xs text-green-500 mt-1">Overall performance</p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
          <h3 className="text-xl font-semibold text-gray-900 mb-4">Recent Updates</h3>
          <div className="space-y-3">
            <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
              <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
              <span className="text-gray-700">Emma's Math test score: 95%</span>
            </div>
            <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
              <div className="w-2 h-2 bg-green-500 rounded-full"></div>
              <span className="text-gray-700">Alex completed Science project</span>
            </div>
            <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
              <div className="w-2 h-2 bg-purple-500 rounded-full"></div>
              <span className="text-gray-700">Parent-teacher meeting scheduled</span>
            </div>
            <div className="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
              <div className="w-2 h-2 bg-orange-500 rounded-full"></div>
              <span className="text-gray-700">Field trip permission slip due</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ParentDashboard; 