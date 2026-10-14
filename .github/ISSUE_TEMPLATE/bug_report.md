---
name: Bug Report
about: Report a reproducible bug
title: "[Bug] "
labels: bug
assignees: ''
---

## Description

A clear, concise description of the bug.

## Steps to Reproduce

1.
2.
3.

## Expected Behavior

What you expected to happen.

## Actual Behavior

What actually happened. Include error messages, HTTP status codes, or stack traces if relevant.

## Environment

- **Deployment:** Docker Compose / manual (backend + frontend) / other
- **OS:** e.g. Ubuntu 22.04, macOS 14, Windows 11 (WSL2)
- **Browser (if frontend):** e.g. Chrome 130, Firefox 132, Safari 18
- **Backend JDK:** `java -version` output
- **Node.js:** `node -v` output (if frontend-related)
- **Commit / version:** `git rev-parse --short HEAD` or release tag

## Logs

<details>
<summary>Backend logs</summary>

```
docker compose logs backend | tail -100
```

</details>

<details>
<summary>Frontend console</summary>

```
Paste browser console errors here.
```

</details>

## Screenshots

If applicable, add screenshots to help explain the problem.

## Additional Context

Anything else relevant — recent changes, workarounds attempted, whether it's reproducible.

## Checklist

- [ ] I searched existing issues and this is not a duplicate
- [ ] I can reproduce the bug on a clean checkout of `main`
- [ ] I've redacted any secrets, tokens, or personal data from this report