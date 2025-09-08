import React from "react";
import { useTranslation } from "react-i18next";

const TeacherTeachingAssignments = () => {
  const { t } = useTranslation();

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">{t('My Teaching Assignments')}</h1>
      <div className="bg-white p-6 rounded-lg shadow">
        <p className="text-gray-600">Teaching assignments management coming soon...</p>
      </div>
    </div>
  );
};

export default TeacherTeachingAssignments;
