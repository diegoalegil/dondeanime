import type { APIRoute } from 'astro';
import { getAllAnime } from '@/lib/api';

export const GET: APIRoute = async () => {
  const anime = await getAllAnime();
  const index = anime.map((a) => ({
    slug: a.slug,
    en: a.titleEnglish,
    jp: a.titleRomaji,
    year: a.year,
    format: a.format,
    cover: a.coverImage,
  }));
  return new Response(JSON.stringify(index), {
    status: 200,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'public, max-age=3600',
    },
  });
};
