import React from "react";
import { useTranslation } from "react-i18next";

const ParentContact = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('Contact Teachers')}</h1>
      <div className="bg-white p-6 rounded-lg shadow">
        <p className="text-gray-600">Teacher contact system coming soon...</p>
      </div>
    </div>
  );
};

export default ParentContact;
