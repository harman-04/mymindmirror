# Contributing to MyMindMirror

Thanks for considering a contribution. This document covers everything you need to land a successful PR.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Commit Style](#commit-style)
- [Pull Requests](#pull-requests)
- [What We Won't Accept](#what-we-wont-accept)

## Code of Conduct

By participating in this project, you agree to follow the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). Be kind, assume good faith, disagree without being disagreeable.

## Getting Started

### Prerequisites

- **JDK 25** (Temurin recommended)
- **Node.js 22 LTS** or newer
- **Docker** (for Testcontainers and the local dev stack)
- **Maven 3.9+** (or use the `./mvnw` wrapper)

### Setup

```bash
# Fork the repo on GitHub, then:
git clone https://github.com/harman-04/mymindmirror.git
cd mymindmirror

# Copy env template and fill in secrets
cp .env.example .env
# Edit .env — at minimum, set GEMINI_API_KEY, JWT_SECRET, ENCRYPTION_MASTER_KEY

# Generate secrets if needed:
openssl rand -base64 64   # for JWT_SECRET
openssl rand -base64 32   # for ENCRYPTION_MASTER_KEY

# Start the databases
docker compose up -d mysql postgres redis

# Run the backend
cd backend/mymindmirror-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# In a separate terminal, run the frontend
cd frontend/mymindmirror-app
npm install
npm run dev
```

See the [README](README.md#manual-setup-for-contributors) for a detailed setup guide.

## Development Workflow

1. **Open an issue first** for non-trivial changes (new features, API changes, architectural shifts). This avoids wasted effort if the direction doesn't fit.
2. **Create a feature branch** off `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
3. **Make small, focused commits.** One logical change per commit.
4. **Run the full test suite** before pushing:
   ```bash
   # Backend
   cd backend/mymindmirror-backend
   ./mvnw verify

   # Frontend
   cd ../../frontend/mymindmirror-app
   npm test
   ```
5. **Push to your fork** and open a PR against `main`.

## Coding Standards

### Java

- Follow standard Java conventions (camelCase for methods, PascalCase for classes)
- Use `@Slf4j` for logging — no `System.out.println`
- Prefer `record` for DTOs, `class` for entities
- Controllers stay thin — business logic belongs in services
- Annotate service methods with `@Transactional` when they write to the database
- Avoid `@Data` on new JPA entities — use `@Getter`/`@Setter` + `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on the ID
- Use `Optional` for return types that may be empty — never return `null` from a repository-facing method
- Name test methods with `shouldDoX_whenY` or `methodName_condition_expected`

### JavaScript / JSX

- Follow the ESLint config — `npm run lint` should pass with zero warnings
- Use `React.memo()` on frequently re-rendering components
- Extract custom logic into hooks under `src/hooks/`
- Prefer `async/await` over `.then()` chains
- No `console.log()` in committed code — use `console.error` only for genuine errors
- Respect the 3-layer elevation palette (see [Architecture](README.md#architecture))

**Testing conventions:**

- **Use `data-testid` for elements that E2E tests interact with.** Text-based selectors (`has-text`, `getByText` on buttons) break on every copy change — `data-testid` attributes only break when the test-id is intentionally renamed.
- **Never assert on toast text.** Toasts are ephemeral, race each other, and render in a portal that `getByText` cannot reliably match. Assert on durable state instead (form reset, list item appears, page navigates).
- **One user journey per E2E test.** If five things can fail, you want five clear failures — not one tangled assertion.
- 
### Both

- No trailing whitespace
- No `TODO` comments without a linked issue: `// TODO(#123): refactor this`
- Delete dead code — don't comment it out
- Update the README if you change setup steps, env vars, or public API

## Testing

Every PR must include tests for new behavior.

### Backend

- **Unit tests** for pure logic (services, mappers, utilities) — `*Test.java`
- **Integration tests** for anything touching the database, cache, or AI gateway — `*IT.java`
- Integration tests use Testcontainers (real MySQL + Redis) — Docker must be running
- Run all: `./mvnw verify`
- Coverage report: `target/site/jacoco/index.html`

### Frontend

- **Component tests** with Vitest + Testing Library
- Mock *hooks* (data layer), not component internals
- Drive the UI with `userEvent`, assert on user-visible outcomes
- Run: `npm test`

### Coverage

The project maintains ~99% backend coverage. New code should be tested. If you can't cover a line, explain why in the PR description.

## Commit Style

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body — optional>

<footer — optional, for breaking changes or issue links>
```

**Types:**

| Type | Use |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `docs` | Documentation only |
| `test` | Adding or fixing tests |
| `chore` | Build, tooling, dependency updates |
| `perf` | Performance improvement |
| `style` | Formatting only |

**Scopes** (optional): `roadmap`, `journal`, `schedule`, `auth`, `ui`, `ai`, `db`, `ci`

**Examples:**

```
feat(roadmap): add batch continuation endpoint
fix(journal): prevent NPE when summary is null
docs(readme): clarify environment variable setup
chore(deps): bump spring-ai to 2.0.0-M9
```

## Pull Requests

Before opening a PR:

- [ ] Tests pass:
    - Backend: `./mvnw verify`
    - Frontend unit: `npm test`
    - Frontend E2E: `npx playwright test` (requires `docker compose up -d`)
- [ ] Coverage doesn't drop below the current baseline
- [ ] No unrelated changes — keep the PR focused on one thing
- [ ] Self-review: read your own diff before submitting
- [ ] Clear title and description — explain *why*, not just *what*
- [ ] Linked issue (`Closes #123`)

**PR title format:** same as commit convention (`feat(x): ...`).

**A maintainer will review within a few days.** Expect questions about design choices — this is normal, not rejection.

If a PR sits for more than a week without response, feel free to ping the maintainer.

## What We Won't Accept

- Breaking changes to the public REST API without a migration path
- Features requiring a paid third-party service beyond the existing Gemini integration
- Removal of encryption at rest for journal text
- Removal of the unified `ErrorResponse` shape
- Changes that weaken cross-user isolation (the 404-on-not-owned policy)
- Adding analytics/tracking to the frontend without explicit consent UI
- Introductions of tracking pixels, third-party cookies, or external analytics without prior discussion

## Recognition

Non-trivial PRs earn a mention in the [Acknowledgments](README.md#acknowledgments) section. If you'd prefer to stay anonymous, just say so in the PR.

## Questions?

Open a [GitHub Discussion](https://github.com/harman-04/mymindmirror/discussions) or file an issue. For security vulnerabilities, see [SECURITY.md](SECURITY.md).