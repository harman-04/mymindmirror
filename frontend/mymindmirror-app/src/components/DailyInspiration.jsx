// src/components/DailyInspiration.jsx
import React, { useState, useEffect } from 'react';
import { WandSparkles, RefreshCw } from 'lucide-react';
import { getRandomPrompt } from '../data/writingPrompts';
import { useTheme } from '../contexts/ThemeContext';

const STORAGE_KEY = 'usedPrompts';

function DailyInspiration() {
  const [prompt, setPrompt] = useState('');
  const [usedPrompts, setUsedPrompts] = useState([]);
  const [isAnimating, setIsAnimating] = useState(false);
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        setUsedPrompts(JSON.parse(stored));
      } catch (e) {
        console.error("Failed to parse usedPrompts", e);
      }
    }
  }, []);

  const generateNewPrompt = () => {
    setIsAnimating(true);
    setTimeout(() => {
      const newPrompt = getRandomPrompt(usedPrompts);
      setPrompt(newPrompt);

      const updated = [newPrompt, ...usedPrompts].slice(0, 10);
      setUsedPrompts(updated);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));

      setIsAnimating(false);
    }, 300);
  };

  useEffect(() => {
    if (!prompt) {
      generateNewPrompt();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className={`p-4 sm:p-5 lg:p-6 rounded-2xl lg:rounded-3xl border ${cardBorder} ${cardBg} ring-1 ring-black/5 dark:ring-white/5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 lg:gap-6 transition-all duration-300 hover:shadow-md hover:-translate-y-0.5`}>
      {/* Icon & Text Section */}
      <div className="flex gap-3 lg:gap-4 items-start sm:items-center w-full">
        {/* 🌟 RESTORED: Purple/Teal Jewel Icon */}
        <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-teal-900/40 dark:to-teal-800/20 text-purple-600 dark:text-teal-400 shrink-0 mt-1 sm:mt-0 shadow-sm border border-purple-200/50 dark:border-teal-700/30">
          <WandSparkles className="w-5 h-5 lg:w-6 lg:h-6" />
        </div>

        <div className="flex-grow">
          <h3 className={`text-[10px] sm:text-[11px] lg:text-xs font-bold uppercase tracking-widest ${textSecondary} mb-1 lg:mb-1.5 font-poppins`}>
            Daily Inspiration
          </h3>
          <p className={`text-sm sm:text-base lg:text-lg ${textPrimary} font-medium italic font-playfair leading-relaxed transition-all duration-300 ${isAnimating ? 'opacity-0 translate-y-1' : 'opacity-100 translate-y-0'}`}>
            "{prompt || "Finding inspiration..."}"
          </p>
        </div>
      </div>

      {/* Action Buttons Section */}
      {/* 🌟 FIX: Applied standard border color for consistent division */}
      <div className={`flex items-center justify-end gap-2 lg:gap-3 self-end sm:self-auto shrink-0 w-full sm:w-auto mt-1 sm:mt-0 pt-3 sm:pt-0 border-t ${isDarkMode ? 'border-white/5' : 'border-slate-200/60'} sm:border-t-0`}>
        <button
          onClick={generateNewPrompt}
          disabled={isAnimating}
          // 🌟 FIX: Matches DownloadChartButton inset styling!
          className="p-2 lg:p-2.5 rounded-full bg-slate-50 dark:bg-black/20 hover:bg-slate-100 dark:hover:bg-black/40 border border-slate-200/60 dark:border-white/5 transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-purple-400/50 active:scale-95 w-full sm:w-auto flex justify-center"
          title="Get a new prompt"
        >
          <RefreshCw className={`w-4 h-4 lg:w-5 lg:h-5 ${textSecondary} ${isAnimating ? 'animate-spin text-purple-500 dark:text-teal-400' : ''}`} />
        </button>
      </div>
    </div>
  );
}

export default React.memo(DailyInspiration);