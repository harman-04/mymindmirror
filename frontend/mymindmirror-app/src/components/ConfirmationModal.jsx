// src/components/ConfirmationModal.jsx
import React from 'react';
import { XCircle, AlertTriangle, Loader } from 'lucide-react';
import { useTheme } from '../contexts/ThemeContext';

function ConfirmationModal({ isOpen, onClose, onConfirm, title, message, confirmText, cancelText, isDestructive = false, isLoading = false }) {
    const { theme } = useTheme();
    const isDarkMode = theme === 'dark';

    if (!isOpen) return null;

    // ==========================================================================
    // 🌟 MASTER ELEVATION PALETTE (Modal/Layer 1 Architecture)
    // ==========================================================================
    const colors = {
        overlayBg: 'bg-black/60', // Crisp, non-blurred overlay to protect GPU

        // 🌟 FIX: Modals float above the page, so they must be Layer 1 (cardBg)
        modalBg: isDarkMode ? 'bg-[#1A162F]/95' : 'bg-white/95',
        modalBorder: isDarkMode ? 'border-white/10' : 'border-slate-200/80',

        // Typography synced to Master Palette
        textColor: isDarkMode ? 'text-gray-100' : 'text-slate-900',
        textSecondary: isDarkMode ? 'text-gray-400' : 'text-slate-500',

        // 🌟 FIX: Destructive synced to ProfilePage "Danger Zone" gradients
        buttonPrimary: isDestructive
            ? 'bg-gradient-to-r from-red-500 to-rose-600 hover:from-red-600 hover:to-rose-700'
            : 'bg-gradient-to-r from-purple-500 to-teal-500 hover:from-purple-600 hover:to-teal-600',

        // 🌟 FIX: Secondary button uses slate-100 for better contrast on white modal
        buttonSecondary: isDarkMode
            ? 'bg-white/5 hover:bg-white/10 text-gray-200 border border-white/10'
            : 'bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200/80',

        buttonText: 'text-white',

        // 🌟 FIX: Synced icons to Master Palette
        iconBg: isDestructive ? 'bg-red-50 dark:bg-red-900/30' : 'bg-purple-50 dark:bg-teal-900/30',
        iconColor: isDestructive ? 'text-red-500 dark:text-red-400' : 'text-purple-600 dark:text-teal-400',
        shadow: 'shadow-2xl shadow-black/20',
    };

    return (
        <div className={`fixed inset-0 z-[200] flex items-center justify-center p-4 ${colors.overlayBg} transition-opacity duration-300`}>
            {/* Click outside to close (disabled if loading) */}
            <div className="absolute inset-0" onClick={!isLoading ? onClose : undefined} aria-hidden="true" />

            <div className={`relative p-6 lg:p-8 rounded-2xl lg:rounded-3xl ${colors.modalBg} border ${colors.modalBorder} ${colors.shadow}
                             max-w-md w-full text-center transform opacity-0 animate-scale-in`}>

                <div className={`mx-auto w-14 h-14 lg:w-16 lg:h-16 mb-4 lg:mb-6 rounded-full flex items-center justify-center border ${isDarkMode ? 'border-white/5' : 'border-black/5'} ${colors.iconBg}`}>
                    {isDestructive ? (
                        <XCircle className={`w-7 h-7 lg:w-8 lg:h-8 ${colors.iconColor}`} />
                    ) : (
                        <AlertTriangle className={`w-7 h-7 lg:w-8 lg:h-8 ${colors.iconColor}`} />
                    )}
                </div>

                <h2 className={`text-xl lg:text-2xl font-poppins font-bold tracking-tight mb-2 ${colors.textColor}`}>
                    {title}
                </h2>
                <p className={`text-sm lg:text-base font-medium mb-6 lg:mb-8 ${colors.textSecondary}`}>
                    {message}
                </p>

                <div className="flex flex-col sm:flex-row justify-center gap-3 mt-2">
                    <button
                        onClick={onClose}
                        className={`flex-1 py-2.5 lg:py-3 px-6 rounded-full font-bold text-sm lg:text-base ${colors.buttonSecondary}
                                   transition-all duration-200 active:scale-95 disabled:opacity-50`}
                        disabled={isLoading}
                    >
                        {cancelText || 'Cancel'}
                    </button>
                    <button
                        onClick={onConfirm}
                        className={`flex-1 py-2.5 lg:py-3 px-6 rounded-full font-bold text-sm lg:text-base ${colors.buttonPrimary} ${colors.buttonText}
                                   transition-all duration-200 active:scale-95 shadow-sm hover:shadow-md disabled:opacity-70 flex justify-center items-center`}
                        disabled={isLoading}
                    >
                        {isLoading ? (
                            <span className="flex items-center gap-2">
                                <Loader className="w-4 h-4 lg:w-5 lg:h-5 animate-spin" /> {confirmText === 'Confirm' ? 'Processing...' : (confirmText || 'Processing...')}
                            </span>
                        ) : (
                            confirmText || 'Confirm'
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ConfirmationModal;