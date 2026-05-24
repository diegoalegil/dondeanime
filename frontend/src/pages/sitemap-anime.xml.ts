import type { APIRoute } from 'astro';
import { animeSitemapPaths, renderUrlSet, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = async () => sitemapResponse(renderUrlSet(await animeSitemapPaths()));
