const PAGE_CACHE = 'dondeanime-pages-v1';
const STATIC_CACHE = 'dondeanime-static-v1';
const OFFLINE_URL = '/offline';
const PRECACHE_URLS = [
  OFFLINE_URL,
  '/favicon.svg',
  '/manifest.json',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE)
      .then((cache) => cache.addAll(PRECACHE_URLS))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', (event) => {
  const keep = new Set([PAGE_CACHE, STATIC_CACHE]);

  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(
        keys.filter((key) => key.startsWith('dondeanime-') && !keep.has(key))
          .map((key) => caches.delete(key)),
      ))
      .then(() => self.clients.claim()),
  );
});

const cachePage = async (request, response) => {
  if (!response || !response.ok || response.type === 'opaque') return;
  const cache = await caches.open(PAGE_CACHE);
  await cache.put(request, response.clone());
};

const navigationResponse = async (request) => {
  try {
    const response = await fetch(request);
    await cachePage(request, response);
    return response;
  } catch {
    const cached = await caches.match(request);
    return cached ?? caches.match(OFFLINE_URL);
  }
};

self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  if (request.method !== 'GET' || url.origin !== self.location.origin) return;

  if (request.mode === 'navigate') {
    event.respondWith(navigationResponse(request));
    return;
  }

  event.respondWith(
    caches.match(request).then((cached) => cached ?? fetch(request)),
  );
});
