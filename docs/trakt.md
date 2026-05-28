# Integracion Trakt.tv

Sprint 22 prepara OAuth para conectar una cuenta Trakt.tv sin crear todavia un sistema de cuentas propio en DondeAnime.

## Variables

Local:

```properties
TRAKT_API_BASE=https://api.trakt.tv
TRAKT_OAUTH_BASE=https://trakt.tv
TRAKT_CLIENT_ID=
TRAKT_CLIENT_SECRET=
TRAKT_REDIRECT_URI=http://localhost:8080/api/trakt/oauth/callback
```

Produccion:

```properties
TRAKT_REDIRECT_URI=https://api.dondeanime.com/api/trakt/oauth/callback
```

`TRAKT_CLIENT_SECRET` tambien firma el parametro `state` del flujo OAuth. Si falta cualquiera de `TRAKT_CLIENT_ID`, `TRAKT_CLIENT_SECRET` o `TRAKT_REDIRECT_URI`, los endpoints devuelven `503`.

## Endpoints

- `GET /api/trakt/oauth/start`: redirige a Trakt con `response_type=code`, `client_id`, `redirect_uri` y `state` firmado.
- `GET /api/trakt/oauth/callback`: valida `state`, intercambia `code` por tokens y devuelve una respuesta segura.
- `GET /api/trakt/watched?externalUserId=...`: devuelve slugs vistos para filtros frontend.
- `POST /api/trakt/sync`: importa vistos y ratings para una cuenta externa existente usando un access token temporal en el body.
- `GET /api/anime/{slug}/similar?watched=...`: acepta slugs vistos opcionales, excluye esos anime y prioriza generos/plataformas que aparecen en el historial.

La respuesta del callback no expone tokens:

```json
{
  "connected": true,
  "provider": "trakt",
  "accessTokenStored": false,
  "refreshTokenStored": false,
  "expiresInSeconds": 7200,
  "scope": "public",
  "message": "Cuenta Trakt conectada temporalmente. Los tokens no se guardan hasta aprobar el almacenamiento cifrado."
}
```

## Decision de seguridad

En PR 22.1 no se guarda `refresh_token`. El backlog permite documentar esta limitacion si no hay cifrado aprobado todavia. El almacenamiento queda para PR 22.2, junto con `ExternalAccount` y una decision explicita sobre cifrado de tokens.

## Modelo persistente

PR 22.2 agrega:

- `external_account`: cuenta externa por `provider + external_user_id`, email opcional, scopes normalizados y columnas `access_token_ciphertext` / `refresh_token_ciphertext`.
- `user_watched_anime`: anime visto por cuenta externa, deduplicado por `external_account_id + anime_slug + source`.

Los campos de token estan nombrados como `ciphertext` a proposito. No se debe guardar un token plano en esas columnas. El servicio de cuenta externa normaliza provider, email, scopes, slug y source antes de persistir.

## Sync

PR 22.3 importa:

- `GET /sync/watched/shows`
- `GET /sync/ratings/shows`

El match contra catalogo local es deliberadamente simple y auditable: titulo normalizado + anio. No toca `AnimeMatchingService`, porque ese servicio es solo para AniList/TMDb.

Ejemplo de request:

```json
{
  "externalUserId": "123456",
  "accessToken": "trakt_access_token_temporal"
}
```

La respuesta solo devuelve contadores y hasta 20 no matcheados. No devuelve tokens.

Cada sync guarda un `trakt_sync_event` agregado con contadores anonimos. El dashboard admin usa esa tabla para mostrar cuentas conectadas, syncs 7/30 dias y matches fallidos sin exponer email, tokens ni ids internos.
