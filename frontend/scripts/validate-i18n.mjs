import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const dictionaries = ['es', 'en'];

async function readDictionary(locale) {
  const filePath = path.join(rootDir, 'src', 'i18n', `${locale}.json`);
  const raw = await readFile(filePath, 'utf8');
  return JSON.parse(raw);
}

function placeholderSet(value) {
  return new Set([...value.matchAll(/\{[a-zA-Z0-9_]+\}/g)].map((match) => match[0]));
}

function sorted(values) {
  return [...values].sort();
}

function sameSet(left, right) {
  return left.size === right.size && [...left].every((value) => right.has(value));
}

const [base, ...translations] = await Promise.all(dictionaries.map(readDictionary));
const [baseLocale, ...translationLocales] = dictionaries;
const baseKeys = new Set(Object.keys(base));
const errors = [];

for (const key of sorted(baseKeys)) {
  if (typeof base[key] !== 'string' || base[key].trim().length === 0) {
    errors.push(`${baseLocale}.${key} must be a non-empty string.`);
  }
}

for (const [index, dictionary] of translations.entries()) {
  const locale = translationLocales[index];
  const keys = new Set(Object.keys(dictionary));
  const missing = sorted([...baseKeys].filter((key) => !keys.has(key)));
  const extra = sorted([...keys].filter((key) => !baseKeys.has(key)));

  for (const key of missing) {
    errors.push(`${locale}.${key} is missing.`);
  }

  for (const key of extra) {
    errors.push(`${locale}.${key} does not exist in ${baseLocale}.`);
  }

  for (const key of sorted(baseKeys)) {
    const value = dictionary[key];
    if (typeof value !== 'string' || value.trim().length === 0) {
      errors.push(`${locale}.${key} must be a non-empty string.`);
      continue;
    }

    const basePlaceholders = placeholderSet(base[key]);
    const translationPlaceholders = placeholderSet(value);
    if (!sameSet(basePlaceholders, translationPlaceholders)) {
      errors.push(
        `${locale}.${key} placeholders differ: expected ${sorted(basePlaceholders).join(', ') || 'none'}, got ${sorted(translationPlaceholders).join(', ') || 'none'}.`,
      );
    }
  }
}

if (errors.length > 0) {
  console.error(`i18n validation failed with ${errors.length} error(s):`);
  for (const error of errors) {
    console.error(`- ${error}`);
  }
  process.exit(1);
}

console.log(`i18n validation passed for ${dictionaries.join(', ')}.`);
