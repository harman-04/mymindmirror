import React from 'react';
import { useTodaysReflection } from '../hooks/useJournalData';
import { RefreshCw, Sparkles, AlertCircle } from 'lucide-react';
import { useTheme } from '../contexts/ThemeContext';
import { SkeletonReflection, SkeletonText } from './Skeleton';

function TodaysReflection({ todayEntries, isLoading }) {
  const {
    data: reflection,
    isLoading: isGenerating,
    isError,
    error,
    refresh,
    isRefetching,
  } = useTodaysReflection(todayEntries);

  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  const handleRefresh = () => refresh();

  if (isLoading) {
      return <SkeletonReflection />;
  }

  // No entries state
  if (!todayEntries || todayEntries.length === 0) {
    return (
      <div className={`p-5 sm:p-6 lg:p-8 rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} transition-shadow duration-300 flex flex-col h-full`}>
       <div className="flex items-center gap-3 lg:gap-4 mb-4 lg:mb-6">
           {/* 🌟 RESTORED: The Beautiful Gradient Jewel Icon */}
           <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-teal-900/40 dark:to-teal-800/20 text-purple-600 dark:text-teal-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-teal-700/30">
               <Sparkles className="w-5 h-5 lg:w-6 lg:h-6" />
           </div>
           <h3 className={`text-xl lg:text-2xl font-poppins font-extrabold ${textPrimary} tracking-tight`}>
             Today's Reflection
           </h3>
       </div>
        <div className="flex-grow flex flex-col justify-center items-center text-center">
          <p className={`font-playfair italic text-base lg:text-lg ${textSecondary} max-w-[80%]`}>
            "Log an entry today to generate your personalized AI reflection."
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className={`p-5 sm:p-6 lg:p-8 rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} transition-shadow hover:shadow-lg duration-300 flex flex-col h-full`}>
      {/* Header */}
      <div className="flex justify-between items-start mb-4 lg:mb-6 gap-4">
         <div className="flex items-center gap-3 lg:gap-4 flex-1 min-w-[200px]">
             {/* 🌟 RESTORED: The Beautiful Gradient Jewel Icon */}
             <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-teal-900/40 dark:to-teal-800/20 text-purple-600 dark:text-teal-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-teal-700/30">
                 <Sparkles className="w-5 h-5 lg:w-6 lg:h-6" />
             </div>
             <h3 className={`text-xl lg:text-2xl font-poppins font-extrabold ${textPrimary} tracking-tight`}>
               Today's Reflection
             </h3>
         </div>

        <button
          onClick={handleRefresh}
          disabled={isGenerating || isRefetching}
          className={`shrink-0 p-2 lg:p-2.5 rounded-full transition-all duration-200 bg-slate-50 hover:bg-slate-100 dark:bg-white/5 dark:hover:bg-white/10 border border-slate-200/80 dark:border-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500 disabled:opacity-50 shadow-sm active:scale-95`}
          title="Regenerate reflection"
        >
          <RefreshCw
            className={`w-4 h-4 lg:w-5 lg:h-5 ${isGenerating || isRefetching ? `animate-spin text-purple-600 dark:text-teal-400` : textSecondary}`}
          />
        </button>
      </div>

      {/* Body Content - flex-grow ensures perfect vertical centering */}
      <div className="flex-grow flex flex-col justify-center w-full">
        {(isGenerating || isRefetching) ? (
             <div className="w-full px-2 lg:px-4">
                 <SkeletonText lines={4} />
             </div>
        ) : isError ? (
          <div className="space-y-3 bg-rose-50 dark:bg-rose-500/10 p-4 rounded-xl border border-rose-200/60 dark:border-rose-500/20 shadow-sm">
            <div className="flex items-center gap-2 text-rose-500">
              <AlertCircle size={18} className="shrink-0" />
              <p className="font-inter text-sm lg:text-base font-medium text-rose-700 dark:text-rose-300">
                {error?.message || 'Failed to generate reflection.'}
              </p>
            </div>
            <p className={`text-xs lg:text-sm ${textSecondary} pl-6`}>
              This usually happens when the AI service is busy,  Gemini quota exceeded or when your Gemini API key isn't set up.
            </p>
            <button
              onClick={handleRefresh}
              className="mt-2 text-sm font-semibold text-purple-600 hover:text-purple-700 dark:text-purple-400 dark:hover:text-purple-300 transition-colors flex items-center gap-1.5 pl-6"
            >
              <RefreshCw size={14} /> Try generating again
            </button>
          </div>
        ) : (
          /* Decorative Quote Text */
          <div className="relative px-4 lg:px-6 py-2">
            <div className={`absolute -top-4 -left-1 lg:-left-2 text-5xl lg:text-6xl ${isDarkMode ? 'text-teal-400/15' : 'text-purple-500/10'} font-serif leading-none select-none`}>“</div>
            <p className={`font-playfair italic text-base sm:text-lg lg:text-xl ${textPrimary} leading-relaxed lg:leading-loose text-center sm:text-left relative z-10`}>
              {reflection}
            </p>
            <div className={`absolute -bottom-6 -right-1 lg:-right-2 text-5xl lg:text-6xl ${isDarkMode ? 'text-teal-400/15' : 'text-purple-500/10'} font-serif leading-none select-none`}>”</div>
          </div>
        )}
      </div>
    </div>
  );
}

export default React.memo(TodaysReflection);