import { useTranslation } from "react-i18next";

export default function CommunicationDashboardPage() {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('Communication Dashboard')}</h1>
      <div className="bg-white p-6 rounded-lg shadow">
        <p className="text-gray-600">Communication analytics are not available yet.</p>
      </div>
    </div>
  );
}
