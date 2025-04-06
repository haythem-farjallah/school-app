import { useState } from "react";
import { useNavigate } from "react-router-dom";

const Login = () => {
  const nav = useNavigate();
  const [email, setEmail] = useState("");
  const [pwd, setPwd] = useState("");

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    // TODO: call real auth API
    localStorage.setItem("token", "fake‑token");
    nav("/", { replace: true });
  };

  return (
    <form
      onSubmit={submit}
      className="mx-auto max-w-sm space-y-4 rounded-md border p-6 shadow"
    >
      <h2 className="text-xl font-semibold text-center">Login</h2>

      <input
        type="email"
        placeholder="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        required
        className="w-full rounded border px-3 py-2"
      />
      <input
        type="password"
        placeholder="Password"
        value={pwd}
        onChange={(e) => setPwd(e.target.value)}
        required
        className="w-full rounded border px-3 py-2"
      />

      <button
        type="submit"
        className="w-full rounded bg-blue-600 py-2 font-medium text-white hover:bg-blue-700"
      >
        Signin
      </button>
    </form>
  );
};

export default Login;
