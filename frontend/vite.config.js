import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// https://vitejs.dev/config/
export default defineConfig({
  base: './', // 设置为相对路径
  plugins: [vue()],
  server: {
    port: 5173, // 你可以指定一个端口
    proxy: {
      // 将 /api 路径的请求代理到 Spring Boot 后端
      '/api': {
        target: 'http://localhost:18080',
        changeOrigin: true,
        // 如果后端API没有/api前缀，可以在这里重写路径
        // rewrite: (path) => path.replace(/^\/api/, ''), 
      },
      // 将 /ws 路径的请求代理到 Spring Boot WebSocket
      '/ws': {
        target: 'http://localhost:18080',
        ws: true, // 开启 WebSocket 代理
        changeOrigin: true,
      }
    }
  },
  build: {
    // 定义构建输出目录
    outDir: 'dist',
    // 定义静态资源根路径
    assetsDir: 'assets',
  }
});
