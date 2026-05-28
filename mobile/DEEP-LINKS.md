# Deep links

Rutas soportadas por Sprint 23.2:

- `dondeanime://anime/{slug}` -> `/anime/{slug}`
- `dondeanime://buscar` -> `/buscar`

Tambien se aceptan URLs canonicas del sitio:

- `https://dondeanime.com/anime/{slug}`
- `https://www.dondeanime.com/buscar`

## Integracion Capacitor

`src/capacitorDeepLinks.mjs` registra `appUrlOpen` desde `@capacitor/app` y
delega el parseo a `src/deepLinks.mjs`. La funcion recibe un `navigate(path)`
para que el frontend decida como cambiar de ruta sin acoplar el parser a Astro.

Los proyectos nativos `ios/` y `android/` todavia no se generan en este sprint.
Cuando existan, habra que declarar el scheme `dondeanime` en Info.plist y en el
intent-filter Android correspondiente.
