// src/components/WeeklyDashboard.jsx
import React from 'react';
import { format } from 'date-fns';
import { useTheme } from '../contexts/ThemeContext';
import MoodChart from './MoodChart';
import AverageEmotionChart from './AverageEmotionChart';
import ConcernFrequencyChart from './ConcernFrequencyChart';
import JournalHistory from './JournalHistory';
import { SkeletonCard } from './Skeleton';
import { FileText, Calendar as CalendarIcon } from 'lucide-react';

function WeeklyDashboard({
    weeklyEntries,
    userId,
    startOfCurrentWeek,
    endOfCurrentWeek,
    isLoading,
}) {
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

    const dateRangeText = `${format(startOfCurrentWeek, 'MMM d')} - ${format(endOfCurrentWeek, 'MMM d')}`;

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 sm:gap-8 w-full items-stretch">

            {/* 1. Mood & Emotion Trends (Full Width) */}
            <div className="col-span-1 lg:col-span-2 w-full block">
                <MoodChart entries={weeklyEntries} isLoading={isLoading} />
            </div>

            {/* 2. Average Emotion Intensity (Half Width on Desktop) */}
            <div className="col-span-1 w-full block">
                <AverageEmotionChart entries={weeklyEntries} isLoading={isLoading} />
            </div>

            {/* 3. Most Frequent Concerns (Half Width on Desktop) */}
            <div className="col-span-1 w-full block">
                <ConcernFrequencyChart entries={weeklyEntries} isLoading={isLoading} />
            </div>

            {/* 4. All Weekly Entries (Full Width) */}
            <div className={`col-span-1 lg:col-span-2 rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-lg ring-1 ring-black/5 dark:ring-white/5 ${cardBg} p-3 sm:p-6 lg:p-8 mt-2 transition duration-300`}>
                <div className={`flex flex-wrap justify-between items-center gap-4 mb-6 lg:mb-8 border-b ${sectionBorder} pb-4 lg:pb-6`}>
                    <div className="flex items-center gap-3 lg:gap-4">
                <div className="p-2 lg:p-3 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-teal-900/40 dark:to-teal-800/20 text-purple-600 dark:text-teal-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-teal-700/30">
                                            <FileText className="w-5 h-5 lg:w-6 lg:h-6" />
                                        </div>
                        <h3 className={`text-xl lg:text-2xl font-poppins font-extrabold ${textPrimary} tracking-tight`}>
                            Weekly Journal History
                        </h3>
                    </div>

                    <div className="flex flex-wrap items-center gap-2 lg:gap-3">
                         {/* Date Range Badge */}
                         <div className="flex items-center gap-1.5 px-3 py-1.5 lg:px-4 lg:py-2 bg-purple-50 dark:bg-purple-500/10 border border-purple-200 dark:border-purple-500/20 rounded-full shadow-sm text-purple-700 dark:text-purple-300">
                             <CalendarIcon className="w-3 h-3 lg:w-4 lg:h-4" />
                             <span className="text-[10px] lg:text-xs font-bold uppercase tracking-wider whitespace-nowrap">
                                 {dateRangeText}
                             </span>
                         </div>

                        {/* Entry Count Badge */}
                        {weeklyEntries.length > 0 && (
                            <div className="flex items-center gap-2 px-3 py-1.5 lg:px-4 lg:py-2 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 rounded-full shadow-sm text-emerald-700 dark:text-emerald-300">
                                <span className="text-[10px] lg:text-xs font-bold uppercase tracking-wider whitespace-nowrap">
                                    {weeklyEntries.length} Entries
                                </span>
                            </div>
                        )}
                    </div>
                </div>

                <div className="w-full block">
                    <JournalHistory
                        entries={weeklyEntries}
                        isLoading={isLoading}
                    />
                </div>
            </div>

        </div>
    );
}

export default React.memo(WeeklyDashboard);