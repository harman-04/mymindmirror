<h1 align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/logo-dark.svg" />
    <source media="(prefers-color-scheme: light)" srcset="docs/logo-light.svg" />
    <img src="docs/logo-light.svg" alt="MyMindMirror" width="320" />
  </picture>
</h1>

<p align="center"><em>AI-powered journaling, RAG-based reflection coaching, and adaptive scheduling — built on a polyglot Spring Boot 4 + React 19 stack.</em></p>

<p align="center">
  <a href="https://github.com/harman-04/mymindmirror/actions/workflows/backend-ci.yml"><img src="https://github.com/harman-04/mymindmirror/actions/workflows/backend-ci.yml/badge.svg" alt="Backend CI" /></a>
  <a href="https://github.com/harman-04/mymindmirror/actions/workflows/frontend-ci.yml"><img src="https://github.com/harman-04/mymindmirror/actions/workflows/frontend-ci.yml/badge.svg" alt="Frontend E2E" /></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License: Apache 2.0" /></a>
  <img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java 25" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg" alt="Spring Boot 4.0.6" />
  <img src="https://img.shields.io/badge/React-19-blue.svg" alt="React 19" />
  <img src="https://img.shields.io/badge/PostgreSQL-pgvector-336791.svg" alt="PostgreSQL pgvector" />
</p>

<p align="center">
  <a href="#-quick-start">Quick Start</a> ·
  <a href="#-features">Features</a> ·
  <a href="#-architecture">Architecture</a> ·
  <a href="#-api-documentation">API</a> ·
  <a href="#-security-model">Security</a>
</p>

<div align="center">
<img src="docs/screenshots/main/today-dashboard.webp" alt="MyMindMirror Today Dashboard — AI reflection, emotion breakdown, and daily journal history" width="70%"  />
</div>

## Table of Contents

- [Why MyMindMirror?](#why-mymindmirror)
- [Screenshots](#screenshots)
    - [Today Dashboard](#today-dashboard)
    - [Full Analytics — History Tab](#full-analytics--history-tab)
    - [Analytics Deep Dive](#analytics-deep-dive)
    - [AI Reflection Coach (RAG)](#ai-reflection-coach-rag)
    - [Roadmap Planner](#roadmap-planner)
    - [Smart Timetable](#smart-timetable)
    - [Milestone Tracker with AI Insights](#milestone-tracker-with-ai-insights)
    - [Achievements](#achievements)
    - [Themes & Responsive Design](#themes--responsive-design)
    - [Full Feature Gallery](#full-feature-gallery)
- [Features](#features)
    - [User-Facing Features](#user-facing-features)
    - [Engineering Highlights](#engineering-highlights)
    - [Backend Platform](#backend-platform)
    - [Frontend Experience](#frontend-experience)
- [Architecture](#architecture)
    - [High-Level Diagram](#high-level-diagram)
    - [Polyglot Data Layer](#polyglot-data-layer)
    - [AI Pipeline](#ai-pipeline)
- [Performance](#performance)
    - [Backend Performance](#backend-performance)
    - [Frontend Performance](#frontend-performance)
- [Tech Stack](#tech-stack)
    - [Backend](#backend)
    - [AI Models (via Google Gen AI)](#ai-models-via-google-gen-ai)
    - [Databases](#databases)
    - [Frontend](#frontend)
    - [Testing Libraries](#testing-libraries)
    - [Infrastructure & DevOps](#infrastructure--devops)
- [Quick Start](#quick-start)
    - [Prerequisites](#prerequisites)
    - [1. Clone the repository](#1-clone-the-repository)
    - [2. Generate secrets](#2-generate-secrets)
    - [3. Create your .env file](#3-create-your-env-file)
    - [4. Start everything](#4-start-everything)
    - [5. Open the app](#5-open-the-app)
- [Environment Variables](#environment-variables)
    - [Required](#required)
    - [Optional (with defaults)](#optional-with-defaults)
- [Manual Setup (for Contributors)](#manual-setup-for-contributors)
    - [Testing on a Physical Android Device](#testing-on-a-physical-android-device)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
    - [Authentication Model](#authentication-model)
    - [Endpoint Reference](#endpoint-reference)
    - [Unified Error Response](#unified-error-response)
- [Database Schema & Triggers](#database-schema--triggers)
    - [Entity Overview](#entity-overview)
    - [Relationship Overview](#relationship-overview)
    - [The daily_journal_summary Trigger Architecture](#the-daily_journal_summary-trigger-architecture)
    - [Vector Schema (PostgreSQL/pgvector)](#vector-schema-postgresqlpgvector)
- [Testing](#testing)
    - [Backend Testing](#backend-testing)
    - [Frontend Testing](#frontend-testing)
    - [Test Coverage](#test-coverage)
    - [What's Not Tested (Yet)](#whats-not-tested-yet)
    - [Troubleshooting the Trigger Pipeline](#troubleshooting-the-trigger-pipeline)
- [Security Model](#security-model)
    - [Encryption at Rest](#encryption-at-rest)
    - [Authentication Flow](#authentication-flow)
    - [Known Gaps](#known-gaps)
- [Configuration Profiles](#configuration-profiles)
- [Deployment](#deployment)
- [Roadmap / Known Limitations](#roadmap--known-limitations)
    - [Currently Deferred](#currently-deferred)
    - [Planned Features](#planned-features)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)
- [Contact](#contact)

## Why MyMindMirror?

Most journaling tools fall into one of two traps. Either they're **too shallow** — a plain text file with no analysis, no structure, no way to see patterns over time — or they're **too locked-in** — proprietary cloud silos that own your data, charge for basic features like export, and treat AI as a black-box add-on with no memory of what you wrote last month.

MyMindMirror is built on a different premise: **your journal is the most personal data you have, and it should be treated that way.** That principle drives three concrete architectural choices:

**1. Privacy is the default, not a feature.** Journal text is encrypted at rest with AES-CBC before it ever hits disk, using a key derived from your own password hash via PBKDF2. Your Gemini API key — if you choose to supply one — is stored with AES-GCM under an independent server master key and never returned in plaintext. Full data export (PDF, CSV, PNG) is available from day one with no paywall, no rate limit, and no "contact us for enterprise data access."

**2. The AI actually remembers what you wrote.** Every journal entry is embedded into a 3072-dimensional vector and stored in PostgreSQL/pgvector. When you ask the AI Coach a question, the backend retrieves your most semantically relevant past entries — not generic advice from a chatbot that's never read your history. RAG is the default, not a bolt-on.

**3. Journaling, planning, and tracking are one loop.** A growing tip surfaced by the AI analysis can be imported into a milestone with one click. A roadmap task can be scheduled onto your calendar in a single drag. A completed task updates your XP, your streak, and your analytics in real time. Nothing lives in a silo.

### Who it's for

- **Developers and self-hosters** who want a full-featured, self-hosted journaling platform they can audit and modify
- **Anyone learning Spring Boot 4 / Spring AI / React 19** who wants a realistic polyglot reference project beyond to-do lists
- **Privacy-conscious users** who want AI-powered reflection without handing their raw journal to a third party

### Who it's *not* for

- Users looking for a managed, zero-setup SaaS — MyMindMirror is self-hosted
- A replacement for professional mental health support — the AI is a reflection aid, not a therapist
- Anyone who needs sub-second AI responses — reflection is a deliberate, thoughtful process

## Screenshots

A quick tour of the main surfaces. Every screenshot is from the actual app, running locally with a seeded demo account.

### Today Dashboard

<img src="docs/screenshots/main/today-dashboard.webp" alt="Today Dashboard showing today's reflection, emotion breakdown, and daily history" width="100%" />

The default landing view. Today's AI-generated reflection sits above your emotion snapshot, with the day's journal entries below.

### Full Analytics — History Tab

<img src="docs/screenshots/main/analytics-overview.webp" alt="Full analytics dashboard showing mood timeline, emotion radar, annual heatmap, and other charts" width="100%" />

Seven custom Chart.js engines stacked vertically — mood timeline, emotion radar, annual heatmap, emotion intensity bars, concern frequency, mood-vs-word-count correlation, and the animated key phrase cloud. The eighth engine (the daily emotion doughnut) lives on the Today Dashboard above. All charts are theme-aware, exportable as Retina PNGs, and use dynamic width for large datasets.

### Analytics Deep Dive

Three of the seven History charts, in full resolution.

**Emotion Radar — 14 Emotional Dimensions at Once**
<img src="docs/screenshots/main/chart-emotion-radar.webp" alt="Radar chart plotting 14 emotional dimensions simultaneously" width="100%" />

**Mood vs. Word Count — Pearson Correlation with Regression Trend Line**
<img src="docs/screenshots/main/chart-scatter.webp" alt="Scatter plot of daily mood versus word count with a linear regression trend line" width="100%" />

**Annual Mood Heatmap — 365 Days in One View**
<img src="docs/screenshots/main/chart-heatmap.webp" alt="GitHub-style contribution heatmap showing a full year of mood intensity" width="100%" />

### AI Reflection Coach (RAG)

<img src="docs/screenshots/main/ai-coach.webp" alt="AI Reflection Coach chat interface with suggestion chips and memory toggle" width="100%" />

Chat with an AI coach that retrieves your most relevant past journal entries via pgvector before answering. Suggestion chips auto-fill common prompts; the memory toggle switches between flowing conversation and clean-slate mode.

### Roadmap Planner

<img src="docs/screenshots/main/roadmap-planner.webp" alt="Roadmap planner showing list view with expandable weeks and task elaboration" width="100%" />

Chunked AI-generated roadmaps with dual view modes — deeply nested List View or horizontal CSS-Grid Timeline. Each task supports inline AI elaboration into step-by-step guides.

### Smart Timetable

<img src="docs/screenshots/main/smart-timetable.webp" alt="Smart timetable with drag-and-drop calendar and unscheduled task pool" width="100%" />

The AI schedules around your lifestyle preferences (energy peak, wake/sleep times, daily habits). Drag any block to reschedule, or drag from the unscheduled pool to add it to your calendar.

### Milestone Tracker with AI Insights

<img src="docs/screenshots/main/milestone-insights.webp" alt="Milestone tracker showing circular progress rings and an AI strategic insights panel" width="100%" />

Milestones show animated circular progress, cross-module task imports from Roadmap and Journal growth tips, and an AI-generated performance assessment on demand.

### Achievements

<img src="docs/screenshots/main/achievements.webp" alt="Achievements page with XP level, streak counter, and badge grid" width="100%" />

XP, streaks, levels, and 16 unlockable badges across four categories. The "closest target" engine surfaces your next achievable badge based on current progress.

### Themes & Responsive Design

Same demo account, same data — different framing.

<table>
<tr>
<td width="50%">

**Light Mode**

<img src="docs/screenshots/themes-responsive/light-mode-today.webp" alt="Today Dashboard in light mode" width="100%" />

</td>
<td width="50%">

**Dark Mode**

<img src="docs/screenshots/themes-responsive/today-dashboard-dark.webp" alt="Today Dashboard in dark mode" width="100%" />

</td>
</tr>
</table>

<div align="center">
<table align="center">
<tr>
<td width="50%">

**Mobile — Journal & Widgets**

<img src="docs/screenshots/themes-responsive/mobile-journal.webp" alt="Mobile view showing navbar, achievements, and journal input" width="100%" />

</td>
<td width="50%" >

**Mobile — Today Dashboard**

<img src="docs/screenshots/themes-responsive/mobile-today-dashboard.webp" alt="Mobile Today Dashboard with reflection and emotion chart" width="100%" />

</td>
</tr>
</table>

<table align="center">
<tr>
<td width="50%">

**Mobile — Analytics (Mood & Heatmap)**

<img src="docs/screenshots/themes-responsive/mobile-analytics-1.webp" alt="Mobile mood timeline and annual heatmap" width="100%" />

</td>
<td width="50%">

**Mobile — Analytics (Radar & Bars)**

<img src="docs/screenshots/themes-responsive/mobile-analytics-2.webp" alt="Mobile emotion radar and top emotion bars" width="100%" />

</td>
</tr>
</table>
</div>

*Full dark/light theme system. PWA-grade responsive layouts. Chart engines that scale to 440px without compromising readability.*

### Full Feature Gallery

Every screen in MyMindMirror, organized by module. Click any heading to expand. Images are lazy-loaded — they download only when you open a section.

<details>
<summary><strong>📓 Journaling & Analysis</strong></summary>

**Journal input with daily inspiration widget**
<img src="docs/screenshots/journaling-analysis/journal-input.webp" alt="Journal input form with the Daily Inspiration prompt above it" width="100%" />

**Async analysis in progress**
<img src="docs/screenshots/journaling-analysis/journal-analyzing.webp" alt="Skeleton loader on an expanded journal entry while AI analysis runs" width="100%" />

**Full analysis view**
<img src="docs/screenshots/journaling-analysis/journal-expanded.webp" alt="Expanded entry with summary, emotions doughnut chart, growth tips, and key phrases" width="100%" />

**Inline edit mode**
<img src="docs/screenshots/journaling-analysis/journal-editing.webp" alt="Journal entry in edit mode with save/cancel buttons" width="100%" />

**Anomaly alerts banner**
<img src="docs/screenshots/journaling-analysis/anomaly-alerts.webp" alt="Anomaly alert cards showing severity tiers and dismissal options" width="100%" />

</details>

<details>
<summary><strong>🔍 Four-Mode Search</strong></summary>

**AI concept search (semantic)**
<img src="docs/screenshots/search-modes/search-semantic.webp" alt="Semantic search results grouped by AI relevance" width="100%" />

**Exact keyword search**
<img src="docs/screenshots/search-modes/search-keyword.webp" alt="Keyword search returning matching entries" width="100%" />

**Mood score search**
<img src="docs/screenshots/search-modes/search-mood.webp" alt="Search by mood score range" width="100%" />

**Date range search**
<img src="docs/screenshots/search-modes/search-date.webp" alt="Search by date range" width="100%" />

</details>

<details>
<summary><strong>📊 Analytics — Remaining Charts</strong></summary>

**Daily emotion breakdown (doughnut)**
<img src="docs/screenshots/analytics-charts/chart-doughnut.webp" alt="Today's aggregated emotion distribution as a doughnut chart" width="100%" />

**Top 7 emotional drivers (gradient bar)**
<img src="docs/screenshots/analytics-charts/chart-emotion-bars.webp" alt="Bar chart of the top 7 strongest emotional drivers with gradient fills" width="100%" />

**Frequent concerns (frequency bar)**
<img src="docs/screenshots/analytics-charts/chart-concerns.webp" alt="Frequency bar chart of top 25 detected concerns" width="100%" />

**Mood timeline (zoomable line)**
<img src="docs/screenshots/analytics-charts/chart-mood-timeline.webp" alt="Line chart of mood score over time with ctrl+scroll zoom" width="100%" />

**Key phrase cloud — floating animation**
<video src="docs/screenshots/analytics-charts/chart-word-cloud.mp4" autoplay loop muted playsinline width="100%"></video>

**Key phrase cloud — interactive click-to-filter**
<img src="docs/screenshots/analytics-charts/chart-word-cloud.webp" alt="Key phrase cloud with click-to-filter applied, showing filtered journal entries below" width="100%" />

</details>

<details>
<summary><strong>🗺️ Roadmap Planner</strong></summary>

**Empty state**
<img src="docs/screenshots/roadmap-planner/roadmap-empty.webp" alt="Roadmap planner empty state with a create-first-roadmap call to action" width="100%" />

**Generation form**
<img src="docs/screenshots/roadmap-planner/roadmap-generating.webp" alt="Roadmap generation form with goal, timeframe, and preference overrides" width="100%" />

**Timeline view (horizontal CSS grid)**
<img src="docs/screenshots/roadmap-planner/roadmap-timeline.webp" alt="Horizontal timeline view with sticky week headers and task cards" width="100%" />

**Task details — two views**

*Modal view — full AI guide with regenerate and enhance actions:*
<img src="docs/screenshots/roadmap-planner/roadmap-task-modal.webp" alt="Task detail modal showing AI-generated step-by-step guide and subtasks" width="100%" />

*Inline expansion — quick view without leaving the list:*
<img src="docs/screenshots/roadmap-planner/roadmap-task-expanded.webp" alt="Roadmap task expanded inline within the list" width="100%" />

**Chunked continuation (Load Next 6 Weeks)**
<img src="docs/screenshots/roadmap-planner/roadmap-continue.webp" alt="Bottom of roadmap with 'Load Next 6 Weeks' button and progress indicator" width="100%" />

</details>

<details>
<summary><strong>📅 Smart Timetable</strong></summary>

**Month view**
<img src="docs/screenshots/smart-timetable/schedule-month.webp" alt="Calendar month view with colored task blocks" width="100%" />

**Month view — day overflow popover**
<img src="docs/screenshots/smart-timetable/schedule-month-popover.webp" alt="Month view showing the day popover with all scheduled tasks for that day" width="100%" />

**Week view**
<img src="docs/screenshots/smart-timetable/schedule-week.webp" alt="Calendar week view showing time-blocked tasks with priorities" width="100%" />

**Day view**
<img src="docs/screenshots/smart-timetable/schedule-day.webp" alt="Calendar day view with hourly slots and injected routines" width="100%" />

**Agenda view**
<img src="docs/screenshots/smart-timetable/schedule-agenda.webp" alt="Agenda view listing upcoming scheduled tasks" width="100%" />

**Custom task form**
<img src="docs/screenshots/smart-timetable/schedule-custom-task.webp" alt="Add custom task form with title, priority, and time estimates" width="100%" />

**Unscheduled tasks pool**
<img src="docs/screenshots/smart-timetable/schedule-unscheduled.webp" alt="Draggable list of unscheduled custom tasks with priority filters" width="100%" />

**Scheduled task detail modal**
<img src="docs/screenshots/smart-timetable/schedule-custom-task-modal.webp" alt="Scheduled task detail modal showing date, time, and action buttons" width="100%" />

</details>

<details>
<summary><strong>🎯 Milestone Tracker</strong></summary>

**Creation form**
<img src="docs/screenshots/milestone-tracker/milestone-create.webp" alt="Milestone creation form with title, description, and due date" width="100%" />

**Progress ring and task list**
<img src="docs/screenshots/milestone-tracker/milestone-progress.webp" alt="Milestone card with animated circular progress ring and nested task list" width="100%" />

**Task with markdown details expanded**
<img src="docs/screenshots/milestone-tracker/milestone-expanded-task.webp" alt="Milestone task expanded to show markdown details and subtasks" width="100%" />

**AI strategic insights panel**
<img src="docs/screenshots/milestone-tracker/milestone-insights-panel.webp" alt="AI insights panel showing remaining work, performance assessment, tips, and next steps" width="100%" />

</details>

<details>
<summary><strong>🏆 Achievements & Profile</strong></summary>

**Full achievements page**
<img src="docs/screenshots/achievements-profile/achievements-page.webp" alt="Achievements page with XP level, streak, badge grid, and 'closest target' nudge" width="100%" />

**Profile — account details**
<img src="docs/screenshots/achievements-profile/profile-main.webp" alt="Profile page account details and edit controls" width="100%" />

**Profile — API key section**
<img src="docs/screenshots/achievements-profile/profile-api-key.webp" alt="Gemini API key section with status badge and save button" width="100%" />

**Profile — API key popover (from navbar)**
<img src="docs/screenshots/achievements-profile/profile-api-key-popover.webp" alt="API key popover menu with Manage in Profile and Clear Key actions" width="100%" />

**Profile — roadmap defaults (AI generation config)**
<img src="docs/screenshots/achievements-profile/profile-roadmap-prefs.webp" alt="Roadmap defaults panel with difficulty, language, learning style, pace, and weekend policy" width="100%" />

**Profile — lifestyle engine (habits + availability)**
<img src="docs/screenshots/achievements-profile/profile-lifestyle.webp" alt="Lifestyle configuration with energy peak, wake/sleep times, habits, and weekly availability grid" width="100%" />

**Profile — danger zone**
<img src="docs/screenshots/achievements-profile/profile-danger-zone.webp" alt="Danger zone with delete account action" width="100%" />

</details>

<details>
<summary><strong>🔐 Auth & Landing</strong></summary>

**Home — hero**
<img src="docs/screenshots/auth-landing/home-hero.webp" alt="MyMindMirror home page hero with animated background and call to action" width="100%" />

**Login**
<img src="docs/screenshots/auth-landing/login.webp" alt="Login page with split layout — image panel on the left, form on the right" width="100%" />

**Register**
<img src="docs/screenshots/auth-landing/register.webp" alt="Registration page with password strength indicator" width="100%" />

**Features Guide**
<img src="docs/screenshots/auth-landing/features-guide.webp" alt="Features guide with sticky sidebar navigation and accordion sections" width="100%" />

</details>

<details>
<summary><strong>📱 PWA & Offline Support</strong></summary>

**Install to home screen**
<img src="docs/screenshots/pwa-offline/pwa-install-prompt.webp" alt="PWA install prompt banner offering to add MyMindMirror as a standalone app" width="100%" />

**Offline detection**
<img src="docs/screenshots/pwa-offline/offline-banner.webp" alt="Offline banner shown at the top of the screen when the browser has no network connection" width="100%" />

</details>

<details>
<summary><strong>📱 Additional Mobile Views</strong></summary>

<table align="center">
<tr>
<td width="50%" align="center">
  <strong>Mobile — Analytics (Concern Frequency &amp; Scatter)</strong>
  <img src="docs/screenshots/additional-mobile/mobile-analytics-3.webp" alt="Mobile concern frequency and mood vs word count charts" width="100%" />
</td>
<td width="50%" align="center">
  <strong>Mobile — Analytics (Word Cloud &amp; Journal History)</strong>
  <img src="docs/screenshots/additional-mobile/mobile-analytics-4.webp" alt="Mobile key phrase cloud and journal history" width="100%" />
</td>
</tr>
</table>

</details>   

## Features

MyMindMirror combines nine integrated modules into a single privacy-first platform. Every feature works standalone, but together they form a closed loop: **journal → analyze → reflect → plan → execute → measure**.

### User-Facing Features

#### 🔐 Authentication
- **Register** — username, email, password with client-side strength meter
- **Login** — JWT-based, stateless
- **Change password** — re-encrypts all journal entries with the new key atomically
- **Delete account** — cascades through every table (journal, roadmaps, tasks, stats)

#### 📝 Secure Journaling
- **AES-CBC encryption at rest** — decrypted only in-memory during your session, never persisted in plaintext
- **Async AI analysis in ~3–5 seconds** — mood score, emotions, summary, growth tips, and key phrases
- **Live re-analysis on edit** — saving a change instantly resets AI fields and re-runs analysis
- **Zero-wait workflow** — navigate away while analysis runs; skeleton loaders auto-refresh
- **Rich markdown rendering** — AI-generated tips support headings, blockquotes, code blocks, and links

#### 📊 Three Dashboard Views
Three tabs, three analytics scopes:

- **Today** — AI reflection, emotion doughnut, and today's journal entries. The default landing view.
- **Weekly** — 7-day mood timeline, weekly emotion distribution, top concerns, and the week's entries in one scroll.
- **History (Overall)** — full 8-chart analytics suite (mood timeline, 14-axis radar, annual heatmap, correlation scatter, word cloud) plus a virtualized, searchable complete journal history.

Each view is optimized for its scope — Today is a snapshot, Weekly is a rhythm, History is the archive.

#### ✨ Daily Inspiration
- **30+ therapeutic writing prompts** — random question each day (e.g., *"What made you smile today?"*)
- **No repeats** — recent prompts tracked in `localStorage` and excluded from the next shuffle
- **One-click refresh** — spin for a new prompt without losing your draft

#### 🚨 Early Burnout Alerts
- **EWMA z-score detection** on mood and word count
- **Three severity tiers** — High Impact (rose), Moderate (amber), Noticeable (blue)
- **Stateful client-side dismissals** — acknowledged alerts suppressed across sessions
- **Rolling 30-day window** — no setup required
- 
#### 🔍 Four-Mode Search
- **Keyword** — plaintext substring across decrypted entries
- **Semantic** — 3072-dimensional pgvector cosine similarity; finds conceptually related entries even without keyword overlap
- **Mood** — range filter (-1.0 to +1.0)
- **Date** — window-based with inclusive boundaries
- All modes share a single results layout with adaptive routing (chronological vs. AI-relevance grouping)

#### 🧠 AI Reflection Coach (RAG)
- **Retrieval-augmented generation** over your own journal history — no generic chatbot responses
- **Redis-backed session memory** — last 10 messages, 1-hour sliding TTL
- **Memory ON/OFF toggle** — flow-state conversation or clean-slate each turn
- **Append vs. Replace modes** — keep the full chat thread visible or clear it per response
- **Suggestion chips** for quick-start prompts
- **One-click clear memory** — fires explicit Redis purge
- **Copy-to-clipboard** on every AI response
- **Session-scoped question caching** — reflective questions cached for 1 hour to avoid redundant LLM calls
- **Auto-growing input** — textarea expands as you type; Enter sends, Shift+Enter adds a newline

#### 🗺️ AI Roadmap Planner
- **One-goal generation** — enter a target, get a multi-week plan
- **Chunked generation** — "Load Next 6 Weeks" appends without losing scroll position
- **Dual view modes**:
    - **List View** — deeply nested accordions with expand-all / collapse-all
    - **Timeline View** — horizontal CSS Grid with sticky week headers
- **Deep task elaboration** — expands one line into a step-by-step guide with 4–6 subtasks, common pitfalls, and time estimates
- **Smart Reschedule** — mathematically shifts pending tasks based on completion rate
- **Continue from progress** — generates next steps based on what's actually completed
- **1-click import to Milestones** — auto-assigns +7-day due date
- **Language and style mirroring** — responds in the same language and tone as your input (supports code-switching like Hinglish)

#### 🎯 Milestone Tracker
- **Animated circular progress rings** per milestone
- **Auto color-shift**: red < 25% · amber > 25% · blue > 75% · emerald at 100%
- **AI Strategic Insights** — performance assessment, remaining work, tips, encouragement, suggested next tasks
- **Cross-module sync** — imported tasks from Roadmap and Journal growth tips appear with origin badges
- **Deep task editor** — dynamic subtask arrays (add, edit, remove inline)
- **Status auto-recalculation** — PENDING → IN_PROGRESS → COMPLETED → OVERDUE transitions driven by task state and due date

#### 📅 Smart Timetable
- **Lifestyle-aware scheduling** — reads energy peak, wake/sleep/lunch times, and daily habits
- **Drag-and-drop rescheduling** — move blocks on the calendar or drag from the unscheduled pool
- **"Re-optimize Today"** — recalculates only the remaining day, preserves tomorrow
- **Automatic block injection** — MEAL, BREAK, and ROUTINE blocks generated alongside work tasks
- **Deterministic fallback scheduler** — if AI is unavailable, a priority- and deadline-aware greedy scheduler runs instead
- **Manual scheduling** — bypass AI entirely for a specific task
- **Multi-view calendar** — Month, Week, Day, Agenda

#### 🏆 Gamification
- **16 unlockable badges** across 4 categories and 4 rarity tiers (Common / Rare / Epic / Legendary):
    - *Consistency* — 3-Day, 7-Day, 30-Day streaks
    - *Productivity* — First Step, Task Master, Productivity Ninja, Roadmap Finisher
    - *Introspection* — First Thought, Reflective Soul, First Chat, AI Whisperer
    - *Planning* — Time Lord, Visionary, Architect, Introspective, Deep Diver
- **XP levels** — 500 XP per level; different actions award different amounts (journaling = 50, roadmap = 30, chat = 15, etc.)
- **Streak tracking** — current and longest, auto-awarded badges at thresholds
- **"Closest Target" engine** — surfaces your next achievable badge based on current progress
- **Animated counter roll-ups** on the Achievements page

#### 📊 High-Resolution Analytics
Eight custom Chart.js engines, all theme-aware and exportable:

| Chart | Type | Purpose |
|---|---|---|
| Daily Emotion Breakdown | Doughnut | Today's aggregated emotional distribution |
| Top Primary Emotions | Gradient Bar | Top 7 strongest emotional drivers |
| Frequent Concerns | Frequency Bar | Top 25 AI-extracted life themes |
| Mood Timeline | Line (zoomable) | Mood score over time, `Ctrl+Scroll` to zoom |
| Annual Heatmap | Contribution Grid | 365-day mood intensity view |
| Emotion Radar | 14-axis Spider | Full emotional profile |
| Mood vs. Word Count | Scatter + Regression | Pearson correlation `r` with trend line |
| Key Phrase Cloud | Animated Float | Frequency-weighted, click-to-filter |

- **Client-side PNG export** per chart (Retina 2x, theme-aware background)
- **Panoramic dynamic width** — large datasets expand horizontally instead of squishing

#### 📤 Universal Data Export
- **PDF** — paginated A4 with clean markdown rendering, headers, and footers
- **CSV** — BOM-encoded UTF-8, markdown-stripped, Excel-safe
- **PNG** — per-chart high-resolution captures
- **No paywalls, no rate limits, no partial exports** — your data is yours

#### 🔐 Bring Your Own Key (BYOK)
- **Paste your own Gemini API key** for dedicated quotas and higher rate limits
- **AES-GCM encryption at rest** with an independent server master key
- **Masked status badge** (e.g., `••••••••12ab`) — plaintext never returned
- **Instant model cache eviction** on key update — new key takes effect immediately
- **Fallback to shared key** when no personal key is set

#### 📱 Installable PWA & Offline
- **Install to home screen** — runs in its own window on desktop and mobile
- **Offline banner** — auto-detects lost connection, disappears on return
- **Service worker caching** — fonts and static assets cached with `CacheFirst` strategy
- **Auto-update** — new build picked up on next page load

#### 📖 In-App Features Guide
- **19 sections** covering architecture, AI pipeline, security, testing, and deployment
- **Sticky sidebar navigation** with scroll-to-section on click
- **Collapsible accordions** — technical detail on demand
- **Written for both audiences** — product depth for evaluators, implementation detail for developers

### Engineering Highlights

#### Polyglot Persistence
Three databases, each chosen for a specific workload:

| Store | Role | Why |
|---|---|---|
| **MySQL 8** | Primary relational data | ACID, triggers, stored procedures, encrypted journal text |
| **PostgreSQL 16 + pgvector** | 3072-dim vector embeddings | Native cosine similarity, B-tree + GIN indexed metadata |
| **Redis 7** | Cache + ephemeral sessions | Sub-10ms reads, TTL-based chat memory, gamification stats |

#### Virtual Threads & Non-Blocking I/O
- `spring.threads.virtual.enabled=true` — Tomcat assigns a **virtual thread per request** instead of a platform thread
- When the code hits a blocking AI call, the virtual thread **unmounts** from the CPU, freeing the carrier thread
- Result: **thousands of concurrent AI requests** without exhausting the OS thread pool

#### AI-Out-of-Transaction Pattern
- All AI calls run **entirely outside any DB transaction**
- `TransactionTemplate` wraps **only the final DB write** in a short, atomic transaction
- Prevents connection pool starvation during the 5–10s AI wait
- Combined with virtual threads, this lets the server sustain heavy concurrent load

#### Model Cascade with Intelligent Cooldown
- **Per-task cascade of 4–6 models** — each `AITask` gets a prioritized fallback chain tuned to its workload:
    - `JOURNAL_ANALYSIS` → 6 models (starts with `gemini-3.5-flash-lite`, cascades through `gemini-3.1-flash-lite`, `gemini-3.6-flash`, `gemini-3.5-flash`, `gemini-2.5-flash`, `gemini-2.5-flash-lite`)
    - `ROADMAP_*` → 5 models (starting from `gemini-3.5-flash-lite`)
    - `TODAY_REFLECTION` / `REFLECTIVE_QUESTION` / `REFLECTION_CHAT` → 4 models (mixing Gemma and Gemini)
- **Dual-family strategy** — Gemma models (`gemma-4-31b-it`, `gemma-4-26b-a4b-it`) handle plain-text generation; Gemini models handle structured JSON and complex reasoning
- **Cooldown strategy**:
    - Rate limit / parsing error → locked for **60 seconds**
    - 404 Not Found (deprecated model) → locked for **24 hours**
    - Success → cleared
- **Fast-path skip** via `ConcurrentHashMap` — cooldown check is O(1) and pre-flight
- **Thinking-model aware** — for models that emit internal reasoning tokens, the client always takes the **final result index** to surface the synthesized answer instead of intermediate thoughts
- **Zero user-visible interruption** on model failure
- 
#### Native Database Triggers & Procedures
- `daily_journal_summary` is **auto-maintained by MySQL triggers** on `INSERT`, `UPDATE`, and `DELETE` of `journal_entries`
- **Zero runtime aggregation** for analytics endpoints — the summary table is always pre-computed
- **Race-safe bootstrap** (`DatabaseBootstrap`):
    - Handles concurrent instance startup (catches MySQL error `1304` / `1359`)
    - Falls through when `information_schema.ROUTINES` is privilege-gated (managed MySQL on Aiven/RDS/PlanetScale)
    - **Fails loud** on genuine errors — a missing trigger means silent data corruption, so startup aborts

#### Structured Output via Spring AI
- `BeanOutputConverter<T>` dynamically **injects JSON schema boundaries** into system prompts
- LLM is mathematically forced to emit schema-compliant JSON
- **No regex parsing** of LLM output — deterministic mapping into Java Records
- Resilient to markdown fences, trailing commas, and illegal escapes

#### Centralized Cache Strategy
- All cache names defined once in `CacheConstants` — no magic strings
- Targeted `@CacheEvict` on every mutating endpoint
- `@Cacheable` on hot reads: gamification stats, user profile, key-phrase frequencies, API key status

#### Race-Safe Schema Bootstrap
- `DatabaseBootstrap` installs procedures and triggers Hibernate can't manage
- `ApplicationRunner` with `@Order(100)` — runs **after** Hibernate creates tables, **after** data backfills
- `ObjectProvider<DataSource>` chain handles dev / Testcontainers / H2 contexts
- Loud failure model: a broken bootstrap aborts startup rather than silently degrading

### Backend Platform

The infrastructure choices that make the API layer consistent and safe.

#### Custom `@CurrentUser` Argument Resolver
Every controller method receives the fully-hydrated authenticated `User` entity via a custom `HandlerMethodArgumentResolver` — no boilerplate lookups, no `SecurityContext` juggling in business logic. When a controller needs the current user, it declares `@CurrentUser User currentUser` and gets it. This eliminates the single most common source of duplication in authenticated Spring apps.

#### Centralized Date Handling (`DateUtil`)
All date-string parsing flows through a single static utility. This means every endpoint that accepts `startDate` / `endDate` returns the same error message on malformed input, uses the same inclusive/exclusive boundary conventions, and fails the same way on `start > end`. No repeated `try-catch DateTimeParseException` blocks scattered across controllers.

#### Cross-Entity Synchronization
Updates propagate automatically across related aggregates so the client never has to orchestrate them:

- Marking a `Task` complete → the linked `RoadmapTask` is marked complete too
- Deleting a `CustomTask` → every `ScheduledTask` pointing at it is deleted in the same transaction
- Completing a `ScheduledTask` linked to a custom task → the master `CustomTask` (and any sibling scheduled entries) are marked complete

Consistency holds even if the client only knows about one of the entities.

#### Ordered Bootstrap Runners
Four `ApplicationRunner` beans execute on every startup in a defined order via `@Order`:

1. `SummaryTableBackfillRunner` (`@Order(1)`) — one-time summary rebuild if the table is empty
2. `ExistingDataBackfillRunner` (`@Order(2)`) — optional word-count + summary backfill
3. `EmbeddingBackfillRunner` (`@Order(5)`) — optional vector embedding generation
4. `DatabaseBootstrap` (`@Order(100)`) — MySQL procedures and triggers, run last so all tables exist

Every runner is idempotent. Each is gated by an `enabled` flag so it can be turned on for one deploy, then off. The app boots cleanly on a fresh database, an existing database, or anything in between.

#### Typed Configuration Records
Configuration is bound to Java records via `@ConfigurationProperties` instead of scattered `@Value` annotations:

```java
@ConfigurationProperties(prefix = "app.datasource.mysql")
public record MySqlDataSourceProperties(
    String url, String username, String password, String driverClassName
) {}
```

Benefits: type safety at boot time, IDE autocomplete, fail-fast on missing properties, and a single source of truth for each configuration block. No runtime surprises from mistyped property names.

### Frontend Experience

Nine user-facing modules, one coherent SPA. Every interaction is optimized for perceived speed and daily ritual.

#### Real-Time Feedback & Optimistic UI
- **Optimistic mutations** — task completion, key clearing, and journal saves update the UI instantly, before the server responds. TanStack Query's `onMutate` lifecycle handles the cache update; failures roll back automatically.
- **Smart polling** — when a journal entry is awaiting AI analysis, the history query polls every 3 seconds until `moodScore` appears, then stops. Zero wasted requests when idle.
- **Skeleton loaders** — 15+ distinct skeleton variants match each surface's real layout. No generic spinners. The UI paints the "shape" of the incoming content, then swaps in real data.
- **Toast notifications** — Sonner handles success, error, and promise toasts with deduplication (`id: 'error-${status}-${message}'`). No stacking spam from repeated failures.

#### Adaptive DOM Virtualization
Rendering thousands of rich journal entries with embedded charts would crash a mobile browser's RAM. The `JournalHistory` component uses a **dynamic rendering router**:

- **Under 20 items** (Today / Weekly tabs) → standard React mapping. Small lists don't need virtualization, and the standard approach avoids the absolute-positioning bugs that virtualizers can introduce.
- **Over 20 items** (History tab) → `@tanstack/react-virtual` calculates scroll offsets and paints *only* the visible rows onto the GPU.

The result: full-history scroll stays smooth on mobile even with thousands of entries.

#### Motion & Micro-Interactions
- **Framer Motion entry animations** — cards, modals, and expanded content fade in with `easeOut` timing. Not decoration — it signals "new content here" as you scroll.
- **Hover elevation** — cards lift by 2px on hover with shadow deepening. Universal pattern across every feature.
- **Chart animations** — Chart.js renders with staggered entry timing. Badge unlock animations trigger on state change.
- **Live counter roll-ups** — the Achievements page animates XP, streak, and task counts from zero to actual values on load.

#### Dark / Light Theme System
- **Toggle in the header** — one click, no page reload.
- **System preference sync** — auto-detects your OS theme on first visit via `matchMedia('(prefers-color-scheme: dark)')`.
- **Persistent choice** — the toggle writes to `localStorage`, so your preference survives reloads.
- **Full theme coverage** — every component, chart, and modal responds to the theme. No unstyled corners.
- **Native element sync** — `colorScheme` is set on `<html>` so browser chrome (scrollbars, form controls) matches the theme natively.

#### Responsive Design (Mobile-First)
- **Breakpoint-driven layouts** — every surface adapts from 440px mobile to 1920px desktop. No horizontal scroll, no overlapping UI.
- **Touch-optimized targets** — navigation, tabs, and drag handles meet 44px minimum tap size.
- **Collapsible mobile drawer** — the desktop navbar collapses into a slide-in drawer with a floating toggle button.
- **Adaptive grids** — 3-column desktop → 2-column tablet → 1-column mobile on every card layout.

#### PWA & Offline Support
- **Installable as a native app** — the browser prompt offers "Add to home screen" on both desktop and mobile. Installed, it launches in its own window with no browser chrome.
- **Offline detection** — a banner appears when the browser loses connection and disappears (with a toast) when it returns.
- **Service worker caching** — Google Fonts and static assets are cached with a `CacheFirst` strategy. Repeat visits load instantly even on slow connections.
- **Auto-update** — the service worker uses `registerType: 'autoUpdate'`, so users always get the latest build on their next page load — no manual refresh prompts.

#### Keyboard Shortcuts
- **Ctrl+Enter / Cmd+Enter** — submits the journal entry from anywhere in the textarea.
- **Escape** — closes the API key popover.
- **Tab navigation** — every interactive element is keyboard-reachable with visible focus rings.

#### Accessibility
- **Keyboard navigation** — every interactive element reachable via Tab with visible focus rings
- **ARIA attributes** — `aria-expanded` on popovers, `aria-haspopup` on menus, `aria-label` on icon-only buttons
- **Semantic HTML** — buttons are `<button>`, links are `<a>`, forms have labeled inputs
- **Reduced motion respect** — heavy animations respect `prefers-reduced-motion`
- **Color contrast** — all text meets WCAG AA against its background

#### Layered Frontend Design System
A strict **3-layer elevation palette** is enforced app-wide:

| Layer | Dark mode | Light mode | Role |
|---|---|---|---|
| **L1** | `#1A162F` | `white` | Cards, modals, header |
| **L2** | `#131127` | `slate-50` | Inner sections, panels |
| **L3** | `black/20` | `white` | Deep content (task rows, inputs) |

Mathematical depth instead of arbitrary shadow values. Consistent contrast ratios across light and dark themes. Every component references the same palette tokens — no rogue colors.

- **Single source of truth for chart colors** — emotion palettes, status tags, and concern colors are defined once and reused everywhere.
- **Consistent icon language** — Lucide icons at fixed sizes, no mixed icon libraries.

#### Daily Inspiration Widget
- **Curated prompt pool** — 30+ therapeutic writing prompts (e.g., *"What made you smile today?"*, *"Describe a moment when you felt truly at peace."*).
- **No repeats** — recently used prompts are stored in `localStorage` and excluded from the next shuffle. You see a fresh question every day.
- **One-click refresh** — spin the icon to load a new prompt without losing your current draft.

> **See it in action:** [Themes & Responsive Design](#themes--responsive-design) in the Screenshots section shows the app in both light and dark modes, plus mobile layouts at 440px.
 
## Architecture

MyMindMirror is a **polyglot, layered system** — three databases, one backend, one frontend, all coordinated through a shared domain model. The sections below walk through the runtime topology, then dig into the two subsystems that carry the most architectural weight: the **polyglot data layer** and the **AI pipeline**.

### High-Level Diagram

```mermaid
graph TB
    subgraph Client["Client — Browser / PWA"]
        UI[React 19 SPA<br/>Vite 7 + Tailwind 4]
    end

    subgraph Backend["Backend — Spring Boot 4 / Java 25"]
        API[REST Controllers<br/>+ JWT filter]
        SVC[Service Layer<br/>+ Virtual Threads]
        AI[DynamicAiClientService<br/>Model Cascade]
    end

    subgraph Data["Polyglot Data Layer"]
        MySQL[("MySQL 8<br/>Users · Journal<br/>Roadmaps · Tasks")]
        PG[("PostgreSQL 16<br/>pgvector · 3072-dim")]
        Redis[("Redis 7<br/>Cache · Sessions")]
    end

    subgraph External["External Services"]
        Google[Google AI<br/>Gemini + Gemma]
    end

    UI -->|HTTPS · REST · JWT| API
    API --> SVC

    SVC -->|JPA / Hibernate| MySQL
    SVC -->|Spring Data Redis| Redis
    SVC -->|Spring AI VectorStore| PG
    AI  -->|HTTPS / Model Cascade| Google

    MySQL -.->|Triggers maintain| Summary[(daily_journal_summary<br/>pre-aggregated)]
```

**Reading the diagram:**
- **Client → Backend:** stateless HTTPS with a JWT bearer token on every request except `/api/auth/**`
- **Backend → MySQL:** all transactional writes (users, journal entries, roadmaps, tasks, milestones, custom tasks, scheduled tasks)
- **Backend → Redis:** ephemeral chat sessions, cached DTOs (`userFullProfile`, `gamificationStats`, `apiKeyStatus`, `keyPhraseFrequencies`), per-user model instances (`userModelCache` inside `DynamicAiClientService`)
- **Backend → PostgreSQL/pgvector:** vector embeddings for semantic search and RAG retrieval
- **Backend → Google AI:** all LLM calls, routed through the per-task cascade in `DynamicAiClientService`
- **MySQL triggers → `daily_journal_summary`:** pre-aggregated per-day metrics, maintained by three stored procedures invoked from `after_journal_entry_{insert,update,delete}` triggers

### Polyglot Data Layer

Three databases, one job each. The split isn't for show — each engine was chosen for a specific access pattern that the others would handle poorly.

| Store | Role | Chosen for |
|---|---|---|
| **MySQL 8** | Primary relational engine | ACID transactions, JPA/Hibernate integration, stored procedures + triggers for pre-aggregation |
| **PostgreSQL 16 + pgvector** | Vector store | Native 3072-dim cosine similarity with B-tree + GIN indexes on metadata |
| **Redis 7** | Cache + ephemeral state | Fast in-memory reads, TTL-scoped chat sessions, per-user model instance cache |

#### What lives where

| Data | Store | Why |
|---|---|---|
| User accounts, credentials | MySQL | ACID, unique constraints, BCrypt password hash |
| Journal entries (encrypted) | MySQL | Same transaction as the AI analysis result |
| AI analysis results (mood, emotions, tips) | MySQL | Atomic write with the parent entry |
| Key phrases | MySQL | Foreign-key cascade on entry delete |
| Roadmaps, tasks, resources, milestones | MySQL | Relational graph with FK cascade |
| Milestones, tasks, custom tasks | MySQL | Same reason |
| Scheduled tasks | MySQL | Date-range queries with composite index |
| Daily journal aggregates (`daily_journal_summary`) | MySQL | **Auto-maintained by triggers** — no runtime aggregation |
| User stats (XP, streaks, badges) | MySQL | Persisted state; cached in Redis |
| Vector embeddings (3072-dim) | PostgreSQL/pgvector | Cosine similarity search, B-tree on `metadata->>'userId'`, GIN on metadata |
| Cached DTOs (profile, stats, key status) | Redis | Avoids repeated DB joins on hot reads |
| Chat session history | Redis | Ephemeral; 1-hour sliding TTL; max 10 messages per session |
| Per-user AI model instances | Redis-adjacent (in-memory `ConcurrentHashMap`) | Avoids re-instantiating Google SDK clients per request |

#### The `daily_journal_summary` pattern

Rather than aggregate `AVG(mood_score)` and `SUM(word_count)` on every analytics request, MyMindMirror pre-computes them at write time:

1. Three stored procedures in `DatabaseBootstrap.java`:
    - `recalc_daily_summary(user_id, date)` — full recalc for a given day
    - `insert_journal_entry_summary(...)` — incremental update on entry insert
    - `update_journal_entry_summary(...)` — handles date-change on entry update
2. Three triggers on `journal_entries`:
    - `after_journal_entry_insert` → calls `insert_journal_entry_summary`
    - `after_journal_entry_update` → calls `update_journal_entry_summary`
    - `after_journal_entry_delete` → calls `recalc_daily_summary`

The result: analytics reads hit a table that already contains the answer. No aggregation logic runs in the request path.

#### Vector schema layout

`VectorDatabaseConfig.java` sets up the pgvector store with:

- **Cosine distance** (`PgDistanceType.COSINE_DISTANCE`) — the standard metric for text embeddings
- **Exact search** (`PgIndexType.NONE`) — no approximate index; 100% recall. Suitable for the current dataset size; can be switched to HNSW/IVFFlat if scale demands
- **3072 dimensions** — matching the `text-embedding-004` output size
- **Two indexes created at startup** by `optimizeVectorDatabase()`:
    - `idx_vector_store_user_id` — B-tree expression index on `metadata->>'userId'`, for fast per-user filtering
    - `idx_vector_store_metadata_gin` — GIN index on `metadata::jsonb`, for arbitrary metadata queries

#### Why not one database?

A single MySQL instance would force a choice between three suboptimal options for vector search: storing embeddings as BLOBs and doing cosine similarity in application code (slow, no index), using MySQL 8's built-in `VECTOR` type (limited maturity, no equivalent to pgvector's metadata indexing), or rolling a custom ANN implementation. Each is measurably worse than letting PostgreSQL/pgvector handle it natively.

Similarly, keeping chat sessions and cached DTOs in MySQL would work but would add unnecessary write load on the primary relational engine — a workload Redis handles in-memory with TTL semantics built in.

### AI Pipeline

Every LLM call in MyMindMirror flows through a single gateway: `DynamicAiClientService`. This class is the only component that talks to Google's AI endpoints, and it owns four responsibilities: **model selection**, **cascade and cooldown**, **structured-output coercion**, and **per-user API key resolution**.

The rest of the backend never sees a raw `ChatModel` — services call one of two methods on the gateway and get back either plain text or a typed Java object.

#### Two entry points

```java
String generate(String prompt, UUID userId, AITask task);
<T> T generateStructured(String prompt, Class<T> responseType, UUID userId, AITask task);
```

- `generate` — for prose (reflections, chats, reflective questions). On total cascade exhaustion it returns a friendly fallback string rather than throwing.
- `generateStructured` — for JSON. On total cascade exhaustion it throws `ResponseStatusException(503)` with a message that depends on whether the user is on the shared key or their own key.

#### The task cascade

Each `AITask` value carries its own ordered list of models. The gateway walks that list, skipping any model currently in cooldown, until one succeeds or all fail.

**Cascade routing (as of October 2026):**

| Task | Models in cascade | Family |
|---|---|---|
| `TODAY_REFLECTION` | `gemma-4-31b-it` → `gemma-4-26b-a4b-it` → `gemini-3.6-flash` → `gemini-3.5-flash` | Gemma + Gemini |
| `REFLECTIVE_QUESTION` | `gemini-3.1-flash-lite` → `gemma-4-26b-a4b-it` → `gemini-3.6-flash` → `gemini-3.5-flash` | Gemma + Gemini |
| `REFLECTION_CHAT` | `gemini-3.5-flash` → `gemini-3.6-flash` → `gemini-3.1-pro` → `gemini-2.5-pro` | Gemini (reasoning) |
| `JOURNAL_ANALYSIS` | `gemini-3.5-flash-lite` → `gemini-3.1-flash-lite` → `gemini-3.6-flash` → `gemini-3.5-flash` → `gemini-2.5-flash` → `gemini-2.5-flash-lite` | Gemini (structured) |
| `ROADMAP_INITIAL` | 5 models (`gemini-3.5-flash-lite` → ... → `gemini-2.5-flash`) | Gemini (structured) |
| `ROADMAP_EXTENSION` | same 5 as above | Gemini (structured) |
| `ROADMAP_NEXT_STEPS` | same 5 | Gemini (structured) |
| `ROADMAP_ELABORATION` | same 5 | Gemini (structured) |
| `ROADMAP_RESCHEDULE` | same 5 | Gemini (structured) |
| `MILESTONE_INSIGHTS` | same 5 | Gemini (structured) |
| `PARSE_GROWTH_TIP` | same 5 | Gemini (structured) |
| `SCHEDULE_GENERATION` | same 5 | Gemini (structured) |
| `SCHEDULE_REOPTIMIZATION` | same 5 | Gemini (structured) |

**Why two families?** Gemma models are cost-efficient prose generators — they handle free-form text well and are used where output shape doesn't matter. Gemini models are used where output must be structurally valid JSON, or where the task requires deeper reasoning (chat with RAG context). The cascade order for each task reflects this: structured tasks start with `flash-lite` models (high quota, cheap) and cascade up to more capable models only when parsing fails.

#### Cooldown mechanism

When a model fails, it's placed in a `ConcurrentHashMap<String, Long>` cooldown map keyed by model name, with an expiration timestamp. The next call that tries to use that model sees the cooldown and skips it instantly — no network round trip, no wasted quota.

| Failure type | Cooldown duration |
|---|---|
| Rate limit (`429`) | **60 seconds** |
| JSON parse failure (structured tasks) | **60 seconds** |
| Empty response | **60 seconds** |
| `404 Not Found` (deprecated or removed model) | **24 hours** |
| Success | Cooldown cleared |

Once the cooldown window expires, the model is automatically restored to the routing pool — no manual intervention.

#### Structured-output coercion

For structured tasks, the raw prompt is augmented before it reaches the model:

```java
BeanOutputConverter<T> converter = new BeanOutputConverter<>(responseType);
String structuredPromptText = prompt
    + "\n\nCRITICAL INSTRUCTIONS FOR AI:\n"
    + "1. You MUST respond with a valid, perfectly formatted JSON object matching this schema:\n"
    + converter.getJsonSchema() + "\n"
    + "2. Return ONLY the raw JSON block. Do NOT use markdown formatting (like ```json).\n"
    + "3. Stop generating immediately after closing the final JSON bracket.";
```

The `BeanOutputConverter` reflects the target class (usually a Java record) and produces a JSON Schema describing its exact shape. That schema is injected into the prompt, and the model is additionally constrained at the API level:

```java
GoogleGenAiChatOptions jsonOptions = GoogleGenAiChatOptions.builder()
        .responseMimeType("application/json")
        .build();
```

**Two enforcement layers:** prompt-level (schema text) and API-level (`responseMimeType`). This makes the JSON contract a hard constraint rather than a polite request.

The response is then defensively parsed by `extractJson()`, which:

1. Strips markdown fences if the model ignored instructions (` ```json ` / ` ``` `)
2. Removes illegal JSON escape sequences that some models hallucinate (e.g., `\$` → `$`)
3. Locates the outermost matching `{...}` or `[...]` block via first-brace/last-brace scan
4. Returns the cleaned substring to the `BeanOutputConverter`, which deserializes it into the target record

**Thinking-model awareness.** Some models (notably Gemma variants) emit internal reasoning tokens before their final answer. The gateway always takes the **last result index** from the response — `response.getResults().get(lastIndex)` — so reasoning blocks are never surfaced to the user.

#### The RAG flow (Reflection Chat)

When you send a message to the AI Coach, the backend runs this pipeline:

```mermaid
flowchart TD
    Q([User query]) --> E["Embed via Spring AI VectorStore<br/>same model as write time"]
    E --> S["Cosine similarity search<br/>PostgreSQL / pgvector<br/>filter: userId, topK=5, threshold=0.50"]
    S --> D{Matches found?}
    D -->|Yes| I["Inject matched entries<br/>summary + emotions + key phrases"]
    D -->|No| F["Fall back to latest 5 entries"]
    I --> H["Append Redis chat history<br/>if sessionId is non-blank"]
    F --> H
    H --> G["Call generate prompt userId AITask.REFLECTION_CHAT"]
    G --> R(["Return AI answer<br/>optionally append to Redis session"])
```

**Legend:** Rounded rectangles are processing steps. The diamond is a branching decision. Arrows represent control flow.

**Threshold and top-K values** come from `application.properties`:

| Property | Value | Purpose |
|---|---|---|
| `app.rag.similarity-threshold` | `0.55` | Public `/search/semantic` endpoint |
| `app.rag.similarity-threshold-fallback` | `0.50` | Internal RAG for chat (slightly looser to maximize recall) |
| `app.rag.top-k` | `10` | Public semantic search |
| `app.rag.top-k-reflective` | `5` | Chat RAG retrieval |
| `app.rag.top-k-fallback` | `4` | Fallback when RAG misses |

Two thresholds exist because public search and internal RAG have different precision/recall trade-offs. Public search tolerates stricter filtering (users expect fewer, higher-quality matches); chat RAG prefers recall (the AI can discount irrelevant context but can't invent missing context).

#### Embedding generation

Embeddings are generated asynchronously in `EmbeddingGenerationService` after the analysis completes:

```java
@Async
public void generateAndStoreEmbedding(JournalEntry entry, UUID userId) {
    String textToEmbed = buildTextForEmbedding(entry);   // summary + key phrases
    Document doc = Document.builder()
            .id(entry.getId().toString())
            .text(textToEmbed)
            .metadata(Map.of(
                    "userId", userId.toString(),
                    "entryId", entry.getId().toString()))
            .build();
    vectorStore.add(List.of(doc));
}
```

`buildTextForEmbedding` combines the AI-generated **summary** and the extracted **key phrases** — not the raw journal text. This keeps embeddings small, cheap, and semantically dense.

Update and delete flows mirror this: `updateEmbedding` deletes the old document before inserting the new one, and `deleteEmbedding` runs when the entry is removed.

#### The async analysis flow (journal entries)

Journal entries are analyzed **after** the HTTP response returns. The controller saves the entry, schedules the analysis via `@Async`, and returns `201 Created` immediately — the client sees the response in ~50 ms while the AI runs in the background.

The async worker (`AsyncJournalAnalysisService.analyzeJournalEntryAsync`) does the following:

1. Runs on a **virtual thread** (Spring Boot 4 `@Async` with virtual thread executor)
2. Calls `aiClientService.generateStructured(prompt, JournalAnalysisResponse.class, userId, JOURNAL_ANALYSIS)` — **outside any transaction**
3. Opens a short, atomic transaction (`TransactionTemplate`) to persist the analysis results to the entry
4. On success, generates and stores the vector embedding
5. On AI failure, falls back to a deterministic stub response (`"AI analysis temporarily unavailable."`) so the entry still saves cleanly

The transaction boundary is deliberately narrow: **AI call first, DB write second**. Holding a DB connection during a 5–10 second LLM call would exhaust the connection pool under load; this pattern avoids it.

#### Reflection question caching

The AI-generated reflective question shown at the top of the AI Coach is cached in-memory for 5 minutes per user:

```java
private final Map<String, CachedQuestion> questionCache = new ConcurrentHashMap<>();

static class CachedQuestion {
    final String question;
    final long timestamp;
    boolean isExpired() { return System.currentTimeMillis() - timestamp > 5 * 60 * 1000; }
}
```

If you refresh the page, reload, or navigate away and back within 5 minutes, the same question is served from memory — no LLM call, no quota spent. After 5 minutes, the next request generates a fresh question.

#### Per-user API key resolution

Every call to `getChatModel(userId, modelName)` resolves the API key in this order:

1. Look up `userId` → `User` entity
2. If the user has an encrypted Gemini key set (`geminiApiKeyEncrypted`), decrypt it via `FieldEncryptionUtil` (AES-GCM) and use it
3. Otherwise, use the global shared key from `spring.ai.google.genai.api-key`
4. Cache the resulting `ChatModel` instance in `userModelCache` keyed by `userId + "_" + modelName`

When the user updates their key via `PUT /api/users/api-key`, the controller calls `dynamicAiClientService.evictUserModel(userId)` to clear all cached models for that user, forcing the next call to build a fresh `ChatModel` with the new key.

**Consequence:** if a user supplies their own key, their calls consume **their** quota, not the shared one. If the shared key is rate-limited, users with their own key are unaffected.

## Performance

MyMindMirror is designed around two constraints that don't apply to typical CRUD apps: **every AI call takes 3–10 seconds**, and **users expect the UI to feel instant**. The performance strategy reflects both — offload the slow work, cache the fast reads, and never block on either.

### Backend Performance

#### Virtual Threads (Java 25)
`spring.threads.virtual.enabled=true` makes Tomcat assign a **virtual thread per request** instead of a platform thread. When the code hits a blocking AI call, the virtual thread unmounts from the CPU — freeing the carrier thread for other work. Result: thousands of concurrent AI requests without exhausting the OS thread pool.

#### AI Calls Outside Transactions
Every method that calls the AI runs **outside** any database transaction. The backend uses `TransactionTemplate` to wrap only the final DB write in a short, atomic block. This prevents connection pool starvation during the 3–10 second AI wait.

#### N+1 Query Elimination
Complex object graphs use:

- **`@EntityGraph`** to fetch the primary collection (e.g., `tasks`) in a single JOIN-optimized query
- **`@BatchSize(50)`** to load secondary collections (e.g., `resources`, `milestones`) in batched `IN` clauses

A roadmap with 4 weeks of tasks, resources, and milestones loads in ~3 queries instead of ~50.

#### Batch Writes
The scheduler collects all generated tasks and calls `saveAll()` once, rather than `save()` in a loop. Combined with:

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

...hundreds of task insertions collapse into a single network round trip.

#### In-Memory Task Maps
The `ScheduleService` preloads all roadmap, milestone, and custom tasks into lightweight `Map<UUID, TaskInfo>` structures before scheduling begins. This eliminates thousands of individual `existsById` / `findById` calls inside the scheduling loop. Scheduling time dropped from ~30 seconds to under 1 second for a typical user.

#### Pre-Aggregated Daily Summary
`daily_journal_summary` is maintained by MySQL triggers on every `INSERT` / `UPDATE` / `DELETE` of `journal_entries`. Analytics endpoints read from this table directly — no runtime aggregation. Chart data for a 1-year window renders in <10 ms.

#### Composite Database Indexes
Every table is indexed on its query-critical columns:

| Table | Indexed columns |
|---|---|
| `journal_entries` | `(user_id, entry_date)`, `(user_id, mood_score)` |
| `tasks` | `(milestone_id)`, `(status)` |
| `scheduled_tasks` | `(user_id, scheduled_date)`, `(reminder_sent)` |
| `daily_journal_summary` | unique `(user_id, date)` |

#### Skip Decryption When Not Needed
Trend and word-cloud endpoints only read `keyPhrases` — they don't need the decrypted `rawText`. The `getEntriesForTrendsWithoutDecryption()` path skips the AES-CBC decryption step entirely. Cuts CPU load on trend queries by ~80%.

#### Hikari Connection Pool Tuning
The primary datasource is configured with:

- `maximumPoolSize=10` — prevents DB-side connection exhaustion
- `connectionTimeout=5000` — fail-fast if the DB is unreachable
- `maxLifetime=240000` — recycles connections before cloud provider timeouts

#### ConcurrentHashMap for Cooldowns
The AI gateway tracks model cooldowns in a `ConcurrentHashMap<String, Long>`. Cooldown checks are O(1) and lock-free. If a model is in cooldown, it's skipped instantly — no network call, no wasted quota.

#### Redis Cache Layer
Heavy read endpoints sit behind Redis with per-cache TTLs:

| Cache | Purpose |
|---|---|
| `userFullProfile` | Aggregated profile + preferences |
| `gamificationStats` | XP, level, badges |
| `apiKeyStatus` | Masked BYOK status |
| `keyPhraseFrequencies` | All-time phrase counts |
| `userPreferencesDto` | Roadmap defaults |

All cache names are centralized in `CacheConstants` — no magic strings, no missed evictions.

#### Gzip Response Compression
All JSON responses above 2 KB are compressed with Gzip via Tomcat's built-in compression filter:

```properties
server.compression.enabled=true
server.compression.mime-types=application/json,application/javascript,text/css,text/html,text/plain
server.compression.min-response-size=2048
```

Typical compression ratio for JSON API responses is **60–80%**. A 200 KB journal history payload ships as 40–80 KB over the wire — meaningful for mobile users on slow connections.

`min-response-size` is set to 2048 bytes so small responses (single objects, status codes) skip the compression overhead — Gzip has a fixed cost that isn't worth paying for tiny payloads.

### Frontend Performance

#### Server State with TanStack Query
Every backend call flows through TanStack Query with tuned defaults:

- `staleTime: 5 minutes` — data is fresh, no refetch on mount
- `gcTime: 10 minutes` — cache lives long enough for navigation
- **`invalidateQueries`** on mutations — precise cache invalidation, not blanket refetch
- **Background refetching** — the UI shows cached data immediately, then revalidates silently

#### Optimistic Mutations
Task completion, key clearing, and journal saves update the UI **before** the server responds. TanStack Query's `onMutate` lifecycle handles the cache update; failures roll back automatically. Perceived latency: zero.

#### Smart Polling (Not Interval Polling)
The journal history query uses a dynamic `refetchInterval`:

```js
refetchInterval: (query) => {
  const isAnalyzing = query.state.data?.some(entry => entry.moodScore === null);
  return isAnalyzing ? 3000 : false;  // poll only while analyzing
},
```

When no entries are analyzing, polling stops completely. No wasted requests, no battery drain.

#### Adaptive DOM Virtualization
`JournalHistory` uses a **dynamic rendering router**:

- **Under 20 items** → standard React mapping (small lists don't need virtualizers)
- **Over 20 items** → `@tanstack/react-virtual` computes absolute positions and paints only the visible rows

Full-history scroll stays smooth on mobile with thousands of entries.

#### Route-Level Code Splitting
Every page is lazy-loaded with `React.lazy()` + `<Suspense>`:

```jsx
const HomePage = lazy(() => import('./pages/HomePage'));
const JournalPage = lazy(() => import('./pages/JournalPage'));
const ProfilePage = lazy(() => import('./pages/ProfilePage'));
const AchievementsPage = lazy(() => import('./pages/AchievementsPage'));
```

The initial JS bundle ships only the shell + Home. Journal, Profile, and Achievements download on first navigation.

#### Component Memoization
Heavy components use `React.memo`, `useMemo`, and `useCallback`:

- All 8 chart components are wrapped in `React.memo`
- Expensive computations (linear regression, emotion aggregation) run inside `useMemo`
- Filter and sort callbacks are stabilized with `useCallback`

Re-renders happen only when data actually changes.

#### Chart.js (Canvas, Not SVG)
The analytics engine uses Chart.js, which renders to HTML5 **canvas** rather than SVG. This is a 5–10× performance win on dense charts (the mood timeline can plot 1,000+ points). Migration from Recharts removed significant DOM overhead.

#### Vite Production Optimizations
- **`esbuild.drop: ['console', 'debugger']`** — all `console.log` and `debugger` statements are stripped in production builds. Smaller bundle, no leaked debug output.
- **`cssCodeSplit: true`** — CSS is split per route, so visiting `/journal` doesn't download Profile styles.
- **`target: 'esnext'`** — modern syntax, smaller output (no transpilation to ES5).

#### Service Worker Caching
PWA service worker caches:

- All static assets on install (precached)
- Google Fonts with a `CacheFirst` strategy (1-year TTL)
- The app shell so repeat visits render instantly

Combined with nginx `Cache-Control: immutable` on hashed assets (`/assets/*`), the second visit is near-instant.

#### Toast Deduplication
Sonner uses deterministic IDs (`error-${status}-${message}`) so the same error firing from multiple axios interceptors stacks **one** toast, not ten. No visual spam from network hiccups.

#### Static Asset Preloading
The login page backgrounds are preloaded via `<link rel="preload">` in `index.html`:

```html
<link rel="preload" href="/login-bg-dark.webp" as="image" />
<link rel="preload" href="/login-bg-light.webp" as="image" />
```

Both themes are ready before the first paint. No flash when the image loads.

### What This Means in Practice

| Metric | Value |
|---|---|
| Journal history initial paint | <100 ms (skeleton) |
| AI analysis (async, background) | 3–10 seconds |
| Analytics dashboard render | <50 ms after data |
| Cache hit latency (Redis) | <10 ms |
| Full CI test run | ~5 minutes |

## Tech Stack

Every layer, every version, every reason. Nothing here is accidental — each dependency earns its place.

### Backend

| Layer | Tech | Version | Why |
|---|---|---|---|
| Language | Java | **25** | Virtual threads (stable), records, pattern matching, sealed interfaces |
| Framework | Spring Boot | **4.0.6** | Native virtual thread support, Spring AI integration, modern reactive stack |
| AI Integration | Spring AI | **2.0.0-M8** | Native Gemini client, `BeanOutputConverter` for structured output |
| Build tool | Maven | (wrapper) | Reproducible builds via `./mvnw` |
| Data access | Spring Data JPA + Hibernate | managed | Standard ORM, `@EntityGraph`, `@BatchSize` |
| Primary DB driver | MySQL Connector/J | managed | MySQL 8 compatible |
| Vector DB driver | PostgreSQL JDBC | managed | pgvector-compatible |
| Cache / sessions | Spring Data Redis | managed | Lettuce client |
| Security | Spring Security | managed | Stateless JWT filter chain |
| Validation | Jakarta Bean Validation | managed | `@Valid`, `@NotBlank`, `@Size` |
| JWT | JJWT | **0.11.5** | HS512 signing, standard claims API |
| Object mapping | MapStruct | **1.6.3** | Compile-time DTO mapping, zero reflection |
| Boilerplate | Lombok | managed | `@Data`, `@Builder`, `@RequiredArgsConstructor` |
| OpenAPI | SpringDoc OpenAPI | **3.1.1** | Swagger UI + `@Tag` / `@Operation` annotations |
| Monitoring | Spring Boot Actuator | managed | `/health`, `/info` (secured in prod) |
| Utilities | Jackson JSR-310 | managed | `LocalDate` / `LocalDateTime` serialization |
| Serialization | Jackson 2.x | managed | JSON for REST, Redis cache, and JJWT |

> **On Jackson 2 vs 3:** The Redis cache serializer (`GenericJackson2JsonRedisSerializer`) is marked deprecated in Spring Data Redis — the replacement targets Jackson 3. MyMindMirror stays on Jackson 2 because **JJWT 0.11.5 (the JWT library) has no Jackson 3 support yet**. The deprecation is suppressed in `CacheConfig.java` and `CacheConfigTest.java` with a comment. Migration is scheduled for when JJWT ships Jackson 3 compatibility.

### AI Models (via Google Gen AI)

*Current routing as of October 2026 — see `AITask.java` for the latest configuration.*

| Family | Model | Used for |
|---|---|---|
| Gemma | `gemma-4-31b-it` | Plain-text generation (reflections, questions) |
| Gemma | `gemma-4-26b-a4b-it` | Plain-text fallback + reflection questions |
| Gemini | `gemini-3.6-flash` | Structured JSON, reasoning fallback |
| Gemini | `gemini-3.5-flash` | Structured JSON, chat primary |
| Gemini | `gemini-3.5-flash-lite` | High-quota structured tasks |
| Gemini | `gemini-3.1-flash-lite` | Structured fallback |
| Gemini | `gemini-3.1-pro` | High-reasoning chat fallback |
| Gemini | `gemini-2.5-pro` | Reasoning last resort |
| Gemini | `gemini-2.5-flash` | Structured fallback |
| Gemini | `gemini-2.5-flash-lite` | Structured fallback |
| Embeddings | `text-embedding-004` | 3072-dim vectors for pgvector |

> The cascade above reflects the current routing. Model names are defined in `AITask.java` and evolve as Google releases new models. If this table and the enum diverge, `AITask.java` is authoritative.

**Model routing** is dynamic, not hard-coded. See the [AI Pipeline](#ai-pipeline) section for the per-task cascade.


### Databases

| Engine | Version | Role | Why |
|---|---|---|---|
| MySQL | **8.0** | Primary relational store | ACID, stored procedures, triggers, mature JDBC driver |
| PostgreSQL + pgvector | **pg16** | Vector store | Native 3072-dim cosine similarity, B-tree + GIN metadata indexes |
| Redis | **7-alpine** | Cache + ephemeral sessions | Sub-millisecond reads, TTL-scoped keys, no disk overhead |

### Frontend

| Layer | Tech | Version | Why |
|---|---|---|---|
| UI framework | React | **19.2.5** | Concurrent rendering, Suspense, automatic batching |
| Build tool | Vite | **7.0.0** | Fast HMR, native ESM, Rollup-based production builds |
| Styling | Tailwind CSS | **4.1.11** | Native Vite plugin, JIT, no PostCSS config needed |
| Routing | React Router | **7.6.3** | Nested routes, lazy loading |
| Server state | TanStack Query | **5.83.0** | Optimistic updates, background refetch, cache invalidation |
| DOM virtualization | TanStack Virtual | **3.14.9** | Recycled rendering for long journal histories |
| HTTP client | Axios | **1.10.0** | Interceptors for global auth + error handling |
| Charts | Chart.js | **4.5.0** | Canvas rendering, small footprint, plugin ecosystem |
| Chart React wrapper | react-chartjs-2 | **5.3.0** | Declarative wrapper for Chart.js |
| Chart zoom | chartjs-plugin-zoom | **2.2.0** | Ctrl+scroll zoom on timeline charts |
| Calendar | react-big-calendar | **1.19.4** | Month/Week/Day/Agenda views with drag-and-drop |
| Heatmap | react-calendar-heatmap | **1.10.0** | GitHub-style contribution grid |
| Progress rings | react-circular-progressbar | **2.2.0** | SVG circular progress with theme support |
| Animation | Framer Motion | **13.0.0** | Declarative entry animations, layout transitions |
| Icons | Lucide React | **0.525.0** | Tree-shakeable, consistent stroke weight |
| Toasts | Sonner | **2.0.7** | Minimal API, stackable notifications, promise toasts |
| Date utilities | date-fns | **4.1.0** | Tree-shakeable, immutable date functions |
| JWT decode | jwt-decode | **4.0.0** | Client-side token introspection |
| PDF export | jsPDF | **4.2.1** | Client-side PDF generation, no server round-trip |
| File saving | file-saver | **2.0.5** | Cross-browser file download |
| DOM → PNG | html-to-image | **1.11.13** | Chart export to Retina PNG |
| UUID generation | uuid | **13.0.0** | Client-side session IDs |
| PWA | vite-plugin-pwa | **1.2.0** | Service worker, offline shell caching |

### Testing Libraries

| Layer | Tech | Version | Why |
|---|---|---|---|
| Backend unit | JUnit 5 + Mockito | managed | Standard JVM test stack |
| Backend integration | Testcontainers | **2.0.5** | Real MySQL + Redis containers (no mocks) |
| Test containers — MySQL | testcontainers-mysql | 2.0.5 | `MySQLContainer` for integration tests |
| Spring Test | spring-boot-starter-test | managed | `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest` |
| Security test | spring-security-test | managed | `@WithMockUser`, mock principals |
| In-memory DB | H2 | managed | Fast slice tests (replaces MySQL for `@DataJpaTest` where applicable) |
| Coverage | JaCoCo | **0.8.15** | Reports excluded: `runner/`, `tools/`, `BackendApplication.class` |
| Frontend unit | Vitest | **4.1.2** | Native Vite integration, Jest-compatible API |
| Frontend component | Testing Library | **16.3.2** | User-centric queries, no implementation coupling |
| Frontend DOM | jsdom | **29.0.1** | Browser environment for Vitest |
| E2E | Playwright | **1.59.1** | Full browser automation |

### Infrastructure & DevOps

| Layer | Tech | Why |
|---|---|---|
| CI | GitHub Actions | `mvn -B verify` on every push/PR; JDK 25 (temurin); Maven cache |
| Containerization | Docker | Multi-stage builds (`build` → `runtime`) |
| Orchestration (local) | Docker Compose | One-command local stack |
| Base images | `eclipse-temurin:25-jre-alpine`, `node:22-alpine`, `nginx:alpine` | Small footprint, LTS |
| Coverage artifact | JaCoCo HTML | Uploaded by CI as build artifact |
| Test reports | Surefire (unit) + Failsafe (IT) | Split by `*Test.java` / `*IT.java` convention |

### Version Compatibility Note

This stack deliberately uses **bleeding-edge versions** — Spring Boot 4.0.6, Java 25, React 19, Vite 7, Tailwind 4. The trade-off is access to virtual threads, native Spring AI, and Tailwind's Vite plugin in exchange for a shorter ecosystem tail. All dependencies are pinned in `pom.xml` and `package.json`.

## Quick Start

Get a full MyMindMirror instance running locally in **under 5 minutes**. This is the recommended path for evaluation and self-hosting.

### Prerequisites

- **Docker** (20.10+) and **Docker Compose** (v2)
- **A Google AI Studio API key** ([get one free](https://aistudio.google.com/apikey)) — required for AI features
- **~4 GB free RAM** for the full stack (MySQL + PostgreSQL + Redis + backend + frontend)
- **~2 GB free disk** for Docker images

Verify your Docker install:

```bash
docker --version          # → Docker version 20.10.x or newer
docker compose version    # → Docker Compose version v2.x or newer
```

### 1. Clone the repository

```bash
git clone https://github.com/harman-04/mymindmirror.git
cd mymindmirror
```

### 2. Generate secrets

You need two cryptographically random secrets. Run these and **save the output**:

```bash
# JWT signing key — must be at least 512 bits for HS512
openssl rand -base64 64

# AES-GCM master key — must be exactly 32 bytes (256 bits)
openssl rand -base64 32
```

### 3. Create your `.env` file

```bash
cp .env.example .env
```

Open `.env` and fill in the following:

```properties
# Required — your Google AI Studio key
GEMINI_API_KEY=your-key-from-aistudio

# Required — paste the two outputs from step 2
JWT_SECRET=<paste the 64-byte output here>
ENCRYPTION_MASTER_KEY=<paste the 32-byte output here>

# Optional — defaults to 24 hours
JWT_EXPIRATION=86400000
```

**Do not commit `.env`.** It's already listed in `.gitignore`.

### 4. Start everything

```bash
docker compose up -d
```

This starts five containers:

| Container | Port | Purpose |
|---|---|---|
| `mymindmirror-mysql` | `3306` | Primary relational database |
| `mymindmirror-postgres` | `5432` | Vector database (pgvector) |
| `mymindmirror-redis` | `6379` | Cache + session storage |
| `mymindmirror-backend` | `8080` | Spring Boot API |
| `mymindmirror-frontend` | `80` | React SPA (served by nginx) |

Wait ~30 seconds for the databases to initialize and the backend to become healthy. Watch progress with:

```bash
docker compose logs -f backend
```

You should see `Started BackendApplication in X seconds` followed by `DatabaseBootstrap: procedures and triggers ensured.`

### 5. Open the app

| Surface | URL |
|---|---|
| **Frontend** | http://localhost |
| **Backend API** | http://localhost:8080/api |
| **Swagger UI** | Disabled in the Docker default (`SWAGGER_ENABLED=false`) |

Register a new account, log in, and start journaling.

### Stopping and cleaning up

```bash
# Stop the stack (keeps data)
docker compose down

# Stop and delete all data volumes (MySQL, PostgreSQL, Redis)
docker compose down -v
```

### Troubleshooting

**"Cannot connect to the Docker daemon"**
Docker Desktop is not running. Start it and retry.

**Backend container restarts repeatedly**
Check the logs: `docker compose logs backend`. The most common cause is a missing or malformed `JWT_SECRET` or `ENCRYPTION_MASTER_KEY`.

**"Table 'mymindmirror_db.users' doesn't exist"**
Hibernate hasn't finished creating the schema. Wait 20–30 seconds and refresh.

**Frontend shows "Network error. Cannot reach the server."**
The backend is still starting. Check `docker compose ps` — the `backend` service should show `healthy` status.

**AI features return empty results**
Verify your `GEMINI_API_KEY` is valid and has quota remaining. Check `docker compose logs backend | grep -i "gemini\|ai"` for errors.

## Manual Setup (for Contributors)

If you want to run the backend and frontend natively (not via Docker), or you're extending the codebase, follow this guide.

### Prerequisites

| Tool | Version | Verify |
|---|---|---|
| **JDK** | 25 (Temurin recommended) | `java -version` |
| **Maven** | 3.9+ (or use the wrapper) | `./mvnw -version` |
| **Node.js** | 22 LTS or newer | `node -v` |
| **npm** | 10+ | `npm -v` |
| **MySQL** | 8.0+ | `mysql --version` |
| **PostgreSQL** | 16+ with pgvector extension | `psql --version` |
| **Redis** | 7+ | `redis-cli --version` |
| **Docker** | (for spinning up the three databases easily) | `docker --version` |

### 1. Start the three databases

The fastest way is to use Docker just for the databases while running the backend and frontend natively:

```bash
docker run -d --name mm-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=mymindmirror_db \
  -p 3306:3306 \
  mysql:8.0

docker run -d --name mm-postgres \
  -e POSTGRES_DB=vectordb \
  -e POSTGRES_USER=vector \
  -e POSTGRES_PASSWORD=vectorpass \
  -p 5432:5432 \
  pgvector/pgvector:pg16

docker run -d --name mm-redis \
  -p 6379:6379 \
  redis:7-alpine
```

Verify all three are running:

```bash
docker ps --filter "name=mm-"
```

### 1b. MySQL server configuration (required)

MySQL 8.0 enables binary logging by default. This blocks creating stored procedures and triggers
unless one of the following is set:

- The app user has the `SUPER` privilege, OR
- The server variable `log_bin_trust_function_creators` is `1`

The Docker Compose stack (step 1) is already configured with the flag. But if you're running
against a **locally installed MySQL** instead of the Docker container, you must add it yourself.

**macOS (Homebrew):** edit `/opt/homebrew/etc/my.cnf`
**Linux (apt/yum):** edit `/etc/mysql/my.cnf` or `/etc/my.cnf`
**Windows:** edit `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`

Add under the `[mysqld]` section:

```ini
[mysqld]
log_bin_trust_function_creators = 1
```

Restart MySQL:

```bash
# macOS
brew services restart mysql

# Linux
sudo systemctl restart mysql
```

Verify:

```sql
SHOW VARIABLES LIKE 'log_bin_trust_function_creators';
-- Expected output: Value = ON
```

**Managed MySQL (Aiven, RDS, PlanetScale, DigitalOcean):** set this via the provider's console
or parameter group. It cannot be configured from application code or the JDBC URL.

**Symptom if missing:** the backend fails to start with
`SQLState HY000, error code 1419 — You do not have the SUPER privilege and binary logging is enabled`.

### 2. Configure the backend

Create a `.env` file at the **repository root** (same directory as `docker-compose.yml`):

```properties
GEMINI_API_KEY=your-google-ai-studio-key
JWT_SECRET=<openssl rand -base64 64>
JWT_EXPIRATION=86400000
ENCRYPTION_MASTER_KEY=<openssl rand -base64 32>

DATASOURCE_URL=jdbc:mysql://localhost:3306/mymindmirror_db
DATASOURCE_USERNAME=root
DATASOURCE_PASSWORD=root

VECTOR_DB_URL=jdbc:postgresql://localhost:5432/vectordb
VECTOR_DB_USERNAME=vector
VECTOR_DB_PASSWORD=vectorpass

REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_CACHE_TYPE=redis

CORS_ALLOWED_ORIGINS=http://localhost:5173
SWAGGER_ENABLED=true
```

### 3. Run the backend

```bash
cd backend/mymindmirror-backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

The backend starts on `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

**What happens on startup:**
1. Hibernate creates/updates the MySQL schema from the JPA entities (`ddl-auto=update` in the `dev` profile).
2. `DatabaseBootstrap` installs the stored procedures and triggers on `journal_entries` (idempotent, race-safe).
3. `VectorDatabaseConfig` creates the pgvector schema and indexes (B-tree on `userId`, GIN on metadata).
4. Backfill runners are skipped unless `backfill.enabled=true` or `rag.backfill.enabled=true`.

Look for these lines in the log to confirm a healthy startup:

```
Started BackendApplication in X.XXX seconds
DatabaseBootstrap: procedures and triggers ensured.
```

### 4. Run the frontend

In a **separate terminal**:

```bash
cd frontend/mymindmirror-app
npm install
npm run dev
```

The frontend starts on `http://localhost:5173`. Open it in a browser, register an account, and confirm the API is reachable (the login page should submit without a network error).

### 5. Running tests

**Backend — unit + integration tests (requires Docker for Testcontainers):**

```bash
cd backend/mymindmirror-backend
./mvnw verify
```

This runs:
- **Unit tests** (`*Test.java`) via Surefire
- **Integration tests** (`*IT.java`) via Failsafe, using Testcontainers to spin up real MySQL + Redis containers
- **JaCoCo coverage report** at `target/site/jacoco/index.html`

To run only unit tests (skip integration):

```bash
./mvnw test
```

To run only integration tests:

```bash
./mvnw failsafe:integration-test failsafe:verify
```

**Frontend — unit + component tests:**

```bash
cd frontend/mymindmirror-app
npm test
```

Or with the Vitest UI:

```bash
npm run test:ui
```

### 6. Common contributor workflows

**Rebuild the backend after code changes:**
Spring Boot DevTools is not enabled by default. To hot-reload, either re-run `./mvnw spring-boot:run` or add `spring-boot-devtools` to `pom.xml`.

**Rebuild the frontend after code changes:**
Vite HMR handles this automatically. Changes appear in the browser within ~100 ms.

**Reset the databases:**

```bash
docker stop mm-mysql mm-postgres mm-redis
docker rm mm-mysql mm-postgres mm-redis
# then re-run the docker run commands from step 1
```

**Inspect the MySQL schema directly:**

```bash
docker exec -it mm-mysql mysql -uroot -proot mymindmirror_db
```

**Inspect the pgvector store:**

```bash
docker exec -it mm-postgres psql -U vector -d vectordb
# then: SELECT COUNT(*) FROM vector_store;
```

### 7. IDE setup (recommended)

**IntelliJ IDEA / VS Code (Java):**
- Enable Lombok plugin / annotation processing
- Set the project SDK to JDK 25
- Import the Maven project to pick up MapStruct annotation processors

**VS Code (Frontend):**
- Install ESLint + Prettier extensions
- The workspace `.eslintrc` config is picked up automatically
## Environment Variables

MyMindMirror reads configuration from environment variables (via `.env` file import). The table below lists every variable, whether it's required, and its default.

### Required

| Variable | Default | Description | How to generate |
|---|---|---|---|
| `GEMINI_API_KEY` | — | Shared Google AI Studio API key for all users without their own BYOK key | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) |
| `JWT_SECRET` | — | Base64-encoded signing key for JWTs (HS512) | `openssl rand -base64 64` |
| `JWT_EXPIRATION` | — | JWT lifetime in milliseconds | e.g. `86400000` (24 hours) |
| `ENCRYPTION_MASTER_KEY` | — | Base64-encoded 32-byte AES-GCM key for encrypting stored API keys | `openssl rand -base64 32` |
| `VECTOR_DB_URL` | — | PostgreSQL JDBC URL for the pgvector store | `jdbc:postgresql://localhost:5432/vectordb` |
| `VECTOR_DB_USERNAME` | — | PostgreSQL username | `vector` |
| `VECTOR_DB_PASSWORD` | — | PostgreSQL password | `vectorpass` |

### Optional (with defaults)

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | Backend HTTP port |
| `DATASOURCE_URL` | `jdbc:mysql://localhost:3306/mymindmirror_db` | MySQL JDBC URL |
| `DATASOURCE_USERNAME` | `root` | MySQL username |
| `DATASOURCE_PASSWORD` | `root` | MySQL password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `SPRING_CACHE_TYPE` | `redis` | Cache backend (`redis` or `simple` for tests) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000,http://localhost` | Comma-separated list of allowed origins (no trailing slashes) |
| `SWAGGER_ENABLED` | `true` (dev) / `false` (prod profile) | Toggle the OpenAPI spec + Swagger UI |
| `SERVER_COMPRESSION_ENABLED` | `true` | Enable Gzip response compression |
| `SERVER_COMPRESSION_MIN_RESPONSE_SIZE` | `2048` | Minimum response size (bytes) before compression |
| `SERVER_COMPRESSION_MIME_TYPES` | `application/json,application/javascript,text/css,text/html,text/plain` | Comma-separated MIME types eligible for Gzip compression |

### Application-specific tuning (advanced)

These have sane defaults and rarely need changing.

| Variable | Default | Description |
|---|---|---|
| `app.rag.similarity-threshold` | `0.55` | Public semantic search threshold |
| `app.rag.similarity-threshold-fallback` | `0.50` | RAG retrieval threshold for chat |
| `app.rag.top-k` | `10` | Public semantic search result count |
| `app.rag.top-k-reflective` | `5` | Chat RAG retrieval count |
| `app.rag.top-k-fallback` | `4` | Fallback retrieval count |
| `app.anomaly.ewma-span` | `7` | EWMA window size for anomaly detection |
| `app.anomaly.mood-threshold-std` | `0.6` | Z-score threshold for mood anomalies |
| `app.anomaly.words-threshold-std` | `0.8` | Z-score threshold for word-count anomalies |
| `app.anomaly.min-days` | `7` | Minimum data points before anomaly detection activates |
| `app.error.expose-debug` | `false` | Include `debug_message` in error responses (dev only) |
| `rag.backfill.enabled` | `false` | Backfill vector embeddings on startup (one-time use) |
| `backfill.enabled` | `false` | Backfill word_count + daily summary (one-time use) |

### Frontend environment variables

The frontend reads from `import.meta.env.VITE_*` at build time. Currently only one is used:

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Base URL for API calls |

### `.env.example` template

A template file is provided at the repo root. Copy it to `.env` and fill in your values:

```properties
# ─── Required ────────────────────────────────────────────────
GEMINI_API_KEY=your-google-ai-studio-key
JWT_SECRET=your-64-byte-base64-secret
JWT_EXPIRATION=86400000
ENCRYPTION_MASTER_KEY=your-32-byte-base64-key

# ─── Databases ───────────────────────────────────────────────
DATASOURCE_URL=jdbc:mysql://localhost:3306/mymindmirror_db
DATASOURCE_USERNAME=root
DATASOURCE_PASSWORD=root

VECTOR_DB_URL=jdbc:postgresql://localhost:5432/vectordb
VECTOR_DB_USERNAME=vector
VECTOR_DB_PASSWORD=vectorpass

# ─── Cache ───────────────────────────────────────────────────
REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_CACHE_TYPE=redis

# ─── Security / CORS ─────────────────────────────────────────
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000,http://localhost

# ─── Optional ────────────────────────────────────────────────
SERVER_PORT=8080
SWAGGER_ENABLED=true
```

### How the `.env` file is loaded

Spring Boot imports `.env` at startup via:

```properties
spring.config.import=optional:file:.env[.properties]
```

The `optional:` prefix means the application starts even if `.env` is missing — it falls back to `application.properties` defaults. In the Docker Compose setup, environment variables are passed explicitly to the `backend` service, so `.env` is consumed by Docker Compose (not Spring Boot directly).

### Secrets hygiene

- **Never commit `.env`.** It's listed in `.gitignore`.
- **Use different secrets for dev, staging, and production.** A leaked dev secret is still a leaked secret.
- **Rotate `JWT_SECRET` and `ENCRYPTION_MASTER_KEY` if exposed.** Rotating `ENCRYPTION_MASTER_KEY` invalidates all stored Gemini API keys — users must re-enter them.
- **Never log env vars.** The codebase does not, but be careful if you add debug logging.

### Testing on a Physical Android Device

Test the full stack on a real Android phone over Wi-Fi without a USB cable, using ADB's reverse port tunneling.

#### Prerequisites (especially for Xiaomi / Poco / Redmi / HyperOS)

- **Same Wi-Fi network** — your laptop and phone must be on the same subnet
- **Developer Options enabled** — tap "Build number" 7 times in Settings → About phone
- Enable these in Developer Options:
    - `Wireless debugging`
    - `Disable adb authorization timeout` — prevents the connection from dropping after 30 minutes
    - `USB debugging (Security settings)` — required by MIUI/HyperOS to permit reverse tunneling

#### 1. Expose your local servers

Both servers must listen on all interfaces, not just localhost:

```bash
# Frontend
npm run dev -- --host 0.0.0.0
# → http://0.0.0.0:5173

# Backend — ensure application.properties has:
server.address=0.0.0.0
# → http://0.0.0.0:8080
```

#### 2. Pair and connect via ADB

Open a terminal in your Android SDK `platform-tools` directory:

```bash
# Restart the ADB daemon
adb kill-server
adb start-server

# Pair (use the IP:PORT shown on the phone's Wireless Debugging screen)
adb pair <PHONE_IP>:<PAIRING_PORT>
# Enter the 6-digit pairing code shown on the phone

# Connect (use the DIFFERENT port shown on the Wireless Debugging screen)
adb connect <PHONE_IP>:<DEBUG_PORT>
```

#### 3. Create reverse tunnels

Forward the phone's `localhost` ports back to your laptop:

```bash
adb -s <PHONE_IP>:<DEBUG_PORT> reverse tcp:5173 tcp:5173
adb -s <PHONE_IP>:<DEBUG_PORT> reverse tcp:8080 tcp:8080
```

#### 4. Open the app

On the phone, open Chrome and navigate to:

```
http://localhost:5173
```

The phone hits its own `localhost:5173`, which ADB tunnels back to your laptop. The Vite dev server's `/api` proxy then reaches the backend on `localhost:8080`.

#### 5. Cleanup

When done:

```bash
adb -s <PHONE_IP>:<DEBUG_PORT> reverse --remove-all
adb kill-server
```

#### Why this works

- **No CORS issues** — the phone sees the app on `localhost:5173` and the API on `localhost:8080`. Same-origin from the browser's perspective.
- **HTTPS not required** — Chrome treats `localhost` as a secure context regardless of protocol.
- **PWA install works** — Chrome can install the PWA on the phone since it's served from a secure origin.

**Note:** This workflow is for development. For a production build, deploy the frontend and backend to real hosts and use their public URLs.
## Project Structure

```
mymindmirror/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md               # Bug report issue template
│   │   └── feature_request.md          # Feature request issue template
│   ├── PULL_REQUEST_TEMPLATE.md        # PR checklist and description template
│   └── workflows/
│   └── workflows/
│       ├── backend-ci.yml              # Backend CI: mvn verify + JaCoCo artifact upload
│       └── frontend-ci.yml             # Frontend E2E: Playwright smoke test on PR
│
├── backend/
│   └── mymindmirror-backend/
│       ├── Dockerfile                  # Multi-stage: Maven build → Temurin JRE 25
│       ├── pom.xml                     # Spring Boot 4.0.6, Java 25, Spring AI 2.0.0-M8
│       ├── mvnw                        # Maven wrapper (Unix/macOS)
│       ├── mvnw.cmd                    # Maven wrapper (Windows)
│       ├── .mvn/
│       │   └── wrapper/
│       │       └── maven-wrapper.properties
│       └── src/
│           ├── main/
│           │   ├── java/com/mymindmirror/backend/
│           │   │   ├── BackendApplication.java
│           │   │   │
│           │   │   ├── annotation/      # Custom @CurrentUser parameter annotation
│           │   │   │
│           │   │   ├── api/             # OpenAPI contract interfaces
│           │   │   │   ├── AnalyticsApi.java
│           │   │   │   ├── ApiKeyApi.java
│           │   │   │   ├── AuthApi.java
│           │   │   │   ├── CustomTaskApi.java
│           │   │   │   ├── GamificationApi.java
│           │   │   │   ├── JournalApi.java
│           │   │   │   ├── MilestoneApi.java
│           │   │   │   ├── ReflectionApi.java
│           │   │   │   ├── ReflectionChatApi.java
│           │   │   │   ├── RoadmapApi.java
│           │   │   │   ├── ScheduleApi.java
│           │   │   │   ├── TaskApi.java
│           │   │   │   └── UserApi.java
│           │   │   │
│           │   │   ├── config/          # Spring configuration beans
│           │   │   │   ├── CacheConfig.java               # Redis serializer + cache manager
│           │   │   │   ├── MySqlDatabaseConfig.java       # Primary datasource (@Profile("!test"))
│           │   │   │   ├── OpenApiConfig.java             # Swagger + JWT security scheme
│           │   │   │   ├── VectorDatabaseConfig.java      # pgvector store + startup indexes
│           │   │   │   ├── WebMvcConfig.java              # Registers CurrentUserArgumentResolver
│           │   │   │   └── properties/                    # @ConfigurationProperties records
│           │   │   │
│           │   │   ├── constants/
│           │   │   │   └── CacheConstants.java            # All cache names centralized
│           │   │   │
│           │   │   ├── controller/      # Spring MVC controllers (thin, delegate to services)
│           │   │   │   ├── AnalyticsController.java
│           │   │   │   ├── ApiKeyController.java
│           │   │   │   ├── AuthController.java
│           │   │   │   ├── CustomTaskController.java
│           │   │   │   ├── GamificationController.java
│           │   │   │   ├── JournalController.java
│           │   │   │   ├── MilestoneController.java
│           │   │   │   ├── ReflectionChatController.java
│           │   │   │   ├── ReflectionController.java
│           │   │   │   ├── RoadmapController.java
│           │   │   │   ├── ScheduleController.java
│           │   │   │   ├── TaskController.java
│           │   │   │   └── UserController.java
│           │   │   │
│           │   │   ├── enums/           # Domain enums
│           │   │   │   ├── AITask.java                    # Model cascade per task
│           │   │   │   ├── GamificationAction.java        # XP triggers
│           │   │   │   ├── InsightStatus.java
│           │   │   │   └── Status.java                    # PENDING/IN_PROGRESS/COMPLETED/...
│           │   │   │
│           │   │   ├── exception/
│           │   │   │   └── RestExceptionHandler.java      # Unified ErrorResponse shape
│           │   │   │
│           │   │   ├── mapper/          # MapStruct DTO mappers
│           │   │   │   ├── CustomTaskMapper.java
│           │   │   │   ├── JournalMapper.java
│           │   │   │   ├── JsonMapperHelper.java          # JSON ⇄ List/Map helpers
│           │   │   │   ├── MilestoneMapper.java
│           │   │   │   ├── RoadmapMapper.java
│           │   │   │   ├── ScheduleMapper.java
│           │   │   │   ├── TaskMapper.java
│           │   │   │   └── UserMapper.java
│           │   │   │
│           │   │   ├── model/           # JPA entities
│           │   │   │   ├── CustomTask.java
│           │   │   │   ├── DailyJournalSummary.java       # Pre-aggregated table
│           │   │   │   ├── JournalEntry.java              # AES-CBC encrypted raw text
│           │   │   │   ├── KeyPhrase.java
│           │   │   │   ├── Milestone.java
│           │   │   │   ├── Roadmap.java
│           │   │   │   ├── RoadmapMilestone.java
│           │   │   │   ├── RoadmapResource.java
│           │   │   │   ├── RoadmapTask.java
│           │   │   │   ├── ScheduledTask.java
│           │   │   │   ├── Task.java
│           │   │   │   ├── User.java
│           │   │   │   ├── UserPreferences.java           # Lifestyle: wake/sleep/lunch, habits
│           │   │   │   ├── UserRoadmapPreferences.java    # Difficulty, language, pace
│           │   │   │   └── UserStats.java                 # XP, streaks, badges
│           │   │   │
│           │   │   ├── payload/
│           │   │   │   ├── request/     # Java records for inbound JSON
│           │   │   │   └── response/    # Java records for outbound JSON
│           │   │   │
│           │   │   ├── repository/      # Spring Data JPA repositories
│           │   │   │   ├── CustomTaskRepository.java
│           │   │   │   ├── DailyJournalSummaryRepository.java  # Custom upsert query
│           │   │   │   ├── JournalEntryRepository.java         # @EntityGraph, custom JPQL
│           │   │   │   ├── MilestoneRepository.java
│           │   │   │   ├── RoadmapRepository.java
│           │   │   │   ├── RoadmapTaskRepository.java
│           │   │   │   ├── ScheduledTaskRepository.java
│           │   │   │   ├── TaskRepository.java
│           │   │   │   ├── UserPreferencesRepository.java
│           │   │   │   ├── UserRepository.java
│           │   │   │   ├── UserRoadmapPreferencesRepository.java
│           │   │   │   └── UserStatsRepository.java
│           │   │   │
│           │   │   ├── resolver/
│           │   │   │   └── CurrentUserArgumentResolver.java    # Injects User into controllers
│           │   │   │
│           │   │   ├── runner/          # ApplicationRunner beans (startup tasks)
│           │   │   │   ├── DatabaseBootstrap.java              # Installs procedures + triggers
│           │   │   │   ├── EmbeddingBackfillRunner.java        # Optional vector backfill
│           │   │   │   ├── ExistingDataBackfillRunner.java     # Optional word_count backfill
│           │   │   │   └── SummaryTableBackfillRunner.java     # One-time summary rebuild
│           │   │   │
│           │   │   ├── security/        # JWT filter chain
│           │   │   │   ├── JwtAccessDeniedHandler.java
│           │   │   │   ├── JwtAuthenticationEntryPoint.java
│           │   │   │   ├── JwtRequestFilter.java
│           │   │   │   ├── JwtUtil.java
│           │   │   │   ├── WebSecurityConfig.java
│           │   │   │   └── services/
│           │   │   │       └── UserDetailsImpl.java
│           │   │   │
│           │   │   ├── service/         # Business logic
│           │   │   │   ├── AnomalyDetectionService.java        # EWMA z-score detector
│           │   │   │   ├── ApiKeyService.java                  # BYOK storage + status
│           │   │   │   ├── AsyncJournalAnalysisService.java    # @Async AI analysis
│           │   │   │   ├── ChatMemoryService.java              # Redis session memory
│           │   │   │   ├── CustomTaskService.java
│           │   │   │   ├── EmbeddingGenerationService.java     # Vector embed + store
│           │   │   │   ├── GamificationService.java            # XP, badges, streaks
│           │   │   │   ├── JournalPersistenceService.java      # Encrypted CRUD
│           │   │   │   ├── JournalReflectionService.java       # RAG chat + reflections
│           │   │   │   ├── JournalSearchService.java           # Keyword/semantic/mood
│           │   │   │   ├── JournalService.java                 # Facade
│           │   │   │   ├── MilestoneInsightService.java        # AI milestone insights
│           │   │   │   ├── MilestoneService.java
│           │   │   │   ├── RoadmapService.java                 # AI roadmap generation
│           │   │   │   ├── ScheduleService.java                # AI scheduling + fallback
│           │   │   │   ├── TaskService.java
│           │   │   │   ├── UserDetailsServiceImpl.java
│           │   │   │   ├── UserService.java
│           │   │   │   └── ai/
│           │   │   │       └── DynamicAiClientService.java     # Model cascade + cooldown
│           │   │   │
│           │   │   ├── tools/
│           │   │   │   └── KeyGenerator.java                   # Dev-only secret generator
│           │   │   │
│           │   │   └── util/
│           │   │       ├── DateRange.java
│           │   │       ├── DateUtil.java                       # Centralized date parsing
│           │   │       ├── EncryptionUtil.java                 # AES-CBC for journal text
│           │   │       └── FieldEncryptionUtil.java            # AES-GCM for API keys
│           │   │
│           │   └── resources/
│           │       ├── application.properties                  # Base config
│           │       ├── application-dev.properties              # Verbose logging, debug on
│           │       ├── application-prod.properties             # Quiet, Swagger off
│           │       └── application-test.properties             # Test secrets, H2 fallback
│           │
│           └── test/
│               └── java/com/mymindmirror/backend/
│                   ├── AbstractIntegrationTest.java            # Base: MySQL + Redis containers
│                   ├── AsyncJournalAnalysisIT.java
│                   ├── AuthFlowIT.java
│                   ├── CacheBehaviorIT.java
│                   ├── ErrorContractIT.java
│                   ├── GamificationFlowIT.java
│                   ├── JournalLifecycleIT.java
│                   ├── MilestoneInsightFlowIT.java
│                   ├── MultiTenantIsolationIT.java
│                   ├── ProfileFlowIT.java
│                   ├── RoadmapFlowIT.java
│                   ├── ScheduleFlowIT.java
│                   ├── config/                                 # Config unit tests
│                   ├── controller/                             # @WebMvcTest controller slices
│                   ├── exception/                              # Exception handler tests
│                   ├── mapper/                                 # MapStruct mapper tests
│                   ├── model/                                  # Entity logic tests
│                   ├── repository/                             # @DataJpaTest repository tests
│                   ├── resolver/                               # Argument resolver tests
│                   ├── security/                               # JWT filter + entry point tests
│                   ├── service/                                # Service unit tests
│                   └── util/                                   # Encryption + date utils
│
├── frontend/
│   └── mymindmirror-app/
│       ├── .gitignore
│       ├── Dockerfile                  # Multi-stage: Node 22 build → nginx serve
│       ├── README.md                   # Frontend-only guide (scripts, structure, conventions)
│       ├── nginx.conf                  # SPA fallback + /api proxy to backend
│       ├── package.json                # React 19, Vite 7, Tailwind 4
│       ├── package-lock.json
│       ├── vite.config.js               # Build + PWA config (PWA only in prod)
│       ├── playwright.config.js         # E2E test runner
│       ├── eslint.config.js
│       ├── public/                      # Static assets served at root
│       │   ├── icon.png                 
│       │   ├── logo.png
│       │   ├── login-bg-dark.webp
│       │   └── login-bg-light.webp
│       ├── index.html                  # Meta tags, fonts, theme-color
│       └── src/
│           ├── main.jsx                # Root: ThemeProvider + Router + QueryClient
│           ├── App.jsx                 # Layout shell, routing, auth state
│           ├── index.css               # Tailwind import + global scrollbars + markdown styles
│           │
│           ├── components/             # 40+ reusable components
│           │   ├── AppLogo.jsx
│           │   ├── AchievementsWidget.jsx
│           │   ├── AnomalyAlerts.jsx
│           │   ├── AverageEmotionChart.jsx
│           │   ├── ConcernFrequencyChart.jsx
│           │   ├── ConfirmationModal.jsx
│           │   ├── DailyEmotionSnapshot.jsx
│           │   ├── DailyInspiration.jsx
│           │   ├── DownloadChartButton.jsx
│           │   ├── EmotionRadarChart.jsx
│           │   ├── ExportButtons.jsx
│           │   ├── FadeIn.jsx
│           │   ├── HeaderApiKeyStatus.jsx
│           │   ├── InfiniteScrollTrigger.jsx
│           │   ├── InstallPrompt.jsx
│           │   ├── JournalHistory.jsx          # Virtualizer + modals + rich rendering
│           │   ├── JournalInput.jsx
│           │   ├── JournalSearch.jsx           # 4-mode search UI
│           │   ├── KeyPhraseCloud.jsx
│           │   ├── MilestoneTracker.jsx
│           │   ├── MoodCalendarHeatmap.jsx
│           │   ├── MoodChart.jsx
│           │   ├── MoodWordCountChart.jsx
│           │   ├── OfflineBanner.jsx
│           │   ├── OverallDashboard.jsx
│           │   ├── PremiumInput.jsx            # Design-system input
│           │   ├── ReflectionChat.jsx
│           │   ├── RoadmapPlanner.jsx
│           │   ├── RoadmapTimeline.jsx
│           │   ├── ScheduleTab.jsx             # Calendar + drag-and-drop
│           │   ├── Skeleton.jsx                # 15+ skeleton variants
│           │   ├── ThemeToggle.jsx
│           │   ├── TodayDashboard.jsx
│           │   ├── TodaysReflection.jsx
│           │   └── WeeklyDashboard.jsx
│           │
│           ├── contexts/
│           │   └── ThemeContext.jsx             # Dark/light + system sync
│           │
│           ├── data/
│           │   └── writingPrompts.js            # Daily inspiration pool
│           │
│           ├── hooks/                           # TanStack Query hooks per domain
│           │   ├── useAnomalyData.js
│           │   ├── useAuth.js
│           │   ├── useGamification.js
│           │   ├── useJournalData.js            # Queries + mutations + polling
│           │   ├── useMilestoneData.js
│           │   ├── useReflectionChat.js
│           │   ├── useRoadmap.js
│           │   ├── useScheduleData.js
│           │   └── useUserProfile.js
│           │
│           ├── pages/                           # Route-level pages
│           │   ├── HomePage.jsx
│           │   ├── LoginPage.jsx
│           │   ├── RegisterPage.jsx
│           │   ├── JournalPage.jsx              # Main app shell with 8 tabs
│           │   ├── ProfilePage.jsx
│           │   ├── AchievementsPage.jsx
│           │   └── FeaturesGuide.jsx
│           │
│           ├── test/
│           │   └── setup.js                     # Vitest setup
│           │
│           └── utils/
│               ├── axiosConfig.js               # Interceptors + unified error toasts
│               ├── chartColors.js               # Per-emotion color palettes
│               └── downloadChart.js             # DOM → Retina PNG export
│
├── docs/
│   └── screenshots/                    # All README images, ~70 files across 12 folders
│       ├── main/                       # Curated hero + main section images
│       ├── achievements-profile/
│       ├── additional-mobile/
│       ├── analytics-charts/
│       ├── auth-landing/
│       ├── journaling-analysis/
│       ├── milestone-tracker/
│       ├── pwa-offline/
│       ├── roadmap-planner/
│       ├── search-modes/
│       ├── smart-timetable/
│       ├── themes-responsive/
│       └── RECAPTURE_NOTES.md          # Demo seed data + capture conventions
│
├── docker-compose.yml                  # 5 services: mysql, postgres, redis, backend, frontend
├── .env.example                        # Template for all required env vars
├── .gitignore
├── CODE_OF_CONDUCT.md              
├── CONTRIBUTING.md                
├── LICENSE                         
├── README.md                       
├── SECURITY.md    

```

### Directory conventions

- **Backend `api/` vs `controller/`:** `api/` holds OpenAPI contract interfaces (`@Tag`, `@Operation`, `@ApiResponse`) with no Spring MVC binding; `controller/` implements them and owns the actual `@GetMapping`, `@RequestBody`, `@CurrentUser` wiring. This keeps documentation separate from routing.
- **Backend `service/` vs `service/ai/`:** `service/ai/` holds the single AI gateway (`DynamicAiClientService`); everything else lives under `service/` and calls into it.
- **Backend `payload/request/` and `payload/response/`:** All inbound and outbound JSON is modeled as Java records — no classes, no setters, no accidental mutability.
- **Frontend `hooks/`:** One file per backend domain. Each exports queries and mutations for that domain, wrapping axios calls with TanStack Query semantics (optimistic updates, cache invalidation, polling intervals).
- **Frontend `components/`:** Flat directory. Components are named after the feature they render, not the layer they belong to. `Skeleton.jsx` exports 15+ skeleton variants as named exports.

## API Documentation

The backend exposes a REST API under `/api/**`. All endpoints (except auth) require a JWT bearer token. Every response follows the same unified error shape.

### Authentication Model

- **Stateless.** No sessions, no cookies. Every request carries `Authorization: Bearer <jwt>`.
- **Token lifetime:** 24 hours by default (configurable via `JWT_EXPIRATION`).
- **Signing:** HS512 with the server's `JWT_SECRET`.
- **Public endpoints:** Only `/api/auth/register` and `/api/auth/login`.
- **Protected endpoints:** Everything else.

### Obtaining a token

```bash
# Register a new account
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'
# → 201 Created

# Log in to get a token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'
# → 200 OK
# → { "token": "eyJhbGciOiJIUzUxMiJ9...", "message": "Login successful." }
```

Use the token on every subsequent request:

```bash
curl http://localhost:8080/api/journal/history \
  -H "Authorization: Bearer <your-token>"
```

### Endpoint Reference

Grouped by tag. All paths are relative to `/api`.

#### Authentication

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/auth/register` | Create a new account |
| `POST` | `/auth/login` | Authenticate and receive a JWT |

#### Journal

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/journal` | Create entry (fires async AI analysis) |
| `PUT` | `/journal/{id}` | Update entry text (triggers re-analysis if text changed) |
| `DELETE` | `/journal/{id}` | Delete entry and its embedding |
| `GET` | `/journal/{id}` | Fetch single entry (decrypted) |
| `GET` | `/journal/history` | List entries in date range (unpaginated) |
| `GET` | `/journal/history/paginated` | List entries with page metadata |
| `GET` | `/journal/mood-data` | Mood score timeline for charting |
| `GET` | `/journal/trends` | Top N key phrases by frequency |
| `GET` | `/journal/key-phrases` | All-time key phrase counts (cached) |
| `GET` | `/journal/search/keyword` | Substring search on decrypted text |
| `GET` | `/journal/search/mood` | Range filter on mood score |
| `GET` | `/journal/search/semantic` | 3072-dim vector similarity search |

#### Analytics

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/ml/daily-aggregated-data` | Pre-aggregated daily mood + word counts |
| `POST` | `/ml/anomaly-detection` | EWMA z-score anomaly detection on supplied data |

#### Reflection

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/reflection/generate` | AI daily reflection from today's entries |

#### Reflection Chat

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/chat/reflect` | RAG chat with AI coach over journal history |
| `POST` | `/chat/suggest-question` | Generate a personalized reflective question |
| `DELETE` | `/chat/clear-memory` | Clear Redis session memory for a session ID |

#### Roadmap

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/roadmap/generate` | AI-generate a personalized roadmap |
| `GET` | `/roadmap` | List all roadmaps for the user |
| `DELETE` | `/roadmap/{id}` | Delete roadmap + cascading tasks |
| `POST` | `/roadmap/import-task` | Copy a roadmap task into a milestone |
| `PATCH` | `/roadmap/task/{taskId}/toggle` | Toggle task completion |
| `POST` | `/roadmap/{id}/continue` | AI-generate 3 more weeks (progress-aware) |
| `POST` | `/roadmap/{id}/continue-batch` | Chunked continuation (up to 12 weeks) |
| `POST` | `/roadmap/task/{taskId}/elaborate` | AI-expand a task into a detailed guide |
| `POST` | `/roadmap/{id}/reschedule` | AI-reassign weeks for incomplete tasks |

#### Milestones

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/milestones` | Create a milestone |
| `GET` | `/milestones` | List all milestones for the user |
| `GET` | `/milestones/{id}` | Fetch single milestone with tasks |
| `PUT` | `/milestones/{id}` | Update milestone fields |
| `DELETE` | `/milestones/{id}` | Delete milestone + cascade tasks |
| `GET` | `/milestones/{id}/insights` | AI strategic insights for a milestone |
| `POST` | `/milestones/import-growth-tip` | Parse a growth tip into milestone tasks |

#### Milestone Tasks

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/milestones/{milestoneId}/tasks` | Create a task under a milestone |
| `GET` | `/milestones/{milestoneId}/tasks` | List all tasks for a milestone |
| `GET` | `/milestones/{milestoneId}/tasks/{taskId}` | Fetch single task |
| `PUT` | `/milestones/{milestoneId}/tasks/{taskId}` | Update task |
| `DELETE` | `/milestones/{milestoneId}/tasks/{taskId}` | Delete task |

#### Custom Tasks

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/custom-tasks` | List all custom tasks for the user |
| `POST` | `/custom-tasks` | Create a custom task |
| `PUT` | `/custom-tasks/{id}` | Update custom task |
| `DELETE` | `/custom-tasks/{id}` | Delete custom task + linked scheduled entries |

#### Schedule

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/schedule/generate` | AI schedule for next 3 days (fallback if AI unavailable) |
| `POST` | `/schedule/reoptimize` | Re-optimize remaining hours of today |
| `GET` | `/schedule/tasks` | Scheduled tasks in a date range |
| `PUT` | `/schedule/task/{taskId}/move` | Move scheduled task to a new date/time |
| `PATCH` | `/schedule/task/{taskId}/complete` | Mark scheduled task complete |
| `POST` | `/schedule/task/custom` | Manually schedule a custom task |

#### Gamification

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/gamification/stats` | XP, level, streaks, badges, counters |

#### User

| Method | Path | Purpose |
|---|---|---|
| `PUT` | `/users/profile` | Update username and/or email |
| `DELETE` | `/users/profile` | Delete account and all cascade data |
| `PUT` | `/users/profile/password` | Change password + re-encrypt journal entries |
| `GET` | `/users/profile-full` | Aggregated profile (identity + preferences + API key status) |
| `PUT` | `/users/roadmap-preferences` | Update roadmap defaults |
| `PUT` | `/users/preferences` | Update lifestyle preferences (hours, timezone, habits) |

#### API Key

| Method | Path | Purpose |
|---|---|---|
| `PUT` | `/users/api-key` | Set or clear the user's personal Gemini key |
| `GET` | `/users/api-key/status` | Get masked status of the stored key |

### Unified Error Response

Every error — from a validation failure to a JWT rejection to an infrastructure outage — returns this exact JSON shape:

```json
{
  "timestamp": "2026-10-08T14:32:11.482",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: email: Email should be valid",
  "path": "/api/auth/register",
  "debug_message": null
}
```

| Field | Type | Notes |
|---|---|---|
| `timestamp` | ISO 8601 | Server-side time the error was generated |
| `status` | Integer | HTTP status code (mirrors the response status) |
| `error` | String | HTTP reason phrase (`Bad Request`, `Unauthorized`, etc.) |
| `message` | String | Human-readable, client-safe message |
| `path` | String | Request URI that triggered the error |
| `debug_message` | String \| null | Only present when `app.error.expose-debug=true` (dev only) |

**Production behavior:** `debug_message` is stripped from all responses (`app.error.expose-debug=false`).

### Common Status Codes

| Code | Meaning | Common triggers |
|---|---|---|
| `200` | OK | Successful GET / PUT / PATCH / DELETE |
| `201` | Created | Successful POST that creates a resource |
| `204` | No Content | Successful DELETE or key-clearing PUT |
| `400` | Bad Request | Validation failure, malformed JSON, invalid date format |
| `401` | Unauthorized | Missing/invalid JWT, wrong credentials |
| `403` | Forbidden | Access denied by security policy |
| `404` | Not Found | Resource doesn't exist or isn't owned by the current user |
| `415` | Unsupported Media Type | `Content-Type` not `application/json` |
| `500` | Internal Server Error | Unexpected runtime exception |
| `503` | Service Unavailable | Database, Redis, or AI service temporarily unavailable |

**Cross-user access policy:** The API returns `404` (not `403`) when a resource exists but belongs to another user. This prevents leaking the existence of other users' resources.

### Swagger UI

When `SWAGGER_ENABLED=true`:

- **Interactive UI:** `http://localhost:8080/swagger-ui.html`
- **Raw OpenAPI spec:** `http://localhost:8080/v3/api-docs`

The spec declares all endpoints, request/response schemas, and the JWT bearer scheme. Use the "Authorize" button to paste a token and test protected endpoints from the browser.

**In production**, Swagger is disabled by default (`SWAGGER_ENABLED=false` in the `prod` profile).

## Database Schema & Triggers

MyMindMirror uses **MySQL 8** as its primary relational store, with **PostgreSQL 16 + pgvector** for vector embeddings and **Redis 7** for caching. This section focuses on the MySQL schema, since that's where the domain model lives.

### Entity Overview

| Entity | Table | Purpose |
|---|---|---|
| `User` | `users` | Account credentials + encrypted Gemini key |
| `JournalEntry` | `journal_entries` | Encrypted journal text + AI analysis results |
| `KeyPhrase` | `journal_entry_key_phrases` | Extracted phrases, child of `JournalEntry` |
| `Milestone` | `milestones` | Goal containers with optional due dates |
| `Task` | `tasks` | Tasks within a milestone |
| `Roadmap` | `roadmaps` | AI-generated learning plans |
| `RoadmapTask` | `roadmap_tasks` | Weekly tasks within a roadmap |
| `RoadmapMilestone` | `roadmap_milestones` | Checkpoint milestones inside a roadmap |
| `RoadmapResource` | `roadmap_resources` | Links to courses, videos, articles |
| `CustomTask` | `custom_tasks` | Standalone tasks (not tied to milestones) |
| `ScheduledTask` | `scheduled_tasks` | Calendar entries (roadmap / milestone / custom) |
| `DailyJournalSummary` | `daily_journal_summary` | Pre-aggregated per-day metrics |
| `UserPreferences` | `user_preferences` | Lifestyle settings (wake/sleep/habits) |
| `UserRoadmapPreferences` | `user_roadmap_preferences` | Roadmap defaults (difficulty, language, pace) |
| `UserStats` | `user_stats` | XP, streaks, badges, counters |

### Relationship Overview

The data model splits into three logical domains.


**Cardinality notation:**

| Symbol | Meaning |
|---|---|
| `\|\|` | Exactly one |
| `o{` | Zero or more |
| `\|o` | Zero or one |
| `\|\|--\|\|` | One-to-one |
| `\|\|--o{` | One-to-many |

#### Journal Domain

```mermaid
erDiagram
    User ||--o{ JournalEntry : "owns"
    JournalEntry ||--o{ KeyPhrase : "contains"
```

#### Planning Domain

```mermaid
erDiagram
    User ||--o{ Roadmap : "owns"
    User ||--o{ Milestone : "owns"
    User ||--o{ CustomTask : "owns"
    User ||--o{ ScheduledTask : "owns"
    Roadmap ||--o{ RoadmapTask : "contains"
    Roadmap ||--o{ RoadmapMilestone : "contains"
    Roadmap ||--o{ RoadmapResource : "contains"
    Milestone ||--o{ Task : "contains"
```

#### User Settings & Stats

```mermaid
erDiagram
    User ||--|| UserPreferences : "has"
    User ||--|| UserRoadmapPreferences : "has"
    User ||--|| UserStats : "has"
```

**Cascade behavior:** Deleting a `User` cascades through every dependent table in the correct order (`ScheduledTask` → `CustomTask` → `Roadmap` (cascades tasks/resources/milestones) → `Milestone` (cascades tasks) → `JournalEntry` (cascades key phrases) → `UserStats` → `UserPreferences` → `UserRoadmapPreferences`). This is enforced in `UserService.deleteUser()` within a single transaction.

### Indexes

Every table is explicitly indexed on foreign keys and query-critical columns:

| Table | Index | Columns |
|---|---|---|
| `users` | `idx_users_username`, `idx_users_email` | `username`, `email` |
| `journal_entries` | `idx_journal_user_date` | `user_id, entry_date` |
| `journal_entries` | `idx_journal_mood` | `user_id, mood_score` |
| `milestones` | `idx_milestone_user`, `idx_milestone_due` | `user_id` / `due_date` |
| `tasks` | `idx_task_milestone`, `idx_task_status` | `milestone_id` / `status` |
| `roadmaps` | `idx_roadmap_user`, `idx_roadmap_created` | `user_id` / `created_at` |
| `roadmap_tasks` | `idx_roadmap_task_roadmap`, `idx_roadmap_task_week` | `roadmap_id` / `week_number` |
| `scheduled_tasks` | `idx_scheduled_user_date`, `idx_scheduled_reminder` | `user_id, scheduled_date` / `reminder_sent` |
| `daily_journal_summary` | `uk_user_date` (unique) | `user_id, date` |

### The `daily_journal_summary` Trigger Architecture

This is the piece of the schema that carries the most weight. Instead of running aggregation queries (`AVG`, `SUM`, `COUNT`) on every analytics request, MyMindMirror **pre-computes per-day metrics at write time** using MySQL triggers and stored procedures.

#### Why triggers?

The alternative — a scheduled job or a service-layer aggregation — would either be slower (adds latency to the write path) or eventually inconsistent (if the job crashes mid-run). Triggers run **inside the same transaction** as the write, guaranteeing consistency without any extra service code.

#### The three procedures

Installed by `DatabaseBootstrap` on startup:

**1. `recalc_daily_summary(user_id, date)`** — full recomputation of a single day's aggregate:

```sql
SELECT AVG(mood_score), SUM(word_count), COUNT(*)
INTO v_avg_mood, v_total_words, v_entry_count
FROM journal_entries
WHERE user_id = p_user_id AND entry_date = p_date;

-- If no entries remain for the day, delete the summary row
IF v_avg_mood IS NULL OR v_entry_count = 0 THEN
    DELETE FROM daily_journal_summary WHERE user_id = p_user_id AND date = p_date;
ELSE
    INSERT INTO daily_journal_summary (user_id, date, avg_mood, total_words, entry_count)
    VALUES (p_user_id, p_date, v_avg_mood, IFNULL(v_total_words, 0), v_entry_count)
    ON DUPLICATE KEY UPDATE
        avg_mood = v_avg_mood,
        total_words = IFNULL(v_total_words, 0),
        entry_count = v_entry_count;
END IF;
```

**2. `insert_journal_entry_summary(user_id, date, mood_score, word_count)`** — incremental update on INSERT:

```sql
INSERT INTO daily_journal_summary (user_id, date, avg_mood, total_words, entry_count)
VALUES (p_user_id, p_date, p_mood_score, p_word_count, 1)
ON DUPLICATE KEY UPDATE
    avg_mood = (avg_mood * entry_count + p_mood_score) / (entry_count + 1),
    total_words = total_words + p_word_count,
    entry_count = entry_count + 1;
```

The `avg_mood` formula uses a **rolling average**: new average = (old average × old count + new value) / (old count + 1). This avoids re-scanning all entries on every insert.

**3. `update_journal_entry_summary(old_user_id, old_date, new_user_id, new_date)`** — handles UPDATE:

```sql
IF old_date != new_date THEN
    CALL recalc_daily_summary(old_user_id, old_date);
    CALL recalc_daily_summary(new_user_id, new_date);
ELSE
    CALL recalc_daily_summary(new_user_id, new_date);
END IF;
```

If the entry moved days, both the old and new days are recomputed. Otherwise, only the current day is recomputed.

#### The three triggers

```sql
CREATE TRIGGER after_journal_entry_insert
AFTER INSERT ON journal_entries
FOR EACH ROW
CALL insert_journal_entry_summary(NEW.user_id, NEW.entry_date, NEW.mood_score, NEW.word_count);

CREATE TRIGGER after_journal_entry_update
AFTER UPDATE ON journal_entries
FOR EACH ROW
CALL update_journal_entry_summary(OLD.user_id, OLD.entry_date, NEW.user_id, NEW.entry_date);

CREATE TRIGGER after_journal_entry_delete
AFTER DELETE ON journal_entries
FOR EACH ROW
CALL recalc_daily_summary(OLD.user_id, OLD.entry_date);
```

#### How they're installed

`DatabaseBootstrap` (`@Order(100)`) runs **after** Hibernate has created the `journal_entries` and `daily_journal_summary` tables. It:

1. Detects the database product — if it's not MySQL, skips entirely (H2 slice tests, Testcontainers Postgres, etc.)
2. For each procedure/trigger: checks existence, then attempts `CREATE`
3. **Race-safe:** if two instances start simultaneously, the second one catches MySQL error `1304` (procedure) or `1359` (trigger) and treats it as a benign "already exists"
4. **Managed-MySQL-safe:** if `information_schema.ROUTINES` is privilege-gated (Aiven, RDS, PlanetScale), the existence check falls through and the `CREATE` path handles the conflict
5. **Fails loud:** any other error aborts startup with `IllegalStateException`

A missing trigger means silent data corruption in `daily_journal_summary`. Failing startup is the correct behavior.

#### Prerequisites — MySQL `log_bin_trust_function_creators`

MySQL 8.0 enables **binary logging** by default. When binlog is on, creating stored procedures or triggers requires one of:

- The app user has the `SUPER` privilege, **OR**
- The server variable `log_bin_trust_function_creators = 1` is set

Without one of these, `DatabaseBootstrap` fails with:

```
SQLState HY000, error code 1419 — You do not have the SUPER privilege and binary logging is enabled
```

**How MyMindMirror handles this per environment:**

| Environment | How the flag gets set |
|---|---|
| **Docker Compose** | `command: ["mysqld", "--log-bin-trust-function-creators=1"]` in `docker-compose.yml` |
| **Integration tests (Testcontainers)** | `.withCommand("mysqld", "--log-bin-trust-function-creators=1")` in `AbstractIntegrationTest` |
| **Local MySQL install** | Add `log_bin_trust_function_creators = 1` under `[mysqld]` in `my.cnf`, restart MySQL |
| **Managed MySQL (Aiven, RDS, PlanetScale, DigitalOcean)** | Set via the provider's parameter group or console — not settable from application code |

**Why not `application.properties`:** The flag is a server-side variable evaluated at MySQL startup. It cannot be changed per-connection, and changing it at runtime via `SET GLOBAL` requires the very `SUPER` privilege the flag exists to bypass. There is no JDBC URL parameter or Spring Boot property that toggles it.

#### Verification

After a fresh start, confirm the objects exist:

```sql
SHOW TRIGGERS WHERE `Table` = 'journal_entries';
SHOW PROCEDURE STATUS WHERE Db = DATABASE();
SHOW INDEX FROM daily_journal_summary WHERE Key_name = 'uk_user_date';
```

Expect exactly:
- 3 triggers on `journal_entries`
- 3 procedures (`recalc_daily_summary`, `insert_journal_entry_summary`, `update_journal_entry_summary`)
- 1 unique index (`uk_user_date`)

### Vector Schema (PostgreSQL/pgvector)

The `vector_store` table is created by Spring AI's `PgVectorStore` on first startup (`spring.ai.vectorstore.pgvector.initialize-schema=true`).

`VectorDatabaseConfig.optimizeVectorDatabase()` then creates two indexes on startup:

```sql
-- Fast per-user filtering
CREATE INDEX IF NOT EXISTS idx_vector_store_user_id
    ON vector_store ((metadata->>'userId'));

-- Arbitrary metadata queries
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_gin
    ON vector_store USING gin ((metadata::jsonb));
```

- **Dimensions:** 3072 (matches `text-embedding-004` output)
- **Distance type:** Cosine (standard for text embeddings)
- **Index type:** None — exact search, 100% recall

For a larger dataset, switch `PgIndexType` to `HNSW` or `IVFFlat` for approximate nearest-neighbor search at the cost of recall.

### Schema Management Notes

**Current state:** Schema is managed by Hibernate's `ddl-auto=update` in dev and by triggers installed via `DatabaseBootstrap`. There is no Flyway or Liquibase migration pipeline.

**Implications:**
- On a fresh database, Hibernate creates all tables from the entity model.
- `DatabaseBootstrap` adds the procedures and triggers that Hibernate can't manage.
- **In production, switch to `ddl-auto=validate` after adopting Flyway** — see [Roadmap / Known Limitations](#roadmap--known-limitations).

**Why not Flyway yet:** Flyway runs before Hibernate by default. On a fresh database, the migration that creates triggers on `journal_entries` would fail because the table doesn't exist yet. Coexisting requires Flyway to own every table and Hibernate to run in `validate` mode — a full migration project that isn't needed at the current scale.

## Testing

MyMindMirror has a **two-tier backend test suite** and a **three-tier frontend test suite**. The split lets you run fast unit tests during development and full integration tests before pushing.

### Backend Testing

#### Test split

| Tier | Naming | Runner | Scope |
|---|---|---|---|
| **Unit tests** | `*Test.java` | Surefire | Isolated classes with mocked dependencies |
| **Integration tests** | `*IT.java` | Failsafe | Full Spring context + real MySQL + real Redis via Testcontainers |

Both run with `./mvnw verify`. Unit tests run first; integration tests run after, in the `integration-test` phase.

#### Running tests

```bash
cd backend/mymindmirror-backend

# Everything (unit + integration + coverage)
./mvnw verify

# Only unit tests (fast, no Docker needed)
./mvnw test

# Only integration tests (requires Docker for Testcontainers)
./mvnw failsafe:integration-test failsafe:verify
```

#### Integration test infrastructure

`AbstractIntegrationTest` provides the shared base for all `*IT.java` classes:

- **MySQL 8 container** via Testcontainers `MySQLContainer`
- **Redis 7 container** via Testcontainers `GenericContainer`
- **`@DynamicPropertySource`** wires both containers into Spring's datasource / Redis config at runtime
- **Mocked `DynamicAiClientService`** via `@MockitoBean` — no real Gemini calls during tests
- **`resetDatabase()`** truncates every table before each test
- **`resetCaches()`** clears every Spring cache before each test
- **`assertNotConnectedToProduction()`** aborts the test if the datasource URL contains `aivencloud.com` — a guardrail against accidentally running tests against production
- **`registerAndLogin()`** helper — creates a user and returns a JWT in one call
- **`generateExpiredToken()`** helper — produces a JWT with a past expiry, for auth failure tests
- **MySQL container flag:** The Testcontainers MySQL instance is started with
  `--log-bin-trust-function-creators=1`. MySQL 8.0's binary logging is on by default,
  which blocks non-`SUPER` users from creating procedures and triggers. Without this
  flag, integration tests fail with `SQLState HY000, error code 1419` the moment
  `DatabaseBootstrap` runs.

Containers are started **once per JVM** via a static initializer and stopped by a shutdown hook, so integration tests across all classes share the same MySQL + Redis instances.

#### Integration test classes

| Class | Scenario |
|---|---|
| `AsyncJournalAnalysisIT` | Fires after commit, populates entry, triggers re-analysis on edit, AI failure uses fallback |
| `AuthFlowIT` | Register → login → JWT → protected endpoint; duplicate username; invalid email; DB down → 503 |
| `CacheBehaviorIT` | Second call is served from cache; `@CacheEvict` invalidates; cache key isolation between users; Redis down → 503 |
| `ErrorContractIT` | Every failure returns the unified `ErrorResponse` shape |
| `GamificationFlowIT` | XP per action, level-ups, streak logic, badge unlocks, cache invalidation |
| `JournalLifecycleIT` | Encryption at rest, decrypt on read, re-encrypt on update, filters, pagination, search |
| `MilestoneInsightFlowIT` | AI success, AI failure fallback, ownership 404, called-twice-no-cache |
| `MultiTenantIsolationIT` | Every resource type returns 404 for cross-user access |
| `ProfileFlowIT` | Profile update, password change, roadmap preferences, lifestyle preferences, API key lifecycle, account delete cascade |
| `RoadmapFlowIT` | Generate, continue, elaborate, reschedule, delete |
| `ScheduleFlowIT` | Generate, fallback, move, complete, manual scheduling, re-generate preserves completed |

#### Coverage

JaCoCo reports are generated at `target/site/jacoco/index.html` after `./mvnw verify`.

**Excluded from coverage:**

- `**/runner/**` — bootstrap logic, exercised implicitly at startup
- `**/tools/**` — dev-only utilities (KeyGenerator)
- `**/BackendApplication.class` — the Spring Boot main class

All other code is measured. Run the report, open the HTML, and drill into packages to see line/branch coverage.

#### CI

The GitHub Actions workflow (`.github/workflows/backend-ci.yml`) runs on every push and PR to `main`:

1. Checkout
2. Set up JDK 25 (Temurin) with Maven cache
3. Verify Docker is available
4. `mvn -B verify`
5. Upload Surefire reports (unit test XML)
6. Upload Failsafe reports (integration test XML)
7. Upload JaCoCo HTML coverage report

All three artifacts are retained for 7 days.

### Frontend Testing

#### Test split

| Tier | Runner | Scope |
|---|---|---|
| **Unit / component** | Vitest | Isolated components with mocked hooks |
| **E2E** | Playwright | Full browser automation against a running stack |

#### Running tests

```bash
cd frontend/mymindmirror-app

# Unit / component tests (single run)
npm test

# Vitest UI (interactive)
npm run test:ui
```

#### E2E Tests (Playwright)

Playwright is configured and runs a **single smoke test** on every frontend PR — the full stack comes up via Docker Compose, the test registers a user, writes a journal entry, and verifies it appears in history. If this test passes, the app is not fundamentally broken.

```bash
# Locally: requires Docker Compose to be running (`docker compose up -d`)
cd frontend/mymindmirror-app
npx playwright test

# Headed mode for debugging
npx playwright test --headed

# Run a single file
npx playwright test e2e/JournalFlow.spec.js
```

**Note:** E2E tests require the backend and frontend to be running. The Playwright fixture in `JournalFlow.spec.js` registers a user via the API, sets the JWT in `localStorage`, and drives the UI.

#### Vitest setup

- **Environment:** `jsdom` (configured in `vite.config.js` via `test.environment`)
- **Global helpers:** `@testing-library/jest-dom` matchers extended in `src/test/setup.js`
- **Cleanup:** `afterEach(cleanup)` ensures no React tree leaks between tests
- **Coverage:** available via `vitest --coverage` (requires `@vitest/coverage-v8`)

#### Example component test

`JournalInput.test.jsx` demonstrates the testing approach:

```jsx
// Mock the custom hooks (not the component's internals)
vi.mock("../../hooks/useJournalData", () => ({
  useAddJournalEntry: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
    isError: false,
    error: null,
  }),
}));

// Render and drive via user events
await userEvent.type(textarea, "My test journal entry");
fireEvent.click(button);
await waitFor(() => {
  expect(mockMutateAsync).toHaveBeenCalledWith({ rawText: "My test journal entry" });
});
```

The pattern: mock the *hooks* (data layer), render the actual component, drive it via `userEvent`, assert on user-visible outcomes.

### What's Not Tested (Yet)

- **Payment flows** — not applicable; there's no monetization
- **Load / stress testing** — no JMeter/Gatling suite; the virtual-thread architecture is designed for it but not validated at scale
- **Frontend E2E coverage** — currently a single flow (`JournalFlow.spec.js`); more flows pending
- **Penetration testing** — no formal security audit; the code follows known-safe patterns but hasn't been externally validated

If you're extending the project, prioritize adding:
1. E2E tests for roadmap and schedule flows (highest complexity)
2. Unit tests for `DynamicAiClientService` cascade logic (cascade order, cooldown thresholds)
3. Migration tests once Flyway is adopted

### Test Coverage

As of the current codebase:

| Metric | Value |
|---|---|
| **Total tests** | 1,159 (1,043 unit + 116 integration) |
| **Instructions covered** | 99% (16,553 / 16,634) |
| **Branches covered** | 99% (1,254 / 1,263) |
| **Lines covered** | 99% (3,252 / 3,265) |
| **Methods covered** | 100% (585 / 585) |
| **Classes covered** | 100% (143 / 143) |

Coverage is measured by JaCoCo 0.8.15. Generate your own report with:

```bash
cd backend/mymindmirror-backend
./mvnw verify
# → open target/site/jacoco/index.html
```

#### Where the remaining 1% lives

The only package below 99% is `util/`, driven entirely by **defensive catch blocks** in the two encryption utilities:

| Class | Line coverage | Uncovered code |
|---|---|---|
| `EncryptionUtil` | 76% | `catch` blocks for `NoSuchAlgorithmException`, `InvalidKeySpecException`, `BadPaddingException`, `IllegalBlockSizeException` |
| `FieldEncryptionUtil` | 93% | `catch` blocks for JCE provider failures |

These branches only execute when the JVM's cryptography provider is misconfigured or a cipher operation fails at the byte level — scenarios that require either a broken JRE or deep reflection into `Cipher`, `SecretKeyFactory`, or `SecureRandom`. Both are excluded from testing by design, not by oversight. The happy paths, invalid-secret paths, and malformed-data paths are all covered.

Every other package — `service`, `service.ai`, `controller`, `mapper`, `security`, `exception`, `config`, `model`, `payload.*`, `enums`, `resolver`, `constants`, `config.properties` — is at **100%**.

#### Coverage exclusions

JaCoCo is configured to skip:

- `**/runner/**` — Spring Boot `ApplicationRunner` beans (`DatabaseBootstrap`, backfill runners). Startup behavior is exercised implicitly by every integration test that boots the context.
- `**/tools/**` — `KeyGenerator`, a developer utility with a `main()` method.
- `**/BackendApplication.class` — the Spring Boot entry point.

#### Integration test prerequisites

Running `./mvnw verify` requires:

- **Docker** available on the host (Testcontainers pulls `mysql:8.0` and `redis:7-alpine`)
- **Port 3306 and 6379** free on the host (Testcontainers maps to random host ports, so this is usually not an issue, but a stale container could block them)
- **~500 MB free disk** for the container images

The MySQL container is started with `--log-bin-trust-function-creators=1` so that `DatabaseBootstrap` can install the summary procedures and triggers — same reason as production.

### Troubleshooting the Trigger Pipeline

The `daily_journal_summary` table is maintained by three MySQL triggers. If analytics charts return empty data or the summary looks wrong, this checklist will isolate the issue.

#### Verify the objects exist

Run these in your MySQL client (DBeaver, mysql CLI, or Adminer):

```sql
-- Should return 3 rows
SHOW TRIGGERS WHERE `Table` = 'journal_entries';

-- Should return 3 rows: recalc_daily_summary, insert_journal_entry_summary, update_journal_entry_summary
SHOW PROCEDURE STATUS WHERE Db = DATABASE();

-- Should return 1 row for uk_user_date
SHOW INDEX FROM daily_journal_summary WHERE Key_name = 'uk_user_date';
```

If any of these return fewer rows than expected, `DatabaseBootstrap` failed silently on startup. Check the backend log for lines starting with `DatabaseBootstrap:`.

#### Common issues

| Symptom | Cause | Fix |
|---|---|---|
| Backend refuses to start with `error code 1419` | `log_bin_trust_function_creators` is not set | See [Database Schema → Prerequisites](#prerequisites--mysql-log_bin_trust_function_creators) |
| Summary table has duplicate `(user_id, date)` rows | Unique key `uk_user_date` missing | Run `ALTER TABLE daily_journal_summary ADD UNIQUE INDEX uk_user_date (user_id, date);` |
| Summary rows do not update on entry UPDATE/DELETE | Triggers are missing or `user_id` type mismatch | Verify `journal_entries.user_id` is `BINARY(16)` via `SHOW COLUMNS FROM journal_entries LIKE 'user_id';` |
| New entries do not appear in analytics | Trigger exists but threw an error silently | Check for warnings in `SHOW WARNINGS;` immediately after an insert |
| Stored procedures missing after redeploy | Managed MySQL revoked privileges | Set `log_bin_trust_function_creators=1` via the provider console |

#### Manual verification (advanced)

To independently confirm the triggers work, run this against your database:

```sql
-- Setup: pick a test user and today's date
SET @test_user_id = (SELECT id FROM users LIMIT 1);
SET @test_date = CURDATE();

-- Wipe any prior test data
DELETE FROM journal_entries WHERE user_id = @test_user_id AND entry_date = @test_date;
DELETE FROM daily_journal_summary WHERE user_id = @test_user_id AND date = @test_date;

-- Insert a test entry (triggers should fire automatically)
INSERT INTO journal_entries
    (id, user_id, entry_date, raw_text, mood_score, word_count, creation_timestamp)
VALUES
    (UUID_TO_BIN(UUID()), @test_user_id, @test_date, 'trigger test', 0.85, 50, NOW());

-- Verify: the summary should now have 1 row with avg_mood=0.85, total_words=50, entry_count=1
SELECT avg_mood, total_words, entry_count
FROM daily_journal_summary
WHERE user_id = @test_user_id AND date = @test_date;

-- Cleanup
DELETE FROM journal_entries WHERE user_id = @test_user_id AND entry_date = @test_date;
DELETE FROM daily_journal_summary WHERE user_id = @test_user_id AND date = @test_date;
```

If the summary row appears after the INSERT, the trigger pipeline is working. If it doesn't, the trigger is missing or was created without the `BINARY(16)` argument type — check the trigger body with `SHOW CREATE TRIGGER after_journal_entry_insert;`.

## Security Model

MyMindMirror handles the most personal data a user will ever type. The security posture reflects that: **encryption at rest by default, stateless auth, no plaintext secrets anywhere in the persistence layer**.

### Encryption at Rest

Two independent encryption systems protect different classes of data. They use different algorithms and different key sources on purpose — compromise of one doesn't compromise the other.

| Data | Algorithm | Key source | Utility |
|---|---|---|---|
| Journal entry text | **AES-CBC / PKCS5Padding** | User's BCrypt password hash, via PBKDF2 | `EncryptionUtil` |
| User's Gemini API key | **AES-GCM / NoPadding** | Server master key (`ENCRYPTION_MASTER_KEY`) | `FieldEncryptionUtil` |

#### Journal text (`EncryptionUtil`)

- **Algorithm:** AES-128 in CBC mode with PKCS5 padding
- **Key derivation:** PBKDF2-HMAC-SHA256, **65,536 iterations**, 128-bit output
- **Salt:** First 16 bytes of the user's BCrypt password hash
- **IV:** 16 random bytes generated per encryption, prepended to the ciphertext before Base64 encoding
- **Key secrecy:** The plaintext password is never stored. The key is derived from the BCrypt hash at read/write time; without the user's plaintext password, the derived key cannot be recomputed.

**Why CBC with random IV:** Deterministic encryption would leak equality (two identical entries would produce identical ciphertext). CBC with a random IV ensures each encryption is unique even for identical plaintext.

**Failure behavior:** Decryption failures (corrupted data, wrong key, or legacy plaintext) are caught and returned unchanged rather than throwing. This preserves access to old data during migrations and prevents a single corrupted entry from breaking history reads.

#### User API keys (`FieldEncryptionUtil`)

- **Algorithm:** AES-256 in GCM mode (authenticated encryption)
- **Key:** 32-byte server master key from `ENCRYPTION_MASTER_KEY`, decoded from Base64 at startup
- **IV:** 12 random bytes per encryption, prepended to ciphertext
- **Auth tag:** 128 bits

**Why GCM here and CBC for journal text:** GCM provides authenticated encryption — a single bit of tampering invalidates the entire ciphertext. For API keys, an attacker who flips a byte in the database should not be able to influence decrypted output. For journal text (larger payloads, more writes), CBC with a random IV is efficient and adequate.

**Master key validation:** The constructor enforces exactly 32 bytes at startup. Anything else aborts with `IllegalArgumentException` before the app accepts traffic.

### Password Storage

- **Algorithm:** BCrypt via `BCryptPasswordEncoder` (default strength 10)
- **Truncation:** BCrypt truncates inputs at 72 bytes. The API enforces `@Size(max = 72)` on password fields to make this limit explicit rather than silent.
- **Minimum length:** 6 characters (enforced via `@Size(min = 6)`)
- **Comparison:** Constant-time comparison via `BCryptPasswordEncoder.matches()`

### Authentication Flow

1. **Login** — `POST /api/auth/login` with username and password.
2. **Verification** — `AuthenticationManager` delegates to `UserDetailsServiceImpl`, which loads the user and BCrypt-verifies the password.
3. **Token issuance** — `JwtUtil.generateToken(UserDetails, userId)` creates an HS512-signed JWT with `sub` (username) and a `userId` claim.
4. **Subsequent requests** — `JwtRequestFilter` (a `OncePerRequestFilter`) intercepts each request:
    - Extracts the token from `Authorization: Bearer <jwt>`
    - Verifies signature and expiry
    - Loads the user via `UserDetailsService`
    - Populates the `SecurityContextHolder`
5. **Endpoint protection** — `WebSecurityConfig` permits `/api/auth/**` and requires authentication for `/api/**`. Actuator health/info are public; Swagger is public when enabled.

**Session model:** None. `SessionCreationPolicy.STATELESS` — no server-side sessions, no cookies. Every request must carry a valid token.

### JWT Details

| Property | Value |
|---|---|
| Signing algorithm | HS512 (HMAC-SHA-512) |
| Key | Server's `JWT_SECRET`, Base64-decoded, must be ≥ 512 bits |
| Claims | `sub` (username), `userId` (UUID as string), `iat`, `exp` |
| Default lifetime | 24 hours (`JWT_EXPIRATION=86400000`) |
| Revocation | None — tokens are valid until expiry |

**Consequence of stateless JWTs:** There is no server-side token blacklist. Logging out is a client-side operation (delete token from `localStorage`). If a token is stolen, it remains valid until expiry. Choose `JWT_EXPIRATION` with this in mind.

### Authorization

- **Endpoint-level:** Every `/api/**` endpoint (except `/api/auth/**`) requires an authenticated principal.
- **Resource-level:** Ownership is enforced in service methods. Every read/write of a user-owned entity filters by `user.getId()`.
- **Cross-user access policy:** Returns **404** (not 403). This prevents leaking the existence of other users' resources — an attacker cannot distinguish "not found" from "not yours."

> **Why 404 instead of 403:** Returning 403 Forbidden would confirm that the resource exists but belongs to someone else. That's a resource-enumeration vulnerability — an attacker could probe UUIDs and learn which ones are real. Returning 404 for both "not found" and "not yours" collapses the two cases into one indistinguishable response. This is the OWASP-recommended pattern for multi-tenant APIs.

### User API Key (BYOK) Handling

- **Storage:** Encrypted with AES-GCM under the server master key.
- **Exposure:** Plaintext is **never** returned by any endpoint. The status endpoint returns a masked hint (last 4 characters prefixed with `••••••••`).
- **Cache eviction:** When a key is updated or cleared, `DynamicAiClientService.evictUserModel(userId)` clears cached model instances so the new key takes effect immediately.
- **Failure modes:** If a stored key fails to decrypt (e.g., after master key rotation), the user falls back to the shared key. They must re-enter their key to restore BYOK.

### Unified Error Responses

Every error path — validation, auth, filter-layer rejections, unexpected runtime exceptions — returns the same `ErrorResponse` shape (see the [API Documentation](#api-documentation) section). No stack traces, no SQL fragments, no internal paths leak to the client.

- **`debug_message`** is stripped in production (`app.error.expose-debug=false`).
- **`message`** is always client-safe.
- **404 vs 403**: cross-user access returns 404 with a generic message.

### CORS

- Controlled by `CORS_ALLOWED_ORIGINS` (comma-separated).
- Defaults to `http://localhost:5173,http://localhost:3000,http://localhost`.
- **No wildcard origins** in the default config. `allowCredentials=true` requires explicit origins.
- Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`.
- Allowed headers: `Authorization`, `Content-Type`, `Accept`, `X-Requested-With`, `Cache-Control`.

### Known Gaps

Documented explicitly so they can be tracked, not hidden.

| Gap | Impact | Mitigation |
|---|---|---|
| **No rate limiting on `/api/auth/**`** | Brute-force password attempts are limited only by BCrypt's cost | Deploy behind a reverse proxy with `limit_req` (see [Deployment](#deployment)) |
| **Redis has no password** in the default Docker setup | Redis is on an internal Docker network, not exposed to the host or internet | Add `requirepass` before exposing Redis externally |
| **`ddl-auto=update` is still active** | Hibernate can modify schema on startup; fine for dev, hazardous in production | Adopt Flyway, then switch to `validate` — see [Roadmap / Known Limitations](#roadmap--known-limitations) |
| **No refresh tokens** | Users must re-login after `JWT_EXPIRATION` | Acceptable for a self-hosted personal tool; add refresh flow if multi-tenant |
| **`DynamicAiClientService` cache is unbounded** | Long-running instances will accumulate model instances per user | Not a problem at current scale; revisit at ~500+ users |
| **`app.error.expose-debug` must stay `false`** in production | Debug messages can leak internal paths | Enforced in `application-prod.properties` |
| **`EncryptionUtil` uses CBC without HMAC** | CBC alone is not authenticated; tampering with ciphertext is possible (though PKCS5 padding provides partial detection) | Journal text is user-owned; tampering would corrupt their own data, not enable privilege escalation. For defense in depth, consider AES-GCM in a future iteration |

### Security Checklist for Deployment

- [ ] `JWT_SECRET` is ≥ 512 bits, generated with `openssl rand -base64 64`
- [ ] `ENCRYPTION_MASTER_KEY` is exactly 32 bytes, generated with `openssl rand -base64 32`
- [ ] `app.error.expose-debug=false` in production
- [ ] `SWAGGER_ENABLED=false` in production
- [ ] MySQL and PostgreSQL are not exposed on public interfaces
- [ ] Redis is either not exposed or requires a password
- [ ] Reverse proxy enforces HTTPS and rate limits on `/api/auth/**`
- [ ] Logs do not contain plaintext secrets, tokens, or user email addresses
- [ ] `CORS_ALLOWED_ORIGINS` does not contain wildcards
- [ ] Backup of `ENCRYPTION_MASTER_KEY` exists in a secret manager
- [ ] Cross-user access returns 404, not 403 (verified in integration tests)

## Configuration Profiles

Spring Boot reads `application.properties` and then overlays `application-{profile}.properties` on top. MyMindMirror ships three profiles: **dev**, **prod**, and **test**. Activate one via `SPRING_PROFILES_ACTIVE`.

If no profile is set, base `application.properties` values apply with no overlay.

### Dev Profile

**Activate:** `SPRING_PROFILES_ACTIVE=dev`

Optimized for developer feedback and debugging.

| Setting | Value | Why |
|---|---|---|
| `logging.level.org.hibernate.SQL` | `DEBUG` | See every SQL statement |
| `spring.jpa.properties.hibernate.format_sql` | `true` | Human-readable SQL |
| `logging.level.org.hibernate.orm.jdbc.bind` | `TRACE` | See bound parameter values |
| `logging.level.org.springframework.cache` | `DEBUG` | Trace cache hits/misses |
| `logging.level.org.springframework.web.cors` | `DEBUG` | Debug CORS rejections |
| `app.error.expose-debug` | `true` | Include `debug_message` in errors |

**Base config (applies to all profiles):**

- `spring.jpa.hibernate.ddl-auto=update` — Hibernate auto-updates the schema from entities
- `spring.jpa.open-in-view=false` — no lazy loading in the view layer
- `spring.threads.virtual.enabled=true` — virtual threads on

### Prod Profile

**Activate:** `SPRING_PROFILES_ACTIVE=prod`

Optimized for production: quiet logs, no leaked internals, no debug surfaces.

| Setting | Value | Why |
|---|---|---|
| `logging.level.root` | `WARN` | Only warnings and errors |
| `logging.level.org.hibernate.SQL` | `WARN` | No SQL logging |
| `logging.level.org.hibernate.orm.jdbc.bind` | `OFF` | No parameter logging |
| `logging.level.org.springframework.web` | `WARN` | No request logging |
| `springdoc.api-docs.enabled` | `false` | Swagger spec disabled |
| `springdoc.swagger-ui.enabled` | `false` | Swagger UI disabled |
| `app.error.expose-debug` | `false` | Strip `debug_message` from errors |
| `management.endpoint.health.show-details` | `when-authorized` | Only authorized users see health details |
| `spring.jpa.properties.hibernate.format_sql` | `false` | No pretty-printed SQL |

**Docker Compose** sets `SPRING_PROFILES_ACTIVE=prod` by default when running the backend service.

### Test Profile

**Activate:** Automatic when running tests (`@ActiveProfiles("test")` or the Surefire/Failsafe config).

Used for both unit and integration tests.

| Setting | Value | Why |
|---|---|---|
| `spring.datasource.url` | `jdbc:h2:mem:testdb;MODE=MySQL` | In-memory H2 for slice tests |
| `spring.datasource.driver-class-name` | `org.h2.Driver` | H2 driver |
| `spring.jpa.properties.hibernate.dialect` | `org.hibernate.dialect.H2Dialect` | H2-compatible DDL |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` | Fresh schema per test |
| `jwt.secret` | (hardcoded test value) | Deterministic tokens for assertions |
| `jwt.expiration.ms` | `86400000` | 24h for test tokens |
| `app.encryption.master-key` | (hardcoded test value) | Deterministic AES-GCM key |
| `spring.ai.google.genai.api-key` | `test-dummy-key` | Placeholder; real AI is mocked in tests |

**Testcontainers override:** Integration tests (`*IT.java`) override the H2 datasource via `@DynamicPropertySource`, pointing Spring at real MySQL and Redis containers. The H2 config remains active for `@DataJpaTest` slices, which don't need a full container.

**Why `application-test.properties` contains "secrets" that are safe to commit:** They are test-only values, scoped by the `test` profile, and never loaded in dev, staging, or production. This is standard practice across Spring Boot projects (including Spring's own test suites). Treating them as leaks is a false positive.

### Base Profile (No `SPRING_PROFILES_ACTIVE`)

If you run the backend without a profile, `application.properties` provides safe defaults:

- Verbose logging is off
- `ddl-auto=update` is still on (dev-friendly)
- `app.error.expose-debug=false`
- Swagger is enabled by default (`SWAGGER_ENABLED=true` unless overridden)

This is fine for local development. For any shared environment, always set a profile.

### Property Import Order

Spring Boot resolves properties in this precedence (later wins):

1. `application.properties` (base)
2. `application-{profile}.properties`
3. Environment variables (Docker Compose, `.env`)
4. Command-line arguments (`--server.port=9000`)

**Practical implication:** The values set in `docker-compose.yml` (env vars) always override what's in `application-prod.properties`. This is why `docker-compose.yml` explicitly sets `SWAGGER_ENABLED` and `app.error.expose-debug` — to enforce prod-safe defaults regardless of what the properties file says.

### Choosing a Profile

| Environment | Profile | Why |
|---|---|---|
| Local development | `dev` | Verbose logs, debug errors, Swagger on |
| Local docker-compose | *(none)* or `dev` | Same as local dev, containers provide infra |
| Staging | `prod` + `SWAGGER_ENABLED=true` | Prod-safe defaults, but expose Swagger for QA |
| Production | `prod` | Full quiet mode, no debug surfaces |
| CI | `test` | Auto-set by Maven Surefire/Failsafe |

## Deployment

MyMindMirror ships with a Docker Compose stack that runs the full system with one command. For production, additional hardening is recommended.

### Docker Compose (Recommended)

The repo's `docker-compose.yml` starts five services:

| Service | Image | Purpose |
|---|---|---|
| `mysql` | `mysql:8.0` | Primary relational store |
| `postgres` | `pgvector/pgvector:pg16` | Vector embeddings |
| `redis` | `redis:7-alpine` | Cache + sessions |
| `backend` | Built from `backend/mymindmirror-backend/Dockerfile` | Spring Boot API |
| `frontend` | Built from `frontend/mymindmirror-app/Dockerfile` | Nginx-served React SPA |

All three databases use named Docker volumes (`mysql_data`, `postgres_data`, `redis_data`) so data persists across `docker compose down` (but is wiped by `docker compose down -v`).

#### Health checks

MySQL uses a health check (`mysqladmin ping`) with retries. The `backend` service waits for MySQL to become healthy before starting, which prevents the classic "table doesn't exist on first boot" race.

#### Network layout

- All five services share a Docker bridge network.
- Only `backend:8080` and `frontend:80` are published to the host by default.
- MySQL, PostgreSQL, and Redis listen on their default ports **inside** the network only — not on the host.
- The frontend nginx config (`frontend/mymindmirror-app/nginx.conf`) proxies `/api/` requests to `http://backend:8080/api/`.

### Required Environment Variables for Production

At minimum, override these when running in any shared environment:

```bash
GEMINI_API_KEY=<your-production-key>
JWT_SECRET=<openssl rand -base64 64>
JWT_EXPIRATION=86400000
ENCRYPTION_MASTER_KEY=<openssl rand -base64 32>
CORS_ALLOWED_ORIGINS=https://your-domain.com
SWAGGER_ENABLED=false
```

`docker-compose.yml` already sets `SPRING_PROFILES_ACTIVE=prod` and `app.error.expose-debug=false` for the `backend` service, so those do not need to be passed at runtime.

### Dockerfile Anatomy

Both Dockerfiles use **multi-stage builds** to keep the runtime images small.

**Backend (`backend/mymindmirror-backend/Dockerfile`):**

```dockerfile
# Build stage: Maven + JDK 25
FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline       # Cache dependencies as a layer
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage: slim JRE-only image
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- **`dependency:go-offline`** downloads every dependency in a separate layer — subsequent builds reuse it if `pom.xml` hasn't changed.
- **`-DskipTests`** skips tests during the Docker build. Tests run in CI via `mvn verify`.

**Frontend (`frontend/mymindmirror-app/Dockerfile`):**

```dockerfile
# Build stage: Node 22
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Runtime stage: nginx serving static files
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

- **`npm ci`** installs exact versions from `package-lock.json` — deterministic builds.
- **Vite's production build** outputs static assets to `dist/`.
- **nginx** serves the SPA and proxies `/api/` to the backend.

### Nginx Configuration

The frontend container serves the Vite-built SPA through nginx. The config in
`frontend/mymindmirror-app/nginx.conf` handles a subtle issue that affects every
PWA-enabled Vite app: **stale service-worker caches causing MIME type errors**.

**The problem:** After a new deploy, users with an old cached `index.html` still hold
references to asset hashes that no longer exist (e.g. `main.abc123.js`). If nginx falls
back to `index.html` for these requests, browsers reject the response:

```
Failed to load module script: Expected a JavaScript-or-Wasm module script but the
server responded with a MIME type of "text/html".
```

This happens because nginx's SPA fallback (`try_files $uri /index.html`) treats a missing
`/assets/main.abc123.js` the same as a missing client route like `/journal`.

**The fix:** The nginx config splits caching behavior into three tiers:

| Path | Behavior | Why |
|---|---|---|
| `/assets/` | `try_files $uri =404` — never falls back | Hashed assets should 404 if missing, not return HTML |
| `/index.html`, `/sw.js`, `/registerSW.js` | `Cache-Control: no-cache, no-store, must-revalidate` | Entry points must always be revalidated |
| `/` (everything else) | `try_files $uri $uri/ /index.html` | SPA routing for client-side routes |

The `/assets/` rule converts the confusing MIME error into a clean 404 the browser handles
gracefully. The `no-cache` rules ensure new asset hashes and service-worker updates are
picked up on the next page load.

**Once deployed, users may need one hard refresh** (`Cmd+Shift+R` / `Ctrl+Shift+R`) to
unregister any stale service worker from a previous deploy. After that, updates work
automatically on every load.

**Full nginx config reference** — `frontend/mymindmirror-app/nginx.conf`:

```nginx
server {
    listen 80;
    server_name localhost;

    root /usr/share/nginx/html;
    index index.html;

    location /assets/ {
        try_files $uri =404;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location = /index.html {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        expires 0;
    }

    location = /sw.js {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        expires 0;
    }

    location = /registerSW.js {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        expires 0;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Reverse Proxy (Recommended for Production)

Docker Compose runs nginx in front of the React SPA, but for production you should add an **edge** reverse proxy (Cloudflare, nginx on the host, Caddy, Traefik) in front of the frontend container. This proxy is where you should:

1. **Terminate TLS** — Let's Encrypt, Cloudflare origin cert, etc.
2. **Enforce rate limits on `/api/auth/**`** — see below.
3. **Add security headers** — HSTS, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`.
4. **Compress responses** — nginx gzip or brotli.
5. **Log at the edge** — keep application logs free of PII.

#### Rate-limiting snippet (nginx)

```nginx
# In http { } block:
limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=5r/m;

# In server { } block:
location /api/auth/ {
    limit_req zone=auth_limit burst=10 nodelay;
    proxy_pass http://localhost:80;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

This allows 5 login/register attempts per minute per IP, with a burst of 10. Adjust to taste.

### Managed Databases

If you deploy the databases separately (Aiven, RDS, PlanetScale, Neon, etc.), there are provider-specific requirements.

#### MySQL — `log_bin_trust_function_creators`

Managed MySQL providers have binary logging enabled by default. This blocks `DatabaseBootstrap`
from creating the summary procedures and triggers unless one of these is true:

- The app user has `SUPER`, OR
- The parameter `log_bin_trust_function_creators` is set to `1`

**Set it via the provider's parameter group or configuration UI:**

| Provider | How to set it |
|---|---|
| **Aiven** | Service → Advanced configuration → add `log_bin_trust_function_creators = 1` |
| **AWS RDS** | Parameter Groups → custom group → set the parameter → apply to instance |
| **DigitalOcean** | Databases → Settings → MySQL config → add the parameter |
| **PlanetScale** | Restricted by design — contact support or use a different provider |
| **Self-hosted MySQL** | Add to `my.cnf` under `[mysqld]`, restart MySQL |

**This cannot be set from `application.properties`.** The flag is evaluated by the MySQL server
process at startup — not per-connection, not via JDBC URL, not via Hibernate.

Without it, the backend refuses to start with `SQLState HY000, error code 1419`.

#### Other provider requirements

- **PostgreSQL 16+** with the `pgvector` extension installed. Most managed providers offer a pgvector-enabled variant (Neon, Supabase, Aiven).
- **Redis 7+** with `requirepass` set. Update `spring.data.redis.password` if you use a password-protected instance.

#### JDBC URL note

Some managed MySQL instances require `allowMultiQueries=true` for `DatabaseBootstrap` to create
the stored procedures in a single round trip. Add it to your `DATASOURCE_URL`:
```
jdbc:mysql://host:3306/db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC&allowMultiQueries=true
```

### Kubernetes / Orchestration (Notes)

MyMindMirror is not currently packaged as a Helm chart or k8s manifest. If you deploy to Kubernetes:

- **Two-replica safety:** The `DatabaseBootstrap` is race-safe (it catches MySQL `1304`/`1359` on concurrent starts). Multiple backend replicas are safe.
- **Session affinity:** Not required — JWTs are stateless. Any replica can serve any request.
- **Redis:** Use a managed Redis (ElastiCache, Memorystore, Upstash) rather than a pod. Sessions don't survive pod restarts.
- **Liveness/readiness probes:** Point them at `/actuator/health` (public).
- **ConfigMaps/Secrets:** Mount `GEMINI_API_KEY`, `JWT_SECRET`, `ENCRYPTION_MASTER_KEY`, and DB credentials as `Secret` objects, not `ConfigMap`.

### Backups

- **MySQL:** Back up `mysql_data` volume, or use `mysqldump` for point-in-time snapshots. Critically, **back up the value of `ENCRYPTION_MASTER_KEY`** — without it, all stored user API keys are unrecoverable.
- **PostgreSQL:** Back up `postgres_data` volume. Embeddings are derived data — they can be regenerated by re-running the backfill runner (`rag.backfill.enabled=true`) — but the backup is faster.
- **Redis:** Not backed up. Cache and ephemeral chat sessions are designed to be lost on restart.

### Upgrading

```bash
git pull
docker compose build --no-cache backend frontend
docker compose up -d
```

**Schema migrations:** MyMindMirror does not yet use Flyway, so schema changes rely on Hibernate's `ddl-auto=update` in dev. In production, prefer deploying to a staging environment first and running migrations manually until Flyway is adopted.

### Monitoring

The backend exposes Spring Boot Actuator endpoints:

| Endpoint | Purpose | Visibility |
|---|---|---|
| `/actuator/health` | Liveness/readiness | Public (safe, no internals) |
| `/actuator/info` | Build metadata | Public |

`/actuator/health` returns `{"status":"UP"}` when the app is ready. Point your load balancer's health checks here.

Detailed health (`show-details=always`) is disabled in prod — `/actuator/health` returns only the aggregate status.

### Production Checklist

- [ ] All env vars from [Environment Variables](#environment-variables) set
- [ ] `JWT_SECRET`, `ENCRYPTION_MASTER_KEY` generated with `openssl`, not copy-pasted
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `SWAGGER_ENABLED=false`
- [ ] `app.error.expose-debug=false`
- [ ] `CORS_ALLOWED_ORIGINS` set to your actual frontend URL(s) — no wildcards
- [ ] Reverse proxy enforces HTTPS and rate limits on `/api/auth/**`
- [ ] MySQL, PostgreSQL, Redis not exposed on public interfaces
- [ ] Redis requires a password if exposed outside the internal network
- [ ] Backups configured for MySQL, PostgreSQL, and `ENCRYPTION_MASTER_KEY`
- [ ] Actuator health check wired to load balancer
- [ ] Application logs scrubbed of PII (no email addresses, no tokens)

## Roadmap / Known Limitations

Every project has gaps. This section lists them explicitly so users know what to expect and contributors know where to help.

### Currently Deferred

These items are known and intentionally not fixed yet. Each has a reason, and each has a trigger condition — the point at which deferring stops being reasonable.

| Item | Reason for deferral | Trigger to revisit |
|---|---|---|
| **Flyway / Liquibase adoption** | Flyway runs before Hibernate by default, so a fresh-database migration that touches `journal_entries` would fail before Hibernate creates the table. Coexisting requires Flyway to own **every** table — a full migration project. | Before first production deploy with real user data |
| **`ddl-auto=validate` in production** | Blocked on Flyway adoption. `validate` would fail startup on the first schema-touching deploy without migration files. | Immediate follow-up to Flyway adoption |
| **Rate limiting on `/api/auth/**`** | Not urgent at current scale. BCrypt's cost provides some protection, but not enough at scale. | Before any public-facing deployment |
| **Redis authentication** (`requirepass`) | Redis runs on an internal Docker network, not exposed to the host or internet. | Before exposing Redis outside the container network |
| **Bounded cache for `DynamicAiClientService`** | The `userModelCache` grows unbounded — one `ChatModel` instance per `(userId, modelName)` pair. Not a problem at 0 users. | Before ~500 concurrent users |
| **Batch password re-encryption** | `changeUserPassword` re-encrypts every journal entry in a single transaction. Fine for small datasets; slow for users with 10k+ entries. | Before a user hits ~5k entries |
| **`CurrentUserArgumentResolver` per-request DB fetch** | Every authenticated request does a `findById(userId)` to hydrate the `User` entity. Could be cached in the security context. | When request latency becomes measurable |
| **`@Data` on JPA entities** | Generates `equals`/`hashCode` from all fields, which is technically wrong for entities with mutable IDs. No bug today; a latent one at scale. | Cleanup pass — no urgency |

### Open Items

Smaller items that surfaced during development but haven't been prioritized.

- **Jackson 2 → 3 migration** — blocked on JJWT. The `@SuppressWarnings({"deprecation", "removal"})` on `CacheConfig` and `CacheConfigTest` is intentional; Spring Data Redis marks the Jackson 2 serializer as deprecated in favor of a Jackson 3 equivalent, but JJWT 0.11.5 doesn't support Jackson 3 yet. Migration happens when the upstream dependency catches up.
- **Password minimum length** is 6 characters. NIST SP 800-63B recommends 8. Raising it would break existing users' logins unless handled carefully.
- **`AsyncJournalAnalysisService` has no retry** for transient DB failures when saving analysis results. If the DB blips during a save, the analysis is lost and the entry stays in "analyzing" state indefinitely.
- **Actuator endpoints** are secured implicitly by `anyRequest().authenticated()`. Adding an explicit `requestMatchers("/actuator/**")` rule would make the intent clearer.
- **`AsyncConfigurer` bean** is not defined; `@Async` uses Spring Boot's default executor. Fine for now, but virtual threads deserve an explicit executor.
- **Code organization:** `RoadmapService` is ~1,000 lines. `JournalHistory.jsx` is a large component with virtualization, modals, and markdown parsing mixed together. `formatText` markdown parser is duplicated in three components.

### Planned Features

Ideas that fit the project's direction, but not yet started.

- **OAuth2 / SSO login** — Google, GitHub providers
- **Multi-language UI (i18n)** — currently English-only
- **Voice journaling** — record audio, transcribe via Gemini
- **Mobile app** — React Native or PWA-installed native shell
- **Client-side encryption** — decrypt only in the browser; server never sees plaintext
- **Shared journaling** — couples journaling, or therapist-client view
- **Reminder notifications** — push notifications for daily journaling prompt
- **Semantic search across roadmaps** — currently only journal entries are embedded

Nothing in this list has a committed date.

### What's *Not* Planned

- **Payment / subscription system** — MyMindMirror is self-hosted and free
- **Public social features** — no sharing, no feeds, no likes
- **Native mobile apps** — the PWA covers the mobile use case
- **Chatbot-style freeform AI** — the AI is scoped to reflection and planning, not general-purpose conversation

### Scale Boundaries

MyMindMirror has not been load-tested. The following are the known architectural
limits — the components that would need to change first if the project ever
needed to serve more than a handful of users.

- **AI model cache** is unbounded. Each cached `GoogleGenAiChatModel` is on the
  order of 100 KB. At scale this would need a bounded cache (Caffeine).
- **Single MySQL primary** has no read replicas. Write-heavy workloads would
  need sharding or replicas.
- **Single Redis instance** has no clustering. Session-heavy workloads would
  need Redis Cluster.
- **Single Postgres/pgvector** has no HNSW index. Large vector collections
  would need one.

None of these has been measured against a specific user count. They are the
architectural weak points, not capacity guarantees.
None of these are close to being hit today.

### Contributing to the Roadmap

If you want to tackle one of the items above, open an issue first so we can discuss the approach. The higher-priority items (Flyway, rate limiting, bounded cache) are the ones that unblock real-world deployment — those are the most valuable contributions.

## Contributing

Contributions are welcome. Whether it's a bug report, feature suggestion, or pull request, here's how to make it land smoothly.

### Reporting Bugs

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.md). Include:

- **What you expected** vs. **what happened**
- **Steps to reproduce** — ideally a minimal sequence
- **Environment** — OS, browser (if frontend), JDK version, Docker version
- **Logs** — backend logs (`docker compose logs backend`) or browser console errors, if relevant
- **Screenshots** — if the issue is visual

Please don't paste secrets (JWT tokens, `GEMINI_API_KEY`, DB passwords) into issues. Redact them.

### Suggesting Features

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.md). Include:

- **The problem you're solving** — not just the feature you want
- **Who it helps** — solo users, multi-user deployments, developers?
- **Alternatives you've considered**

Features that align with the project's [Roadmap / Known Limitations](#roadmap--known-limitations) get priority.

### Development Workflow

```bash
# 1. Fork the repo on GitHub
# 2. Clone your fork
git clone https://github.com/harman-04/mymindmirror.git
cd mymindmirror

# 3. Create a feature branch
git checkout -b feat/your-feature-name

# 4. Make changes
# ...edit files...

# 5. Run the full test suite
cd backend/mymindmirror-backend && ./mvnw verify
cd ../../frontend/mymindmirror-app && npm test

# 6. Commit with a clear message
git add .
git commit -m "feat: add X"

# 7. Push to your fork
git push origin feat/your-feature-name

# 8. Open a pull request against main
```

### Commit Style

[Conventional Commits](https://www.conventionalcommits.org/):

| Prefix | When to use |
|---|---|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `refactor:` | Code change that neither fixes a bug nor adds a feature |
| `docs:` | Documentation only |
| `test:` | Adding or fixing tests |
| `chore:` | Build, tooling, dependency updates |
| `perf:` | Performance improvement |
| `style:` | Formatting only (no code change) |

Examples:

```
feat(roadmap): add batch continuation endpoint
fix(journal): prevent NPE when summary is null
docs(readme): clarify environment variable setup
chore(deps): bump spring-ai to 2.0.0-M9
```

### Pull Request Guidelines

Before opening a PR:

- [ ] **Tests pass** — `./mvnw verify` in backend, `npm test` in frontend
- [ ] **Coverage doesn't drop** — check `target/site/jacoco/index.html`
- [ ] **No unrelated changes** — keep the PR focused
- [ ] **Self-review** — read your own diff before submitting
- [ ] **Clear title and description** — explain *why*, not just *what*
- [ ] **Linked issue** — reference the issue this closes, if any (`Closes #123`)

A maintainer will review within a few days. Expect questions about design choices — this is normal, not rejection.

### Code Style

**Backend (Java):**

- Follow standard Java conventions
- Use `@Slf4j` for logging, never `System.out.println`
- Prefer records for DTOs, classes for entities
- Keep controllers thin — business logic in services
- Annotate service methods with `@Transactional` where appropriate
- Avoid `@Data` on new JPA entities — use explicit `@Getter`/`@Setter` + `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on the ID field

**Frontend (JSX):**

- Follow the ESLint config (`.eslintrc`)
- Use `React.memo()` on components that re-render frequently
- Extract custom logic into hooks under `src/hooks/`
- Prefer `async/await` over `.then()` chains
- Never `console.log()` in committed code — use `console.error` for genuine errors
- Respect the 3-layer elevation palette (see [Architecture](#architecture))

**Both:**

- No trailing whitespace
- No `TODO` comments without a linked issue (`// TODO(#123): refactor this`)
- No dead code in commits — delete it, don't comment it out
- **Line endings:** `.gitattributes` normalizes `mvnw` to LF and `*.cmd` to CRLF so the Maven wrapper works on every OS. Don't modify it unless you're changing the wrapper.

### What We Won't Accept

- Breaking changes to the REST API without a migration path
- Features that require a paid third-party service beyond the existing Gemini integration
- Removal of encryption at rest
- Removal of the unified `ErrorResponse` shape
- Any change that weakens cross-user isolation (404-on-not-owned policy)

### Community Standards

By participating, you agree to follow the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). Be kind. Assume good faith. Disagree without being disagreeable.

### Recognition

Contributors who land non-trivial PRs get added to the Acknowledgments section below, unless you'd prefer not to be. Just say so in the PR.

## License

MyMindMirror is released under the **Apache License 2.0**.

The full text is in the [LICENSE](LICENSE) file at the repository root. The short version:

```
Copyright 2026 Harmandeep Singh

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

### What This Means

You are free to:

- **Use** MyMindMirror for personal, commercial, or internal purposes
- **Modify** the source code however you like
- **Distribute** copies, including modified versions
- **Sublicense** the software under different terms
- **Sell** products built on top of it
- **Patent protection** — this license explicitly grants you a license to any patents that the software necessarily infringes

The requirements are:

- **Include the license** and copyright notice in any distribution
- **State significant changes** you've made (a CHANGELOG, or a note in your fork)
- **Don't use the project name or trademarks** to endorse your own version
- **Preserve any NOTICE file** the project ships with (MyMindMirror does not currently ship one)

### Why Apache-2.0

Three reasons this is the right license for MyMindMirror:

1. **Explicit patent grant.** Enterprise legal departments prefer Apache-2.0 over MIT for this reason — it removes ambiguity around patent rights. If MyMindMirror is used at a company, this makes the review process faster.
2. **Trademark clause.** Prevents confusing forks ("MyMindMirror Pro") that could mislead users.
3. **Consistency with the broader portfolio.** Project licenses should be uniform across the ecosystem to avoid code-reuse friction between projects.

MIT would work equally well for personal use, but Apache-2.0 provides stronger footing for the "developer, self-hoster, enterprise-adjacent" audience this project targets.

### Third-Party Licenses

MyMindMirror depends on several open-source libraries. All are compatible with Apache-2.0:

- **Spring Boot, Spring Security, Spring Data, Spring AI** — Apache-2.0
- **React, Chart.js, TanStack Query, Vite, Tailwind CSS** — MIT (compatible)
- **MySQL Connector/J** — GPL v2 with FOSS exception (compatible with Apache-2.0 for non-MySQL-server distribution)
- **PostgreSQL JDBC, pgvector** — BSD / PostgreSQL License (compatible)
- **Lettuce (Redis client)** — Apache-2.0
- **JJWT** — Apache-2.0
- **MapStruct** — Apache-2.0
- **Lombok** — MIT (compatible)

No dependency imposes restrictions on how you can use or distribute MyMindMirror.

## Acknowledgments

MyMindMirror stands on the shoulders of the open-source ecosystem. This project would not exist without:

### Frameworks & Platforms

- **[Spring Boot](https://spring.io/projects/spring-boot)** — the backend foundation
- **[Spring AI](https://spring.io/projects/spring-ai)** — the AI abstraction layer that makes `BeanOutputConverter` and multi-model cascading possible
- **[React](https://react.dev)** — the frontend framework
- **[Vite](https://vitejs.dev)** — the build tool
- **[Tailwind CSS](https://tailwindcss.com)** — the styling system

### AI & Data

- **[Google AI Studio](https://aistudio.google.com)** — for the Gemini and Gemma models that power every AI feature
- **[pgvector](https://github.com/pgvector/pgvector)** — the vector similarity extension that makes RAG practical on PostgreSQL
- **[MySQL](https://www.mysql.com)** — for the triggers, procedures, and stored-procedure support that keep analytics fast

### Libraries That Deserve a Shout-Out

- **[TanStack Query](https://tanstack.com/query)** — the best data-fetching library for React, period
- **[Chart.js](https://www.chartjs.org)** — canvas-rendered charts that stay smooth on mobile
- **[TanStack Virtual](https://tanstack.com/virtual)** — the reason journal history doesn't crash at 1,000 entries
- **[Lucide](https://lucide.dev)** — a consistent, tasteful icon set
- **[Framer Motion](https://www.framer.com/motion/)** — the entry animations that make the UI feel alive
- **[Sonner](https://sonner.emilkowal.ski)** — the toast library that never gets in the way
- **[jsPDF](https://github.com/parallax/jsPDF)** and **[html-to-image](https://github.com/bubkoo/html-to-image)** — for client-side export without server round-trips
- **[Lombok](https://projectlombok.org)** and **[MapStruct](https://mapstruct.org)** — the reason the backend code stays readable
- **[Testcontainers](https://testcontainers.com)** — real databases in integration tests, not mocks

### Inspiration

- The journaling and self-reflection tools that came before — Day One, Obsidian, Notion — for showing what good looked like
- The open-source RAG community, particularly patterns around retrieval quality and context budgeting
- Every Stack Overflow answer that saved an hour of debugging

### Contributors

*(This section will list contributors as PRs are merged. If you contribute and want to be listed, just say so in your PR.)*


## Contact

- **GitHub Issues:** [github.com/harman-04/mymindmirror/issues](https://github.com/harman-04/mymindmirror/issues) — for bugs, feature requests, and questions

[//]: # (- **Email:** *&#40;add your contact email if you want public inquiries&#41;*)
[//]: # (- **Portfolio:** *&#40;optional — link to your personal site or LinkedIn&#41;*)

For **security vulnerabilities**, do not open a public issue. Follow the disclosure process in [SECURITY.md](SECURITY.md) instead.

For **general questions** about setup or usage, the [Quick Start](#-quick-start) and [Manual Setup](#manual-setup-for-contributors) sections cover most scenarios. If they don't, open a discussion or issue.

---

*Built with care for people who write.*
