import { resolve } from 'path'

import vue from '@vitejs/plugin-vue'
import UnoCSS from 'unocss/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import Components from 'unplugin-vue-components/vite'
import { defineConfig } from 'vite'

import autoImport from './vite/autoImport'

const realtimeProxy = {
  target: 'http://localhost:8080',
  // Preserve Host so WebSocket requests keep the browser's same-origin boundary.
  changeOrigin: false,
}

const backendProxy = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
  '/realtime/sse': realtimeProxy,
  '/realtime/ws': { ...realtimeProxy, ws: true },
}

export default defineConfig({
  plugins: [
    vue(),
    UnoCSS(),
    autoImport,
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': resolve(import.meta.dirname, 'src'),
    },
  },
  preview: { proxy: backendProxy },
  server: {
    proxy: backendProxy,
  },
})
