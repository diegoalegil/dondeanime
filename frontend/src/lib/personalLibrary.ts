/**
 * Biblioteca personal del usuario, 100% en el navegador (localStorage). Sin cuenta
 * ni backend: favoritos ("Mi lista") y "vistos recientemente". El sitio es estático,
 * así que cada item guarda lo justo para pintar su tarjeta en cliente sin pedir nada
 * a la API (título, portada, meta ya formateada y nota).
 *
 * Se importa desde los `<script>` de los componentes (FavoriteButton, la home y la
 * página Mi lista). No importar nada de servidor aquí: este módulo corre en cliente.
 */
import { scoreBadgeClass } from './animeBadge';

export interface LibraryItem {
  slug: string;
  title: string;
  image: string;
  /** Línea ya formateada "año · tipo" (se construye con la locale donde se guardó). */
  meta: string;
  /** Nota media 0-100, o null si no hay. */
  score: number | null;
}

export const FAVORITES_KEY = 'dondeanime-favorites';
export const RECENT_KEY = 'dondeanime-recent';

const MAX_FAVORITES = 200;
const MAX_RECENT = 12;

/** Evento que disparamos al cambiar la biblioteca, para que la UI se refresque. */
export const LIBRARY_CHANGE_EVENT = 'dondeanime:library-change';

function isItem(value: unknown): value is LibraryItem {
  if (!value || typeof value !== 'object') return false;
  const item = value as Record<string, unknown>;
  return (
    typeof item.slug === 'string' &&
    item.slug.length > 0 &&
    typeof item.title === 'string' &&
    typeof item.image === 'string' &&
    typeof item.meta === 'string' &&
    (item.score === null || typeof item.score === 'number')
  );
}

function readList(key: string): LibraryItem[] {
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(isItem);
  } catch {
    return [];
  }
}

function writeList(key: string, items: LibraryItem[]): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(items));
    window.dispatchEvent(new CustomEvent(LIBRARY_CHANGE_EVENT, { detail: { key } }));
  } catch {
    // Modo privado o cuota llena: ignoramos, la biblioteca es best-effort.
  }
}

export function getFavorites(): LibraryItem[] {
  return readList(FAVORITES_KEY);
}

export function isFavorite(slug: string): boolean {
  return getFavorites().some((item) => item.slug === slug);
}

/** Añade o quita de favoritos. Devuelve el nuevo estado (true = ahora es favorito). */
export function toggleFavorite(item: LibraryItem): boolean {
  const current = getFavorites();
  const without = current.filter((existing) => existing.slug !== item.slug);
  if (without.length !== current.length) {
    writeList(FAVORITES_KEY, without);
    return false;
  }
  writeList(FAVORITES_KEY, [item, ...without].slice(0, MAX_FAVORITES));
  return true;
}

export function removeFavorite(slug: string): void {
  const current = getFavorites();
  const without = current.filter((item) => item.slug !== slug);
  if (without.length !== current.length) {
    writeList(FAVORITES_KEY, without);
  }
}

export function getRecent(): LibraryItem[] {
  return readList(RECENT_KEY);
}

/** Registra una ficha como "vista recientemente" (la pone primera, sin duplicar). */
export function recordRecent(item: LibraryItem): void {
  const current = getRecent().filter((existing) => existing.slug !== item.slug);
  writeList(RECENT_KEY, [item, ...current].slice(0, MAX_RECENT));
}

/**
 * Pinta una tarjeta de anime en cliente con el MISMO marcado/clases que AnimeCard.astro,
 * para que Mi lista y la home se vean idénticas al resto del catálogo. Construido con
 * createElement + textContent (nunca innerHTML con datos) para evitar cualquier XSS.
 */
export function renderLibraryCard(item: LibraryItem, animePrefix: string): HTMLElement {
  const article = document.createElement('article');
  article.className = 'group';
  article.dataset.animeSlug = item.slug;

  const link = document.createElement('a');
  link.href = `${animePrefix}/${item.slug}`;
  link.className = 'block';

  const cover = document.createElement('div');
  cover.className = 'relative aspect-[2/3] overflow-hidden rounded-lg bg-surface-2';

  const img = document.createElement('img');
  img.src = item.image;
  img.alt = item.title;
  img.loading = 'lazy';
  img.decoding = 'async';
  img.width = 320;
  img.height = 480;
  img.className = 'h-full w-full object-cover transition-transform duration-300 group-hover:scale-105';
  img.addEventListener(
    'error',
    () => {
      if (img.src.endsWith('/cover-placeholder.webp')) return;
      img.src = '/cover-placeholder.webp';
    },
    { once: true },
  );
  cover.appendChild(img);

  if (item.score !== null) {
    const badge = document.createElement('span');
    badge.className = `absolute right-2 top-2 rounded-md px-2 py-1 text-xs font-semibold backdrop-blur-md ${scoreBadgeClass(item.score)}`;
    badge.textContent = String(item.score);
    cover.appendChild(badge);
  }

  link.appendChild(cover);

  const body = document.createElement('div');
  body.className = 'mt-3';

  const heading = document.createElement('h3');
  heading.className =
    'line-clamp-2 text-sm font-semibold text-fg-primary transition-colors group-hover:text-accent-violet';
  heading.textContent = item.title;

  const metaLine = document.createElement('p');
  metaLine.className = 'mt-1 text-xs text-fg-muted';
  metaLine.textContent = item.meta;

  body.append(heading, metaLine);
  link.appendChild(body);
  article.appendChild(link);

  return article;
}
