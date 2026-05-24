import type { APIRoute } from 'astro';
import { countrySitemapPaths, renderUrlSet, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = async () => sitemapResponse(renderUrlSet(await countrySitemapPaths()));
