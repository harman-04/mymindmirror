import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { Key, AlertTriangle, Settings, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { useTheme } from '../contexts/ThemeContext';
import { useApiKeyStatus, useUpdateApiKey } from '../hooks/useUserProfile';

function HeaderApiKeyStatus({ compact = true }) {
    const { theme } = useTheme();
    const isDarkMode = theme === 'dark';
    const { data, isLoading, error } = useApiKeyStatus();
    const updateApiKeyMutation = useUpdateApiKey();

    const [isOpen, setIsOpen] = useState(false);
    const [clearing, setClearing] = useState(false);
    const triggerRef = useRef(null);
    const popoverRef = useRef(null);

    // Close popover on outside click or Escape
    useEffect(() => {
        if (!isOpen) return;

        const handleClickOutside = (e) => {
            if (triggerRef.current?.contains(e.target)) return;
            if (popoverRef.current?.contains(e.target)) return;
            setIsOpen(false);
        };
        const handleEscape = (e) => {
            if (e.key === 'Escape') setIsOpen(false);
        };

        document.addEventListener('mousedown', handleClickOutside);
        document.addEventListener('keydown', handleEscape);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.removeEventListener('keydown', handleEscape);
        };
    }, [isOpen]);

    if (isLoading || error || !data) return null;

    const isUsingOwnKey = data.usingOwnKey;
    const label = isUsingOwnKey ? 'Own key' : 'Shared key';
    const tooltip = isUsingOwnKey
        ? 'Using your own Gemini API key — click for options'
        : 'Using shared key — click for options';

    const iconColor = isUsingOwnKey ? 'text-emerald-500' : 'text-amber-500';
    const textColor = isDarkMode ? 'text-gray-400' : 'text-slate-500';

    const handleClearKey = async () => {
        setClearing(true);
        try {
            await updateApiKeyMutation.mutateAsync('');
            toast.success('API key cleared. Falling back to the shared key.');
            setIsOpen(false);
        } catch (err) {
            toast.error('Failed to clear API key. Please try again.');
        } finally {
            setClearing(false);
        }
    };

    // Mobile drawer variant — unchanged, always visible
    if (!compact) {
        const colorClasses = isUsingOwnKey
            ? isDarkMode
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                : 'bg-emerald-50 text-emerald-700 border-emerald-200/80'
            : isDarkMode
                ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                : 'bg-amber-50 text-amber-700 border-amber-200/80';
        const icon = isUsingOwnKey ? <Key size={20} /> : <AlertTriangle size={20} />;

        return (
            <div
                className={`flex items-center gap-3 py-4 px-5 rounded-xl font-poppins font-semibold border w-full ${colorClasses}`}
                title={tooltip}
            >
                {icon}
                <span>{label}</span>
            </div>
        );
    }

    // Desktop variant — clickable trigger + popover
    return (
        <div className="relative">
            <button
                ref={triggerRef}
                type="button"
                onClick={() => setIsOpen((prev) => !prev)}
                className={`flex items-center gap-1.5 px-3 py-2 rounded-full transition-colors duration-200 hover:bg-slate-100 dark:hover:bg-white/5 ${textColor}`}
                aria-haspopup="true"
                aria-expanded={isOpen}
                title={tooltip}
            >
                <Key className={`w-3.5 h-3.5 ${iconColor}`} />
                <span className="text-sm font-semibold">{label}</span>
            </button>

            {isOpen && (
                <div
                    ref={popoverRef}
                    role="menu"
                    className={`absolute right-0 top-full mt-2 w-64 rounded-xl border shadow-xl z-50 overflow-hidden ${isDarkMode
                            ? 'bg-[#1A162F] border-white/10'
                            : 'bg-white border-slate-200/80'
                        }`}
                >
                    {/* Header */}
                    <div
                        className={`p-4 border-b ${isDarkMode ? 'border-white/10' : 'border-slate-200/60'
                            }`}
                    >
                        <div className="flex items-center gap-2 mb-1.5">
                            <Key className={`w-4 h-4 ${iconColor}`} />
                            <span
                                className={`text-sm font-bold ${isDarkMode ? 'text-gray-100' : 'text-slate-900'
                                    }`}
                            >
                                {isUsingOwnKey ? 'Own API key active' : 'Shared API key'}
                            </span>
                        </div>
                        {isUsingOwnKey && data.maskedKey && (
                            <p className={`text-xs font-mono ${textColor} truncate mb-2`}>
                                {data.maskedKey}
                            </p>
                        )}
                        <p className={`text-xs ${textColor} leading-relaxed`}>
                            {isUsingOwnKey
                                ? 'Your requests use your own Google AI quota.'
                                : 'Add your own key to bypass shared limits.'}
                        </p>
                    </div>

                    {/* Actions */}
                    <div className="p-1.5">
                        <Link
                            to="/profile#api-key"
                            onClick={() => setIsOpen(false)}
                            role="menuitem"
                            className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-semibold transition-colors ${isDarkMode
                                    ? 'text-gray-200 hover:bg-white/5'
                                    : 'text-slate-700 hover:bg-slate-100'
                                }`}
                        >
                            <Settings className="w-4 h-4" />
                            Manage in Profile
                        </Link>

                        {isUsingOwnKey && (
                            <button
                                type="button"
                                onClick={handleClearKey}
                                disabled={clearing}
                                role="menuitem"
                                className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm font-semibold transition-colors disabled:opacity-50 ${isDarkMode
                                        ? 'text-rose-400 hover:bg-rose-500/10'
                                        : 'text-rose-600 hover:bg-rose-50'
                                    }`}
                            >
                                <Trash2 className="w-4 h-4" />
                                {clearing ? 'Clearing…' : 'Clear Key'}
                            </button>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

export default React.memo(HeaderApiKeyStatus);