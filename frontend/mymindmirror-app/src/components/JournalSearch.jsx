import React, { useState, useCallback } from 'react';
import JournalHistory from './JournalHistory';
import { useTheme } from '../contexts/ThemeContext';
import { format, parseISO } from 'date-fns';
import { useSearchJournalEntries } from '../hooks/useJournalData';
import {
  Search,
  Sliders,
  Calendar,
  X,
  Loader,
  FileText,
  Sparkles
} from 'lucide-react';
import PremiumInput from './PremiumInput';
// import { toast } from 'sonner';
import FadeIn from './FadeIn';

function JournalSearch({ userId }) {
  const [keyword, setKeyword] = useState('');
  const [concept, setConcept] = useState('');
  const [minMood, setMinMood] = useState('');
  const [maxMood, setMaxMood] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [searchType, setSearchType] = useState('semantic');
  const [activeSearchParams, setActiveSearchParams] = useState(null);
  const [searchErrors, setSearchErrors] = useState({});
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

 const {
     data: searchResults,
     isLoading,
     isError,
     error,
 } = useSearchJournalEntries(activeSearchParams);
//   useEffect(() => {
//     if (isError && error) {
//       toast.error(error.message || 'Search failed. Please try again.');
//     }
//   }, [isError, error]);

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const sectionBg = isDarkMode ? 'bg-[#131127]/80 shadow-inner' : 'bg-slate-50/80 shadow-inner';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  const buttonActive = isDarkMode
    ? 'bg-gradient-to-r from-teal-500 to-teal-600 text-white shadow-lg shadow-teal-900/30 border border-transparent'
    : 'bg-gradient-to-r from-purple-500 to-purple-600 text-white shadow-lg shadow-purple-500/30 border border-transparent';
  const buttonInactive = isDarkMode
    ? 'bg-white/5 text-gray-400 hover:bg-white/10 border border-white/5'
    : 'bg-slate-100 text-slate-600 hover:bg-slate-200 border border-slate-200/80';

  const handleSearch = useCallback((e) => {
    if (e) e.preventDefault();
    setSearchErrors({});
    let params = { searchType };
    let errors = {};

    if (searchType === 'semantic') {
      if (!concept.trim()) errors.concept = 'Please describe a concept or memory.';
      else params.concept = concept.trim();
    }
    else if (searchType === 'keyword') {
      if (!keyword.trim()) errors.keyword = 'Please enter a keyword to search.';
      else params.keyword = keyword.trim();
    }
    else if (searchType === 'mood') {
      const parsedMinMood = minMood === '' ? null : parseFloat(minMood);
      const parsedMaxMood = maxMood === '' ? null : parseFloat(maxMood);

      if (minMood === '' && maxMood === '') {
        errors.mood = 'Please enter at least a min or max mood score.';
      }
      else if (
        (parsedMinMood !== null && (parsedMinMood < -1 || parsedMinMood > 1)) ||
        (parsedMaxMood !== null && (parsedMaxMood < -1 || parsedMaxMood > 1))
      ) {
        errors.mood = 'Mood scores must be between -1.0 and 1.0.';
      }
      else if (parsedMinMood !== null && parsedMaxMood !== null && parsedMinMood > parsedMaxMood) {
        errors.mood = 'Min mood cannot be greater than max mood.';
      } else {
        params.minMood = minMood;
        params.maxMood = maxMood;
      }
    }
    else {
      if (!startDate && !endDate) {
        errors.date = 'Please select at least a start date or end date.';
      } else if (startDate && endDate && parseISO(startDate) > parseISO(endDate)) {
        errors.date = 'Start date cannot be after end date.';
      } else {
        params.startDate = startDate;
        params.endDate = endDate;
      }
    }

    if (Object.keys(errors).length > 0) {
      setSearchErrors(errors);
      setActiveSearchParams(null);
      return;
    }

    setActiveSearchParams(params);
  }, [searchType, keyword, concept, minMood, maxMood, startDate, endDate]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSearch();
    }
  };

  const clearSearch = () => {
    setActiveSearchParams(null);
    setKeyword('');
    setConcept('');
    setMinMood('');
    setMaxMood('');
    setStartDate('');
    setEndDate('');
    setSearchErrors({});
  };

  const isSearchActive = activeSearchParams !== null;

  return (
    <div className="space-y-6 sm:space-y-8 w-full">
      <FadeIn delay={0.1} direction="down" fullWidth>
        <div className={`rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} p-5 sm:p-6 lg:p-8 shadow-sm transition-shadow hover:shadow-md`}>

          <div className="flex items-center justify-center gap-3 mb-6 lg:mb-8">
            {/* 🌟 RESTORED: Purple Jewel Icon */}
            <div className="p-2 lg:p-3 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-teal-900/40 dark:to-teal-800/20 text-purple-600 dark:text-teal-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-teal-700/30">
              <Search className="w-5 h-5 lg:w-6 lg:h-6" />
            </div>
            <h3 className={`text-xl lg:text-2xl font-poppins font-extrabold text-center ${textPrimary} tracking-tight`}>
              Search Journal Entries
            </h3>
          </div>

          <div className="flex justify-center gap-2 sm:gap-3 mb-8 lg:mb-10 flex-wrap">
            {[
              { id: 'semantic', label: 'AI Concept', icon: <Sparkles size={16} /> },
              { id: 'keyword', label: 'Exact Keyword', icon: <Search size={16} /> },
              { id: 'mood', label: 'Mood Score', icon: <Sliders size={16} /> },
              { id: 'date', label: 'Date Range', icon: <Calendar size={16} /> }
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setSearchType(tab.id)}
                className={`flex items-center gap-1.5 lg:gap-2 py-2 px-4 lg:px-5 rounded-full font-poppins font-medium text-xs lg:text-sm transition-all duration-300 active:scale-95 shadow-sm hover:-translate-y-0.5
                           ${searchType === tab.id ? buttonActive : buttonInactive}`}
              >
                {React.cloneElement(tab.icon, { className: 'w-4 h-4 lg:w-4 lg:h-4' })}
                {tab.label}
              </button>
            ))}
          </div>

          <form className="space-y-4 lg:space-y-5" onSubmit={handleSearch} noValidate>
            {searchType === 'semantic' && (
              <div className="animate-fade-in">
                <PremiumInput
                  label={
                    <span className="flex items-center gap-2">
                      Ask AI to find a memory
                      <span className="text-[10px] bg-gradient-to-r from-purple-500 to-teal-500 text-white px-2 py-0.5 rounded-full uppercase font-bold tracking-wider">New</span>
                    </span>
                  }
                  value={concept}
                  onChange={(e) => {
                    setConcept(e.target.value);
                    if (searchErrors.concept) setSearchErrors({});
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="e.g., 'Times I felt overwhelmed at work', 'Peaceful moments in nature'"
                  error={searchErrors.concept}
                  showError={!!searchErrors.concept}
                />
              </div>
            )}

            {searchType === 'keyword' && (
              <div className="animate-fade-in">
                <PremiumInput
                  label="Exact Keyword"
                  value={keyword}
                  onChange={(e) => {
                    setKeyword(e.target.value);
                    if (searchErrors.keyword) setSearchErrors({});
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="e.g., happy, stress, meeting"
                  error={searchErrors.keyword}
                  showError={!!searchErrors.keyword}
                />
              </div>
            )}

            {searchType === 'mood' && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 lg:gap-6 animate-fade-in">
                <PremiumInput
                  type="number"
                  step="0.01"
                  label="Min Mood"
                  value={minMood}
                  onChange={(e) => {
                    setMinMood(e.target.value);
                    if (searchErrors.mood) setSearchErrors({});
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="-1.0 to 1.0"
                  error={searchErrors.mood}
                  showError={!!searchErrors.mood}
                />
                <PremiumInput
                  type="number"
                  step="0.01"
                  label="Max Mood"
                  value={maxMood}
                  onChange={(e) => {
                    setMaxMood(e.target.value);
                    if (searchErrors.mood) setSearchErrors({});
                  }}
                  onKeyDown={handleKeyDown}
                  placeholder="-1.0 to 1.0"
                  error={searchErrors.mood}
                  showError={!!searchErrors.mood}
                />
              </div>
            )}

            {searchType === 'date' && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 lg:gap-6 animate-fade-in">
                <PremiumInput
                  type="date"
                  label="Start Date"
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value);
                    if (searchErrors.date) setSearchErrors({});
                  }}
                  onKeyDown={handleKeyDown}
                  error={searchErrors.date}
                  showError={!!searchErrors.date}
                />
                <PremiumInput
                  type="date"
                  label="End Date"
                  value={endDate}
                  onChange={(e) => {
                    setEndDate(e.target.value);
                    if (searchErrors.date) setSearchErrors({});
                  }}
                  onKeyDown={handleKeyDown}
                  error={searchErrors.date}
                  showError={!!searchErrors.date}
                />
              </div>
            )}
          </form>

          <div className="flex flex-col sm:flex-row gap-3 lg:gap-4 mt-8 lg:mt-10">
            <button
              onClick={handleSearch}
              disabled={isLoading}
              className="flex-1 py-3 lg:py-4 rounded-xl bg-gradient-to-r from-purple-500 to-teal-500 hover:from-purple-600 hover:to-teal-600 text-white font-bold text-sm lg:text-base shadow-md hover:shadow-lg active:scale-[0.98] transition-all duration-300 flex items-center justify-center gap-2 disabled:opacity-50"
            >
              {isLoading ? <Loader className="w-5 h-5 animate-spin" /> : <Search className="w-5 h-5" />}
              {isLoading ? 'Searching...' : 'Search Journal'}
            </button>
            {isSearchActive && (
              <button
                onClick={clearSearch}
                className={`py-3 lg:py-4 px-6 lg:px-8 rounded-xl bg-slate-100 dark:bg-white/5 ${textPrimary} font-bold text-sm lg:text-base border border-slate-200/80 dark:border-white/10 hover:bg-slate-200 dark:hover:bg-white/10 active:scale-[0.98] transition-all flex items-center justify-center gap-2`}
              >
                <X className="w-5 h-5" /> Clear
              </button>
            )}
          </div>
        </div>
      </FadeIn>

{/* 1. Results */}
      {(isSearchActive && (isLoading || searchResults?.length > 0)) && (
        <FadeIn delay={0.2} direction="up" fullWidth>
          <div className={`rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} p-4 sm:p-6 lg:p-8 shadow-sm`}>
            {/* 🌟 FIX: Applied sectionBorder for consistent division */}
            <div className={`flex flex-wrap justify-between items-center gap-4 mb-6 lg:mb-8 border-b ${sectionBorder} pb-4 lg:pb-6`}>
                <div className="flex items-center gap-3 lg:gap-4">
                    {/* 🌟 RESTORED: Purple Jewel Icon */}
                    <div className="p-2 lg:p-3 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-teal-900/40 dark:to-teal-800/20 text-purple-600 dark:text-teal-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-teal-700/30">
                        <FileText className="w-5 h-5 lg:w-6 lg:h-6" />
                    </div>
                    <h3 className={`text-xl lg:text-2xl font-poppins font-extrabold ${textPrimary} tracking-tight`}>
                        Search Results
                    </h3>
                </div>

                {isLoading ? (
                    <div className="w-20 lg:w-24 h-6 lg:h-8 bg-slate-200/80 dark:bg-white/10 rounded-full animate-pulse" />
                ) : (
                    <div className="flex items-center gap-2 px-3 lg:px-4 py-1.5 lg:py-2 bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/20 rounded-full shadow-sm text-emerald-700 dark:text-emerald-300">
                        <span className="text-[10px] lg:text-xs font-bold uppercase tracking-wider whitespace-nowrap">
                            {searchResults.length} {searchResults.length === 1 ? 'Entry' : 'Entries'} Found
                        </span>
                    </div>
                )}
            </div>

            <JournalHistory entries={searchResults || []} searchType={searchType} isLoading={isLoading} />
          </div>
        </FadeIn>
      )}

{/* 2. No Results */}
      {!isLoading && !isError && searchResults?.length === 0 && isSearchActive && (
        <FadeIn delay={0.2} direction="up" fullWidth>
          <div className={`rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} p-10 lg:p-14 text-center flex flex-col items-center justify-center shadow-sm`}>
            <div className="w-16 h-16 lg:w-20 lg:h-20 mb-4 rounded-full bg-slate-100 dark:bg-white/5 flex items-center justify-center">
              <Search className={`w-8 h-8 lg:w-10 lg:h-10 ${textSecondary} opacity-50`} />
            </div>
            <h3 className={`text-xl lg:text-2xl font-poppins font-bold ${textPrimary} mb-2`}>No Results Found</h3>
            <p className={`text-sm lg:text-base ${textSecondary} max-w-md`}>We couldn't find any journal entries matching your exact search criteria.</p>
            <button
              onClick={clearSearch}
              className="mt-6 text-sm lg:text-base font-bold text-purple-600 hover:text-purple-700 dark:text-teal-400 dark:hover:text-teal-300 transition-colors px-6 py-2 rounded-full hover:bg-purple-50 dark:hover:bg-teal-900/20"
            >
              Clear search & try again
            </button>
          </div>
        </FadeIn>
      )}

{/* 3. Search Failed */}
  {!isLoading && isError && isSearchActive && (
      <FadeIn delay={0.2} direction="up" fullWidth>
          <div className={`rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} p-10 lg:p-14 text-center flex flex-col items-center justify-center shadow-sm`}>
              <div className="w-16 h-16 lg:w-20 lg:h-20 mb-4 rounded-full bg-rose-100 dark:bg-rose-500/10 flex items-center justify-center">
                  <Search className={`w-8 h-8 lg:w-10 lg:h-10 text-rose-500 opacity-80`} />
              </div>
              <h3 className={`text-xl lg:text-2xl font-poppins font-bold ${textPrimary} mb-2`}>
                  Search Failed
              </h3>
              <p className={`text-sm lg:text-base ${textSecondary} max-w-md`}>
                  {error?.response?.data?.message || 'Something went wrong while searching. Please try again.'}
              </p>
              <button
                  onClick={clearSearch}
                  className="mt-6 text-sm lg:text-base font-bold text-purple-600 hover:text-purple-700 dark:text-teal-400 dark:hover:text-teal-300 transition-colors px-6 py-2 rounded-full hover:bg-purple-50 dark:hover:bg-teal-900/20"
              >
                  Clear search & try again
              </button>
          </div>
      </FadeIn>
  )}
    </div>
  );
}

export default JournalSearch;