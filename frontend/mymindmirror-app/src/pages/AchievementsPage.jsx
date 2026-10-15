// src/pages/AchievementsPage.jsx
import React, { useEffect, useState } from 'react';
import ReactDOM from 'react-dom';
import { useGamificationStats } from '../hooks/useGamification';
import { useTheme } from '../contexts/ThemeContext';
import FadeIn from '../components/FadeIn';
import { SkeletonAchievements } from '../components/Skeleton';

import {
  Flame, Award, Target, Star, Trophy, Sparkles, CheckCircle, Clock,
  TrendingUp, Zap, Medal, Gem, Crown, BookOpen, CalendarCheck, BrainCircuit, Shield, Eye, Map, Compass, Search
} from 'lucide-react';

const allBadges = {
  FIRST_STEP: { icon: Star, label: 'First Step', color: 'text-amber-500', bg: 'bg-amber-100 dark:bg-amber-900/30 border-amber-200 dark:border-amber-800/50', description: 'Completed your first task', requirement: 'Complete 1 task', rarity: 'Common' },
  TASK_MASTER: { icon: Target, label: 'Task Master', color: 'text-blue-500', bg: 'bg-blue-100 dark:bg-blue-900/30 border-blue-200 dark:border-blue-800/50', description: 'Completed 10 tasks', requirement: 'Complete 10 tasks', rarity: 'Rare' },
  PRODUCTIVITY_NINJA: { icon: Zap, label: 'Productivity Ninja', color: 'text-indigo-500', bg: 'bg-indigo-100 dark:bg-indigo-900/30 border-indigo-200 dark:border-indigo-800/50', description: 'Crushed 50 tasks', requirement: 'Complete 50 tasks', rarity: 'Epic' },
  FIRST_THOUGHT: { icon: BookOpen, label: 'First Thought', color: 'text-teal-500', bg: 'bg-teal-100 dark:bg-teal-900/30 border-teal-200 dark:border-teal-800/50', description: 'Wrote your first journal entry', requirement: 'Write 1 journal entry', rarity: 'Common' },
  REFLECTIVE_SOUL: { icon: BrainCircuit, label: 'Reflective Soul', color: 'text-cyan-500', bg: 'bg-cyan-100 dark:bg-cyan-900/30 border-cyan-200 dark:border-cyan-800/50', description: 'Logged 5 journal entries', requirement: 'Write 5 journal entries', rarity: 'Rare' },
  TIME_LORD: { icon: CalendarCheck, label: 'Time Lord', color: 'text-fuchsia-500', bg: 'bg-fuchsia-100 dark:bg-fuchsia-900/30 border-fuchsia-200 dark:border-fuchsia-800/50', description: 'Used AI to generate a Smart Timetable', requirement: 'Generate 1 Smart Schedule', rarity: 'Common' },
  FIRST_CHAT: { icon: Sparkles, label: 'First Chat', color: 'text-pink-500', bg: 'bg-pink-100 dark:bg-pink-900/30 border-pink-200 dark:border-pink-800/50', description: 'Talked to the AI Coach', requirement: 'Send 1 chat message', rarity: 'Common' },
  AI_WHISPERER: { icon: Sparkles, label: 'AI Whisperer', color: 'text-pink-500', bg: 'bg-pink-100 dark:bg-pink-900/30 border-pink-200 dark:border-pink-800/50', description: 'Talked to the AI Coach 20 times', requirement: 'Send 20 chat messages', rarity: 'Epic' },
  VISIONARY: { icon: Eye, label: 'Visionary', color: 'text-emerald-500', bg: 'bg-emerald-100 dark:bg-emerald-900/30 border-emerald-200 dark:border-emerald-800/50', description: 'Created your first Milestone', requirement: 'Create 1 Milestone', rarity: 'Common' },
  ARCHITECT: { icon: Map, label: 'Architect', color: 'text-indigo-500', bg: 'bg-indigo-100 dark:bg-indigo-900/30 border-indigo-200 dark:border-indigo-800/50', description: 'Generated your first AI Roadmap', requirement: 'Generate 1 Roadmap', rarity: 'Rare' },
  INTROSPECTIVE: { icon: Compass, label: 'Introspective', color: 'text-rose-500', bg: 'bg-rose-100 dark:bg-rose-900/30 border-rose-200 dark:border-rose-800/50', description: 'Generated an AI Daily Reflection', requirement: 'Generate 1 Reflection', rarity: 'Common' },
  DEEP_DIVER: { icon: Search, label: 'Deep Diver', color: 'text-sky-500', bg: 'bg-sky-100 dark:bg-sky-900/30 border-sky-200 dark:border-sky-800/50', description: 'Used AI to elaborate on a task', requirement: 'Elaborate 1 Task', rarity: 'Rare' },
  THREE_DAY_STREAK: { icon: Flame, label: '3‑Day Streak', color: 'text-orange-500', bg: 'bg-orange-100 dark:bg-orange-900/30 border-orange-200 dark:border-orange-800/50', description: 'Maintained a 3‑day streak', requirement: 'Complete any activity 3 days in a row', rarity: 'Common' },
  SEVEN_DAY_STREAK: { icon: Flame, label: '7‑Day Streak', color: 'text-red-500', bg: 'bg-red-100 dark:bg-red-900/30 border-red-200 dark:border-red-800/50', description: 'Maintained a 7‑day streak', requirement: 'Complete any activity 7 days in a row', rarity: 'Rare' },
  THIRTY_DAY_LEGEND: { icon: Crown, label: '30‑Day Legend', color: 'text-yellow-500', bg: 'bg-yellow-100 dark:bg-yellow-900/30 border-yellow-200 dark:border-yellow-800/50', description: 'Maintained a 30‑day streak', requirement: 'Complete any activity 30 days in a row', rarity: 'Legendary' },
  ROADMAP_FINISHER: { icon: Trophy, label: 'Roadmap Finisher', color: 'text-purple-500', bg: 'bg-purple-100 dark:bg-purple-900/30 border-purple-200 dark:border-purple-800/50', description: 'Completed a full roadmap', requirement: 'Complete all tasks in any roadmap', rarity: 'Epic' },
};

const XP_PER_LEVEL = 500;

function AchievementsPage() {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';
  const { data: stats, isLoading, isError, refetch } = useGamificationStats();

  const [animatedValues, setAnimatedValues] = useState({
    streak: 0, longestStreak: 0, tasks: 0, xp: 0, journals: 0, chats: 0
  });
  const [newBadgeEarned, setNewBadgeEarned] = useState(null);

  useEffect(() => {
    if (stats) {
      const duration = 1000;
      const steps = 40;
      const stepDuration = duration / steps;
      let step = 0;
      const interval = setInterval(() => {
        step++;
        setAnimatedValues({
          streak: Math.min(stats.currentStreak, Math.floor((stats.currentStreak * step) / steps)),
          longestStreak: Math.min(stats.longestStreak, Math.floor((stats.longestStreak * step) / steps)),
          tasks: Math.min(stats.totalTasksCompleted, Math.floor((stats.totalTasksCompleted * step) / steps)),
          xp: Math.min(stats.experiencePoints, Math.floor((stats.experiencePoints * step) / steps)),
          journals: Math.min(stats.totalJournalEntries, Math.floor((stats.totalJournalEntries * step) / steps)),
          chats: Math.min(stats.totalChats, Math.floor((stats.totalChats * step) / steps)),
        });
        if (step >= steps) clearInterval(interval);
      }, stepDuration);
      return () => clearInterval(interval);
    }
  }, [stats]);

  useEffect(() => {
    if (stats?.badges?.length) {
      const previousBadges = JSON.parse(localStorage.getItem('previousBadges') || '[]');
      const newBadge = stats.badges.find(b => !previousBadges.includes(b));
      if (newBadge) {
        setNewBadgeEarned(newBadge);
        setTimeout(() => setNewBadgeEarned(null), 6000);
      }
      localStorage.setItem('previousBadges', JSON.stringify(stats.badges));
    }
  }, [stats]);

  const { earnedBadges, earnedCount, totalBadges, completionPercent, nextBadgeInfo } = React.useMemo(() => {
    if (!stats) return { earnedBadges: [], earnedCount: 0, totalBadges: 1, completionPercent: 0, nextBadgeInfo: null };

    const earned = stats.badges || [];
    const total = Object.keys(allBadges).length;

    const unearned = Object.keys(allBadges).filter(b => !earned.includes(b));
    const nextKey = unearned.length > 0 ? unearned[0] : null;

    return {
      earnedBadges: earned,
      earnedCount: earned.length,
      totalBadges: total,
      completionPercent: (earned.length / total) * 100,
      nextBadgeInfo: nextKey ? allBadges[nextKey] : null
    };
  }, [stats]);



  // Instantly locks the entire complex page layout into place!
  if (isLoading) {
    return <SkeletonAchievements />;
  }

  if (isError || !stats) {
    return (
      <div className="w-full flex-grow flex items-center justify-center p-4">
        <div className="text-center space-y-4">
          <p className="font-bold text-lg text-slate-900 dark:text-gray-100">Failed to load achievements.</p>
          <button onClick={() => refetch()} className="px-6 py-2.5 rounded-full bg-purple-500 hover:bg-purple-600 text-white font-bold transition-all active:scale-95 shadow-md">
            Retry Connection
          </button>
        </div>
      </div>
    );
  }

  const level = stats.level || 1;
  const currentLevelXP = animatedValues.xp % XP_PER_LEVEL;
  const levelProgressPercent = (currentLevelXP / XP_PER_LEVEL) * 100;
  const NextBadgeIcon = nextBadgeInfo?.icon || Award;

  // ==========================================================================
  //  MASTER ELEVATION PALETTE (3-Layer Architecture)
  // ==========================================================================
  // Layer 1: Main Container Cards
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';

  // Layer 2: Inner Panels (Progress bars, Stat Cards, Unearned Badges)
  const sectionBg = isDarkMode ? 'bg-[#131127]/80' : 'bg-slate-50/80';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';

  // Layer 3: Deep Elements (Icons, Badges)
  const innerContentBg = isDarkMode ? 'bg-black/20' : 'bg-white';

  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  return (
    <div className="w-full flex-grow flex flex-col relative">

      <FadeIn direction="none" delay={0.5}>
        <div className="absolute top-32 left-5 lg:left-10 opacity-20 animate-float hidden lg:block pointer-events-none z-0">
          <Shield className="w-10 h-10 lg:w-16 lg:h-16 text-purple-400" />
        </div>
        <div className="absolute bottom-10 right-5 lg:right-10 opacity-20 animate-float-delayed hidden lg:block pointer-events-none z-0">
          <Flame className="w-10 h-10 lg:w-16 lg:h-16 text-orange-400" />
        </div>
      </FadeIn>

      {newBadgeEarned && ReactDOM.createPortal(
        <FadeIn direction="left" className="fixed top-20 lg:top-24 right-4 lg:right-8 z-[200]">
          <div className="bg-gradient-to-r from-amber-500 to-orange-500 text-white px-6 py-4 rounded-2xl lg:rounded-3xl shadow-2xl flex items-center gap-4 border border-white/20">
            <Sparkles className="w-8 h-8 lg:w-10 lg:h-10 animate-pulse" />
            <div>
              <p className="font-poppins font-extrabold text-lg lg:text-xl leading-tight tracking-tight">Badge Unlocked!</p>
              <p className="text-sm lg:text-base font-bold opacity-90">{allBadges[newBadgeEarned]?.label}</p>
            </div>
          </div>
        </FadeIn>,
        document.body
      )}

      <main className="relative w-full max-w-7xl mx-auto flex-grow flex flex-col space-y-8 lg:space-y-12 z-10 pb-10">
        <FadeIn delay={0.1} direction="down" fullWidth className="text-center pt-4 lg:pt-6">
          <div className="inline-flex p-3 lg:p-4 rounded-2xl lg:rounded-3xl bg-gradient-to-br from-purple-500/20 to-teal-500/20 dark:from-purple-900/30 dark:to-teal-900/30 mb-4 lg:mb-6 shadow-inner border border-purple-200/50 dark:border-teal-700/30">
            <Award className="w-8 h-8 lg:w-10 lg:h-10 text-purple-600 dark:text-teal-400" />
          </div>
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-poppins font-extrabold bg-gradient-to-r from-purple-600 to-teal-600 dark:from-purple-400 dark:to-teal-400 bg-clip-text text-transparent tracking-tight">
            Growth & Achievements
          </h1>
          <p className={`text-sm lg:text-base mt-3 font-medium ${textSecondary}`}>Level up by journaling, planning, and executing tasks.</p>
        </FadeIn>

        {/*  HERO: Level & XP Card */}
        <FadeIn delay={0.2} direction="up" fullWidth>
          <div className={`rounded-3xl lg:rounded-[2rem] ${cardBg} border ${cardBorder} transition-shadow hover:shadow-md overflow-hidden relative`}>
            <div className="absolute top-0 right-0 w-64 h-64 lg:w-96 lg:h-96 bg-gradient-to-bl from-purple-500/20 dark:from-teal-500/10 to-transparent rounded-full blur-3xl pointer-events-none" />

            <div className="p-8 sm:p-10 lg:p-12 flex flex-col md:flex-row items-center gap-8 lg:gap-12 relative z-10">
              {/*  Applied sectionBg and sectionBorder to the Level Ring */}
              <div className={`relative flex-shrink-0 flex items-center justify-center w-32 h-32 lg:w-40 lg:h-40 rounded-full ${sectionBg} border-4 ${sectionBorder} shadow-lg`}>
                <div className="absolute inset-0 rounded-full border-4 border-transparent bg-gradient-to-br from-purple-500 to-teal-400 mask-border animate-spin-slow opacity-50" />
                <div className="text-center z-10">
                  <p className="text-[10px] lg:text-xs font-bold tracking-widest text-purple-600 dark:text-teal-400 uppercase mb-1">Level</p>
                  <p className="text-5xl lg:text-6xl font-poppins font-black bg-gradient-to-br from-purple-600 to-teal-600 dark:from-purple-400 dark:to-teal-400 bg-clip-text text-transparent leading-none drop-shadow-sm">
                    {level}
                  </p>
                </div>
              </div>

              <div className="flex-1 w-full text-center md:text-left">
                <h2 className={`text-2xl lg:text-3xl font-poppins font-extrabold mb-3 tracking-tight ${textPrimary}`}>Keep Growing!</h2>
                <p className={`text-sm lg:text-base mb-6 lg:mb-8 max-w-xl leading-relaxed ${textSecondary}`}>
                  You are currently a <strong className="text-purple-600 dark:text-teal-400">Level {level}</strong> Achiever. Earn XP by completing tasks, writing journal entries, and exploring your AI insights.
                </p>

                <div className="relative max-w-2xl">
                  <div className="flex justify-between items-end mb-2.5 px-1">
                    <span className="text-sm lg:text-base font-bold text-purple-700 dark:text-teal-400">
                      {Math.floor(animatedValues.xp)} Total XP
                    </span>
                    <span className={`text-[10px] lg:text-xs font-bold uppercase tracking-widest ${textSecondary}`}>
                      {Math.floor(currentLevelXP)} / {XP_PER_LEVEL} XP to Next Level
                    </span>
                  </div>
                  {/*  Applied innerContentBg and sectionBorder for depth */}
                  <div className={`w-full ${innerContentBg} rounded-full h-3 lg:h-4 shadow-inner overflow-hidden border ${sectionBorder}`}>
                    <div
                      className="h-full bg-gradient-to-r from-purple-500 via-fuchsia-500 to-teal-400 rounded-full shadow-[0_0_12px_rgba(20,184,166,0.6)] transition-all duration-1000 ease-out"
                      style={{ width: `${levelProgressPercent}%` }}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </FadeIn>
        {/*  6-GRID: Holistic Stats */}
        <FadeIn delay={0.3} direction="up" fullWidth>
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
            <StatCard icon={<Flame className="w-6 h-6 lg:w-8 lg:h-8 text-orange-500" />} title="Current Streak" value={animatedValues.streak} suffix="Days" />
            <StatCard icon={<TrendingUp className="w-6 h-6 lg:w-8 lg:h-8 text-teal-500" />} title="Longest Streak" value={animatedValues.longestStreak} suffix="Days" />
            <StatCard icon={<BookOpen className="w-6 h-6 lg:w-8 lg:h-8 text-cyan-500" />} title="Journal Entries" value={animatedValues.journals} suffix="Logged" />
            <StatCard icon={<Target className="w-6 h-6 lg:w-8 lg:h-8 text-blue-500" />} title="Tasks Finished" value={animatedValues.tasks} suffix="Done" />
            <StatCard icon={<BrainCircuit className="w-6 h-6 lg:w-8 lg:h-8 text-pink-500" />} title="AI Coaching" value={animatedValues.chats} suffix="Chats" />
            <StatCard icon={<CalendarCheck className="w-6 h-6 lg:w-8 lg:h-8 text-fuchsia-500" />} title="Schedules Built" value={stats.schedulesGenerated || 0} suffix="Generated" />
          </div>
        </FadeIn>
        {/*  BADGE COLLECTION */}
        <FadeIn delay={0.4} direction="up" fullWidth>
          <div className={`rounded-3xl lg:rounded-[2rem] ${cardBg} border ${cardBorder} p-6 sm:p-8 lg:p-10 shadow-sm transition-shadow hover:shadow-md`}>
            <div className={`flex flex-col md:flex-row justify-between items-start md:items-end mb-8 lg:mb-10 gap-6 border-b ${sectionBorder} pb-6 lg:pb-8`}>
              <div>
                <h2 className={`text-2xl lg:text-3xl font-poppins font-extrabold flex items-center gap-3 mb-2 tracking-tight ${textPrimary}`}>
                  <Medal className="w-8 h-8 lg:w-10 lg:h-10 text-purple-500 dark:text-teal-400" />
                  Badge Collection
                </h2>
                <p className={`text-sm lg:text-base font-medium ${textSecondary}`}>Unlock these by exploring every feature of MyMindMirror.</p>
              </div>

              {/*  Applied sectionBg and sectionBorder to completion box */}
              <div className={`w-full md:w-72 shrink-0 ${sectionBg} p-4 lg:p-5 rounded-2xl border ${sectionBorder} shadow-sm`}>
                <div className={`flex justify-between text-xs lg:text-sm mb-2.5 font-bold uppercase tracking-wider ${textSecondary}`}>
                  <span>Completion</span>
                  <span className="text-purple-600 dark:text-teal-400">{earnedCount} / {totalBadges}</span>
                </div>
                <div className={`w-full ${innerContentBg} rounded-full h-2.5 lg:h-3 overflow-hidden shadow-inner border ${sectionBorder}`}>
                  <div className="bg-gradient-to-r from-purple-500 to-teal-500 h-full rounded-full transition-all duration-1000 shadow-[0_0_8px_rgba(20,184,166,0.5)]" style={{ width: `${completionPercent}%` }} />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 lg:gap-6">
              {Object.entries(allBadges).map(([key, badge]) => {
                const earned = earnedBadges.includes(key);
                const Icon = badge.icon;
                return (
                  <div
                    key={key}
                    // Applied Master Palette to unearned Badges
                    className={`group relative p-5 lg:p-6 rounded-2xl lg:rounded-3xl transition-all duration-300 cursor-pointer active:scale-[0.98] ${earned
                      ? `${badge.bg} border shadow-sm hover:-translate-y-1 hover:shadow-md active:shadow-sm`
                      : `${sectionBg} opacity-70 grayscale border ${sectionBorder}`
                      }`}
                  >
                    <div className="absolute top-4 right-4 lg:top-5 lg:right-5 flex flex-col items-end gap-1.5">
                      {earned && <CheckCircle className="w-4 h-4 lg:w-5 lg:h-5 text-emerald-500 drop-shadow-sm" />}
                      <span className={`text-[9px] lg:text-[10px] uppercase font-bold tracking-widest ${earned ? badge.color : textSecondary}`}>
                        {badge.rarity}
                      </span>
                    </div>

                    {/* Absolute purity using Master Palette variables for BOTH states */}
                    <div className={`inline-flex p-3 lg:p-4 rounded-xl lg:rounded-2xl mb-4 lg:mb-5 shadow-sm border ${innerContentBg} ${earned ? 'border-white/40 dark:border-white/10' : sectionBorder}`}>
                      <Icon className={`w-6 h-6 lg:w-8 lg:h-8 ${earned ? badge.color : textSecondary}`} />
                    </div>

                    <h3 className={`font-poppins font-extrabold text-lg lg:text-xl mb-1.5 tracking-tight ${earned ? textPrimary : textSecondary}`}>
                      {badge.label}
                    </h3>
                    <p className={`text-xs lg:text-sm font-medium line-clamp-2 mb-4 ${earned ? 'text-slate-700 dark:text-gray-300' : textSecondary}`}>
                      {badge.description}
                    </p>

                    {!earned && (
                      <div className={`mt-auto pt-4 border-t border-dashed ${sectionBorder}`}>
                        <p className={`text-[10px] lg:text-[11px] font-bold uppercase tracking-wider flex items-center gap-1.5 ${textSecondary}`}>
                          <Clock className="w-3 h-3 lg:w-3.5 lg:h-3.5" /> {badge.requirement}
                        </p>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {nextBadgeInfo && (
              <div className={`mt-10 lg:mt-12 p-6 lg:p-8 rounded-2xl lg:rounded-3xl bg-gradient-to-r from-purple-50/80 to-teal-50/80 dark:from-purple-900/10 dark:to-teal-900/10 border border-purple-200/50 dark:border-teal-500/20 shadow-inner flex flex-col md:flex-row items-center justify-between gap-6`}>
                <div className="flex items-center gap-4 lg:gap-6 text-center md:text-left">
                  {/*  Applied Layer 3 (innerContentBg) */}
                  <div className={`p-4 lg:p-5 rounded-2xl ${innerContentBg} shadow-sm border border-purple-200/50 dark:border-white/10`}>
                    <NextBadgeIcon className="w-8 h-8 lg:w-10 lg:h-10 text-purple-600 dark:text-teal-400" />
                  </div>
                  <div>
                    <p className="text-[10px] lg:text-xs font-bold uppercase tracking-widest text-purple-600 dark:text-teal-500 mb-1">Closest Target</p>
                    <p className={`font-poppins font-extrabold text-xl lg:text-2xl ${textPrimary}`}>Next Badge: {nextBadgeInfo.label}</p>
                  </div>
                </div>
                {/*  Applied Layer 3 (innerContentBg) */}
                <div className={`${innerContentBg} border border-purple-200/50 dark:border-white/10 px-5 py-3 lg:px-6 lg:py-4 rounded-xl lg:rounded-2xl shadow-sm text-sm lg:text-base font-bold ${textPrimary} flex items-center gap-2`}>
                  <Target className="w-5 h-5 text-rose-500" /> {nextBadgeInfo.requirement}
                </div>
              </div>
            )}
          </div>
        </FadeIn>

        <FadeIn delay={0.5} direction="up" fullWidth className="text-center pt-4 lg:pt-8 pb-4">
          <div className={`inline-flex items-center gap-2 text-xs lg:text-sm font-bold ${sectionBg} border ${sectionBorder} ${textSecondary} px-6 py-3 lg:px-8 lg:py-4 rounded-full shadow-sm`}>
            <Gem className="w-4 h-4 lg:w-5 lg:h-5 text-purple-500 dark:text-teal-400" />
            "Every thought logged and task completed is a step toward your highest self."
            <Gem className="w-4 h-4 lg:w-5 lg:h-5 text-teal-500 dark:text-purple-400" />
          </div>
        </FadeIn>
      </main>
    </div>
  );
}

function StatCard({ icon, title, value, suffix }) {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const innerContentBg = isDarkMode ? 'bg-black/20' : 'bg-slate-50/80';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  return (
    <div className={`p-4 sm:p-5 lg:p-6 rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} flex flex-col sm:flex-row items-center sm:items-start lg:items-center text-center sm:text-left gap-3 sm:gap-4 lg:gap-5 hover:-translate-y-1 hover:shadow-md active:scale-[0.98] transition-all duration-300 cursor-pointer group`}>
      <div className={`p-3 lg:p-4 rounded-xl lg:rounded-2xl ${innerContentBg} shadow-sm border ${sectionBorder} shrink-0 group-hover:scale-110 transition-transform`}>
        {icon}
      </div>
      <div>
        <p className={`text-[10px] lg:text-xs font-bold uppercase tracking-widest mb-1 ${textSecondary}`}>{title}</p>
        <p className={`text-2xl sm:text-3xl lg:text-4xl font-poppins font-black ${textPrimary} flex items-baseline justify-center sm:justify-start gap-1.5 tracking-tight`}>
          {value} <span className={`text-[10px] lg:text-xs font-bold uppercase tracking-wider ${textSecondary}`}>{suffix}</span>
        </p>
      </div>
    </div>
  );
}

export default AchievementsPage;