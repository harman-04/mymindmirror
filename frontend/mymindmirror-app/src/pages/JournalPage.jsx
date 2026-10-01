import React, { useState, useEffect, useRef, useMemo, lazy, Suspense } from 'react';
import { useNavigate } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { format, startOfWeek, endOfWeek } from 'date-fns';
import { useTheme } from '../contexts/ThemeContext';
import FadeIn from '../components/FadeIn';
import DailyInspiration from '../components/DailyInspiration';
import RoadmapPlanner from '../components/RoadmapPlanner';
import AchievementsWidget from '../components/AchievementsWidget';
import {
  useTodayEntries,
  useWeeklyEntries,
  usePaginatedJournalEntries,
} from '../hooks/useJournalData';
import JournalInput from '../components/JournalInput';
import AnomalyAlerts from '../components/AnomalyAlerts';
import TodayDashboard from '../components/TodayDashboard';
import WeeklyDashboard from '../components/WeeklyDashboard';
import JournalSearch from '../components/JournalSearch';
import MilestoneTracker from '../components/MilestoneTracker';
import ReflectionChat from '../components/ReflectionChat';
import {
  Calendar,
  BarChart3,
  History,
  Search,
  Target,
  Map,
  Sparkles,
  Feather,
  User,
  CalendarDays,
  Loader
} from 'lucide-react';

const OverallDashboard = lazy(() => import('../components/OverallDashboard'));
const ScheduleTab = lazy(() => import('../components/ScheduleTab'));

const ALL_TABS = ['today', 'weekly', 'all', 'search', 'milestones', 'roadmap', 'chat', 'schedule'];

function JournalPage() {
  const [username, setUsername] = useState('');
  const [userId, setUserId] = useState(null);
  const [activeTab, setActiveTab] = useState(() => {
    const saved = sessionStorage.getItem('journalActiveTab');
    return saved && ALL_TABS.includes(saved) ? saved : 'today';
  });

  const navigate = useNavigate();
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';
  const journalInputRef = useRef();

  // Date ranges for queries
  const todayDate = new Date();
  const startOfCurrentWeek = startOfWeek(todayDate, { weekStartsOn: 0 });
  const endOfCurrentWeek = endOfWeek(todayDate, { weekStartsOn: 0 });

  // Queries
  const todayQuery = useTodayEntries();
  const weeklyQuery = useWeeklyEntries(
    format(startOfCurrentWeek, 'yyyy-MM-dd'),
    format(endOfCurrentWeek, 'yyyy-MM-dd')
  );
  const paginatedQuery = usePaginatedJournalEntries(20, {
    enabled: activeTab === 'all',
  });

  // Force React to memorize the history array.
  const allHistoryData = useMemo(() => {
    return paginatedQuery.data?.pages.flatMap(page => page.content) || [];
  }, [paginatedQuery.data]);
  const totalEntries = paginatedQuery.data?.pages[0]?.totalElements || 0;

  let entriesData, isLoading, isError, error, refetch, hasNextPage, fetchNextPage, isFetchingNextPage;
  switch (activeTab) {
    case 'today':
      entriesData = todayQuery.data || [];
      isLoading = todayQuery.isLoading;
      isError = todayQuery.isError;
      error = todayQuery.error;
      refetch = todayQuery.refetch;
      break;
    case 'weekly':
      entriesData = weeklyQuery.data || [];
      isLoading = weeklyQuery.isLoading;
      isError = weeklyQuery.isError;
      error = weeklyQuery.error;
      refetch = weeklyQuery.refetch;
      break;
    case 'all':
      entriesData = allHistoryData;
      isLoading = paginatedQuery.isLoading;
      isError = paginatedQuery.isError;
      error = paginatedQuery.error;
      refetch = paginatedQuery.refetch;
      hasNextPage = paginatedQuery.hasNextPage;
      fetchNextPage = paginatedQuery.fetchNextPage;
      isFetchingNextPage = paginatedQuery.isFetchingNextPage;
      break;
    default:
      entriesData = [];
      isLoading = false;
      isError = false;
      error = null;
      refetch = () => {};
  }

  const todayEntries = activeTab === 'today' ? entriesData : [];
  const weeklyEntries = activeTab === 'weekly' ? entriesData : [];

  useEffect(() => {
    const hash = window.location.hash.slice(1);
    if (hash && ALL_TABS.includes(hash)) {
      setActiveTab(hash);
      window.history.replaceState(null, '', window.location.pathname);
    }
  }, []);

  useEffect(() => {
    sessionStorage.setItem('journalActiveTab', activeTab);
  }, [activeTab]);

  useEffect(() => {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      navigate('/login');
      return;
    }
    try {
      const decodedToken = jwtDecode(token);
      setUsername(decodedToken.sub);
      setUserId(decodedToken.userId);
    } catch (decodeError) {
      console.error('Error decoding JWT:', decodeError);
      localStorage.removeItem('jwtToken');
      navigate('/login');
    }
  }, [navigate]);

//   const handleLogout = () => {
//     localStorage.removeItem('jwtToken');
//     navigate('/login');
//   };

  // Memoized tabs
  const tabs = useMemo(() => [
    { id: 'today', label: 'Today', icon: CalendarDays },
    { id: 'weekly', label: 'Weekly', icon: BarChart3 },
    { id: 'all', label: 'History', icon: History },
    { id: 'search', label: 'Search', icon: Search },
    { id: 'milestones', label: 'Milestones', icon: Target },
    { id: 'roadmap', label: 'Roadmap', icon: Map },
    { id: 'chat', label: 'AI Coach', icon: Sparkles },
    { id: 'schedule', label: 'Schedule', icon: Calendar },
  ], []);

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const sectionBg = isDarkMode ? 'bg-[#131127]/80 shadow-inner' : 'bg-slate-50/80 shadow-inner';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
  const inputBg = isDarkMode ? 'bg-[#131127] text-gray-100' : 'bg-slate-50 text-slate-900';
  const inputBorder = isDarkMode ? 'border-white/10' : 'border-slate-300';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  if (isError) {
    return (
      // 🌟 Cleaned up using Master Palette
      <div className={`w-full max-w-7xl mx-auto flex-grow p-6 rounded-2xl border ${cardBg} ${cardBorder} flex flex-col items-center justify-center font-inter text-lg text-rose-500`}>
{/*         <p>{error?.message || 'Failed to load journal data.'}</p> */}
<p>{error?.response?.data?.message || 'Failed to load journal data.'}</p>

{/*         <button */}
{/*           onClick={handleLogout} */}
{/*           className="mt-6 py-2.5 px-6 rounded-full font-poppins font-semibold text-white bg-gradient-to-r from-rose-500 to-rose-600 hover:from-rose-600 hover:to-rose-700 active:scale-95 shadow-sm transition-all duration-200" */}
{/*         > */}
{/*           Logout */}
{/*         </button> */}
<button
  onClick={() => refetch()}
  className="mt-6 py-2.5 px-6 rounded-full font-poppins font-semibold text-white bg-gradient-to-r from-rose-500 to-rose-600 hover:from-rose-600 hover:to-rose-700 active:scale-95 transition-all duration-200"
>
  Retry
</button>
      </div>
    );
  }

  return (
    <div className="w-full flex-grow flex flex-col relative">
      {/* 🌟 AESTHETIC UPGRADE: Responsive Floating Background Icons */}
      <div className="fixed top-[15%] lg:top-[30%] -left-10 sm:left-4 xl:left-[calc(50%-44rem)] opacity-5 sm:opacity-10 lg:opacity-20 animate-float z-0 pointer-events-none">
        <Feather className="w-48 h-48 sm:w-12 sm:h-12 lg:w-16 lg:h-16 text-purple-500 dark:text-purple-400" />
      </div>

      <div className="fixed bottom-[15%] lg:bottom-[30%] -right-10 sm:right-4 xl:right-[calc(50%-44rem)] opacity-5 sm:opacity-10 lg:opacity-20 animate-float-delayed z-0 pointer-events-none">
        <User className="w-48 h-48 sm:w-12 sm:h-12 lg:w-16 lg:h-16 text-teal-500 dark:text-teal-400" />
      </div>

      <main className="relative w-full max-w-7xl mx-auto flex-grow flex flex-col space-y-6 lg:space-y-8 z-10 pb-10">

        {/* Core Widgets Area */}
        <div className="space-y-6 lg:space-y-8">
          <FadeIn delay={0.1} fullWidth>
            <AchievementsWidget />
          </FadeIn>

          <FadeIn delay={0.2} fullWidth>
            <DailyInspiration />
          </FadeIn>
          <FadeIn delay={0.3} fullWidth>
            <JournalInput ref={journalInputRef} />
          </FadeIn>

          <FadeIn delay={0.4} fullWidth>
            <AnomalyAlerts />
          </FadeIn>
        </div>

        {/* 🚀 DYNAMIC CENTER DOCK (Tabs Only) */}
        <FadeIn delay={0.5} direction="up" fullWidth>
          <div className="flex items-center justify-center w-full pt-6 pb-4 relative z-30">
            <div className="w-full xl:w-auto overflow-x-auto scrollbar-hide -mx-4 px-4 xl:mx-0 xl:px-0 text-center">
              {/* 🌟 Cleaned up using Master Palette */}
              <div className={`inline-flex items-center p-1.5 rounded-[1.25rem] border ${cardBg} ${cardBorder} hover:shadow-md transition-shadow min-w-max`}>
                <nav className="flex flex-nowrap gap-1">
                  {tabs.map((tab) => {
                    const Icon = tab.icon;
                    const isActive = activeTab === tab.id;

                    return (
                        <button
                          key={tab.id}
                          onClick={() => setActiveTab(tab.id)}
                          className={`relative flex items-center justify-center gap-2 px-3 sm:px-4 lg:px-5 py-2.5 lg:py-3 rounded-xl transition-all duration-200 active:scale-95 outline-none group ${
                            isActive
                              ? 'text-white shadow-sm'
                              // 🌟 Synced hover states to the Master Palette
                              : `${textSecondary} hover:${textPrimary} hover:bg-slate-100 dark:hover:bg-white/10`
                          }`}
                        >
                        {isActive && (
                          <div className="absolute inset-0 bg-gradient-to-r from-purple-500 to-purple-600 dark:from-teal-500 dark:to-teal-700 rounded-xl z-0 shadow-inner" />
                        )}
                        <span className={`relative z-10 transition-transform duration-300 ${isActive ? 'scale-110' : 'group-hover:scale-110'}`}>
                          <Icon className="w-4 h-4 sm:w-4 sm:h-4 lg:w-5 lg:h-5" />
                        </span>
                        <span className="relative z-10 font-poppins font-semibold text-xs sm:text-sm lg:text-base hidden sm:inline whitespace-nowrap tracking-wide">
                          {tab.label}
                        </span>
                      </button>
                    );
                  })}
                </nav>
              </div>
            </div>
          </div>
        </FadeIn>

       {/* Dashboard Content Container */}
               <FadeIn delay={0.6} direction="up" fullWidth className="pt-2 lg:pt-4 min-h-[50vh]">
                 {/* ✅ ADD SUSPENSE WRAPPER HERE */}
                 <Suspense fallback={
                   <div className="flex items-center justify-center w-full h-64">
                     <Loader className="w-8 h-8 animate-spin text-purple-500" />
                   </div>
                 }>
                   {activeTab === 'today' && <TodayDashboard todayEntries={todayEntries} isLoading={isLoading} />}
                   {activeTab === 'weekly' && (
                     <WeeklyDashboard
                       weeklyEntries={weeklyEntries}
                       isLoading={isLoading}
                       userId={userId}
                       startOfCurrentWeek={startOfCurrentWeek}
                       endOfCurrentWeek={endOfCurrentWeek}
                     />
                   )}
                   {activeTab === 'all' && (
                     <OverallDashboard
                       journalEntries={entriesData}
                       isLoading={isLoading}
                       userId={userId}
                       loadMore={paginatedQuery.fetchNextPage}
                       hasNextPage={paginatedQuery.hasNextPage}
                       isFetchingNextPage={paginatedQuery.isFetchingNextPage}
                       totalEntries={totalEntries}
                     />
                   )}
                   {activeTab === 'search' && <JournalSearch userId={userId} />}
                   {activeTab === 'milestones' && <MilestoneTracker userId={userId} />}
                   {activeTab === 'roadmap' && <RoadmapPlanner />}
                   {activeTab === 'chat' && <ReflectionChat />}
                   {activeTab === 'schedule' && <ScheduleTab />}
                 </Suspense>
               </FadeIn>
      </main>
    </div>
  );
}

export default JournalPage;