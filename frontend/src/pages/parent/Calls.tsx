import React from "react";
import { useTranslation } from "react-i18next";

const ParentCalls = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('Parent-Teacher Calls')}</h1>
      <div className="bg-white p-6 rounded-lg shadow">
        <p className="text-gray-600">Call scheduling coming soon...</p>
      </div>
    </div>
  );
};

export default ParentCalls;
