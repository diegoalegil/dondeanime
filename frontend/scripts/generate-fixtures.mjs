#!/usr/bin/env node
/**
 * Regenera frontend/fixtures/ desde la API real.
 *
 * El CI builda contra estos JSON (PUBLIC_FIXTURE_DIR=fixtures) para no
 * fetchear producción en cada PR. El mapeo ruta -> fichero es el mismo que
 * aplica fixtureResponse() en src/lib/api.ts: `${ruta.replace(/[?&=]/g, '_')}.json`.
 *
 * Uso:  node scripts/generate-fixtures.mjs
 * Vars: FIXTURE_SOURCE_API (default https://api.dondeanime.com)
 *       FIXTURE_SUBSET_SIZE (default 40)
 */
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const API = process.env.FIXTURE_SOURCE_API ?? 'https://api.dondeanime.com';
const SUBSET_SIZE = Number(process.env.FIXTURE_SUBSET_SIZE ?? 40);
const OUT = resolve(dirname(fileURLToPath(import.meta.url)), '../fixtures');

// Slugs que los specs de e2e (y el manifest) asumen presentes.
const REQUIRED_SLUGS = ['attack-on-titan', 'death-note'];
// Mantener en sync con src/lib/countries.ts y src/lib/programmaticSeo.ts.
const COUNTRY_ISOS = ['ES', 'MX', 'AR', 'CO', 'CL'];
const DURATION_MINUTES = [12, 22, 24, 25, 45, 60];
const EPISODE_LIMITS = [12, 24, 50, 100, 200];
const NEWS_LIMITS = [6, 50, 100];
const UPCOMING_DAYS = [7, 30];
const MAX_UPCOMING = 10;
// La home destaca la temporada ACTUAL; sin títulos en emisión el e2e no
// encuentra la sección de estrenos (el top por popularidad son clásicos).
const MAX_RELEASING = 12;

let written = 0;
let missing = 0;

const fetchJson = async (path) => {
  const res = await fetch(`${API}${path}`, { headers: { Accept: 'application/json' } });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`${path} -> ${res.status}`);
  return res.json();
};

const fixtureFile = (path) => resolve(OUT, `.${path.replace(/[?&=]/g, '_')}.json`);

const write = async (path, data) => {
  if (data === null || data === undefined) {
    missing += 1;
    return;
  }
  const file = fixtureFile(path);
  await mkdir(dirname(file), { recursive: true });
  await writeFile(file, JSON.stringify(data, null, 1));
  written += 1;
};

// Copia el endpoint tal cual, opcionalmente filtrando arrays de anime al subset.
const mirror = async (path, filter) => {
  const data = await fetchJson(path);
  await write(path, filter && Array.isArray(data) ? data.filter(filter) : data);
  return data;
};

const main = async () => {
  await rm(OUT, { recursive: true, force: true });

  const allAnime = await fetchJson('/api/anime');
  if (!Array.isArray(allAnime) || allAnime.length === 0) {
    throw new Error('La API no devolvió catálogo.');
  }

  const byPopularity = [...allAnime].sort((a, b) => (b.popularity ?? 0) - (a.popularity ?? 0));
  const upcoming = allAnime.filter((a) => a.status === 'NOT_YET_RELEASED').slice(0, MAX_UPCOMING);
  const releasing = byPopularity.filter((a) => a.status === 'RELEASING').slice(0, MAX_RELEASING);
  const subset = new Map();
  for (const slug of REQUIRED_SLUGS) {
    const entry = allAnime.find((a) => a.slug === slug);
    if (!entry) throw new Error(`Slug requerido ausente en la API: ${slug}`);
    subset.set(slug, entry);
  }
  for (const entry of upcoming) subset.set(entry.slug, entry);
  for (const entry of releasing) subset.set(entry.slug, entry);
  for (const entry of byPopularity) {
    if (subset.size >= SUBSET_SIZE) break;
    subset.set(entry.slug, entry);
  }
  const slugs = new Set(subset.keys());
  const inSubset = (a) => a && typeof a === 'object' && slugs.has(a.slug);
  console.log(`Subset: ${subset.size} anime (de ${allAnime.length}).`);

  await write('/api/anime', [...subset.values()]);

  for (const slug of slugs) {
    await mirror(`/api/anime/${slug}`);
    await mirror(`/api/anime/${slug}/similar`, inSubset);
    await mirror(`/api/news/anime/${slug}`);
  }

  for (const days of UPCOMING_DAYS) {
    await mirror(`/api/anime/upcoming?days=${days}`, inSubset);
  }
  for (const minutes of DURATION_MINUTES) {
    await mirror(`/api/anime/duration/${minutes}`, inSubset);
  }
  for (const limit of EPISODE_LIMITS) {
    await mirror(`/api/anime/episodes/less-than/${limit}`, inSubset);
  }

  const genres = await mirror('/api/genres');
  for (const genre of genres ?? []) {
    await mirror(`/api/genres/${genre.slug}`, inSubset);
    await mirror(`/api/genres/${genre.slug}/beginner`, (b) => inSubset(b.anime ?? b));
  }

  // Solo las temporadas del subset: el build de CI no necesita 80 páginas vacías.
  const seasons = await fetchJson('/api/seasons');
  const subsetSeasons = (seasons ?? []).filter((s) =>
    [...subset.values()].some((a) => a.seasonYear === s.year && a.season === s.season));
  await write('/api/seasons', subsetSeasons);
  for (const s of subsetSeasons) {
    await mirror(`/api/seasons/${s.year}/${s.season}`, inSubset);
  }

  const subsetStudios = new Set(
    [...subset.values()].map((a) => a.studio).filter(Boolean));
  const studios = await fetchJson('/api/studios');
  const keptStudios = (studios ?? []).filter((st) =>
    subsetStudios.has(st.name) || st.slug === 'madhouse');
  await write('/api/studios', keptStudios.length > 0 ? keptStudios : studios);
  for (const st of keptStudios) {
    await mirror(`/api/studios/${st.slug}`, inSubset);
    await mirror(`/api/studios/${st.slug}/best`, inSubset);
  }

  const providers = await mirror('/api/providers');
  for (const iso of COUNTRY_ISOS) {
    await mirror(`/api/providers?country=${iso}`);
    await mirror(`/api/providers/country/${iso}/anime`, inSubset);
    for (const provider of providers ?? []) {
      await mirror(`/api/providers/${provider.slug}/${iso}`, inSubset);
    }
  }

  const lists = await mirror('/api/lists');
  for (const list of lists ?? []) {
    await mirror(`/api/lists/${list.slug}`);
  }

  let newsItems = [];
  await mirror('/api/news/slugs');
  for (const limit of NEWS_LIMITS) {
    const news = await mirror(`/api/news?limit=${limit}`);
    if (Array.isArray(news) && news.length > newsItems.length) newsItems = news;
  }
  for (const item of newsItems) {
    await mirror(`/api/news/${item.slug}`);
  }

  console.log(`Fixtures: ${written} ficheros escritos, ${missing} endpoints sin datos (404).`);
};

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
