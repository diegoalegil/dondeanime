# DondeAnime — Brand logo lockup

Horizontal lockup (isotype + wordmark) in the "nocturnal dusk" system. Flat vector,
crisp outlines, subtle low-intensity neon glow. Warm coral (left) → cool cyan (right).
Replaces the old violet→pink `.brand-gradient-text` brand link.

Palette · night `#0E1020` · surface `#1A1D33` · coral `#FF6B4A` · cyan `#36E0E6` · text `#F4F5FB`
(no warm gold `#FFD27A` here — that's reserved for Premium.)

The wordmark is **Geist**, converted to vector outlines — so every `.svg` renders correctly
when loaded via `<img>` (where the page's `@font-face` is NOT available). No font dependency.

## Files (all 1024×256, transparent, unless noted)

| File | Use |
| --- | --- |
| `logo-lockup-horizontal.svg` | **Primary.** Color + glow. Header on dark. |
| `logo-lockup-horizontal-soft.svg` | Alt wordmark (Geist 500, open tracking). |
| `logo-lockup-horizontal-flat.svg` | No glow, color. Dark bg / print. |
| `logo-lockup-horizontal-flat-light.svg` | No glow, color, dark text. Print on white. |
| `logo-lockup-horizontal-mono-white.svg` | Single-ink `#F4F5FB`. **Footer / dark bg.** |
| `logo-lockup-horizontal-mono-dark.svg` | Single-ink `#19151F`. Light bg. |
| `logo-lockup-horizontal-editable.svg` | Live `<text>` Geist — **design tools only** (needs Geist installed). |
| `logo-lockup-horizontal@2x.png` | 2048×512 transparent raster (email, press, social). |
| `logo-lockup-horizontal.png` | 1024×256 transparent raster (flat). |
| `isotype.svg` / `isotype-flat.svg` / `isotype-mono-white.svg` | 256×256 mark. Favicon, app icon, mobile collapsed header. |

## Integration

```astro
<!-- Header.astro — replace the .brand-gradient-text link -->
<a href={localizedPath('/')} class="inline-flex items-center" aria-label={t('brand.name')}>
  <img src="/brand/logo-lockup-horizontal.svg" alt="DondeAnime" height="32" width="128" class="h-8 w-auto" />
</a>

<!-- Footer.astro — brand slot -->
<img src="/brand/logo-lockup-horizontal-mono-white.svg" alt="DondeAnime" height="28" class="h-7 w-auto opacity-90" />
```

- **Clear space:** ≥ the height of the play triangle on all sides.
- **Minimum width:** 96px for the full lockup; below that use `isotype.svg`.
- **Mobile:** swap the lockup for `isotype.svg` under the header breakpoint.

## Re-generating outlines
If the wordmark text/weight changes, regenerate outlines from the licensed Geist with any
font tool (Illustrator/Inkscape "Convert to outlines", or fontkit) and replace the `<g>`
wordmark group. Keep baseline at `translate(300,156)`, isotype at `translate(8,4)`.
