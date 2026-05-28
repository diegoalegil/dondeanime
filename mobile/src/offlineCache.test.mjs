import assert from 'node:assert/strict';
import test from 'node:test';

import {
  createOfflineAnimeCache,
  isOfflineAnimeRoute,
  sanitizeAnimeForOffline,
} from './offlineCache.mjs';

test('sanitiza una ficha para offline sin ids ni tokens', () => {
  const snapshot = sanitizeAnimeForOffline({
    id: 123,
    tmdbId: 456,
    token: 'secret',
    userEmail: 'diego@example.com',
    slug: 'frieren',
    titleEnglish: 'Frieren: Beyond Journey End',
    titleRomaji: 'Sousou no Frieren',
    description: ' Fantasia tranquila ',
    episodes: 28.7,
    coverImage: 'https://img.example/frieren.jpg',
    bannerImage: 'javascript:alert(1)',
    genres: ['Adventure', 'Adventure', 'Fantasy', ''],
    studios: [{ id: 1, name: 'Madhouse' }, { token: 'x', name: ' TOHO animation ' }],
    trailers: [
      { site: 'YouTube', url: 'https://youtube.com/watch?v=abc', auth: 'hidden' },
      { site: 'Bad', url: 'ftp://example.test/video' },
    ],
  });

  assert.deepEqual(snapshot, {
    slug: 'frieren',
    title: 'Frieren: Beyond Journey End',
    titleEnglish: 'Frieren: Beyond Journey End',
    titleRomaji: 'Sousou no Frieren',
    description: 'Fantasia tranquila',
    episodes: 28,
    coverImage: 'https://img.example/frieren.jpg',
    genres: ['Adventure', 'Fantasy'],
    studios: ['Madhouse', 'TOHO animation'],
    trailers: [{ site: 'YouTube', url: 'https://youtube.com/watch?v=abc' }],
  });
});

test('mantiene las ultimas fichas visitadas en orden reciente', () => {
  const storage = memoryStorage();
  const cache = createOfflineAnimeCache({
    storage,
    limit: 2,
    now: () => '2026-05-27T04:30:00.000Z',
  });

  cache.saveVisitedAnime({ slug: 'one-piece', titleEnglish: 'One Piece' });
  cache.saveVisitedAnime({ slug: 'frieren', titleEnglish: 'Frieren' });
  cache.saveVisitedAnime({ slug: 'naruto', titleEnglish: 'Naruto' });

  assert.deepEqual(cache.listVisitedAnime().map((entry) => entry.slug), ['naruto', 'frieren']);
  assert.equal(cache.getVisitedAnime('one-piece'), null);
  assert.equal(cache.getVisitedAnime('frieren').cachedAt, '2026-05-27T04:30:00.000Z');
});

test('reemplaza una ficha existente sin duplicarla', () => {
  const cache = createOfflineAnimeCache({ storage: memoryStorage() });

  cache.saveVisitedAnime({ slug: 'frieren', titleEnglish: 'Frieren' });
  cache.saveVisitedAnime({ slug: 'naruto', titleEnglish: 'Naruto' });
  cache.saveVisitedAnime({ slug: 'frieren', titleEnglish: 'Frieren actualizado' });

  const entries = cache.listVisitedAnime();
  assert.deepEqual(entries.map((entry) => entry.slug), ['frieren', 'naruto']);
  assert.equal(entries[0].titleEnglish, 'Frieren actualizado');
});

test('ignora storage corrupto y permite limpiar cache', () => {
  const storage = memoryStorage();
  storage.setItem('dondeanime.mobile.offline.anime.v1', '{bad json');
  const cache = createOfflineAnimeCache({ storage });

  assert.deepEqual(cache.listVisitedAnime(), []);
  cache.saveVisitedAnime({ slug: 'bleach', titleEnglish: 'Bleach' });
  assert.equal(cache.listVisitedAnime().length, 1);
  cache.clear();
  assert.deepEqual(cache.listVisitedAnime(), []);
});

test('reconoce solo rutas offline de fichas publicas', () => {
  assert.equal(isOfflineAnimeRoute('/anime/frieren'), true);
  assert.equal(isOfflineAnimeRoute('https://dondeanime.com/en/anime/frieren'), true);
  assert.equal(isOfflineAnimeRoute('/admin/anime/frieren'), false);
  assert.equal(isOfflineAnimeRoute('/api/anime/frieren'), false);
  assert.equal(isOfflineAnimeRoute('/anime/Frieren'), false);
});

test('rechaza slugs invalidos', () => {
  assert.throws(
    () => sanitizeAnimeForOffline({ slug: '../admin', titleEnglish: 'Nope' }),
    /anime.slug no es valido/,
  );
});

function memoryStorage() {
  const data = new Map();
  return {
    getItem: (key) => data.get(key) ?? null,
    setItem: (key, value) => data.set(key, String(value)),
    removeItem: (key) => data.delete(key),
  };
}
