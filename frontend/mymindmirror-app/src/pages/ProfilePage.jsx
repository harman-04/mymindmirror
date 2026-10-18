// src/pages/ProfilePage.jsx
import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../contexts/ThemeContext';
import { toast } from 'sonner';
import FadeIn from '../components/FadeIn';
import {
    User, Edit, Save, X, Trash2, Loader, CheckCircle, AlertCircle,
    KeyRound, Lock, Info, Sparkles, Shield, Database, Clock, Activity, Coffee,
    MapPin, CalendarIcon, ChevronDown
} from 'lucide-react';
import { SkeletonProfile } from '../components/Skeleton';
import PremiumInput from '../components/PremiumInput';

import ConfirmationModal from '../components/ConfirmationModal';
import {
    useUserFullProfile,
    useApiKeyStatus,
    useUpdateUserProfile,
    useChangeUserPassword,
    useDeleteUserProfile,
    useUpdateApiKey,
    useUpdateRoadmapPreferences,
    useUpdateUserPreferences
} from '../hooks/useUserProfile';

function ProfilePage() {
    const navigate = useNavigate();
    const { theme } = useTheme();
    const isDarkMode = theme === 'dark';

    const { data: profile, isLoading, isError, error } = useUserFullProfile();
    const apiKeyStatus = useApiKeyStatus();

    const updateProfileMutation = useUpdateUserProfile();
    const deleteProfileMutation = useDeleteUserProfile();
    const changePasswordMutation = useChangeUserPassword();
    const updateApiKeyMutation = useUpdateApiKey();
    const updateRoadmapPrefsMutation = useUpdateRoadmapPreferences();
    const updatePreferencesMutation = useUpdateUserPreferences();

    const updateProfile = updateProfileMutation.mutateAsync;
    const deleteProfile = deleteProfileMutation.mutateAsync;
    const changePassword = changePasswordMutation.mutateAsync;
    const updateApiKey = updateApiKeyMutation.mutateAsync;
    const updateRoadmapPreferences = updateRoadmapPrefsMutation.mutateAsync;
    const updatePreferences = updatePreferencesMutation.mutateAsync;

    const isDeleting = deleteProfileMutation.isPending;
    const isChangingPassword = changePasswordMutation.isPending;

    // Profile edit state
    const [isEditing, setIsEditing] = useState(false);
    const [editedUsername, setEditedUsername] = useState('');
    const [editedEmail, setEditedEmail] = useState('');

    // Password change state
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmNewPassword, setConfirmNewPassword] = useState('');

    // API key state
    const [newApiKey, setNewApiKey] = useState('');
    //     const [showApiKey, setShowApiKey] = useState(false);

    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    // Localized error state for the Profile form to prevent global collisions
    const [profileErrors, setProfileErrors] = useState({});

    //  Localized error state for the Password form
    const [passwordErrors, setPasswordErrors] = useState({});

    //  Localized error state for Roadmap validation
    const [roadmapErrors, setRoadmapErrors] = useState({});

    // ROADMAP DEFAULTS STATE
    const [roadmapPrefs, setRoadmapPrefs] = useState({
        difficulty: 'BEGINNER',
        languagePreference: 'en',
        learningStyle: 'READING',
        hoursPerWeek: 10,
        avoidWeekends: false,
    });

    // LIFESTYLE STATE
    const [energyPeak, setEnergyPeak] = useState('MORNING');
    const [wakeTime, setWakeTime] = useState('07:00');
    const [sleepTime, setSleepTime] = useState('23:00');
    const [lunchTime, setLunchTime] = useState('13:00');
    const [dailyHabits, setDailyHabits] = useState('');

    const [availableHours, setAvailableHours] = useState({
        monday: [["09:00", "12:00"], ["13:00", "17:00"]],
        tuesday: [["09:00", "12:00"], ["13:00", "17:00"]],
        wednesday: [["09:00", "12:00"], ["13:00", "17:00"]],
        thursday: [["09:00", "12:00"], ["13:00", "17:00"]],
        friday: [["09:00", "12:00"], ["13:00", "17:00"]],
        saturday: [],
        sunday: []
    });
    const [timezone, setTimezone] = useState("Asia/Kolkata");

    const [savingProfile, setSavingProfile] = useState(false);
    const [savingPassword, setSavingPassword] = useState(false);
    const [savingApiKey, setSavingApiKey] = useState(false);
    const [savingRoadmapPrefs, setSavingRoadmapPrefs] = useState(false);
    const [savingLifestyle, setSavingLifestyle] = useState(false);

    useEffect(() => {
        if (profile?.roadmapPreferences) {
            setRoadmapPrefs({
                difficulty: profile.roadmapPreferences.difficulty || 'BEGINNER',
                languagePreference: profile.roadmapPreferences.languagePreference || 'en',
                learningStyle: profile.roadmapPreferences.learningStyle || 'READING',
                hoursPerWeek: profile.roadmapPreferences.hoursPerWeek || 10,
                avoidWeekends: profile.roadmapPreferences.avoidWeekends || false,
            });
        }
    }, [profile?.roadmapPreferences]);

    useEffect(() => {
        if (profile) {
            if (profile.availableHoursJson) {
                try { setAvailableHours(JSON.parse(profile.availableHoursJson)); } catch (e) { console.error(e); }
            }
            if (profile.timezone) setTimezone(profile.timezone);
            if (profile.energyPeak) setEnergyPeak(profile.energyPeak);
            if (profile.wakeTime) setWakeTime(profile.wakeTime.substring(0, 5));
            if (profile.sleepTime) setSleepTime(profile.sleepTime.substring(0, 5));
            if (profile.lunchTime) setLunchTime(profile.lunchTime.substring(0, 5));
            if (profile.dailyHabitsJson) {
                try {
                    const habitsArray = JSON.parse(profile.dailyHabitsJson);
                    setDailyHabits(habitsArray.join('\n'));
                } catch (e) { console.error(e); }
            }
            setEditedUsername(profile.username || '');
            setEditedEmail(profile.email || '');
        }
    }, [profile]);

    // Handle hash-based deep links (e.g. /profile#api-key from the navbar popover)
    useEffect(() => {
        if (window.location.hash === '#api-key') {
            const timer = setTimeout(() => {
                document.getElementById('api-key')?.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start',
                });
            }, 200);
            return () => clearTimeout(timer);
        }
    }, []);

    const handlePrefChange = (key, value) => {
        setRoadmapPrefs(prev => ({ ...prev, [key]: value }));
    };

    const handleSaveRoadmapPrefs = useCallback(async () => {
        if (savingRoadmapPrefs) return;
        setRoadmapErrors({}); // Clear old errors

        const hrs = parseInt(roadmapPrefs.hoursPerWeek);

        // Validate the Pace input
        if (!hrs || hrs < 1 || hrs > 112) {
            setRoadmapErrors({ hoursPerWeek: 'Must be a valid number between 1 and 112.' });
            return;
        }

        setSavingRoadmapPrefs(true);
        try {
            await updateRoadmapPreferences({ ...roadmapPrefs, hoursPerWeek: hrs });
            toast.success('Roadmap defaults saved successfully!');
        } catch (err) {
            //                 toast.error(err.message || 'Failed to save roadmap preferences.');
        } finally {
            setSavingRoadmapPrefs(false);
        }
    }, [roadmapPrefs, updateRoadmapPreferences, savingRoadmapPrefs]);
    const handleEditClick = () => setIsEditing(true);

    const handleCancelEdit = () => {
        setIsEditing(false);
        if (profile) {
            setEditedUsername(profile.username || '');
            setEditedEmail(profile.email || '');
        }
    };

    const handleSaveProfile = useCallback(async () => {
        if (savingProfile) return;
        setProfileErrors({}); // Clear previous errors

        const errors = {};
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        // 1. Username validation
        if (!editedUsername.trim() || editedUsername.length < 3 || editedUsername.length > 50) {
            errors.username = 'Username must be between 3 and 50 characters.';
        }

        // 2. Email validation
        if (!editedEmail.trim()) {
            errors.email = 'Email cannot be empty.';
        } else if (!emailRegex.test(editedEmail.trim())) {
            errors.email = 'Please enter a valid email address.';
        } else if (editedEmail.trim().length > 100) {
            errors.email = 'Email must not exceed 100 characters.';
        }

        // If any error exists, show them on the inputs and stop submission
        if (Object.keys(errors).length > 0) {
            setProfileErrors(errors);
            return;
        }

        setSavingProfile(true);
        try {
            await updateProfile({ username: editedUsername.trim(), email: editedEmail.trim() });
            setIsEditing(false);
            toast.success('Profile updated successfully!');
        } catch (err) {
            //                 toast.error(err.response?.data?.message || err.message || 'Failed to update profile.');
        } finally {
            setSavingProfile(false);
        }
    }, [editedUsername, editedEmail, updateProfile, savingProfile]);

    const handleChangePassword = useCallback(async () => {
        if (savingPassword) return;
        setPasswordErrors({}); //  Clear old errors

        let errors = {};

        // 1. Check for empty fields
        if (!currentPassword) errors.currentPassword = 'Required.';
        if (!newPassword) errors.newPassword = 'Required.';
        if (!confirmNewPassword) errors.confirmNewPassword = 'Required.';

        if (Object.keys(errors).length > 0) {
            setPasswordErrors(errors);
            return;
        }

        // 2. Deep validation
        if (newPassword.length < 6) {
            setPasswordErrors({ newPassword: 'Must be at least 6 characters.' });
            return;
        }
        if (newPassword.length > 72) {
            setPasswordErrors({ newPassword: 'Must not exceed 72 characters.' });
            return;
        }
        if (newPassword !== confirmNewPassword) {
            setPasswordErrors({ confirmNewPassword: 'Passwords do not match.' });
            return;
        }
        if (currentPassword === newPassword) {
            setPasswordErrors({ newPassword: 'Cannot be the same as current password.' });
            return;
        }

        setSavingPassword(true);
        try {
            await changePassword({ currentPassword, newPassword });
            toast.success('Password changed successfully!');
            setCurrentPassword('');
            setNewPassword('');
            setConfirmNewPassword('');
        } catch (err) {
            // System error from backend
            //                 toast.error(err.message || 'Failed to change password.');
            // toast handle by axiosConfig 
        } finally {
            setSavingPassword(false);
        }
    }, [currentPassword, newPassword, confirmNewPassword, changePassword, savingPassword]);

    const handleSaveApiKey = useCallback(async () => {
        if (savingApiKey) return;
        setSavingApiKey(true);
        try {
            const result = await updateApiKey(newApiKey);

            switch (result.status) {
                case 'VERIFIED':
                    toast.success(result.message);
                    setNewApiKey('');
                    break;
                case 'CLEARED':
                    toast.success(result.message);
                    setNewApiKey('');
                    break;
                case 'UNVERIFIED':
                    toast.warning(result.message);
                    setNewApiKey('');
                    break;
                case 'REJECTED':
                    toast.error(result.message);
                    // Keep the input so the user can fix the typo and retry.
                    break;
                default:
                    toast.success('Saved.');
                    setNewApiKey('');
            }
        } catch (err) {
            // axios interceptor displays the error toast
        } finally {
            setSavingApiKey(false);
        }
    }, [newApiKey, updateApiKey, savingApiKey]);

    const handleDeleteAccount = () => setShowDeleteConfirm(true);
    const confirmDeleteAccount = async () => {
        try {
            await deleteProfile();
            toast.success('Account deleted successfully. Redirecting...');
            setTimeout(() => navigate('/'), 2000);
        } catch (err) {
            //             toast.error(err.message || 'Failed to delete account.');
        } finally {
            setShowDeleteConfirm(false);
        }
    };
    const cancelDeleteAccount = () => setShowDeleteConfirm(false);

    const handleSaveLifestyle = useCallback(async () => {
        if (savingLifestyle) return;
        setSavingLifestyle(true);
        try {
            const habitsArray = dailyHabits.split('\n').map(h => h.trim()).filter(h => h.length > 0);
            await updatePreferences({
                availableHoursJson: JSON.stringify(availableHours),
                timezone: timezone,
                energyPeak: energyPeak,
                wakeTime: wakeTime + ":00",
                sleepTime: sleepTime + ":00",
                lunchTime: lunchTime + ":00",
                dailyHabitsJson: JSON.stringify(habitsArray)
            });
            toast.success("Lifestyle & Schedule saved successfully!");
        } catch (err) {
            //             toast.error("Failed to save lifestyle preferences");
        } finally {
            setSavingLifestyle(false);
        }
    }, [availableHours, timezone, energyPeak, wakeTime, sleepTime, lunchTime, dailyHabits, updatePreferences, savingLifestyle]);

    const SectionHeader = ({ icon: Icon, title, colorClass, subtitle }) => (
        //  Synced to Master Palette borders
        <div className={`mb-6 lg:mb-8 border-b ${colors.cardBorder} pb-5`}>
            <div className="flex items-center gap-3 lg:gap-4">
                {/*  Replaced hardcoded bg-white/bg-[#131127] with our dynamic Layer 1 Input Bg */}
                <div className={`p-3 lg:p-4 rounded-xl lg:rounded-2xl ${colors.inputBgLayer1} shadow-sm border ${colors.inputBorder} ${colorClass}`}>
                    <Icon className="w-6 h-6 lg:w-7 lg:h-7" />
                </div>
                <div>
                    {/* Replaced text-gray-800 with textPrimary */}
                    <h2 className={`text-xl lg:text-2xl font-poppins font-extrabold tracking-tight ${colors.textPrimary}`}>{title}</h2>
                    {/*  Replaced text-gray-500 with textSecondary */}
                    {subtitle && <p className={`text-xs lg:text-sm font-medium mt-1 ${colors.textSecondary}`}>{subtitle}</p>}
                </div>
            </div>
        </div>
    );



    // ==========================================================================
    //  MASTER ELEVATION PALETTE (3-Layer Architecture)
    // ==========================================================================
    const colors = {
        background: 'bg-transparent',

        // Layer 1: Main Cards
        cardBg: isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm',
        cardBorder: isDarkMode ? ' border-white/10' : ' border-slate-200/80',

        // Layer 2: Inner Sections (Like the Availability Grid)
        sectionBg: isDarkMode ? 'bg-[#131127]/80 shadow-inner' : 'bg-slate-50/80 shadow-inner',
        //  Softened the light mode border to /60 to differentiate it from the outer card
        sectionBorder: isDarkMode ? ' border-white/5' : ' border-slate-200/60',
        // Typography
        textPrimary: isDarkMode ? ' text-gray-100' : ' text-slate-900',
        textSecondary: isDarkMode ? ' text-gray-400' : ' text-slate-500',

        // Layer 3: Inputs (Punched-in effect)
        inputBgLayer1: isDarkMode ? 'bg-[#131127] text-gray-100' : 'bg-slate-50 text-slate-900',
        inputBgLayer2: isDarkMode ? 'bg-black/20 text-gray-100' : 'bg-white text-slate-900',
        inputBorder: isDarkMode ? 'border-white/10' : 'border-slate-300',
        inputFocusRing: ' focus:ring-2 focus:ring-purple-500/50 focus:border-purple-500/50 dark:focus:ring-teal-400/50 dark:focus:border-teal-400/50',

        // Buttons
        buttonPrimary: 'bg-gradient-to-r from-purple-500 to-teal-500 hover:from-purple-600 hover:to-teal-600 shadow-sm hover:shadow-md hover:-translate-y-0.5 active:scale-95 text-white disabled:opacity-50 disabled:hover:scale-100 disabled:hover:-translate-y-0 transition-all duration-200',
        buttonSecondary: isDarkMode ? 'bg-white/5 hover:bg-white/10 border border-white/10 text-gray-200 active:scale-95 disabled:opacity-50 transition-colors duration-200' : 'bg-white hover:bg-slate-50 active:scale-95 border border-slate-200/80 disabled:opacity-50 transition-colors duration-200',
        buttonDanger: 'bg-gradient-to-r from-red-500 to-rose-600 hover:from-red-600 hover:to-rose-700 hover:shadow-md active:scale-95 text-white disabled:opacity-50 disabled:hover:scale-100 transition-all duration-200',
    };


    if (isLoading) {
        return (
            <div className="w-full relative">
                <SkeletonProfile />
            </div>
        );
    }
    if (isError) {
        return (
            <div className={`min-h-[60vh] w-full flex items-center justify-center p-4`}>
                <div className="text-center space-y-4">
                    <AlertCircle size={48} className="text-red-500 mx-auto" />
                    <p className={`font-bold ${colors.textPrimary}`}>{error?.response?.data?.message || 'Failed to load profile.'}</p>
                    <button onClick={() => navigate('/login')} className={`px-6 py-2.5 rounded-full text-white ${colors.buttonPrimary} transition-all`}>Return to Login</button>
                </div>
            </div>
        );
    }

    return (
        <div className={`w-full transition-colors duration-300 relative`}>
            {/* Responsive Floating Background Icons */}
            <div className="fixed top-[15%] lg:top-[30%] -left-10 sm:left-4 xl:left-[calc(50%-44rem)] opacity-5 sm:opacity-10 lg:opacity-20 animate-float z-0 pointer-events-none">
                <User className="w-48 h-48 sm:w-12 sm:h-12 lg:w-16 lg:h-16 text-purple-500 dark:text-purple-400" />
            </div>
            <div className="fixed bottom-[15%] lg:bottom-[10%] -right-10 sm:right-4 xl:right-[calc(50%-45rem)] opacity-5 sm:opacity-10 lg:opacity-20 animate-float-delayed z-0 pointer-events-none">
                <Shield className="w-48 h-48 sm:w-12 sm:h-12 lg:w-16 lg:h-16 text-teal-500 dark:text-teal-400" />
            </div>

            <div className="max-w-7xl mx-auto relative z-10 space-y-6 sm:space-y-8 pb-10">

                {/* Profile Header */}
                <FadeIn delay={0.1} direction="down" fullWidth>
                    <div className={`rounded-2xl lg:rounded-3xl ${colors.cardBg} border${colors.cardBorder} p-6 lg:p-8 shadow-sm transition-shadow hover:shadow-md mt-4 flex flex-col sm:flex-row items-center gap-5`}>
                        <div className="p-4 lg:p-5 rounded-2xl bg-gradient-to-br from-purple-500/20 to-teal-500/20 dark:from-purple-900/30 dark:to-teal-900/30 shadow-inner border border-purple-200/50 dark:border-teal-700/30 shrink-0">
                            <User className="w-8 h-8 lg:w-10 lg:h-10 text-purple-600 dark:text-teal-400" />
                        </div>
                        <div className="text-center sm:text-left">
                            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-poppins font-extrabold tracking-tight bg-gradient-to-r from-purple-600 to-teal-500 dark:from-purple-400 dark:to-teal-400 bg-clip-text text-transparent leading-tight">
                                Account Settings
                            </h1>
                            <p className={`text-sm lg:text-base ${colors.textSecondary} mt-1.5 font-medium`}>
                                Manage your personal information and application preferences.
                            </p>
                        </div>
                    </div>
                </FadeIn>

                {/* Account Details */}
                <FadeIn delay={0.2} direction="up" fullWidth>
                    <div className={`rounded-2xl lg:rounded-3xl ${colors.cardBg} border${colors.cardBorder} p-6 sm:p-8 lg:p-10 shadow-sm transition-shadow hover:shadow-md`}>
                        <SectionHeader icon={User} title="Account Details" colorClass="text-purple-500" />
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-5 lg:gap-6">
                            <div>
                                {isEditing ? (
                                    <PremiumInput
                                        label="Username"
                                        value={editedUsername}
                                        inputBgClass={colors.inputBgLayer1}
                                        onChange={(e) => {
                                            setEditedUsername(e.target.value);
                                            if (profileErrors.username) setProfileErrors(prev => ({ ...prev, username: null }));
                                        }}
                                        disabled={savingProfile}
                                        error={profileErrors.username}
                                        showError={!!profileErrors.username}
                                    />
                                ) : (
                                    <div className="w-full space-y-1.5 lg:space-y-2">
                                        <label className={`block text-xs lg:text-sm font-bold uppercase tracking-wider ${colors.textSecondary}`}>Username</label>
                                        <div className={`p-3 lg:p-4 rounded-xl ${colors.inputBgLayer1} border ${colors.inputBorder} font-bold text-sm lg:text-base ${colors.textPrimary} shadow-sm`}>
                                            {profile?.username}
                                        </div>
                                    </div>
                                )}
                            </div>
                            <div>
                                {isEditing ? (
                                    <PremiumInput
                                        type="email"
                                        label="Email"
                                        value={editedEmail}
                                        inputBgClass={colors.inputBgLayer1}
                                        onChange={(e) => {
                                            setEditedEmail(e.target.value);
                                            if (profileErrors.email) setProfileErrors(prev => ({ ...prev, email: null }));
                                        }}
                                        disabled={savingProfile}
                                        error={profileErrors.email}
                                        showError={!!profileErrors.email}
                                    />
                                ) : (
                                    <div className="w-full space-y-1.5 lg:space-y-2">
                                        <label className={`block text-xs lg:text-sm font-bold uppercase tracking-wider ${colors.textSecondary}`}>Email</label>
                                        <div className={`p-3 lg:p-4 rounded-xl ${colors.inputBgLayer1} border ${colors.inputBorder} font-bold text-sm lg:text-base ${colors.textPrimary} shadow-sm`}>
                                            {profile?.email}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                        <div className={`flex flex-wrap justify-end gap-3 mt-8 pt-6 border-t${colors.sectionBorder}`}>
                            {isEditing ? (
                                <>
                                    <button onClick={handleCancelEdit} className={`px-6 py-2.5 rounded-xl font-bold transition-all flex items-center justify-center gap-2 ${colors.buttonSecondary}${colors.textPrimary}`} disabled={savingProfile}>
                                        <X size={18} /> Cancel
                                    </button>
                                    <button onClick={handleSaveProfile} className={`px-8 py-2.5 rounded-xl text-white font-bold transition-all flex items-center justify-center gap-2 ${colors.buttonPrimary}`} disabled={savingProfile}>
                                        {savingProfile ? <Loader size={18} className="animate-spin" /> : <Save size={18} />}
                                        {savingProfile ? 'Saving...' : 'Save Profile'}
                                    </button>
                                </>
                            ) : (
                                <button onClick={handleEditClick} className={`px-8 py-2.5 rounded-xl text-white font-bold transition-all flex items-center justify-center gap-2 ${colors.buttonPrimary}`}>
                                    <Edit size={18} /> Edit Profile
                                </button>
                            )}
                        </div>
                    </div>
                </FadeIn>

                <FadeIn delay={0.3} direction="up" fullWidth>
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 sm:gap-8 items-stretch">

                        {/* Change Password */}
                        <div className={`col-span-1 flex flex-col h-full rounded-2xl lg:rounded-3xl ${colors.cardBg} border${colors.cardBorder} p-6 sm:p-8 shadow-sm transition-shadow hover:shadow-md`}>
                            <SectionHeader icon={KeyRound} title="Change Password" colorClass="text-indigo-500" />
                            <div className="space-y-4 lg:space-y-5 flex-grow">
                                <PremiumInput
                                    type="password"
                                    label="Current Password"
                                    inputBgClass={colors.inputBgLayer1}
                                    value={currentPassword}
                                    onChange={(e) => {
                                        setCurrentPassword(e.target.value);
                                        if (passwordErrors.currentPassword) setPasswordErrors(prev => ({ ...prev, currentPassword: null }));
                                    }}
                                    placeholder="••••••••"
                                    disabled={savingPassword || isChangingPassword}
                                    error={passwordErrors.currentPassword}
                                    showError={!!passwordErrors.currentPassword}
                                />
                                <PremiumInput
                                    type="password"
                                    label="New Password"
                                    inputBgClass={colors.inputBgLayer1}
                                    value={newPassword}
                                    onChange={(e) => {
                                        setNewPassword(e.target.value);
                                        if (passwordErrors.newPassword) setPasswordErrors(prev => ({ ...prev, newPassword: null }));
                                    }}
                                    placeholder="••••••••"
                                    disabled={savingPassword || isChangingPassword}
                                    error={passwordErrors.newPassword}
                                    showError={!!passwordErrors.newPassword}
                                />
                                <PremiumInput
                                    type="password"
                                    label="Confirm Password"
                                    inputBgClass={colors.inputBgLayer1}
                                    value={confirmNewPassword}
                                    onChange={(e) => {
                                        setConfirmNewPassword(e.target.value);
                                        if (passwordErrors.confirmNewPassword) setPasswordErrors(prev => ({ ...prev, confirmNewPassword: null }));
                                    }}
                                    placeholder="••••••••"
                                    disabled={savingPassword || isChangingPassword}
                                    error={passwordErrors.confirmNewPassword}
                                    showError={!!passwordErrors.confirmNewPassword}
                                />
                            </div>
                            <div className={`mt-8 pt-6 border-t${colors.sectionBorder}`}>
                                <button
                                    onClick={handleChangePassword}
                                    className={`w-full py-3 lg:py-4 rounded-xl text-white font-bold text-sm lg:text-base transition-all flex items-center justify-center gap-2 ${colors.buttonPrimary}`}
                                    disabled={savingPassword || isChangingPassword}
                                >
                                    {(savingPassword || isChangingPassword) ? <Loader size={18} className="animate-spin" /> : <Lock size={18} />}
                                    {(savingPassword || isChangingPassword) ? 'Changing...' : 'Update Password'}
                                </button>
                            </div>
                        </div>
                        {/* Gemini API Key */}
                        <div
                            id="api-key"
                            className={`col-span-1 flex flex-col h-full rounded-2xl lg:rounded-3xl ${colors.cardBg} border${colors.cardBorder} p-6 sm:p-8 shadow-sm transition-shadow hover:shadow-md scroll-mt-24`}
                        >
                            <SectionHeader icon={Database} title="Gemini API Key" colorClass="text-amber-500" />

                            <div className="space-y-5 lg:space-y-6 flex-grow">
                                {apiKeyStatus.isLoading ? (
                                    <div className={`h-14 lg:h-16 ${colors.sectionBg} border${colors.sectionBorder} rounded-xl animate-pulse`} />
                                ) : apiKeyStatus.data ? (
                                    <div className={`p-4 lg:p-5 rounded-xl flex items-center gap-3 border shadow-sm ${apiKeyStatus.data.usingOwnKey ? 'bg-emerald-50 dark:bg-emerald-500/10 border-emerald-200 dark:border-emerald-500/30 text-emerald-700 dark:text-emerald-400' : 'bg-amber-50 dark:bg-amber-500/10 border-amber-200 dark:border-amber-500/30 text-amber-700 dark:text-amber-400'}`}>
                                        {apiKeyStatus.data.usingOwnKey ? <CheckCircle size={20} className="shrink-0" /> : <AlertCircle size={20} className="shrink-0" />}
                                        <div className="flex flex-col min-w-0">
                                            <span className="font-bold text-sm lg:text-base truncate">{apiKeyStatus.data.message}</span>
                                            {apiKeyStatus.data.usingOwnKey && <span className="text-[10px] lg:text-xs font-mono opacity-80 mt-0.5 truncate">({apiKeyStatus.data.maskedKey})</span>}
                                        </div>
                                    </div>
                                ) : null}

                                <PremiumInput
                                    type="password"
                                    label="Private Key"
                                    inputBgClass={colors.inputBgLayer1}
                                    value={newApiKey}
                                    onChange={(e) => setNewApiKey(e.target.value)}
                                    placeholder="Paste Gemini API key..."
                                    disabled={savingApiKey || updateApiKeyMutation.isPending}
                                />

                                <p className={`text-[10px] lg:text-xs font-medium leading-relaxed flex items-start gap-1.5 ${colors.textSecondary} bg-amber-50 dark:bg-amber-900/10 p-3 lg:p-4 rounded-xl border border-amber-200/50 dark:border-amber-500/20`}>
                                    <Info size={14} className="shrink-0 text-amber-500 mt-0.5" />
                                    <span>
                                        Key is heavily encrypted (AES-GCM). Used to bypass shared quotas.
                                        <a href="https://aistudio.google.com/apikey" target="_blank" rel="noopener noreferrer" className="text-teal-600 dark:text-teal-400 hover:text-teal-700 hover:underline ml-1 font-bold transition-colors">
                                            Get one here.
                                        </a>
                                    </span>
                                </p>
                            </div>

                            <div className={`mt-8 pt-6 border-t${colors.sectionBorder}`}>
                                <button
                                    onClick={handleSaveApiKey}
                                    className={`w-full py-3 lg:py-4 rounded-xl text-white font-bold text-sm lg:text-base transition-all flex items-center justify-center gap-2 ${colors.buttonPrimary}`}
                                    disabled={savingApiKey || updateApiKeyMutation.isPending}
                                >
                                    {(savingApiKey || updateApiKeyMutation.isPending) ? <Loader size={18} className="animate-spin" /> : <Save size={18} />}
                                    {(savingApiKey || updateApiKeyMutation.isPending) ? 'Saving...' : 'Securely Save Key'}
                                </button>
                            </div>
                        </div>
                    </div>
                </FadeIn>

                {/* Roadmap Defaults */}
                <FadeIn delay={0.4} direction="up" fullWidth>
                    <div className={`rounded-2xl lg:rounded-3xl ${colors.cardBg} border${colors.cardBorder} p-6 sm:p-8 lg:p-10 shadow-sm transition-shadow hover:shadow-md`}>
                        <SectionHeader
                            icon={MapPin}
                            title="Roadmap Defaults"
                            colorClass="text-purple-500 dark:text-teal-400"
                            subtitle="Configure the global baseline parameters used when generating new AI Learning Roadmaps."
                        />

                        {/*  Expanded to grid-cols-5 to fit the Language dropdown natively */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-5 lg:gap-6 mb-6 items-start">

                            {/* Difficulty */}
                            <div className="w-full space-y-1.5 lg:space-y-2">
                                <label className={`block text-xs lg:text-sm font-bold uppercase tracking-wider ${colors.textSecondary}`}>Difficulty</label>
                                <div className="relative">
                                    <select value={roadmapPrefs.difficulty} onChange={(e) => handlePrefChange('difficulty', e.target.value)} className={`w-full h-[46px] lg:h-[58px] p-3 lg:p-4 pr-10 appearance-none rounded-xl border ${colors.inputBorder} ${colors.inputBgLayer1}${colors.inputFocusRing} outline-none cursor-pointer font-bold text-sm lg:text-base transition-colors shadow-sm`}>
                                        <option value="BEGINNER">Beginner</option>
                                        <option value="INTERMEDIATE">Intermediate</option>
                                        <option value="ADVANCED">Advanced</option>
                                    </select>
                                    <ChevronDown className={`absolute right-3 lg:right-4 top-1/2 -translate-y-1/2 w-4 h-4 lg:w-5 lg:h-5 pointer-events-none ${colors.textSecondary}`} />
                                </div>
                            </div>

                            {/*  Language Preference */}
                            <div className="w-full space-y-1.5 lg:space-y-2">
                                <label className={`block text-xs lg:text-sm font-bold uppercase tracking-wider ${colors.textSecondary}`}>Language</label>
                                <div className="relative">
                                    <select value={roadmapPrefs.languagePreference} onChange={(e) => handlePrefChange('languagePreference', e.target.value)} className={`w-full h-[46px] lg:h-[58px] p-3 lg:p-4 pr-10 appearance-none rounded-xl border ${colors.inputBorder} ${colors.inputBgLayer1}${colors.inputFocusRing} outline-none cursor-pointer font-bold text-sm lg:text-base transition-colors shadow-sm`}>
                                        <option value="en">English</option>
                                        <option value="hi">Hindi</option>
                                        <option value="es">Spanish</option>
                                        <option value="fr">French</option>
                                        <option value="de">German</option>
                                    </select>
                                    <ChevronDown className={`absolute right-3 lg:right-4 top-1/2 -translate-y-1/2 w-4 h-4 lg:w-5 lg:h-5 pointer-events-none ${colors.textSecondary}`} />
                                </div>
                            </div>

                            {/* Learning Style */}
                            <div className="w-full space-y-1.5 lg:space-y-2">
                                <label className={`block text-xs lg:text-sm font-bold uppercase tracking-wider ${colors.textSecondary}`}>Learning Style</label>
                                <div className="relative">
                                    <select value={roadmapPrefs.learningStyle} onChange={(e) => handlePrefChange('learningStyle', e.target.value)} className={`w-full h-[46px] lg:h-[58px] p-3 lg:p-4 pr-10 appearance-none rounded-xl border ${colors.inputBorder} ${colors.inputBgLayer1}${colors.inputFocusRing} outline-none cursor-pointer font-bold text-sm lg:text-base transition-colors shadow-sm`}>
                                        <option value="READING">Reading (Docs)</option>
                                        <option value="VISUAL">Visual (Video)</option>
                                        <option value="HANDS_ON">Hands-on</option>
                                    </select>
                                    <ChevronDown className={`absolute right-3 lg:right-4 top-1/2 -translate-y-1/2 w-4 h-4 lg:w-5 lg:h-5 pointer-events-none ${colors.textSecondary}`} />
                                </div>
                            </div>

                            {/* Pace (PremiumInput) */}
                            <PremiumInput
                                type="number"
                                min="1"
                                max="112"
                                label="Pace (Hrs/Wk)"
                                inputBgClass={colors.inputBgLayer1}
                                value={roadmapPrefs.hoursPerWeek}
                                onChange={(e) => {
                                    handlePrefChange('hoursPerWeek', e.target.value);
                                    if (roadmapErrors.hoursPerWeek) setRoadmapErrors({});
                                }}
                                error={roadmapErrors.hoursPerWeek}
                                showError={!!roadmapErrors.hoursPerWeek}
                            />

                            {/* Weekend Policy */}
                            <div className="w-full space-y-1.5 lg:space-y-2">
                                <label className={`hidden sm:block text-xs lg:text-sm font-bold uppercase tracking-wider opacity-0`}>Weekend Policy</label>
                                <div className={`w-full h-[46px] lg:h-[58px] px-4 rounded-xl border ${colors.inputBorder} ${colors.inputBgLayer1} flex items-center gap-3 transition-colors shadow-sm`}>
                                    <input type="checkbox" id="avoidWeekends" checked={roadmapPrefs.avoidWeekends} onChange={(e) => handlePrefChange('avoidWeekends', e.target.checked)} className="w-5 h-5 rounded border-slate-300 text-purple-500 focus:ring-purple-500 dark:border-white/10 dark:text-teal-400 dark:focus:ring-teal-400 cursor-pointer transition-colors" />
                                    <label htmlFor="avoidWeekends" className={`text-sm lg:text-base font-bold cursor-pointer select-none ${colors.textPrimary}`}>Avoid Weekends</label>
                                </div>
                            </div>
                        </div>
                        <div className={`flex justify-end mt-8 pt-6 border-t${colors.sectionBorder}`}>
                            <button
                                onClick={handleSaveRoadmapPrefs}
                                disabled={savingRoadmapPrefs}
                                className={`px-8 py-3 lg:py-4 rounded-xl text-white font-bold text-sm lg:text-base shadow-md hover:shadow-lg transition-all flex items-center justify-center gap-2 w-full sm:w-auto ${colors.buttonPrimary}`}
                            >
                                {savingRoadmapPrefs ? <Loader size={18} className="animate-spin" /> : <Save size={18} />}
                                {savingRoadmapPrefs ? "Saving Defaults..." : "Save Roadmap Defaults"}
                            </button>
                        </div>
                    </div>
                </FadeIn>
                {/* Lifestyle Engine */}
                <FadeIn delay={0.5} direction="up" fullWidth>
                    <div className={`rounded-2xl lg:rounded-3xl ${colors.cardBg} border${colors.cardBorder} p-6 sm:p-8 lg:p-10 shadow-sm transition-shadow hover:shadow-md`}>
                        <SectionHeader
                            icon={Activity}
                            title="Lifestyle & Learning Engine"
                            colorClass="text-teal-500"
                            subtitle="Help the AI understand your routine so it can build perfect, burnout-free Smart Timetables."
                        />

                        {/* Row 1: Time Inputs */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 lg:gap-6 mb-8 items-start">
                            {/* Energy Peak */}
                            <div className="w-full space-y-1.5 lg:space-y-2">
                                {/* Synced label typography and icon sizing to match PremiumInput precisely */}
                                <label className={`flex items-center gap-1.5 text-xs lg:text-sm font-bold uppercase tracking-wider ${colors.textSecondary}`}>
                                    <Sparkles size={16} className="text-purple-500 dark:text-teal-400" /> Energy Peak
                                </label>
                                <div className="relative">
                                    <select value={energyPeak} onChange={(e) => setEnergyPeak(e.target.value)} className={`w-full h-[46px] lg:h-[58px] p-3 lg:p-4 pr-10 appearance-none rounded-xl border ${colors.inputBorder} ${colors.inputBgLayer1}${colors.inputFocusRing} outline-none cursor-pointer font-bold text-sm lg:text-base transition-colors shadow-sm`}>
                                        <option value="MORNING">Morning (Focus early)</option>
                                        <option value="AFTERNOON">Afternoon (Steady)</option>
                                        <option value="EVENING">Evening (Night Owl)</option>
                                    </select>
                                    <ChevronDown className={`absolute right-3 lg:right-4 top-1/2 -translate-y-1/2 w-4 h-4 lg:w-5 lg:h-5 pointer-events-none ${colors.textSecondary}`} />
                                </div>
                            </div>

                            <PremiumInput
                                type="time"
                                inputBgClass={colors.inputBgLayer1}
                                label={<span className="flex items-center gap-1.5 ml-1"><Clock size={14} className="text-emerald-500" /> Wake Time</span>}
                                value={wakeTime}
                                onChange={(e) => setWakeTime(e.target.value)}
                            />

                            <PremiumInput
                                type="time"
                                inputBgClass={colors.inputBgLayer1}
                                label={<span className="flex items-center gap-1.5 ml-1"><Clock size={14} className="text-indigo-500" /> Sleep Time</span>}
                                value={sleepTime}
                                onChange={(e) => setSleepTime(e.target.value)}
                            />

                            <PremiumInput
                                type="time"
                                inputBgClass={colors.inputBgLayer1}
                                label={<span className="flex items-center gap-1.5 ml-1"><Coffee size={14} className="text-amber-500" /> Lunch Break</span>}
                                value={lunchTime}
                                onChange={(e) => setLunchTime(e.target.value)}
                            />
                        </div>

                        {/* Row 2: Habits */}
                        <div className="mb-10">
                            <PremiumInput
                                multiline={true}
                                rows={3}
                                inputBgClass={colors.inputBgLayer1}
                                label={<span className="ml-1">Daily Habits / Routines (One per line)</span>}
                                value={dailyHabits}
                                onChange={(e) => setDailyHabits(e.target.value)}
                                placeholder="15 mins meditation&#10;30 min walk outside&#10;Read 10 pages of a book"
                            />
                        </div>

                        {/* Row 4: Availability Grid (Layer 2) */}
                        <div className={`pt-8 border-t${colors.sectionBorder}`}>
                            <h3 className={`font-poppins font-bold text-lg lg:text-xl mb-6 flex items-center gap-3 ${colors.textPrimary}`}>
                                <CalendarIcon size={24} className="text-purple-500 dark:text-teal-400" /> Availability Slots
                            </h3>

                            {/* Removed 2xl:grid-cols-4 to cap the layout at 3 columns maximum for better breathing room */}
                            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 lg:gap-6">
                                {Object.keys(availableHours).map((day) => (
                                    <div key={day} className={`rounded-xl lg:rounded-2xl border ${colors.sectionBorder} ${colors.sectionBg} p-4 lg:p-5 shadow-sm flex flex-col`}>
                                        <div className={`flex items-center justify-between mb-4 pb-3 border-b${colors.sectionBorder} shrink-0`}>
                                            <span className={`capitalize font-poppins font-bold text-sm lg:text-base ${colors.textPrimary}`}>{day}</span>
                                            <button onClick={() => setAvailableHours({ ...availableHours, [day]: [...availableHours[day], ["09:00", "17:00"]] })} className={`text-xs font-bold text-teal-600 dark:text-teal-400 hover:text-teal-700 hover:bg-teal-50 dark:hover:bg-teal-900/30 px-3 py-1.5 rounded-lg transition-colors ${colors.inputBgLayer2} border border-teal-200/50 dark:border-teal-500/20 shrink-0 shadow-sm`}>+ Add</button>
                                        </div>
                                        <div className="space-y-3 flex-1">
                                            {availableHours[day].map((slot, idx) => (
                                                <div key={idx} className="flex items-center gap-1.5 sm:gap-2">
                                                    <div className="flex-1 min-w-0">
                                                        {/*  Reduced horizontal padding (px-1 sm:px-2) so the AM/PM text has more space to render */}
                                                        <input type="time" value={slot[0]} onChange={(e) => { const newSlots = [...availableHours[day]]; newSlots[idx][0] = e.target.value; setAvailableHours({ ...availableHours, [day]: newSlots }); }} className={`w-full px-1 sm:px-2 py-2 text-[11px] sm:text-xs lg:text-sm font-bold tracking-tighter rounded-lg border ${colors.inputBorder} ${colors.inputBgLayer2} ${colors.textPrimary} outline-none focus:ring-1 focus:ring-purple-500 transition-colors shadow-sm`} />
                                                    </div>
                                                    <span className={`text-[10px] sm:text-xs font-bold shrink-0 ${colors.textSecondary}`}>-</span>
                                                    <div className="flex-1 min-w-0">
                                                        {/*  Reduced horizontal padding here as well */}
                                                        <input type="time" value={slot[1]} onChange={(e) => { const newSlots = [...availableHours[day]]; newSlots[idx][1] = e.target.value; setAvailableHours({ ...availableHours, [day]: newSlots }); }} className={`w-full px-1 sm:px-2 py-2 text-[11px] sm:text-xs lg:text-sm font-bold tracking-tighter rounded-lg border ${colors.inputBorder} ${colors.inputBgLayer2} ${colors.textPrimary} outline-none focus:ring-1 focus:ring-teal-500 transition-colors shadow-sm`} />
                                                    </div>
                                                    <button onClick={() => setAvailableHours({ ...availableHours, [day]: availableHours[day].filter((_, i) => i !== idx) })} className={`shrink-0 p-2 text-red-500 ${colors.inputBgLayer2} hover:bg-red-50 dark:hover:bg-red-900/40 rounded-lg transition-colors border border-red-100 dark:border-red-900/30 shadow-sm`}>
                                                        <Trash2 className="w-3.5 h-3.5 sm:w-4 sm:h-4 lg:w-5 lg:h-5" />
                                                    </button>
                                                </div>
                                            ))}
                                            {availableHours[day].length === 0 && (
                                                <div className={`text-center text-xs lg:text-sm font-medium italic py-3 ${colors.inputBgLayer2} rounded-xl border border-dashed ${colors.inputBorder} h-full flex items-center justify-center min-h-[45px] ${colors.textSecondary}`}>No slots added</div>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                        <div className={`flex justify-end mt-8 pt-6 border-t${colors.sectionBorder}`}>
                            <button
                                onClick={handleSaveLifestyle}
                                disabled={savingLifestyle}
                                className={`px-8 py-3 lg:py-4 rounded-xl text-white font-bold text-sm lg:text-base shadow-md hover:shadow-lg transition-all flex items-center justify-center gap-2 w-full sm:w-auto ${colors.buttonPrimary}`}
                            >
                                {savingLifestyle ? <Loader size={18} className="animate-spin" /> : <Save size={18} />}
                                {savingLifestyle ? "Saving Lifestyle..." : "Save Lifestyle Engine"}
                            </button>
                        </div>
                    </div>
                </FadeIn>

                {/* Danger Zone */}
                <FadeIn delay={0.6} direction="up" fullWidth>
                    <div className={`rounded-2xl lg:rounded-3xl bg-red-50/80 dark:bg-red-900/10 border border-red-200 dark:border-red-900/30 p-6 sm:p-8 lg:p-10 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-6`}>
                        <div>
                            <div className="flex items-center gap-3 mb-2">
                                {/* Synced to inputBgLayer1 instead of hardcoded white/dark hex */}
                                <div className={`p-3 lg:p-4 rounded-xl lg:rounded-2xl ${colors.inputBgLayer1} shadow-sm border border-red-200 dark:border-red-900/50 text-red-500`}>
                                    <AlertCircle className="w-6 h-6 lg:w-8 lg:h-8" />
                                </div>
                                <h2 className={`text-xl lg:text-2xl font-poppins font-extrabold tracking-tight ${colors.textPrimary}`}>Danger Zone</h2>
                            </div>
                            <p className={`text-sm lg:text-base font-medium md:ml-16 lg:ml-[4.5rem] ${colors.textSecondary}`}>
                                Permanently delete your account and all associated data.
                            </p>
                        </div>

                        <button onClick={handleDeleteAccount} className={`shrink-0 w-full md:w-auto px-6 py-3 lg:px-8 lg:py-4 rounded-xl lg:rounded-2xl font-bold text-sm lg:text-base shadow-md flex items-center justify-center gap-2 ${colors.buttonDanger}`} disabled={isDeleting}>
                            {isDeleting ? <Loader size={18} className="animate-spin" /> : <Trash2 size={18} />}
                            {isDeleting ? 'Deleting...' : 'Delete Account Forever'}
                        </button>
                    </div>
                </FadeIn>
            </div>

            <ConfirmationModal
                isOpen={showDeleteConfirm}
                onClose={cancelDeleteAccount}
                onConfirm={confirmDeleteAccount}
                title="Delete Account"
                message="Are you absolutely sure you want to delete your account? This action is irreversible and all your data will be permanently lost."
                confirmText="Yes, Delete My Account"
                cancelText="Cancel"
                isDestructive={true}
                isLoading={isDeleting}
            />
        </div>
    );
}

export default React.memo(ProfilePage);