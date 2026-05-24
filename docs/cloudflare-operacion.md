# Operación Cloudflare

Guía para dos tareas pendientes de mejora continua:

- Email Routing: `contacto@dondeanime.com` hacia el correo personal de Diego.
- Cache Rules: caché de páginas públicas sin tocar la API ni romper Vercel/Caddy.

No ejecutar nada a ciegas. Cloudflare cambia pantallas con frecuencia; si el dashboard muestra opciones distintas, seguir la documentación oficial enlazada al final.

---

## 1. Email Routing

Objetivo: recibir correos en `contacto@dondeanime.com` sin contratar buzón propio.

### Regla importante

Email Routing solo sirve para recibir y reenviar correo. No convierte Gmail en emisor desde `contacto@dondeanime.com`.

Para enviar emails transaccionales desde DondeAnime, Sprint 2 usa Resend y sus propios SPF/DKIM. No mezclar ambos conceptos.

### Pasos

1. Entrar en Cloudflare Dashboard.
2. Seleccionar la zona `dondeanime.com`.
3. Ir a Email → Email Routing.
4. Activar Email Routing.
5. Añadir destination address: Gmail personal de Diego.
6. Confirmar el correo desde Gmail.
7. Crear custom address:

```text
contacto@dondeanime.com → Gmail personal
```

8. Dejar que Cloudflare cree los DNS necesarios.
9. Revisar en DNS que no queden MX antiguos de Namecheap u otro proveedor.

Cloudflare necesita MX y TXT para Email Routing. Si hay registros MX previos, el dashboard suele pedir reemplazarlos. Hacerlo solo cuando Diego confirme que no usa otro buzón real en ese dominio.

### Prueba

Desde una cuenta externa:

```text
Para: contacto@dondeanime.com
Asunto: prueba routing
Texto: test
```

Debe llegar al Gmail personal. Probar también desde otro proveedor distinto si Gmail lo manda a spam.

### Si no llega

- Revisar Email → Email Routing → Routing status.
- Revisar DNS: los MX deben ser los que Cloudflare indique.
- Revisar que el destination address esté confirmado.
- Revisar spam/promociones en Gmail.

---

## 2. Cache Rules

Objetivo: bajar coste/latencia del frontend estático sin cachear datos vivos de la API.

### Regla importante

En este proyecto, `api.dondeanime.com` debe seguir con proxy gris si Caddy usa Let's Encrypt HTTP-01. No activar proxy naranja en la API salvo que se migre Caddy a DNS-01 con token de Cloudflare.

Para el frontend, Vercel maneja SSL. En la configuración actual también se mantiene proxy gris. Si Diego decide activar proxy naranja para el frontend, aplicar estas reglas.

### Reglas recomendadas

Cloudflare ya recomienda Cache Rules como configuración moderna. Usar Cache Rules en vez de crear Page Rules nuevas.

#### Regla 1: anime pages

```text
Nombre: Cache anime pages
Expression:
(http.host eq "dondeanime.com" or http.host eq "www.dondeanime.com")
and starts_with(http.request.uri.path, "/anime/")

Settings:
- Eligible for cache: true
- Edge TTL: 1 day
- Browser TTL: Respect existing headers
```

#### Regla 2: sitemap

```text
Nombre: Cache sitemap
Expression:
(http.host eq "dondeanime.com" or http.host eq "www.dondeanime.com")
and starts_with(http.request.uri.path, "/sitemap")

Settings:
- Eligible for cache: true
- Edge TTL: 1 hour
- Browser TTL: Respect existing headers
```

#### Regla 3: no cache API

Solo aplica si algún día `api.dondeanime.com` pasa a proxy naranja.

```text
Nombre: Bypass API
Expression:
http.host eq "api.dondeanime.com"
and starts_with(http.request.uri.path, "/api/")

Settings:
- Cache eligibility: Bypass cache
```

### Orden

Poner `Bypass API` por encima de reglas genéricas si existe. Las reglas específicas de API siempre deben ganar a cualquier caché amplia.

### Prueba

Con proxy naranja activo en frontend:

```bash
curl -I https://dondeanime.com/anime/attack-on-titan
curl -I https://dondeanime.com/sitemap-index.xml
```

Buscar headers de Cloudflare:

```text
cf-cache-status: HIT
```

La primera request puede dar `MISS`; repetir la misma URL. Para la API, si algún día se proxea:

```bash
curl -I https://api.dondeanime.com/api/anime
```

Debe devolver `cf-cache-status: DYNAMIC` o bypass equivalente, nunca HIT.

### Purga

Si el scheduler actualiza providers y Vercel rebuildea, Vercel sirve HTML nuevo. Si Cloudflare está delante con proxy naranja, puede quedar HTML viejo hasta el TTL.

Opciones:

- Mantener TTL corto: 1 día anime, 1 hora sitemap.
- Purgar manualmente por URL tras cambios críticos.
- Futuro: automatizar purge de Cloudflare al final del webhook de rebuild, pero eso requiere token API y no entra aquí.

---

## Referencias oficiales

- Cloudflare Email Routing: https://developers.cloudflare.com/email-routing/get-started/enable-email-routing/
- Cloudflare Cache Rules settings: https://developers.cloudflare.com/cache/how-to/cache-rules/settings/
- Cloudflare Edge/Browser Cache TTL: https://developers.cloudflare.com/cache/how-to/edge-browser-cache-ttl/
