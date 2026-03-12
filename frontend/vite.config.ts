import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            // 确保 Authorization 头被转发到后端（部分代理会默认移除）
            const auth = (req.headers as Record<string, string>).authorization
            if (auth) {
              proxyReq.setHeader('Authorization', auth)
            }
          })
        }
      }
    }
  }
})

