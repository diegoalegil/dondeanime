import type { APIRoute } from 'astro';
import { renderUrlSet, sitemapResponse, spanishSitemapPaths } from '@/lib/sitemaps';

export const GET: APIRoute = async () => sitemapResponse(renderUrlSet(await spanishSitemapPaths()));
