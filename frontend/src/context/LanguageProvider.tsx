import React, {
    useCallback,
    useEffect,
    useMemo,
    useState,
  } from "react";
  import i18n, { supportedLocales, SupportedLocale } from "../services/i18n";
  import { LanguageContext } from "./LanguageContext";
  
  export const LanguageProvider: React.FC<React.PropsWithChildren> = ({
    children,
  }) => {
    const initial =
      (localStorage.getItem("lang") as SupportedLocale | null) ??
      (i18n.resolvedLanguage as SupportedLocale) ??
      "en";
  
    const [lang, setLangState] = useState<SupportedLocale>(initial);
  
    const changeLanguage = useCallback((lng: SupportedLocale) => {
      i18n.changeLanguage(lng);
      setLangState(lng);
      localStorage.setItem("lang", lng);
    }, []);
  
    useEffect(() => {
      if (i18n.language !== lang) i18n.changeLanguage(lang);
    }, [lang]);
  
    const value = useMemo(
      () => ({ lang, setLang: changeLanguage, available: supportedLocales }),
      [lang, changeLanguage],
    );
  
    return (
      <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
    );
  };
  