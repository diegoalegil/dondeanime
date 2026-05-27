import { readdir, readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const distDir = fileURLToPath(new URL('../dist', import.meta.url));
const scriptPattern = /<script\b(?=[^>]*type=["']application\/ld\+json["'])[^>]*>([\s\S]*?)<\/script>/gi;
const schemaContext = new Set(['https://schema.org', 'http://schema.org']);
const knownTypes = new Set([
  'BlogPosting',
  'BreadcrumbList',
  'FAQPage',
  'ItemList',
  'Organization',
  'Review',
  'Service',
  'TVSeries',
  'WebSite',
]);

const errors = [];

const isRecord = (value) => value !== null && typeof value === 'object' && !Array.isArray(value);
const asArray = (value) => Array.isArray(value) ? value : [value];
const rel = (file) => path.relative(distDir, file);

async function listHtmlFiles(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = await Promise.all(entries.map(async (entry) => {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) return listHtmlFiles(fullPath);
    return entry.isFile() && entry.name.endsWith('.html') ? [fullPath] : [];
  }));
  return files.flat();
}

function typesFor(node) {
  if (!isRecord(node)) return [];
  const type = node['@type'];
  if (Array.isArray(type)) return type.filter((item) => typeof item === 'string');
  return typeof type === 'string' ? [type] : [];
}

function readJsonLd(html, file) {
  const blocks = [];
  for (const match of html.matchAll(scriptPattern)) {
    try {
      const parsed = JSON.parse(match[1].trim());
      blocks.push(...asArray(parsed));
    } catch (error) {
      errors.push(`${rel(file)}: JSON-LD inválido (${error.message})`);
    }
  }
  return blocks;
}

function requireString(node, key, file, type) {
  if (typeof node[key] !== 'string' || node[key].trim() === '') {
    errors.push(`${rel(file)}: ${type}.${key} debe ser un string no vacío`);
  }
}

function requireArray(node, key, file, type, { allowEmpty = false } = {}) {
  if (!Array.isArray(node[key]) || (!allowEmpty && node[key].length === 0)) {
    errors.push(`${rel(file)}: ${type}.${key} debe ser un array${allowEmpty ? '' : ' no vacío'}`);
  }
}

function requireRating(node, key, file, type) {
  const rating = node[key];
  if (!isRecord(rating)) {
    errors.push(`${rel(file)}: ${type}.${key} debe existir`);
    return;
  }

  const value = Number(rating.ratingValue);
  if (!Number.isFinite(value) || value < 0 || value > 10) {
    errors.push(`${rel(file)}: ${type}.${key}.ratingValue debe estar entre 0 y 10`);
  }
}

function validateNode(node, file) {
  if (!isRecord(node)) {
    errors.push(`${rel(file)}: cada bloque JSON-LD debe ser un objeto`);
    return;
  }

  if (!schemaContext.has(node['@context'])) {
    errors.push(`${rel(file)}: @context debe ser https://schema.org`);
  }

  const types = typesFor(node);
  if (types.length === 0) {
    errors.push(`${rel(file)}: falta @type en JSON-LD`);
    return;
  }

  for (const type of types) {
    if (!knownTypes.has(type)) {
      errors.push(`${rel(file)}: @type desconocido en bloque raíz (${type})`);
      continue;
    }

    if (type === 'BreadcrumbList') requireArray(node, 'itemListElement', file, type);
    if (type === 'BlogPosting') {
      requireString(node, 'headline', file, type);
      requireString(node, 'description', file, type);
      requireString(node, 'datePublished', file, type);
      requireString(node, 'dateModified', file, type);
      if (!isRecord(node.mainEntityOfPage)) errors.push(`${rel(file)}: BlogPosting.mainEntityOfPage debe existir`);
      if (!isRecord(node.author)) errors.push(`${rel(file)}: BlogPosting.author debe existir`);
      if (!isRecord(node.publisher)) errors.push(`${rel(file)}: BlogPosting.publisher debe existir`);
    }
    if (type === 'ItemList') requireArray(node, 'itemListElement', file, type, { allowEmpty: true });
    if (type === 'WebSite') {
      requireString(node, 'name', file, type);
      requireString(node, 'url', file, type);
      if (!isRecord(node.potentialAction)) {
        errors.push(`${rel(file)}: WebSite.potentialAction debe existir`);
      }
    }
    if (type === 'TVSeries') {
      requireString(node, 'name', file, type);
      requireString(node, 'url', file, type);
      if (node.aggregateRating !== undefined) requireRating(node, 'aggregateRating', file, type);
    }
    if (type === 'Review') {
      if (!isRecord(node.itemReviewed)) errors.push(`${rel(file)}: Review.itemReviewed debe existir`);
      if (!isRecord(node.author)) errors.push(`${rel(file)}: Review.author debe existir`);
      requireRating(node, 'reviewRating', file, type);
    }
    if (type === 'Service') {
      requireString(node, 'name', file, type);
      requireString(node, 'url', file, type);
      requireString(node, 'serviceType', file, type);
      requireString(node, 'description', file, type);
      if (!isRecord(node.provider)) errors.push(`${rel(file)}: Service.provider debe existir`);
    }
    if (type === 'FAQPage') {
      requireArray(node, 'mainEntity', file, type);
      for (const question of node.mainEntity ?? []) {
        if (!isRecord(question) || question['@type'] !== 'Question') {
          errors.push(`${rel(file)}: FAQPage.mainEntity debe contener Question`);
          continue;
        }
        requireString(question, 'name', file, 'Question');
        if (!isRecord(question.acceptedAnswer) || question.acceptedAnswer['@type'] !== 'Answer') {
          errors.push(`${rel(file)}: Question.acceptedAnswer debe ser Answer`);
        } else {
          requireString(question.acceptedAnswer, 'text', file, 'Answer');
        }
      }
    }
    if (type === 'Organization') {
      requireString(node, 'name', file, type);
      requireString(node, 'url', file, type);
      requireArray(node, 'availableLanguage', file, type);
      for (const locale of ['es', 'en']) {
        if (!node.availableLanguage?.includes(locale)) {
          errors.push(`${rel(file)}: Organization.availableLanguage debe incluir ${locale}`);
        }
      }
      if (!isRecord(node.logo) || typeof node.logo.url !== 'string') {
        errors.push(`${rel(file)}: Organization.logo.url debe existir`);
      }
      requireArray(node, 'sameAs', file, type);
    }
  }
}

function assertPageHas(file, blocks, type) {
  if (!blocks.some((block) => typesFor(block).includes(type))) {
    errors.push(`${rel(file)}: falta schema ${type}`);
  }
}

function isTopLevelAnimePage(file) {
  const parts = rel(file).split(path.sep);
  return parts.length === 2 && parts[0] === 'anime' && parts[1].endsWith('.html');
}

const htmlFiles = await listHtmlFiles(distDir);
const blocksByFile = new Map();

for (const file of htmlFiles) {
  const html = await readFile(file, 'utf8');
  const blocks = readJsonLd(html, file);
  blocksByFile.set(file, blocks);

  if (blocks.length === 0) {
    errors.push(`${rel(file)}: no contiene JSON-LD`);
  }

  for (const block of blocks) validateNode(block, file);

  const hasRatedTvSeries = blocks.some((block) =>
    typesFor(block).includes('TVSeries') && block.aggregateRating !== undefined
  );
  const hasReview = blocks.some((block) => typesFor(block).includes('Review'));
  if (hasRatedTvSeries && !hasReview) {
    errors.push(`${rel(file)}: TVSeries con rating necesita Review asociado`);
  }
}

const homeFile = path.join(distDir, 'index.html');
const homeBlocks = blocksByFile.get(homeFile) ?? [];
assertPageHas(homeFile, homeBlocks, 'WebSite');
assertPageHas(homeFile, homeBlocks, 'FAQPage');
assertPageHas(homeFile, homeBlocks, 'ItemList');
assertPageHas(homeFile, homeBlocks, 'Organization');

const animeFiles = htmlFiles.filter(isTopLevelAnimePage);
if (animeFiles.length === 0) {
  errors.push('dist/anime: no se encontraron fichas de anime generadas');
}

for (const file of animeFiles) {
  const blocks = blocksByFile.get(file) ?? [];
  assertPageHas(file, blocks, 'TVSeries');
  assertPageHas(file, blocks, 'Organization');
}

if (errors.length > 0) {
  console.error(errors.join('\n'));
  process.exit(1);
}

console.log(`JSON-LD válido en ${htmlFiles.length} páginas HTML.`);
