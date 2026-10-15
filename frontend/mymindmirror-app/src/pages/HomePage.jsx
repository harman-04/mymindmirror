import React, { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import FadeIn from '../components/FadeIn';
import {
  Sparkles, Brain, Target, Shield, ArrowRight, ChevronRight,
  CalendarDays, MessageCircle, Search, Download, FileText, LockKeyhole, CheckCircle, Trophy,
  Award, Activity, Database, Coffee, Lightbulb, Flame, HeartPulse, Image as ImageIcon, LineChart, MapPin, Zap
} from 'lucide-react';

function HomePage() {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';
  const [animatedStats, setAnimatedStats] = useState(false);
  const statsRef = useRef(null);

  const isAuthenticated = !!localStorage.getItem('jwtToken');

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setAnimatedStats(true);
          observer.disconnect();
        }
      },
      { threshold: 0.3 }
    );
    if (statsRef.current) observer.observe(statsRef.current);
    return () => observer.disconnect();
  }, []);

  // ==========================================================================
    // 🌟 MASTER ELEVATION PALETTE (3-Layer Architecture)
    // ==========================================================================
    const colors = {
      background: 'bg-transparent', // Lets the App.jsx premium gradient shine through!

      // Layer 1: Main Cards
      cardBg: isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm',
      cardBorder: isDarkMode ? 'border-white/10' : 'border-slate-200/80',

      // Layer 2: Inner Sections (Showcase Graphics, Panels)
      sectionBg: isDarkMode ? 'bg-[#131127]/80' : 'bg-slate-50/80',
      sectionBorder: isDarkMode ? 'border-white/5' : 'border-slate-200/60',

      // Layer 3: Deep Elements (Inner boxes inside showcases)
      innerContentBg: isDarkMode ? 'bg-black/20' : 'bg-white',

// 🌟 FIX: Codified the full-width alternating bands into the Master Palette!
    bandBg: isDarkMode ? 'bg-black/20' : 'bg-slate-50/50',
      // Typography
      textPrimary: isDarkMode ? 'text-gray-100' : 'text-slate-900',
      textSecondary: isDarkMode ? 'text-gray-400' : 'text-slate-500',

      // Buttons
      buttonPrimary: 'bg-gradient-to-r from-purple-500 to-teal-500 hover:from-purple-600 hover:to-teal-600 shadow-md hover:shadow-lg hover:-translate-y-0.5 active:scale-95 text-white transition-all duration-300',
    };
const features = [
    {
      title: "RAG-Powered AI Coach",
      description: "Chat with an AI that actively remembers your journal entries using pgvector semantic memory. Toggle between 'Replace' or 'Append' modes for flowing conversations.",
      icon: <MessageCircle className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-purple-500/20 to-pink-500/20",
      iconBg: "bg-purple-50 dark:bg-purple-500/20",
      iconColor: "text-purple-600 dark:text-purple-400",
    },
    {
      title: "4-Mode Semantic Search",
      description: "A comprehensive 4-tab routing system. Filter by Date, exact Keyword, mathematical Mood Scores, or utilize our 3072-dimensional vector space to search for abstract AI Concepts.",
      icon: <Search className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-blue-500/20 to-cyan-500/20",
      iconBg: "bg-blue-50 dark:bg-blue-500/20",
      iconColor: "text-blue-600 dark:text-blue-400",
    },
    {
      title: "Smart Lifestyle Timetable",
      description: "Set your Energy Peak, Sleep Times, and Daily Habits. The AI schedules tasks perfectly around your life, with a 'Re-optimize' engine to salvage your day when life happens.",
      icon: <CalendarDays className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-teal-500/20 to-emerald-500/20",
      iconBg: "bg-teal-50 dark:bg-teal-500/20",
      iconColor: "text-teal-600 dark:text-teal-400",
    },
    {
      title: "Dynamic Gamification",
      description: "Level up your life. Watch your XP and streaks tick up via custom animations. Our 'Closest Target' engine isolates your next unlockable achievement to keep you motivated.",
      icon: <Award className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-amber-500/20 to-orange-500/20",
      iconBg: "bg-amber-50 dark:bg-amber-500/20",
      iconColor: "text-amber-600 dark:text-amber-400",
    },
    {
      title: "High-Res Visual Analytics",
      description: "Powered by 8 custom Chart.js engines. Explore dynamic-width mood timelines, interactive radar charts, linear regression scatter plots, and CSS-animated phrase clouds.",
      icon: <LineChart className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-rose-500/20 to-pink-500/20",
      iconBg: "bg-rose-50 dark:bg-rose-500/20",
      iconColor: "text-rose-600 dark:text-rose-400",
    },
    {
      title: "Universal Data Export",
      description: "Your data is yours. Export beautifully paginated PDF reports of your entire journal, structured CSVs with \uFEFF BOM encoding, or panoramic PNGs of your visual timelines.",
      icon: <Download className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-indigo-500/20 to-blue-500/20",
      iconBg: "bg-indigo-50 dark:bg-indigo-500/20",
      iconColor: "text-indigo-600 dark:text-indigo-400",
    },
    {
      title: "Chunked AI Roadmaps",
      description: "Generate comprehensive learning roadmaps tailored to your difficulty, language, and pace. Seamlessly toggle between deeply nested List Views and horizontal CSS Grid Timelines.",
      icon: <MapPin className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-cyan-500/20 to-teal-500/20",
      iconBg: "bg-cyan-50 dark:bg-cyan-500/20",
      iconColor: "text-cyan-600 dark:text-cyan-400",
    },
    {
      title: "Bring Your Own Key",
      description: "Securely input your own Gemini API key into our reactive BYOK vault. Bypass global shared limits and unlock dedicated infrastructure quotas directly from Google.",
      icon: <LockKeyhole className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-fuchsia-500/20 to-purple-500/20",
      iconBg: "bg-fuchsia-50 dark:bg-fuchsia-500/20",
      iconColor: "text-fuchsia-600 dark:text-fuchsia-400",
    },
    {
      title: "Enterprise-Grade Security",
      description: "Zero compromises. Dual databases (MySQL + Postgres), Redis caching, strict stateless JWT interceptors, and AES-CBC encryption ensure your deepest thoughts remain impenetrable.",
      icon: <Shield className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-emerald-500/20 to-green-500/20",
      iconBg: "bg-emerald-50 dark:bg-emerald-500/20",
      iconColor: "text-emerald-600 dark:text-emerald-400",
    },
    {
      title: "Deep-Dive Task Elaboration",
      description: "Stuck on a roadmap step? Click 'Elaborate' and the AI dynamically expands the task into a comprehensive, step-by-step markdown guide with actionable micro-subtasks.",
      icon: <Lightbulb className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-yellow-500/20 to-amber-500/20",
      iconBg: "bg-yellow-50 dark:bg-yellow-500/20",
      iconColor: "text-yellow-600 dark:text-yellow-500",
    },
    {
          title: "Smart Insights & AI Extraction",
          description: "Get instant AI performance assessments on your milestones. Or, click '+' on any journal Growth Tip to trigger a secondary AI pipeline that parses abstract advice into highly structured, actionable subtasks.",
          icon: <Target className="w-6 h-6 lg:w-7 lg:h-7" />,
          gradient: "from-emerald-500/20 to-teal-500/20",
          iconBg: "bg-emerald-50 dark:bg-emerald-500/20",
          iconColor: "text-emerald-600 dark:text-emerald-500",
        },
    {
      title: "Early Burnout Alerts",
      description: "Our native Java EWMA algorithm operates with O(1) memory complexity to monitor your daily mood and word volume. It calculates Z-scores to detect shifts and warn you of potential burnout.",
      icon: <HeartPulse className="w-6 h-6 lg:w-7 lg:h-7" />,
      gradient: "from-rose-500/20 to-orange-500/20",
      iconBg: "bg-rose-50 dark:bg-rose-500/20",
      iconColor: "text-rose-600 dark:text-rose-500",
    },
  ];

  const stats = [
    { value: "100%", label: "Encrypted Privacy", icon: <Shield className="w-5 h-5 lg:w-6 lg:h-6" /> },
    { value: "pgvector", label: "Semantic Memory", icon: <Database className="w-5 h-5 lg:w-6 lg:h-6" /> },
    { value: "BYOK", label: "API Flexibility", icon: <LockKeyhole className="w-5 h-5 lg:w-6 lg:h-6" /> },
    { value: "<10ms", label: "Redis Caching", icon: <Zap className="w-5 h-5 lg:w-6 lg:h-6" /> },
  ];

return (
    <div className={`min-h-screen w-full ${colors.background} ${colors.textPrimary} transition-colors duration-300 relative`}>

      {/* Hero Section */}
      <section className="relative overflow-hidden pt-28 lg:pt-36 pb-20 lg:pb-32 px-4 sm:px-6 lg:px-8 z-10 flex justify-center">
        <div className="w-full max-w-7xl flex flex-col items-center text-center relative z-10">

          <FadeIn direction="down" delay={0.1}>
            <div className="inline-flex items-center px-4 py-2 rounded-full bg-gradient-to-r from-purple-500/10 to-teal-500/10 border border-purple-500/20 dark:border-white/10 mb-8 shadow-sm">
              <Sparkles className="w-4 h-4 lg:w-5 lg:h-5 text-purple-500 mr-2" />
              <span className={`text-xs lg:text-sm font-bold tracking-wider uppercase ${colors.textPrimary}`}>Powered by Spring AI & pgvector</span>
            </div>
          </FadeIn>

          <FadeIn delay={0.2}>
            <h1 className={`text-5xl sm:text-6xl lg:text-7xl font-poppins font-extrabold leading-[1.1] tracking-tight ${colors.textPrimary}`}>
              Reflect, Grow, and <br className="hidden sm:block" />
              <span className="bg-gradient-to-r from-purple-600 via-pink-500 to-teal-500 dark:from-purple-400 dark:via-fuchsia-400 dark:to-teal-400 bg-clip-text text-transparent">
                Transform.
              </span>
            </h1>
          </FadeIn>

          <FadeIn delay={0.3}>
            <p className={`text-base sm:text-lg lg:text-xl max-w-3xl mx-auto mt-6 lg:mt-8 ${colors.textSecondary} leading-relaxed font-medium`}>
              MyMindMirror is an enterprise-grade, privacy-first AI journaling ecosystem. Featuring semantic vector memory, lifestyle-aware smart scheduling, and deep visual analytics to help you understand your emotional fingerprint.
            </p>
          </FadeIn>

          <FadeIn delay={0.4}>
            <div className="flex flex-col sm:flex-row justify-center items-center gap-4 lg:gap-6 mt-10 lg:mt-12">
              <Link
                to={isAuthenticated ? "/journal" : "/register"}
                className={`${colors.buttonPrimary} w-full sm:w-auto px-8 py-3.5 lg:px-10 lg:py-4 rounded-xl lg:rounded-2xl font-bold text-sm lg:text-base flex items-center justify-center gap-2`}
              >
                {isAuthenticated ? "Enter Your Dashboard" : "Start Free Journey"}
                <ArrowRight className="w-5 h-5" />
              </Link>
              <Link
                to="/features"
                className={`w-full sm:w-auto px-8 py-3.5 lg:px-10 lg:py-4 rounded-xl lg:rounded-2xl font-bold text-sm lg:text-base flex items-center justify-center gap-2 transition-all duration-200 border ${colors.cardBorder} bg-white/50 dark:bg-white/5 hover:bg-slate-50 dark:hover:bg-white/10 active:scale-95 shadow-sm ${colors.textPrimary}`}
              >
                Explore Architecture <ChevronRight className="w-5 h-5" />
              </Link>
            </div>
          </FadeIn>
        </div>

        <div className="absolute inset-0 w-full max-w-7xl mx-auto pointer-events-none z-0">
          <FadeIn direction="none" delay={0.6}>
            <div className="absolute top-40 sm:top-48 left-10 lg:left-32 opacity-15 animate-float">
              <Sparkles className="w-8 h-8 xl:w-10 xl:h-10 text-purple-400" />
            </div>
            <div className="absolute top-24 sm:top-28 right-8 lg:right-24 opacity-10 animate-float-delayed">
              <Brain className="w-10 h-10 xl:w-12 xl:h-12 text-teal-400" />
            </div>
            <div className="absolute bottom-40 sm:bottom-48 left-20 lg:left-48 opacity-20 animate-float-slow">
              <Trophy className="w-12 h-12 xl:w-14 xl:h-14 text-amber-400" />
            </div>
            <div className="absolute bottom-20 sm:bottom-28 right-12 lg:right-32 opacity-15 animate-float-slow">
              <Target className="w-10 h-10 xl:w-12 xl:h-12 text-rose-400" />
            </div>
          </FadeIn>
        </div>
      </section>

      {/* Stats Bar */}
      <section ref={statsRef} className={`w-full py-10 lg:py-12 px-4 sm:px-6 lg:px-8 border-y ${colors.sectionBorder} ${colors.sectionBg} z-10 relative shadow-sm`}>
        <div className="max-w-6xl mx-auto">
          <FadeIn delay={0.1} direction="none">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-8 lg:gap-12 text-center">
              {stats.map((stat, idx) => (
                <div key={idx} className="space-y-3 lg:space-y-4 group">
                  <div className="flex justify-center text-purple-600 dark:text-teal-400 group-hover:scale-110 transition-transform duration-300">{stat.icon}</div>
                  <div className={`text-3xl lg:text-4xl font-poppins font-extrabold ${colors.textPrimary} tracking-tight`}>
                    {animatedStats ? stat.value : "..."}
                  </div>
                  <div className={`text-xs lg:text-sm font-bold uppercase tracking-widest ${colors.textSecondary}`}>{stat.label}</div>
                </div>
              ))}
            </div>
          </FadeIn>
        </div>
      </section>

      {/* Showcase 1 – AI Reflection Coach */}
            <section className="w-full py-20 lg:py-32 px-4 sm:px-6 lg:px-8 z-10 relative">
              <div className="max-w-7xl mx-auto">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-center">
                  <FadeIn direction="left" fullWidth className="order-2 lg:order-1">
                    <div className={`rounded-3xl lg:rounded-[2rem] ${colors.cardBg} border ${colors.cardBorder} p-6 lg:p-10 shadow-2xl relative overflow-hidden group hover:shadow-purple-500/10 transition-shadow duration-300`}>
                      <div className="absolute top-0 right-0 w-40 h-40 bg-purple-500/10 rounded-full blur-3xl group-hover:bg-purple-500/20 transition-all duration-500" />

                      <div className={`flex items-center gap-4 mb-8 border-b ${colors.sectionBorder} pb-5`}>
                        <div className="p-3 lg:p-4 rounded-xl lg:rounded-2xl bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 shadow-sm border border-purple-200/50 dark:border-purple-500/20">
                          <Database className="w-6 h-6 lg:w-8 lg:h-8" />
                        </div>
                        <div>
                          <h3 className={`font-poppins font-bold text-lg lg:text-xl ${colors.textPrimary}`}>pgvector Search Activated</h3>
                          <p className={`text-xs lg:text-sm font-medium ${colors.textSecondary} mt-0.5`}>Retrieving Semantic Context...</p>
                        </div>
                      </div>

                      <div className="space-y-5 lg:space-y-6 relative z-10">
                        <div className={`${colors.sectionBg} p-4 lg:p-5 rounded-2xl rounded-br-sm border ${colors.sectionBorder} ml-4 sm:ml-12 shadow-sm relative`}>
                          <p className={`text-sm lg:text-base font-medium ${colors.textPrimary}`}>Why do I feel so anxious when starting new projects?</p>
                        </div>
                        <div className="bg-gradient-to-r from-purple-500 to-teal-500 dark:from-purple-600 dark:to-teal-600 p-4 lg:p-5 rounded-2xl rounded-bl-sm mr-4 sm:mr-12 shadow-md relative">
{/* 🌟 FIX: Removed gray-900, syncs perfectly to the card background */}
<Sparkles className={`absolute -left-2 -top-2 w-5 h-5 text-purple-500 dark:text-teal-400 ${isDarkMode ? 'bg-[#1A162F]' : 'bg-white'} rounded-full p-0.5`} />                          <p className="text-sm lg:text-base font-medium text-white leading-relaxed">
                            I noticed in your entry from last month, you mentioned feeling overwhelmed before your presentation. You often feel this way when stepping into the unknown. Remember how capable you felt afterward?
                          </p>
                        </div>
                      </div>
                    </div>
                  </FadeIn>

                  <FadeIn direction="right" fullWidth className="order-1 lg:order-2">
                    <div className="inline-flex p-3 lg:p-4 rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-purple-900/30 dark:to-purple-800/20 text-purple-600 dark:text-purple-400 mb-6 shadow-sm border border-purple-200/50 dark:border-purple-700/30">
                      <Brain className="w-6 h-6 lg:w-8 lg:h-8" />
                    </div>
                    <h2 className={`text-3xl sm:text-4xl lg:text-5xl font-poppins font-extrabold mb-6 tracking-tight ${colors.textPrimary}`}>
                      An AI that <span className="text-transparent bg-clip-text bg-gradient-to-r from-purple-600 to-teal-500 dark:from-purple-400 dark:to-teal-400">Actually Remembers</span>
                    </h2>
                    <p className={`text-base sm:text-lg lg:text-xl ${colors.textSecondary} mb-8 leading-relaxed font-medium`}>
                      Unlike standard chatbots, MyMindMirror uses advanced Retrieval-Augmented Generation (RAG). Every time you ask a question, the system queries a high-performance PostgreSQL vector database to fetch your most relevant past emotions and summaries, providing deeply contextual, hyper-personalized advice.
                    </p>
                    <ul className={`space-y-4 font-semibold text-sm lg:text-base ${colors.textPrimary}`}>
                      <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-teal-500 shrink-0" /> Redis-backed conversational memory.</li>
                      <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-teal-500 shrink-0" /> Context-aware reflective questions.</li>
                      <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-teal-500 shrink-0" /> AES-encrypted vector storage.</li>
                    </ul>
                  </FadeIn>
                </div>
              </div>
            </section>

            {/* Showcase 2 – Lifestyle & Smart Schedule */}
<section className={`w-full py-20 lg:py-32 px-4 sm:px-6 lg:px-8 border-y ${colors.sectionBorder} ${colors.bandBg} z-10 relative shadow-sm`}>              <div className="max-w-7xl mx-auto">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-center">
                  <FadeIn direction="right" fullWidth>
                    <div className="inline-flex p-3 lg:p-4 rounded-2xl bg-gradient-to-br from-teal-100 to-teal-50 dark:from-teal-900/30 dark:to-teal-800/20 text-teal-600 dark:text-teal-400 mb-6 shadow-sm border border-teal-200/50 dark:border-teal-700/30">
                      <CalendarDays className="w-6 h-6 lg:w-8 lg:h-8" />
                    </div>
                    <h2 className={`text-3xl sm:text-4xl lg:text-5xl font-poppins font-extrabold mb-6 tracking-tight ${colors.textPrimary}`}>
                      The <span className="text-transparent bg-clip-text bg-gradient-to-r from-teal-600 to-emerald-500 dark:from-teal-400 dark:to-emerald-400">Burnout-Free</span> Schedule
                    </h2>
                    <p className={`text-base sm:text-lg lg:text-xl ${colors.textSecondary} mb-8 leading-relaxed font-medium`}>
                      Stop fighting unrealistic timetables. Tell the AI your energy peaks, sleep schedule, and daily habits. It weaves your roadmap tasks, milestones, and breaks into a perfectly balanced schedule. Life happened? Hit <strong>"Re-optimize Today"</strong> and the AI instantly recalculates your remaining hours.
                    </p>
                    <Link to="/journal#schedule" className="inline-flex items-center gap-2 font-bold text-teal-600 dark:text-teal-400 hover:text-teal-700 dark:hover:text-teal-300 transition-colors text-base lg:text-lg">
                      Explore Lifestyle Engine <ArrowRight className="w-5 h-5 lg:w-6 lg:h-6" />
                    </Link>
                  </FadeIn>

                  <FadeIn direction="left" fullWidth>
                    <div className={`rounded-3xl lg:rounded-[2rem] ${colors.cardBg} border ${colors.cardBorder} p-6 lg:p-10 shadow-2xl space-y-6 hover:shadow-teal-500/10 transition-shadow duration-300`}>
                      <div className="grid grid-cols-2 gap-4 lg:gap-5 mb-4">
                        <div className={`${colors.sectionBg} p-4 lg:p-5 rounded-2xl border ${colors.sectionBorder} flex flex-col items-center shadow-sm`}>
                          <Activity className="w-6 h-6 lg:w-8 lg:h-8 text-amber-500 mb-2" />
                          <span className={`text-[10px] lg:text-xs font-bold uppercase tracking-widest ${colors.textSecondary}`}>Energy Peak</span>
                          <span className={`text-sm lg:text-base font-bold ${colors.textPrimary} mt-0.5`}>Night Owl</span>
                        </div>
                        <div className={`${colors.sectionBg} p-4 lg:p-5 rounded-2xl border ${colors.sectionBorder} flex flex-col items-center shadow-sm`}>
                          <Coffee className="w-6 h-6 lg:w-8 lg:h-8 text-orange-500 mb-2" />
                          <span className={`text-[10px] lg:text-xs font-bold uppercase tracking-widest ${colors.textSecondary}`}>Daily Habit</span>
                          <span className={`text-sm lg:text-base font-bold ${colors.textPrimary} mt-0.5`}>15m Meditation</span>
                        </div>
                      </div>

                      <div className="space-y-3 lg:space-y-4">
                        <div className={`flex items-center justify-between p-4 lg:p-5 rounded-xl lg:rounded-2xl ${colors.innerContentBg} border-l-4 border-l-teal-500 shadow-sm border ${colors.sectionBorder}`}>
                          <div>
                            <h4 className={`text-sm lg:text-base font-bold ${colors.textPrimary}`}>Build React Component</h4>
                            <p className={`text-xs lg:text-sm font-medium ${colors.textSecondary} mt-0.5`}>14:00 - 15:30 • High Focus</p>
                          </div>
                        </div>
                        <div className={`flex items-center justify-between p-4 lg:p-5 rounded-xl lg:rounded-2xl ${colors.innerContentBg} border-l-4 border-l-amber-500 opacity-80 shadow-sm border ${colors.sectionBorder}`}>
                          <div>
                            <h4 className="text-sm lg:text-base font-bold text-amber-700 dark:text-amber-400">Meditation Break</h4>
                            <p className={`text-xs lg:text-sm font-medium ${colors.textSecondary} mt-0.5`}>15:30 - 15:45 • Routine</p>
                          </div>
                        </div>
                      </div>

                      <button className="w-full mt-6 lg:mt-8 py-3.5 lg:py-4 rounded-xl lg:rounded-2xl bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold text-sm lg:text-base shadow-md hover:shadow-lg transition-transform flex items-center justify-center gap-2 hover:-translate-y-0.5 active:scale-95">
                        <Zap className="w-5 h-5 lg:w-6 lg:h-6" /> Re-optimize Rest of Today
                      </button>
                    </div>
                  </FadeIn>
                </div>
              </div>
            </section>
   {/* Showcase 2.5 – Gamification & Progression */}
         <section className="w-full py-20 lg:py-32 px-4 sm:px-6 lg:px-8 z-10 relative">
           <div className="max-w-7xl mx-auto">
             <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-center">

               <FadeIn direction="left" fullWidth className="order-2 lg:order-1">
                 <div className={`rounded-3xl lg:rounded-[2rem] ${colors.cardBg} border ${colors.cardBorder} p-6 lg:p-10 shadow-2xl space-y-6 lg:space-y-8 relative overflow-hidden group hover:shadow-amber-500/10 transition-shadow duration-300`}>
                   <div className="absolute -top-10 -right-10 w-48 h-48 bg-amber-500/20 rounded-full blur-3xl group-hover:bg-amber-500/30 transition-all duration-500" />

                   <div className={`flex items-center gap-5 lg:gap-6 border-b ${colors.sectionBorder} pb-6 lg:pb-8 relative z-10`}>
                     <div className="relative flex items-center justify-center w-20 h-20 lg:w-24 lg:h-24 shrink-0 rounded-full bg-gradient-to-br from-amber-400 to-orange-500 shadow-lg shadow-orange-500/30">
                       <span className="text-white font-poppins font-black text-3xl lg:text-4xl">12</span>
                     </div>
                     <div className="flex-1 min-w-0">
                       <div className="flex flex-wrap justify-between items-end mb-2.5 gap-2">
                         <h4 className={`font-poppins font-extrabold text-xl lg:text-2xl ${colors.textPrimary} truncate`}>Mindful Master</h4>
                         <span className="text-xs lg:text-sm font-bold text-amber-600 dark:text-amber-400 whitespace-nowrap">5,850 / 6,000 XP</span>
                       </div>
{/* 🌟 FIX: Synced to sectionBorder variable */}
<div className={`w-full h-3 lg:h-4 ${colors.sectionBg} rounded-full overflow-hidden shadow-inner border ${colors.sectionBorder}`}>                         <div className="h-full w-[95%] bg-gradient-to-r from-amber-400 to-orange-500 rounded-full shadow-[0_0_8px_rgba(245,158,11,0.6)]" />
                       </div>
                     </div>
                   </div>

                   <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 lg:gap-5 relative z-10">
                     <div className={`${colors.innerContentBg} p-4 lg:p-5 rounded-2xl border ${colors.sectionBorder} flex flex-row items-center gap-3 lg:gap-4 shadow-sm min-w-0`}>
                       <div className="p-3 rounded-xl bg-orange-100 dark:bg-orange-500/20 text-orange-600 dark:text-orange-400 shrink-0 border border-orange-200/50 dark:border-orange-500/30">
                         <Flame className="w-6 h-6 lg:w-7 lg:h-7" />
                       </div>
                       <div className="min-w-0 w-full">
                         <p className={`text-[10px] lg:text-xs ${colors.textSecondary} uppercase font-bold tracking-wider mb-0.5 truncate`}>Current Streak</p>
                         <p className={`font-poppins font-extrabold text-lg lg:text-xl ${colors.textPrimary} truncate`}>14 Days</p>
                       </div>
                     </div>

                     <div className={`${colors.innerContentBg} p-4 lg:p-5 rounded-2xl border ${colors.sectionBorder} flex flex-row items-center gap-3 lg:gap-4 shadow-sm min-w-0`}>
                       <div className="p-3 rounded-xl bg-purple-100 dark:bg-purple-500/20 text-purple-600 dark:text-purple-400 shrink-0 border border-purple-200/50 dark:border-purple-500/30">
                         <Award className="w-6 h-6 lg:w-7 lg:h-7" />
                       </div>
                       <div className="min-w-0 w-full">
                         <p className={`text-[10px] lg:text-xs ${colors.textSecondary} uppercase font-bold tracking-wider mb-0.5 truncate`}>Latest Badge</p>
                         <p className={`font-poppins font-extrabold text-lg lg:text-xl ${colors.textPrimary} truncate`}>AI Whisperer</p>
                       </div>
                     </div>
                   </div>

                   <div className="bg-amber-50/80 dark:bg-amber-500/10 border border-amber-200/80 dark:border-amber-500/20 p-4 lg:p-5 rounded-xl lg:rounded-2xl flex items-center gap-3 lg:gap-4 relative z-10 shadow-sm">
                     <Sparkles className="w-5 h-5 lg:w-6 lg:h-6 text-amber-500 shrink-0" />
                     <p className="text-sm lg:text-base font-bold text-amber-800 dark:text-amber-300 leading-snug">
                       +50 XP earned for completing today's journal entry!
                     </p>
                   </div>
                 </div>
               </FadeIn>

               <FadeIn direction="right" fullWidth className="order-1 lg:order-2">
                 <div className="inline-flex p-3 lg:p-4 rounded-2xl bg-gradient-to-br from-amber-100 to-amber-50 dark:from-amber-900/30 dark:to-amber-800/20 text-amber-600 dark:text-amber-400 mb-6 shadow-sm border border-amber-200/50 dark:border-amber-700/30">
                   <Trophy className="w-6 h-6 lg:w-8 lg:h-8" />
                 </div>
                 <h2 className={`text-3xl sm:text-4xl lg:text-5xl font-poppins font-extrabold mb-6 tracking-tight ${colors.textPrimary}`}>
                   Level Up Your <span className="text-transparent bg-clip-text bg-gradient-to-r from-amber-500 to-orange-500 dark:from-amber-400 dark:to-orange-400">Mental Growth</span>
                 </h2>
                 <p className={`text-base sm:text-lg lg:text-xl ${colors.textSecondary} mb-8 leading-relaxed font-medium`}>
                   Consistency is the hardest part of self-improvement. MyMindMirror turns your journey into a rewarding experience. Earn Experience Points (XP) for analyzing entries, scheduling roadmaps, and hitting milestones.
                 </p>
                 <ul className={`space-y-4 font-semibold text-sm lg:text-base ${colors.textPrimary}`}>
                   <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-amber-500 shrink-0" /> Unlock dynamic achievement badges.</li>
                   <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-amber-500 shrink-0" /> Track your longest reflection streaks.</li>
                   <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-amber-500 shrink-0" /> Progress through global mental mastery levels.</li>
                 </ul>
               </FadeIn>
             </div>
           </div>
         </section>

         {/* Showcase 3 - Data Export & Analytics */}
<section className={`w-full py-20 lg:py-32 px-4 sm:px-6 lg:px-8 border-y ${colors.sectionBorder} ${colors.bandBg} z-10 relative shadow-sm`}>           <div className="max-w-7xl mx-auto">
             <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-center">
               <FadeIn direction="left" fullWidth className="order-2 lg:order-1">
                 <div className="grid grid-cols-2 gap-4 lg:gap-6">
                   <div className={`rounded-2xl lg:rounded-3xl ${colors.cardBg} border ${colors.cardBorder} p-6 lg:p-8 shadow-xl flex flex-col items-center text-center group hover:-translate-y-1 hover:shadow-rose-500/10 transition-transform duration-300`}>
                     <div className="p-4 lg:p-5 bg-rose-100 dark:bg-rose-500/20 text-rose-600 dark:text-rose-400 rounded-full mb-4 lg:mb-5 group-hover:scale-110 transition-transform border border-rose-200/50 dark:border-rose-500/30 shadow-sm"><FileText className="w-8 h-8 lg:w-10 lg:h-10" /></div>
                     <h3 className={`font-poppins font-bold text-lg lg:text-xl ${colors.textPrimary} mb-2`}>PDF Reports</h3>
                     <p className={`text-xs lg:text-sm font-medium ${colors.textSecondary}`}>Paginated & beautifully formatted.</p>
                   </div>
                   <div className={`rounded-2xl lg:rounded-3xl ${colors.cardBg} border ${colors.cardBorder} p-6 lg:p-8 shadow-xl flex flex-col items-center text-center group hover:-translate-y-1 hover:shadow-blue-500/10 transition-transform duration-300`}>
                     <div className="p-4 lg:p-5 bg-blue-100 dark:bg-blue-500/20 text-blue-600 dark:text-blue-400 rounded-full mb-4 lg:mb-5 group-hover:scale-110 transition-transform border border-blue-200/50 dark:border-blue-500/30 shadow-sm"><Database className="w-8 h-8 lg:w-10 lg:h-10" /></div>
                     <h3 className={`font-poppins font-bold text-lg lg:text-xl ${colors.textPrimary} mb-2`}>CSV Data</h3>
                     <p className={`text-xs lg:text-sm font-medium ${colors.textSecondary}`}>Raw structural data for analysts.</p>
                   </div>
                   <div className={`col-span-2 rounded-2xl lg:rounded-3xl ${colors.cardBg} border ${colors.cardBorder} p-6 lg:p-8 shadow-xl flex flex-col sm:flex-row items-center sm:items-start lg:items-center text-center sm:text-left gap-4 lg:gap-6 group hover:-translate-y-1 hover:shadow-indigo-500/10 transition-transform duration-300`}>
                     <div className="p-4 lg:p-5 bg-indigo-100 dark:bg-indigo-500/20 text-indigo-600 dark:text-indigo-400 rounded-2xl group-hover:rotate-12 transition-transform border border-indigo-200/50 dark:border-indigo-500/30 shadow-sm shrink-0"><ImageIcon className="w-8 h-8 lg:w-10 lg:h-10" /></div>
                     <div>
                       <h3 className={`font-poppins font-bold text-lg lg:text-xl ${colors.textPrimary} mb-2`}>High-Res Chart PNGs</h3>
                       <p className={`text-xs lg:text-sm font-medium ${colors.textSecondary}`}>Download panoramic timelines and radar charts in Retina quality directly to your device.</p>
                     </div>
                   </div>
                 </div>
               </FadeIn>

               <FadeIn direction="right" fullWidth className="order-1 lg:order-2">
                 <div className="inline-flex p-3 lg:p-4 rounded-2xl bg-gradient-to-br from-rose-100 to-rose-50 dark:from-rose-900/30 dark:to-rose-800/20 text-rose-600 dark:text-rose-400 mb-6 shadow-sm border border-rose-200/50 dark:border-rose-700/30">
                   <LineChart className="w-6 h-6 lg:w-8 lg:h-8" />
                 </div>
                 <h2 className={`text-3xl sm:text-4xl lg:text-5xl font-poppins font-extrabold mb-6 tracking-tight ${colors.textPrimary}`}>
                   Own Your <span className="text-transparent bg-clip-text bg-gradient-to-r from-rose-500 to-orange-500 dark:from-rose-400 dark:to-orange-400">Data</span>
                 </h2>
                 <p className={`text-base sm:text-lg lg:text-xl ${colors.textSecondary} mb-8 leading-relaxed font-medium`}>
                   We believe your mental health data belongs strictly to you. Enjoy comprehensive visual analytics inside the app, or instantly export everything. From perfectly structured CSVs to gorgeous, paginated PDF dossiers of your entire journey.
                 </p>
                 <ul className={`space-y-4 font-semibold text-sm lg:text-base ${colors.textPrimary}`}>
                   <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-rose-500 shrink-0" /> Zero paywalls on data extraction.</li>
                   <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-rose-500 shrink-0" /> Client-side PDF compilation.</li>
                 </ul>
               </FadeIn>
             </div>
           </div>
         </section>
  {/* Showcase 4 - Dynamic Roadmaps & Elaboration */}
        <section className="w-full py-20 lg:py-32 px-4 sm:px-6 lg:px-8 z-10 relative">
          <div className="max-w-7xl mx-auto">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 lg:gap-20 items-center">

              <FadeIn direction="left" fullWidth className="order-2 lg:order-1">
                <div className={`rounded-3xl lg:rounded-[2rem] ${colors.cardBg} border ${colors.cardBorder} p-6 lg:p-10 shadow-2xl relative overflow-hidden group hover:shadow-cyan-500/10 transition-shadow duration-300`}>
                  <div className="absolute -bottom-10 -left-10 w-48 h-48 bg-cyan-500/20 rounded-full blur-3xl group-hover:bg-cyan-500/30 transition-all duration-500" />

                  <div className="space-y-4 lg:space-y-5 relative z-10">
                    <div className={`flex items-center justify-between border-b ${colors.sectionBorder} pb-5 mb-5`}>
                        <div className="flex items-center gap-3 lg:gap-4">
                          <div className="p-2.5 lg:p-3 rounded-xl bg-cyan-100 dark:bg-cyan-900/30 text-cyan-600 dark:text-cyan-400 border border-cyan-200/50 dark:border-cyan-500/30 shadow-sm"><MapPin className="w-5 h-5 lg:w-6 lg:h-6" /></div>
                          <h3 className={`font-poppins font-bold text-lg lg:text-xl ${colors.textPrimary}`}>React Architecture</h3>
                        </div>
                        <span className="text-[10px] lg:text-xs font-bold uppercase tracking-wider text-cyan-700 dark:text-cyan-300 bg-cyan-50 dark:bg-cyan-500/10 border border-cyan-200 dark:border-cyan-500/20 px-3 py-1.5 rounded-lg shadow-sm">Week 2</span>
                    </div>

                    <div className={`${colors.sectionBg} p-5 lg:p-6 rounded-2xl border ${colors.sectionBorder} shadow-sm hover:border-cyan-300 dark:hover:border-cyan-500/30 transition-colors`}>
                      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-3 mb-3">
                        <h4 className={`font-poppins font-extrabold text-base lg:text-lg ${colors.textPrimary}`}>Understand Context API</h4>
                        <button className="text-[10px] lg:text-xs font-bold uppercase tracking-widest text-purple-700 dark:text-purple-300 bg-purple-100 dark:bg-purple-900/40 border border-purple-200 dark:border-purple-500/30 px-3 py-1.5 rounded-lg flex items-center gap-1.5 cursor-default shadow-sm shrink-0 w-max">
                          <Sparkles className="w-3 h-3 lg:w-3.5 lg:h-3.5" /> Elaborated
                        </button>
                      </div>
                      <p className={`text-xs lg:text-sm font-medium ${colors.textSecondary} mb-5 leading-relaxed`}>Learn how to avoid prop drilling by sharing state globally across your application.</p>

                      <div className="space-y-3 lg:space-y-3.5 pl-4 lg:pl-5 border-l-2 border-purple-400/40 dark:border-purple-500/30">
                          <div className={`flex items-start gap-3 text-xs lg:text-sm font-bold ${colors.textPrimary}`}><div className="w-2 h-2 rounded-full bg-purple-500 shadow-[0_0_8px_rgba(168,85,247,0.8)] mt-1 shrink-0" /> Create a ThemeProvider component</div>
                          <div className={`flex items-start gap-3 text-xs lg:text-sm font-bold ${colors.textPrimary}`}><div className="w-2 h-2 rounded-full bg-purple-500 shadow-[0_0_8px_rgba(168,85,247,0.8)] mt-1 shrink-0" /> Wrap your root App component</div>
{/* 🌟 FIX: Replaced hardcoded dots with current text color using currentColor (bg-current) */}
<div className={`flex items-start gap-3 text-xs lg:text-sm font-bold ${colors.textSecondary}`}><div className="w-2 h-2 rounded-full bg-current opacity-50 mt-1 shrink-0" /> Implement the useContext hook</div>                      </div>
                    </div>
                  </div>
                </div>
              </FadeIn>

              <FadeIn direction="right" fullWidth className="order-1 lg:order-2">
                <div className="inline-flex p-3 lg:p-4 rounded-2xl bg-gradient-to-br from-cyan-100 to-cyan-50 dark:from-cyan-900/30 dark:to-cyan-800/20 text-cyan-600 dark:text-cyan-400 mb-6 shadow-sm border border-cyan-200/50 dark:border-cyan-700/30">
                  <Target className="w-6 h-6 lg:w-8 lg:h-8" />
                </div>
                <h2 className={`text-3xl sm:text-4xl lg:text-5xl font-poppins font-extrabold mb-6 tracking-tight ${colors.textPrimary}`}>
                  Turn Ambition into <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-600 to-blue-600 dark:from-cyan-400 dark:to-blue-400">Action</span>
                </h2>
<p className={`text-base sm:text-lg lg:text-xl ${colors.textSecondary} mb-8 leading-relaxed font-medium`}>
  Don't just set goals—engineer them. Generate perfectly paced, chunked learning roadmaps that adapt to your schedule and language. View your progress through a deeply nested List View, or toggle to our sleek horizontal CSS Grid Timeline. Hit a wall? Click <strong>"Elaborate"</strong> and our AI will break any complex task down into actionable micro-subtasks.
</p>
<ul className={`space-y-4 font-semibold text-sm lg:text-base ${colors.textPrimary}`}>
  <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-cyan-500 shrink-0" /> Dual-DOM Timeline & List rendering.</li>
  <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-cyan-500 shrink-0" /> Deep-dive AI task elaboration.</li>
  <li className="flex items-center gap-3"><CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-cyan-500 shrink-0" /> Continual roadmap batch generation.</li>
</ul>
              </FadeIn>

            </div>
          </div>
        </section>

        {/* Features Grid (Bento Box Style) */}
        {/* 🌟 FIX: Removed heavy backdrop-blur but kept the band so the 12 cards have a clean visual zone */}
{/* 🌟 FIX: Removed hardcoded bg and replaced with our mapped bandBg */}
      <section className={`w-full py-24 lg:py-32 px-4 sm:px-6 lg:px-8 border-t ${colors.sectionBorder} ${colors.bandBg} z-10 relative shadow-sm`}>          <div className="max-w-7xl mx-auto">
            <FadeIn delay={0.1}>
              <div className="text-center mb-16 lg:mb-20">
                <h2 className={`text-3xl sm:text-4xl lg:text-5xl font-poppins font-extrabold mb-6 tracking-tight ${colors.textPrimary}`}>
                  Everything You Need for{" "}
                  <span className="bg-gradient-to-r from-purple-600 to-teal-500 dark:from-purple-400 dark:to-teal-400 bg-clip-text text-transparent">
                    Personal Growth
                  </span>
                </h2>
                <p className={`text-lg lg:text-xl max-w-3xl mx-auto ${colors.textSecondary} font-medium leading-relaxed`}>
                  A flawless integration of modern UI, secure data architecture, and advanced AI models working together seamlessly.
                </p>
              </div>
            </FadeIn>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
              {features.map((feature, idx) => (
                <FadeIn key={idx} delay={idx * 0.1} fullWidth>
                  <div className={`group relative p-8 lg:p-10 rounded-3xl lg:rounded-[2rem] ${colors.cardBg} border ${colors.cardBorder} transition duration-500 hover:-translate-y-2 hover:shadow-2xl hover:border-purple-500/30 dark:hover:border-white/20 active:scale-[0.98] active:bg-slate-100 dark:active:bg-white/5 overflow-hidden flex flex-col h-full cursor-pointer`}>
                    <div className={`absolute top-0 right-0 w-32 h-32 lg:w-40 lg:h-40 bg-gradient-to-br ${feature.gradient} rounded-full blur-[50px] opacity-0 group-hover:opacity-100 transition-opacity duration-700 pointer-events-none`} />
                    <div className="relative z-10 flex-grow">
                      <div className={`mb-6 lg:mb-8 w-14 h-14 lg:w-16 lg:h-16 rounded-2xl lg:rounded-3xl ${feature.iconBg} flex items-center justify-center ${feature.iconColor} shadow-inner border border-white/40 dark:border-white/5 group-hover:scale-110 transition-transform duration-500`}>
                        {feature.icon}
                      </div>
                      <h3 className={`text-xl lg:text-2xl font-poppins font-extrabold mb-3 lg:mb-4 tracking-tight ${colors.textPrimary}`}>{feature.title}</h3>
                      <p className={`text-sm lg:text-base font-medium leading-relaxed ${colors.textSecondary}`}>{feature.description}</p>
                    </div>
                  </div>
                </FadeIn>
              ))}
            </div>
          </div>
        </section>

        {/* Final CTA */}
        <section className="w-full py-24 lg:py-32 px-4 sm:px-6 lg:px-8 z-10 relative">
          <div className="max-w-5xl mx-auto text-center">
            <FadeIn direction="up">
              <div className={`rounded-[2.5rem] lg:rounded-[3.5rem] ${colors.cardBg} border ${colors.cardBorder} p-10 sm:p-16 lg:p-20 relative overflow-hidden transition-shadow duration-500 shadow-2xl hover:shadow-[0_0_50px_rgba(168,85,247,0.15)]`}>
                <div className="absolute inset-0 bg-gradient-to-tr from-purple-500/10 via-transparent to-teal-500/10 pointer-events-none" />
                <div className="relative z-10">
                  <div className="inline-flex p-4 lg:p-5 rounded-3xl bg-gradient-to-br from-purple-500/20 to-teal-500/20 dark:from-purple-900/30 dark:to-teal-900/30 mb-8 lg:mb-10 shadow-inner border border-white/40 dark:border-white/10">
                    <Brain className="w-10 h-10 lg:w-12 lg:h-12 text-purple-600 dark:text-purple-400" />
                  </div>
                  <h2 className={`text-4xl sm:text-5xl lg:text-6xl font-poppins font-extrabold mb-6 lg:mb-8 tracking-tight ${colors.textPrimary} leading-tight`}>
                    Ready to Start Your Journey?
                  </h2>
                  <p className={`text-lg sm:text-xl lg:text-2xl mb-10 lg:mb-12 max-w-3xl mx-auto ${colors.textSecondary} font-medium leading-relaxed`}>
                    Join a secure, intelligent ecosystem that adapts to your life, protects your thoughts, and engineers your success.
                  </p>
                  <Link
                    to={isAuthenticated ? "/journal" : "/register"}
                    className={`${colors.buttonPrimary} px-10 py-4 lg:px-12 lg:py-5 rounded-2xl lg:rounded-3xl font-bold text-lg lg:text-xl inline-flex items-center gap-3 transition-transform hover:scale-105 shadow-xl`}
                  >
                    {isAuthenticated ? "Go to Dashboard" : "Create Free Account"}
                    <ArrowRight className="w-5 h-5 lg:w-6 lg:h-6" />
                  </Link>
                </div>
              </div>
            </FadeIn>
          </div>
        </section>
      </div>
    );
  }

  export default React.memo(HomePage);