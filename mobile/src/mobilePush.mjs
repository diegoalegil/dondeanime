const SUPPORTED_PLATFORMS = new Set(['ios', 'android']);

export async function registerMobilePush({
  apiBaseUrl,
  countryIso,
  platform,
  pushProvider,
  fetchImpl = globalThis.fetch,
}) {
  if (!SUPPORTED_PLATFORMS.has(platform)) {
    throw new Error('platform debe ser ios o android');
  }
  if (!countryIso || !/^[A-Za-z]{2}$/.test(countryIso)) {
    throw new Error('countryIso debe tener 2 letras');
  }
  if (!pushProvider) {
    throw new Error('pushProvider es obligatorio');
  }
  if (typeof fetchImpl !== 'function') {
    throw new Error('fetchImpl debe ser una funcion');
  }

  const permission = await pushProvider.requestPermissions();
  if (permission.receive !== 'granted') {
    return {
      registered: false,
      reason: 'permission_denied',
    };
  }

  const token = await pushProvider.register();
  const deviceToken = normalizeToken(token);
  const response = await fetchImpl(`${apiBaseUrl}/api/mobile/push/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      platform,
      countryIso,
      deviceToken,
    }),
  });

  if (!response.ok) {
    throw new Error(`mobile_push_register_failed:${response.status}`);
  }

  return {
    registered: true,
    response: await response.json(),
  };
}

function normalizeToken(token) {
  const value = typeof token === 'string' ? token : token?.value;
  if (!value || typeof value !== 'string' || value.trim() === '') {
    throw new Error('deviceToken vacio');
  }
  return value.trim();
}
