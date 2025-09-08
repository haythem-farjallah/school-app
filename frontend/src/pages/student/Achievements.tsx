import React from "react";
import { useTranslation } from "react-i18next";

const StudentAchievements = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('My Achievements')}</h1>
      <div className="bg-white p-6 rounded-lg shadow">
        <p className="text-gray-600">Achievements system coming soon...</p>
      </div>
    </div>
  );
};

export default StudentAchievements;
