#!/usr/bin/env node
/**
 * Genera los PNG de PWA desde los SVG fuente (que se mantienen):
 * iOS no soporta SVG en apple-touch-icon y algunos instaladores PWA
 * exigen PNG en manifest.icons.
 *
 * Uso: node scripts/generate-icons.mjs
 */
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const PWA_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '../public/pwa');

const TARGETS = [
  { dir: 'icons', source: 'icon-1024.svg', output: 'apple-touch-icon.png', width: 180, height: 180 },
  { dir: 'icons', source: 'icon-1024.svg', output: 'icon-192.png', width: 192, height: 192 },
  { dir: 'icons', source: 'icon-1024.svg', output: 'icon-512.png', width: 512, height: 512 },
  { dir: 'icons', source: 'maskable-icon.svg', output: 'maskable-icon-512.png', width: 512, height: 512 },
  { dir: 'screenshots', source: 'home-wide.svg', output: 'home-wide.png', width: 1280, height: 720 },
  { dir: 'screenshots', source: 'detail-narrow.svg', output: 'detail-narrow.png', width: 390, height: 844 },
];

for (const { dir, source, output, width, height } of TARGETS) {
  await sharp(resolve(PWA_DIR, dir, source), { density: 300 })
    .resize(width, height)
    .png()
    .toFile(resolve(PWA_DIR, dir, output));
  console.log(`${dir}/${output} (${width}x${height}) <- ${source}`);
}
