import React, { useId } from 'react';
import { Link } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';

function AppLogo() {
    const { theme } = useTheme();
    const isDarkMode = theme === 'dark';

    const rawId = useId();
    const gradientId = `logo-grad-${rawId.replace(/:/g, '')}`;

    const isAuthenticated = !!localStorage.getItem('jwtToken');
    const logoLinkPath = isAuthenticated ? '/journal' : '/';

    const ringBgClass = isDarkMode ? 'bg-[#5EEAD4]/20' : 'bg-slate-300/40';
    const dotColorClass = isDarkMode ? 'bg-[#5EEAD4]' : 'bg-[#9333EA]';

    const colors = {
        // 🌟 FIX: Removed unused iconColor variable to keep code pristine

        iconGlow: isDarkMode
            ? 'drop-shadow(0 0 4px rgba(45,212,191,0.4))'
            : 'drop-shadow(0 2px 3px rgba(0,0,0,0.15))',
        textGradient: isDarkMode
            ? 'from-purple-300 via-pink-300 to-teal-300'
            : 'from-purple-600 via-pink-500 to-teal-600',
        hoverTranslate: 'hover:-translate-y-0.5',
        transition: 'transition duration-300 ease-out',
    };

    return (
        <Link
            to={logoLinkPath}
            className={`flex items-center gap-2 sm:gap-3 lg:gap-3.5 group
                        ${colors.hoverTranslate} ${colors.transition}
                        focus:outline-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2
                        ${isDarkMode
                            /* 🌟 FIX: Focus offset perfectly matches the Layer 1 Header background! */
                            ? 'focus-visible:ring-teal-500 focus-visible:ring-offset-[#1A162F]'
                            : 'focus-visible:ring-purple-500 focus-visible:ring-offset-white'}
                        rounded-full`}
            aria-label="Go to MyMindMirror Homepage or Journal"
        >
            {/* Icon Wrapper */}
            <div className="relative flex items-center justify-center">
                {isAuthenticated && (
                    <div
                        className={`absolute -inset-1 lg:-inset-1.5 rounded-full blur-md ${ringBgClass} animate-pulse-soft transition-all duration-700 group-hover:scale-110 group-hover:opacity-80`}
                        style={{ animationDuration: '3s' }}
                    />
                )}

                {/* "The Mirrored M" Custom SVG Logo */}
                <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                    className={`w-7 h-7 sm:w-8 sm:h-8 lg:w-9 lg:h-9 ${colors.transition}
                               group-hover:scale-110 relative z-10`}
                    style={{ filter: colors.iconGlow }}
                >
                    <defs>
                        <linearGradient id={gradientId} x1="0%" y1="0%" x2="100%" y2="100%">
                            <stop offset="0%" stopColor={isDarkMode ? '#D8B4FE' : '#9333EA'} />
                            <stop offset="50%" stopColor={isDarkMode ? '#F9A8D4' : '#EC4899'} />
                            <stop offset="100%" stopColor={isDarkMode ? '#5EEAD4' : '#0D9488'} />
                        </linearGradient>
                    </defs>

                    <path
                        d="M4 12V8C4 5.8 5.8 4 8 4C9.7 4 11.1 5.1 11.7 6.6L12 7.5L12.3 6.6C12.9 5.1 14.3 4 16 4C18.2 4 20 5.8 20 8V12"
                        stroke={`url(#${gradientId})`}
                        strokeWidth="2.4"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    />

                    <path
                        d="M4 12V16C4 18.2 5.8 20 8 20C9.7 20 11.1 18.9 11.7 17.4L12 16.5L12.3 17.4C12.9 18.9 14.3 20 16 20C18.2 20 20 18.2 20 16V12"
                        stroke={`url(#${gradientId})`}
                        strokeWidth="2.4"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        className="opacity-40"
                    />

                    <circle
                        cx="12"
                        cy="12"
                        r="2.2"
                        fill={`url(#${gradientId})`}
                        className="animate-pulse-soft"
                    />
                </svg>

                {isAuthenticated && (
                    <span
                        className={`absolute -top-1 -right-1 sm:-top-1 sm:-right-1.5 lg:-top-1 lg:-right-2
                                   w-2 h-2 sm:w-2.5 sm:h-2.5 ${dotColorClass} rounded-full shadow-sm animate-pulse-soft z-20
                                   ring-2 ${isDarkMode ? 'ring-[#1A162F]' : 'ring-white'}`} /* 🌟 FIX: Ring Cutout strictly matches Layer 1 Header background! */
                        style={{ animationDuration: '2s' }}
                        title="Logged in"
                    />
                )}
            </div>

            {/* Text logo */}
            <h1 className={`text-2xl sm:text-3xl lg:text-[2rem] font-poppins font-extrabold tracking-tight
                           bg-gradient-to-r ${colors.textGradient} bg-clip-text text-transparent
                           ${colors.transition} group-hover:opacity-90 group-hover:scale-[1.02]`}>
                MyMindMirror
            </h1>
        </Link>
    );
}

export default React.memo(AppLogo);