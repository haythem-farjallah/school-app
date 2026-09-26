import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { I18nextProvider } from "react-i18next";
import type { ReactNode } from "react";
import { PageContainer } from "@/components/Layout/PageContainer";
import { PageHeader } from "@/components/Layout/PageHeader";
import { PageSection } from "@/components/Layout/PageSection";
import { EmptyState } from "./EmptyState";
import { ErrorState } from "./ErrorState";
import { LoadingState } from "./LoadingState";
import { UnavailableState } from "./UnavailableState";
import { createTestI18n } from "@/test/i18n";
import type { SupportedLocale } from "@/services/i18n";

const rawKey = /\b(common|shell|navigation|roles)\.[a-zA-Z]/;

function renderIn(lng: SupportedLocale, ui: ReactNode) {
  return render(<I18nextProvider i18n={createTestI18n(lng)}>{ui}</I18nextProvider>);
}

describe("page layout primitives", () => {
  it("PageHeader renders the title as the page h1, with description and actions", () => {
    renderIn(
      "en",
      <PageContainer>
        <PageHeader title="Students" description="Everyone enrolled this year" actions={<button>Add student</button>} />
      </PageContainer>,
    );

    expect(screen.getByRole("banner")).toContainElement(screen.getByRole("heading", { level: 1, name: "Students" }));
    expect(screen.getByText("Everyone enrolled this year")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Add student" })).toBeInTheDocument();
  });

  it("PageSection is a region named by its h2 and renders its content without a card", () => {
    renderIn("en", <PageSection title="Recent grades"><p>Section body</p></PageSection>);

    const region = screen.getByRole("region", { name: "Recent grades" });
    expect(screen.getByRole("heading", { level: 2, name: "Recent grades" })).toBeInTheDocument();
    expect(region).toHaveTextContent("Section body");
  });
});

describe("page states", () => {
  it("LoadingState announces itself as a polite status with translated default copy", () => {
    renderIn("en", <LoadingState />);

    const status = screen.getByRole("status");
    expect(status).toHaveAttribute("aria-live", "polite");
    expect(status).toHaveTextContent("Loading…");
  });

  it("EmptyState shows default copy and a custom action", async () => {
    const onCreate = vi.fn();
    renderIn("en", <EmptyState action={<button onClick={onCreate}>Create class</button>} />);

    expect(screen.getByText("Nothing here yet")).toBeInTheDocument();
    await userEvent.setup().click(screen.getByRole("button", { name: "Create class" }));
    expect(onCreate).toHaveBeenCalledOnce();
  });

  it("ErrorState is an alert whose retry button calls onRetry", async () => {
    const onRetry = vi.fn();
    renderIn("en", <ErrorState onRetry={onRetry} />);

    expect(screen.getByRole("alert")).toHaveTextContent("Something went wrong");
    await userEvent.setup().click(screen.getByRole("button", { name: "Retry" }));
    expect(onRetry).toHaveBeenCalledOnce();
  });

  it("ErrorState offers no retry button without onRetry", () => {
    renderIn("en", <ErrorState />);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("UnavailableState is a named region that says the feature is unavailable", () => {
    renderIn("en", <UnavailableState />);

    const region = screen.getByRole("region", { name: "Not available yet" });
    expect(region).toHaveTextContent("This feature is not available in the application yet.");
  });

  it("uses caller-provided copy instead of the defaults", () => {
    renderIn("en", <UnavailableState title="Grades are not available" description="Grade entry is disabled." />);

    expect(screen.getByRole("region", { name: "Grades are not available" })).toHaveTextContent("Grade entry is disabled.");
  });

  it("shows French default copy with no raw translation keys", () => {
    const { container } = renderIn(
      "fr",
      <>
        <LoadingState />
        <EmptyState />
        <ErrorState onRetry={() => {}} />
        <UnavailableState />
      </>,
    );

    expect(screen.getByRole("status")).toHaveTextContent("Chargement…");
    expect(screen.getByText("Rien pour le moment")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("Une erreur est survenue");
    expect(screen.getByRole("button", { name: "Réessayer" })).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "Pas encore disponible" })).toBeInTheDocument();
    expect(container.textContent).not.toMatch(rawKey);
  });
  it.each([
    ["zh-TW", "載入中…", "目前沒有內容", "重試", "尚未開放"],
    ["ar", "جارٍ التحميل…", "لا يوجد شيء بعد", "إعادة المحاولة", "غير متاح بعد"],
  ] as const)("shows %s default copy with no raw translation keys", (lng, loading, empty, retry, unavailable) => {
    const { container } = renderIn(
      lng,
      <>
        <LoadingState />
        <EmptyState />
        <ErrorState onRetry={() => {}} />
        <UnavailableState />
      </>,
    );

    expect(screen.getByRole("status")).toHaveTextContent(loading);
    expect(screen.getByText(empty)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: retry })).toBeInTheDocument();
    expect(screen.getByRole("region", { name: unavailable })).toBeInTheDocument();
    expect(container.textContent).not.toMatch(rawKey);
  });
});
