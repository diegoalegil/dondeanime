import { expect, test } from '@playwright/test';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const criticalFiles = [
  'src/pages/index.astro',
  'src/pages/anime/[slug].astro',
  'src/pages/anime/[slug]/[pais].astro',
  'src/pages/pais/[pais].astro',
  'src/components/layout/Header.astro',
  'src/components/layout/Footer.astro',
  'src/components/ui/SearchBox.astro',
  'src/components/ui/ThemeToggle.astro',
  'src/components/ui/CountrySwitcher.astro',
  'src/components/anime/AnimeHero.astro',
  'src/components/anime/AnimeSynopsis.astro',
  'src/components/anime/MetaList.astro',
  'src/components/providers/ProviderRow.astro',
  'src/components/providers/ProvidersByCountry.astro',
  'src/components/providers/NotAvailableInCountry.astro',
  'src/components/alerts/AvailabilityAlertForm.astro',
];

const forbiddenSpanishLiterals = [
  'Encuentra dónde ver',
  'Catálogo actualizado',
  'Tendencia ahora',
  'Ver temporada',
  'Lo mejor en',
  'Explora por país',
  'Plataformas destacadas',
  'Mejores por año',
  'Preguntas frecuentes',
  'Catálogo completo',
  'Buscar anime',
  'Sin resultados',
  'Cambiar tema',
  'Dónde verlo',
  'No tenemos información',
  'Ver más',
  'No disponible',
  'Sinopsis',
  'Pendiente de traducción',
  'Detalles',
  'Formato',
  'Estado',
  'Episodios',
  'Estreno',
  'Puntuación',
  'Popularidad',
  'Aviso de disponibilidad',
  'Recibe un correo',
  'Acepto la',
  'política de privacidad',
  'Crear alerta',
  'Anime en streaming en',
  'Plataformas disponibles',
];

test('critical pages keep Spanish UI strings in the i18n dictionary', () => {
  const dictionarySource = readFileSync(path.join(rootDir, 'src/i18n/es.json'), 'utf8');

  for (const phrase of forbiddenSpanishLiterals) {
    expect(dictionarySource, `missing extracted phrase in es.json: ${phrase}`).toContain(phrase);
  }

  for (const relativeFile of criticalFiles) {
    const source = readFileSync(path.join(rootDir, relativeFile), 'utf8');
    for (const phrase of forbiddenSpanishLiterals) {
      expect(source, `${relativeFile} still contains "${phrase}"`).not.toContain(phrase);
    }
  }
});
