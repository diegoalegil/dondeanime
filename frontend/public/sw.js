const PAGE_CACHE = 'dondeanime-pages-v1';
const STATIC_CACHE = 'dondeanime-static-v1';
const OFFLINE_URL = '/offline';
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
    badge: '/pwa/icons/icon-192-maskable.svg',
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

  if (request.method !== 'GET' || url.origin !== self.location.origin) return;

  if (request.mode === 'navigate') {
    event.respondWith(navigationResponse(request));
    return;
  }

  event.respondWith(
    caches.match(request).then((cached) => cached ?? fetch(request)),
  );
});

self.addEventListener('sync', (event) => {
  if (event.tag !== ALERT_SYNC_TAG) return;
  event.waitUntil(flushQueuedAlerts());
});

self.addEventListener('message', (event) => {
  if (event.data?.type !== 'SYNC_ALERTS') return;
  event.waitUntil(flushQueuedAlerts());
});
