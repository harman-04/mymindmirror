import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { VitePWA } from "vite-plugin-pwa";
import crypto from "node:crypto";

if (!crypto.hash) {
  crypto.hash = (algo, content) => {
    return crypto.createHash(algo).update(content).digest("hex");
  };
}

export default defineConfig(({ mode }) => ({
  define: {
    global: "window",
  },
  plugins: [
    react({ jsxRuntime: 'automatic' }),
    tailwindcss(),
    mode === 'production' && VitePWA({
      registerType: "autoUpdate",
      manifestFilename: "manifest.json",
      includeAssets: ["icon.png", "og-image.png"],
      manifest: {
        name: "MyMindMirror",
        short_name: "MyMindMirror",
        description: "AI-Powered Journaling & Personal Growth",
        // 🌟 AESTHETIC UPGRADE: Synced PWA OS colors to your new #131127 dark background!
        theme_color: "#131127",
        background_color: "#131127",
        display: "standalone",
        start_url: "/",
        icons: [
          { src: "/icon.png", sizes: "192x192", type: "image/png" },
          { src: "/icon.png", sizes: "512x512", type: "image/png" }
        ]
      },
      workbox: {
        globPatterns: ["**/*.{js,css,html,ico,png,svg,woff2}"],
        globIgnores: ["**/icon.png", "**/logo.png"],
        maximumFileSizeToCacheInBytes: 5 * 1024 * 1024,
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/fonts\.googleapis\.com\/.*/i,
            handler: "CacheFirst",
            options: {
              cacheName: "google-fonts-cache",
              expiration: {
                maxEntries: 10,
                maxAgeSeconds: 60 * 60 * 24 * 365
              }
            }
          },
          {
            urlPattern: /^https:\/\/fonts\.gstatic\.com\/.*/i,
            handler: "CacheFirst",
            options: {
              cacheName: "gstatic-fonts-cache",
              expiration: {
                maxEntries: 10,
                maxAgeSeconds: 60 * 60 * 24 * 365
              }
            }
          }
        ]
      }
    }),
  ],

  // 🌟 PERFORMANCE UPGRADE: Strips all console.logs in production automatically!
  esbuild: {
    drop: mode === 'production' ? ['console', 'debugger'] : [],
  },

  // 🚀 PHASE 6: ENTERPRISE BUILD OPTIMIZATION
    build: {
      target: 'esnext',
      cssCodeSplit: true,
      chunkSizeWarningLimit: 2500, // Silences the warning
      // REMOVED rollupOptions entirely! Vite will now mathematically sort your chunks.
    },

  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: "./src/test/setup.js",
    css: true,
  },
}));