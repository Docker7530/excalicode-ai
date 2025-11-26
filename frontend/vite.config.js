import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export default defineConfig({
  plugins: [
    vue({
      script: {
        defineModel: true,
        propsDestructure: true,
      },
    }),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: [
        'vue',
        'vue-router',
        {
          'element-plus': [
            'ElMessage',
            'ElMessageBox',
            'ElNotification',
            'ElLoading',
          ],
        },
      ],
      dts: true,
      eslintrc: {
        enabled: true,
        filepath: './.eslintrc-auto-import.json',
        globalsPropValue: true,
      },
    }),
    Components({
      resolvers: [
        ElementPlusResolver({
          importStyle: 'sass',
        }),
      ],
      dts: true,
      directoryAsNamespace: true,
    }),
  ],

  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@components': resolve(__dirname, 'src/components'),
      '@views': resolve(__dirname, 'src/views'),
      '@styles': resolve(__dirname, 'src/styles'),
      '@api': resolve(__dirname, 'src/api'),
      '@assets': resolve(__dirname, 'src/assets'),
    },
  },

  server: {
    port: 3000,
    host: true,
    open: true,
    warmup: {
      clientFiles: ['./src/main.js', './src/App.vue'],
    },
    proxy: {
      '/web': {
        target: 'http://localhost:9527',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/web/, ''),
        timeout: 600000,
        configure: (proxy) => {
          proxy.on('error', (err) => console.error('proxy error', err));
          proxy.on('proxyReq', (proxyReq, req) => {
            console.warn('Sending Request to the Target:', req.method, req.url);
          });
        },
      },
    },
  },

  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    minify: 'terser',
    target: 'esnext',
    cssTarget: 'chrome80',
    rollupOptions: {
      output: {
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router'],
          utils: ['axios'],
        },
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
      },
    },
    reportCompressedSize: false,
    chunkSizeWarningLimit: 1500,
  },

  css: {
    preprocessorOptions: {
      scss: {
        additionalData: '@use "@/styles/_variables.scss" as *;',
      },
    },
    codeSplit: true,
  },

  optimizeDeps: {
    include: ['vue', 'vue-router', '@element-plus/icons-vue', 'axios'],
    exclude: [],
  },

  experimental: {
    hmrPartialAccept: true,
  },

  logLevel: 'info',
  clearScreen: false,
});
