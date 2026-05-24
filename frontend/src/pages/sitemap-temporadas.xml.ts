import type { APIRoute } from 'astro';
import { renderUrlSet, seasonSitemapPaths, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = async () => sitemapResponse(renderUrlSet(await seasonSitemapPaths()));
