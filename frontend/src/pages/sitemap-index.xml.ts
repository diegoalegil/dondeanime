import type { APIRoute } from 'astro';
import { renderSitemapIndex, sitemapResponse } from '@/lib/sitemaps';

export const GET: APIRoute = () => sitemapResponse(renderSitemapIndex());
