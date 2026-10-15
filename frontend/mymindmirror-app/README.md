# MyMindMirror — Frontend

React 19 + Vite 7 + Tailwind 4 single-page application. See the [main README](../../README.md) for the full project overview, architecture, and deployment guide.

---

## Quick Start

```bash
npm install
npm run dev
```

Runs at http://localhost:5173. Requires the backend running at http://localhost:8080 — see [Manual Setup](../../README.md#manual-setup-for-contributors).

---

## Scripts

| Command | Purpose |
|---|---|
| `npm run dev` | Start the Vite dev server with HMR |
| `npm run build` | Production build to `dist/` (also strips `console.*` and `debugger`) |
| `npm run preview` | Preview the production build locally |
| `npm run lint` | ESLint across all `.jsx` files |
| `npm test` | Vitest unit + component tests |
| `npm run test:ui` | Vitest in interactive UI mode |
| `npx playwright test` | Run E2E tests (requires Docker Compose stack running) |

---

## Structure

```
src/
├── components/     # ~40 reusable UI components (charts, forms, modals, skeletons)
├── pages/          # Route-level pages (Home, Journal, Profile, Achievements, ...)
├── hooks/          # TanStack Query hooks, one file per backend domain
├── contexts/       # ThemeContext (dark/light + system sync)
├── utils/          # axios interceptors, chart export, color palettes
├── data/           # Static data (writing prompts)
└── test/           # Vitest setup
```

---

## Conventions

- **`data-testid` for E2E selectors** — never text-based selectors; copy changes break tests.
- **Mock hooks, not components** in unit tests — assert on user-visible outcomes.
- **`React.memo` on chart components** — they render frequently and their props rarely change.
- **Strict 3-layer palette** — `#1A162F` (L1) / `#131127` (L2) / `black/20` (L3) in dark mode. See [Frontend Experience](../../README.md#frontend-experience) in the main README.
- **No `console.log` in committed code** — production build strips them anyway; leave only `console.error` for genuine failures.

---

## Testing

```bash
npm test                       # unit + component tests
npx playwright test            # E2E (needs the Docker stack up)
npx playwright show-report     # view the last test run
```

E2E tests use the smoke test pattern — one user journey per file. See [`e2e/journal-flow.spec.js`](e2e/journal-flow.spec.js) for the template.

---

## Environment

The frontend reads a single environment variable at build time:

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Backend API base URL |

For all other configuration (JWT secret, database, etc.), see the [main README's Environment Variables section](../../README.md#environment-variables).