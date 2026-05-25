<script lang="ts">
  import { onDestroy, onMount } from 'svelte';

  export let apiUrl = '';

  interface SearchResult {
    slug: string;
    titleEnglish: string;
    titleRomaji: string;
    format: string;
    year: number | null;
    coverImage: string;
  }

  const inputId = 'site-search-input';
  const listId = 'site-search-results';

  let root: HTMLFormElement;
  let input: HTMLInputElement;
  let query = '';
  let results: SearchResult[] = [];
  let open = false;
  let loading = false;
  let timer: ReturnType<typeof setTimeout> | undefined;
  let requestId = 0;

  const searchUrl = (value: string) =>
    `${apiUrl}/api/search?q=${encodeURIComponent(value)}&limit=5`;

  const isEditable = (target: EventTarget | null) => {
    if (!(target instanceof HTMLElement)) return false;
    return (
      target.tagName === 'INPUT' ||
      target.tagName === 'TEXTAREA' ||
      target.isContentEditable
    );
  };

  const clearResults = () => {
    results = [];
    open = false;
    loading = false;
  };

  const runSearch = async () => {
    const value = query.trim();
    if (value.length < 2) {
      clearResults();
      return;
    }

    const currentRequest = ++requestId;
    loading = true;
    open = true;

    try {
      const response = await fetch(searchUrl(value), {
        headers: { Accept: 'application/json' },
      });
      if (!response.ok) {
        if (currentRequest === requestId) clearResults();
        return;
      }
      const data = (await response.json()) as SearchResult[];
      if (currentRequest === requestId) {
        results = data;
        open = true;
      }
    } catch {
      if (currentRequest === requestId) clearResults();
    } finally {
      if (currentRequest === requestId) loading = false;
    }
  };

  const scheduleSearch = () => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      void runSearch();
    }, 200);
  };

  const onDocumentClick = (event: MouseEvent) => {
    if (root && !root.contains(event.target as Node)) {
      open = false;
    }
  };

  const onDocumentKeydown = (event: KeyboardEvent) => {
    if (event.key === '/' && !isEditable(event.target)) {
      event.preventDefault();
      input?.focus();
      input?.select();
    }

    if (event.key === 'Escape') {
      open = false;
      input?.blur();
    }
  };

  if (typeof document !== 'undefined') {
    onMount(() => {
      document.addEventListener('click', onDocumentClick);
      document.addEventListener('keydown', onDocumentKeydown);
    });

    onDestroy(() => {
      if (timer) clearTimeout(timer);
      document.removeEventListener('click', onDocumentClick);
      document.removeEventListener('keydown', onDocumentKeydown);
    });
  }
</script>

<form bind:this={root} class="relative" role="search" action="/buscar" method="get">
  <label class="sr-only" for={inputId}>Buscar anime</label>
  <div class="flex items-center gap-2 rounded-md border border-surface-2 bg-surface-1 px-3 py-1.5">
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      class="h-4 w-4 text-fg-muted"
      aria-hidden="true"
    >
      <circle cx="11" cy="11" r="7" />
      <path d="m21 21-4.3-4.3" />
    </svg>
    <input
      bind:this={input}
      bind:value={query}
      id={inputId}
      name="q"
      type="search"
      placeholder="Buscar anime..."
      autocomplete="off"
      class="w-32 bg-transparent text-sm text-fg-primary placeholder:text-fg-muted focus:outline-none md:w-48"
      aria-autocomplete="list"
      aria-controls={listId}
      aria-expanded={open}
      on:input={scheduleSearch}
      on:focus={() => query.trim().length >= 2 && (open = true)}
    />
  </div>

  {#if open}
    <div
      id={listId}
      class="absolute right-0 top-full z-50 mt-2 max-h-[60vh] w-80 overflow-y-auto rounded-lg border border-surface-2 bg-surface-1 shadow-2xl shadow-black/40"
      role="listbox"
    >
      {#if loading}
        <p class="px-4 py-3 text-sm text-fg-muted">Buscando...</p>
      {:else if results.length === 0}
        <p class="px-4 py-3 text-sm text-fg-muted">Sin resultados</p>
      {:else}
        {#each results as anime}
          <a
            href={`/anime/${anime.slug}`}
            class="flex items-center gap-3 border-b border-surface-2/50 px-3 py-2 last:border-b-0 transition-colors hover:bg-surface-2 focus:bg-surface-2 focus:outline-none"
            role="option"
          >
            <img
              src={anime.coverImage}
              alt=""
              loading="lazy"
              class="h-12 w-9 rounded object-cover ring-1 ring-surface-2"
            />
            <span class="min-w-0 flex-1">
              <span class="block truncate text-sm font-semibold text-fg-primary">
                {anime.titleEnglish || anime.titleRomaji}
              </span>
              <span class="block truncate text-xs text-fg-muted">
                {anime.year ?? '---'} - {anime.format}
              </span>
            </span>
          </a>
        {/each}
      {/if}
    </div>
  {/if}
</form>
