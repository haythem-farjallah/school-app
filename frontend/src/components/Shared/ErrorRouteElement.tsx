import { isRouteErrorResponse, useRouteError, Link } from "react-router-dom";

const ErrorRouteElement = () => {
  const err = useRouteError();

  let message = "Something went wrong.";
  if (isRouteErrorResponse(err)) message = `${err.status} – ${err.statusText}`;

  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4">
      <h1 className="text-3xl font-bold text-red-600">Oops!</h1>
      <p className="text-lg">{message}</p>
      <Link
        to="/"
        className="rounded bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700"
      >
        GoHome
      </Link>
    </div>
  );
};

export default ErrorRouteElement;
