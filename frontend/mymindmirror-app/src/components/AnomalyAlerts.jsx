import React, { useState, useEffect } from 'react';
import { format } from 'date-fns';
import { useTheme } from '../contexts/ThemeContext';
import { useAnomalies } from '../hooks/useAnomalyData';
import {
  AlertCircle, CheckCircle, ChevronDown, ChevronUp,
  TrendingDown, TrendingUp, Activity, Calendar as CalendarIcon,
  XCircle, Trash2, Sparkles
} from 'lucide-react';
import { SkeletonAnomalyList } from './Skeleton';

const DISMISSED_ANOMALIES_KEY = 'dismissedAnomalies';

function AnomalyAlerts() {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  const [showAllAnomalies, setShowAllAnomalies] = useState(false);
  const [dismissedKeys, setDismissedKeys] = useState(new Set());

//   const { data: anomalies = [], isLoading, isError } = useAnomalies();
// const { data, isLoading, isError } = useAnomalies();
const { data, isLoading, isError, error, refetch } = useAnomalies();

const anomalies = data?.anomalies ?? [];
const insufficientData = data?.insufficientData ?? false;
const daysAvailable = data?.daysAvailable ?? 0;

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  useEffect(() => {
    const stored = localStorage.getItem(DISMISSED_ANOMALIES_KEY);
    if (stored) {
      try {
        setDismissedKeys(new Set(JSON.parse(stored)));
      } catch (e) {
        console.error('Failed to parse dismissed anomalies', e);
      }
    }
  }, []);

  useEffect(() => {
    localStorage.setItem(DISMISSED_ANOMALIES_KEY, JSON.stringify([...dismissedKeys]));
  }, [dismissedKeys]);

  const getAnomalyKey = (anomaly) => `${anomaly.date}-${anomaly.type?.join(',') || 'unknown'}`;

  const dismissAnomaly = (anomaly) => {
    const key = getAnomalyKey(anomaly);
    setDismissedKeys(prev => new Set([...prev, key]));
  };

  const dismissAll = () => {
    const allKeys = anomalies.map(a => getAnomalyKey(a));
    setDismissedKeys(prev => new Set([...prev, ...allKeys]));
  };

  const visibleAnomalies = anomalies.filter(a => !dismissedKeys.has(getAnomalyKey(a)));

  const getSeverityInfo = (message, type) => {
    const lower = message.toLowerCase();
    if (lower.includes('significantly lower') || lower.includes('significantly higher') || (type?.includes('mood') && (message.includes('-0.') || message.includes('+0.')))) {
      return {
        level: 'high', label: 'High Impact',
        bg: 'bg-rose-50 dark:bg-rose-500/10', border: 'border-rose-200 dark:border-rose-500/20',
        text: 'text-rose-800 dark:text-rose-200', icon: 'text-rose-500', tag: 'bg-rose-100 dark:bg-rose-900/30 text-rose-600 dark:text-rose-400'
      };
    }
    if (lower.includes('lower') || lower.includes('higher')) {
      return {
        level: 'medium', label: 'Moderate',
        bg: 'bg-amber-50 dark:bg-amber-500/10', border: 'border-amber-200 dark:border-amber-500/20',
        text: 'text-amber-800 dark:text-amber-200', icon: 'text-amber-500', tag: 'bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400'
      };
    }
    return {
      level: 'low', label: 'Noticeable',
      bg: 'bg-blue-50 dark:bg-blue-500/10', border: 'border-blue-200 dark:border-blue-500/20',
      text: 'text-blue-800 dark:text-blue-200', icon: 'text-blue-500', tag: 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
    };
  };

  const MAX_INITIAL_ANOMALIES = 3;
  const displayedAnomalies = showAllAnomalies ? visibleAnomalies : visibleAnomalies.slice(0, MAX_INITIAL_ANOMALIES);

  const baseCardClasses = `rounded-2xl lg:rounded-3xl border ${cardBg} ${cardBorder} ring-1 ring-black/5 dark:ring-white/5 overflow-hidden transition duration-300`;

   if (isLoading) {
     return <SkeletonAnomalyList />;
   }

if (isError) {
  return (
    <div className={`${baseCardClasses} p-6 lg:p-10 text-center`}>
      <div className="inline-flex p-3 lg:p-4 rounded-full bg-rose-500/10 mb-4 lg:mb-5">
        <AlertCircle className="w-8 h-8 lg:w-10 lg:h-10 text-rose-500" />
      </div>
      <h3 className="text-lg lg:text-xl font-bold text-rose-600 dark:text-rose-400">
        Couldn't analyze your patterns
      </h3>
      <p className={`text-sm lg:text-base ${textSecondary} mt-2 max-w-sm mx-auto`}>
        {error?.response?.data?.message || 'Something went wrong while checking your journal patterns.'}
      </p>
      <button
        onClick={() => refetch()}
        className="mt-5 px-5 py-2 rounded-full text-sm font-bold text-purple-600 dark:text-teal-400 bg-purple-50 dark:bg-teal-900/20 border border-purple-200/50 dark:border-teal-500/20 hover:bg-purple-100 dark:hover:bg-teal-900/40 active:scale-95 transition-all"
      >
        Try again
      </button>
    </div>
  );
}


if (visibleAnomalies.length === 0) {
    // Case 1: Not enough data to analyze
    if (insufficientData) {
        return (
          <div className={`${baseCardClasses} p-6 sm:p-8 lg:p-10 text-center hover:shadow-md hover:-translate-y-0.5`}>
            <div className="relative inline-flex p-4 lg:p-5 rounded-full bg-blue-500/10 mb-4 lg:mb-5">
              <div className="absolute inset-0 bg-blue-400/20 rounded-full blur-xl" />
              <CalendarIcon className="relative w-9 h-9 lg:w-12 lg:h-12 text-blue-500" />
            </div>
            <h3 className="text-xl lg:text-2xl font-poppins font-bold text-blue-600 dark:text-blue-400 mb-2">
              Keep Journaling
            </h3>
            <p className={`${textSecondary} max-w-md mx-auto text-sm lg:text-base`}>
              Anomaly insights appear once you have at least 7 days of entries.
              You have {daysAvailable} {daysAvailable === 1 ? 'day' : 'days'} so far.
            </p>
          </div>
        );
    }

    // Case 2: Real analysis ran, no anomalies detected
    return (
      <div className={`${baseCardClasses} p-6 sm:p-8 lg:p-10 text-center hover:shadow-md hover:-translate-y-0.5`}>
        <div className="relative inline-flex p-4 lg:p-5 rounded-full bg-emerald-500/10 mb-4 lg:mb-5">
          <div className="absolute inset-0 bg-emerald-400/20 rounded-full blur-xl" />
          <CheckCircle className="relative w-9 h-9 lg:w-12 lg:h-12 text-emerald-500" />
        </div>
        <h3 className="text-xl lg:text-2xl font-poppins font-bold text-emerald-600 dark:text-emerald-400 mb-2">
          Patterns Look Steady
        </h3>
        <p className={`${textSecondary} max-w-md mx-auto text-sm lg:text-base`}>
          No unusual emotional shifts detected recently. You're maintaining a great emotional balance!
        </p>
      </div>
    );
}

return (
    <div className={`${baseCardClasses} hover:shadow-md hover:-translate-y-0.5`}>
      <div className="p-4 sm:p-6 lg:p-8">
{/* Header - Flex wrap ensures no squishing on mobile */}
        <div className="flex flex-wrap items-center justify-between mb-5 lg:mb-6 gap-4">
          <div className="flex items-center gap-3 lg:gap-4">
            {/* 🌟 UX UPGRADE: Unified to the Squircle Jewel Icon shape, but kept the urgent Rose gradient! */}
            <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-rose-100 to-rose-50 dark:from-rose-900/40 dark:to-rose-800/20 text-rose-600 dark:text-rose-400 shrink-0 shadow-sm border border-rose-200/50 dark:border-rose-700/30">
              <Activity className="w-5 h-5 lg:w-6 lg:h-6" />
            </div>
            <div>
              {/* 🌟 FIX: Typography matches other cards, but retains the Rose color for Alert attention */}
              <h2 className="text-lg sm:text-xl lg:text-2xl font-poppins font-extrabold text-rose-600 dark:text-rose-400 tracking-tight leading-none">
                Insights & Anomalies
              </h2>
              <p className={`text-[10px] sm:text-[11px] lg:text-xs mt-1 lg:mt-1.5 font-bold uppercase tracking-widest ${textSecondary}`}>
                Significant Shifts
              </p>
            </div>
          </div>
          <button
            onClick={dismissAll}
            className={`flex items-center gap-1.5 px-3 py-1.5 lg:px-4 lg:py-2 rounded-full text-xs lg:text-sm font-semibold ${textSecondary} bg-slate-50 dark:bg-white/5 border border-slate-200/60 dark:border-white/10 hover:bg-slate-100 dark:hover:bg-white/10 active:scale-95 transition duration-200 shadow-sm`}
          >
            <Trash2 className="w-3.5 h-3.5 lg:w-4 lg:h-4" /> Clear All
          </button>
        </div>
        <div className="space-y-3 lg:space-y-4">
          {displayedAnomalies.map((anomaly, index) => {
            const severity = getSeverityInfo(anomaly.message, anomaly.type);
            return (
              <div
                key={index}
                className={`relative p-3.5 sm:p-4 lg:p-5 rounded-xl lg:rounded-2xl border transition duration-300 hover:-translate-y-1 hover:shadow-md lg:hover:shadow-lg ${severity.bg} ${severity.border}`}
              >
                <div className="flex items-start gap-3 sm:gap-4 lg:gap-5">

                  {/* 🌟 FIX: Removed solid bg-white/60 to let severity.bg show through nicely */}
                  <div className="mt-1 shrink-0 bg-white/40 dark:bg-black/20 p-2 lg:p-2.5 rounded-full shadow-sm">
                    {anomaly.type?.includes('mood') ? (
                      <TrendingDown className={`w-4 h-4 lg:w-5 lg:h-5 ${severity.icon}`} />
                    ) : (
                      <Sparkles className={`w-4 h-4 lg:w-5 lg:h-5 ${severity.icon}`} />
                    )}
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center flex-wrap gap-2 lg:gap-3 mb-1.5 lg:mb-2">
                      <div className={`flex items-center gap-1.5 text-[11px] lg:text-xs font-bold ${textSecondary} bg-white/50 dark:bg-black/20 border border-slate-200/60 dark:border-transparent px-2 lg:px-2.5 py-1 rounded-md shadow-sm`}>
                        <CalendarIcon className="w-3 h-3 lg:w-3.5 lg:h-3.5" />
                        {format(new Date(anomaly.date), 'MMM d, yyyy')}
                      </div>
                      <span className={`text-[9px] lg:text-[10px] font-extrabold uppercase tracking-wider px-2 py-1 rounded-md shadow-sm ${severity.tag}`}>
                        {severity.label}
                      </span>
                    </div>
                    <p className={`text-sm lg:text-base font-medium leading-relaxed ${severity.text}`}>
                      {anomaly.message}
                    </p>

                    {anomaly.type && anomaly.type.length > 0 && (
                      <div className="mt-2.5 lg:mt-3 flex flex-wrap gap-1.5 lg:gap-2">
                        {anomaly.type.map(t => (
                          <span key={t} className={`text-[10px] lg:text-xs font-bold px-2.5 py-0.5 lg:py-1 rounded-full bg-white/50 dark:bg-black/20 ${severity.text} border ${severity.border} shadow-sm`}>
                            {t.charAt(0).toUpperCase() + t.slice(1)}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  <button
                    onClick={() => dismissAnomaly(anomaly)}
                    className="shrink-0 text-slate-400 hover:text-slate-700 dark:hover:text-gray-200 active:scale-90 transition-all duration-200 p-1.5 lg:p-2 bg-white/40 dark:bg-black/10 hover:bg-white/80 dark:hover:bg-black/30 rounded-full"
                    title="Dismiss"
                  >
                    <XCircle className="w-4 h-4 lg:w-5 lg:h-5" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {visibleAnomalies.length > MAX_INITIAL_ANOMALIES && (
          <div className={`flex justify-center mt-5 lg:mt-6 pt-4 lg:pt-5 border-t ${isDarkMode ? 'border-white/5' : 'border-slate-200/60'}`}>
            <button
              onClick={() => setShowAllAnomalies(!showAllAnomalies)}
              className={`flex items-center gap-1.5 px-4 py-2 lg:px-5 lg:py-2.5 rounded-full text-xs lg:text-sm font-bold ${textSecondary} bg-slate-50 dark:bg-white/5 border border-slate-200/60 dark:border-white/10 hover:bg-slate-100 dark:hover:bg-white/10 active:scale-95 transition-all duration-200 shadow-sm`}
            >
              {showAllAnomalies ? (
                <>Show Less <ChevronUp className="w-3.5 h-3.5 lg:w-4 lg:h-4" /></>
              ) : (
                <>Show All ({visibleAnomalies.length - MAX_INITIAL_ANOMALIES} more) <ChevronDown className="w-3.5 h-3.5 lg:w-4 lg:h-4" /></>
              )}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default React.memo(AnomalyAlerts);