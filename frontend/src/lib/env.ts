export const API_URL = import.meta.env.VITE_API_URL as string || "http://localhost:8088/api";

console.log("🔧 Environment - API_URL:", API_URL);

if (!API_URL) throw new Error("VITE_API_URL missing in .env file");
