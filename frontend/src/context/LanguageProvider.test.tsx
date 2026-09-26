import { afterEach, describe, expect, it } from "vitest";
import { render, waitFor } from "@testing-library/react";
import { LanguageProvider } from "./LanguageProvider";

describe("LanguageProvider", () => {
  afterEach(() => {
    document.documentElement.removeAttribute("lang");
    document.documentElement.removeAttribute("dir");
  });

  it.each([
    ["en", "ltr"],
    ["fr", "ltr"],
    ["zh-TW", "ltr"],
    ["ar", "rtl"],
  ])("sets <html lang=%s dir=%s> for the stored language", async (lang, dir) => {
    localStorage.setItem("lang", lang);
    render(<LanguageProvider>content</LanguageProvider>);

    await waitFor(() => expect(document.documentElement.lang).toBe(lang));
    expect(document.documentElement.dir).toBe(dir);
  });

  it("ignores a stored language that is not supported", async () => {
    localStorage.setItem("lang", "de");
    render(<LanguageProvider>content</LanguageProvider>);

    await waitFor(() => expect(document.documentElement.lang).not.toBe(""));
    expect(document.documentElement.lang).not.toBe("de");
    expect(document.documentElement.dir).toBe("ltr");
  });
});
