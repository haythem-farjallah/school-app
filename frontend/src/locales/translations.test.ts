import { describe, expect, it } from "vitest";
import { menuConfig } from "@/config/menuConfig";
import { supportedLocales } from "@/services/i18n";
import { localeBundles, translationKeys } from "@/test/i18n";

const en = localeBundles.en;
const fr = localeBundles.fr;

describe("translation bundles", () => {
  it("has a bundle for every supported locale and no unsupported ones", () => {
    expect(Object.keys(localeBundles).sort()).toEqual([...supportedLocales].sort());
  });

  it("French defines exactly the keys English defines", () => {
    expect(translationKeys(fr).sort()).toEqual(translationKeys(en).sort());
  });

  it("every menu label and section title is a key with English and French text", () => {
    const keys = Object.values(menuConfig).flatMap((sections) =>
      sections.flatMap((section) => [section.title, ...section.items.map((item) => item.label)]),
    );
    const enKeys = new Set(translationKeys(en));
    const frKeys = new Set(translationKeys(fr));

    expect(keys.filter((key) => !enKeys.has(key))).toEqual([]);
    expect(keys.filter((key) => !frKeys.has(key))).toEqual([]);
  });
});
