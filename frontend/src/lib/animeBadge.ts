/**
 * Clase de color para la insignia de nota (averageScore 0-100). Compartida por
 * AnimeCard.astro (build) y personalLibrary.ts (cliente), así que se mantiene
 * PURA (sin imports): personalLibrary corre en el navegador y no puede arrastrar
 * módulos de servidor (p. ej. el de i18n con AsyncLocalStorage).
 */
export function scoreBadgeClass(score: number): string {
  if (score >= 80) return 'bg-success/15 text-success';
  if (score >= 60) return 'bg-warning/15 text-warning';
  return 'bg-danger/15 text-danger';
}
