import { existsSync, mkdirSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';

const __dirname = dirname(fileURLToPath(import.meta.url));
const rootDir = join(__dirname, '..');
const distDir = join(rootDir, 'dist');
const reportPath = join(rootDir, 'lighthouse-report.json');
const tmpDir = join(rootDir, '.tmp', 'lighthouse');
const lighthouseBin = join(rootDir, 'node_modules', 'lighthouse', 'cli', 'index.js');
const port = Number(process.env.LIGHTHOUSE_PORT ?? 4322);
const baseUrl = `http://127.0.0.1:${port}`;
const categories = ['performance', 'accessibility', 'best-practices', 'seo'];
const thresholds = {
  performance: Number(process.env.LIGHTHOUSE_MIN_PERFORMANCE_SCORE ?? 80),
  accessibility: Number(process.env.LIGHTHOUSE_MIN_ACCESSIBILITY_SCORE ?? 90),
  'best-practices': Number(process.env.LIGHTHOUSE_MIN_BEST_PRACTICES_SCORE ?? 80),
  seo: Number(process.env.LIGHTHOUSE_MIN_SEO_SCORE ?? 80),
};
const lighthouseConcurrency = Number(process.env.LIGHTHOUSE_CONCURRENCY ?? 2);
const lighthouseTimeoutMs = Number(process.env.LIGHTHOUSE_TIMEOUT_MS ?? 120_000);
const chromeFlags = [
  '--headless=new',
  '--no-sandbox',
  '--disable-extensions',
  '--disable-gpu',
  '--disable-dev-shm-usage',
].join(' ');

const templatePatterns = [
  ['home', /^\/$/],
  ['anime-detail', /^\/anime\/[^/]+$/],
  ['anime-country', /^\/anime\/[^/]+\/[^/]+$/],
  ['country-hub', /^\/pais\/[^/]+$/],
  ['platform-hub', /^\/plataforma\/[^/]+$/],
  ['platform-country', /^\/plataforma\/[^/]+\/[^/]+$/],
  ['genre-hub', /^\/genero\/[^/]+$/],
  ['season', /^\/temporada\/\d{4}\/(winter|spring|summer|fall)$/],
  ['best-year', /^\/mejores\/\d{4}$/],
  ['genre-platform', /^\/anime\/[^/]+\/en\/[^/]+$/],
  ['release-week', /^\/estrenos\/proxima-semana$/],
  ['release-month', /^\/estrenos\/proximo-mes$/],
  ['blog-index', /^\/blog$/],
  ['blog-article', /^\/blog\/[^/]+$/],
  ['legal-affiliates', /^\/legal\/afiliados$/],
  ['legal-privacy', /^\/legal\/privacidad$/],
];

const commandName = (name) => process.platform === 'win32' ? `${name}.cmd` : name;

const htmlFileToRoute = (fullPath) => {
  const path = relative(distDir, fullPath).split(sep).join('/');
  if (path === 'index.html') return '/';
  if (path.endsWith('/index.html')) return `/${path.slice(0, -'/index.html'.length)}`;
  if (path.endsWith('.html')) return `/${path.slice(0, -'.html'.length)}`;
  return null;
};

const findHtmlRoutes = (dir, acc = []) => {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const fullPath = join(dir, entry.name);
    if (entry.isDirectory()) {
      findHtmlRoutes(fullPath, acc);
      continue;
    }

    if (!entry.name.endsWith('.html')) continue;

    const route = htmlFileToRoute(fullPath);
    if (route) acc.push(route);
  }

  return acc.sort((a, b) => a.length - b.length || a.localeCompare(b));
};

const selectTemplateRoutes = (routes) => {
  const selected = [];
  const missing = [];

  for (const [template, pattern] of templatePatterns) {
    const route = routes.find((candidate) => pattern.test(candidate));
    if (route) {
      selected.push({ template, route, url: `${baseUrl}${route}` });
    } else {
      missing.push(template);
    }
  }

  if (missing.length > 0) {
    throw new Error(`No generated routes found for templates: ${missing.join(', ')}`);
  }

  return selected;
};

const waitForServer = async (url, attempts = 90) => {
  for (let i = 0; i < attempts; i += 1) {
    try {
      const response = await fetch(url);
      if (response.ok) return;
    } catch {
      // Preview server is still booting.
    }

    await new Promise((resolve) => setTimeout(resolve, 1000));
  }

  throw new Error(`Preview server did not become ready at ${url}`);
};

const runCommand = (command, args, options = {}) => new Promise((resolve, reject) => {
  const child = spawn(command, args, {
    cwd: rootDir,
    env: process.env,
    shell: false,
    stdio: options.stdio ?? 'inherit',
  });
  let timedOut = false;
  const timeoutId = options.timeoutMs
    ? setTimeout(() => {
        timedOut = true;
        child.kill('SIGKILL');
      }, options.timeoutMs)
    : null;

  child.on('error', reject);
  child.on('exit', (code) => {
    if (timeoutId) clearTimeout(timeoutId);
    if (timedOut) {
      reject(new Error(`${command} ${args.join(' ')} timed out after ${options.timeoutMs}ms`));
      return;
    }

    if (code === 0 || options.allowFailure) {
      resolve(code);
      return;
    }

    reject(new Error(`${command} ${args.join(' ')} failed with exit code ${code}`));
  });
});

const runWithConcurrency = async (items, concurrency, worker) => {
  const results = new Array(items.length);
  let nextIndex = 0;
  const workerCount = Math.max(1, Math.min(concurrency, items.length));

  await Promise.all(Array.from({ length: workerCount }, async () => {
    while (nextIndex < items.length) {
      const index = nextIndex;
      nextIndex += 1;
      results[index] = await worker(items[index]);
    }
  }));

  return results;
};

const startPreview = () => {
  const args = ['run', 'preview', '--', '--host', '127.0.0.1', '--port', String(port)];

  if (process.platform === 'win32') {
    return spawn(`${commandName('npm')} ${args.join(' ')}`, {
      cwd: rootDir,
      env: process.env,
      shell: true,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
  }

  return spawn('npm', args, {
    cwd: rootDir,
    env: process.env,
    shell: false,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
};

const stopPreview = async (preview) => {
  if (preview.exitCode !== null) return;

  if (process.platform === 'win32') {
    await runCommand('taskkill', ['/pid', String(preview.pid), '/T', '/F'], {
      allowFailure: true,
      stdio: 'ignore',
    });
    return;
  }

  preview.kill('SIGTERM');
  await new Promise((resolve) => preview.once('exit', resolve));
};

const runLighthouse = async ({ template, route, url }) => {
  const outputPath = join(tmpDir, `${template}.json`);

  await runCommand(process.execPath, [
    lighthouseBin,
    url,
    '--quiet',
    '--preset=desktop',
    `--only-categories=${categories.join(',')}`,
    '--output=json',
    `--output-path=${outputPath}`,
    '--throttling-method=provided',
    '--max-wait-for-load=45000',
    `--chrome-flags=${chromeFlags}`,
  ], { allowFailure: true, stdio: 'pipe', timeoutMs: lighthouseTimeoutMs });

  const raw = JSON.parse(readFileSync(outputPath, 'utf8'));
  const scores = Object.fromEntries(
    categories.map((category) => [
      category,
      Math.round((raw.categories[category]?.score ?? 0) * 100),
    ]),
  );

  const failures = Object.entries(scores)
    .filter(([category, score]) => score < thresholds[category])
    .map(([category, score]) => `${category}=${score} < ${thresholds[category]}`);

  return {
    template,
    route,
    url,
    scores,
    passed: failures.length === 0,
    failures,
  };
};

if (!existsSync(distDir)) {
  throw new Error('Missing frontend/dist. Run npm run build before npm run audit:lighthouse.');
}

rmSync(tmpDir, { force: true, recursive: true });
mkdirSync(tmpDir, { recursive: true });

const routes = findHtmlRoutes(distDir)
  .filter((route) => !route.startsWith('/admin'));
const targets = selectTemplateRoutes(routes);
const preview = startPreview();

try {
  await waitForServer(baseUrl);
  const audits = await runWithConcurrency(targets, lighthouseConcurrency, async (target) => {
    console.log(`Lighthouse ${target.template}: ${target.route}`);
    return runLighthouse(target);
  });

  const report = {
    generatedAt: new Date().toISOString(),
    thresholds,
    categories,
    audits,
  };

  writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);

  const failing = audits.filter((audit) => !audit.passed);
  if (failing.length > 0) {
    console.error('Lighthouse thresholds failed:');
    for (const audit of failing) {
      console.error(`- ${audit.template} ${audit.route}: ${audit.failures.join(', ')}`);
    }
    process.exit(1);
  }

  console.log(`Lighthouse audit passed for ${audits.length} templates.`);
} finally {
  await stopPreview(preview);
}
