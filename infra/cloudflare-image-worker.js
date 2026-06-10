/**
 * Cloudflare Worker — proxy + caché de portadas de AniList.
 *
 * Por qué: la web sirve las portadas directas de s4.anilist.co, y su CDN
 * estrangula cuando una página pide muchas a la vez (la home pide ~90), así
 * que la mitad fallan. Este Worker las descarga una vez, las cachea 30 días
 * en el edge de Cloudflare (gratis) y las sirve sin ese límite. Coste 0 en el
 * plan gratuito (100k req/día), sin tocar el VPS ni meter dependencias de pago.
 *
 * Despliegue (una vez):
 *   1. Cloudflare dashboard → Workers & Pages → Create → Worker.
 *   2. Pega este archivo, Deploy. Te da una URL tipo
 *      https://dondeanime-img.TU_SUBDOMINIO.workers.dev/
 *      (o asígnale una ruta en tu dominio, p.ej. img.dondeanime.com).
 *   3. En Vercel (proyecto dondeanime → Settings → Environment Variables):
 *      PUBLIC_IMAGE_PROXY = https://dondeanime-img.TU_SUBDOMINIO.workers.dev/
 *      (Production). Redeploy sin cache. Listo: las portadas pasan por el Worker.
 *
 * Para desactivar: borra la env var y redeploy (vuelve a directo).
 */
export default {
  async fetch(request, ctx) {
    const reqUrl = new URL(request.url);
    const target = reqUrl.searchParams.get('url');
    if (!target) {
      return new Response('missing url', { status: 400 });
    }

    let parsed;
    try {
      parsed = new URL(target);
    } catch {
      return new Response('bad url', { status: 400 });
    }

    // Allowlist estricta: este Worker solo proxea portadas de AniList,
    // nunca una URL arbitraria (evita convertirlo en un open proxy).
    if (parsed.protocol !== 'https:' || parsed.hostname !== 's4.anilist.co') {
      return new Response('forbidden host', { status: 403 });
    }

    const cache = caches.default;
    const cacheKey = new Request(reqUrl.toString(), request);
    const cached = await cache.match(cacheKey);
    if (cached) {
      return cached;
    }

    const upstream = await fetch(parsed.toString(), {
      cf: { cacheTtl: 2592000, cacheEverything: true },
    });
    if (!upstream.ok) {
      return new Response('upstream error', { status: 502 });
    }

    const response = new Response(upstream.body, upstream);
    response.headers.set('Cache-Control', 'public, max-age=2592000, immutable');
    response.headers.set('Access-Control-Allow-Origin', '*');
    response.headers.delete('set-cookie');

    ctx.waitUntil(cache.put(cacheKey, response.clone()));
    return response;
  },
};
