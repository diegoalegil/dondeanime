import type { APIRoute } from 'astro';
import { curatedListSitemapPaths, renderUrlSet, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = async () => sitemapResponse(renderUrlSet(await curatedListSitemapPaths()));
