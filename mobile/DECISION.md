# Decision movil

Sprint 23 empaqueta DondeAnime como app instalable sin abrir todavia cuentas de
Apple, Google ni publicar en stores.

## Opciones

### Capacitor

Ventajas:

- Reutiliza el frontend Astro/PWA actual y su SEO web no se toca.
- Permite deep links, push y cache offline con APIs nativas cuando hagan falta.
- El coste inicial es bajo: wrapper, config y validacion en CI sin SDK movil.

Riesgos:

- La UX nativa depende de que el frontend siga siendo responsive y ligero.
- Android/iOS reales necesitaran SDK local o runners especificos mas adelante.

### React Native

Ventajas:

- UX nativa real desde el primer dia.
- Mejor control de navegacion, gestos y componentes por plataforma.

Riesgos:

- Duplica producto: habria que reimplementar pantallas, estado y tracking.
- Aumenta mantenimiento justo cuando el proyecto todavia esta cerrando sprints
  de backend, SEO y monetizacion.

## Decision

Usamos Capacitor para Sprint 23.

La app movil queda como wrapper minimo sobre el frontend existente. No se crean
proyectos `ios/` ni `android/` en este PR para evitar SDKs locales, certificados
o cuentas de store. CI valida que el scaffold y la config sean coherentes.

## Dependencias nuevas

- `@capacitor/core`: runtime del wrapper nativo que cargara DondeAnime dentro de
  WebView y habilitara plugins nativos posteriores.
- `@capacitor/cli`: herramienta de desarrollo para validar/sincronizar el
  wrapper; queda en `devDependencies` y no se ejecuta contra SDKs en CI.
