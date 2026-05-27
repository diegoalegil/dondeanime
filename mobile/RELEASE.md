# Release movil

Guia para generar builds internos de DondeAnime mobile. No publica nada en App
Store ni Play Store.

## Preflight

1. Confirmar que el PR final de Sprint 23 esta mergeado en `main`.
2. Usar una maquina local con SDKs instalados:
   - Android Studio + Android SDK para Android.
   - Xcode + cuenta Apple Developer para iOS.
3. Revisar variables publicas antes de construir:
   - `PUBLIC_API_URL=https://api.dondeanime.com`
   - `PUBLIC_DATA_API_URL=https://api.dondeanime.com`
   - `PUBLIC_SITE_URL=https://dondeanime.com`
   - `PUBLIC_VAPID_PUBLIC_KEY` si se prueba push web.
   - `PUBLIC_STRIPE_PUBLISHABLE_KEY` si se prueba Premium.
4. No incluir secretos privados en `mobile/`, `frontend/dist`, Xcode ni Gradle.
5. Ejecutar validacion base:

```bash
cd frontend
npm ci
PUBLIC_API_URL=https://api.dondeanime.com \
PUBLIC_DATA_API_URL=https://api.dondeanime.com \
PUBLIC_SITE_URL=https://dondeanime.com \
PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_test_REEMPLAZAR \
npm run build

cd ../mobile
npm ci
npm run lint
npm test
npm run build
```

## Build interno Android

Primera preparacion local:

```bash
cd mobile
npx cap add android
npx cap sync android
npx cap open android
```

Build debug para instalar en un dispositivo propio:

```bash
cd mobile/android
./gradlew assembleDebug
```

Build release interno:

```bash
cd mobile/android
./gradlew bundleRelease
```

Antes de compartir un release:

- Configurar firma en Gradle con un keystore local no commiteado.
- Cambiar version code/name desde el proyecto Android.
- Probar instalacion limpia y actualizacion sobre una version anterior.
- Verificar deep links `dondeanime://anime/{slug}` y `dondeanime://buscar`.
- Verificar que push solo registra alertas solicitadas.

## Build interno iOS

Primera preparacion local:

```bash
cd mobile
npx cap add ios
npx cap sync ios
npx cap open ios
```

En Xcode:

1. Seleccionar el team de Apple Developer.
2. Configurar bundle id `com.dondeanime.app`.
3. Revisar signing y provisioning profile.
4. Ejecutar en simulador y en un iPhone real.
5. Crear archive con Product > Archive.
6. Distribuir solo como build interno o TestFlight cuando Diego lo apruebe.

Antes de compartir un build:

- Probar instalacion limpia y update.
- Abrir una ficha, cerrar red y comprobar `/offline`.
- Probar deep links desde Safari/Notas.
- Probar permisos de push y denegacion de permisos.

## Checklist de privacidad

- La app no pide login propio en Sprint 23.
- Device tokens se envian solo a `POST /api/mobile/push/register`.
- El backend no devuelve device tokens en la respuesta publica.
- `alertsOnly=true` evita usar tokens para newsletters o campanas.
- Offline cache guarda solo datos publicos de fichas sanitizadas.
- No se cachean `/admin`, `/api`, emails, passwords, tokens ni secretos.
- No se anaden SDKs de tracking nativo en Sprint 23.
- Si se activa Plausible web, mantener la politica de privacidad actualizada.
- Si se activa Stripe en movil, usar solo claves publicas en cliente.

## Capturas

Preparar capturas reales, sin datos privados ni panel admin:

- Home con catalogo visible.
- Ficha de anime con providers por pais.
- Busqueda.
- Pantalla offline.
- Flujo de permiso push en estado concedido y denegado.
- Premium solo si el flujo ya esta aprobado para movil.

Antes de subir a stores, verificar en App Store Connect y Play Console los
tamanos y cantidades vigentes. Esas reglas cambian y no deben quedar congeladas
en el repo como verdad permanente.

## Iconos

- Fuente actual: `frontend/public/pwa/icons/`.
- Generar assets nativos desde un master cuadrado de alta resolucion.
- Revisar icono normal y maskable/adaptive icon.
- No usar fondos transparentes si la plataforma exige fondo opaco.
- Validar que el icono se reconoce en modo claro y oscuro.

## Permisos

Permisos esperados en Sprint 23:

- Notificaciones push: solo para alertas solicitadas.
- Enlaces externos/deep links: navegacion a rutas publicas.
- Red: acceso a `https://api.dondeanime.com` y assets publicos.

No pedir permisos de ubicacion, contactos, calendario, camara, microfono ni
fotos. Si una plataforma los detecta en el manifest nativo, hay que quitarlos
antes de subir el build.

## Textos legales

Textos minimos para ficha de store o build interno:

- Nombre: DondeAnime.
- Categoria: entretenimiento / utilidades de streaming.
- Descripcion corta: descubre donde ver anime por pais y plataforma.
- Politica de privacidad: `https://dondeanime.com/legal/privacidad`.
- Soporte: `https://dondeanime.com`.
- Aviso afiliados: DondeAnime puede mostrar enlaces afiliados sin coste extra
  para el usuario.
- Aviso push: las notificaciones se usan solo para alertas solicitadas.

## No publicar desde el equipo

el flujo de desarrollo puede preparar codigo, docs y PRs. No debe:

- Entrar en cuentas Apple, Google, Vercel, Cloudflare ni VPS.
- Crear certificados, provisioning profiles o keystores reales.
- Subir builds a App Store Connect o Play Console.
- Publicar en stores.

Diego hace la publicacion manual cuando revise el Sprint 23 completo.
