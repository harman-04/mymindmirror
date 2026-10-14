import React, { useState, forwardRef, useImperativeHandle } from 'react';
import { useAddJournalEntry } from '../hooks/useJournalData';
import { useTheme } from '../contexts/ThemeContext';
import { Sparkles, NotebookPen, Loader2 } from 'lucide-react';
import PremiumInput from './PremiumInput';
import { toast } from 'sonner';

const JournalInput = forwardRef((props, ref) => {
    const [text, setText] = useState('');
    const { theme } = useTheme();
    const isDarkMode = theme === 'dark';
    const addEntryMutation = useAddJournalEntry();

    // ==========================================================================
    //  MASTER ELEVATION PALETTE (Single Source of Truth)
    // ==========================================================================
    const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
    const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
    const sectionBg = isDarkMode ? 'bg-[#131127]/80 shadow-inner' : 'bg-slate-50/80 shadow-inner';
    const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
    const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';

    const handleSubmit = async (e) => {
        if (e) e.preventDefault();

        if (!text.trim()) {
            toast.error('Journal entry cannot be empty.');
            return;
        }

        try {
            await addEntryMutation.mutateAsync({ rawText: text });
            toast.success('Entry saved and analyzed successfully!');
            setText('');
        } catch (err) {
        // handle by interpreter axios config
        }
    };

    const handleKeyDown = (e) => {
        // Submit on Ctrl+Enter (Mac: Cmd+Enter)
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            e.preventDefault();
            handleSubmit(e);
        }
    };

    useImperativeHandle(ref, () => ({
        setText: (newText) => setText(newText),
        clearText: () => setText('')
    }));

    return (
        <form
            onSubmit={handleSubmit}
            data-testid="journal-input-form"
            className={`flex flex-col space-y-4 lg:space-y-5 p-4 sm:p-6 lg:p-8 rounded-2xl lg:rounded-3xl border ${cardBg} ${cardBorder} shadow-lg ring-1 ring-black/5 dark:ring-white/5 transition-shadow duration-300 hover:shadow-xl`}
        >
            <div className="flex items-center gap-3 lg:gap-4 mb-2 lg:mb-3">
                <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-purple-50 dark:from-teal-900/40 dark:to-teal-800/20 text-purple-600 dark:text-teal-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-teal-700/30">
                    <NotebookPen className="w-5 h-5 lg:w-6 lg:h-6" />
                </div>
                <div>
                    <h2 className={`text-lg sm:text-xl lg:text-2xl font-poppins font-extrabold ${textPrimary} tracking-tight leading-none`}>
                        What's on your mind today?
                    </h2>
                </div>
            </div>

            <div className="pt-2">
                <PremiumInput
                    multiline={true}
                    rows={window.innerWidth < 640 ? 5 : 7}
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="Write your thoughts here... Press Ctrl+Enter to save."
                    disabled={addEntryMutation.isPending}
                    focusRingClass="focus:border-[#FF8A7A] focus:ring-[#FF8A7A]/30 dark:focus:border-[#FF8A7A] dark:focus:ring-[#FF8A7A]/20"
                    data-testid="journal-input-textarea"
                />
            </div>

            <div className="flex flex-wrap items-center justify-end gap-3 lg:gap-4 pt-2 lg:pt-3">
                <button
                    type="submit"
                    data-testid="journal-input-submit"
                    className="flex items-center justify-center gap-2 rounded-full font-poppins font-semibold text-white
                               bg-gradient-to-r from-[#FF8A7A] to-[#FF6C5A] hover:from-[#FF7B6A] hover:to-[#FF5C4A]
                               active:scale-95 shadow-md hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-[#FF8A7A] focus:ring-opacity-75
                               transition-all duration-200 shrink-0
                               disabled:opacity-60 disabled:cursor-not-allowed disabled:active:scale-100
                               py-2.5 sm:py-3 px-6 sm:px-8 text-sm sm:text-base"
                    disabled={addEntryMutation.isPending || !text.trim()}
                >
                    {addEntryMutation.isPending ? (
                        <>
                            <Loader2 size={18} className="animate-spin sm:w-5 sm:h-5" />
                            <span>Analyzing...</span>
                        </>
                    ) : (
                        <>
                            <Sparkles size={18} className="sm:w-5 sm:h-5" />
                            <span>Analyze & Save</span>
                        </>
                    )}
                </button>
            </div>
        </form>
    );
});

export default React.memo(JournalInput);