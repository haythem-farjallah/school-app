import { useTranslation } from "react-i18next";

/** A quiet dash for a missing value, announced as "Not provided". */
export function NotProvided() {
  const { t } = useTranslation();

  return (
    <>
      <span aria-hidden="true" className="text-muted-foreground">
        —
      </span>
      <span className="sr-only">{t("students.detail.notProvided")}</span>
    </>
  );
}
