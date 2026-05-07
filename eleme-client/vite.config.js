import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// const baseUrl = {
//   development: './',
//   beta: './',
//   release: './'
// }

export default defineConfig({
  base: '/client/',
  plugins: [
    vue()
  ],
  resolve: {
    alias: {  // 设置别名
      '~': path.resolve(__dirname, './'),
      '@': path.resolve(__dirname, './src'),
    },
    extensions: ['.vue', '.js', '.jsx', '.json']
  },
  server: {
    cors: true, // 默认启用并允许任何源
    // hmr: {
    //   protocol: 'ws',
    //   host: 'localhost',
    // },
    // host: '0.0.0.0',
    proxy: {
      '/v1': {
        target: 'http://cangdu.org:8001',
        changeOrigin: true
      },
      '/v2': {
        target: 'http://cangdu.org:8001',
        changeOrigin: true
      },
      '/v4': {
        target: 'http://cangdu.org:8001',
        changeOrigin: true
      },
      "/img": {
        target: "http://cangdu.org:8001",   // 代理地址
        changeOrigin: true, // 是否跨域
      },
      "/shopping": {
        target: "http://cangdu.org:8001",   // 代理地址
        changeOrigin: true, // 是否跨域
      },
      "/ugc": {
        target: "http://cangdu.org:8001",   // 代理地址
        changeOrigin: true, // 是否跨域
      },
      "/bos": {
        target: "http://cangdu.org:8001",   // 代理地址
        changeOrigin: true, // 是否跨域
      },
      "/payapi": {
        target: "http://cangdu.org:8001",   // 代理地址
        changeOrigin: true, // 是否跨域
      },
      "/eus": {
        target: "http://cangdu.org:8001",   // 代理地址
        changeOrigin: true, // 是否跨域
      },
      // '/assistant': {
      //   target: 'https://restapi.amap.com/v3/assistant',
      //   // secure: true,  // 如果是https接口，需要配置这个参数
      //   changeOrigin: true,
      //   rewrite: (path) => path.replace(/^\/assistant/, '')
      // },
      // '/weather': {
      //   target: 'https://restapi.amap.com/v3/weather',
      //   secure: false,  // 如果是https接口，需要配置这个参数
      //   changeOrigin: true,  //是否跨域
      //   rewrite: (path) => path.replace(/^\/weather/, '')
      // },

    }
  }
})
