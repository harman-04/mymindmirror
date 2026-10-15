import React, { useState, lazy, Suspense } from 'react';
import { FileText, X, Loader2 } from 'lucide-react';
import { useTheme } from '../contexts/ThemeContext';
import MoodChart from './MoodChart';
import AverageEmotionChart from './AverageEmotionChart';
import MoodCalendarHeatmap from './MoodCalendarHeatmap';
import ConcernFrequencyChart from './ConcernFrequencyChart';
import JournalHistory from './JournalHistory';
import KeyPhraseCloud from './KeyPhraseCloud';
import MoodWordCountChart from './MoodWordCountChart';
import InfiniteScrollTrigger from './InfiniteScrollTrigger';
import EmotionRadarChart from './EmotionRadarChart';
import { SkeletonCard } from './Skeleton';


const ExportButtons = lazy(() => import('./ExportButtons'));

function OverallDashboard({
    journalEntries,
    isLoading,
    userId,
    loadMore,
    hasNextPage,
    isFetchingNextPage,
    totalEntries,
}) {
    const [filterPhrase, setFilterPhrase] = useState(null);
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

    const handlePhraseClick = (phrase) => setFilterPhrase(phrase);
    const clearFilter = () => setFilterPhrase(null);

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 sm:gap-8 w-full items-stretch">

            {/* 1. Mood & Emotion Trends (Full Width) */}
            <div className="col-span-1 lg:col-span-2 w-full block">
                <MoodChart entries={journalEntries} isLoading={isLoading} />
            </div>

            {/* 2. Mood Calendar Heatmap (Full Width) */}
            <div className="col-span-1 lg:col-span-2 w-full block">
                <MoodCalendarHeatmap journalEntries={journalEntries} displayYear={new Date()} isLoading={isLoading}/>
            </div>

            {/* 3. Emotional Profile Radar (Full Width) */}
            <div className="col-span-1 lg:col-span-2 w-full block">
                <EmotionRadarChart entries={journalEntries} isLoading={isLoading} />
            </div>

            {/* 4. Average Emotion Intensity (Half Width on Desktop) */}
            <div className="col-span-1 w-full block">
                <AverageEmotionChart entries={journalEntries} isLoading={isLoading} />
            </div>

            {/* 5. Most Frequent Concerns (Half Width on Desktop) */}
            <div className="col-span-1 w-full block">
                <ConcernFrequencyChart entries={journalEntries} isLoading={isLoading} />
            </div>

            {/* 6. Mood vs Word Count Correlation (Full Width) */}
            <div className="col-span-1 lg:col-span-2 w-full block">
                <MoodWordCountChart entries={journalEntries} isLoading={isLoading} />
            </div>

            {/* 7. Key Phrase Cloud (Full Width) */}
            <div className="col-span-1 lg:col-span-2 w-full block">
                <KeyPhraseCloud onWordClick={handlePhraseClick} isLoading={isLoading} />

                {/* Smoothly animated Filter Button */}
                {filterPhrase && (
                    <div className="mt-4 lg:mt-6 flex justify-center animate-in fade-in duration-300 px-4">
                        <button
                            onClick={clearFilter}
                            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full text-sm lg:text-base font-bold bg-purple-100 dark:bg-purple-500/20 text-purple-700 dark:text-purple-300 hover:bg-purple-200 dark:hover:bg-purple-500/30 transition-all shadow-sm hover:shadow-md active:scale-95 border border-purple-200 dark:border-purple-500/30"
                        >
                            <X className="w-4 h-4 lg:w-5 lg:h-5" />
                            <span>Clear Filter: <span className="italic">"{filterPhrase}"</span></span>
                        </button>
                    </div>
                )}
            </div>

            {/* 8. All Journal Entries (Full Width Layout) */}
            <div className={`col-span-1 lg:col-span-2 rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-lg ring-1 ring-black/5 dark:ring-white/5 ${cardBg} p-3 sm:p-6 lg:p-8 mt-2 transition duration-300`}>
                <div className={`flex flex-wrap justify-between items-center gap-4 mb-6 lg:mb-8 border-b ${sectionBorder} pb-4 lg:pb-6`}>
                    <div className="flex items-center gap-3 lg:gap-4">
                      <div className="p-2 lg:p-3 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-teal-900/40 dark:to-teal-800/20 text-purple-600 dark:text-teal-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-teal-700/30">
                                                  <FileText className="w-5 h-5 lg:w-6 lg:h-6" />
                                              </div>
                        <h3 className={`text-xl lg:text-2xl font-poppins font-extrabold ${textPrimary} tracking-tight`}>
                            Journal History
                        </h3>
                    </div>

<div className="flex flex-wrap items-center gap-2 sm:gap-3 lg:gap-5 w-full sm:w-auto justify-start sm:justify-end mt-1 sm:mt-0">

                        {/* 🌟 SUSPENSE BOUNDARY: Shows a smooth loader while the 1MB export libraries download */}
                        <Suspense fallback={
                            <div className="flex items-center justify-center gap-2 px-4 py-2 lg:px-5 lg:py-2.5 rounded-xl border border-transparent min-w-[170px] lg:min-w-[210px] h-[38px] sm:h-[42px] lg:h-[46px]">
                                <Loader2 className="w-4 h-4 lg:w-5 lg:h-5 animate-spin text-purple-500 dark:text-teal-400" />
                                <span className="text-xs lg:text-sm font-semibold text-slate-500 dark:text-gray-400">Loading tools...</span>
                            </div>
                        }>
                            <ExportButtons />
                        </Suspense>

                        {totalEntries > 0 && (
                            <div className="flex items-center gap-2 px-3 sm:px-4 py-2 sm:py-2.5 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 rounded-xl lg:rounded-2xl shadow-sm text-emerald-700 dark:text-emerald-300 h-[38px] sm:h-[42px] lg:h-[46px]">
                                <span className="text-[10px] lg:text-xs font-bold uppercase tracking-wider whitespace-nowrap">
                                    {journalEntries.length} of {totalEntries} Entries
                                </span>
                            </div>
                        )}
                    </div>
                </div>

                <div className="w-full">
                    <JournalHistory
                        entries={journalEntries}
                        filterPhrase={filterPhrase}
                        isLoading={isLoading}
                    />
                </div>

                <div className="mt-8 lg:mt-10">
                    <InfiniteScrollTrigger
                        onIntersect={loadMore}
                        isLoading={isFetchingNextPage}
                        hasNextPage={hasNextPage}
                    />
                </div>
            </div>
        </div>
    );
}

export default React.memo(OverallDashboard);