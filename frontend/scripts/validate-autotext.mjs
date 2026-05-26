import { AutoTextGenerator, countWords } from '../src/lib/autoTextGenerator.mjs';

const genres = ['Action', 'Drama', 'Comedy', 'Adventure', 'Supernatural', 'Fantasy', 'Psychological'];
const platforms = ['Crunchyroll', 'Netflix', 'Amazon Prime Video', 'HBO Max', 'Disney+'];
const generator = new AutoTextGenerator();
const seen = new Set();
const errors = [];

for (const genreName of genres) {
  for (const platformName of platforms) {
    const { markdown } = generator.generateGenrePlatform({
      animeCount: 12,
      countries: 'España, Mexico, Argentina, Colombia y Chile',
      genreName,
      platformName,
      topTitles: [
        `${genreName} ejemplo 1`,
        `${platformName} ejemplo 2`,
        `${genreName} ejemplo 3`,
      ],
    });

    const words = countWords(markdown);
    const normalized = markdown.replace(/\s+/g, ' ').trim();

    if (words < 150 || words > 500) {
      errors.push(`${genreName} + ${platformName}: ${words} words`);
    }

    if (seen.has(normalized)) {
      errors.push(`${genreName} + ${platformName}: duplicate text`);
    }
    seen.add(normalized);
  }
}

if (errors.length > 0) {
  console.error(errors.join('\n'));
  process.exit(1);
}

console.log(`AutoTextGenerator valido: ${seen.size} textos unicos entre 150 y 500 palabras.`);
