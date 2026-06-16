import path from 'path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tsconfigPaths from 'vite-tsconfig-paths';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const API_URL = `${env.VITE_APP_BASE_NAME}`;
  const PORT = 3000;

  return {
    server: {
      // this ensures that the browser opens upon server start
      open: true,
      // this sets a default port to 3000
      port: PORT,
      host: true
    },
    preview: {
      open: true,
      host: true
    },
    define: {
      global: 'window'
    },
    resolve: {
      alias: [
        {
          find: '@novnc/novnc/core/rfb',
          replacement: path.resolve(__dirname, 'node_modules/@novnc/novnc/core/rfb.js')
        }
      ]
    },
    optimizeDeps: {
      include: ['@novnc/novnc/core/rfb'],
      esbuildOptions: { target: 'esnext' }
    },
    build: {
      target: 'esnext'
    },
    base: API_URL,
    plugins: [react(), tsconfigPaths()]
  };
});
