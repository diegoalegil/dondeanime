# DondeAnime

> Saber cuándo sale y dónde ver cualquier anime en España y LatAm, en menos de 5 segundos.

---

## Qué es esto

DondeAnime es una web en español que responde a dos preguntas concretas para cualquier anime:

1. **¿Cuándo se estrena en mi país?**
2. **¿Dónde puedo verlo legalmente (con doblaje o subtítulos en español)?**

Combina datos de varias APIs públicas con verificación manual para los títulos más populares, y los presenta en páginas rápidas, limpias y optimizadas para SEO.

---

## Para quién

Personas hispanohablantes de 16-35 años que consumen anime de forma casual o regular, ven un trailer en redes sociales o reciben una recomendación, y necesitan respuesta inmediata sin registrarse en una plataforma extranjera ni rebuscar en foros.

---

## Qué problema resuelve

Hoy, buscar *"cuándo sale [anime] en España"* devuelve foros desactualizados, blogs con info parcial y resultados en inglés. No existe una web rápida, en español, pensada para responder esta pregunta concreta. Ese es el hueco.

---

## Stack técnico

- **Backend:** Java 21 + Spring Boot 3
- **Base de datos:** PostgreSQL 16
- **Frontend:** Astro 4 (estático, SEO-first)
- **Hosting backend + BD:** Hetzner VPS
- **Hosting frontend:** Vercel (free tier)
- **CDN / DNS:** Cloudflare
- **Fuentes de datos:** AniList API (GraphQL) + TMDb API

Decisiones técnicas justificadas en `docs/arquitectura.md`.

---

## Estado actual

`v0.0.1` — Pre-arranque.

- [x] Dominio registrado
- [x] Brief y plan documentados
- [ ] Validación de APIs con script de prueba
- [ ] Esqueleto backend Spring Boot
- [ ] Esqueleto frontend Astro
- [ ] Primera ingesta de datos
- [ ] Primera página pública desplegada

Roadmap completo en `docs/roadmap.md`.

---

## Estructura del repo

```
.
├── README.md              ← Estás aquí
├── .gitignore
├── .env.example           ← Copiar a .env y rellenar
├── docs/
│   ├── arquitectura.md    ← Decisiones técnicas
│   ├── apis.md            ← Endpoints y ejemplos de cada API
│   └── roadmap.md         ← Plan 90 días semana a semana
├── backend/               ← Spring Boot (pendiente)
├── frontend/              ← Astro (pendiente)
└── scripts/
    └── test-apis.js       ← Validación inicial de las 3 APIs
```

---

## Arranque rápido

### Requisitos previos

- Node.js 20+
- Java 21
- PostgreSQL 16
- Cuenta gratis en [TMDb](https://www.themoviedb.org/signup) para obtener clave de API

### Primer paso (validar APIs antes de programar nada)

```bash
# Clonar
git clone git@github.com:[tu-usuario]/dondeanime.git
cd dondeanime

# Copiar variables de entorno
cp .env.example .env
# Editar .env con tu clave de TMDb

# Validar que las APIs responden
cd scripts
npm install node-fetch
node test-apis.js
```

Si el script devuelve datos reales de las 3 APIs, todo está listo para empezar a programar el backend.

---

## Filosofía del proyecto

1. **Velocidad > completitud.** Mejor una web rápida con 1.000 títulos bien presentados que una web lenta con 30.000 títulos.
2. **SEO-first.** Cada decisión técnica considera el impacto en posicionamiento orgánico.
3. **Datos verificados en el top.** Los 500 títulos más buscados se enriquecen a mano. El resto, automático.
4. **Sin humo.** Sin cookies innecesarias, sin newsletters spam, sin pop-ups intrusivos.

---

## Licencia

Privado. Todos los derechos reservados.

