import { useTranslation } from 'react-i18next';

const TeacherDashboard = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('Teacher Dashboard')}</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-2">{t('My Classes')}</h2>
          <p className="text-3xl font-bold">5</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-2">{t('Total Students')}</h2>
          <p className="text-3xl font-bold">150</p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-lg font-semibold mb-2">{t('Upcoming Classes')}</h2>
          <p className="text-3xl font-bold">3</p>
        </div>
      </div>
    </div>
  );
}; 

export default TeacherDashboard; 