// Sanitizador HTML de allowlist, sin dependencias. Se aplica antes de
// cualquier `set:html` con texto que provenga de la API o de overrides
// editoriales del admin (AniList/TMDb traen <i>, <br>, etc.). Evita XSS
// almacenado: descarta <script>, atributos (onerror, href=javascript:) y
// cualquier etiqueta fuera de la lista de formato básico.

const ALLOWED_TAGS = new Set(['b', 'i', 'em', 'strong', 'br', 'p', 'ul', 'ol', 'li', 'span']);

export function sanitizeHtml(input: string | null | undefined): string {
  if (!input) return '';
  let html = String(input);

  // 1. Eliminar bloques peligrosos junto con su contenido.
  html = html.replace(
    /<(script|style|iframe|object|embed|noscript|svg|math)\b[\s\S]*?<\/\1\s*>/gi,
    '',
  );

  // 2. Procesar cada etiqueta: permitir solo la allowlist y SIN atributos
  //    (así se eliminan onclick/onerror/href=javascript: y similares).
  html = html.replace(/<\/?\s*([a-zA-Z][a-zA-Z0-9]*)\b[^>]*?>/g, (match, tagName) => {
    const tag = String(tagName).toLowerCase();
    if (!ALLOWED_TAGS.has(tag)) return '';
    const isClosing = /^<\s*\//.test(match);
    return isClosing ? `</${tag}>` : `<${tag}>`;
  });

  // 3. Restos de aperturas peligrosas sin cierre (p.ej. "<script src=x")
  //    quedarían como texto; eliminamos cualquier "<" suelto de etiqueta
  //    no cerrada para no dejar markup a medias.
  return html;
}
