import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import { useLogin } from '../hooks/useAuth';
import { toast } from 'sonner';
import PremiumInput from '../components/PremiumInput';
import { Lock, User, ArrowRight, ChevronLeft, Sparkles } from 'lucide-react';
import FadeIn from '../components/FadeIn';

function LoginPage() {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [formErrors, setFormErrors] = useState({});
  const [imageLoaded, setImageLoaded] = useState(false);

  const navigate = useNavigate();
  const loginMutation = useLogin();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormErrors({});

    let errors = {};
    if (!username.trim()) errors.username = 'Username is required.';
    if (!password) errors.password = 'Password is required.';

    if (Object.keys(errors).length > 0) {
        setFormErrors(errors);
        return;
    }

    try {
      const data = await loginMutation.mutateAsync({ username, password });
      toast.success('Login successful! Welcome back.');
      localStorage.setItem('jwtToken', data.token);
      navigate('/journal');
    } catch (err) {
      console.error('Login error:', err);
//       toast.error(err.response?.data?.message || 'Login failed. Please check your credentials.');
    }
  };

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (3-Layer Architecture)
  // ==========================================================================
  const colors = {
      // 🌟 FIX: We only need Layer 1 for the card surface!
      cardBg: isDarkMode ? 'bg-[#1A162F]/95' : 'bg-white/95',
      cardBorder: isDarkMode ? 'border-white/10' : 'border-slate-200/80',
      textPrimary: isDarkMode ? 'text-gray-100' : 'text-slate-900',
      textSecondary: isDarkMode ? 'text-gray-400' : 'text-slate-500',
      buttonPrimary: 'bg-gradient-to-r from-purple-500 to-teal-500 hover:from-purple-600 hover:to-teal-600 shadow-md hover:shadow-lg hover:-translate-y-0.5 active:scale-95 text-white transition-all duration-300',
    };

  return (
       <div className={`min-h-screen w-full flex items-center justify-center p-4 sm:p-6 lg:p-8 ${colors.textPrimary}`}>

         {/* Desktop Back Button */}
         {/* 🌟 FIX: Synced to Master Palette colors and border! */}
<Link to="/" className={`hidden lg:inline-flex fixed top-6 left-6 lg:top-10 lg:left-10 items-center gap-2 ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 transition-all duration-300 font-bold z-50 ${colors.cardBg} px-5 py-2.5 rounded-full border ${colors.cardBorder} shadow-sm hover:shadow-md active:scale-95`}>             <ChevronLeft className="w-4 h-4" /> Home
         </Link>

         <FadeIn direction="up" delay={0.1} className="w-full max-w-5xl z-10 lg:max-h-[90vh] flex">
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

                   <div className="relative z-10 p-12 mt-auto w-full">
                      <div className={`inline-flex items-center gap-3 mb-4 p-3 backdrop-blur-md rounded-2xl border ${isDarkMode ? 'bg-white/10 border-white/20' : 'bg-slate-900/5 border-slate-900/10'}`}>
                         <Sparkles className={`w-6 h-6 ${isDarkMode ? 'text-teal-300' : 'text-teal-600'}`} />
                         <span className={`font-poppins font-bold tracking-widest uppercase text-sm ${isDarkMode ? 'text-white' : 'text-slate-800'}`}>MyMindMirror</span>
                      </div>

                      <h1 className={`text-4xl lg:text-5xl font-poppins font-extrabold leading-tight mb-4 drop-shadow-sm ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>
                        Reflect.<br/>Grow.<br/>Evolve.
                      </h1>
                      <p className={`text-base font-medium leading-relaxed max-w-sm drop-shadow-sm ${isDarkMode ? 'text-gray-200' : 'text-slate-600'}`}>
                        Your personal AI-powered journal for a clearer mind and a brighter future.
                      </p>
                   </div>
               </div>

               {/* RIGHT SIDE: The Login Form */}
               {/* 🌟 FIX: Applied sectionBg (Layer 2) and changed 'scrollbar-hide' to 'custom-scrollbar' for premium overflow handling! */}
{/* 🌟 FIX: Removed sectionBg. It now naturally acts as the Layer 1 Card surface! */}
<div className="w-full lg:w-1/2 flex flex-col items-center p-6 sm:p-8 lg:p-12 relative z-0 overflow-y-auto custom-scrollbar">
                   <div className="w-full max-w-sm my-auto py-4">

                       {/* Mobile-only inline Back Button! */}
                       {/* 🌟 FIX: Synced to textSecondary */}
                       <Link to="/" className={`lg:hidden inline-flex items-center gap-1.5 ${colors.textSecondary} hover:text-purple-600 dark:hover:text-teal-400 transition-colors font-bold mb-6 text-sm`}>
                           <ChevronLeft className="w-4 h-4" /> Back to Home
                       </Link>

                       <div className="text-center mb-8 lg:mt-0">
                           <h2 className="text-3xl font-poppins font-extrabold bg-gradient-to-r from-purple-600 to-teal-500 dark:from-purple-400 dark:to-teal-400 bg-clip-text text-transparent tracking-tight">
                               Welcome Back
                           </h2>
                           <p className={`text-sm lg:text-base font-medium ${colors.textSecondary} mt-2`}>
                               Sign in to continue your journey.
                           </p>
                       </div>

                       <form onSubmit={handleSubmit} className="space-y-5 lg:space-y-6" noValidate>

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
                               disabled={loginMutation.isPending}
                           />

                           {/* 🌟 FIX: Explicitly pass inputBg (Layer 3) */}
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
                               disabled={loginMutation.isPending}
                           />

                           <button
                               type="submit"
                               disabled={loginMutation.isPending}
                               className={`w-full mt-2 py-4 rounded-xl lg:rounded-2xl font-bold text-base lg:text-lg flex items-center justify-center gap-2 ${colors.buttonPrimary} group`}
                           >
                               {loginMutation.isPending ? (
                               <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                               ) : (
                               <>
                                   Login <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                               </>
                               )}
                           </button>
                       </form>

                       {/* 🌟 FIX: Applied sectionBorder to the divider */}
{/* 🌟 FIX: Explicitly set the border color so it doesn't default to bright white! */}
<div className="mt-8 pt-6 border-t border-slate-200/60 dark:border-white/10 text-center">
                           <p className={`text-sm font-medium ${colors.textSecondary}`}>
                           Don't have an account?{' '}
                           <Link to="/register" className="text-purple-600 dark:text-teal-400 hover:text-purple-700 dark:hover:text-teal-300 font-bold transition-colors">
                               Register here
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

export default LoginPage;