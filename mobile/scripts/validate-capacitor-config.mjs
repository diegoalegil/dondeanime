import { access, readFile } from 'node:fs/promises';
import path from 'node:path';

const root = new URL('..', import.meta.url);
const configPath = new URL('capacitor.config.json', root);
const packagePath = new URL('package.json', root);

const fail = (message) => {
  console.error(message);
  process.exitCode = 1;
};

const readJson = async (url) => JSON.parse(await readFile(url, 'utf8'));

const [config, pkg] = await Promise.all([
  readJson(configPath),
  readJson(packagePath),
]);

if (!/^com\.dondeanime\.[a-z0-9]+$/.test(config.appId ?? '')) {
  fail('capacitor.config.json appId debe usar com.dondeanime.<id>');
}

if (config.appName !== 'DondeAnime') {
  fail('capacitor.config.json appName debe ser DondeAnime');
}

if (config.webDir !== '../frontend/dist') {
  fail('capacitor.config.json webDir debe apuntar a ../frontend/dist');
}

if (config.server?.url) {
  fail('No uses server.url en el scaffold inicial: debe empaquetar webDir.');
}

if (pkg.dependencies?.['@capacitor/core'] !== '8.3.4') {
  fail('package.json debe fijar @capacitor/core 8.3.4');
}

if (pkg.dependencies?.['@capacitor/app'] !== '8.1.0') {
  fail('package.json debe fijar @capacitor/app 8.1.0');
}

if (pkg.devDependencies?.['@capacitor/cli'] !== '8.3.4') {
  fail('package.json debe fijar @capacitor/cli 8.3.4');
}

const webDir = path.resolve(new URL('.', root).pathname, config.webDir);
const expectedFrontend = path.resolve(new URL('../frontend/dist', root).pathname);
if (webDir !== expectedFrontend) {
  fail('webDir resuelve fuera de frontend/dist');
}

await access(packagePath);
await access(configPath);
