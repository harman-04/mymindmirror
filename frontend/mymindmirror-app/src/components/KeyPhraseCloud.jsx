// src/components/KeyPhraseCloud.jsx
import React, { useMemo, useRef, useEffect, useState } from 'react';
import { useTheme } from '../contexts/ThemeContext';
import { useKeyPhraseFrequencies } from '../hooks/useJournalData';
import { Cloud, ChevronDown } from 'lucide-react';
import { toPng } from 'html-to-image';
import DownloadChartButton from './DownloadChartButton';
import { SkeletonCloud } from './Skeleton';

const KeyPhraseCloud = ({ onWordClick, isLoading: parentIsLoading }) => {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  const { data: freqMap, isLoading: isFetchingData, isError } = useKeyPhraseFrequencies();
  const cardRef = useRef(null);
  const scrollContainerRef = useRef(null);
  const [isDownloading, setIsDownloading] = useState(false);

  const [isMobile, setIsMobile] = useState(window.innerWidth < 640);
  useEffect(() => {
      const handleResize = () => setIsMobile(window.innerWidth < 640);
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

  const words = useMemo(() => {
    if (!freqMap) return [];
    const entries = Object.entries(freqMap);
    if (entries.length === 0) return [];

    const counts = entries.map(([_, v]) => v);
    const minCount = Math.min(...counts);
    const maxCount = Math.max(...counts);

    const minFontSize = isMobile ? 12 : 16;
    const maxFontSize = isMobile ? 40 : 56;

    return entries
      .map(([text, value], index) => {
        const size = entries.length === 1
          ? (isMobile ? 24 : 36)
          : minFontSize + ((value - minCount) / (Math.max(maxCount - minCount, 1))) * (maxFontSize - minFontSize);

        const hue = (index * 137) % 360;
        const sat = isDarkMode ? 90 : 75;
        const light = isDarkMode ? 70 : 45;

        return {
          text,
          size,
          color: `hsl(${hue}, ${sat}%, ${light}%)`,
          delay: `${(index * 0.05).toFixed(2)}s`,
          duration: `${(4 + Math.random() * 3).toFixed(2)}s`,
        };
      })
      .sort(() => Math.random() - 0.5);
  }, [freqMap, isDarkMode, isMobile]);

  const handleDownload = async (e) => {
    e?.stopPropagation();
    if (isDownloading) return;

    const cardEl = cardRef.current;
    const scrollEl = scrollContainerRef.current;
    if (!cardEl || !scrollEl) return;

    setIsDownloading(true);

    const originalMaxHeight = scrollEl.style.maxHeight;
    const originalOverflow = scrollEl.style.overflow;

    scrollEl.style.maxHeight = 'none';
    scrollEl.style.overflow = 'visible';

    await new Promise(resolve => setTimeout(resolve, 150));

    try {
      const dataUrl = await toPng(cardEl, {
        // 🌟 FIX: We keep the solid background color ONLY for the exported PNG!
        backgroundColor: isDarkMode ? '#131127' : '#ffffff',
        cacheBust: true,
        fontEmbedCSS: '',
        style: { margin: '0' }
      });
      const link = document.createElement('a');
      link.download = `key_phrase_cloud.png`;
      link.href = dataUrl;
      link.click();
    } catch (error) {
      console.error('Failed to download cloud:', error);
    } finally {
      scrollEl.style.maxHeight = originalMaxHeight;
      scrollEl.style.overflow = originalOverflow;
      setIsDownloading(false);
    }
  };

  const isLoading = parentIsLoading || isFetchingData;

  if (isLoading) {
      return <SkeletonCloud />;
  }

  if (isError || words.length === 0) {
    return (
      <div className={`w-full rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-sm ring-1 ring-black/5 dark:ring-white/5 overflow-hidden ${cardBg} flex flex-col h-full`}>
        <div className={`flex flex-wrap justify-between items-center gap-4 p-4 lg:p-6 border-b ${sectionBorder} ${sectionBg}`}>
          <div className="flex-1 min-w-[200px]">
              <h3 className={`text-lg lg:text-xl font-poppins font-extrabold ${textPrimary} tracking-tight flex items-center gap-3`}>
                  {/* 🌟 RESTORED: Sky Blue Jewel Icon */}
                  <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-sky-100 to-indigo-50 dark:from-sky-900/40 dark:to-indigo-800/20 text-sky-500 dark:text-sky-400 shrink-0 shadow-sm border border-sky-200/50 dark:border-sky-700/30">
                      <Cloud className="w-5 h-5 lg:w-6 lg:h-6" />
                  </div>
                  Key Phrase Cloud
              </h3>
          </div>
          <DownloadChartButton filename="key_phrase_cloud" darkMode={isDarkMode} className="opacity-50 pointer-events-none shrink-0" />
        </div>
        {/* 🌟 FIX: Removed hardcoded background color */}
        <div className="flex-grow flex flex-col items-center justify-center p-6 lg:p-10 text-center min-h-[300px]">
          <div className="p-4 rounded-full bg-slate-100 dark:bg-[#131127] border border-slate-200/80 dark:border-white/5 mb-4 shadow-inner">
              <Cloud className={`w-8 h-8 lg:w-10 lg:h-10 ${textSecondary}`} />
          </div>
          <p className={`text-lg lg:text-xl font-bold ${textPrimary}`}>
            {isError ? "Couldn't load patterns" : "No patterns detected yet."}
          </p>
          <p className={`text-sm lg:text-base mt-2 ${textSecondary}`}>
            Start journaling to see your key phrases appear here!
          </p>
        </div>
      </div>
    );
  }

  return (
    <div ref={cardRef} className={`relative w-full rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-sm ring-1 ring-black/5 dark:ring-white/5 overflow-hidden ${cardBg} flex flex-col transition-all duration-300 hover:shadow-md hover:-translate-y-0.5`}>
      {/* Header */}
      <div className={`flex flex-wrap justify-between items-center gap-4 p-4 lg:p-6 border-b ${sectionBorder} ${sectionBg}`}>
          <div className="flex-1 min-w-[200px]">
              <h3 className={`text-lg lg:text-xl font-poppins font-extrabold ${textPrimary} tracking-tight flex items-center gap-3`}>
                  {/* 🌟 RESTORED: Sky Blue Jewel Icon */}
                  <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-sky-100 to-indigo-50 dark:from-sky-900/40 dark:to-indigo-800/20 text-sky-500 dark:text-sky-400 shrink-0 shadow-sm border border-sky-200/50 dark:border-sky-700/30">
                      <Cloud className="w-5 h-5 lg:w-6 lg:h-6" />
                  </div>
                  Key Phrase Cloud
              </h3>
              <p className={`text-[11px] lg:text-xs mt-0.5 lg:mt-1 font-medium ${textSecondary}`}>
                 Common themes from your journaling. Click words to filter your history.
              </p>
          </div>

          <DownloadChartButton
              onClick={handleDownload}
              isCustomDownloading={isDownloading}
              filename="key_phrase_cloud"
              darkMode={isDarkMode}
              className="hover:scale-105 transition-transform shrink-0"
          />
      </div>

      {/* 🌟 FIX: Removed hardcoded backgroundColor string here so cardBg shows through! */}
      <div
        ref={scrollContainerRef}
        className="p-4 sm:p-6 lg:p-8 overflow-y-auto relative"
        style={{
          maxHeight: isMobile ? '350px' : '450px',
          scrollbarWidth: 'thin',
          scrollbarColor: isDarkMode ? '#4B5563 transparent' : '#CBD5E1 transparent',
        }}
      >
        <div className="flex flex-wrap justify-center items-center gap-3 sm:gap-4 lg:gap-6 z-10 pb-4">
          {words.map((word) => (
            <button
              key={word.text}
              onClick={() => onWordClick?.(word.text)}
              className="transition-all duration-700 ease-out hover:scale-125 active:scale-90 hover:z-20 cursor-pointer focus:outline-none"
              style={{
                fontSize: `${word.size}px`,
                color: word.color,
                fontWeight: word.size > (isMobile ? 24 : 36) ? '800' : '600',
                textShadow: isDarkMode ? '0 0 15px rgba(255,255,255,0.05)' : '0 2px 4px rgba(0,0,0,0.05)',
                animationName: 'float',
                animationDuration: word.duration,
                animationTimingFunction: 'ease-in-out',
                animationIterationCount: 'infinite',
                animationDelay: word.delay,
              }}
            >
              {word.text}
            </button>
          ))}
        </div>
      </div>

      {scrollContainerRef.current && scrollContainerRef.current.scrollHeight > scrollContainerRef.current.clientHeight && (
        <div className="absolute bottom-0 left-0 right-0 flex justify-center pointer-events-none pb-2">
          {/* 🌟 FIX: Gradient matched to the new cardBg colors */}
          <div className={`bg-gradient-to-t ${isDarkMode ? 'from-[#1A162F]/90 via-[#1A162F]/50' : 'from-white/90 via-white/50'} to-transparent w-full h-12 flex items-end justify-center pb-2`}>
            <ChevronDown className={`w-5 h-5 ${textSecondary} animate-bounce`} />
          </div>
        </div>
      )}

    </div>
  );
};

export default React.memo(KeyPhraseCloud);