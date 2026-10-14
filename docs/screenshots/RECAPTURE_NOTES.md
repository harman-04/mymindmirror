# Screenshot Re-Capture Notes

Internal notes for updating the README screenshots. Not user-facing.

## When to Recapture

- A UI redesign changes the visual identity
- A new feature needs coverage
- The existing screenshots look outdated compared to the current app

## Conventions

- **Theme:** Dark mode for most; the `themes-responsive/` folder holds the light-mode showcase
- **Format:** `.webp` (WebP, ~40% smaller than PNG); videos as `.mp4`
- **Naming:** lowercase-hyphenated, matching the module (`schedule-week.webp`, `profile-api-key.webp`)
- **Width:** 1920px for desktop captures, 440px for mobile (iPhone 16 Pro Max)
- **Height:** variable — full-page captures are fine up to ~5000px; the README handles any aspect ratio
- **Crop:** for individual chart cards, crop to the card boundary (not the full page)

## Folder → Content Mapping

| Folder | Contains |
|---|---|
| `main/` | Hero image + 9 curated main-section screenshots |
| `themes-responsive/` | Light/dark theme pairs + mobile hero shots |
| `journaling-analysis/` | Journal input, analysis states, editing, anomaly alerts |
| `search-modes/` | All 4 search modes (semantic, keyword, mood, date) |
| `analytics-charts/` | Remaining 5 charts + word cloud video |
| `roadmap-planner/` | Roadmap generation, timeline, task details, continuation |
| `smart-timetable/` | All calendar views + custom task modals |
| `milestone-tracker/` | Milestone forms, progress, task details, insights panel |
| `achievements-profile/` | Achievements page + all profile sections |
| `auth-landing/` | Home hero, login, register, features guide |
| `pwa-offline/` | Install prompt + offline banner |
| `additional-mobile/` | Extra mobile analytics shots (gallery only) |

## Demo Account Seed Data

To reproduce the state shown in the screenshots:

### Account

| Field | Value |
|---|---|
| Username | `arjun_mehta` |
| Email | `arjun.mehta@example.com` |
| Password | Any 8+ character string (demo account) |

**Set the API key** to "Own key" (green) in the navbar — this is the state shown in every screenshot.

### Journal Entries

Write 23 entries through the UI with the content documented in the original seed script. Then run the SQL backdating script to spread them across dates.

**Entry dates:** August 11 → October 11, 2026, one or two per day.

**Themes represented:** work stress, project anxiety, small wins, gratitude, frustration with bugs, career reflection, self-doubt about a side project.

### Roadmap

| Field | Value |
|---|---|
| Goal | `Learn React Native` |
| Duration | `6 WEEKS` |
| Progress | Mark 3 of the first 5 tasks as complete (~30%) |

### Milestones

**Milestone 1 — "Launch personal portfolio site"**
- 5 tasks: 3 completed, 2 pending
- Run AI Insights once, expand the panel

**Milestone 2 — "Run a half-marathon"**
- 4 tasks: 1 completed, 3 pending

### Custom Tasks

| Title | Due | Est. Hours | Priority |
|---|---|---|---|
| Book dentist appointment | +2 days | 0.5 | HIGH |
| Renew passport | +14 days | 1.0 | MEDIUM |
| Read 30 pages | +1 day | 0.5 | LOW |
| Prep 1:1 with manager | +3 days | 1.0 | HIGH |

### Preferences

| Field | Value |
|---|---|
| Energy Peak | Morning |
| Wake Time | 07:00 |
| Sleep Time | 23:00 |
| Lunch Time | 13:00 |
| Daily Habits | `15 min meditation` and `30 min walk` |
| Difficulty | Intermediate |
| Language | English |
| Learning Style | Visual |
| Pace | 8 hrs/week |
| Avoid Weekends | ✅ |

### Schedule

Generate a schedule via the Schedule tab. Aim for at least 4–6 blocks visible in the current week.

### AI Coach

Ask once: *"Why do I feel so anxious when starting new projects?"* — this seeds the conversation for the chat screenshot.

## SQL Backdating Script

Run this after writing all 23 entries. It redistributes them across the intended dates based on insertion order.

```sql
SET @user_id = (SELECT id FROM users WHERE username = 'arjun_mehta');

CREATE TEMPORARY TABLE entry_dates (
    rn INT PRIMARY KEY,
    target_date DATE,
    target_timestamp DATETIME
);

INSERT INTO entry_dates (rn, target_date, target_timestamp) VALUES
(1,  '2026-08-11', '2026-08-11 21:15:00'),
(2,  '2026-08-14', '2026-08-14 19:30:00'),
(3,  '2026-08-17', '2026-08-17 22:00:00'),
(4,  '2026-08-20', '2026-08-20 07:30:00'),
(5,  '2026-08-24', '2026-08-24 20:45:00'),
(6,  '2026-08-27', '2026-08-27 21:00:00'),
(7,  '2026-09-01', '2026-09-01 18:00:00'),
(8,  '2026-09-04', '2026-09-04 19:15:00'),
(9,  '2026-09-07', '2026-09-07 20:30:00'),
(10, '2026-09-10', '2026-09-10 22:00:00'),
(11, '2026-09-13', '2026-09-13 21:30:00'),
(12, '2026-09-15', '2026-09-15 20:00:00'),
(13, '2026-09-18', '2026-09-18 22:15:00'),
(14, '2026-09-22', '2026-09-22 21:00:00'),
(15, '2026-09-25', '2026-09-25 07:45:00'),
(16, '2026-09-28', '2026-09-28 18:30:00'),
(17, '2026-10-01', '2026-10-01 20:15:00'),
(18, '2026-10-04', '2026-10-04 19:00:00'),
(19, '2026-10-07', '2026-10-07 08:00:00'),
(20, '2026-10-08', '2026-10-08 21:30:00'),
(21, '2026-10-09', '2026-10-09 22:00:00'),
(22, CURDATE(), CURDATE() + INTERVAL 8 HOUR),
(23, CURDATE(), CURDATE() + INTERVAL 20 HOUR);

UPDATE journal_entries je
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY creation_timestamp ASC, id ASC) AS rn
    FROM journal_entries
    WHERE user_id = @user_id
) ordered ON je.id = ordered.id
JOIN entry_dates ed ON ordered.rn = ed.rn
SET je.entry_date = ed.target_date,
    je.creation_timestamp = ed.target_timestamp;

DROP TEMPORARY TABLE entry_dates;
```

Verify with:

```sql
SELECT entry_date, COUNT(*) FROM journal_entries 
WHERE user_id = @user_id GROUP BY entry_date ORDER BY entry_date;
```

## What's NOT Documented Here

The old version of this file listed specific capture dimensions and per-screenshot specs. Those were aspirational and didn't survive contact with reality. Don't try to match them — just follow the conventions at the top of this file.