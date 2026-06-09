# Integracion Trakt.tv

OAuth permite conectar una cuenta Trakt.tv sin crear todavia un sistema de cuentas propio en DondeAnime. La integracion queda apagada por defecto y solo se activa con credenciales completas.

## Variables

Local:

```properties
TRAKT_ENABLED=false
TRAKT_API_BASE=https://api.trakt.tv
TRAKT_OAUTH_BASE=https://trakt.tv
TRAKT_CLIENT_ID=
TRAKT_CLIENT_SECRET=
TRAKT_REDIRECT_URI=http://localhost:8080/api/trakt/oauth/callback
TRAKT_TOKEN_ENCRYPTION_SECRET=
```

Produccion:

```properties
TRAKT_REDIRECT_URI=https://api.dondeanime.com/api/trakt/oauth/callback
```

`TRAKT_ENABLED=false` es el valor por defecto. `TRAKT_CLIENT_SECRET` firma el parametro `state` del flujo OAuth. `TRAKT_TOKEN_ENCRYPTION_SECRET` cifra `access_token` y `refresh_token` antes de guardar. Si falta cualquiera de `TRAKT_CLIENT_ID`, `TRAKT_CLIENT_SECRET`, `TRAKT_REDIRECT_URI` o `TRAKT_TOKEN_ENCRYPTION_SECRET`, los endpoints devuelven `503`.

## Endpoints

- `GET /api/trakt/oauth/start`: redirige a Trakt con `response_type=code`, `client_id`, `redirect_uri` y `state` firmado.
- `GET /api/trakt/oauth/callback`: valida `state`, intercambia `code` por tokens, identifica el usuario y guarda tokens cifrados.
- `GET /api/trakt/watched?externalUserId=...`: devuelve slugs vistos para filtros frontend.
- `POST /api/trakt/sync`: importa vistos y ratings para una cuenta conectada. Usa tokens cifrados guardados y refresca si han caducado.
- `GET /api/anime/{slug}/similar?watched=...`: acepta slugs vistos opcionales, excluye esos anime y prioriza generos/plataformas que aparecen en el historial.

La respuesta del callback no expone tokens:

```json
{
  "connected": true,
  "provider": "trakt",
  "externalUserId": "diego",
  "accessTokenStored": true,
  "refreshTokenStored": true,
  "expiresInSeconds": 7200,
  "scope": "public",
  "message": "Cuenta Trakt conectada. Los tokens se guardan cifrados."
}
```

## Decision de seguridad

No se guarda ningun token plano. `access_token` y `refresh_token` se cifran con AES-GCM y un secreto operativo separado. La columna mantiene el sufijo `ciphertext` para que cualquier revision detecte rapido si alguien intenta persistir plaintext.

## Modelo persistente

El modelo persistente usa:

- `external_account`: cuenta externa por `provider + external_user_id`, email opcional, scopes normalizados y columnas `access_token_ciphertext` / `refresh_token_ciphertext`.
- `user_watched_anime`: anime visto por cuenta externa, deduplicado por `external_account_id + anime_slug + source`.

Los campos de token estan nombrados como `ciphertext` a proposito. No se debe guardar un token plano en esas columnas. El servicio de cuenta externa normaliza provider, email, scopes, slug y source antes de persistir.

## Sync

El sync importa:

- `GET /sync/watched/shows`
- `GET /sync/ratings/shows`

El match contra catalogo local es deliberadamente simple y auditable: titulo normalizado + anio. No toca `AnimeMatchingService`, porque ese servicio es solo para AniList/TMDb.

Ejemplo de request:

```json
{
  "externalUserId": "diego"
}
```

La respuesta solo devuelve contadores y hasta 20 no matcheados. No devuelve tokens. Si el access token esta caducado, el backend refresca con el refresh token cifrado antes de llamar a Trakt.

Cada sync guarda un `trakt_sync_event` agregado con contadores anonimos. El dashboard admin usa esa tabla para mostrar cuentas conectadas, syncs 7/30 dias y matches fallidos sin exponer email, tokens ni ids internos.
