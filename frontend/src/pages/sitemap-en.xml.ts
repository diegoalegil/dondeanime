import type { APIRoute } from 'astro';
import { englishSitemapPaths, renderUrlSet, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = async () => sitemapResponse(renderUrlSet(await englishSitemapPaths()));
