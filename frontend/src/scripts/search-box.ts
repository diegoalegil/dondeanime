interface SearchEntry {
  slug: string;
  en: string | null;
  jp: string | null;
  year: number | null;
  format: string;
  cover: string;
}

const normalize = (value: string) =>
  value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');

const normalizeNullable = (value: string | null | undefined) => normalize(value ?? '');

const escapeHtml = (value: string) =>
  value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');

export async function initSearchBox(root: HTMLElement) {
  if (root.dataset.searchReady === 'true') return;
  root.dataset.searchReady = 'true';

  const input = root.querySelector<HTMLInputElement>('[data-search-input]');
  const panel = root.querySelector<HTMLElement>('[data-search-results]');
  if (!input || !panel) return;

  let indexPromise: Promise<SearchEntry[]> | null = null;

  const loadIndex = () => {
    indexPromise ??= fetch('/search-index.json', {
      headers: { Accept: 'application/json' },
    }).then((res) => {
      if (!res.ok) throw new Error(`Search index failed: ${res.status}`);
      return res.json() as Promise<SearchEntry[]>;
    });
    return indexPromise;
  };

  const hidePanel = () => {
    panel.classList.add('hidden');
    panel.innerHTML = '';
  };

  const renderMatches = (matches: SearchEntry[]) => {
    if (matches.length === 0) {
      panel.innerHTML = '<p class="px-4 py-3 text-sm text-fg-muted">Sin resultados</p>';
      panel.classList.remove('hidden');
      return;
    }

    panel.innerHTML = matches
      .map((anime) => `
        <a href="/anime/${encodeURIComponent(anime.slug)}" class="flex items-center gap-3 border-b border-surface-2/50 px-3 py-2 last:border-b-0 transition-colors hover:bg-surface-2">
          <img src="${escapeHtml(anime.cover)}" alt="" loading="lazy" decoding="async" class="h-12 w-9 rounded object-cover ring-1 ring-surface-2" />
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-semibold text-fg-primary">${escapeHtml(anime.en ?? anime.jp ?? anime.slug)}</p>
            <p class="truncate text-xs text-fg-muted">${anime.year ?? '-'} · ${escapeHtml(anime.format)}</p>
          </div>
        </a>`)
      .join('');
    panel.classList.remove('hidden');
  };

  const search = async () => {
    const query = input.value.trim();
    if (!query) {
      hidePanel();
      return;
    }

    panel.innerHTML = '<p class="px-4 py-3 text-sm text-fg-muted">Buscando...</p>';
    panel.classList.remove('hidden');

    try {
      const index = await loadIndex();
      const normalizedQuery = normalize(query);
      const matches = index
        .filter((anime) =>
          normalizeNullable(anime.en).includes(normalizedQuery) ||
          normalizeNullable(anime.jp).includes(normalizedQuery) ||
          anime.slug.includes(normalizedQuery),
        )
        .slice(0, 8);

      renderMatches(matches);
    } catch (error) {
      console.error('Search index failed:', error);
      panel.innerHTML = '<p class="px-4 py-3 text-sm text-fg-muted">No se pudo cargar la búsqueda</p>';
      panel.classList.remove('hidden');
    }
  };

  input.addEventListener('input', () => { void search(); });
  input.addEventListener('focus', () => { void search(); });
  document.addEventListener('click', (event) => {
    if (!root.contains(event.target as Node)) hidePanel();
  });

  await search();
}
