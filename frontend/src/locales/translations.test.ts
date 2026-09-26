import { describe, expect, it } from "vitest";
import { menuConfig } from "@/config/menuConfig";
import { supportedLocales } from "@/services/i18n";
import { localeBundles, translationKeys } from "@/test/i18n";

const en = localeBundles.en;
const otherLocales = supportedLocales.filter((locale) => locale !== "en");

/** Leaf values by key, e.g. { "shell.search.label": "Search pages" }. */
function leaves(bundle: Record<string, unknown>, prefix = ""): Record<string, string> {
  return Object.fromEntries(
    Object.entries(bundle).flatMap(([key, value]) =>
      value !== null && typeof value === "object"
        ? Object.entries(leaves(value as Record<string, unknown>, `${prefix}${key}.`))
        : [[`${prefix}${key}`, String(value)]],
    ),
  );
}

const variables = (text: string) => (text.match(/\{\{\s*\w+\s*\}\}/g) ?? []).sort();

// Values that are intentionally the same in every language.
const untranslated = new Set(["shell.appName", "shell.search.keys.enter", "shell.search.keys.escape"]);

describe("translation bundles", () => {
  it("has a bundle for every supported locale and no unsupported ones", () => {
    expect(Object.keys(localeBundles).sort()).toEqual([...supportedLocales].sort());
  });

  it.each(otherLocales)("%s defines exactly the keys English defines", (locale) => {
    expect(translationKeys(localeBundles[locale]).sort()).toEqual(translationKeys(en).sort());
  });

  it.each(otherLocales)("%s keeps every interpolation variable of the English text", (locale) => {
    const translated = leaves(localeBundles[locale]);
    for (const [key, english] of Object.entries(leaves(en))) {
      expect({ key, variables: variables(translated[key]) }).toEqual({ key, variables: variables(english) });
    }
  });

  it("every menu label and section title is a key with text in every locale", () => {
    const keys = Object.values(menuConfig).flatMap((sections) =>
      sections.flatMap((section) => [section.title, ...section.items.map((item) => item.label)]),
    );
    for (const locale of supportedLocales) {
      const defined = new Set(translationKeys(localeBundles[locale]));
      expect({ locale, missing: keys.filter((key) => !defined.has(key)) }).toEqual({ locale, missing: [] });
    }
  });

  it.each([
    ["ar", /[؀-ۿ]/],
    ["zh-TW", /[一-鿿]/],
  ])("%s translates every value instead of leaving English", (locale, script) => {
    const notTranslated = Object.entries(leaves(localeBundles[locale]))
      .filter(([key, value]) => !untranslated.has(key) && !script.test(value))
      .map(([key]) => key);
    expect(notTranslated).toEqual([]);
  });

  it("zh-TW uses Traditional, not Simplified, characters", () => {
    // Simplified forms of characters this UI uses; each has a different Traditional form.
    const simplified = /[学师课设权习错载时级绩务选单页开关户号资员长会谈动览侧边栏导结统讯联络请内显发误预应这为们个]/;
    const offending = Object.entries(leaves(localeBundles["zh-TW"])).filter(([, value]) => simplified.test(value));
    expect(offending).toEqual([]);
  });
});
