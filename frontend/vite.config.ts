import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import type { Plugin } from 'vite'
import { defineConfig } from 'vitest/config'
import { serviceWorkerTemplate } from './serviceWorkerTemplate'

function fingerprintBundle(fileNames: string[]): string {
  let hash = 2_166_136_261
  for (const character of fileNames.sort().join('|')) {
    hash ^= character.charCodeAt(0)
    hash = Math.imul(hash, 16_777_619)
  }
  return (hash >>> 0).toString(16)
}

function versionedServiceWorker(): Plugin {
  return {
    name: 'reelz-versioned-service-worker',
    apply: 'build',
    generateBundle(_options, bundle) {
      const bundleFileNames = Object.keys(bundle)
      const buildVersion = fingerprintBundle(bundleFileNames)
      const bundleAssets = bundleFileNames
        .filter((fileName) => fileName.startsWith('assets/'))
        .sort()
        .map((fileName) => `  '/${fileName}',`)
        .join('\n')
      this.emitFile({
        type: 'asset',
        fileName: 'sw.js',
        source: serviceWorkerTemplate
          .replace('__REELZ_BUILD_VERSION__', buildVersion)
          .replace('  __REELZ_BUNDLE_ASSETS__', bundleAssets),
      })
    },
  }
}

export default defineConfig({
  plugins: [react(), tailwindcss(), versionedServiceWorker()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
})
