import { useTranslation } from "react-i18next";

const ParentSchedule = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('Schedule')}</h1>
      <div className="bg-white p-6 rounded-lg shadow">
        <p className="text-gray-600">Children's schedules are not available yet.</p>
      </div>
    </div>
  );
};

export default ParentSchedule;
