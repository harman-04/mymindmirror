// src/pages/FeaturesGuide.jsx
import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import ReactDOM from 'react-dom';
import { Link } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import {
  Sparkles, Brain, Target, Shield, ArrowRight,
  Layers, Zap, HeartPulse, Trophy,
  LineChart, ListChecks, MapPin, Download, Search,
  ChevronDown, ChevronUp, ListTree, Menu, X, Settings, Clock as ClockIcon,
  Calendar as CalendarIcon, Database, Cpu, Server, LockKeyhole,
  TestTube, MonitorSmartphone
} from 'lucide-react';
import FadeIn from '../components/FadeIn';

/**
 * Enterprise Markdown Parser
 * Optimized to handle bold, italics, ordered/unordered lists, and inline code blocks.
 */
const formatText = (text) => {
  if (!text) return '';

  const escapeHtml = (str) => {
    return str.replace(/[&<>]/g, (m) => {
      if (m === '&') return '&amp;';
      if (m === '<') return '&lt;';
      if (m === '>') return '&gt;';
      return m;
    });
  };

  const lines = text.split('\n');
  const result = [];
  let i = 0;
  const total = lines.length;

  while (i < total) {
    const line = lines[i];

    const bulletMatch = line.match(/^\s*(\*|\-)\s+(.*)/);
    const numberMatch = line.match(/^\s*(\d+)\.\s+(.*)/);

    if (bulletMatch || numberMatch) {
      const isOrdered = !!numberMatch;
      const listItems = [];

      while (i < total) {
        const currentLine = lines[i];
        const bullet = currentLine.match(/^\s*(\*|\-)\s+(.*)/);
        const number = currentLine.match(/^\s*(\d+)\.\s+(.*)/);

        if (bullet || number) {
          const content = bullet ? bullet[2] : number[2];
          let formatted = escapeHtml(content);

          formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
          formatted = formatted.replace(/\*(.*?)\*/g, '<em>$1</em>');
          formatted = formatted.replace(/`(.*?)`/g, '<code class="guide-code">$1</code>');

          listItems.push(`<li>${formatted}</li>`);
          i++;
        } else {
          break;
        }
      }
      const listTag = isOrdered ? 'ol' : 'ul';
      result.push(`<${listTag} class="guide-list">${listItems.join('')}</${listTag}>`);

    } else {
      let formatted = escapeHtml(line);

      formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      formatted = formatted.replace(/\*(.*?)\*/g, '<em>$1</em>');
      formatted = formatted.replace(/`(.*?)`/g, '<code class="guide-code">$1</code>');

      if (formatted.trim()) {
        result.push(`<p class="guide-paragraph">${formatted}</p>`);
      } else if (line === '') {
        result.push('<br/>');
      }
      i++;
    }
  }
  return result.join('');
};

const FeatureSectionCard = React.memo(React.forwardRef(({ section, isOpen, onToggle, colors }, ref) => {
  return (
    <div
      ref={ref}
      className={`rounded-2xl lg:rounded-3xl ${colors.cardBg} border ${colors.cardBorder} overflow-hidden transition duration-300 hover:shadow-md hover:-translate-y-1 hover:border-purple-500/30 dark:hover:border-teal-500/30 group scroll-mt-24 lg:scroll-mt-32`}
    >
      <button
        onClick={() => onToggle(section.id)}
        aria-expanded={isOpen}
        className="w-full flex justify-between items-center p-5 lg:p-8 text-left hover:bg-slate-50/50 dark:hover:bg-white/5 active:bg-slate-100 dark:active:bg-white/10 active:scale-[0.99] transition-all duration-200 focus:outline-none"
      >
        <div className="flex items-center gap-4 lg:gap-6 pr-4">
          {/* Applied Layer 3 Backgrounds to the icons! */}
          {/* Switched to Layer 2 (sectionBg) so the icon box perfectly contrasts against the Layer 1 Card! */}
          <div className={`p-3 lg:p-4 rounded-xl lg:rounded-2xl ${colors.sectionBg} shadow-sm border ${colors.sectionBorder} ${section.color}`}>
            {React.cloneElement(section.icon, { className: "w-6 h-6 lg:w-8 lg:h-8" })}
          </div>
          <h2 className={`text-lg sm:text-xl lg:text-2xl font-poppins font-extrabold tracking-tight ${colors.textPrimary} leading-tight`}>
            {section.title}
          </h2>
        </div>
        <div className={`shrink-0 p-2 lg:p-2.5 rounded-full transition-all duration-300 shadow-sm ${isOpen ? 'bg-purple-500 dark:bg-teal-500 text-white rotate-180' : `bg-slate-100 dark:bg-white/5 ${colors.textSecondary} group-hover:bg-slate-200 dark:group-hover:bg-white/10`}`}>
          <ChevronDown className="w-5 h-5 lg:w-6 lg:h-6" />
        </div>
      </button>

      {isOpen && (
        <div className={`p-5 lg:p-8 pt-0 lg:pt-0 border-t ${colors.sectionBorder} animate-in slide-in-from-top-4 fade-in duration-300`}>
          {/*  Added 'prose-slate' so Tailwind's typography engine perfectly aligns with our Master Palette! */}
          <div className={`guide-content prose prose-slate prose-sm md:prose-base lg:prose-lg dark:prose-invert max-w-none mt-6 lg:mt-8 ${colors.textPrimary}`}>
            <div dangerouslySetInnerHTML={{ __html: formatText(section.content) }} />
          </div>
          <div className={`mt-8 lg:mt-10 text-right border-t ${colors.sectionBorder} pt-4 lg:pt-6`}>
            <button
              onClick={() => onToggle(section.id)}
              className={`inline-flex items-center gap-2 text-sm lg:text-base font-bold ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 transition-colors px-5 py-2.5 rounded-full bg-slate-100 dark:bg-white/5 hover:bg-slate-200 dark:hover:bg-white/10 shadow-sm`}
            >
              <ChevronUp className="w-4 h-4 lg:w-5 lg:h-5" /> Close Section
            </button>
          </div>
        </div>
      )}
    </div>
  );
}));
FeatureSectionCard.displayName = 'FeatureSectionCard';

function FeaturesGuide() {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  const [openSection, setOpenSection] = useState('journal-and-analysis');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const sectionRefs = useRef({});
  const isAuthenticated = !!localStorage.getItem('jwtToken');

  const toggleSection = useCallback((sectionId) => {
    setOpenSection((prevSection) => {
      const isOpening = prevSection !== sectionId;

      if (isOpening) {
        setTimeout(() => {
          const element = sectionRefs.current[sectionId];
          if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'start' });
          }
        }, 50);
      }
      return isOpening ? sectionId : null;
    });
    setMobileMenuOpen(false);
  }, []);

  // ==========================================================================
  //  MASTER ELEVATION PALETTE (3-Layer Architecture)
  // ==========================================================================
  const colors = useMemo(() => ({
    cardBg: isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm',
    cardBorder: isDarkMode ? 'border-white/10' : 'border-slate-200/80',
    sectionBg: isDarkMode ? 'bg-[#131127]/80' : 'bg-slate-50/80',
    sectionBorder: isDarkMode ? 'border-white/5' : 'border-slate-200/60',
    innerContentBg: isDarkMode ? 'bg-black/20' : 'bg-white',
    textPrimary: isDarkMode ? 'text-gray-100' : 'text-slate-900',
    textSecondary: isDarkMode ? 'text-gray-400' : 'text-slate-500',
    //  Stripped backdrop blurs from sidebars for max performance
    //  Sidebar is now officially Layer 1 (matching cardBg)
    sidebarBg: isDarkMode ? 'bg-[#1A162F]/95 shadow-lg' : 'bg-white/95 shadow-lg',
    // Active button becomes Layer 2 (slate-50) so it contrasts against the white sidebar
    sidebarActiveBg: isDarkMode ? 'bg-white/10 border border-transparent' : 'bg-slate-50 shadow-sm border border-slate-200/80',
    sidebarActiveText: isDarkMode ? 'text-teal-400 font-bold' : 'text-purple-700 font-bold',
    drawerBg: isDarkMode ? 'bg-[#1A162F]/95' : 'bg-white/95',
  }), [isDarkMode]);

  const sections = useMemo(() => [
    {
      id: 'journal-and-analysis',
      title: 'Journaling, Real-Time Polling & Smart DOM Virtualization',
      icon: <Database />,
      color: 'text-purple-500 dark:text-purple-400',
      content: `
          **How to use:**
          - Write your deepest thoughts in the heavily optimized \`PremiumInput\` textarea on the Journal page.
          - **Overcoming Writer's Block:** A dedicated "Daily Inspiration" widget sits above your journal input. It utilizes a randomized, stateful array to surface therapeutic questions (e.g., "What made you smile today?"). It stores your recently seen questions in \`localStorage\` to ensure you never see the same question twice in a row when clicking the refresh button.
          - **Power-User Shortcut:** Click the **"Analyze & Save"** button, or simply press **Ctrl+Enter** (Cmd+Enter on Mac) to dispatch the entry instantly.
          - **Zero-Wait Workflow:** AI analysis runs entirely asynchronously in the background. You do not have to stare at a loading spinner. The UI instantly updates with a skeleton loader and fires a toast notification (*"AI is reading your thoughts..."*). You can immediately navigate away, create tasks, or log out.
          - **Smart Reactive Polling:** While the AI processes your entry, the frontend utilizes TanStack Query to poll the server exactly every 3 seconds. The moment the \`moodScore\` is calculated (indicating the analysis is complete), the skeleton seamlessly fades into your high-fidelity analysis.
          - **Live Re-Analysis:** You can edit past entries inline. Saving an edit immediately triggers a localized re-analysis (*"Re-analyzing your updated entry..."*), keeping your metrics historically accurate.

          **The AI processes your raw text and provides:**
          - **Calculated Mood Score:** Color-coded ranging from Very Negative (Red) to Very Positive (Emerald) (ranging from -1.0 = very negative to +1.0 = very positive).
          - **High-Fidelity Emotion Breakdown:** Plotted dynamically via an interactive Chart.js Doughnut chart natively embedded inside the entry row. Percentage intensities for 14 specific emotional states (e.g., joy, sadness, anger, fear, surprise, love, anxiety, relief, neutral, excitement, contentment, frustration, gratitude, hope).
          - **Core Concerns:** Key themes identified and categorized (e.g., "work stress", "academic pressure").
          - **Concise Summary:** A short, scan-friendly summary of the entry for timeline reading.
          - **Actionable Growth Tips:** Highly formatted rich markdown offering therapeutic advice.
          - **Key Phrases:** Extracted conceptual phrases utilized to build your semantic word cloud.
          - **1-Click Milestone Import:** Every growth tip features a dedicated inline \`+\` action button to instantly import that specific AI advice into your persistent Milestone Tracker.

          **Enterprise Architecture (How it works):**
          - **Smart DOM Virtualization Engine:** Rendering thousands of rich historical entries with embedded charts will instantly crash a mobile browser's RAM. The \`JournalHistory\` component features a dynamic rendering router. If the payload is under 20 items (e.g., the Today tab), it uses standard DOM mapping to prevent mathematical absolute-positioning bugs. If the payload exceeds 20 items (e.g., the History tab), it seamlessly routes to \`@tanstack/react-virtual\`, calculating scroll-offsets and painting *only* the visible rows onto the GPU.
          - **Two-Tone Layering System:** The history feed employs a strictly enforced 3-Layer CSS architectural palette. The outer card rests on Layer 1 (\`bg-[#1A162F]\`), the row headers sit on Layer 2 (\`bg-black/30\`), and the parsed AI markdown content is punched into Layer 3 (\`bg-black/20\`), creating a flawless, mathematically precise depth of field.
          - **Server-Side Encryption at Rest:** Your raw text is encrypted immediately on the backend before ever touching a disk. We use AES-CBC with a 256-bit cryptographic key derived via PBKDF2 from your BCrypt password hash. The key never leaves the server, and the ciphertext is unreadable without it — even if the disk or database is compromised.
          - **Asynchronous Spring AI & Polling:** The \`AsyncJournalAnalysisService\` takes over post-commit, offloading the heavy HTTP request to the native Spring AI client (no intermediate Flask layer). The frontend uses a reactive polling hook to check for \`moodScore != null\`; when it appears, the analysis is complete.
          - **Virtual Threads & Non-Blocking I/O:** All AI calls are executed on **virtual threads** (Spring Boot 4 / Java 25), allowing thousands of concurrent long-running requests without exhausting OS threads. The analysis runs entirely outside any database transaction (using \`TransactionTemplate\`), so the database connection is released during the AI wait.
          - **Model Cascade Resilience:** The backend uses a dynamic model cascade (multiple Gemini models) with intelligent cooldown logic. If one model hits a rate limit, the system automatically falls back to the next available model without user-visible interruption.
          - **pgvector Integration:** Once analyzed, the \`EmbeddingGenerationService\` automatically generates a 3072-dimensional vector embedding of your entry's summary and key phrases, storing it in a secondary PostgreSQL database for ultra-fast semantic similarity searches.
          - **Pre-Aggregated Performance:** A MySQL table (\`daily_journal_summary\`) is maintained via stored procedures and triggers, calculating daily mood and word averages on the fly as entries are inserted. This allows analytics charts to render in <10ms even with thousands of rows.
        `,
    },

    //           - **O(1) Chart Performance:** A pre-aggregated MySQL table (\`daily_journal_summary\`) is maintained via database triggers. It calculates your daily mood and word averages on the fly as entries are inserted, allowing analytics charts to render in <10ms even with thousands of rows.

    {
      id: 'rag-ai-coach',
      title: 'AI Reflection Coach (RAG & Redis Session Memory)',
      icon: <Brain />,
      color: 'text-pink-500 dark:text-pink-400',
      content: `
            **How to use:**

            - Navigate to the **AI Coach** tab on your Journal page.
            - The system will immediately greet you with a deeply personalized reflective question (*"Reviewing your journal..."*). This isn't random—it is synthetically generated based on a mathematical synthesis of your recent journal entries.
            - **Quick-Start Prompts:** Don't know what to say? Click any of the interactive **Suggestion Chips** (e.g., *"What can I do to feel better?"* or *"How can I apply this to my life?"*) to instantly auto-fill the input box.
            - **Continuous Memory:** The AI remembers what you just said! Use the **Memory ON/OFF** toggle to control whether the AI maintains a flowing, multi-turn conversation or treats each message as a clean slate.
            - **Append vs. Replace:** Toggle between **Append** (keeps the full chat history visible like a messaging app) or **Replace** (clears the screen for a fresh thought, overwriting the last UI block).
            - Use the **Refresh** button to force the AI to generate a completely new reflective question.
            - Use the **Trash (Clear Memory)** button to definitively wipe the conversation and reset the AI's internal state.
            - Easily extract insights using the quick-copy button on any generated message.

            **Enterprise Architecture (How it works):**

            - **Retrieval-Augmented Generation (RAG):** Standard chatbots suffer from immediate amnesia or generic advice. MyMindMirror utilizes advanced RAG. When you ask a question, the backend queries the PostgreSQL \`pgvector\` database to find your most highly relevant past entries (Cosine similarity threshold > 0.50, configurable via \`app.rag.similarity-threshold-fallback\`).
            - **Context Injection & Decryption:** These past memories are fetched, decrypted safely in active memory, and injected directly into the LLM system prompt. This forces the AI to ground its advice purely on your actual lived experiences rather than generic internet training data.
            - **Redis Session Memory TTL:** Powered by the \`useSendChatMessage\` and \`useClearChatMemory\` hooks, your active conversation is maintained in a lightning-fast Redis cache using a rolling window of the last 10 messages. Hitting the "Clear Memory" button fires an explicit \`DELETE /api/chat/clear-memory\` payload to instantly purge this Redis cache, ensuring absolute privacy control.
            - **High-Reasoning Model Cascade:** The chat engine dynamically routes tasks through a prioritized model cascade:
              - \`gemini-3.6-flash\` (primary)
              - \`gemini-3.5-flash\` (fallback)
              - \`gemini-3.1-pro-preview\` (secondary)
              - \`gemini-2.5-pro\` (last resort)
              If a model hits a rate limit or returns an error, the system instantly falls back to the next model without user‑visible interruption.
            - **Intelligent Cooldown Logic:** When a model fails (rate limit, 404, or JSON parsing error), it is placed on cooldown:
              - **60 seconds** for rate limits / parsing errors.
              - **24 hours** for 404 Not Found (deprecated endpoints).
              This ensures dead models are not retried and quota is preserved.
            - **Virtual Threads:** All AI calls execute on virtual threads (Spring Boot 4 / Java 25), enabling thousands of concurrent chat sessions without exhausting OS threads.
            - **Smart Client-Side Caching:** To preserve backend quotas, your initial AI-generated reflection question is serialized into the browser's \`sessionStorage\` with a strict 1-hour Time-To-Live (TTL). If you navigate away and come back, the UI rehydrates instantly without firing a redundant HTTP request.
            `,
    },

    {
      id: 'roadmap-planner',
      title: 'AI Roadmap Planner, Dual-Views & Chunked Generation',
      icon: <MapPin />,
      color: 'text-teal-500 dark:text-teal-400',
      content: `
              **How to use:**

              - Navigate to the **Roadmap** tab and click **"+ New Roadmap"** to launch the initialization engine.
              - **Precision Scoping:** Enter your target goal and define the exact duration (Days, Weeks, Months, or Years).
              - **Custom Override Panel:** Need this roadmap to be different from your global profile? Check "Use custom preferences" to locally override your Difficulty (Beginner/Intermediate/Advanced), Language, Learning Style, and Weekend scheduling rules just for this specific plan.
              - **Dual-View Interface:** Once generated, seamlessly toggle between two distinct UI architectures:
                - *List View:* A deeply nested, accordion-style vertical breakdown. Click "Expand All" to scan tasks, subtasks, and rich-text details instantly.
                - *Timeline View:* A sleek, horizontal CSS Grid mapping your tasks across a sticky weekly header, perfect for high-level project management.
              - **Deep Task Elaboration:** Sometimes a roadmap task like "Learn Context API" isn't enough. Click on the task to open the Detail Modal and click **"Generate Detailed Guide"**. The AI dynamically expands that single line into a massive guide, breaking the concept down into 4–6 actionable micro-subtasks, warning you about common pitfalls, and providing an exact hourly time estimate.
              - **Infinite Chunking:** To protect performance, long roadmaps are generated in chunks. Click **"Load Next 6 Weeks"** at the bottom of the list to seamlessly append the next sequence of your journey.
              - **Dynamic Adjustments:** Falling behind? Click **"Smart Reschedule"** to mathematically shift pending tasks forward, or click **"Generate Next Steps"** to dynamically adapt the roadmap based on your current completion rate.
              - **1-Click Sync:** Click the **"+ Add"** button on any roadmap task to instantly port it over to your Milestone Tracker. Imported tasks automatically receive a default due date of **7 days from now**, ensuring they appear in your schedule immediately.

              **Enterprise Architecture (How it works):**

              - **Advanced Style & Language Mirroring:** The elaboration engine performs active linguistic analysis. The system explicitly detects if your tasks are written in English, Spanish, or even mixed code-switching languages like *Hinglish*. It forces the LLM to generate the elaborate guides in the exact same language and stylistic tone (casual, formal, or motivational) as your original input.
              - **Context-Preserving Batch Generation:** To eliminate LLM context-window overflow and minimize network latency, the \`useContinueRoadmapBatch\` hook processes roadmaps incrementally. The backend bundles a state vector of previously produced items into a compressed summary frame, feeding it back into the LLM to guarantee logical continuity for the next 6 weeks.
              - **Stateful Progression Tracking:** The database column \`generated_weeks\` registers the current terminal point of concrete generation. When a user requests an expansion, the backend bundles a state vector of previously produced items into a compressed summary contextual frame, feeding it back into the LLM to guarantee logical continuity.
              - **Subtask and Asset Serialization:** Tasks are stored with explicit mappings for \`details\` (TEXT) and \`subtasks_json\` (JSON data type array) right inside the relational MySQL schema, preventing brittle nested array string splitting.
              - **Cartesian Product Elimination:** We use JPA \`@EntityGraph\` to fetch only the \`tasks\` collection eagerly in a single optimized SQL query, while \`@BatchSize\` efficiently loads \`resources\` and \`milestones\` in a secondary query. This completely eliminates the Cartesian product (duplicate task rows) that previously caused massive data duplication in the UI.
              - **UUID‑Based Reschedule (Stable Mapping):** The Smart Reschedule feature no longer uses fragile list indices. The AI receives the exact \`taskId\` (UUID) for each task and returns those IDs. This guarantees that even if task order changes, the correct tasks are always updated.
              - **Default Due Dates for Imported Tasks:** When a task is synced to the Milestone Tracker, the backend automatically assigns a due date of \`LocalDate.now().plusDays(7)\`, ensuring all imported tasks have a concrete timeline and never appear blank in the UI.
              - **Virtual Threads & AI‑Out‑of‑Transaction:** All AI calls (generation, elaboration, reschedule) are executed outside any database transaction using \`TransactionTemplate\`. This ensures database connections are not held during the 5‑10 second AI wait, preventing connection pool exhaustion. Virtual threads further improve concurrency, enabling hundreds of simultaneous roadmap operations.
              - **Dual-DOM Rendering Engine:** The \`RoadmapPlanner\` component dynamically swaps its rendering tree based on your view state. The Timeline view maps to \`RoadmapTimeline.jsx\`, utilizing a strictly defined CSS Grid (\`gridTemplateColumns: 100px repeat(N, minmax(200px, 1fr))\`) with sticky headers to ensure zero layout-thrashing during horizontal scrolling.
              - **Custom Markdown & Annotation Parsing:** The AI generates highly complex instructional text. The frontend utilizes a bespoke \`formatText\` and \`formatInline\` parser that sanitizes HTML, converts markdown to Tailwind typography, and explicitly hunts for programmatic annotations (e.g., \`@Override\` or \`@Bean\`), wrapping them in premium Java/C# style pill badges.
              - **Optimistic UI Updates:** The \`useToggleTaskCompletion\` hook utilizes TanStack Query's \`onMutate\` lifecycle method. When you check off a task, the UI updates instantly (Optimistic UI), while the actual \`PATCH\` request fires in the background. If the network request fails, the cache automatically rolls back to its previous state.
              - **Transactional Synchronization Listener:** A dedicated database-level mapping links \`milestone_tasks\` to their respective \`roadmap_tasks\`. When a PATCH request alters a status flag, an asynchronous application event triggers, forcing an atomic dual-write operation across your schemas to maintain 100% accurate system consistency.
              `,
    },
    {
      id: 'milestone-tracker-insights',
      title: 'Milestone Tracking, Circular Progress & AI Strategic Insights',
      icon: <Target />,
      color: 'text-amber-500 dark:text-amber-400',
      content: `
       **How to use:**

       - Navigate to the **Milestones** tab to manage your active, high-level objectives.
       - **Create & Manage:** Click **"Add Milestone"** to define a target goal and due date. As you add tasks and subtasks underneath it, the system calculates your completion rate.
       - **Dynamic Progress Rings:** Every milestone features a stunning, animated circular progress bar. The color mathematically shifts based on your momentum (e.g., Red for < 25%, Amber for > 25%, Blue for > 75%, and Emerald for 100% completion).
       - **Cross-Module Integrations:** The Milestone Tracker acts as the central hub for the entire application. Tasks generated in the **Roadmap Planner** can be beamed directly here (indicated by a special "from Roadmap" badge) – these tasks automatically receive a default due date of 7 days from now.
       - **Automated Growth Tips (Upgraded Workflow):** Instead of manually copying and pasting self-help text into a parser, Growth Tips are now automatically extracted during your Journal Analysis. Simply click the **"+" (Add to Milestones)** button on any journal tip to instantly port it into an actionable task – each tip task receives a default due date of 3 days from now.
       - **AI Strategic Insights:** Feeling stuck? Click the **Lightbulb** icon on any active milestone. The AI will evaluate your pending vs. completed tasks, read your subtask data, and generate a highly structured performance assessment, surfacing remaining work, actionable tips, and exact next steps.
       - **Deep Task Editing:** Click the Edit (pencil) icon on any task to open a comprehensive editor. You can dynamically add, edit, or remove nested subtasks, update statuses, and write rich-text markdown details directly into the task.

       **Enterprise Architecture (How it works):**

       - **Optimistic UI with Temporary IDs:** To ensure the interface feels lightning-fast, the \`useMilestoneData\` hooks leverage TanStack Query's \`onMutate\` lifecycles. When you create a task, the system immediately paints it to the DOM using a \`temp-task-\${Date.now()}\` ID. Once the server confirms the MySQL database insertion, the temporary ID is silently swapped for the true database UUID, resulting in zero perceived latency.
       - **Robust Structural Extraction:** Parsing unstructured raw text (like an AI Growth Tip) into highly structured database rows is notoriously difficult. We utilize Spring AI's \`BeanOutputConverter\` mapped to a strict \`GrowthTipParseResponse\` Java Record. The AI is forced to output a JSON array of objects containing a title, description, and subtasks array, which the backend then instantly persists to the MySQL \`tasks\` table.
       - **Default Due Dates for Imported Tasks:** When a growth tip is imported, the backend automatically assigns a due date of \`LocalDate.now().plusDays(3)\` to each generated task. Roadmap tasks synced to milestones get \`plusDays(7)\`. This ensures all imported tasks have a concrete timeline and never appear blank in the UI.
       - **AI Call Outside Transaction:** The growth tip parsing AI call runs entirely outside any database transaction (using \`TransactionTemplate\`). Only the final batch insert of tasks is wrapped in a short transaction, preventing connection pool starvation during the AI wait.
       - **Clean Controller Layer with \`@CurrentUser\`:** All milestone endpoints now use the \`@CurrentUser\` annotation resolver – controllers no longer manually fetch users from the database. This makes the web layer thin and eliminates duplicate user‑lookup code.
       - **Complex Subtask Arrays:** Subtasks are managed using dynamic React state arrays (\`newTaskSubtasks\`). When persisted, they are serialized and stored efficiently within the database, preventing the need for an overly complex relational schema for simple checklist items.
       - **Non-Blocking Insight Generation:** Generating the AI Strategic Insights requires heavy LLM processing. The frontend explicitly disables the specific Lightbulb button and shows a targeted loading spinner, ensuring the rest of the application remains fully interactive while the background thread pool computes the response.
       - **Gamification Event Triggers:** The Milestone ecosystem is heavily wired into the Gamification engine. Creating milestones, adding tasks, completing items, and generating insights all fire automated \`queryClient.invalidateQueries(['gamificationStats'])\` triggers, instantly rewarding the user with XP in the background.
       - **Virtual Threads & Model Cascade:** The insight generation and growth tip parsing both leverage virtual threads and the dynamic model cascade, ensuring reliability even during high‑load periods.
       `
    },
    {
      id: 'smart-timetable-scheduling',
      title: 'Smart Timetable Engine, Drag-and-Drop & Re-optimization',
      icon: <CalendarIcon />,
      color: 'text-orange-500 dark:text-orange-400',
      content: `
  **How to use:**

  - Access the interactive **Schedule & Calendar** ecosystem directly from your Dashboard navigation.
  - **Define Boundaries:** First, ensure your base constraints are configured by specifying your weekly operational allocations under Profile → "Your Available Hours" (e.g., configuring exact active intervals such as Monday 09:00–12:00 and 13:00–17:00).
  - **Routing Modes:** Toggle between **"All Tasks"** (pools open entries from Roadmaps, Milestones, and Custom Tasks) or **"Custom"** (restricts scheduling strictly to standalone tasks created on this page).
  - Click **"Generate"** to invoke the AI engine. The system automates the layout of your agenda items, populating your calendar views while respecting defined deadlines, item weight, and priorities.
  - **Visual Block Types:** The AI doesn't just schedule work. It intelligently partitions your day by generating specialized \`MEAL\`, \`BREAK\`, and \`ROUTINE\` blocks, each rendered with unique iconography (Utensils, Coffee, Activity) to prevent visual fatigue.
  - **Fluid Drag-and-Drop Adjustment:** Directly adjust your daily distribution by dragging event blocks into new time slots. You can even grab items from the "Unscheduled Custom Tasks" list and drop them directly onto the calendar grid!
  - **In-Line Completion Checkboxes:** Click the check action directly on any calendar node to instantaneously close an event, propagating completion triggers down to its source origin task.
  - **"Re-optimise" (Salvage the rest of today):** Life happens. If you fall behind schedule, click the Re-optimise button. The AI recalculates your remaining daylight hours and instantly shifts your pending tasks without destroying tomorrow's schedule.
  - **Custom Calendar Tasks:** Add standalone custom tasks using the "Add Task" button (title, description, due date, estimated hours, priority) which can be injected straight into the AI scheduling pool.

  **Enterprise Architecture (How it works):**

  - **Constraint-Satisfaction Engine:** The schedule processor compiles your availability patterns into a structured contextual array. It forwards this data payload to the generative ML routing service to resolve optimal temporal distribution.
  - **In‑Memory Task Maps (N+1 Elimination):** Before scheduling begins, the backend preloads all roadmap, milestone, and custom tasks into lightweight in‑memory maps (\`TaskMaps\`). This eliminates the need for thousands of individual \`existsById\` and \`findById\` queries inside the scheduling loop, dropping scheduling time from ~30 seconds to under 1 second.
  - **Batch Saving for Bulk Inserts:** Instead of saving each scheduled task individually via \`save()\`, the system collects all generated tasks into a list and executes a single \`saveAll()\` batch insert. Combined with \`hibernate.jdbc.batch_size=50\` and \`rewriteBatchedStatements=true\`, this reduces database round‑trips from hundreds to a single network pass.
  - **Simulated Deletion (Data‑Safe Rescheduling):** When regenerating a schedule, the AI must be told which tasks are already scheduled. Instead of deleting tasks before the AI call (which would cause data loss if the AI fails), we simulate the deletion in memory – we only tell the AI about tasks that are already completed. The actual database deletion is deferred and executed in the final transactional step, ensuring zero data loss if the AI call fails.
  - **Strict Date & Time Parsing:** All schedule endpoints use dedicated DTOs with typed \`LocalDate\` and \`LocalTime\` fields. Invalid formats (e.g., "25:00") are caught by Jackson's built‑in deserialization and automatically return a clean 400 Bad Request via the global exception handler.
  - **Deterministic Fallback Loop:** If upstream AI rate limits are tripped, the backend drops down seamlessly to a localized, high-speed deterministic greedy scheduler. This fallback maps out items sequentially based on immediate deadline weight and priority scores to ensure zero runtime interruptions.
  - **REST Optimization & Partial Updates:** Modifying a task's temporal coordinates via dragging invokes a highly optimized \`PUT /api/schedule/task/{id}/move\` endpoint. This passes clean delta payloads containing only the modified timestamp coordinates rather than heavy, full-object representations, saving massive network bandwidth.
  - **Virtual Threads & AI‑Out‑of‑Transaction:** The AI scheduling call runs entirely outside any database transaction using \`TransactionTemplate\`. This releases the database connection during the AI wait. Virtual threads further improve concurrency, allowing hundreds of simultaneous schedule generations.
  - **Headless Calendar DOM Manipulation:** We utilize \`react-big-calendar\` for the mathematical grid layout, but we completely disabled its default, clunky toolbars via CSS overrides. Instead, we drive the calendar state via headless React state variables (\`currentView\`, \`currentDate\`), mapping them to our custom, Master Palette-compliant buttons for a seamless, native feel.
  - **Real-Time Synchronous Mutation:** The underlying \`scheduled_tasks\` relational table maintains loose-coupling references to source tasks. This enables a single mutations class to execute cascade event dispatches, updating state across custom tasks, roadmap structures, or milestone components cleanly.
  `,
    },

    {
      id: 'custom-tasks-adhoc-scheduling',
      title: 'Custom Tasks & Ad-Hoc Scheduling',
      icon: <ListChecks />,
      color: 'text-indigo-500 dark:text-indigo-400',
      content: `
      **How to use:**
      - Navigate to the **Schedule** tab and click **"Add Task"** to create a task that isn't tied to any milestone or roadmap.
      - Custom tasks are perfect for one-off items: "Call dentist", "Renew passport", "30-min deep work sprint."
      - Every custom task has a title, optional description, due date, estimated hours, and priority (HIGH / MEDIUM / LOW).
      - Once created, custom tasks appear in the **Unscheduled Custom Tasks** pool below the calendar.
      - **Two ways to schedule:** Drag any custom task directly onto the calendar grid, or click **"+ Add"** to open a manual scheduling dialog.
      - Completing a custom task from the calendar propagates the completion to the master record and any other scheduled entries pointing to the same custom task ID.

      **Enterprise Architecture (How it works):**
      - **Independent Lifecycle:** Custom tasks have their own table (\`custom_tasks\`), their own CRUD endpoints, and their own gamification hook. They're not nested under milestones the way regular tasks are — this makes them the right tool for ad-hoc work that doesn't fit a broader goal structure.
      - **Cross-Entity Completion Sync:** When you check off a scheduled block linked to a custom task, the backend cascades the completion to the parent \`custom_tasks\` row AND to every other scheduled entry with the same \`customTaskId\`. This guarantees you can't have a completed calendar block pointing to an incomplete task.
      - **Cascade Delete:** Deleting a custom task deletes every scheduled entry linked to it. No orphaned calendar blocks.
      - **Priority-Driven Sorting:** The unscheduled pool sorts custom tasks by priority (HIGH → MEDIUM → LOW), then by due date. The AI scheduler applies the same ordering when auto-scheduling custom tasks in "custom-only" mode.
      - **Gamification Hook:** Every custom task creation fires a \`TASK_CREATE\` event, awarding 5 XP and potentially unlocking the \`VISIONARY\` badge chain on first use.
      `,
    },

    {
      id: 'lifestyle-personalization-engine',
      title: 'Lifestyle Engine & Global AI Personalization',
      icon: <Settings />,
      color: 'text-cyan-500 dark:text-cyan-400',
      content: `
  **How to use:**

  - Navigate to your **Account Settings** to configure your core AI generation parameters. The system is divided into two deeply integrated personalization engines:
  - **1. Roadmap Defaults:**
    - *Difficulty Matrix:* Choose from Beginner, Intermediate, or Advanced.
    - *Global Language Target:* Set your default language (e.g., English, Hindi, Spanish, French, German). The system will instantly command the underlying LLMs to translate roadmap outputs to this language natively.
    - *Learning Style Profile:* Select Reading (Docs), Visual (Video), or Hands-on.
    - *Pace & Boundaries:* Define your target hours per week and toggle the "Avoid Weekends" strict policy to prevent the planner from overscheduling your personal rest windows.
  - **2. The Lifestyle & Learning Engine:**
    - *Bio-Rhythms:* Set your exact Wake Time, Sleep Time, and Lunch Break intervals, along with your primary "Energy Peak" (Morning, Afternoon, or Evening).
    - *Daily Non-Negotiable Habits:* Enter your persistent daily routines into the multi-line text area (e.g., "15 mins meditation", "Read 10 pages").
    - *Availability Grid:* Define your exact available working blocks for every single day of the week using the dynamic timeline grid. Click "+ Add" to create multiple blocks per day, or use the trash icon to clear a day completely.

  **The AI Integration (What it does):**

  - Every time you execute a roadmap generation, the system injects your defaults directly into the foundational context window. An "Advanced" roadmap skips standard definitions entirely, while a "Visual" setting mandates the compilation of video asset pathways.
  - When the Smart Timetable runs, it reads your complete Lifestyle Matrix. It will automatically invent and inject specialized "ROUTINE" blocks for your daily habits, safely quarantine your lunch break, strictly avoid scheduling past your sleep time, and route heavy "WORK_TASKS" directly into your designated high-energy window.

  **Enterprise Architecture (How it works):**

  - **JSON Serialization Engine:** Your daily habits and availability blocks are complex nested arrays. To optimize performance, the frontend stringifies these directly into \`availableHoursJson\` and \`dailyHabitsJson\`. They are stored in MySQL as raw JSON strings, eliminating the overhead of processing multi-table relational joins during intensive scheduling sequences.
  - **DTO‑Based Endpoints with Validation:** All preference update endpoints now use structured DTOs with typed fields (\`LocalTime\`, \`LocalDate\`, \`UUID\`). Invalid formats are automatically caught by Jackson's built‑in deserialization and return a clean 400 Bad Request via the global exception handler – no more manual null checks or \`DateTimeParseException\` handling in controllers.
  - **Centralised Cache Keys:** Cache names are defined once in \`CacheConstants\` and used everywhere via static imports. This eliminates typos and makes global cache eviction strategies easy to maintain.
  - **Redis Caching & DTO Projection:** User configurations are heavy data structures. The backend extracts them into lightweight Data Transfer Objects (DTOs) and caches them in Redis via \`@Cacheable\` with keys from \`CacheConstants\`. This drops configuration fetch latencies to sub-milliseconds.
  - **Clean Controllers with \`@CurrentUser\`:** All lifestyle endpoints use the \`@CurrentUser\` annotation resolver – controllers no longer manually fetch users, making the web layer thin and eliminating duplicate user‑lookup code.
  - **Dynamic Prompt Engineering:** Inside the scheduling services, your lifestyle preferences are formatted into strict system rules: *"IRON-CLAD RULE 3: 'WORK_TASK' blocks MUST strictly fall within the 'Strict Available Work Hours': {availableHoursJson}"*.
  - **Polymorphic JSON Storage:** Your daily habits and availability blocks are stored directly inside the MySQL database as raw JSON strings. This eliminates the overhead of processing multi-table relational joins for simple arrays, heavily optimizing database read operations during intensive scheduling sequences.
  - **Virtual Threads Ready:** All preference operations are non‑blocking and leverage virtual threads for optimal concurrency.
  `,
    },
    {
      id: 'dynamic-gamification',
      title: 'Dynamic Gamification, Animated Progress & Badge System',
      icon: <Trophy />,
      color: 'text-amber-500 dark:text-amber-400',
      content: `
       **How to use:**

       - **Earn Experience (XP):** Every meaningful action you take (journaling, completing tasks, generating roadmaps, chatting with the AI) rewards you with XP. As you accumulate XP, your global **Mental Mastery Level** increases (every 500 XP = 1 Level).
       - **Animated Progress:** Watch your stats grow. When you visit the Achievements page, your Total XP, Current Streak, Longest Streak, and Task counts visually animate from zero to their actual values, providing satisfying immediate feedback.
       - **Real-Time Badge Unlocks:** The system actively monitors your progression. If an action pushes you over a badge threshold, a stunning "Badge Unlocked!" notification will instantly slide onto your screen.
       - **"Closest Target" Engine:** Scroll to the bottom of your trophy room. The UI automatically analyzes your unearned badges and surfaces your immediate "Next Badge" objective (e.g., "Write 5 journal entries"), giving you a clear micro-goal to focus on.

       **XP Distribution Table:**
       - **50 XP:** Saving a Journal Entry
       - **30 XP:** Generating a Smart Schedule Timetable
       - **30 XP:** Architecting a new AI Roadmap
       - **20 XP:** Completing a Roadmap, Milestone, or Custom Task
       - **20 XP:** Generating an AI Milestone Insight
       - **15 XP:** Chatting with the AI Reflection Coach
       - **15 XP:** Generating the "Today's Reflection" summary
       - **10 XP:** Creating a new Milestone manually
       - **5 XP:** Creating a micro-task or using the AI "Elaborate Task" feature

       **Unlockable Achievement Categories (16 Total Badges):**
       - *Consistency:* **3-Day Streak** (\`THREE_DAY_STREAK\`), **7-Day Streak** (\`SEVEN_DAY_STREAK\`), **30-Day Legend** (\`THIRTY_DAY_LEGEND\`)
       - *Productivity:* **First Step** (\`FIRST_STEP\`), **Task Master** (\`TASK_MASTER\`), **Productivity Ninja** (\`PRODUCTIVITY_NINJA\`), **Roadmap Finisher** (\`ROADMAP_FINISHER\`)
       - *Introspection:* **First Thought** (\`FIRST_THOUGHT\`), **Reflective Soul** (\`REFLECTIVE_SOUL\`), **First Chat** (\`FIRST_CHAT\`), **AI Whisperer** (\`AI_WHISPERER\`)
       - *Planning:* **Time Lord** (\`TIME_LORD\`), **Visionary** (\`VISIONARY\`), **Architect** (\`ARCHITECT\`), **Introspective** (\`INTROSPECTIVE\`), **Deep Diver** (\`DEEP_DIVER\`)

       **Enterprise Architecture (How it works):**
       - **Type‑Safe Gamification Actions:** All XP triggers now use the \`GamificationAction\` enum (e.g., \`GamificationAction.TASK\`, \`GamificationAction.JOURNAL\`). This eliminates magic strings and prevents typos, making the code more maintainable and discoverable.
       - **Service‑Layer Triggers (Not Controllers):** Gamification XP is awarded directly inside the service methods (e.g., \`JournalReflectionService\`, \`RoadmapService\`, \`TaskService\`), not in controllers. This ensures XP is always awarded regardless of how the service is invoked (e.g., via API, scheduled jobs, or internal calls).
       - **Global Cache Invalidation:** The gamification engine feels instantaneous because it leverages TanStack Query. Whenever you execute a mutation anywhere in the app (e.g., checking off a milestone task), the hook immediately fires \`queryClient.invalidateQueries(['gamificationStats'])\`. The background fetches the new XP total and smoothly updates the progress rings in your sidebar.
       - **Centralised Cache Keys:** Cache names are defined once in \`CacheConstants\` (e.g., \`GAMIFICATION_STATS\`) and used everywhere. This eliminates typos and ensures consistent cache eviction strategies across the codebase.
       - **Stateful Notification Memory:** To prevent the "Badge Unlocked" popup from spamming the user on every page load, the frontend compares incoming badge arrays against a \`previousBadges\` object stored in the browser's \`localStorage\`. The celebration animation is only triggered if a strict mathematical difference is detected.
       - **Atomic Transactions (Backend):** The \`GamificationService\` evaluates your progression state entirely inside strict \`@Transactional\` database locks, guaranteeing that concurrent task completions (like checking off 3 tasks rapidly) don't cause race conditions or drop XP points.
       - **JSON Object Persistence:** To avoid heavy relational join tables for achievements, earned badges are serialized using Jackson \`ObjectMapper\` into a highly efficient flat \`badges_json\` string column inside the \`user_stats\` table.
       - **High-Speed Redis Caching:** Because Gamification Stats are displayed constantly across the dashboard header and sidebars, the \`/api/gamification/stats\` endpoint is heavily cached in Redis under the \`gamificationStats\` namespace (using \`CacheConstants.GAMIFICATION_STATS\`). Cache eviction policies are surgically applied so the UI updates instantly, but database read-load remains virtually zero.
       - **Clean Controllers with \`@CurrentUser\`:** The gamification endpoints use the \`@CurrentUser\` annotation resolver – controllers no longer manually fetch users, making the web layer thin and eliminating duplicate user‑lookup code.
       `,
    },

    {
      id: 'high-res-visual-analytics',
      title: 'High-Resolution Visual Analytics & Chart.js Architecture',
      icon: <LineChart />,
      color: 'text-blue-500 dark:text-blue-400',
      content: `
    **How to use:**

    - Navigate to the **Journal** analytics environment and switch cleanly between three presentation layers:
      - *Today Dashboard:* An immediate evaluation snapshot featuring today's AI reflection and your current emotion breakdown.
      - *Weekly Dashboard:* High-fidelity tracking arrays focused on short-term mood variances and recent core concern weights.
      - *History (Overall) Dashboard:* The ultimate chronological viewport containing your complete multi-year line charts, calendar heatmaps, and semantic phrase clouds.

    **The Upgraded Chart.js Arsenal (8 Custom Engines):**

    - **Daily Emotion Breakdown (Doughnut Chart):** Renders today's aggregated emotional distribution. The chart cutout is mathematically matched to the exact hex code of the Layer 1 card background (\`#1A162F\`), creating a flawless, transparent "floating" effect.
    - **Top Primary Emotions (Bar Chart):** Isolates your top 7 strongest emotional drivers. Custom HTML5 Canvas \`createLinearGradient\` functions are used to paint each bar with a fading opacity gradient based on its specific emotional hex color.
    - **Frequent Concerns (Bar Chart):** Maps your top 25 AI-extracted life concerns into a frequency distribution. Because the X-axis can be massive, it is wrapped in our custom dynamic-width container to prevent the labels from overlapping.
    - **Overall Mood Timeline (Line Chart):** Plots your mathematically derived mood score (-1.0 to +1.0). Enabled with \`chartjs-plugin-zoom\`, allowing seamless horizontal dragging and \`Ctrl + Scroll\` wheel zooming across thousands of data nodes.
    - **Consistency Grid (Heatmap):** A GitHub-style contribution calendar mapping tracking density over a 365-day boundary. Days featuring intense emotional evaluations are shaded dynamically using deeper opacity configurations.
    - **Sprawling Emotional Profile (Radar Chart):** A full-width circular spider configuration plotting the absolute intensity averages of **14 baseline emotions** simultaneously.
    - **Expression Correlation Matrix (Scatter Plot):** Maps word volume metrics (X-axis) against structural mood values (Y-axis). The system automatically calculates a Pearson correlation coefficient (r) and draws a linear regression trend line to show if writing more correlates with feeling better.
    - **Animated Key Phrase Cloud:** A bespoke, physics-based word cloud where extracted AI phrases literally float and bounce on the screen using randomized \`@keyframes\`. Clicking any phrase fires a React state change, filtering your historical timeline instantly.

    **Enterprise Architecture (How it works):**

    - **Centralised Date Parsing (\`DateUtil\`):** All analytics endpoints now use the centralised \`DateUtil\` utility for parsing date strings. Invalid formats are automatically handled and return a clean 400 Bad Request via the global exception handler. No more manual \`try-catch\` or \`DateTimeParseException\` handling in controllers.
    - **Lightweight Trends Fetch (No Decryption):** The key phrase frequency endpoint (used for the word cloud) now uses \`getEntriesForTrendsWithoutDecryption\`. This skips the expensive AES decryption of \`rawText\` because the trends logic only needs the pre‑parsed \`keyPhrases\` from the database. This reduces CPU load by ~80% for large history ranges.
    - **DTO Mapping via \`JournalMapper\`:** All journal entry responses are mapped using \`JournalMapper\` (with \`JsonMapperHelper\` for JSON parsing). This removes manual builder methods and ensures consistent transformation across all analytics endpoints.
    - **Pagination via \`PageResponse\`:** The history list leverages a standard \`PageResponse\` DTO containing content, page number, page size, total elements, and total pages. The frontend uses this to drive the pagination controls efficiently.
    - **Canvas Rendering (Deprecating Recharts):** By completely purging the unoptimized, heavy Recharts library and migrating to \`chart.js\`, rendering engine overhead dropped significantly. Chart.js leverages HTML5 canvas rendering instead of heavy DOM-based SVG manipulation, freeing up massive CPU cycles.
    - **Anti-Compression Dynamic Widths:** To prevent massive datasets from "squishing" into unreadable spikes, the frontend mathematically calculates the minimum required pixels per data point (\`labels.length * minWidthPerPoint\`). The charts render into a dynamically expanding container wrapped in a smooth CSS \`overflow-x-auto\` scrollbar.
    - **Component Memoization Layouts:** Every chart container is isolated using structural React components heavily wrapped within \`React.memo\`, \`useMemo\`, and \`useCallback\` hooks. Mathematical operations (like the Linear Regression in the scatter plot) only re-evaluate when their underlying coordinate arrays encounter explicit mutations.
    - **Dynamic DOM Isolations (html-to-image):** Chart exports don't rely on unoptimized server-side rendering. We use advanced client-side DOM capturing that actively checks your current user interface theme (Light/Dark). The script isolates the target chart, temporarily strips out the UI interactive buttons, and renders a pristine, transparent-background PNG directly to your local device.
    - **Virtual Threads Ready:** All analytics read operations are non‑blocking and leverage virtual threads for optimal concurrency.
    `,
    },
    {
      id: 'early-burnout-anomaly-alerts',
      title: 'Early Burnout Alerts & EWMA Anomaly Detection',
      icon: <HeartPulse />,
      color: 'text-rose-600 dark:text-rose-400',
      content: `
**How to use:**

- **Ambient Dashboard Banner:** MyMindMirror operates proactively rather than reactively. The \`<AnomalyAlerts/>\` widget sits above the dynamic center dock on your Journal dashboard. If your emotional or writing patterns shift unexpectedly, an alert card surfaces before you open your analytics charts.
- **Three-Tier Severity Classification:** The UI parses incoming detection results to assign visual severity:
  - *High Impact (Rose):* Flags extreme statistical drops or sharp negative mood plunges.
  - *Moderate (Amber):* Flags noticeable shifts in baseline emotional momentum.
  - *Noticeable (Blue):* Flags minor behavioral drifts or word-count anomalies.
- **Stateful Client-Side Dismissals:** Acknowledging an alert via the "Dismiss" (X) button caches its composite signature (\`date-type\`) directly in \`localStorage\` under \`dismissedAnomalies\`. The alert remains suppressed across sessions without requiring database writes.
- **Smart Accordion Limit:** To prevent layout clutter during high-variance periods, the banner renders the 3 most recent anomalies (\`MAX_INITIAL_ANOMALIES = 3\`), grouping remaining items behind an expandable "Show All" toggle.

**Enterprise Architecture (How it works):**
- **Native JVM Migration (Deprecating Python/Pandas):** Anomaly detection originally required transmitting historical data over HTTP to an external service. This bottleneck was eliminated – the anomaly detection engine now runs **100% natively** inside the core Spring Boot JVM, eliminating network hops and serialization overhead.
- **Dual-Metric EWMA Algorithm:** The \`AnomalyDetectionService\` evaluates data using an Exponentially Weighted Moving Average across both daily mood scores and total word counts. Unlike a Simple Moving Average (SMA), EWMA applies exponentially decreasing weights to older entries:
  \`alpha = 2.0 / (ewmaSpan + 1.0)\`
  Yesterday's entries carry significantly higher weight than entries from weeks ago, establishing a sensitive, dynamically updating psychological baseline.
- **Configurable Thresholds:** All detection parameters are externalized to \`application.properties\` for easy tuning without recompilation:
  - \`app.anomaly.ewma-span=7\` – number of days used for the baseline window.
  - \`app.anomaly.mood-threshold-std=0.6\` – Z‑score threshold for mood anomalies.
  - \`app.anomaly.words-threshold-std=0.8\` – Z‑score threshold for word‑count anomalies.
  - \`app.anomaly.min-days=7\` – minimum data points required before detection begins.
- **Strict O(1) Memory Complexity:** Calculating moving variance across historical datasets can produce memory spikes. Because the EWMA algorithm only requires the previous day's moving average to compute the current day's average, the Java loop executes in strict O(1) auxiliary memory complexity without loading full multi-year history arrays into JVM heap space.
- **Z-Score Statistical Triggers:** The service computes running variance (\`moodVar\`, \`wordsVar\`) and derives standard deviations (\`moodStd\`, \`wordsStd\`). It calculates exact Z‑scores:
  \`zScore = (currentValue - currentEma) / currentStd\`
  When a Z‑score breaches configured thresholds, the entry is flagged as a statistical anomaly with targeted messaging.
- **Pre-Aggregated Database Acceleration:** Rather than running aggregate functions (\`AVG\`, \`SUM\`) across raw journal entries at runtime, the service queries pre‑aggregated rows from the \`daily_journal_summary\` table populated via database triggers and stored procedures. Burnout evaluations complete in under 5 milliseconds, avoiding blocking on Tomcat worker threads.
- **Virtual Threads Enabled:** All anomaly detection computations run on virtual threads, ensuring that even large historical recalculations do not block the Tomcat worker pool.
`,
    },
    {
      id: 'search-and-filtering',
      title: 'Advanced Search & Semantic Filtering (3072-Dim pgvector)',
      icon: <Search />,
      color: 'text-cyan-500 dark:text-cyan-400',
      content: `
    **How to use:**

    - Navigate to the **Search** tab within your Journal history. The UI presents four distinct, interactive query modes:
      - **AI Concept (Semantic Search):** Type an abstract memory, feeling, or concept (e.g., *"Times I felt overwhelmed"* or *"Peaceful moments in nature"*). The AI finds matches based on emotional intent, even if the exact words don't match.
      - **Exact Keyword:** Standard plaintext matching for specific words or phrases.
      - **Mood Score:** Leverage Min/Max inputs to isolate entries where your mood fell within specific mathematical ranges (between -1.0 and 1.0).
      - **Date Range:** Filter your history between exact chronological brackets.
    - **AI Relevance Grouping:** When using the AI Concept search, the frontend dynamically disables standard chronological grouping. Your results bypass the calendar layout and are grouped strictly under a **"Matches Ranked by AI Relevance"** header, proving the engine is sorting by vector proximity, not time.
    - **Input Guardrails:** The UI actively validates your inputs before hitting the server, flagging errors (like setting a minimum mood higher than a maximum mood) and turning the inputs red to prevent wasted API calls.

    **Enterprise Architecture (How it works):**

    - **High-Fidelity 3072-Dimensional Embeddings:** Your journal entries are processed through the official Spring AI implementation of the Google Vertex AI platform, configuring a massive **3072-dimensional vector space**. This represents an enormous leap in semantic resolution compared to standard 384-dimensional layers, capturing micro-nuances in complex expressions.
    - **Configurable Similarity Thresholds:** Semantic search sensitivity is externally configurable via \`application.properties\`:
      - \`app.rag.similarity-threshold=0.55\` – used for standard semantic search.
      - \`app.rag.similarity-threshold-fallback=0.50\` – used for RAG retrieval in the AI Coach.
      This allows fine‑tuning of recall vs. precision without recompilation.
    - **Vector Similarity Matching:** The Semantic Search string is converted into a 3072-element float array on the fly. We then query the secondary PostgreSQL \`pgvector\` database utilizing an optimized Cosine Similarity algorithm. The vector store leverages B-tree and GIN indexes to execute high-speed similarity matches, returning matching UUID arrays in under 50ms.
    - **Eager Loading with \`@EntityGraph\`:** After the vector search returns a list of matching entry IDs, the backend fetches the full entities using \`findByIdIn\` with a dedicated \`@EntityGraph(attributePaths = {"keyPhrases", "user"})\`. This loads the \`keyPhrases\` and \`user\` associations eagerly in a single optimized query, completely eliminating N+1 query problems.
    - **\`@BatchSize\` for Collection Performance:** The \`keyPhrases\` collection is annotated with \`@BatchSize(size = 20)\` in the \`JournalEntry\` entity. When multiple entries are loaded, Hibernate fetches their key phrases in batch using a single \`IN\` clause per batch, preventing the N+1 problem even for large result sets.
    - **In-Memory Decryption Filtering:** Because your journal content is stored encrypted via AES-CBC at rest to protect user privacy, standard SQL \`LIKE\` queries cannot read the data. The backend \`JournalService\` queries the targeted primary rows, rapidly decrypts the text streams in active memory using your active session key, and applies keyword matching filters entirely on the fly.
    - **Virtual Threads Ready:** All search operations are non‑blocking and leverage virtual threads for optimal concurrency, ensuring responsiveness even during heavy simultaneous search loads.
    `,
    },

    {
      id: 'universal-data-export',
      title: 'Universal Data Export & Client-Side Document Compilation',
      icon: <Download />,
      color: 'text-emerald-500 dark:text-emerald-400',
      content: `
          **How to use:**
          - Navigate to the **Journal History** view. At the top right, you will find the dedicated \`<ExportButtons/>\` module featuring theme-aware "Jewel" buttons.
          - **Retina-Quality Document Generation (PDF):** Click the Rose/Purple PDF button to instantly generate a highly professional, paginated A4 document containing your complete journaling history. Every entry maps out its exact timestamp, mood score, AI summary, core concerns, and complex multi-line growth tips.
          - **Raw Tabular Data (CSV):** Click the Emerald/Teal CSV button to download a cleanly structured spreadsheet. The system automatically scrubs UI-specific markdown from your text, making it perfect for users who want to run localized data analytics using Excel or Python/Pandas.
          - **Retina-Quality Canvas Capturing:** Every visual analytics chart (Spider Radar, Scatter Plot, Heatmap) features a download icon in its header card. Clicking this button captures the exact state of that specific DOM element and saves a crystal-clear PNG image straight to your local device.
          - **Absolute Data Ownership:** We believe your psychological tracking data belongs strictly to you. There are no paywalls, hidden gates, or rate-limits. You can mass-export 100% of your records at any time.

          **Enterprise Architecture (How it works):**
          - **Client-Side CPU Offloading & Manual Pagination:** To maximize server efficiency and prevent heavy multi-threaded memory spikes, all document compilation is offloaded to the client browser using \`jsPDF\`. Instead of relying on basic table plugins, the engine manually calculates string boundaries (\`doc.getTextDimensions\`) and dynamic \`yOffset\` coordinates to perfectly wrap text and trigger \`addNewPage()\` functions without breaking the A4 margins.
          - **Regex Markdown Sanitization & BOM Encoding:** The CSV exporter uses custom Regex parsers (\`cleanMarkdown\`) to strip UI-specific markup (\`##\`, \`**\`, \`[links]\`) before serialization. The string payload is prepended with a UTF-8 Byte Order Mark (\`\\uFEFF\`) and compiled into a JavaScript \`Blob\`, mathematically guaranteeing that emojis and special characters render flawlessly in Excel.
          - **Asynchronous UI Event Loop Unblocking:** Generating massive PDFs locks up the single-threaded JavaScript engine. The export logic is intentionally wrapped in a \`setTimeout\` block. This forces the browser to prioritize painting the React \`Loader2\` spinners to the screen *before* executing the heavy compilation loop, preventing the UI from feeling frozen.
          - **Dynamic DOM Isolations (html-to-image):** Chart exports don't rely on static server-side rendering. We use advanced client-side DOM capturing that actively checks your current user interface theme (Light or Dark mode). The script isolates the target chart container, rendering a pristine, transparent-background PNG directly from the active Canvas elements.
        `,
    },

    {
      id: 'enterprise-security-byok',
      title: 'Enterprise-Grade Security & BYOK Vault',
      icon: <LockKeyhole />,
      color: 'text-indigo-500 dark:text-indigo-400',
      content: `
      **How to use:**

      - Navigate to your Account Settings and locate the **Gemini API Key** module.
      -       - **Bring Your Own Key (BYOK):** Paste your personal token generated via Google AI Studio.
              - **Pre-Save Validation Matrix:** The backend tests the key against Google's API *before* saving it, returning one of four distinct states:
                - **Rapid Validation Cascade:** The validation call uses a strict 7-second combined connect/read timeout and tries up to 2 fallback models. It also bypasses the SDK's \`temperature(0.0)\` bug (which breaks Gemini 3.5+ Lite models) to guarantee a clean validation response.
                - \`VERIFIED\`: The key works and is saved.
                - \`REJECTED\`: Google explicitly rejected the key. The save is blocked so a typo never overwrites a working key.
                - \`UNVERIFIED\`: Google was unreachable (timeout). The key is saved anyway to survive third-party outages.
                - \`CLEARED\`: The input was blank, so the stored key was deleted.
              - **Quick-Access Navbar Popover:** You don't need to open the Profile page to manage your key. Click the \`Own key\` indicator in the global navbar to view your masked key, jump directly to settings, or clear the key with one click.
              - **Reactive Status Badge:** The UI utilizes the \`useApiKeyStatus\` hook to constantly monitor your active key. If your custom key is active, the navbar and profile panel shift to an Emerald Green status, securely masking your key (e.g., \`(••••12ab)\`).
      - **Bypass Global Shared Quotas:** By using your own personal API key, you unlock your own dedicated infrastructure limits directly from Google. This ensures your heavy roadmap generations and continuous AI chats are never throttled during high global application traffic.
      - **The Danger Zone:** If you ever choose to leave the platform, the "Delete Account Forever" option executes a true, destructive cascading hard‑delete. Your records are not soft‑deleted or hidden—they are completely wiped from existence.
       - **The Absolute Right to be Forgotten:** When you delete your account, we don't just delete your login. A single click executes a 6-layer hard-delete across the entire stack. Your journals, roadmaps, and encrypted keys are wiped from MySQL; your AI memory vectors are purged from PostgreSQL; and your active chat sessions and caches are instantly unlinked from Redis. We hold zero soft-deletes, zero hidden backups, and zero retained metadata. Once you click delete, it's cryptographically gone.

      **Zero-Compromise Privacy Isolation:**
      - Journaling text data represents an extraordinary degree of personal vulnerability. MyMindMirror is engineered with a strict "Zero-Trust" data architecture. Even if the underlying primary database servers were fully compromised, your personal thoughts remain unreadable, highly encrypted cryptographic gibberish.

      **Enterprise Architecture (How it works):**
      - **AES‑CBC Journal Encryption:** When you click save on a journal entry, your plaintext thoughts are never written directly to cold disk storage. The backend immediately encrypts the text block using the Advanced Encryption Standard (AES) operating in Cipher Block Chaining (CBC) mode. The 256‑bit key is derived using PBKDF2 from your unique BCrypt password hash. Without your active plaintext login password, your text records are mathematically impossible to crack.
      - **AES‑GCM Key Vault Storage (FieldEncryptionUtil):** Your personal Gemini API Key is stored using an isolated AES‑GCM (Galois/Counter Mode) encryption algorithm driven by an independent, server‑side system master secret key variable (\`app.encryption.master-key\`). This prevents side‑channel leakage and ensures that even if the database is compromised, the API keys remain unreadable.
      - **Centralised Cache Keys:** Cache names (e.g., \`API_KEY_STATUS\`, \`USER_FULL_PROFILE\`) are defined once in \`CacheConstants\` and used everywhere. This ensures consistent cache eviction when the API key is updated, preventing stale statuses in the UI.
      - **Clean Controllers with \`@CurrentUser\`:** All security‑related endpoints use the \`@CurrentUser\` annotation resolver – controllers no longer manually fetch users, making the web layer thin and eliminating duplicate user‑lookup code.
      - **Cached API Key Status:** The \`ApiKeyService\` caches the key status response using \`@Cacheable(value = CacheConstants.API_KEY_STATUS, key = "#user.id")\`. When a key is updated, the cache is automatically evicted via \`@CacheEvict\`, ensuring the UI always shows the current status.
      - **Stateless JWT Interceptors:** Session security is enforced across every single API route using strict JSON Web Tokens (JWT). The backend \`JwtRequestFilter\` intercepts incoming requests, verifying cryptographic signatures before routing to the business layer.
      - **Hard Cascading Account Teardown:** Triggering an account deletion fires an atomic, transactional method. The system purges your Redis chat history, wipes out your PostgreSQL vectors, executes cross-table cascade deletes across your MySQL tasks and roadmaps, and finally obliterates your primary user record.
      - **Virtual Threads Ready:** All security operations are non‑blocking and leverage virtual threads for optimal concurrency.
      `,
    },
    {
      id: 'ai-engine-orchestration-routing',
      title: 'AI Engine Ensemble & Strategic Quota Orchestration',
      icon: <Cpu />,
      color: 'text-amber-500 dark:text-amber-400',
      content: `
              **The User & System Experience:**
              - **Zero‑Throttle Reliability:** By implementing a dynamic model cascade, you never experience total system blackout if a single AI model hits its Google AI Studio rate limit. The system automatically rotates through a pool of available models.
                     - **Intelligent Traffic Routing:** Each task is assigned a prioritized list of models. The system tries the primary model first; if it fails (due to rate limits, 404 errors, or JSON parsing issues), it instantly cascades to the next model in the list without any user-visible interruption.
                     - **Startup Observability:** A \`@PostConstruct\` method logs every task's cascade to the console on boot, ensuring the active routing matrix is always visible in production logs.
                     - **All-Models-On-Cooldown Fast-Path:** If every model in a cascade is on cooldown, the client skips the network entirely, immediately returning a graceful fallback (or 503 with a key-aware message).
                     - **Resilient Fallback Protection:** When a model returns a \`429 Rate Limit\` or \`404 Not Found\`, the backend applies a **smart cooldown**:
                - \`429\` / parsing errors → locked for **60 seconds**.
                - \`404\` (deprecated model) → locked for **24 hours**.
                This prevents retrying dead endpoints and preserves quota.

              **The AITask Cascade & Cooldown Architecture Matrix:**

              - **1. Plain Text Generation (High-Volume Prose Pools):**
                - *TODAY_REFLECTION:* Cascade: \`gemma-4-31b-it\` → \`gemma-4-26b-a4b-it\` → \`gemini-3.6-flash\` → \`gemini-3.5-flash\`
                - *REFLECTIVE_QUESTION:* Cascade: \`gemini-3.1-flash-lite\` → \`gemma-4-26b-a4b-it\` → \`gemini-3.6-flash\` → \`gemini-3.5-flash\`
                - *REFLECTION_CHAT:* Cascade: \`gemini-3.5-flash\` → \`gemini-3.6-flash\` → \`gemini-3.1-pro\` → \`gemini-2.5-pro\`
              - **2. Structured JSON Generation (Strict Schema Enforcement):**
                - All structured operations are routed through the Google Gemini family to guarantee perfect JSON compliance with Spring AI's \`BeanOutputConverter\`.
                - *JOURNAL_ANALYSIS:* Cascade: \`gemini-3.5-flash-lite\` → \`gemini-3.1-flash-lite\` → \`gemini-3.6-flash\` → \`gemini-3.5-flash\` → \`gemini-2.5-flash\` → \`gemini-2.5-flash-lite\`
                - *ROADMAP_INITIAL / EXTENSION / NEXT_STEPS / ELABORATION / RESCHEDULE:* Cascade: \`gemini-3.5-flash-lite\` → \`gemini-3.1-flash-lite\` → \`gemini-3.6-flash\` → \`gemini-3.5-flash\` → \`gemini-2.5-flash\`
                - *MILESTONE_INSIGHTS / PARSE_GROWTH_TIP:* Same 5-model cascade as ROADMAP_*
                - *SCHEDULE_GENERATION / REOPTIMIZATION:* Same 5-model cascade as ROADMAP_*

              **Enterprise Architecture & Quota Guardrails (How it works):**
              - **Model Cascade Loop:** Instead of using external retry frameworks (like Resilience4j or Spring Retry), we implemented a **lightweight, internal Java loop** that iterates through the \`modelCascade\` list. The loop checks a thread-safe \`ConcurrentHashMap\` cooldown map before attempting each model – if the model is on cooldown, it is skipped instantly.
              - **Intelligent Cooldown Mapping:**
                - *Rate Limit / Parsing Error:* Cooldown = **60 seconds**.
                - *404 Not Found (Deprecated):* Cooldown = **24 hours**.
                - *Success:* Cooldown is cleared.
              - **Thinking Model Safety:** For models that return internal reasoning tokens (e.g., \`gemini-3.6-flash\` with thinking mode), the client selects the **last result index**, ensuring the final synthesized answer is surfaced rather than intermediate thought blocks.
              - **JSON Cleanup Pipeline:** The \`extractJson()\` method aggressively strips markdown fences (\`\`\`json\`\`\`), removes invalid escape sequences, and extracts the outermost JSON structure, ensuring that malformed LLM responses never crash downstream converters.
              - **Zero External Retry Frameworks:** Circuit breaker and retry dependencies for AI calls were replaced with a self‑contained, deterministic cascade loop, eliminating proxy type‑erasure and reflection overhead.
              - **Virtual Threads Enabled:** All AI calls execute on **Java Virtual Threads** (Spring Boot 4 / Java 25), enabling thousands of concurrent long‑running AI operations without consuming underlying OS platform threads.
              `,
    },

    {
      id: 'ai-technology-stack',
      title: 'AI Technology Stack & Polyglot Data Architecture',
      icon: <Layers />,
      color: 'text-fuchsia-500 dark:text-fuchsia-400',
      content: `
                     **The Ecosystem (What powers your experience):**

                     - **The Generative AI Ensemble (Model Cascade):** The application moves past brittle single-model paradigms, implementing a dynamic routing network across Google's absolute latest LLM architectures:
                       - *Gemma 4 Family (\`gemma-4-31b-it\`, \`gemma-4-26b-a4b-it\`):* Cost-efficient prose generation for reflections and reflective questions.
                       - *Gemini 3.5 Flash Lite:* High-quota structured generation (journal analysis, schedule generation, roadmap tasks).
                       - *Gemini 3.1 Flash Lite:* Alternate high-quota structured fallback.
                       - *Gemini 3.6 Flash:* Mid-cascade fallback for structured tasks; primary for reflection chat.
                       - *Gemini 3.5 Flash:* Reasoning fallback; primary for reflection chat's first attempt.
                       - *Gemini 3.1 Pro / 2.5 Pro:* High-reasoning last resorts for chat.
                     - **Intelligent Fallback & Cooldown:** The system maintains a dynamic cooldown map – if a model hits a rate limit, it is locked for 60 seconds. If a model returns a 404 (deprecated), it is locked for 24 hours. This ensures zero downtime and optimal quota utilization.
                     - **High-Fidelity Embeddings:** Your journal summaries are mathematically translated into 3072-dimensional vector embeddings using Spring AI's native Google embedding model.

                     **Enterprise Architecture (How it works):**

                     - **The Native Spring AI Migration:** We completely deprecated our legacy, intermediate Python/Flask middleware layer for core AI routines. All generative communications are now handled directly inside the core Spring Boot application using the native **Spring AI** framework. This completely eliminated an unoptimized secondary network hop, cutting overall AI processing and serialization latency by approximately ~40%.
                     - **AI Call Outside Transaction (\`TransactionTemplate\`):** All AI calls run entirely outside any database transaction. The backend uses \`TransactionTemplate\` to wrap only the final DB writes (e.g., saving a roadmap, updating a task) in a short, atomic transaction. This prevents database connection pool starvation during the 5‑10 second AI wait.
                     - **Mathematical JSON Schema Guarantees:** Instead of relying on unreliable, fragile Regex expressions to parse stringified JSON out of raw model responses, MyMindMirror leverages Spring AI's native \`BeanOutputConverter<T>\`. This dynamically injects explicit JSON schema boundaries straight into the system-level prompts, mathematically forcing the LLM engine to structure its outputs to bind with our target Java Records and DTOs.
                     - **Clean Controller Layer with \`@CurrentUser\`:** All endpoints use the \`@CurrentUser\` annotation resolver – controllers no longer manually fetch users, making the web layer thin and eliminating duplicate user‑lookup code.
                     - **Efficient Data Fetching (\`@EntityGraph\` + \`@BatchSize\`):** Complex object graphs are loaded using \`@EntityGraph\` to fetch the primary collection (e.g., \`tasks\`) in a single optimized SQL query. Secondary collections (e.g., \`resources\`, \`milestones\`) are fetched lazily with \`@BatchSize\` to prevent N+1 query problems. This completely eliminates the Cartesian product (duplicate task rows) that previously caused massive data duplication in the UI.
                     - **True Polyglot Persistence Layer:** We separate data structures across three uniquely distinct database engines, matching data types to their optimal transactional engines:
                       - *MySQL (Primary Relational Engine):* Manages atomic transactional integrity, user profiles, relational milestone tables, and AES-encrypted text.
                       - *PostgreSQL + pgvector (Semantic Store):* A secondary datasource dedicated entirely to processing 3072-dimensional vector embedding queries with deep GIN and B-tree vector indices.
                       - *Redis (Distributed Memory):* An ultra-fast, in-memory cache layer managing ephemeral state, active chat history sessions, and gamification metrics.
                     - **Virtual Threads:** All AI calls and database operations are executed on virtual threads (Spring Boot 4 / Java 25), enabling thousands of concurrent requests without exhausting OS threads.
                     `,
    },
    {
      id: 'complete-polyglot-dependency-manifest',
      title: 'Enterprise Infrastructure & Polyglot Dependency Manifest',
      icon: <Server />,
      color: 'text-sky-500 dark:text-sky-400',
      content: `
                                 **The Architecture Spectrum (Full-Stack Alignment):**

                                 - **Modern Runtime Ecosystem:** Driven entirely by a bleeding-edge infrastructure layer utilizing **Java 25 (LTS)** on the backend and **React 19.2** paired with **Vite 7** on the frontend, establishing compile-time security, modern garbage collection optimization, and high-speed Hot Module Replacement (HMR).
                                 - **Virtual Threads Enabled:** Spring Boot 4 virtual threads are activated via \`spring.threads.virtual.enabled=true\`. Tomcat now assigns a lightweight virtual thread per request instead of a heavy OS thread. When the code hits a blocking AI call, the virtual thread "unmounts" from the CPU, allowing the server to handle thousands of concurrent users without exhausting the OS thread pool.

                                 **Production Backend Technical Stack Frameworks (pom.xml Analysis):**
                                 - **1. Native Spring AI Foundation (v2.0.0-M8 Stable BOM):**
                                   - \`spring-ai-starter-model-google-genai\`: Direct interface integration managing native API transactions with Google GenAI infrastructure.
                                   - \`spring-ai-google-genai-embedding\`: Orchestrates vectorized formatting for raw data processing.
                                   - \`spring-ai-starter-vector-store-pgvector\`: High-performance relational vector layer mapping embedding indices securely.

                                 - **2. Core Multi-Tenant Storage Layer:**
                                   - Driven by \`spring-boot-starter-data-jpa\` handling strict transactional operations over **MySQL** (via \`mysql-connector-j\`) for relational user records, combined with a secondary **PostgreSQL** runtime engine to process 3072-dimensional vector datasets.

                                 - **3. Stateful Security, Session Memory & Cache Core:**
                                   - Encrypted token boundaries managed using JSON Web Tokens (\`jjwt-api\` 0.11.5) integrated with \`spring-boot-starter-security\`.
                                   - Multi-tier performance acceleration driven by \`spring-boot-starter-data-redis\` for active session caching and ephemeral state management.
                                   - **\`@CurrentUser\` Argument Resolver:** A custom \`HandlerMethodArgumentResolver\` injects the authenticated \`User\` entity directly into controller methods. This eliminates manual user lookup in every controller, making the web layer thin and DRY.
                                   - **Centralised Cache Keys (\`CacheConstants\`):** All cache names are defined once in \`CacheConstants\` and used throughout the codebase. This eliminates typos and ensures consistent cache eviction strategies.

                                 - **4. Enterprise Quality Assurance:**
                                   - Full structural verification executed via automated \`jacoco-maven-plugin\` coverage boundaries, bypassing non-functional tool packages.
                                   - **No external retry frameworks:** We replaced Resilience4j retry and circuit breaker dependencies with an internal model cascade loop. This eliminates proxy type‑erasure issues and keeps AI resilience self‑contained.

                                 - **5. Utility & Development Enhancements:**
                                   - **\`DateUtil\`:** A centralised static utility for parsing date strings, handling \`DateTimeParseException\` consistently, and validating date ranges across all controllers and services.
                                   - **Lombok & MapStruct:** Reduces boilerplate and ensures type‑safe DTO mapping.
                                   - **Spring Boot Actuator:** Exposes \`/actuator/health\` (public) and \`/actuator/info\` (public) endpoints for production monitoring. Detailed health is disabled in production.

                                 **Production Frontend Interface Stack (package.json Analysis):**
                                 - **1. Rendering, State, and Style Core:** Built on top of **React 19**. Global look-and-feel variables are compiled directly through **Tailwind CSS v4** utilizing the performant \`@tailwindcss/vite\` bundling engine. Asynchronous server synchronization, structural retries, and browser cache queries are automated via **TanStack React Query v5**.

                                 - **2. High-Performance Canvas Analytics:** Data tracking is rendered exclusively via **Chart.js v4** and \`react-chartjs-2\`, backed by \`chartjs-plugin-zoom\` to natively capture rich horizontal user scrolling and pinch-to-zoom matrix adjustments.

                                 - **3. Smart DOM Virtualization:** Massive journal histories bypass standard React rendering and are routed through **TanStack Virtualizer**, computing absolute DOM positioning on the fly to prevent browser RAM exhaustion on mobile devices.

                                 - **4. Productivity Layout Engines:** Interactive calendars are drawn via \`react-big-calendar\`. Daily contribution patterns leverage \`react-calendar-heatmap\`. Visual phrase clustering executes via \`react-tagcloud\` powered by customized CSS keyframe animations.

                                 - **5. Secure Client-Side Data Export Pipeline:** Tabular extractions use \`xlsx\` and \`file-saver\`. Beautiful PDF reporting leverages \`jspdf\` integrated with custom table boundaries via \`jspdf-autotable\`. Crisp canvas captures bypass server load completely by utilizing \`html-to-image\` directly within the client sandbox.
                                 `,
    },
    {
      id: 'testing-quality-assurance',
      title: 'Testing, CI/CD & Quality Assurance',
      icon: <TestTube />,
      color: 'text-emerald-500 dark:text-emerald-400',
      content: `
       **The Numbers:**
       - **1,200 tests** — 1,082 unit + 118 integration
       - **99% instruction coverage**
  - **99% branch coverage**
  - **100% method coverage**
  - **100% class coverage**
  - **Zero skipped tests** in CI
  - **SDK Boundary Testing:** \`ApiKeyValidator\` achieves 100% coverage by using Mockito's \`mockConstruction\` to simulate the Google GenAI SDK (testing empty payloads, 401 auth rejections, and network timeouts) without making real network calls.

  **How to run them:**
  - \`./mvnw verify\` runs unit (Surefire) and integration (Failsafe) tests together.
  - \`./mvnw test\` runs unit only — fast, no Docker needed.
  - Coverage report at \`target/site/jacoco/index.html\`.

  **Test tier breakdown:**
  - **Unit tests** (\`*Test.java\`): isolated services, mappers, utilities, controllers. Mocked dependencies via Mockito.
  - **Slice tests** (\`@WebMvcTest\`, \`@DataJpaTest\`): single-layer integration with H2.
  - **Integration tests** (\`*IT.java\`): full Spring context + real MySQL 8 + real Redis 7 via Testcontainers.
- **6-Layer Deletion Verification:** Dedicated integration tests explicitly verify that deleting a user account successfully purges data across *all* infrastructure layers — asserting that PostgreSQL vector embeddings are deleted and Redis chat sessions are purged using non-blocking \`SCAN\` + \`UNLINK\` commands.

  **Enterprise Architecture (How it works):**
  - **Real Datastores, Not Mocks:** Integration tests spin up real MySQL and Redis containers via Testcontainers 2.0.5. No mock-DB drift, no "works in test but fails in prod."
  - **Container Lifecycle Optimization:** Containers start once per JVM via a static initializer and stop via shutdown hook. Test classes share them, so the full IT suite runs in a fraction of the time a naive per-class setup would take.
  - **Production Guardrail:** \`AbstractIntegrationTest\` asserts the datasource URL does NOT contain \`aivencloud.com\` before every test. If a misconfiguration ever points tests at production, the suite aborts before running a single query.
  - **Cache State Isolation:** \`resetCaches()\` clears every Spring cache between test methods. No stale Redis state bleeding across tests.
  - **Mocked AI Gateway:** The AI client (\`DynamicAiClientService\`) is mocked via \`@MockitoBean\`, so integration tests never hit real Gemini endpoints. Tests are deterministic and cost nothing.
  - **CI Pipeline:** GitHub Actions runs on every push and PR: JDK 25 setup, Maven cache, \`mvn -B verify\`, artifact upload for Surefire reports, Failsafe reports, and JaCoCo coverage HTML. Runs in under 5 minutes.
  - **Coverage Exclusions (with reason):** \`runner/\` (Spring Boot startup beans — exercised implicitly by every IT context boot), \`tools/\` (dev-only utilities), and \`BackendApplication\` (the main method). Everything else is measured. The only package below 99% is \`util/\`, driven by unreachable JCE catch blocks in the encryption utilities.
  `,
    },

    {
      id: 'pwa-offline-support',
      title: 'PWA, Offline Support & Native App Behavior',
      icon: <MonitorSmartphone />,
      color: 'text-sky-500 dark:text-sky-400',
      content: `
  **What you get:**
  - **Install to Home Screen:** A slide-in banner offers to install MyMindMirror as a standalone app on desktop or mobile. Once installed, it launches in its own window — no browser chrome, no address bar.
  - **Offline Resilience:** A persistent banner appears at the top of the screen when the browser detects no network. When the connection returns, an inline toast confirms the app is back online.
  - **App-Like Experience:** The installed PWA runs in a full-screen window with its own icon, theme color, and startup screen — indistinguishable from a native app on iOS and Android.
  - **Cached Assets:** Google Fonts and static assets are cached with a first-load-wins strategy, so repeat visits load instantly even on slow connections.

  **Enterprise Architecture (How it works):**
  - **vite-plugin-pwa + Workbox:** The PWA is built via \`vite-plugin-pwa\` using Workbox under the hood. Service worker registration is automatic (\`registerType: 'autoUpdate'\`), so users always get the latest build on next load without manual refresh.
  - **Explicit Runtime Caching:** Font requests to \`fonts.googleapis.com\` and \`fonts.gstatic.com\` use a \`CacheFirst\` strategy with a 1-year expiration. Static build assets (\`.js\`, \`.css\`, \`.html\`, \`.woff2\`) are precached on install.
  - **PWA Manifest:** The manifest is served at \`/manifest.json\` with proper icons (192×192 and 512×512), theme color (\`#131127\`), background color, and \`display: standalone\` — meeting the installability criteria for both Chrome and Safari.
  - **Offline Detection:** \`OfflineBanner.jsx\` listens to native \`online\` / \`offline\` events and uses \`sonner\` toasts for smooth transition messages.
  - **Install Prompt Handling:** \`InstallPrompt.jsx\` defers to the browser's native \`beforeinstallprompt\` event, only showing the install banner when the browser signals installability. No nagging.
  `,
    },
    {
      id: 'enterprise-performance',
      title: 'Enterprise Performance & System Optimizations',
      icon: <Zap />,
      color: 'text-yellow-500 dark:text-yellow-400',
      content: `
 **What you experience:**

 - **Sub-Second Dashboard Rendering:** Dashboards render instantly. There is zero lag when hopping between the calendar layout, your complete historical timeline, or heavy analytics.
 - **Zero-Blocking Architecture:** High-latency tasks like executing 12-week roadmap compilations or processing complex journal breakdowns execute quietly in the background, keeping the user interface completely reactive.

 **Enterprise Architecture (What we built to make it fast):**
 - **Virtual Threads (Spring Boot 3+):** All AI calls and database operations are executed on virtual threads (\`spring.threads.virtual.enabled=true\`). Tomcat assigns a lightweight virtual thread per request instead of a heavy OS thread. When the code hits a blocking AI call (5‑10 seconds), the virtual thread "unmounts" from the CPU, allowing the server to handle thousands of concurrent users without exhausting the OS thread pool.
 - **AI Call Outside Transaction (\`TransactionTemplate\`):** Every method that calls the AI service runs entirely outside any database transaction. The backend uses \`TransactionTemplate\` to wrap only the final DB writes (e.g., saving a roadmap, updating a task) in a short, atomic transaction. This prevents database connection pool starvation during the AI wait.
 - **Database Level Indexing:** Every relational table is fortified with explicit multi-column indices on critical foreign keys and heavy filtering columns (including \`user_id\`, \`entry_date\`, \`created_at\`, and task parent links), dropping search execution times to milliseconds.
 - **JPA N+1 Query Elimination (\`@EntityGraph\` + \`@BatchSize\`):** Complex object structures (like a Master Roadmap consisting of weeks, which contain tasks, which wrap subtask arrays) normally trigger hundreds of sequential database queries. We use:
   - \`@EntityGraph\` to fetch the primary collection (e.g., \`tasks\`) in a single optimized SQL JOIN statement.
   - \`@BatchSize\` to load secondary collections (e.g., \`resources\`, \`milestones\`) in batch using \`IN\` clauses, preventing N+1 queries.
   This completely eliminates the Cartesian product (duplicate task rows) that previously caused massive data duplication in the UI.
 - **Pre-Aggregated Cache Invalidation Tables:** The trigger-backed \`daily_journal_summary\` table tracks daily metrics on the fly. This prevents heavy aggregate math functions from running on the primary entries table during chart paints.
 - **Multi-Layer Redis Caching Engine:** Heavy read endpoints are intercepted by Redis running in RAM. Items like your Roadmap Preferences, Achievement matrices, and Token statuses are served directly from memory, dropping query times from ~120ms to under 10ms.
- **Non-Blocking Redis Deletion:** When a user deletes their account, we don't use the blocking \`KEYS\` and \`DEL\` commands, which would stall the single-threaded Redis server. Instead, we use a cursor-based \`SCAN\` to batch-find the user's chat sessions and \`UNLINK\` to delete them asynchronously in the background. This guarantees that wiping an account never degrades performance for other active users.
 - **Centralised Cache Keys (\`CacheConstants\`):** All cache names are defined once in \`CacheConstants\` and used throughout the codebase. Annotations like \`@CacheEvict(value = CacheConstants.USER_FULL_PROFILE, key = "#userId")\` are surgically attached to corresponding \`PUT/POST/PATCH\` mutations to instantly drop outdated data frames. This eliminates typos and ensures consistent eviction strategies.
 - **Centralised Date Parsing (\`DateUtil\`):** All date string parsing is routed through a static \`DateUtil\` utility. This handles \`DateTimeParseException\` consistently, validates date ranges, and returns clean 400 Bad Request responses via the global exception handler. No more manual \`try-catch\` blocks in controllers.
 - **Model Cascade Resilience:** Instead of using external retry frameworks, the AI client uses an internal model cascade loop. If a model hits a rate limit, it is locked for 60 seconds; if it returns a 404 (deprecated), it is locked for 24 hours. The system automatically falls back to the next available model without user‑visible interruption.
 - **Frontend Component Memoization & Deferred Loads:** The React application leverages \`React.memo\`, \`useMemo\`, and \`useCallback\` hooks to optimize browser performance. Massive visual layout blocks only calculate re-renders when their underlying data parameters experience an explicit structural mutation.
 - **Optimization Tactics:**
   - *Lazy-Loaded Route Splits:* Split bundles map Journal, Profiles, and Achievements into separate async chunks, loading elements only on demand.
   - *Deferred History Buffers:* Heavy paginated journal histories are completely deferred, fetching rows only when the user explicitly clicks the History tab.
   - *Network Gzip Compression:* Backend JSON response streams are compressed using Gzip, shrinking data payloads over the wire by up to 80% for rapid mobile operations.
   - *Debounced Input Streams:* Search inputs and text entry updates are heavily debounced to prevent redundant API thrashing while typing.
 `,
    },
  ], []);

  return (
    <div className={`min-h-screen w-full ${isAuthenticated ? '' : 'pt-12 lg:pt-12'} transition-colors duration-300 relative bg-transparent text-slate-900 dark:text-gray-100`}>
      {/* Hero Section */}
      <section className="relative w-full py-16 lg:py-24 px-4 sm:px-6 lg:px-8 text-center z-10">
        <FadeIn direction="down" delay={0.1}>
          <div className="max-w-4xl mx-auto">
            <div className="inline-flex items-center px-4 py-2 rounded-full bg-gradient-to-r from-purple-500/10 to-teal-500/10 border border-purple-500/20 dark:border-purple-500/30 mb-6 lg:mb-8 shadow-sm">
              <Sparkles className="w-4 h-4 lg:w-5 lg:h-5 text-purple-600 dark:text-purple-400 mr-2" />
              <span className={`text-xs lg:text-sm font-bold uppercase tracking-wider ${colors.textPrimary}`}>Enterprise Architecture & Features</span>
            </div>
            <h1 className={`text-4xl sm:text-5xl lg:text-6xl font-poppins font-extrabold mb-6 lg:mb-8 leading-tight tracking-tight ${colors.textPrimary}`}>
              Comprehensive Platform <br className="hidden sm:block" />
              <span className="bg-gradient-to-r from-purple-600 via-pink-500 to-teal-500 dark:from-purple-400 dark:via-pink-400 dark:to-teal-400 bg-clip-text text-transparent">
                Deep Dive
              </span>
            </h1>
            <p className={`text-base sm:text-lg lg:text-xl font-medium ${colors.textSecondary} max-w-3xl mx-auto leading-relaxed`}>
              Explore every operational feature in detail—from RAG AI analysis and gamification, to smart scheduling and polyglot persistence.
            </p>

            {/* Floating Background Icons */}
            <FadeIn direction="none" delay={0.6}>
              <div className="absolute top-20 left-10 opacity-20 animate-float hidden lg:block pointer-events-none">
                <Layers className="w-10 h-10 text-purple-400" />
              </div>
              <div className="absolute top-40 right-10 opacity-20 animate-float-delayed hidden lg:block pointer-events-none">
                <Cpu className="w-10 h-10 text-teal-400" />
              </div>
              <div className="absolute bottom-10 left-1/4 opacity-20 animate-float-slow hidden lg:block pointer-events-none">
                <Database className="w-10 h-10 text-amber-400" />
              </div>
            </FadeIn>
          </div>
        </FadeIn>
      </section>

      {/* Main Content with Sidebar + Accordion */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 lg:py-12 relative z-10">
        <div className="flex flex-col lg:flex-row gap-6 lg:gap-8 xl:gap-10">

          {/* Desktop Sidebar */}
          <aside className="hidden lg:block w-80 xl:w-96 shrink-0 relative">
            <div className="sticky top-32 z-20">
              <FadeIn direction="right" delay={0.2}>
                {/*  Utilizing the newly fixed sidebarBg which now perfectly acts as Layer 1 */}
                <div className={`rounded-2xl lg:rounded-3xl ${colors.sidebarBg} border ${colors.cardBorder} p-5 lg:p-6`}>
                  <h3 className={`text-lg lg:text-xl font-poppins font-bold mb-4 lg:mb-6 flex items-center gap-2 ${colors.textPrimary} tracking-tight`}>
                    <Menu className="w-5 h-5 lg:w-6 lg:h-6 text-purple-500 dark:text-teal-400" /> Architecture Index
                  </h3>
                  <nav className="space-y-1.5 lg:space-y-2 max-h-[calc(100vh-12rem)] overflow-y-auto custom-scrollbar pr-2">
                    {sections.map((section) => (
                      <button
                        key={section.id}
                        onClick={() => toggleSection(section.id)}
                        className={`w-full text-left px-4 py-3 rounded-xl lg:rounded-2xl transition-all duration-200 active:scale-95 flex items-center gap-3 lg:gap-4 text-xs lg:text-sm font-bold tracking-wide group ${openSection === section.id
                          ? `${colors.sidebarActiveText} ${colors.sidebarActiveBg}`
                          : `${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 hover:bg-slate-100 dark:hover:bg-white/5 border border-transparent`
                          }`}
                      >
                        <span className={`shrink-0 transition-transform ${openSection === section.id ? 'scale-110' : 'group-hover:scale-110'}`}>
                          {React.cloneElement(section.icon, { className: "w-4 h-4 lg:w-5 lg:h-5" })}
                        </span>
                        <span className="truncate">{section.title}</span>
                      </button>
                    ))}
                  </nav>
                </div>
              </FadeIn>
            </div>
          </aside>

          {ReactDOM.createPortal(
            <>
              {/* Floating Toggle Button */}
              <div className="lg:hidden fixed bottom-6 right-6 z-[9999]">
                <button
                  onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                  className="p-4 rounded-full bg-gradient-to-r from-purple-600 to-teal-600 text-white shadow-xl hover:shadow-2xl hover:scale-105 active:scale-95 transition-all"
                  aria-label="Toggle Table of Contents"
                >
                  {mobileMenuOpen ? <X size={24} /> : <ListTree size={24} />}
                </button>
              </div>

              {/* Mobile Drawer (overlay + content) */}
              {mobileMenuOpen && (
                <div
                  //  Stripped backdrop-blur-sm to match our global mobile performance rule
                  className="fixed inset-0 z-[9998] bg-black/60 lg:hidden"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <div
                    className={`absolute right-0 top-0 bottom-0 w-80 sm:w-96 ${colors.drawerBg} border-l ${colors.sectionBorder} shadow-2xl p-6 overflow-y-auto animate-in slide-in-from-right duration-300`}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <div className={`flex justify-between items-center mb-8 border-b ${colors.sectionBorder} pb-4`}>
                      <h3 className={`text-xl font-poppins font-bold tracking-tight ${colors.textPrimary}`}>Index</h3>
                      <button onClick={() => setMobileMenuOpen(false)} className={`p-2 rounded-full bg-slate-100 dark:bg-white/10 hover:bg-slate-200 dark:hover:bg-white/20 transition ${colors.textSecondary}`}>
                        <X size={20} />
                      </button>
                    </div>
                    <nav className="space-y-2">
                      {sections.map((section) => (
                        <button
                          key={section.id}
                          onClick={() => toggleSection(section.id)}
                          className={`w-full text-left px-4 py-3.5 rounded-xl transition-all duration-200 active:scale-95 flex items-center gap-3 text-sm font-bold ${openSection === section.id
                            ? `${colors.sidebarActiveText} ${colors.sidebarActiveBg}` // Stripped the duplicated border!
                            : `${colors.textSecondary} hover:bg-slate-100 dark:hover:bg-white/5 border border-transparent`
                            }`}
                        >
                          <span className="shrink-0">{React.cloneElement(section.icon, { className: "w-5 h-5" })}</span>
                          <span className="truncate">{section.title}</span>
                        </button>
                      ))}
                    </nav>
                  </div>
                </div>
              )}
            </>,
            document.body
          )}

          {/* Accordion Content Area */}
          <div className="flex-1 space-y-6 lg:space-y-8">
            {sections.map((section, index) => (
              <FadeIn key={section.id} delay={0.1 * (index % 3)} direction="up" fullWidth>
                <FeatureSectionCard
                  ref={(el) => (sectionRefs.current[section.id] = el)}
                  section={section}
                  isOpen={openSection === section.id}
                  onToggle={toggleSection}
                  colors={colors}
                />
              </FadeIn>
            ))}
          </div>
        </div>
      </div>

      {/* Call to Action */}
      <section className="relative w-full py-16 lg:py-24 px-4 sm:px-6 lg:px-8 z-10">
        <FadeIn direction="up" delay={0.2} fullWidth>
          <div className="max-w-5xl mx-auto text-center">
            <div className={`rounded-[2rem] lg:rounded-[3rem] ${colors.cardBg} border ${colors.cardBorder} p-10 lg:p-16 relative overflow-hidden group shadow-2xl`}>
              <div className="absolute inset-0 bg-gradient-to-r from-purple-500/10 via-transparent to-teal-500/10 opacity-0 group-hover:opacity-100 transition duration-700 ease-in-out pointer-events-none" />

              <div className="relative z-10">
                <div className={`inline-flex p-4 lg:p-5 rounded-2xl lg:rounded-3xl bg-gradient-to-br from-purple-500/20 to-teal-500/20 dark:from-purple-900/30 dark:to-teal-900/30 mb-6 lg:mb-8 shadow-inner border border-purple-200/50 dark:border-teal-700/30`}>
                  <Shield className="w-8 h-8 lg:w-10 lg:h-10 text-purple-600 dark:text-teal-400" />
                </div>
                <h2 className={`text-3xl lg:text-5xl font-poppins font-extrabold mb-4 lg:mb-6 tracking-tight ${colors.textPrimary}`}>Ready to experience it yourself?</h2>
                <p className={`text-base lg:text-lg mb-8 lg:mb-10 ${colors.textSecondary} max-w-3xl mx-auto font-medium leading-relaxed`}>
                  Start tracking, learning, and optimizing your personal and professional growth with our private, AI-driven architecture.
                </p>
                <Link
                  to={localStorage.getItem('jwtToken') ? "/journal" : "/register"}
                  className="inline-flex items-center gap-3 bg-gradient-to-r from-purple-500 to-teal-500 hover:from-purple-600 hover:to-teal-600 text-white px-8 py-3.5 lg:px-10 lg:py-4 rounded-full font-bold text-sm lg:text-base shadow-lg hover:shadow-xl hover:-translate-y-0.5 active:scale-95 transition-all duration-300"
                >
                  {localStorage.getItem('jwtToken') ? "Enter Application" : "Create Free Account"}
                  <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </Link>
              </div>
            </div>
          </div>
        </FadeIn>
      </section>
    </div>
  );
}

export default FeaturesGuide;