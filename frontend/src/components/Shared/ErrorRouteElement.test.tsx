import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { I18nextProvider } from "react-i18next";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import ErrorRouteElement from "./ErrorRouteElement";
import { createTestI18n } from "@/test/i18n";

function renderAt(path: string, lng: "en" | "fr" = "en") {
  const router = createMemoryRouter(
    [{ path: "/", element: <p>Home page</p>, errorElement: <ErrorRouteElement /> }],
    { initialEntries: [path] },
  );
  return render(
    <I18nextProvider i18n={createTestI18n(lng)}>
      <RouterProvider router={router} />
    </I18nextProvider>,
  );
}

describe("ErrorRouteElement", () => {
  it("explains a missing page, keeps the status details and links home", () => {
    renderAt("/no-such-page");

    const alert = screen.getByRole("alert");
    expect(alert).toHaveTextContent("Page not found");
    expect(alert).toHaveTextContent("Details: 404 Not Found");
    expect(screen.getByRole("link", { name: "Go to home page" })).toHaveAttribute("href", "/");
  });

  it("is translated to French", () => {
    renderAt("/no-such-page", "fr");

    expect(screen.getByRole("alert")).toHaveTextContent("Page introuvable");
    expect(screen.getByRole("link", { name: "Retour à l'accueil" })).toHaveAttribute("href", "/");
  });
});
