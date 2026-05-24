import type { APIRoute } from 'astro';
import { platformSitemapPaths, renderUrlSet, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = async () => sitemapResponse(renderUrlSet(await platformSitemapPaths()));
