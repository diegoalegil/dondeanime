<script lang="ts">
  import { onMount } from 'svelte';
  import type { TraktWatchedResponse } from '@/lib/api';

  export let apiUrl = '';

  const externalUserKey = 'dondeanime.trakt.externalUserId';
  const hideWatchedKey = 'dondeanime.trakt.hideWatched';
  const watchedSlugsKey = 'dondeanime.trakt.watchedSlugs';
  const externalUserInputId = 'trakt-external-user-id';

  let externalUserId = '';
  let connectedUserId = '';
  let hideWatched = false;
  let watchedSlugs = new Set<string>();
  let status = '';
  let loading = false;

  const watchedUrl = (userId: string) =>
    `${apiUrl}/api/trakt/watched?externalUserId=${encodeURIComponent(userId)}`;

  const readStoredSlugs = () => {
    try {
      const value = window.localStorage.getItem(watchedSlugsKey);
      watchedSlugs = new Set(value ? JSON.parse(value) as string[] : []);
    } catch {
      watchedSlugs = new Set();
    }
  };

  const storeSlugs = () => {
    window.localStorage.setItem(watchedSlugsKey, JSON.stringify([...watchedSlugs]));
  };

  const applyFilter = () => {
    const cards = document.querySelectorAll<HTMLElement>('[data-anime-filter-item][data-anime-slug]');
    cards.forEach((card) => {
      const slug = card.dataset.animeSlug ?? '';
      const shouldHide = hideWatched && watchedSlugs.has(slug);
      card.classList.toggle('hidden', shouldHide);
      card.dataset.watchedHidden = shouldHide ? 'true' : 'false';
    });
  };

  const loadWatched = async (userId: string) => {
    loading = true;
    status = '';
    try {
      const response = await fetch(watchedUrl(userId), {
        headers: { Accept: 'application/json' },
      });
      if (!response.ok) throw new Error(`watched:${response.status}`);
      const data = await response.json() as TraktWatchedResponse;
      watchedSlugs = new Set(data.slugs);
      storeSlugs();
      status = `${data.slugs.length} vistos sincronizados`;
    } catch {
      status = 'No se pudo cargar Trakt. Mantengo el filtro local.';
    } finally {
      loading = false;
      applyFilter();
    }
  };

  const connect = async () => {
    const userId = externalUserId.trim();
    if (!userId) return;
    connectedUserId = userId;
    window.localStorage.setItem(externalUserKey, userId);
    await loadWatched(userId);
  };

  const clear = () => {
    connectedUserId = '';
    externalUserId = '';
    hideWatched = false;
    watchedSlugs = new Set();
    status = '';
    window.localStorage.removeItem(externalUserKey);
    window.localStorage.removeItem(hideWatchedKey);
    window.localStorage.removeItem(watchedSlugsKey);
    applyFilter();
  };

  $: if (typeof window !== 'undefined') {
    window.localStorage.setItem(hideWatchedKey, hideWatched ? 'true' : 'false');
    applyFilter();
  }

  onMount(() => {
    connectedUserId = window.localStorage.getItem(externalUserKey) ?? '';
    externalUserId = connectedUserId;
    hideWatched = window.localStorage.getItem(hideWatchedKey) === 'true';
    readStoredSlugs();
    applyFilter();
    if (connectedUserId) void loadWatched(connectedUserId);
  });
</script>

<section class="mx-auto max-w-6xl px-4 pb-8" aria-labelledby="watched-filter-title">
  <div class="rounded-lg border border-surface-2 bg-surface-1 p-4">
    <div class="grid gap-3 lg:grid-cols-[1fr_auto] lg:items-end">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-accent-violet">Trakt</p>
        <h2 id="watched-filter-title" class="mt-1 text-xl font-semibold text-fg-primary">
          Ocultar anime ya vistos
        </h2>
      </div>

      <form class="grid gap-3 sm:grid-cols-[minmax(12rem,1fr)_auto_auto]" on:submit|preventDefault={connect}>
        <label class="sr-only" for={externalUserInputId}>ID de Trakt</label>
        <input
          id={externalUserInputId}
          bind:value={externalUserId}
          type="text"
          autocomplete="off"
          placeholder="ID de Trakt"
          class="h-10 rounded-md border border-surface-2 bg-surface-0 px-3 text-sm text-fg-primary placeholder:text-fg-muted outline-none transition-colors focus:border-accent-violet"
          data-trakt-user-input
        />
        <button
          type="submit"
          disabled={loading || externalUserId.trim().length === 0}
          class="inline-flex h-10 items-center justify-center rounded-md bg-accent-violet px-4 text-sm font-semibold text-surface-0 transition hover:bg-accent-pink disabled:cursor-not-allowed disabled:opacity-55"
        >
          {loading ? 'Conectando...' : 'Conectar'}
        </button>
        <button
          type="button"
          on:click={clear}
          class="inline-flex h-10 items-center justify-center rounded-md border border-surface-2 px-4 text-sm font-semibold text-fg-secondary transition-colors hover:border-accent-violet hover:text-fg-primary"
        >
          Limpiar
        </button>
      </form>
    </div>

    <div class="mt-3 flex flex-wrap items-center gap-3">
      <label class="inline-flex items-center gap-2 text-sm text-fg-secondary">
        <input
          bind:checked={hideWatched}
          type="checkbox"
          class="h-4 w-4 rounded border-surface-2 bg-surface-0 accent-accent-violet"
          data-hide-watched-toggle
        />
        <span>Ocultar vistos</span>
      </label>
      {#if connectedUserId}
        <span class="text-xs text-fg-muted">Cuenta: {connectedUserId}</span>
      {/if}
      {#if status}
        <span class="text-xs text-fg-muted" data-trakt-filter-status>{status}</span>
      {/if}
    </div>
  </div>
</section>
