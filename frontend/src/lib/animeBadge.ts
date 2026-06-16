/**
 * Clase de color para la insignia de nota (averageScore 0-100). Compartida por
 * AnimeCard.astro (build) y personalLibrary.ts (cliente), así que se mantiene
 * PURA (sin imports): personalLibrary corre en el navegador y no puede arrastrar
 * módulos de servidor (p. ej. el de i18n con AsyncLocalStorage).
 */
export function scoreBadgeClass(score: number): string {
  // Fondo oscuro opaco (no un tinte translucido del color): sobre portadas
  // claras un bg-success/15 daba contraste ~1.2:1 y la nota no se leia. Con
  // bg-surface-0/90 el numero queda legible (>=4.5:1) sobre cualquier portada.
  if (score >= 80) return 'bg-surface-0/90 text-success';
  if (score >= 60) return 'bg-surface-0/90 text-warning';
  return 'bg-surface-0/90 text-danger';
}
