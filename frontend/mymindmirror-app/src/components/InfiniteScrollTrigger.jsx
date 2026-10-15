// src/components/InfiniteScrollTrigger.jsx
import React, { useEffect, useRef } from 'react';
import { Loader2 } from 'lucide-react';
import { useTheme } from '../contexts/ThemeContext'; // 🌟 Added Theme Hook

const InfiniteScrollTrigger = ({ onIntersect, isLoading, hasNextPage }) => {
  const triggerRef = useRef(null);
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Layer 2 Floating Pill)
  // ==========================================================================
  const pillBg = isDarkMode ? 'bg-white/5' : 'bg-slate-50';
  const pillBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  useEffect(() => {
    if (!triggerRef.current || isLoading || !hasNextPage) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && !isLoading && hasNextPage) {
          onIntersect();
        }
      },
      { threshold: 0.1, rootMargin: '100px' }
    );

    observer.observe(triggerRef.current);
    return () => observer.disconnect();
  }, [onIntersect, isLoading, hasNextPage]);

  if (!hasNextPage) return null;

  return (
    <div ref={triggerRef} className="flex justify-center py-6 w-full">
      {isLoading && (
        <div className={`flex items-center gap-2 px-4 py-2 rounded-full shadow-sm border ${pillBg} ${pillBorder}`}>
           <Loader2 size={18} className="animate-spin text-purple-500 dark:text-teal-400" />
           <span className={`text-xs font-semibold ${textSecondary} font-poppins tracking-wide`}>
             Loading more...
           </span>
        </div>
      )}
    </div>
  );
};

export default React.memo(InfiniteScrollTrigger);