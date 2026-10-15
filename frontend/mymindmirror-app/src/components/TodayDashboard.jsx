// src/components/TodayDashboard.jsx
import React from 'react';
import TodaysReflection from './TodaysReflection';
import DailyEmotionSnapshot from './DailyEmotionSnapshot';
import JournalHistory from './JournalHistory';
import { SkeletonCard } from './Skeleton';
import { useTheme } from '../contexts/ThemeContext';
import { Calendar, Sparkles } from 'lucide-react';

function TodayDashboard({ todayEntries, isLoading }) {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const sectionBg = isDarkMode ? 'bg-[#131127]/80 shadow-inner' : 'bg-slate-50/80 shadow-inner';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  const baseCardClasses = `rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} shadow-lg ring-1 ring-black/5 dark:ring-white/5 transition-shadow duration-300 hover:shadow-xl`;

  // 🚀 Shared Header Component (Fully Responsive & Native Badge Support)
  const DashboardHeader = ({ icon: Icon, title, colorClass, badgeText }) => (
      <div className={`flex flex-wrap items-center justify-between gap-3 mb-5 lg:mb-6 border-b ${sectionBorder} pb-4 lg:pb-5`}>
          <div className="flex items-center gap-3 lg:gap-4">
<div className="p-2 lg:p-3 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-teal-900/40 dark:to-teal-800/20 text-purple-600 dark:text-teal-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-teal-700/30">                  <Icon className="w-5 h-5 lg:w-6 lg:h-6" />
              </div>
              <h3 className={`text-lg lg:text-xl font-poppins font-extrabold ${textPrimary} tracking-tight`}>
                  {title}
              </h3>
          </div>
          {badgeText && (
              <div className="flex items-center gap-1.5 px-3 lg:px-4 py-1.5 lg:py-2 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 rounded-full shadow-sm text-emerald-700 dark:text-emerald-300">
                  <span className="text-[10px] lg:text-xs font-bold uppercase tracking-wider whitespace-nowrap">
                      {badgeText}
                  </span>
              </div>
          )}
      </div>
  );

  return (
    <div className="space-y-4 sm:space-y-6 lg:space-y-8 w-full">
      {/* Top Row: Reflection & Snapshot - Flex-grow enforces equal height stretching */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6 lg:gap-8 w-full items-stretch">
        <div className="col-span-1 w-full flex flex-col [&>div]:h-full">
           <TodaysReflection todayEntries={todayEntries} isLoading={isLoading} />
        </div>
        <div className="col-span-1 w-full flex flex-col [&>div]:h-full">
           <DailyEmotionSnapshot todayEntries={todayEntries} isLoading={isLoading} />
        </div>
      </div>

      {/* Bottom Section: Today's History */}
      { isLoading || todayEntries.length > 0 ? (
        <div className={`p-3 sm:p-6 lg:p-8 w-full flex flex-col ${baseCardClasses}`}>
          <DashboardHeader
            icon={Calendar}
            title="Today's Journal History"
            colorClass="text-emerald-500"
            badgeText={`${todayEntries.length} ${todayEntries.length === 1 ? 'Entry' : 'Entries'} Today`}
          />
          <div className="w-full">
             <JournalHistory entries={todayEntries} isLoading={isLoading} />
          </div>
        </div>
      ) : (
        <div className={`p-6 sm:p-8 lg:p-12 w-full flex flex-col items-center justify-center text-center min-h-[250px] sm:min-h-[280px] lg:min-h-[320px] ${baseCardClasses}`}>
          <div className="relative inline-flex p-4 lg:p-5 rounded-full bg-purple-500/10 mb-4 lg:mb-6">
              <div className="absolute inset-0 bg-purple-400/20 rounded-full blur-xl" />
              <Sparkles className="relative w-8 h-8 lg:w-12 lg:h-12 text-purple-500" />
          </div>
          <h3 className={`text-lg sm:text-xl lg:text-2xl font-poppins font-extrabold ${textPrimary} mb-2 tracking-tight`}>
            Your Canvas is Blank
          </h3>
          <p className={`font-inter text-sm lg:text-base ${textSecondary} max-w-sm mx-auto leading-relaxed`}>
            No entries recorded for today yet. Use the prompt above or just start typing to capture your thoughts!
          </p>
        </div>
      )}
    </div>
  );
}

export default React.memo(TodayDashboard);