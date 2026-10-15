// src/components/MoodCalendarHeatmap.jsx
import React, { useRef, useMemo, useState, useEffect } from 'react';
import CalendarHeatmap from 'react-calendar-heatmap';
import {
  format,
  parseISO,
  startOfYear,
  endOfYear,
  subDays,
} from 'date-fns';
import { useTheme } from '../contexts/ThemeContext';
import DownloadChartButton from './DownloadChartButton';
import { Calendar, CalendarDays } from 'lucide-react';
import { SkeletonHeatmap } from './Skeleton';

const MoodCalendarHeatmap = ({ journalEntries, displayYear = new Date(),  isLoading }) => {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';
  const chartContainerRef = useRef(null);

  const [isMobile, setIsMobile] = useState(window.innerWidth < 1024);
  const [selectedDateInfo, setSelectedDateInfo] = useState(null);

  useEffect(() => {
      const handleResize = () => setIsMobile(window.innerWidth < 1024);
      window.addEventListener('resize', handleResize);
      return () => window.removeEventListener('resize', handleResize);
  }, []);

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const sectionBg = isDarkMode ? 'bg-[#131127]/80' : 'bg-slate-50/80';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  const moodColors = {
    light: {
      veryNegative: '#EF4444',
      negative: '#F97316',
      neutral: '#9CA3AF',
      positive: '#10B981',
      veryPositive: '#3B82F6',
      empty: '#E5E7EB',
      text: '#1F2937',
      secondaryText: '#6B7280',
      stroke: '#D1D5DB'
    },
    dark: {
      veryNegative: '#DC2626',
      negative: '#EA580C',
      neutral: '#6B7280',
      positive: '#059669',
      veryPositive: '#2563EB',
      empty: 'rgba(255, 255, 255, 0.05)',
      text: '#F3F4F6',
      secondaryText: '#9CA3AF',
      stroke: 'rgba(255, 255, 255, 0.1)'
    },
  };

  const currentColors = isDarkMode ? moodColors.dark : moodColors.light;

  const dailyMoods = useMemo(() => {
    const map = {};
    if (journalEntries && Array.isArray(journalEntries)) {
      journalEntries.forEach((entry) => {
        if (entry.moodScore !== null && entry.moodScore !== undefined) {
          const dateKey = format(parseISO(entry.entryDate), 'yyyy-MM-dd');
          if (!map[dateKey]) map[dateKey] = { sum: 0, count: 0 };
          map[dateKey].sum += entry.moodScore;
          map[dateKey].count++;
        }
      });
    }
    return map;
  }, [journalEntries]);

  const values = useMemo(() => {
    return Object.keys(dailyMoods).map((dateKey) => {
      const { sum, count } = dailyMoods[dateKey];
      return {
        date: dateKey,
        count: count > 0 ? sum / count : null,
        totalEntries: count
      };
    });
  }, [dailyMoods]);

  const getClassForValue = (value) => {
    if (!value || value.count === null) return 'color-empty';
    const mood = value.count;
    if (mood >= 0.7) return 'color-very-positive';
    if (mood >= 0.3) return 'color-positive';
    if (mood > -0.3) return 'color-neutral';
    if (mood > -0.7) return 'color-negative';
    return 'color-very-negative';
  };

  const getTitleForValue = (value) => {
    if (!value || !value.date) return 'No entry';
    try {
      const dateObj = typeof value.date === 'string' ? parseISO(value.date) : value.date;
      const formattedDate = format(dateObj, 'MMM d, yyyy');

      if (value.count === null || value.count === undefined) {
        return `${formattedDate} • No entries`;
      }
      const entryText = value.totalEntries === 1 ? '1 Entry' : `${value.totalEntries} Entries`;
      const moodPrefix = value.totalEntries === 1 ? 'Mood' : 'Avg Mood';
      return `${formattedDate} • ${entryText} • ${moodPrefix}: ${value.count.toFixed(2)}`;
    } catch (e) {
      return 'No entry';
    }
  };

  const handleBoxClick = (value) => {
    if (!value || !value.date) {
      setSelectedDateInfo('No entry on selected date');
      return;
    }
    setSelectedDateInfo(getTitleForValue(value));
  };

  const startDate = subDays(startOfYear(displayYear), 1);
  const endDate = endOfYear(displayYear);
  const weekdayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const monthLabels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  if (isLoading) return <SkeletonHeatmap/>;

  const renderHeader = () => (
    <div className={`p-4 lg:p-6 border-b ${sectionBorder} ${sectionBg} flex justify-between items-start gap-4`}>
        <div className="flex items-start gap-3 sm:gap-4 flex-1">
            {/* 🌟 RESTORED: Emerald Jewel Icon */}
            <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-emerald-100 to-teal-50 dark:from-emerald-900/40 dark:to-teal-800/20 text-emerald-600 dark:text-emerald-400 shrink-0 shadow-sm border border-emerald-200/50 dark:border-emerald-700/30">
                <Calendar className="w-5 h-5 lg:w-6 lg:h-6" />
            </div>
            <div className="flex flex-col">
                <h3 className={`text-lg lg:text-xl font-poppins font-extrabold ${textPrimary} tracking-tight leading-tight`}>
                    Annual Mood<br className="sm:hidden" /> Heatmap
                </h3>
                <p className={`text-[11px] lg:text-xs mt-1 lg:mt-1.5 font-medium ${textSecondary}`}>
                    {isMobile ? "Swipe horizontally to view the full year." : "Scroll horizontally to view the full year. Hover to see dates."}
                </p>
            </div>
        </div>

        <div className="flex items-center gap-2 sm:gap-4 shrink-0 mt-0.5">
            <span className={`text-sm lg:text-base font-bold ${textSecondary} font-poppins`}>
                ({format(displayYear, 'yyyy')})
            </span>
            <DownloadChartButton
                chartRef={chartContainerRef}
                filename={`mood_heatmap_${format(displayYear, 'yyyy')}`}
                darkMode={isDarkMode}
                className={`hover:scale-105 transition-transform shrink-0 ${values.length === 0 ? 'opacity-50 pointer-events-none' : ''}`}
            />
        </div>
    </div>
  );

  if (values.length === 0) {
    return (
        <div className={`w-full rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-sm ring-1 ring-black/5 dark:ring-white/5 overflow-hidden ${cardBg} flex flex-col h-full`}>
          {renderHeader()}
          {/* 🌟 FIX: Removed hardcoded background color to let cardBg show through */}
          <div className="w-full flex-grow flex flex-col items-center justify-center p-6 lg:p-10 text-center min-h-[260px]">
             <div className="relative mb-4 lg:mb-6">
                <div className="absolute inset-0 bg-gradient-to-r from-purple-400/20 to-teal-400/20 rounded-full blur-2xl" />
                <CalendarDays className={`w-12 h-12 lg:w-16 lg:h-16 relative ${textSecondary}`} />
              </div>
              <p className={`text-lg lg:text-xl font-bold ${textPrimary}`}>
                No journal entries for {format(displayYear, 'yyyy')}
              </p>
              <p className={`text-sm lg:text-base text-center max-w-md mt-2 ${textSecondary}`}>
                Start journaling to see your mood patterns throughout the year.
              </p>
          </div>
      </div>
    );
  }

  return (
      <div className={`w-full rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-sm ring-1 ring-black/5 dark:ring-white/5 overflow-hidden ${cardBg} h-full flex flex-col transition-all duration-300 hover:shadow-md hover:-translate-y-0.5`}>

      {renderHeader()}

      {/* 🌟 FIX: Removed hardcoded background color */}
      <div
        ref={chartContainerRef}
        className="w-full flex-grow overflow-x-auto custom-scrollbar flex flex-col"
      >
        <style>
          {`
            .react-calendar-heatmap text {
              font-family: 'Inter', sans-serif;
              font-size: ${isMobile ? '10px' : '11px'};
              fill: ${currentColors.secondaryText};
            }
            .react-calendar-heatmap .month-label {
              font-weight: 600;
              font-size: ${isMobile ? '11px' : '12px'};
              fill: ${currentColors.text};
              text-anchor: start;
              transform: translateX(4px);
            }
            .react-calendar-heatmap .weekday-label {
              font-weight: 500;
              font-size: ${isMobile ? '10px' : '11px'};
              fill: ${currentColors.secondaryText};
              text-anchor: end;
              dominant-baseline: middle;
              transform: translateX(-6px);
            }
            .react-calendar-heatmap .day {
              shape-rendering: crispEdges;
              stroke: ${currentColors.stroke};
              stroke-width: 1px;
              rx: 2;
              ry: 2;
              transition: all 0.2s ease-in-out;
            }
            .react-calendar-heatmap .color-empty {
              fill: ${currentColors.empty};
              stroke-width: 1px;
            }
            .react-calendar-heatmap .color-very-negative { fill: ${currentColors.veryNegative}; }
            .react-calendar-heatmap .color-negative { fill: ${currentColors.negative}; }
            .react-calendar-heatmap .color-neutral { fill: ${currentColors.neutral}; }
            .react-calendar-heatmap .color-positive { fill: ${currentColors.positive}; }
            .react-calendar-heatmap .color-very-positive { fill: ${currentColors.veryPositive}; }

            .react-calendar-heatmap .day:hover {
              stroke-width: 2px;
              stroke: ${isDarkMode ? '#F3F4F6' : '#1F2937'};
              filter: brightness(1.2);
              transform: scale(1.15);
              transform-origin: center;
              cursor: pointer;
            }
          `}
        </style>

        <div className="p-4 sm:p-6 lg:p-8 pb-2 min-w-[700px] lg:min-w-[900px] flex-grow flex items-center justify-center">
          <CalendarHeatmap
            startDate={startDate}
            endDate={endDate}
            values={values}
            classForValue={getClassForValue}
            titleForValue={getTitleForValue}
            onClick={handleBoxClick}
            showWeekdayLabels={true}
            showMonthLabels={true}
            gutterSize={isMobile ? 2 : 3}
            weekdayLabels={weekdayLabels}
            monthLabels={monthLabels}
          />
        </div>

        {/* Selected Date Info Badge */}
        <div className="min-h-[28px] flex justify-center items-center w-full min-w-[700px] lg:min-w-[900px] mb-4">
            {selectedDateInfo ? (
                <span className={`inline-block px-4 py-1.5 ${sectionBg} rounded-full text-xs font-bold ${textPrimary} shadow-sm border ${sectionBorder} animate-fade-in transition-all`}>
                    {selectedDateInfo}
                </span>
            ) : (
                <span className={`text-xs font-medium ${textSecondary} italic`}>
                    Tap a square to view exact details
                </span>
            )}
        </div>

        {/* Color Legend */}
        <div className={`flex flex-wrap justify-center items-center pb-6 lg:pb-8 px-4 gap-3 lg:gap-4 text-xs lg:text-sm font-inter ${textPrimary} min-w-[700px] lg:min-w-[900px]`}>
          <span className={`font-bold tracking-wide uppercase text-[10px] lg:text-xs ${textSecondary} mr-2`}>Mood Scale:</span>
          {[
            { color: currentColors.veryNegative, label: 'Very Negative' },
            { color: currentColors.negative, label: 'Negative' },
            { color: currentColors.neutral, label: 'Neutral' },
            { color: currentColors.positive, label: 'Positive' },
            { color: currentColors.veryPositive, label: 'Very Positive' },
          ].map((item, idx) => (
            <div key={idx} className="flex items-center gap-1.5 lg:gap-2">
              <div
                className="w-3.5 h-3.5 lg:w-4 lg:h-4 rounded-sm shadow-sm border"
                style={{ backgroundColor: item.color, borderColor: currentColors.stroke }}
              />
              <span className="font-medium whitespace-nowrap">{item.label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default React.memo(MoodCalendarHeatmap);