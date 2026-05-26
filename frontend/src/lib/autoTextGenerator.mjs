const GENRE_CONTEXT = {
  Action: 'peleas, ritmo alto y conflictos faciles de seguir',
  Adventure: 'viajes, descubrimiento y grupos que avanzan hacia un objetivo claro',
  Comedy: 'situaciones ligeras, personajes expresivos y capitulos faciles de encadenar',
  Drama: 'conflictos emocionales, relaciones tensas y decisiones con peso',
  Fantasy: 'mundos inventados, reglas propias y un tono mas escapista',
  Psychological: 'tension mental, dudas morales y giros que conviene ver con calma',
  Supernatural: 'poderes extraños, misterio y reglas que rompen lo cotidiano',
};

const TEMPLATES = [
  `## Anime de {genreName} en {platformName}

Esta pagina cruza dos preguntas muy concretas: que anime de {genreName} merece la pena revisar y cuales aparecen en {platformName} dentro de los paises hispanos que cubre DondeAnime. La seleccion no intenta sustituir una lista editorial cerrada; ordena el catalogo por popularidad y disponibilidad para que el usuario encuentre rapido opciones reales.

En {genreName}, suelen pesar {genreContext}. Por eso conviene mirar primero titulos con una premisa clara y suficiente traccion entre fans. {topSentence} Si el cruce esta vacio o corto, la pagina sigue siendo util como aviso de catalogo: permite detectar que esa plataforma todavia no tiene una presencia fuerte del genero en los datos sincronizados.

- Plataforma revisada: {platformName}
- Genero principal: {genreName}
- Resultados actuales: {animeCount}
- Paises considerados: {countries}

La disponibilidad puede cambiar por licencias, asi que el listado se regenera con el catalogo y evita prometer permanencia. Usa esta pagina como punto de partida: abre la ficha de cada anime para comprobar pais, proveedor y enlaces disponibles antes de decidir que ver.`,
  `## Guia rapida de {genreName} en {platformName}

Cuando alguien busca anime de {genreName} en {platformName}, normalmente no quiere una enciclopedia: quiere saber si hay opciones suficientes y cuales son las mas reconocibles. Esta pagina resume ese cruce con datos del catalogo sincronizado y prioriza los titulos con mas popularidad para reducir ruido.

El valor del filtro esta en mezclar intencion y disponibilidad. {genreName} suele funcionar bien para usuarios que buscan {genreContext}, pero cada plataforma reparte sus licencias de forma distinta entre España y Latinoamerica. {topSentence} Si solo aparecen pocos resultados, puede ser una señal de que conviene revisar otra plataforma o volver tras el siguiente refresco del catalogo.

En esta combinacion se revisan {animeCount} anime detectados en {platformName}. El orden no es una critica definitiva: es una forma practica de empezar por lo mas visible y luego bajar hacia opciones menos conocidas.

La ficha individual sigue siendo la fuente importante para confirmar paises, enlaces y proveedores. Las licencias cambian, pero la estructura de la pagina queda estable para SEO y para usuarios que llegan con una busqueda muy concreta.`,
  `## Que esperar de {genreName} en {platformName}

Este listado existe para una busqueda larga y directa: anime de {genreName} disponible en {platformName}. En vez de mezclar todos los generos o todas las plataformas, separa el catalogo en un cruce concreto que ayuda a comparar sin abrir veinte pestañas.

El genero {genreName} suele atraer por {genreContext}. En plataformas grandes, ese tipo de anime puede estar repartido entre series recientes, clasicos muy vistos y licencias que cambian por pais. {topSentence} Por eso el listado usa popularidad como primer orden, no como juicio absoluto.

Ahora mismo el cruce muestra {animeCount} resultados en los paises cubiertos: {countries}. Si el numero sube, la pagina gana profundidad; si baja, tambien deja una pista util sobre como se mueve el catalogo de {platformName}.

Antes de empezar una serie, entra en su ficha. Alli se ve el detalle por pais y proveedor, con enlaces cuando existen. Esta pagina solo hace el trabajo previo: reducir una busqueda amplia a una lista concreta, indexable y facil de revisar.`,
];

const hashText = (value) => {
  let hash = 0;
  for (const char of value) {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  }
  return hash;
};

const escapeHtml = (value) => String(value)
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;');

const applyVars = (template, vars) =>
  template.replace(/\{([a-zA-Z0-9_]+)\}/g, (_, key) => escapeHtml(vars[key] ?? ''));

export const countWords = (text) => {
  const matches = text
    .replace(/[#*`_\-[\]().,:;]/g, ' ')
    .match(/[A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ]+/g);
  return matches?.length ?? 0;
};

export const markdownToHtml = (markdown) => {
  const blocks = markdown.trim().split(/\n{2,}/);

  return blocks.map((block) => {
    if (block.startsWith('## ')) {
      return `<h2 class="text-2xl font-semibold text-fg-primary">${block.slice(3)}</h2>`;
    }

    if (block.startsWith('- ')) {
      const items = block.split('\n')
        .filter((line) => line.startsWith('- '))
        .map((line) => `<li>${line.slice(2)}</li>`)
        .join('');
      return `<ul class="grid gap-2 pl-5 text-sm leading-relaxed text-fg-secondary">${items}</ul>`;
    }

    return `<p class="text-sm leading-relaxed text-fg-secondary">${block.replace(/\n/g, ' ')}</p>`;
  }).join('\n');
};

export class AutoTextGenerator {
  generateGenrePlatform({ genreName, platformName, animeCount, topTitles, countries }) {
    const safeGenre = genreName || 'anime';
    const safePlatform = platformName || 'la plataforma';
    const titles = (topTitles ?? []).filter(Boolean).slice(0, 4);
    const template = TEMPLATES[hashText(`${safeGenre}:${safePlatform}`) % TEMPLATES.length];
    const topSentence = titles.length > 0
      ? `Entre los primeros titulos aparecen ${titles.join(', ')}, que sirven como referencia rapida para entender el tono del cruce.`
      : 'Cuando no hay titulos destacados, el texto evita inventar recomendaciones y deja claro que el catalogo necesita mas datos.';

    const markdown = applyVars(template, {
      animeCount: String(animeCount ?? 0),
      countries: countries || 'España, Mexico, Argentina, Colombia y Chile',
      genreContext: GENRE_CONTEXT[safeGenre] ?? 'un tono reconocible y una promesa clara para el espectador',
      genreName: safeGenre,
      platformName: safePlatform,
      topSentence,
    });

    return {
      html: markdownToHtml(markdown),
      markdown,
      wordCount: countWords(markdown),
    };
  }
}
