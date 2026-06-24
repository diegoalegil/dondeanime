import { getLocale, type Locale } from '@/i18n';

// Los nombres de genero llegan del API en ingles (AniList). El long-tail hispano
// busca "anime de accion", "anime de terror", "anime de deportes" — no "Action".
// Mapa por slug (estable) -> nombre en espanol para todo el texto y meta ES.
const ES_GENRE_NAMES: Record<string, string> = {
  action: 'Acción',
  adventure: 'Aventura',
  comedy: 'Comedia',
  drama: 'Drama',
  ecchi: 'Ecchi',
  fantasy: 'Fantasía',
  horror: 'Terror',
  'mahou-shoujo': 'Mahou Shoujo',
  mecha: 'Mecha',
  music: 'Música',
  mystery: 'Misterio',
  psychological: 'Psicológico',
  romance: 'Romance',
  'sci-fi': 'Ciencia ficción',
  'slice-of-life': 'Recuentos de la vida',
  sports: 'Deportes',
  supernatural: 'Sobrenatural',
  thriller: 'Suspense',
};

/**
 * Nombre del genero localizado. En ES devuelve el nombre espanol por slug (con
 * fallback al nombre del API si el slug no esta mapeado); en EN devuelve el
 * nombre del API tal cual (ingles).
 */
export const localizedGenreName = (
  slug: string,
  fallbackName: string,
  locale: Locale = getLocale(),
): string => (locale === 'es' ? ES_GENRE_NAMES[slug] ?? fallbackName : fallbackName);

// Descripcion editorial por genero: prosa lider unica para /genero/[slug] y su
// espejo /en/genre/[slug]. Antes la pagina solo tenia H1 + contador (thin
// content que Google no premia). Texto distinto por genero (no plantilla con la
// palabra cambiada). Si el slug no esta mapeado, no se renderiza nada.
const ES_GENRE_DESCRIPTIONS: Record<string, string> = {
  action: 'El anime de acción vive del ritmo: combates coreografiados, poderes que escalan y héroes que se superan pelea a pelea. Aquí tienes los títulos de acción que puedes ver en streaming, del shonen más taquillero a las joyas de culto, con la plataforma de cada país.',
  adventure: 'La aventura va de viajes, mundos por descubrir y grupos que crecen camino a una meta. Fantasía épica, exploración y road trips imposibles: encuentra cada serie y dónde verla en España y Latinoamérica.',
  comedy: 'Comedia para desconectar: gags rápidos, romances torpes y situaciones absurdas llevadas al límite. Del slice of life amable a la parodia que no respeta nada, con su disponibilidad por plataforma.',
  drama: 'El drama busca emocionar con conflictos humanos, decisiones difíciles y personajes que cargan con su pasado. Historias que se quedan contigo mucho después del final, y dónde verlas en streaming.',
  ecchi: 'El ecchi juega con el fanservice y el humor subido de tono sin llegar al contenido explícito. Comedias atrevidas y romances pícaros, con aviso claro de en qué plataforma están.',
  fantasy: 'Magia, otros mundos y criaturas imposibles: la fantasía es el terreno del isekai, las espadas y los hechizos. Reúne aquí los grandes mundos de fantasía y la plataforma donde verlos en cada país.',
  horror: 'El terror en anime no se corta: maldiciones, cuerpos que se retuercen y tensión que no afloja. Para quien busca pasar miedo de verdad, con la disponibilidad de cada título por país.',
  'mahou-shoujo': 'Chicas mágicas que se transforman para proteger lo que quieren: el mahou shoujo mezcla amistad, sacrificio y mucha luz de color. De los clásicos del género a las reinvenciones más oscuras.',
  mecha: 'Robots gigantes, pilotos jóvenes y guerras que pesan: el mecha habla tanto de las máquinas como de quienes van dentro. Del super robot clásico al real robot más político, con dónde verlo.',
  music: 'Bandas, idols y escenarios: el anime musical late al ritmo de sus canciones, entre ensayos, rivalidades y el subidón del directo. Encuentra cada serie y la plataforma donde escucharla.',
  mystery: 'Casos por resolver, pistas escondidas y giros que no ves venir: el misterio te mantiene atando cabos hasta el final. Detectives, conspiraciones y enigmas, con su disponibilidad en streaming.',
  psychological: 'El anime psicológico se mete en la cabeza: mentes al límite, dilemas morales y una tensión que viene de dentro, no de la acción. Para ver con los cinco sentidos puestos.',
  romance: 'Del flechazo torpe al amor que duele: el romance va de relaciones que crecen capítulo a capítulo. Comedia romántica, drama y triángulos imposibles, con la plataforma de cada país.',
  'sci-fi': 'Futuros posibles, tecnología que inquieta y preguntas grandes: la ciencia ficción usa el mañana para hablar del hoy. Distopías, espacio y ciberpunk, con dónde verlos en streaming.',
  'slice-of-life': 'Lo cotidiano hecho historia: el slice of life encuentra belleza en lo pequeño, sin grandes batallas ni mundos en juego. Calma, humor amable y personajes con los que apetece convivir.',
  sports: 'Entrenar, perder, levantarse y volver: el anime de deportes va de superación y equipo tanto como del juego. Partidos que se sienten épicos, con la plataforma donde seguirlos.',
  supernatural: 'Espíritus, poderes y reglas que la realidad no explica: lo sobrenatural mezcla lo cotidiano con lo imposible. Yokai, fantasmas y dones extraños, con su disponibilidad por país.',
  thriller: 'Suspense que aprieta: cuenta atrás, juegos del gato y el ratón y decisiones bajo presión. Tramas que no te dejan respirar, con dónde verlas en cada plataforma.',
};

const EN_GENRE_DESCRIPTIONS: Record<string, string> = {
  action: 'Action anime lives on momentum: choreographed fights, escalating powers and heroes who level up battle by battle. Find the action titles you can stream, from blockbuster shonen to cult gems, with the platform for each country.',
  adventure: 'Adventure is about journeys, worlds to discover and crews that grow on the way to a goal. Epic fantasy, exploration and impossible road trips: find each series and where to watch it across Spain and Latin America.',
  comedy: 'Comedy to switch off: quick gags, clumsy romances and absurd situations pushed to the limit. From gentle slice of life to parody that spares no one, with availability by platform.',
  drama: 'Drama aims to move you with human conflict, hard choices and characters who carry their past. Stories that stay with you long after the finale, and where to stream them.',
  ecchi: 'Ecchi plays with fanservice and risqué humor without crossing into explicit content. Cheeky comedies and flirty romances, with a clear note of which platform has them.',
  fantasy: 'Magic, other worlds and impossible creatures: fantasy is the home of isekai, swords and spells. Gather the great fantasy worlds here and the platform to watch them in each country.',
  horror: 'Horror anime pulls no punches: curses, twisting bodies and tension that never lets up. For anyone after a real scare, with each title\'s availability by country.',
  'mahou-shoujo': 'Magical girls who transform to protect what they love: mahou shoujo blends friendship, sacrifice and plenty of color. From genre classics to darker reinventions.',
  mecha: 'Giant robots, young pilots and wars that weigh heavy: mecha is as much about the people inside as the machines. From classic super robot to political real robot, with where to watch.',
  music: 'Bands, idols and stages: music anime beats to its songs, between rehearsals, rivalries and the rush of a live show. Find each series and the platform to hear it on.',
  mystery: 'Cases to crack, hidden clues and twists you won\'t see coming: mystery keeps you connecting the dots to the end. Detectives, conspiracies and puzzles, with streaming availability.',
  psychological: 'Psychological anime gets inside your head: minds at the edge, moral dilemmas and tension that comes from within rather than the action. One to watch with full attention.',
  romance: 'From an awkward crush to love that hurts: romance is about relationships that grow episode by episode. Rom-com, drama and impossible triangles, with the platform for each country.',
  'sci-fi': 'Possible futures, unsettling tech and big questions: sci-fi uses tomorrow to talk about today. Dystopias, space and cyberpunk, with where to stream them.',
  'slice-of-life': 'The everyday turned into story: slice of life finds beauty in small things, with no grand battles or worlds at stake. Calm, kind humor and characters worth spending time with.',
  sports: 'Train, lose, get up and go again: sports anime is about growth and teamwork as much as the game. Matches that feel epic, with the platform to follow them on.',
  supernatural: 'Spirits, powers and rules reality can\'t explain: the supernatural mixes the everyday with the impossible. Yokai, ghosts and strange gifts, with availability by country.',
  thriller: 'Tight suspense: countdowns, cat-and-mouse games and decisions under pressure. Plots that won\'t let you breathe, with where to watch them on each platform.',
};

/**
 * Descripcion editorial del genero por locale (ES/EN). Devuelve null si el slug
 * no esta mapeado, para que la pagina no renderice un parrafo vacio.
 */
export const genreDescription = (
  slug: string,
  locale: Locale = getLocale(),
): string | null =>
  (locale === 'en' ? EN_GENRE_DESCRIPTIONS[slug] : ES_GENRE_DESCRIPTIONS[slug]) ?? null;
