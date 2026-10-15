// src/components/AchievementsWidget.jsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';
import { useGamificationStats } from '../hooks/useGamification';
import { useTheme } from '../contexts/ThemeContext';
import {
  Flame, Award, Target, Star, Trophy, Sparkles,
  Zap, Crown, BookOpen, CalendarCheck, BrainCircuit, Shield, Eye, Map, Compass, Search, ChevronDown, ChevronUp
} from 'lucide-react';

const badgeIcons = {
  // Task Badges
  FIRST_STEP: { icon: Star, label: 'First Step', color: 'text-yellow-500', bg: 'bg-yellow-50 dark:bg-yellow-500/10', border: 'border-yellow-200 dark:border-yellow-500/20', description: 'Completed your first task' },
  TASK_MASTER: { icon: Target, label: 'Task Master', color: 'text-blue-500', bg: 'bg-blue-50 dark:bg-blue-500/10', border: 'border-blue-200 dark:border-blue-500/20', description: 'Completed 10 tasks' },
  PRODUCTIVITY_NINJA: { icon: Zap, label: 'Productivity Ninja', color: 'text-indigo-500', bg: 'bg-indigo-50 dark:bg-indigo-500/10', border: 'border-indigo-200 dark:border-indigo-500/20', description: 'Crushed 50 tasks' },

  // Journaling Badges
  FIRST_THOUGHT: { icon: BookOpen, label: 'First Thought', color: 'text-teal-500', bg: 'bg-teal-50 dark:bg-teal-500/10', border: 'border-teal-200 dark:border-teal-500/20', description: 'Wrote your first journal entry' },
  REFLECTIVE_SOUL: { icon: BrainCircuit, label: 'Reflective Soul', color: 'text-cyan-500', bg: 'bg-cyan-50 dark:bg-cyan-500/10', border: 'border-cyan-200 dark:border-cyan-500/20', description: 'Logged 5 journal entries' },

  // AI & Schedule Badges
  TIME_LORD: { icon: CalendarCheck, label: 'Time Lord', color: 'text-fuchsia-500', bg: 'bg-fuchsia-50 dark:bg-fuchsia-500/10', border: 'border-fuchsia-200 dark:border-fuchsia-500/20', description: 'Used AI to generate a Smart Timetable' },
  FIRST_CHAT: { icon: Sparkles, label: 'First Chat', color: 'text-pink-500', bg: 'bg-pink-50 dark:bg-pink-500/10', border: 'border-pink-200 dark:border-pink-500/20', description: 'Talked to AI Coach' },
  AI_WHISPERER: { icon: Sparkles, label: 'AI Whisperer', color: 'text-pink-500', bg: 'bg-pink-50 dark:bg-pink-500/10', border: 'border-pink-200 dark:border-pink-500/20', description: 'Talked to AI Coach 20 times' },

  // Advanced Feature Badges
  VISIONARY: { icon: Eye, label: 'Visionary', color: 'text-emerald-500', bg: 'bg-emerald-50 dark:bg-emerald-500/10', border: 'border-emerald-200 dark:border-emerald-500/20', description: 'Created your first Milestone' },
  ARCHITECT: { icon: Map, label: 'Architect', color: 'text-indigo-500', bg: 'bg-indigo-50 dark:bg-indigo-500/10', border: 'border-indigo-200 dark:border-indigo-500/20', description: 'Generated your first AI Roadmap' },
  INTROSPECTIVE: { icon: Compass, label: 'Introspective', color: 'text-rose-500', bg: 'bg-rose-50 dark:bg-rose-500/10', border: 'border-rose-200 dark:border-rose-500/20', description: 'Generated an AI Daily Reflection' },
  DEEP_DIVER: { icon: Search, label: 'Deep Diver', color: 'text-sky-500', bg: 'bg-sky-50 dark:bg-sky-500/10', border: 'border-sky-200 dark:border-sky-500/20', description: 'Used AI to elaborate on a task' },

  // Streak & Final Badges
  THREE_DAY_STREAK: { icon: Flame, label: '3-Day Streak', color: 'text-orange-500', bg: 'bg-orange-50 dark:bg-orange-500/10', border: 'border-orange-200 dark:border-orange-500/20', description: 'Maintained a 3-day streak' },
  SEVEN_DAY_STREAK: { icon: Flame, label: '7-Day Streak', color: 'text-red-500', bg: 'bg-red-50 dark:bg-red-500/10', border: 'border-red-200 dark:border-red-500/20', description: 'Maintained a 7-day streak' },
  THIRTY_DAY_LEGEND: { icon: Crown, label: '30-Day Legend', color: 'text-amber-500', bg: 'bg-amber-50 dark:bg-amber-500/10', border: 'border-amber-200 dark:border-amber-500/20', description: 'Maintained a 30-day streak' },
  ROADMAP_FINISHER: { icon: Trophy, label: 'Roadmap Finisher', color: 'text-purple-500', bg: 'bg-purple-50 dark:bg-purple-500/10', border: 'border-purple-200 dark:border-purple-500/20', description: 'Completed a full roadmap' },
};

const XP_PER_LEVEL = 500;

function AchievementsWidget() {
  const { theme } = useTheme();
  const { data: stats, isLoading, isError } = useGamificationStats();
  const isDarkMode = theme === 'dark';
  const navigate = useNavigate();

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const sectionBg = isDarkMode ? 'bg-[#131127]/80 shadow-inner' : 'bg-slate-50/80 shadow-inner';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  if (isLoading) {
    return (
      <div className={`p-4 sm:p-6 lg:p-8 rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} ring-1 ring-black/5 dark:ring-white/5 animate-pulse`}>
        {/* Header Skeleton */}
        <div className="flex justify-between items-center mb-6 lg:mb-8">
          <div className="flex items-center gap-3 lg:gap-4 w-full max-w-[250px] lg:max-w-[300px]">
            <div className={`w-9 h-9 lg:w-11 lg:h-11 rounded-xl lg:rounded-2xl shrink-0 ${isDarkMode ? 'bg-white/5' : 'bg-gray-200/80'}`} />
            <div className={`h-6 lg:h-8 rounded-full w-full ${isDarkMode ? 'bg-white/5' : 'bg-gray-200/80'}`} />
          </div>
          <div className={`h-8 lg:h-10 rounded-full w-24 lg:w-32 shrink-0 ${isDarkMode ? 'bg-white/5' : 'bg-gray-200/80'}`} />
        </div>

        {/* Level Progress Skeleton */}
        <div className={`mb-6 lg:mb-8 p-4 lg:p-6 rounded-xl lg:rounded-2xl border ${sectionBorder} ${sectionBg} space-y-4`}>
          <div className="flex justify-between items-end">
            <div className={`h-6 lg:h-8 rounded-md w-16 lg:w-20 ${isDarkMode ? 'bg-white/5' : 'bg-gray-200/80'}`} />
            <div className={`h-4 lg:h-5 rounded-full w-24 lg:w-32 ${isDarkMode ? 'bg-white/5' : 'bg-gray-200/80'}`} />
          </div>
          <div className={`w-full h-2 sm:h-2.5 lg:h-3 rounded-full ${isDarkMode ? 'bg-white/5' : 'bg-gray-200/80'}`} />
        </div>

        {/* Badges Grid Skeleton */}
        <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-3 lg:gap-4 mb-2">
          {[...Array(6)].map((_, i) => (
            <div key={i} className={`flex flex-col items-center justify-center gap-2 p-3 lg:p-4 rounded-2xl border ${isDarkMode ? 'border-white/5 bg-white/5' : 'border-gray-100 bg-gray-50/50'}`}>
              <div className={`w-10 h-10 lg:w-12 lg:h-12 rounded-full ${isDarkMode ? 'bg-white/10' : 'bg-gray-200/80'}`} />
              <div className={`h-3 w-3/4 rounded-full mt-1 ${isDarkMode ? 'bg-white/10' : 'bg-gray-200/80'}`} />
              <div className={`h-3 w-1/2 rounded-full ${isDarkMode ? 'bg-white/10' : 'bg-gray-200/80'}`} />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (isError || !stats) return null;

  const badges = stats.badges || [];
  const level = stats.level || 1;
  const totalXp = stats.experiencePoints || 0;

  const displayLimit = 6;
  const visibleBadges = badges.slice(0, displayLimit);
  const currentLevelXP = totalXp % XP_PER_LEVEL;
  const progressPercentage = (currentLevelXP / XP_PER_LEVEL) * 100;

  return (
    <div className={`p-4 sm:p-6 lg:p-8 rounded-2xl lg:rounded-3xl border ${cardBorder} ring-1 ring-black/5 dark:ring-white/5 transition-all duration-300 hover:shadow-md ${cardBg}`}>
      {/* Header */}
      <div className="flex flex-wrap items-start sm:items-center justify-between gap-4 mb-6 lg:mb-8">
        <div className="flex-1 min-w-[150px]">
           <h3 className={`text-lg sm:text-xl lg:text-2xl font-poppins font-extrabold flex items-center gap-2.5 ${textPrimary}`}>
            <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-teal-50 dark:from-teal-900/40 dark:to-purple-900/20 text-purple-600 dark:text-teal-400 border border-purple-200/50 dark:border-teal-700/30 shadow-sm">
                <Award className="w-5 h-5 lg:w-6 lg:h-6" />
            </div>
             Achievements
           </h3>
           <p className={`text-xs lg:text-sm mt-1 lg:mt-1.5 leading-tight ${textSecondary}`}>
             Your journey progress and unlocked badges.
           </p>
        </div>

        {stats.currentStreak > 0 && (
          <div className="flex items-center gap-1.5 lg:gap-2 text-orange-600 dark:text-orange-400 bg-orange-100 dark:bg-orange-500/15 px-3 lg:px-4 py-1.5 lg:py-2 rounded-full shadow-sm border border-orange-200 dark:border-orange-500/30 max-w-max shrink-0">
            <Flame className="animate-bounce w-3.5 h-3.5 sm:w-4 sm:h-4 lg:w-5 lg:h-5" style={{ animationDuration: '2s' }} />
            <span className="text-xs sm:text-sm lg:text-base font-bold font-poppins whitespace-nowrap">{stats.currentStreak} Day Streak!</span>
          </div>
        )}
      </div>

      {/* RPG Leveling Section */}
      <div className={`mb-6 lg:mb-8 p-4 lg:p-6 rounded-xl lg:rounded-2xl border ${sectionBorder} ${sectionBg}`}>
        <div className="flex flex-wrap justify-between items-end gap-2 mb-3 lg:mb-4">
          <div className="flex items-center gap-3 lg:gap-4">
            <div className="bg-gradient-to-br from-purple-500 to-teal-500 text-white text-xs sm:text-sm lg:text-base font-bold px-2.5 lg:px-3.5 py-0.5 lg:py-1 rounded-md shadow-md shrink-0">
              Lv. {level}
            </div>
            <span className={`text-[10px] sm:text-xs lg:text-sm font-semibold uppercase tracking-wider ${textSecondary}`}>
              {totalXp} Total XP
            </span>
          </div>
          <span className="text-[10px] sm:text-xs lg:text-sm font-bold text-purple-600 dark:text-purple-400 tracking-wide whitespace-nowrap">
            {currentLevelXP} / {XP_PER_LEVEL} XP
          </span>
        </div>
        {/* Glowing Progress Bar */}
        <div className="w-full h-2 sm:h-2.5 lg:h-3 bg-gray-200 dark:bg-gray-800 rounded-full overflow-hidden shadow-inner relative">
          <div
            className="h-full bg-gradient-to-r from-purple-500 via-fuchsia-500 to-teal-400 rounded-full transition-all duration-1000 ease-out relative z-10"
            style={{ width: `${progressPercentage}%` }}
          />
          {isDarkMode && (
             <div
               className="absolute top-0 left-0 h-full bg-gradient-to-r from-transparent via-white/30 to-transparent blur-[2px] z-20"
               style={{ width: `${progressPercentage}%` }}
             />
          )}
        </div>
      </div>

      {/* Badges Grid */}
      <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 lg:grid-cols-6 gap-3 lg:gap-4 mb-2">
        {visibleBadges.map((badgeKey) => {
          const badge = badgeIcons[badgeKey];
          if (!badge) return null;
          const Icon = badge.icon;
          return (
            <div
              key={badgeKey}
              onClick={() => {
                  if (window.innerWidth < 1024) {
                      toast.success(badge.label, { description: badge.description });
                  }
              }}
              className={`group relative flex flex-col items-center justify-center gap-2 p-3 lg:p-4 rounded-2xl text-center
                         ${badge.bg} border ${badge.border}
                         hover:-translate-y-1 hover:shadow-md active:scale-95 transition-all duration-300 cursor-pointer lg:cursor-default`}
            >
              {/* Central Icon Container */}
              <div className="w-10 h-10 lg:w-12 lg:h-12 rounded-full bg-white/60 dark:bg-black/20 flex items-center justify-center shadow-sm shrink-0">
                  <Icon className={`${badge.color} w-5 h-5 lg:w-6 lg:h-6`} />
              </div>

              {/* Locked 2-line text height to ensure perfect symmetry */}
              <span className={`text-[10px] lg:text-xs font-bold leading-tight line-clamp-2 h-[28px] lg:h-[32px] flex items-center justify-center w-full ${textPrimary}`}>
                  {badge.label}
              </span>

              {/* Hover Tooltip */}
              <div className="hidden lg:block absolute bottom-full left-1/2 transform -translate-x-1/2 mb-3 px-3 py-2 rounded-lg text-xs whitespace-nowrap bg-gray-900 dark:bg-gray-100 text-white dark:text-gray-900 opacity-0 group-hover:opacity-100 transition duration-200 pointer-events-none z-50 shadow-xl font-medium">
                {badge.description}
                <div className="absolute top-full left-1/2 transform -translate-x-1/2 -mt-1 border-4 border-transparent border-t-gray-900 dark:border-t-gray-100"></div>
              </div>
            </div>
          );
        })}
      </div>

     {/* Full-Width Anchor Button */}
     <div className="mt-4 lg:mt-6 w-full">
        <button
            onClick={() => navigate('/achievements')}
            className={`group w-full flex items-center justify-center gap-2 px-6 py-3.5 lg:py-4 rounded-xl lg:rounded-2xl text-xs lg:text-sm font-bold ${sectionBg} border ${sectionBorder} ${textSecondary} hover:text-purple-600 dark:hover:text-teal-400 hover:border-purple-200/50 dark:hover:border-teal-500/30 active:scale-[0.99] transition-all duration-300 shadow-sm`}
        >
            <Trophy className="w-4 h-4 lg:w-5 lg:h-5 group-hover:scale-110 transition-transform" />
            View Full Trophy Room
        </button>
     </div>
    </div>
  );
}

export default React.memo(AchievementsWidget);