import { useTranslation } from 'react-i18next';

const StudentSchedule = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('My Schedule')}</h1>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">{t('Your schedule will be displayed here')}</p>
      </div>
    </div>
  );
};

export default StudentSchedule; 