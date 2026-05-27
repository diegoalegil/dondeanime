# Lighthouse thresholds

Current threshold:

- Performance: 80
- Accessibility: 90
- Best Practices: 80
- SEO: 80

Target threshold:

- Performance: 90
- Accessibility: 90
- Best Practices: 90
- SEO: 90

Plan:

- PR 18.2 must raise at least one affected threshold by 2-3 points after the Cloudflare cache rules work.
- PR 18.3 must raise the relevant rendering and LCP-related threshold by 2-3 points after critical CSS work.
- PR 18.4 must raise the relevant runtime and offline-related threshold by 2-3 points after service worker improvements.
- PR 18.5 must leave all Lighthouse thresholds at 90.

Accessibility is fixed at 90 throughout Sprint 18 and must not be lowered.
