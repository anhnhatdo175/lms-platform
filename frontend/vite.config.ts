import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'node:path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: env.VITE_DEV_API_PROXY_TARGET ?? 'http://localhost:8080',
        },
        '/lms-files': {
          target: env.VITE_FILE_STORAGE_PROXY_TARGET ?? 'http://127.0.0.1:9000',
          changeOrigin: false,
        },
      },
    },
    resolve: {
      alias: {
        '@': resolve(import.meta.dirname, './src'),
      },
    },
  }
})
