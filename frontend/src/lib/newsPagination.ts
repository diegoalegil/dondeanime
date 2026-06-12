// Paginación del índice de noticias. El API capa /api/news a 100 ítems:
// el índice pagina sobre esos 100 más recientes; los artículos antiguos
// siguen accesibles por URL directa y sitemap (getStaticPaths usa /slugs,
// que no tiene tope).
export const NEWS_PAGE_SIZE = 24;

export const newsPageCount = (totalItems: number): number =>
  Math.max(1, Math.ceil(totalItems / NEWS_PAGE_SIZE));

export const newsPageSlice = <T>(items: T[], page: number): T[] =>
  items.slice((page - 1) * NEWS_PAGE_SIZE, page * NEWS_PAGE_SIZE);
