import assert from 'node:assert/strict';
import test from 'node:test';

import { createDeepLinkHandler, parseDeepLink } from './deepLinks.mjs';

test('parsea deep link de ficha anime', () => {
  assert.deepEqual(parseDeepLink('dondeanime://anime/attack-on-titan'), {
    type: 'anime',
    slug: 'attack-on-titan',
    path: '/anime/attack-on-titan',
  });
});

test('parsea deep link de busqueda', () => {
  assert.deepEqual(parseDeepLink('dondeanime://buscar'), {
    type: 'search',
    path: '/buscar',
  });
});

test('acepta enlaces web canonicos del dominio', () => {
  assert.deepEqual(parseDeepLink('https://dondeanime.com/anime/death-note'), {
    type: 'anime',
    slug: 'death-note',
    path: '/anime/death-note',
  });
  assert.deepEqual(parseDeepLink('https://www.dondeanime.com/buscar'), {
    type: 'search',
    path: '/buscar',
  });
});

test('rechaza rutas desconocidas y slugs no seguros', () => {
  assert.equal(parseDeepLink('dondeanime://admin'), null);
  assert.equal(parseDeepLink('dondeanime://anime/Attack%20On%20Titan'), null);
  assert.equal(parseDeepLink('http://dondeanime.com/anime/death-note'), null);
  assert.equal(parseDeepLink('https://example.com/anime/death-note'), null);
});

test('handler navega solo cuando el enlace es valido', () => {
  const navigations = [];
  const handler = createDeepLinkHandler({
    navigate: (path) => navigations.push(path),
  });

  assert.deepEqual(handler({ url: 'dondeanime://buscar' }), {
    type: 'search',
    path: '/buscar',
  });
  assert.equal(handler({ url: 'dondeanime://admin' }), null);
  assert.deepEqual(navigations, ['/buscar']);
});
