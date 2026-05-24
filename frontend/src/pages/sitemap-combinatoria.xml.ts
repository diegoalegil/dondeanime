import type { APIRoute } from 'astro';
import { combinationSitemapPaths, renderUrlSet, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = async () => sitemapResponse(renderUrlSet(await combinationSitemapPaths()));
