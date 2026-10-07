import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import { useRegister } from '../hooks/useAuth';
import { toast } from 'sonner';
import PremiumInput from '../components/PremiumInput';

import {
  Sparkles, Mail, Lock, User, ArrowRight, ChevronLeft, Key
} from 'lucide-react';
import FadeIn from '../components/FadeIn';

function RegisterPage() {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [formErrors, setFormErrors] = useState({});
  const [imageLoaded, setImageLoaded] = useState(false);

  const navigate = useNavigate();
  const registerMutation = useRegister();

  const getPasswordStrength = (pwd) => {
    let score = 0;
    if (!pwd) return { score: 0, label: '', color: '' };
    if (pwd.length >= 8) score++;
    if (pwd.match(/[a-z]/) && pwd.match(/[A-Z]/)) score++;
    if (pwd.match(/\d/)) score++;
    if (pwd.match(/[^a-zA-Z\d]/)) score++;
    const strengths = ['', 'Weak', 'Fair', 'Good', 'Strong'];
    const colors = [
      '', 'bg-red-500', 'bg-amber-500', 'bg-blue-500', 'bg-emerald-500'
    ];
    return {
      score,
      label: strengths[score] || '',
      color: colors[score] || 'bg-gray-500'
    };
  };
  const strength = getPasswordStrength(password);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormErrors({});

let errors = {};
if (!username.trim()) {
    errors.username = 'Username is required.';
} else if (username.trim().length < 3 || username.trim().length > 50) {
    errors.username = 'Username must be between 3 and 50 characters.';
}
if (!email.trim() || !/\S+@\S+\.\S+/.test(email)) {
    errors.email = 'Valid email is required.';
} else if (email.trim().length > 100) {
    errors.email = 'Email must not exceed 100 characters.';
}
if (password.length < 6) {
    errors.password = 'Password must be at least 6 characters.';
} else if (password.length > 72) {
    errors.password = 'Password must not exceed 72 characters.';
}
if (password !== confirmPassword) errors.confirmPassword = 'Passwords do not match.';

    if (Object.keys(errors).length > 0) {
        setFormErrors(errors);
        return;
    }

    try {
      const data = await registerMutation.mutateAsync({ username, email, password });
      toast.success(data.message || 'Registration successful! You can now login.');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      console.error('Registration error:', err);
//       toast.error(err.response?.data?.message || 'Registration failed. Please try again.');
    }
  };

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (3-Layer Architecture)
  // ==========================================================================
const colors = {
    cardBg: isDarkMode ? 'bg-[#1A162F]/95' : 'bg-white/95',
    cardBorder: isDarkMode ? 'border-white/10' : 'border-slate-200/80',
    textPrimary: isDarkMode ? 'text-gray-100' : 'text-slate-900',
    textSecondary: isDarkMode ? 'text-gray-400' : 'text-slate-500',
    buttonPrimary: 'bg-gradient-to-r from-purple-500 to-teal-500 hover:from-purple-600 hover:to-teal-600 shadow-md hover:shadow-lg hover:-translate-y-0.5 active:scale-95 text-white transition-all duration-300',
  };

  return (
      <div className={`min-h-screen w-full flex items-center justify-center p-4 sm:p-6 lg:p-8 ${colors.textPrimary}`}>

         {/* Desktop Back Button */}
         {/* 🌟 FIX: Synced to Master Palette colors, border, and stripped blur! */}
         <Link to="/" className={`hidden lg:inline-flex fixed top-6 left-6 lg:top-10 lg:left-10 items-center gap-2 ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 transition-all duration-300 font-bold z-50 ${colors.cardBg} px-5 py-2.5 rounded-full border ${colors.cardBorder} shadow-sm hover:shadow-md active:scale-95`}>
             <ChevronLeft className="w-4 h-4" /> Home
         </Link>

        <FadeIn direction="up" delay={0.1} className="w-full max-w-5xl z-10 lg:max-h-[90vh] flex">
          {/* 🌟 FIX: Removed scroll-lagging backdrop-blur-xl */}
          <div className={`w-full flex flex-col lg:flex-row rounded-[2rem] lg:rounded-[2.5rem] ${colors.cardBg} border ${colors.cardBorder} shadow-2xl overflow-hidden transition-all duration-300`}>

              {/* LEFT SIDE: Lush Image Panel */}
              {/* 🌟 FIX: Synced internal border to cardBorder */}
              <div className={`hidden lg:flex relative w-1/2 overflow-hidden items-center justify-center group border-r ${colors.cardBorder}`}>

                  <div className={`absolute inset-0 bg-gradient-to-br ${isDarkMode ? 'from-purple-900/40 to-teal-900/40' : 'from-purple-200/50 to-teal-200/50'} animate-pulse`}></div>

                  <img
                      src={isDarkMode ? "/login-bg-dark.webp" : "/login-bg-light.webp"}
                      alt="Abstract glowing fluid"
                      onLoad={() => setImageLoaded(true)}
                      className={`absolute inset-0 w-full h-full object-cover transform group-hover:scale-105 transition-all duration-1000 ease-out
                                 ${imageLoaded ? 'opacity-100 scale-100' : 'opacity-0 scale-110'}`}
                    />
                  <div className={`absolute inset-0 bg-gradient-to-t to-transparent ${isDarkMode ? 'from-black/90 via-black/30' : 'from-white/90 via-white/30'}`}></div>

                  <div className="relative z-10 p-10 lg:p-12 mt-auto w-full">
                     <div className={`inline-flex items-center gap-3 mb-4 p-3 backdrop-blur-md rounded-2xl border ${isDarkMode ? 'bg-white/10 border-white/20' : 'bg-slate-900/5 border-slate-900/10'}`}>
                        <Sparkles className={`w-6 h-6 ${isDarkMode ? 'text-teal-300' : 'text-teal-600'}`} />
                        <span className={`font-poppins font-bold tracking-widest uppercase text-sm ${isDarkMode ? 'text-white' : 'text-slate-800'}`}>MyMindMirror</span>
                     </div>

                     <h1 className={`text-4xl lg:text-5xl font-poppins font-extrabold leading-tight mb-4 drop-shadow-sm ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>
                       Start Your<br/>Journey.
                     </h1>
                     <p className={`text-base font-medium leading-relaxed max-w-sm drop-shadow-sm ${isDarkMode ? 'text-gray-200' : 'text-slate-600'}`}>
                       Create an account today to unlock AI-powered reflections and map out your personal growth.
                     </p>
                  </div>
              </div>

              {/* RIGHT SIDE: The Registration Form */}
              {/* 🌟 FIX: Applied sectionBg (Layer 2) and changed 'scrollbar-hide' to 'custom-scrollbar' for premium overflow handling! */}
{/* 🌟 FIX: Removed sectionBg so it acts as Layer 1 */}
<div className="w-full lg:w-1/2 flex flex-col items-center p-6 sm:p-8 lg:px-10 lg:py-8 relative z-0 overflow-y-auto custom-scrollbar">
                    <div className="w-full max-w-sm my-auto py-4">

                        {/* Mobile-only inline Back Button */}
                        {/* 🌟 FIX: Synced to textSecondary */}
                        <Link to="/" className={`lg:hidden inline-flex items-center gap-1.5 ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 transition-colors font-bold mb-6 text-sm`}>
                            <ChevronLeft className="w-4 h-4" /> Back to Home
                        </Link>

                        <div className="text-center mb-6 lg:mb-8">
                            <h2 className="text-2xl lg:text-3xl font-poppins font-extrabold bg-gradient-to-r from-purple-600 to-teal-500 dark:from-purple-400 dark:to-teal-400 bg-clip-text text-transparent tracking-tight">
                                Create Account
                            </h2>
                            <p className={`text-sm lg:text-base font-medium ${colors.textSecondary} mt-2`}>
                                Join us and start reflecting today.
                            </p>
                        </div>

                        <form onSubmit={handleSubmit} className="space-y-4 lg:space-y-5" noValidate>

                            {/* 🌟 FIX: We explicitly pass inputBg (Layer 3) to pop off the Layer 2 form background! */}
                            <PremiumInput
                                icon={User}
                                label="Username"
                                value={username}
                                onChange={(e) => {
                                    setUsername(e.target.value);
                                    if (formErrors.username) setFormErrors(prev => ({ ...prev, username: null }));
                                }}
                                placeholder="johndoe"
                                error={formErrors.username}
                                showError={!!formErrors.username}
                                disabled={registerMutation.isPending}
                            />

                            <PremiumInput
                                type="email"
                                icon={Mail}
                                label="Email"
                                value={email}
                                onChange={(e) => {
                                    setEmail(e.target.value);
                                    if (formErrors.email) setFormErrors(prev => ({ ...prev, email: null }));
                                }}
                                placeholder="you@example.com"
                                error={formErrors.email}
                                showError={!!formErrors.email}
                                disabled={registerMutation.isPending}
                            />

                            <div>
                                <PremiumInput
                                    type="password"
                                    icon={Lock}
                                    label="Password"
                                    value={password}
                                    onChange={(e) => {
                                        setPassword(e.target.value);
                                        if (formErrors.password) setFormErrors(prev => ({ ...prev, password: null }));
                                    }}
                                    placeholder="••••••••"
                                    error={formErrors.password}
                                    showError={!!formErrors.password}
                                    disabled={registerMutation.isPending}
                                />

                                {password && (
                                  <div className="mt-2 space-y-1 animate-in fade-in slide-in-from-top-1 px-1">
                                    <div className="flex items-center gap-3 text-xs font-medium">
                                      <span className={colors.textSecondary}>Strength:</span>
                                      {/* 🌟 FIX: Synced Password track background to Layer 3 so it punches in! */}
{/* 🌟 FIX: Synced Password track back to Layer 2 to match the inputs */}
<div className="flex-1 h-1.5 bg-slate-50 dark:bg-[#131127] rounded-full overflow-hidden border border-slate-300 dark:border-white/10 shadow-inner">
                                        <div className={`h-full ${strength.color} transition-all duration-300 shadow-sm`} style={{ width: `${(strength.score / 4) * 100}%` }} />
                                      </div>
                                      <span className={`text-[10px] font-bold uppercase tracking-wider ${
                                        strength.score === 1 ? 'text-red-500' :
                                        strength.score === 2 ? 'text-amber-500' :
                                        strength.score === 3 ? 'text-blue-500' :
                                        strength.score === 4 ? 'text-emerald-500' : colors.textSecondary
                                      }`}>
                                        {strength.label}
                                      </span>
                                    </div>
                                  </div>
                                )}
                            </div>

                            <PremiumInput
                                type="password"
                                icon={Key}
                                label="Confirm Password"
                                value={confirmPassword}
                                onChange={(e) => {
                                    setConfirmPassword(e.target.value);
                                    if (formErrors.confirmPassword) setFormErrors(prev => ({ ...prev, confirmPassword: null }));
                                }}
                                placeholder="••••••••"
                                error={formErrors.confirmPassword}
                                showError={!!formErrors.confirmPassword}
                                disabled={registerMutation.isPending}
                            />

                            <button
                                type="submit"
                                disabled={registerMutation.isPending}
                                className={`w-full mt-4 py-3.5 lg:py-4 rounded-xl lg:rounded-2xl font-bold text-sm lg:text-base flex items-center justify-center gap-2 ${colors.buttonPrimary} group`}
                            >
                                {registerMutation.isPending ? (
                                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                ) : (
                                <>
                                    Create Account <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                                </>
                                )}
                            </button>
                        </form>

                        {/* 🌟 FIX: Applied sectionBorder to the divider */}
{/* 🌟 FIX: Applied the subtle border color explicitly */}
<div className="mt-6 lg:mt-8 pt-4 lg:pt-6 border-t border-slate-200/60 dark:border-white/10 text-center">                            <p className={`text-sm font-medium ${colors.textSecondary}`}>
                            Already have an account?{' '}
                            <Link to="/login" className="text-purple-600 dark:text-teal-400 hover:text-purple-700 dark:hover:text-teal-300 font-bold transition-colors">
                                Login here
                            </Link>
                            </p>
                        </div>
                    </div>
                </div>
          </div>
        </FadeIn>
      </div>
    );
}

export default RegisterPage;