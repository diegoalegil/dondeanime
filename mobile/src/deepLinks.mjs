const CUSTOM_SCHEME = 'dondeanime:';
const SITE_HOSTS = new Set(['dondeanime.com', 'www.dondeanime.com']);
const SLUG_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

export function parseDeepLink(value) {
  if (typeof value !== 'string' || value.trim() === '') {
    return null;
  }

  let url;
  try {
    url = new URL(value);
  } catch {
    return null;
  }

  if (url.protocol === CUSTOM_SCHEME) {
    return parseCustomScheme(url);
  }

  if (url.protocol === 'https:' && SITE_HOSTS.has(url.hostname)) {
    return parsePath(url.pathname);
  }

  return null;
}

export function createDeepLinkHandler({ navigate }) {
  if (typeof navigate !== 'function') {
    throw new TypeError('navigate debe ser una funcion');
  }

  return ({ url }) => {
    const target = parseDeepLink(url);
    if (!target) {
      return null;
    }
    navigate(target.path);
    return target;
  };
}

function parseCustomScheme(url) {
  if (url.hostname === 'buscar' && (url.pathname === '' || url.pathname === '/')) {
    return searchTarget();
  }

  if (url.hostname === 'anime') {
    const slug = decodeURIComponent(url.pathname.replace(/^\/+/, ''));
    return animeTarget(slug);
  }

  return null;
}

function parsePath(pathname) {
  if (pathname === '/buscar' || pathname === '/buscar/') {
    return searchTarget();
  }

  const animeMatch = pathname.match(/^\/anime\/([^/?#]+)\/?$/);
  if (!animeMatch) {
    return null;
  }

  return animeTarget(decodeURIComponent(animeMatch[1]));
}

function animeTarget(slug) {
  if (!SLUG_PATTERN.test(slug)) {
    return null;
  }

  return {
    type: 'anime',
    slug,
    path: `/anime/${slug}`,
  };
}

function searchTarget() {
  return {
    type: 'search',
    path: '/buscar',
  };
}
