import React from 'react';
import { useTheme } from '../contexts/ThemeContext';
import { Sun, Moon } from 'lucide-react';

const ThemeToggle = () => {
    const { theme, toggleTheme } = useTheme();
    const isDarkMode = theme === 'dark';

    // 🌟 FIX: Synced completely to the Master Palette Secondary Button
    const buttonBg = isDarkMode
        ? 'bg-white/5 hover:bg-white/10 border border-white/10'
        : 'bg-slate-100 hover:bg-slate-200 border border-slate-200/80';

    return (
        <button
            onClick={toggleTheme}
            className={`p-2 sm:p-2.5 rounded-full transition-all duration-200
                        ${buttonBg}
                        shadow-sm hover:shadow-md hover:-translate-y-0.5 active:scale-95
                        focus:outline-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2
                        ${isDarkMode
                            ? 'focus-visible:ring-teal-500 focus-visible:ring-offset-[#131127]'
                            : 'focus-visible:ring-purple-500 focus-visible:ring-offset-white'}
                        flex items-center justify-center group`}
            aria-label="Toggle theme"
            title={`Switch to ${isDarkMode ? 'light' : 'dark'} mode`}
        >
            <div className="relative flex items-center justify-center w-5 h-5">
                {isDarkMode ? (
                    <Sun
                        size={20}
                        className="text-yellow-400 transition-transform duration-500 transform
                                   group-hover:rotate-90 group-hover:scale-110"
                    />
                ) : (
                    <Moon
                        size={20}
                        className="text-purple-600 transition-transform duration-500 transform
                                   group-hover:-rotate-12 group-hover:scale-110"
                    />
                )}
            </div>
        </button>
    );
};

export default React.memo(ThemeToggle);