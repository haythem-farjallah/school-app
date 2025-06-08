import { useTranslation } from 'react-i18next';

const StudentDashboard = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('Student Dashboard')}</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-2">{t('My Courses')}</h2>
          <p className="text-3xl font-bold">6</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-2">{t('Assignments Due')}</h2>
          <p className="text-3xl font-bold">3</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-2">{t('Average Grade')}</h2>
          <p className="text-3xl font-bold">85%</p>
        </div>
      </div>
    </div>
  );
}; 

export default StudentDashboard; 