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
import { HelmetProvider } from "react-helmet-async";
import { AppRoutes } from "./routes/AppRoutes";

const queryClient =new QueryClient();
function App() {

  return (
  <HelmetProvider>
    <ErrorBoundary FallbackComponent={FallbackErrorBoundary}>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
          <ThemeProvider>
            <LanguageProvider>
              <Suspense fallback={<div>Loading...</div>}>
                <AppRoutes/>
              </Suspense>
              <Toaster/>
                {import.meta.env.DEV && (
                    <ReactQueryDevtools initialIsOpen={false} />
                  )}          
                </LanguageProvider>
            </ThemeProvider>
          </QueryClientProvider>
        </Provider>
    </ErrorBoundary>
  </HelmetProvider>
  )
}

export default App
