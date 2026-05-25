# Redesign audit

Estado: auditoria visual y UX. Sin implementacion.

## Paginas revisadas

| Pagina | Ruta de referencia | Estado visual | Riesgo principal |
| --- | --- | --- | --- |
| Home | `/` | Correcta para SEO, con secciones claras y densidad razonable. | La jerarquia inicial depende mucho del texto; falta un primer bloque mas utilitario para buscar rapido. |
| Ficha anime | `/anime/{slug}` | Buena base: hero, providers, sinopsis y metadata. | La disponibilidad por pais compite visualmente con el hero; conviene priorizar accion. |
| Listado pais | `/pais/espana` | Navegacion clara y buen uso de cards. | Puede quedarse largo si crece el catalogo; haran falta filtros visibles arriba. |
| Plataforma | `/plataforma/crunchyroll` | Simple y escaneable. | Falta diferenciar "hub de plataforma" de "listado de resultados"; ahora se sienten parecidos. |

## Problemas detectados

- El buscador existe, pero no domina la primera pantalla. Para un agregador, buscar "anime + pais" deberia ser la accion mas obvia.
- Las cards usan bien portada y metadata, pero la informacion de disponibilidad aparece tarde en varios recorridos.
- Las paginas SEO de pais, plataforma y combinatoria comparten estructura casi identica. Es mantenible, pero visualmente puede parecer contenido duplicado.
- El color actual tiene personalidad, aunque la paleta morado/rosa domina demasiado y reduce sensacion de producto operativo.
- En mobile, los bloques de enlaces por pais/plataforma funcionan, pero no hay controles compactos para cambiar contexto rapido.

## Direccion recomendada

- Mantener dark mode como identidad base, pero rebajar gradientes y usar un acento secundario frio para estados, filtros y disponibilidad.
- Convertir la home en una superficie de descubrimiento: buscador prominente, temporada activa, proximos estrenos y plataformas populares.
- En ficha anime, hacer que "Donde verlo" sea el bloque principal despues del hero, con selector de pais mas visible.
- En listados, subir filtros y ordenacion a una barra compacta: pais, plataforma, genero, temporada y disponibilidad.
- Reservar cards grandes para anime. Usar filas compactas para providers, paises y combinaciones SEO.

## Mockups de baja fidelidad

Home:

```text
[Buscar anime...] [Pais v] [Plataforma v]
Temporada actual      Proximos estrenos
Anime grid            Compact list
Tendencia ahora
Anime grid
```

Ficha anime:

```text
Hero compacto con portada + titulo + metadata
Donde verlo [Pais v]
[Provider row] [Provider row]
Sinopsis
Datos tecnicos
```

Listado:

```text
Titulo SEO corto
[Genero v] [Pais v] [Plataforma v] [Orden v]
Resultados en grid
Paginacion / enlaces internos
```

## Esfuerzo estimado

- Refresh visual sin cambiar arquitectura: 2-3 dias.
- Reordenar home y ficha anime: 3-4 dias.
- Filtros compactos compartidos para listados: 4-6 dias.
- QA mobile + Playwright visual basico: 1-2 dias.

## No hacer todavia

- No redisenar contenido editorial hasta que haya textos reales.
- No introducir libreria de componentes nueva.
- No cambiar rutas SEO existentes.
- No mover datos a cliente si la pagina puede seguir siendo estatica.
