# Security Policy

## Supported Versions

MyMindMirror is currently in active development. Security fixes are applied to:

| Version | Supported |
|---|---|
| Latest `main` branch | ✅ |
| Tagged releases | ✅ (most recent release only) |
| Older releases | ❌ |

If you're running a forked or modified version, please reproduce the issue against a clean checkout of `main` before reporting.

## Reporting a Vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Instead, use [GitHub's private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability) on this repository:

1. Go to the **Security** tab
2. Click **Report a vulnerability**
3. Fill in the private advisory form

[//]: # (Alternatively, email: *&#40;add your contact email if you'd prefer email over GitHub's tooling&#41;*)

## What to Include

The more detail, the faster the fix. Please include:

- **A clear description** of the vulnerability and its impact
- **Steps to reproduce** — ideally a minimal proof-of-concept
- **Affected components** — API endpoint, frontend page, database, config
- **Environment** — version/commit hash, JDK version, deployment model (Docker Compose, k8s, bare metal)
- **Your assessment of severity** — Critical / High / Medium / Low
- **Any suggested fix** — optional, but appreciated

Do **not** include live secrets, real user data, or production URLs. Reproduce against a local instance.

## What to Expect

| Timeline | Action |
|---|---|
| **Within 48 hours** | Acknowledgement of receipt |
| **Within 7 days** | Initial assessment and severity confirmation |
| **Within 30 days** | Fix or mitigation plan shared (for High/Critical) |
| **On fix release** | Public disclosure with credit (unless you request anonymity) |

Timelines are best-effort. This is a solo-maintained project, not a company with a 24/7 security team. If you don't hear back within 7 days, feel free to send a follow-up.

## Scope

**In scope:**

- Authentication and authorization flaws
- Encryption weaknesses (journal text, API keys, passwords)
- SQL injection, XSS, CSRF, SSRF
- IDOR / cross-user data access
- Path traversal, arbitrary file read/write
- Remote code execution
- Denial of service via unauthenticated endpoints
- Secrets leaked in logs, error responses, or version control

**Out of scope:**

- Vulnerabilities requiring physical access to the machine running the app
- Social engineering of maintainers or users
- Attacks requiring a compromised TLS certificate or DNS
- Rate-limiting and brute-force on `/api/auth/**` (already documented in the README as a known gap)
- Missing Redis authentication in the default Docker Compose setup (documented, internal-network-only)
- `ddl-auto=update` schema risk (documented, blocked on Flyway adoption)
- Vulnerabilities in third-party dependencies (report those upstream — but do let us know if a dependency vulnerability is exploitable in MyMindMirror specifically)

## Known Security Gaps

These are documented in the README's [Security Model](README.md#security-model) section and are **not** eligible for security reports unless you have a novel exploitation path:

- No rate limiting on `/api/auth/**`
- Redis has no password by default (internal Docker network only)
- `ddl-auto=update` is active in dev (blocked on Flyway adoption for prod)
- No refresh token mechanism (JWTs are valid until expiry)
- `DynamicAiClientService` cache is unbounded (scale concern, not a security issue)

## Recognition

With your permission, security researchers who report valid issues are credited in:

- The GitHub Security Advisory
- The `## Acknowledgments` section of the README

If you'd prefer to remain anonymous, just say so in your report.

## Thank You

Security research on personal-data platforms matters. If you've found something, you've already made MyMindMirror safer for everyone who uses it. Thank you for taking the time to report it responsibly.