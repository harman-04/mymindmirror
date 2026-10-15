// src/components/InstallPrompt.jsx
import React, { useState, useEffect } from 'react';
import { Download, X } from 'lucide-react';
import { useTheme } from '../contexts/ThemeContext';

function InstallPrompt() {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  const [deferredPrompt, setDeferredPrompt] = useState(null);
  const [showInstall, setShowInstall] = useState(false);

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Layer 1 Floating Card)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95' : 'bg-white/95';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  useEffect(() => {
    const handler = (e) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setShowInstall(true);
    };

    window.addEventListener('beforeinstallprompt', handler);
    return () => window.removeEventListener('beforeinstallprompt', handler);
  }, []);

  const handleInstall = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      if (outcome === 'accepted') {
        setDeferredPrompt(null);
        setShowInstall(false);
      }
    }
  };

  const handleDismiss = () => {
    setShowInstall(false);
  };

  if (!showInstall) return null;

  return (
    <div className="fixed bottom-6 right-6 z-50 animate-fade-in-up">
      <div className={`p-1.5 pl-4 pr-1.5 rounded-full border shadow-xl flex items-center gap-3 transition duration-300 ${cardBg} ${cardBorder}`}>
        <div className="flex flex-col">
          <span className={`text-sm font-semibold font-poppins ${textPrimary}`}>
            Install App
          </span>
          <span className={`text-[10px] font-medium ${textSecondary}`}>
            Add to home screen
          </span>
        </div>

        <div className="flex items-center gap-1">
          <button
            onClick={handleInstall}
            className="flex items-center justify-center p-2.5 rounded-full bg-gradient-to-r from-purple-500 to-teal-500 text-white hover:-translate-y-0.5 active:scale-95 transition-all duration-200 shadow-sm hover:shadow-md"
            title="Install"
          >
            <Download size={16} />
          </button>

          <button
            onClick={handleDismiss}
            className={`p-2 rounded-full ${textSecondary} hover:bg-slate-100 dark:hover:bg-white/10 hover:${textPrimary} transition-all duration-200 active:scale-95`}
            title="Dismiss"
          >
            <X size={16} />
          </button>
        </div>
      </div>
    </div>
  );
}

export default React.memo(InstallPrompt);