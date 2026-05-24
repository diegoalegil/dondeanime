export const BEST_YEAR_START = 2010;
export const BEST_YEAR_END = 2026;

export const BEST_ANIME_YEARS = Array.from(
  { length: BEST_YEAR_END - BEST_YEAR_START + 1 },
  (_, index) => BEST_YEAR_START + index,
);

export const BEST_ANIME_YEARS_DESC = [...BEST_ANIME_YEARS].reverse();
