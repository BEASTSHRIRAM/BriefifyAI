// frontend/vite.config.js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // Removed any 'css' or 'optimizeDeps' configurations for ultimate simplicity
});