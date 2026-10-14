import React, { lazy, Suspense, useState, useEffect, useMemo } from 'react';
import { Routes, Route, Navigate, useLocation, useNavigate, Link } from 'react-router-dom';
import { Menu, X, Home, Sparkles, Award, User, LogOut, LogIn, UserPlus } from 'lucide-react';
import ThemeToggle from './components/ThemeToggle';
import AppLogo from './components/AppLogo';
import HeaderApiKeyStatus from './components/HeaderApiKeyStatus';
import { useTheme } from './contexts/ThemeContext';

// Lazy load pages
const HomePage = lazy(() => import('./pages/HomePage'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const RegisterPage = lazy(() => import('./pages/RegisterPage'));
const JournalPage = lazy(() => import('./pages/JournalPage'));
const ProfilePage = lazy(() => import('./pages/ProfilePage'));
const AchievementsPage = lazy(() => import('./pages/AchievementsPage'));
const FeaturesGuide = lazy(() => import('./pages/FeaturesGuide'));
const ScheduleTab = lazy(() => import('./components/ScheduleTab'));

const ScrollToTop = () => {
  const { pathname } = useLocation();
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [pathname]);
  return null;
};

const PageLoader = () => {
  const location = useLocation();
  const isAbsoluteHeader = ['/', '/features'].includes(location.pathname);

  return (
    <div className={`flex flex-col items-center justify-center w-full min-h-[80vh] ${isAbsoluteHeader ? 'pt-24' : ''} animate-in fade-in duration-500`}>
      <div className="relative flex items-center justify-center w-20 h-20 mb-6">
        <div className="absolute inset-0 rounded-full border-t-2 border-r-2 border-purple-500/80 animate-spin" style={{ animationDuration: '2s' }}></div>
        <div className="absolute inset-2 rounded-full border-b-2 border-l-2 border-teal-500/80 animate-spin" style={{ animationDuration: '1.5s', animationDirection: 'reverse' }}></div>
        <div className="absolute inset-4 rounded-full border-t-2 border-l-2 border-pink-500/80 animate-spin" style={{ animationDuration: '1s' }}></div>
        <div className="absolute bg-gradient-to-br from-purple-500/10 to-teal-500/10 rounded-full inset-6 animate-pulse flex items-center justify-center blur-[1px]"></div>
        <Sparkles className="w-5 h-5 text-purple-400 animate-pulse relative z-10" />
      </div>
      <div className="flex flex-col items-center gap-2">
        <p className="text-slate-500 dark:text-gray-400 font-medium font-poppins tracking-[0.2em] uppercase text-xs animate-pulse">
          Loading Area
        </p>
        <div className="flex gap-1 mt-1">
          <div className="w-1.5 h-1.5 rounded-full bg-purple-500 animate-bounce" style={{ animationDelay: '0ms' }} />
          <div className="w-1.5 h-1.5 rounded-full bg-pink-500 animate-bounce" style={{ animationDelay: '150ms' }} />
          <div className="w-1.5 h-1.5 rounded-full bg-teal-500 animate-bounce" style={{ animationDelay: '300ms' }} />
        </div>
      </div>
    </div>
  );
};

const PrivateRoute = ({ children }) => {
  const isAuthenticated = !!localStorage.getItem('jwtToken');
  return isAuthenticated ? children : <Navigate to="/login" replace />;
};

function App() {
  const { theme } = useTheme();
  const location = useLocation();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('jwtToken'));

  const isDarkMode = theme === 'dark';

  //  ENTERPRISE UX: Route detection logic
  const isAuthPage = ['/login', '/register'].includes(location.pathname);
  const isMarketingPage = !isAuthenticated && ['/', '/features'].includes(location.pathname);
  const isFullBleedPage = isAuthPage;

  // ==========================================================================
  //  MASTER ELEVATION PALETTE (3-Layer Architecture for the Root)
  // ==========================================================================
  const colors = {
    // Headers and Drawers float above the bg, they MUST be Layer 1
    cardBg: isDarkMode ? 'bg-[#1A162F]/95' : 'bg-white/95',
    drawerBg: isDarkMode ? 'bg-[#1A162F]' : 'bg-white',
    cardBorder: isDarkMode ? 'border-white/10' : 'border-slate-200/80',

    // Layer 2: For Footer Pill
    sectionBg: isDarkMode ? 'bg-[#131127]/90' : 'bg-slate-50/90',

    textPrimary: isDarkMode ? 'text-gray-100' : 'text-slate-900',
    textSecondary: isDarkMode ? 'text-gray-400' : 'text-slate-500',
  };

  useEffect(() => {
    const handleStorageChange = () => setIsAuthenticated(!!localStorage.getItem('jwtToken'));
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  useEffect(() => {
    setIsAuthenticated(!!localStorage.getItem('jwtToken'));
  }, [location.pathname]);

  useEffect(() => {
    const metaThemeColor = document.querySelector("meta[name=theme-color]");
    if (metaThemeColor) {
      metaThemeColor.setAttribute("content", isDarkMode ? '#131127' : '#F8F9FA');
    }
  }, [isDarkMode]);

  const handleLogout = () => {
    localStorage.removeItem('jwtToken');
    setIsAuthenticated(false);
    navigate('/login', { replace: true });
    setMobileMenuOpen(false);
  };

  const showLogoutAndProfile = isAuthenticated &&
    ['/journal', '/profile', '/achievements', '/features', '/'].includes(location.pathname);

  const navLinks = useMemo(() => {
    const links = [
      { to: '/', label: 'Home', icon: Home, color: 'bg-gradient-to-r from-gray-500 to-gray-600 hover:from-gray-600 hover:to-gray-700' },
      { to: '/features', label: 'Features', icon: Sparkles, color: 'bg-gradient-to-r from-[#7B9EC2] to-[#6A8DB0] hover:from-[#6A8DB0] hover:to-[#5A7A9E]' },
    ];

    if (!isAuthenticated) {
      links.push(
        { to: '/login', label: 'Login', icon: LogIn, color: 'bg-gradient-to-r from-teal-500 to-teal-600 hover:from-teal-600 hover:to-teal-700' },
        { to: '/register', label: 'Register', icon: UserPlus, color: 'bg-gradient-to-r from-purple-500 to-purple-600 hover:from-purple-600 hover:to-purple-700' }
      );
    } else {
      links.push(
        { to: '/achievements', label: 'Achievements', icon: Award, color: 'bg-gradient-to-r from-[#B399D4] to-[#9B7BBF] hover:from-[#9B7BBF] hover:to-[#806AA0]' },
        { to: '/profile', label: 'Profile', icon: User, color: 'bg-gradient-to-r from-[#5CC8C2] to-[#4bb3ac] hover:from-[#4bb3ac] hover:to-[#3da19a]' }
      );
    }
    return links;
  }, [isAuthenticated]);

  const closeMobileMenu = () => setMobileMenuOpen(false);

  const bgGradient = isDarkMode
    ? 'from-[#131127] via-[#1E1A3E] to-[#2B2748]'
    : 'from-[#F1F5F9] via-[#E2E8F0] to-[#CBD5E1]';

  const layoutMaxWidth = "max-w-7xl";

  return (
    <div className={`min-h-screen flex flex-col items-center ${isFullBleedPage ? 'p-0' : 'px-2 sm:px-6 py-4 sm:py-6'} ${colors.textPrimary} relative selection:bg-purple-500/30`}>
      <ScrollToTop />

      {/* Global Animated Background */}
      <div className={`fixed inset-0 pointer-events-none overflow-hidden -z-10 bg-gradient-to-br ${bgGradient} transition-colors duration-700 transform-gpu`}>
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0IiBoZWlnaHQ9IjQiPgo8cmVjdCB3aWR0aD0iNCIgaGVpZ2h0PSI0IiBmaWxsPSIjZmZmIiBmaWxsLW9wYWNpdHk9IjAuMDUiLz4KPC9zdmc+')] opacity-20 dark:opacity-[0.03]"></div>
        <div className="absolute -top-[10%] -left-[10%] w-[90vw] h-[90vw] lg:w-[50vw] lg:h-[50vw] rounded-full animate-pulse-slow will-change-transform" style={{ background: 'radial-gradient(circle, rgba(168,85,247,0.15) 0%, rgba(168,85,247,0) 70%)' }} />
        <div className="absolute -bottom-[10%] -right-[10%] w-[90vw] h-[90vw] lg:w-[50vw] lg:h-[50vw] rounded-full animate-pulse-slow delay-1000 will-change-transform" style={{ background: 'radial-gradient(circle, rgba(20,184,166,0.15) 0%, rgba(20,184,166,0) 70%)' }} />
        <div className="absolute top-[30%] left-[10%] lg:left-[25%] w-[80vw] h-[80vw] lg:w-[40vw] lg:h-[40vw] rounded-full animate-pulse-slow delay-500 will-change-transform" style={{ background: 'radial-gradient(circle, rgba(236,72,153,0.1) 0%, rgba(236,72,153,0) 70%)' }} />
      </div>

      {/* DYNAMIC DESKTOP HEADER */}
      {!isAuthPage && (
        <header
          className={`hidden lg:flex flex-nowrap justify-between items-center transition-all duration-300 z-50
               ${isMarketingPage
              ? `absolute top-0 left-0 right-0 mx-auto w-full ${layoutMaxWidth} px-4 sm:px-6 lg:px-10 py-6 bg-transparent border-transparent`
              /* Synced corner radius exactly to Layer 1 Cards (rounded-2xl lg:rounded-3xl) */
              : `relative w-full ${layoutMaxWidth} py-4 px-4 sm:px-6 lg:px-8 mb-10 mt-4 sm:mt-6 rounded-2xl lg:rounded-3xl ${colors.cardBg} shadow-sm border ${colors.cardBorder} gap-6`}`}
        >
          <div className="transform hover:scale-[1.02] transition-transform duration-300 shrink-0">
            <AppLogo />
          </div>

          <div className="flex flex-nowrap justify-end items-center shrink-0">
            {isMarketingPage ? (
              <div className="flex items-center gap-6 xl:gap-8">
                <Link to="/features" className={`text-sm font-semibold ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 transition-colors`}>Features</Link>
                {!isAuthenticated ? (
                  <>
                    <Link to="/login" className={`text-sm font-semibold ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 transition-colors`}>Login</Link>
                    <Link to="/register" className="py-2 px-6 rounded-full font-poppins font-semibold text-purple-600 dark:text-teal-400 text-sm border border-purple-500/30 dark:border-teal-500/30 hover:bg-purple-500/10 dark:hover:bg-teal-500/10 active:scale-95 transition-all duration-300">
                      Sign Up
                    </Link>
                  </>
                ) : (
                  <>
                    <Link to="/achievements" className={`text-sm font-semibold ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 transition-colors`}>Achievements</Link>
                    <Link to="/profile" className={`text-sm font-semibold ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 transition-colors`}>Profile</Link>
                    <div className="pl-1"><HeaderApiKeyStatus /></div>
                    <Link to="/journal" className="py-2 px-6 rounded-full font-poppins font-semibold text-purple-600 dark:text-teal-400 text-sm border border-purple-500/30 dark:border-teal-500/30 hover:bg-purple-500/10 dark:hover:bg-teal-500/10 active:scale-95 transition-all duration-300">
                      Dashboard
                    </Link>
                  </>
                )}
                <div className="flex items-center pl-2">
                  <ThemeToggle />
                </div>
              </div>
            ) : (
              <div className="flex items-center gap-1 xl:gap-2">
                <Link
                  to="/features"
                  className={`px-3 xl:px-4 py-2 rounded-full font-poppins font-semibold text-sm whitespace-nowrap transition-colors duration-200 ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 hover:bg-slate-100 dark:hover:bg-white/10`}
                >
                  Features
                </Link>
                {!isAuthenticated ? (
                  <>
                    <Link to="/login" className="py-2.5 px-5 xl:px-6 rounded-full font-poppins font-semibold text-white text-sm whitespace-nowrap bg-gradient-to-r from-teal-500 to-teal-600 hover:-translate-y-0.5 active:translate-y-0 shadow-sm hover:shadow-md transition-all duration-300">
                      Login
                    </Link>
                    <Link to="/register" className="py-2.5 px-5 xl:px-6 rounded-full font-poppins font-semibold text-white text-sm whitespace-nowrap bg-gradient-to-r from-purple-500 to-purple-600 hover:-translate-y-0.5 active:translate-y-0 shadow-sm hover:shadow-md transition-all duration-300">
                      Register
                    </Link>
                  </>
                ) : (
                  <>
                    <Link
                      to="/achievements"
                      className={`px-3 xl:px-4 py-2 rounded-full font-poppins font-semibold text-sm whitespace-nowrap transition-colors duration-200 ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 hover:bg-slate-100 dark:hover:bg-white/10`}
                    >
                      Achievements
                    </Link>
                    <Link
                      to="/profile"
                      className={`px-3 xl:px-4 py-2 rounded-full font-poppins font-semibold text-sm whitespace-nowrap transition-colors duration-200 ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 hover:bg-slate-100 dark:hover:bg-white/10`}
                    >
                      Profile
                    </Link>
                    <div className="pl-1 xl:pl-2">
                      <HeaderApiKeyStatus />
                    </div>
                    {showLogoutAndProfile && (
                      <button
                        onClick={handleLogout}
                        className={`px-3 xl:px-4 py-2 rounded-full font-poppins font-semibold text-sm whitespace-nowrap transition-colors duration-200 ${colors.textSecondary} hover:text-rose-500 dark:hover:text-rose-400 hover:bg-slate-100 dark:hover:bg-white/10`}
                      >
                        Logout
                      </button>
                    )}
                  </>
                )}
                <div className={`pl-4 xl:pl-5 border-l ${colors.cardBorder} flex items-center`}>
                  <ThemeToggle />
                </div>
              </div>
            )}
          </div>
        </header>
      )}

      {/* DYNAMIC MOBILE HEADER */}
      {!isAuthPage && (
        <div
          className={`lg:hidden flex justify-between items-center transition-all duration-300 z-50
              ${isMarketingPage
              ? `absolute top-0 left-0 right-0 mx-auto w-full max-w-7xl px-4 sm:px-6 md:px-8 py-3 sm:py-4`
              /*  Mobile header uses Layer 1 cardBg & cardBorder */
              : `relative w-full py-2.5 px-3 sm:py-3 sm:px-5 md:py-4 md:px-6 mb-4 sm:mb-6 mt-1 sm:mt-2 md:mt-4 rounded-xl sm:rounded-2xl md:rounded-[2rem] ${colors.cardBg} shadow-sm border ${colors.cardBorder}`}`}
        >
          <AppLogo />

          <div className="flex items-center gap-1.5 sm:gap-3 md:gap-4">
            <div className="scale-90 sm:scale-100 origin-right transition-transform">
              <ThemeToggle />
            </div>

            <button
              onClick={() => setMobileMenuOpen(true)}
              className={`p-1.5 sm:p-2 md:p-2.5 rounded-lg sm:rounded-xl transition-colors duration-200 active:scale-95 border
                  ${isMarketingPage
                  ? 'bg-white/20 dark:bg-black/20 border-white/20 dark:border-white/10'
                  : 'bg-slate-100 dark:bg-white/5 hover:bg-slate-200 dark:hover:bg-white/10 border-slate-200/60 dark:border-transparent'
                }`}
              aria-label="Open Mobile Menu"
              aria-expanded={mobileMenuOpen}
            >
              <Menu className={`w-5 h-5 sm:w-6 sm:h-6 ${colors.textPrimary}`} />
            </button>
          </div>
        </div>
      )}

      {/* Mobile Menu Overlay & Drawer */}
      <div className="lg:hidden">
        {/* Stripped backdrop-blur-sm from the overlay to keep mobile fast! */}
        <div
          className={`fixed inset-0 bg-black/60 z-[60] transition-opacity duration-300 ease-in-out ${mobileMenuOpen ? 'opacity-100' : 'opacity-0 pointer-events-none'
            }`}
          onClick={closeMobileMenu}
          aria-hidden="true"
        />

        {/* Drawer uses Layer 1 (drawerBg) and cardBorder */}
        <div
          className={`fixed top-0 right-0 h-full w-[280px] md:w-[320px] ${colors.drawerBg} shadow-2xl z-[70] flex flex-col p-6 border-l ${colors.cardBorder} transform transition-transform duration-300 ease-in-out ${mobileMenuOpen ? 'translate-x-0' : 'translate-x-full'
            }`}
        >
          <div className={`flex justify-between items-center mb-8 pb-4 border-b ${colors.cardBorder}`}>
            <span className="text-xl font-poppins font-extrabold bg-gradient-to-r from-purple-500 to-teal-500 bg-clip-text text-transparent tracking-wide">
              Navigation
            </span>
            <button onClick={closeMobileMenu} className="p-2 rounded-full hover:bg-slate-100 dark:hover:bg-white/10 transition-colors duration-200 active:scale-90" aria-label="Close Menu">
              <X size={22} className={colors.textSecondary} />
            </button>
          </div>

          <div className="flex flex-col gap-3 flex-1 overflow-y-auto">
            {navLinks.map((link) => {
              const Icon = link.icon;
              return (
                <Link key={link.to} to={link.to} onClick={closeMobileMenu} className={`flex items-center gap-3 py-4 px-5 rounded-xl font-poppins font-semibold text-white transition-transform duration-200 ${link.color} active:scale-95 shadow-md`}>
                  <Icon size={20} />
                  {link.label}
                </Link>
              );
            })}
            {isAuthenticated && (
              <div className={`mt-4 border-t ${colors.cardBorder} pt-4`}>
                <HeaderApiKeyStatus compact={false} />
              </div>
            )}
            {showLogoutAndProfile && (
              <button onClick={handleLogout} className="flex items-center gap-3 py-4 px-5 mt-2 rounded-xl font-poppins font-semibold text-white bg-gradient-to-r from-rose-500 to-red-600 active:scale-95 transition-transform duration-200 shadow-md">
                <LogOut size={20} />
                Logout
              </button>
            )}
          </div>
        </div>
      </div>

      <main id="main-content" className={`w-full ${isFullBleedPage ? '' : layoutMaxWidth} flex-grow flex flex-col items-center justify-start relative z-10`}>
        <Suspense fallback={<PageLoader />}>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/features" element={<FeaturesGuide />} />
            <Route path="/achievements" element={<PrivateRoute><AchievementsPage /></PrivateRoute>} />
            <Route path="/journal" element={<PrivateRoute><JournalPage /></PrivateRoute>} />
            <Route path="/profile" element={<PrivateRoute><ProfilePage /></PrivateRoute>} />
            <Route path="/schedule" element={<PrivateRoute><ScheduleTab /></PrivateRoute>} />
          </Routes>
        </Suspense>
      </main>

      {!isAuthPage && (
        <footer className={`w-full ${layoutMaxWidth} flex justify-center mt-16 mb-6 relative z-10`}>
          {/* Footer pill uses Layer 2 (sectionBg) and cardBorder */}
          <div className={`px-6 py-3 rounded-full ${colors.sectionBg} border ${colors.cardBorder} shadow-sm flex items-center justify-center`}>
            <p className={`text-sm font-medium ${colors.textSecondary} tracking-wide`}>
              &copy; {new Date().getFullYear()} <span className="text-purple-600 dark:text-teal-400 font-semibold">MyMindMirror</span>. All rights reserved.
            </p>
          </div>
        </footer>
      )}
    </div>
  );
}

export default App;