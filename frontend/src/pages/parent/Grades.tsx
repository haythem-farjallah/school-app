import { useTranslation } from 'react-i18next';

const ParentGrades = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('Children Grades')}</h1>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">{t('Your children grades will be displayed here')}</p>
      </div>
    </div>
  );
};

export default ParentGrades; 