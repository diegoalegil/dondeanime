import type { APIRoute } from 'astro';
import { genreSitemapPaths, renderUrlSet, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = async () => sitemapResponse(renderUrlSet(await genreSitemapPaths()));
