// src/contexts/ThemeContext.jsx

import React, { createContext, useContext, useState, useEffect, useMemo } from 'react';

// 1. Create the Context
export const ThemeContext = createContext(null);

// 2. Create the custom hook
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (context === undefined || context === null) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

// 3. Create the Provider Component
export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState(() => {
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) return savedTheme;
    // Fallback to system preference
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  // 💡 UPGRADE 1: Real-time System Theme Syncing
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

    const handleSystemThemeChange = (e) => {
      // Only auto-switch if the user hasn't explicitly set a preference in localStorage
      if (!localStorage.getItem('theme')) {
        setTheme(e.matches ? 'dark' : 'light');
      }
    };

    // Listen for OS-level theme changes (Mac/Windows)
    mediaQuery.addEventListener('change', handleSystemThemeChange);
    return () => mediaQuery.removeEventListener('change', handleSystemThemeChange);
  }, []);

  // 💡 UPGRADE 2: DOM Manipulation & Transition Fix
//   useEffect(() => {
//     const root = window.document.documentElement;
//
//     // Swap classes instantly
//     root.classList.remove('light', 'dark');
//     root.classList.add(theme);
//
//     // Save preference
//     localStorage.setItem('theme', theme);
//
//     // Add transitions AFTER initial load to prevent a "flashing" screen on reload
//     const timeoutId = setTimeout(() => {
//         root.classList.add('transition-colors', 'duration-500');
//     }, 50);
//
//     return () => clearTimeout(timeoutId);
//   }, [theme]);

// 💡 UPGRADE 2: DOM Manipulation & Transition Fix
  useEffect(() => {
    const root = window.document.documentElement;

    // Swap classes instantly
    root.classList.remove('light', 'dark');
    root.classList.add(theme);

    // 🌟 ENTERPRISE FIX: Native Browser Element Sync
    // Forces the browser's native engine (scrollbars, input carets, select arrows) to instantly match the theme
    root.style.colorScheme = theme;

    // Save preference
    localStorage.setItem('theme', theme);

    // Add transitions AFTER initial load to prevent a "flashing" screen on reload
    const timeoutId = setTimeout(() => {
        // 🌟 SAFE: transition-colors ensures ONLY colors animate across the app, protecting global layout
        root.classList.add('transition-colors', 'duration-500');
    }, 50);

    return () => clearTimeout(timeoutId);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prevTheme) => (prevTheme === 'light' ? 'dark' : 'light'));
  };

  // 💡 UPGRADE 3: Memoize the context value for massive global app performance
  const contextValue = useMemo(() => ({ theme, toggleTheme }), [theme]);

  return (
    <ThemeContext.Provider value={contextValue}>
      {children}
    </ThemeContext.Provider>
  );
};