import { QueryClient } from "@tanstack/react-query";

/** The application's React Query client. Ending the session clears it. */
export const queryClient = new QueryClient();
