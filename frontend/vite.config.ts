import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 默认后端地址（本机直接跑前端 dev 时）
const DEFAULT_BACKEND = 'http://localhost:9090'
// 允许通过环境变量覆盖（例如在 Docker 容器中设置为 http://backend:9090）
const BACKEND_TARGET = process.env.VITE_BACKEND_URL || DEFAULT_BACKEND

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
        target: BACKEND_TARGET,
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
      },
      // 上传资源（背景图等）是通过 /uploads/** 直接访问的，需要代理到后端
      '/uploads': {
        target: BACKEND_TARGET,
        changeOrigin: true
      }
    }
  }
})

