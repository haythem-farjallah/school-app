import { lazy, Suspense } from "react";
import {
  createBrowserRouter,
  RouterProvider,
} from "react-router-dom";
import Layout from "../components/Layout/Layout ";               // your site frame
import Shimmer from "../components/Shimmer/Shimmer"; // generic loader
import ErrorRouteElement from "../components/Shared/ErrorRouteElement";
//import useAuthCheck from "@/hooks/useAuthCheck";


/* -------------------------------------------------------------------------- */
/*  Lazy pages                                                                */
/* -------------------------------------------------------------------------- */
const Home    = lazy(() => import("../pages/Home"));
const Profile = lazy(() => import("../pages/Profile"));
const Login   = lazy(() => import("../pages/Login"));

/* -------------------------------------------------------------------------- */
/*  Small guard component                                                     */
/* -------------------------------------------------------------------------- */
/*const Protected = ({ children }: { children: JSX.Element }) => {
  const isAuth = useAuthCheck();          // your custom hook
  return isAuth ? children : <Navigate to="/login" replace />;
};
*/

/* -------------------------------------------------------------------------- */
/*  Router definition                                                         */
/* -------------------------------------------------------------------------- */
const router = createBrowserRouter([
  {
    path: "/",
    element: <Layout />,                  // nav bar, footer, etc.
    errorElement: <ErrorRouteElement />,  // catches render / loader errors
    children: [
      /* ---------- Public routes ------------------------------------------ */
      {
        path: "login",
        element: (
          <Suspense fallback={<Shimmer />}>
            <Login />
          </Suspense>
        ),
        handle: { title: "Log in | MyApp" },
      },

      /* ---------- Protected routes -------------------------------------- */
      {
        index: true,
        element: (
            <Suspense fallback={<Shimmer />}>
              <Home />
            </Suspense>
        ),
        handle: { title: "Home | MyApp" },
      },
      {
        path: "profile/:username",
        element: (
            <Suspense fallback={<Shimmer />}>
              <Profile />
            </Suspense>
        ),
        handle: { title: "Profile | MyApp" },
      },
    ],
  },
]);

/* -------------------------------------------------------------------------- */
/*  Router provider                                                           */
/* -------------------------------------------------------------------------- */
export const AppRoutes = () => <RouterProvider router={router} />;
