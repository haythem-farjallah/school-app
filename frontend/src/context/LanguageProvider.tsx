import React, {
    useCallback,
    useEffect,
    useMemo,
    useState,
  } from "react";
  import i18n, { isSupportedLocale, supportedLocales, SupportedLocale } from "../services/i18n";
  import { LanguageContext } from "./LanguageContext";
  
  export const LanguageProvider: React.FC<React.PropsWithChildren> = ({
    children,
  }) => {
    // A stored language that is no longer supported falls back to the detected one.
    const stored = localStorage.getItem("lang");
    const initial: SupportedLocale = isSupportedLocale(stored)
      ? stored
      : isSupportedLocale(i18n.resolvedLanguage)
        ? i18n.resolvedLanguage
        : "en";
  
    const [lang, setLangState] = useState<SupportedLocale>(initial);
  
    const changeLanguage = useCallback((lng: SupportedLocale) => {
      i18n.changeLanguage(lng);
      setLangState(lng);
      localStorage.setItem("lang", lng);
    }, []);
  
    useEffect(() => {
      if (i18n.language !== lang) i18n.changeLanguage(lang);
    }, [lang]);

    // Keep the document language and direction in step with the UI language.
    useEffect(() => {
      document.documentElement.lang = lang;
      document.documentElement.dir = i18n.dir(lang);
    }, [lang]);
  
    const value = useMemo(
      () => ({ lang, setLang: changeLanguage, available: supportedLocales }),
      [lang, changeLanguage],
    );
  
    return (
      <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
    );
  };
  