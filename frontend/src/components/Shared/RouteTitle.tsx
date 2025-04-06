// src/components/RouteTitle.tsx
import { Helmet } from "react-helmet-async";
import { useMatches } from "react-router-dom";
import { TitleHandle } from "../../types/react-router-meta";

export const RouteTitle = () => {
  const matches = useMatches().reverse(); // deepest route first

  /** Narrow the match so TypeScript knows handle.title exists */
  const matchWithTitle = matches.find(
    (m): m is typeof m & { handle: TitleHandle } =>
      Boolean((m.handle as TitleHandle | undefined)?.title),
  );

  if (!matchWithTitle) return null;

  const { title } = matchWithTitle.handle;

  const resolvedTitle: string =
    typeof title === "function"
      ? title({
          params: matchWithTitle.params as Record<string, string>,
          pathname: matchWithTitle.pathname,
        })
      : (title as string);

  return (
    <Helmet>
      <title>{resolvedTitle}</title>
    </Helmet>
  );
};
