import { isRouteErrorResponse, useRouteError, Link } from "react-router-dom";
import { Home } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { ErrorState } from "./ErrorState";

const ErrorRouteElement = () => {
  const err = useRouteError();
  const { t } = useTranslation();

  const status = isRouteErrorResponse(err) ? err.status : undefined;
  const notFound = status === 404;
  const details = isRouteErrorResponse(err) ? `${err.status} ${err.statusText}`.trim() : undefined;

  return (
    <main className="flex min-h-svh items-center justify-center bg-background px-4">
      <ErrorState
        title={t(notFound ? "common.states.notFound.title" : "common.states.error.title")}
        description={
          <>
            {t(notFound ? "common.states.notFound.description" : "common.states.error.unexpected")}
            {details && <span className="mt-1 block text-xs">{t("common.states.error.details", { details })}</span>}
          </>
        }
        action={
          <Button asChild>
            <Link to="/">
              <Home aria-hidden="true" />
              {t("common.actions.goHome")}
            </Link>
          </Button>
        }
      />
    </main>
  );
};

export default ErrorRouteElement;
