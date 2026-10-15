import React, { useId, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Eye, EyeOff } from 'lucide-react';

const PremiumInput = ({
    label,
    type = 'text',
    value,
    onChange,
    placeholder,
    error,
    showError,
    required = false,
    multiline = false,
    rows = 3,
    className = '',
    icon: Icon,
    focusRingClass = 'focus:ring-purple-500 focus:border-purple-500 dark:focus:ring-teal-400 dark:focus:border-teal-400',
    // 🌟 UX UPGRADE: Added a prop to override the background depending on the Elevation Layer!
    inputBgClass = 'bg-slate-50 dark:bg-[#131127]',
    ...props
}) => {
    const id = useId();
    const [showPassword, setShowPassword] = useState(false);
    const hasError = showError && error;

    const baseInputClasses = `w-full p-3 lg:p-4 rounded-xl border focus:outline-none transition-all duration-200 text-sm lg:text-base shadow-sm ${className}`;

    // 🌟 FIX: Uses the dynamic layer background instead of hardcoding it
    const defaultStateClasses = `${inputBgClass} border-slate-300 dark:border-white/10 text-slate-900 dark:text-gray-100 focus:ring-2 ${focusRingClass}`;

    const errorStateClasses = 'bg-rose-50/50 dark:bg-rose-900/20 border-rose-500 dark:border-rose-500/50 text-rose-900 dark:text-rose-200 focus:ring-2 focus:ring-rose-500 focus:border-rose-500 placeholder-rose-300 dark:placeholder-rose-700/50';

    const inputClasses = `${baseInputClasses} ${hasError ? errorStateClasses : defaultStateClasses} ${Icon ? 'pl-10 lg:pl-12' : ''} ${type === 'password' ? 'pr-10 lg:pr-12' : ''}`;
    const actualType = type === 'password' ? (showPassword ? 'text' : 'password') : type;

    return (
        <div className="w-full space-y-1.5 lg:space-y-2">
            {label && (
                <label htmlFor={id} className={`block text-xs lg:text-sm font-bold uppercase tracking-wider transition-colors ${hasError ? 'text-rose-500 dark:text-rose-400' : 'text-slate-500 dark:text-gray-400'}`}>
                    {label} {required && <span className="text-rose-500">*</span>}
                </label>
            )}

            <div className="relative">
                {Icon && (
                    <div className={`absolute left-3 lg:left-4 top-1/2 -translate-y-1/2 transition-colors ${hasError ? 'text-rose-500' : 'text-slate-400 dark:text-gray-500'}`}>
                        <Icon className="w-4 h-4 lg:w-5 lg:h-5" />
                    </div>
                )}

                {multiline ? (
                    <textarea
                        id={id}
                        value={value}
                        onChange={onChange}
                        placeholder={placeholder}
                        rows={rows}
                        className={`${inputClasses} resize-y`}
                        {...props}
                    />
                ) : (
                    <>
                        <input
                            id={id}
                            type={actualType}
                            value={value}
                            onChange={onChange}
                            placeholder={placeholder}
                            className={inputClasses}
                            {...props}
                        />
                        {type === 'password' && (
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className={`absolute right-3 lg:right-4 top-1/2 -translate-y-1/2 transition-colors ${hasError ? 'text-rose-500 hover:text-rose-600' : 'text-slate-400 hover:text-slate-600 dark:text-gray-500 dark:hover:text-gray-300'}`}
                                tabIndex="-1"
                            >
                                {showPassword ? <EyeOff className="w-4 h-4 lg:w-5 lg:h-5" /> : <Eye className="w-4 h-4 lg:w-5 lg:h-5" />}
                            </button>
                        )}
                    </>
                )}
            </div>

            <AnimatePresence>
                {hasError && (
                    <motion.div
                        initial={{ opacity: 0, y: -5, height: 0 }}
                        animate={{ opacity: 1, y: 0, height: 'auto' }}
                        exit={{ opacity: 0, y: -5, height: 0 }}
                        transition={{ duration: 0.2 }}
                        className="text-rose-500 dark:text-rose-400 text-xs lg:text-sm font-bold flex items-center gap-1.5 overflow-hidden pt-0.5"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4 shrink-0">
                            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-5a.75.75 0 01.75.75v4.5a.75.75 0 01-1.5 0v-4.5A.75.75 0 0110 5zm0 10a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
                        </svg>
                        {error}
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default React.memo(PremiumInput);