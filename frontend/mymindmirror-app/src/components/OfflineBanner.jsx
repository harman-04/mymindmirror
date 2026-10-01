import React, { useEffect, useState } from 'react';
import { WifiOff } from 'lucide-react';
import { toast } from 'sonner';
import { useTheme } from '../contexts/ThemeContext';

export default function OfflineBanner() {
    const [isOnline, setIsOnline] = useState(navigator.onLine);
    const { theme } = useTheme();
    const isDarkMode = theme === 'dark';

    useEffect(() => {
        const handleOnline = () => {
            setIsOnline(true);
            toast.success('Back online', {
                description: 'Reconnected. Loading your data...',
                duration: 2000,
            });
        };
        const handleOffline = () => setIsOnline(false);

        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);

        return () => {
            window.removeEventListener('online', handleOnline);
            window.removeEventListener('offline', handleOffline);
        };
    }, []);

    if (isOnline) return null;

    return (
            // 🌟 Native CSS animation applied!
            <div
                className={`fixed top-0 left-0 right-0 z-[9999] py-3 px-4 flex items-center justify-center gap-3 text-sm font-poppins font-semibold shadow-lg animate-slide-down-fade
                    ${isDarkMode
                        ? 'bg-[#1A162F]/95 text-amber-400 border-b border-amber-500/20'
                        : 'bg-amber-50 text-amber-700 border-b border-amber-200/80'}`}
            >
                <div className={`p-1.5 rounded-full ${isDarkMode ? 'bg-amber-500/10' : 'bg-amber-200/50'}`}>
                    <WifiOff className="w-4 h-4 animate-pulse" />
                </div>
                <span className="tracking-wide">
                    You're offline. Some actions may not respond until connection returns.
                </span>
            </div>
        );
}