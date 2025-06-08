import { useTranslation } from 'react-i18next';

const Assignments = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('Assignments')}</h1>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-600">{t('Assignments will be displayed here')}</p>
      </div>
    </div>
  );
};

export default Assignments; 