// @ts-check
import { defineConfig } from 'astro/config';

import mdx from '@astrojs/mdx';
import tailwindcss from '@tailwindcss/vite';
import { VitePWA } from 'vite-plugin-pwa';

const pwaOptions = {
  registerType: 'autoUpdate',
  injectRegister: false,
  manifestFilename: 'manifest.json',
  includeAssets: ['favicon.svg', 'favicon.ico', 'og-default.png'],
  manifest: {
    id: '/',
    name: 'DondeAnime',
    short_name: 'DondeAnime',
    description: 'Descubre donde ver anime en streaming en Espana y Latinoamerica.',
    lang: 'es',
    start_url: '/',
    scope: '/',
    display: 'standalone',
    background_color: '#0A0A0F',
    theme_color: '#A78BFA',
    icons: [
      {
        src: '/pwa-icon-192.png',
        sizes: '192x192',
        type: 'image/png',
      },
      {
        src: '/pwa-icon-512.png',
        sizes: '512x512',
        type: 'image/png',
      },
      {
        src: '/pwa-icon-maskable-512.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'maskable',
      },
    ],
  },
  workbox: {
    globPatterns: ['index.html', '**/*.{css,ico,js,json,png,svg,txt,woff2}'],
    navigateFallback: null,
    runtimeCaching: [
      {
        urlPattern: /^https:\/\/api\.dondeanime\.com\/api\//,
        handler: 'NetworkFirst',
        options: {
          cacheName: 'dondeanime-api',
          networkTimeoutSeconds: 4,
          expiration: {
            maxEntries: 80,
            maxAgeSeconds: 60 * 60,
          },
          cacheableResponse: {
            statuses: [0, 200],
          },
        },
      },
      {
        urlPattern: ({ request, sameOrigin, url }) =>
          sameOrigin && request.mode === 'navigate' && !url.pathname.startsWith('/admin'),
        handler: 'CacheFirst',
        options: {
          cacheName: 'dondeanime-pages',
          expiration: {
            maxEntries: 250,
            maxAgeSeconds: 7 * 24 * 60 * 60,
          },
          cacheableResponse: {
            statuses: [200],
          },
        },
      },
      {
        urlPattern: ({ request, sameOrigin }) =>
          sameOrigin && ['font', 'image', 'script', 'style'].includes(request.destination),
        handler: 'CacheFirst',
        options: {
          cacheName: 'dondeanime-assets',
          expiration: {
            maxEntries: 400,
            maxAgeSeconds: 30 * 24 * 60 * 60,
          },
          cacheableResponse: {
            statuses: [0, 200],
          },
        },
      },
    ],
  },
};

// @vite-pwa/astro does not declare Astro 6 support yet.
function pwaForAstro(options) {
  let pwaApi;
  let buildDone = false;

  return {
    name: 'dondeanime-pwa',
    hooks: {
      'astro:config:setup': ({ command, updateConfig }) => {
        if (command === 'preview' || command === 'sync') return;

        const withAstroManifestTransform = {
          ...options,
          workbox: {
            ...options.workbox,
            manifestTransforms: [
              ...(options.workbox?.manifestTransforms ?? []),
              async (entries) => {
                if (!buildDone) return { manifest: entries, warnings: [] };
                return {
                  manifest: entries.map((entry) => {
                    if (!entry?.url?.endsWith('.html')) return entry;
                    return { ...entry, url: cleanHtmlUrl(entry.url) };
                  }),
                  warnings: [],
                };
              },
            ],
          },
        };

        let plugins = VitePWA(withAstroManifestTransform);
        if (command === 'build') {
          plugins = plugins.filter((plugin) => plugin?.name !== 'vite-plugin-pwa:build');
          plugins.push({
            name: 'dondeanime-pwa:build',
            configResolved(resolvedConfig) {
              if (!resolvedConfig.build.ssr) {
                pwaApi = resolvedConfig.plugins
                  .flat(Number.POSITIVE_INFINITY)
                  .find((plugin) => plugin.name === 'vite-plugin-pwa')?.api;
              }
            },
            generateBundle(_, bundle) {
              pwaApi?.generateBundle(bundle, this);
            },
          });
        }

        updateConfig({ vite: { plugins } });
      },
      'astro:build:done': async () => {
        buildDone = true;
        if (pwaApi && !pwaApi.disabled) {
          await pwaApi.generateSW();
        }
      },
    },
  };
}

function cleanHtmlUrl(url) {
  const cleanUrl = url.startsWith('/') ? url.slice(1) : url;
  if (cleanUrl === 'index.html') return '/';
  return cleanUrl.replace(/\.html$/, '');
}

// https://astro.build/config
export default defineConfig({
  site: 'https://dondeanime.com',
  trailingSlash: 'never',
  build: {
    format: 'file',
  },
  vite: {
    plugins: [tailwindcss()],
  },
  integrations: [
    mdx(),
    pwaForAstro(pwaOptions),
  ],
});
