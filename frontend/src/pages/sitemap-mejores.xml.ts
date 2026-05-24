import type { APIRoute } from 'astro';
import { bestYearSitemapPaths, renderUrlSet, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = () => sitemapResponse(renderUrlSet(bestYearSitemapPaths()));
