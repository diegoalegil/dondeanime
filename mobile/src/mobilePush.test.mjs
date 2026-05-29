import assert from 'node:assert/strict';
import test from 'node:test';

import { registerMobilePush } from './mobilePush.mjs';

test('registra token movil con pais preferido', async () => {
  const requests = [];
  const result = await registerMobilePush({
    apiBaseUrl: 'https://api.dondeanime.com',
    countryIso: 'es',
    platform: 'ios',
    pushProvider: {
      requestPermissions: async () => ({ receive: 'granted' }),
      register: async () => ({ value: ' native-token ' }),
    },
    fetchImpl: async (url, options) => {
      requests.push({ url, options });
      return {
        ok: true,
        json: async () => ({
          platform: 'IOS',
          countryIso: 'ES',
          alertsOnly: true,
        }),
      };
    },
  });

  assert.equal(result.registered, true);
  assert.equal(requests[0].url, 'https://api.dondeanime.com/api/mobile/push/register');
  assert.deepEqual(JSON.parse(requests[0].options.body), {
    platform: 'ios',
    countryIso: 'es',
    deviceToken: 'native-token',
  });
  assert.equal(result.response.alertsOnly, true);
});

test('no registra si el usuario deniega permisos', async () => {
  const result = await registerMobilePush({
    apiBaseUrl: 'https://api.dondeanime.com',
    countryIso: 'ES',
    platform: 'android',
    pushProvider: {
      requestPermissions: async () => ({ receive: 'denied' }),
      register: async () => {
        throw new Error('no debe registrar');
      },
    },
    fetchImpl: async () => {
      throw new Error('no debe llamar backend');
    },
  });

  assert.deepEqual(result, {
    registered: false,
    reason: 'permission_denied',
  });
});

test('rechaza plataformas no soportadas', async () => {
  await assert.rejects(
    () => registerMobilePush({
      apiBaseUrl: 'https://api.dondeanime.com',
      countryIso: 'ES',
      platform: 'web',
      pushProvider: {},
      fetchImpl: async () => ({ ok: true }),
    }),
    /platform debe ser ios o android/,
  );
});
