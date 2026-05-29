# Push movil

Sprint 23.3 registra tokens moviles para alertas solicitadas.

## Flujo

1. `createCapacitorPushProvider()` pide permisos con
   `@capacitor/push-notifications`.
2. `registerMobilePush()` obtiene el token nativo del dispositivo.
3. El token se envia a `POST /api/mobile/push/register` con plataforma y pais.
4. Backend responde sin devolver el token.

## Alcance

- Solo se guarda `platform`, `deviceToken`, `countryIso` y flags operativos.
- `alertsOnly=true` queda fijado desde backend.
- No hay newsletters, campañas ni envios masivos desde este adaptador.
- El envio real APNs/FCM queda para un sprint posterior con proveedor aprobado.
