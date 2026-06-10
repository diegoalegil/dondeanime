<script lang="ts">
  import type { ChatSearchResponse } from '@/lib/api';

  export let apiUrl = '';
  export let animePathPrefix = '/anime';
  // Textos resueltos en el .astro con t(): el cliente no tiene i18n.
  // Los defaults en español cubren usos sin props (y los specs actuales).
  export let labels = {
    eyebrow: 'Buscador semantico',
    title: 'Pide una recomendacion concreta',
    example: 'Ejemplo: algo corto, oscuro y disponible en Crunchyroll.',
    questionLabel: 'Pregunta para el buscador de anime',
    placeholder: 'Quiero un anime corto, oscuro y con buen ritmo',
    countryLabel: 'Pais preferido',
    submit: 'Buscar recomendaciones',
    loading: 'Buscando...',
    error: 'No se pudo buscar ahora. Prueba otra consulta en unos segundos.',
  };
  export let countries = [
    { code: '', label: 'Cualquier pais' },
    { code: 'ES', label: 'España' },
    { code: 'MX', label: 'México' },
    { code: 'AR', label: 'Argentina' },
    { code: 'CO', label: 'Colombia' },
    { code: 'CL', label: 'Chile' },
  ];

  type RequestState = 'idle' | 'loading' | 'success' | 'error';

  const inputId = 'anime-chat-question';
  const countryId = 'anime-chat-country';

  let question = '';
  let countryCode = '';
  let state: RequestState = 'idle';
  let response: ChatSearchResponse | null = null;
  let errorMessage = '';

  const endpoint = () => `${apiUrl}/api/chat/search`;
  const animeHref = (slug: string) => `${animePathPrefix.replace(/\/$/, '')}/${slug}`;
  const titleFor = (anime: ChatSearchResponse['recommendations'][number]['anime']) =>
    anime.titleEnglish || anime.titleRomaji;

  const search = async () => {
    const value = question.trim();
    if (value.length < 3 || state === 'loading') return;

    state = 'loading';
    errorMessage = '';

    try {
      const res = await fetch(endpoint(), {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          question: value,
          countryCode: countryCode || null,
        }),
      });

      if (!res.ok) {
        throw new Error(`chat-search:${res.status}`);
      }

      response = (await res.json()) as ChatSearchResponse;
      state = 'success';
    } catch {
      response = null;
      errorMessage = labels.error;
      state = 'error';
    }
  };
</script>

<section class="mx-auto max-w-6xl px-4 pb-12" aria-labelledby="anime-chat-title">
  <div class="rounded-lg border border-surface-2 bg-surface-1 p-4 md:p-5">
    <div class="grid gap-4 lg:grid-cols-[0.85fr_1.15fr] lg:items-start">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-accent-violet">{labels.eyebrow}</p>
        <h2 id="anime-chat-title" class="mt-2 text-2xl font-semibold text-fg-primary">
          {labels.title}
        </h2>
        <p class="mt-2 max-w-xl text-sm leading-6 text-fg-secondary">
          {labels.example}
        </p>
      </div>

      <form class="grid gap-3" on:submit|preventDefault={search}>
        <label class="sr-only" for={inputId}>{labels.questionLabel}</label>
        <textarea
          id={inputId}
          bind:value={question}
          rows="3"
          maxlength="500"
          placeholder={labels.placeholder}
          class="min-h-24 w-full resize-none rounded-md border border-surface-2 bg-surface-0 px-4 py-3 text-sm leading-6 text-fg-primary placeholder:text-fg-muted outline-none transition-colors focus:border-accent-violet"
          data-chat-question
        />

        <div class="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto]">
          <label class="sr-only" for={countryId}>{labels.countryLabel}</label>
          <select
            id={countryId}
            bind:value={countryCode}
            class="h-11 rounded-md border border-surface-2 bg-surface-0 px-3 text-sm text-fg-primary outline-none transition-colors focus:border-accent-violet"
            data-chat-country
          >
            {#each countries as country}
              <option value={country.code}>{country.label}</option>
            {/each}
          </select>
          <button
            type="submit"
            disabled={state === 'loading' || question.trim().length < 3}
            class="inline-flex h-11 items-center justify-center gap-2 rounded-md bg-accent-violet px-5 text-sm font-semibold text-surface-0 transition hover:bg-accent-pink disabled:cursor-not-allowed disabled:opacity-55"
            data-chat-submit
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="h-4 w-4"
              aria-hidden="true"
            >
              <path d="m22 2-7 20-4-9-9-4Z" />
              <path d="M22 2 11 13" />
            </svg>
            {state === 'loading' ? labels.loading : labels.submit}
          </button>
        </div>
      </form>
    </div>

    {#if state === 'error'}
      <p class="mt-4 rounded-md border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-fg-secondary" role="alert">
        {errorMessage}
      </p>
    {:else if state === 'success' && response}
      <div class="mt-5 border-t border-surface-2 pt-5" data-chat-results>
        <p class="text-sm leading-6 text-fg-secondary">{response.answer}</p>

        {#if response.recommendations.length > 0}
          <div class="mt-4 grid gap-3 md:grid-cols-2">
            {#each response.recommendations as recommendation}
              <a
                href={animeHref(recommendation.anime.slug)}
                class="grid min-h-32 grid-cols-[4.5rem_minmax(0,1fr)] gap-3 rounded-lg border border-surface-2 bg-surface-0 p-3 transition-colors hover:border-accent-violet/60 focus:border-accent-violet focus:outline-none"
                data-chat-recommendation
              >
                <img
                  src={recommendation.anime.coverImage}
                  alt=""
                  loading="lazy"
                  class="h-28 w-18 rounded-md object-cover ring-1 ring-surface-2"
                />
                <span class="min-w-0">
                  <span class="block truncate text-sm font-semibold text-fg-primary">
                    {titleFor(recommendation.anime)}
                  </span>
                  <span class="mt-1 block text-xs text-fg-muted">
                    {recommendation.anime.year ?? '---'} - {recommendation.anime.format}
                  </span>
                  <span class="mt-2 line-clamp-3 block text-sm leading-6 text-fg-secondary">
                    {recommendation.explanation}
                  </span>
                </span>
              </a>
            {/each}
          </div>
        {/if}
      </div>
    {/if}
  </div>
</section>
