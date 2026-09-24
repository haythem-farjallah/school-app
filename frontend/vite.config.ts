/// <reference types="vitest" />
import path from "path"
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/

export default defineConfig({
  plugins: [react()],
  define: {
    global: 'globalThis',
  },
  css: {
    postcss: './postcss.config.cjs',
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/setupTests.ts',
    include: ['src/**/*.test.{ts,tsx}'],
    coverage: {
      include: ['src/**'],
      reportsDirectory: './coverage',
    },
  },
   resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
})
