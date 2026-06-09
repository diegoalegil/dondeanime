const PAGE_CACHE = 'dondeanime-pages-v1';
const STATIC_CACHE = 'dondeanime-static-v1';
const ASSET_CACHE = 'dondeanime-assets-v1';
const IMAGE_CACHE = 'dondeanime-images-v1';
const OFFLINE_URL = '/offline';
const MAX_CACHED_ANIME_PAGES = 12;
const MAX_CACHED_ASSETS = 80;
const MAX_CACHED_IMAGES = 120;
const ANIME_PAGE_PATTERN = /^\/(?:en\/)?anime\/[a-z0-9]+(?:-[a-z0-9]+)*\/?$/;
const STATIC_PATH_PATTERN = /^\/(?:_astro\/|pwa\/|favicon\.svg$|manifest\.json$)/;
const ALERT_SYNC_TAG = 'dondeanime-alerts-sync';
const ALERT_DB_NAME = 'dondeanime-alerts';
const ALERT_STORE = 'alerts';
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
  const keep = new Set([PAGE_CACHE, STATIC_CACHE, ASSET_CACHE, IMAGE_CACHE]);

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
  const url = new URL(request.url);
  const contentType = response.headers.get('content-type') ?? '';
  if (!ANIME_PAGE_PATTERN.test(url.pathname) || !contentType.includes('text/html')) return;

  const cache = await caches.open(PAGE_CACHE);
  await cache.delete(request);
  await cache.put(request, response.clone());

  const cachedPages = await cache.keys();
  const overflow = cachedPages.length - MAX_CACHED_ANIME_PAGES;
  if (overflow <= 0) return;

  await Promise.all(cachedPages.slice(0, overflow).map((cachedRequest) => cache.delete(cachedRequest)));
};

const trimCache = async (cache, maxEntries) => {
  const keys = await cache.keys();
  const overflow = keys.length - maxEntries;
  if (overflow <= 0) return;

  await Promise.all(keys.slice(0, overflow).map((cachedRequest) => cache.delete(cachedRequest)));
};

const putRuntimeResponse = async (cacheName, request, response, maxEntries, { allowOpaque = false } = {}) => {
  if (!response || (!response.ok && !(allowOpaque && response.type === 'opaque'))) return;

  const cache = await caches.open(cacheName);
  await cache.delete(request);
  await cache.put(request, response.clone());
  await trimCache(cache, maxEntries);
};

const cacheFirst = async (cacheName, request, maxEntries, options) => {
  const cache = await caches.open(cacheName);
  const cached = await cache.match(request);
  if (cached) return cached;

  const response = await fetch(request);
  await putRuntimeResponse(cacheName, request, response, maxEntries, options);
  return response;
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

const isAdminOrApiPath = (url) => (
  url.origin === self.location.origin
    && (url.pathname === '/api'
      || url.pathname.startsWith('/api/')
      || url.pathname === '/admin'
      || url.pathname.startsWith('/admin/'))
);

const isStaticAssetRequest = (request, url) => (
  url.origin === self.location.origin
    && (STATIC_PATH_PATTERN.test(url.pathname)
      || ['font', 'script', 'style', 'manifest'].includes(request.destination))
);

const openAlertDb = () => new Promise((resolve, reject) => {
  const request = indexedDB.open(ALERT_DB_NAME, 1);

  request.onupgradeneeded = () => {
    const db = request.result;
    if (!db.objectStoreNames.contains(ALERT_STORE)) {
      db.createObjectStore(ALERT_STORE, { keyPath: 'id' });
    }
  };

  request.onsuccess = () => resolve(request.result);
  request.onerror = () => reject(request.error);
});

const queuedAlerts = async () => {
  const db = await openAlertDb();
  const alerts = await new Promise((resolve, reject) => {
    const tx = db.transaction(ALERT_STORE, 'readonly');
    const request = tx.objectStore(ALERT_STORE).getAll();

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });

  db.close();
  return alerts;
};

const deleteQueuedAlert = async (id) => {
  const db = await openAlertDb();

  await new Promise((resolve, reject) => {
    const tx = db.transaction(ALERT_STORE, 'readwrite');
    tx.objectStore(ALERT_STORE).delete(id);
    tx.oncomplete = () => resolve(undefined);
    tx.onerror = () => reject(tx.error);
  });

  db.close();
};

const notifyAlertSynced = async () => {
  if (!self.registration.showNotification || Notification.permission !== 'granted') return;

  await self.registration.showNotification('Alerta enviada', {
    body: 'La alerta pendiente se ha enviado. Revisa el correo de confirmacion.',
    icon: '/pwa/icons/icon-192.svg',
    badge: '/pwa/icons/maskable-icon.svg',
    tag: 'dondeanime-alert-sync',
  });
};

const flushQueuedAlerts = async () => {
  const alerts = await queuedAlerts();

  for (const alert of alerts) {
    const response = await fetch(alert.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(alert.payload),
    });

    if (!response.ok) {
      throw new Error(`Alert sync failed with ${response.status}`);
    }

    await deleteQueuedAlert(alert.id);
    await notifyAlertSynced();
  }
};

self.addEventListener('fetch', (event) => {
  const { request } = event;
  const url = new URL(request.url);

  if (request.method !== 'GET' || isAdminOrApiPath(url)) return;

  if (request.mode === 'navigate') {
    event.respondWith(navigationResponse(request));
    return;
  }

  if (request.destination === 'image') {
    event.respondWith(cacheFirst(IMAGE_CACHE, request, MAX_CACHED_IMAGES, { allowOpaque: true }));
    return;
  }

  if (isStaticAssetRequest(request, url)) {
    event.respondWith(cacheFirst(ASSET_CACHE, request, MAX_CACHED_ASSETS));
    return;
  }

  if (url.origin === self.location.origin) {
    event.respondWith(
      caches.match(request).then((cached) => cached ?? fetch(request)),
    );
  }
});

self.addEventListener('sync', (event) => {
  if (event.tag !== ALERT_SYNC_TAG) return;
  event.waitUntil(flushQueuedAlerts());
});

self.addEventListener('message', (event) => {
  if (event.data?.type !== 'SYNC_ALERTS') return;
  event.waitUntil(flushQueuedAlerts());
});
