import type { APIRoute } from 'astro';

// ads.txt para AdSense. Se genera en build a partir de PUBLIC_ADSENSE_CLIENT_ID:
// al definir la variable en Vercel y redeployar, la línea aparece sola. El id
// del script lleva prefijo "ca-" (ca-pub-XXXX) pero ads.txt lo exige sin él.
const rawClientId = (import.meta.env.PUBLIC_ADSENSE_CLIENT_ID ?? '').trim();
const publisherId = rawClientId.replace(/^ca-/, '');

export const GET: APIRoute = () => {
  const body = publisherId
    ? `google.com, ${publisherId}, DIRECT, f08c47fec0942fa0\n`
    : '# Sin cuenta AdSense todavia. Define PUBLIC_ADSENSE_CLIENT_ID en el build para publicar la linea.\n';
  return new Response(body, {
    headers: { 'Content-Type': 'text/plain; charset=utf-8' },
  });
};
