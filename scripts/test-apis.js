#!/usr/bin/env node

/**
 * Script de validación de APIs - DondeAnime
 * --------------------------------------------------
 * Objetivo: en el día 1 del proyecto, verificar que las dos
 * APIs externas que vamos a usar (AniList y TMDb) responden
 * con datos reales y útiles, antes de escribir nada del
 * backend "de verdad".
 *
 * Uso:
 *   1. Crear .env en la raíz del repo (copiando .env.example)
 *   2. Rellenar TMDB_API_KEY con tu token de TMDb
 *   3. Desde la raíz del repo:
 *        cd scripts
 *        npm init -y
 *        npm install node-fetch dotenv
 *        node test-apis.js
 *
 * Si todo va bien verás los datos formateados de 3 animes
 * desde AniList y los proveedores de streaming en España
 * desde TMDb. Si algo falla, te dirá QUÉ ha fallado.
 */

// Cargar variables de entorno desde el .env del repo
require('dotenv').config({ path: '../.env' });

// node-fetch v3 es ESM, usamos la v2 para CommonJS o dynamic import
let fetch;
(async () => {
  fetch = (await import('node-fetch')).default;
  await runTests();
})();

// ============================================================
// Configuración
// ============================================================

const ANILIST_URL = process.env.ANILIST_API_BASE || 'https://graphql.anilist.co';
const TMDB_URL = process.env.TMDB_API_BASE || 'https://api.themoviedb.org/3';
const TMDB_KEY = process.env.TMDB_API_KEY;

// Colores para consola (sin librerías)
const c = {
  reset: '\x1b[0m',
  bold: '\x1b[1m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  cyan: '\x1b[36m',
  gray: '\x1b[90m',
};

const log = {
  title: (msg) => console.log(`\n${c.bold}${c.cyan}━━━ ${msg} ━━━${c.reset}\n`),
  ok: (msg) => console.log(`${c.green}✓${c.reset} ${msg}`),
  fail: (msg) => console.log(`${c.red}✗${c.reset} ${msg}`),
  warn: (msg) => console.log(`${c.yellow}!${c.reset} ${msg}`),
  info: (msg) => console.log(`${c.gray}  ${msg}${c.reset}`),
};

// ============================================================
// Test 1: AniList API
// ============================================================

async function testAniList() {
  log.title('Test 1 — AniList API (GraphQL, sin auth)');

  const query = `
    query {
      Page(page: 1, perPage: 3) {
        media(type: ANIME, sort: POPULARITY_DESC, status: RELEASING) {
          id
          title { romaji english }
          startDate { year month day }
          episodes
          format
          averageScore
          coverImage { large }
        }
      }
    }
  `;

  try {
    const res = await fetch(ANILIST_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify({ query }),
    });

    if (!res.ok) {
      log.fail(`AniList respondió HTTP ${res.status}`);
      const text = await res.text();
      log.info(text.substring(0, 200));
      return null;
    }

    const data = await res.json();

    if (data.errors) {
      log.fail('AniList devolvió errores GraphQL:');
      console.log(data.errors);
      return null;
    }

    const animes = data.data.Page.media;
    log.ok(`Recibidos ${animes.length} anime emitiéndose ahora mismo:`);
    animes.forEach((a, i) => {
      const title = a.title.english || a.title.romaji;
      const date = a.startDate.year
        ? `${a.startDate.year}-${String(a.startDate.month).padStart(2, '0')}-${String(a.startDate.day).padStart(2, '0')}`
        : 'sin fecha';
      console.log(
        `${c.gray}  ${i + 1}.${c.reset} ${c.bold}${title}${c.reset}`
      );
      log.info(`ID: ${a.id} | Estrenó: ${date} | Episodios: ${a.episodes || '?'} | Score: ${a.averageScore || '?'}/100`);
    });

    // Devolvemos el primero para testear TMDb con su título
    return animes[0];
  } catch (err) {
    log.fail(`Error de red llamando a AniList: ${err.message}`);
    return null;
  }
}

// ============================================================
// Test 2: TMDb API (con auth)
// ============================================================

async function testTmdb(animeTitle) {
  log.title('Test 2 — TMDb API (REST, con API key)');

  if (!TMDB_KEY || TMDB_KEY === 'tu_clave_tmdb_aqui') {
    log.fail('TMDB_API_KEY no está configurada en .env');
    log.info('Edita el .env y pon tu clave de https://www.themoviedb.org/settings/api');
    return;
  }

  const searchTitle = animeTitle || 'Frieren';
  log.info(`Buscando "${searchTitle}" en TMDb...`);

  try {
    // Paso 1: buscar el título
    const searchUrl = `${TMDB_URL}/search/tv?query=${encodeURIComponent(searchTitle)}&language=es-ES`;
    const searchRes = await fetch(searchUrl, {
      headers: {
        Authorization: `Bearer ${TMDB_KEY}`,
        Accept: 'application/json',
      },
    });

    if (!searchRes.ok) {
      log.fail(`TMDb search respondió HTTP ${searchRes.status}`);
      if (searchRes.status === 401) {
        log.info('La API key parece incorrecta. Revisa el token v4 (el largo que empieza por eyJ...).');
      }
      return;
    }

    const searchData = await searchRes.json();

    if (!searchData.results || searchData.results.length === 0) {
      log.warn(`TMDb no encontró resultados para "${searchTitle}"`);
      return;
    }

    const first = searchData.results[0];
    log.ok(`Encontrado: "${first.name}" (id ${first.id})`);

    // Paso 2: obtener providers de streaming
    const provUrl = `${TMDB_URL}/tv/${first.id}/watch/providers`;
    const provRes = await fetch(provUrl, {
      headers: {
        Authorization: `Bearer ${TMDB_KEY}`,
        Accept: 'application/json',
      },
    });

    const provData = await provRes.json();
    const paises = ['ES', 'MX', 'AR', 'CO', 'CL'];

    log.ok('Plataformas de streaming por país:');
    paises.forEach((cc) => {
      const r = provData.results?.[cc];
      if (!r) {
        log.info(`${cc}: no hay info`);
        return;
      }
      const platforms = [
        ...(r.flatrate || []).map((p) => p.provider_name),
        ...(r.free || []).map((p) => `${p.provider_name} (gratis)`),
      ];
      log.info(`${cc}: ${platforms.length ? platforms.join(', ') : 'sin proveedores'}`);
    });
  } catch (err) {
    log.fail(`Error de red llamando a TMDb: ${err.message}`);
  }
}

// ============================================================
// Resumen final
// ============================================================

async function runTests() {
  console.log(`\n${c.bold}╔════════════════════════════════════════╗${c.reset}`);
  console.log(`${c.bold}║   DondeAnime — Validación de APIs      ║${c.reset}`);
  console.log(`${c.bold}╚════════════════════════════════════════╝${c.reset}`);

  const anime = await testAniList();
  const titleForTmdb = anime?.title?.english || anime?.title?.romaji;
  await testTmdb(titleForTmdb);

  log.title('Resumen');
  console.log(
    `${c.gray}Si ves datos arriba, las APIs están operativas y puedes`
  );
  console.log(`empezar a programar el backend. Siguiente paso:`);
  console.log(`${c.cyan}  → Inicializar proyecto Spring Boot 3${c.reset}\n`);
}
