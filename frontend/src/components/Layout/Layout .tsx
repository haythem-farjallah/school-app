import { Outlet, Link } from "react-router-dom";
import { RouteTitle } from "../Shared/RouteTitle";

const Layout = () => (
  <div className="flex min-h-screen flex-col">
    {/* --- Header --------------------------------------------------------- */}
    <header className="flex items-center justify-between bg-slate-800 px-4 py-3 text-white">
      <Link to="/" className="text-lg font-semibold">
        MyApp
      </Link>

      <nav className="space-x-4">
        <Link to="/" className="hover:underline">
          Home
        </Link>
        <Link to="/profile/me" className="hover:underline">
          Profile
        </Link>
      </nav>
    </header>

    {/* --- Main outlet ---------------------------------------------------- */}
    <main className="flex-1 bg-slate-50 p-4">
      <RouteTitle /> 
      <Outlet />
    </main>

    {/* --- Footer (optional) --------------------------------------------- */}
    <footer className="bg-slate-800 p-4 text-center text-xs text-white">
      ©{new Date().getFullYear()} MyApp
    </footer>
  </div>
);

export default Layout;
