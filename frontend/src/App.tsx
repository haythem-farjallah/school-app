import {Suspense} from "react";
import {ErrorBoundary} from "react-error-boundary";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { Toaster } from "react-hot-toast";
import {QueryClient,QueryClientProvider} from "@tanstack/react-query";
import { Provider } from "react-redux";
import { FallbackErrorBoundary } from "./components/Shared/FallbackErrorBoundary";
import { store } from "./stores/store";
import { LanguageProvider } from "./context/LanguageProvider";
import { ThemeProvider } from "./context/ThemeProvider"
import { RoleThemeProvider } from "./components/providers/RoleThemeProvider";
import { HelmetProvider } from "react-helmet-async";
import { AppRoutes } from "./routes/AppRoutes";
import { NuqsAdapter } from "nuqs/adapters/react-router";
import { WebSocketBridge } from "./components/Realtime/WebSocketBridge";

const queryClient =new QueryClient();
function App() {

  return (
  <HelmetProvider>
    <ErrorBoundary FallbackComponent={FallbackErrorBoundary}>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
          <ThemeProvider>
            <RoleThemeProvider>
              <LanguageProvider>
                <NuqsAdapter>
                  <Suspense fallback={<div>Loading...</div>}>
                    <AppRoutes/>
                  </Suspense>
                  <WebSocketBridge />
                  <Toaster/>
                    {import.meta.env.DEV && (
                        <ReactQueryDevtools initialIsOpen={false} />
                      )}          
                </NuqsAdapter>
                </LanguageProvider>
              </RoleThemeProvider>
            </ThemeProvider>
          </QueryClientProvider>
        </Provider>
    </ErrorBoundary>
  </HelmetProvider>
  )
}

export default App
