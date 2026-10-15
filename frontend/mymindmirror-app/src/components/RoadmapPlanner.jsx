// src/components/RoadmapPlanner.jsx
import React, { useState, useEffect, useCallback } from 'react';
import ReactDOM from 'react-dom';
import { useRoadmaps, useGenerateRoadmap, useDeleteRoadmap, useImportTaskToMilestone,
    useToggleTaskCompletion, useContinueRoadmap, useElaborateTask,
    useRescheduleRoadmap, useContinueRoadmapBatch } from '../hooks/useRoadmap';
import { useTheme } from '../contexts/ThemeContext';
import { toast } from 'sonner';
import FadeIn from './FadeIn'; // 💡 NEW: Import the animation engine
import {
  Loader, Sparkles, CheckCircle, Circle, ExternalLink, Target, Trash2,
  ChevronDown, ChevronUp, BookOpen, ListChecks, Award, Calendar, Clock,Plus,
  TrendingUp, AlertTriangle, BarChart2, List, Video, FileText, GraduationCap
} from 'lucide-react';
import RoadmapTimeline from './RoadmapTimeline';
import { useQueryClient } from '@tanstack/react-query';
import { useUserFullProfile } from '../hooks/useUserProfile';
import { motion } from 'framer-motion';
import { SkeletonRoadmap } from './Skeleton';
import PremiumInput from './PremiumInput'; // 🌟 NEW: Import Design System
// ------------------------------------------------------------------
// Upgraded Markdown Parser (Extracted Inline for Subtasks)
// ------------------------------------------------------------------

const escapeHtml = (str) => {
  return str.replace(/[&<>]/g, (m) => {
    if (m === '&') return '&amp;';
    if (m === '<') return '&lt;';
    if (m === '>') return '&gt;';
    return m;
  });
};

// Extracted so we can run subtasks through it individually
export const formatInline = (line) => {
  if (!line) return '';
  let formatted = escapeHtml(line);

  // Inline code (maps to guide-code in index.css)
  formatted = formatted.replace(/`([^`]+)`/g, '<code class="guide-code">$1</code>');
  // Links
  formatted = formatted.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-purple-600 dark:text-teal-400 hover:underline font-medium">$1</a>');
  // Bold
  formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
  // Italics
  formatted = formatted.replace(/\*(.*?)\*/g, '<em>$1</em>');

  // Java/C# Annotation styling (e.g., @FunctionalInterface)
  formatted = formatted.replace(/(^|[\s([{])(@[A-Za-z0-9_]+)/g, '$1<span class="text-purple-600 dark:text-teal-400 font-mono font-bold bg-purple-50 dark:bg-teal-900/20 px-1.5 py-0.5 rounded-md shadow-sm border border-purple-200/50 dark:border-teal-500/20">$2</span>');

  return formatted;
};

const formatText = (text) => {
  if (!text) return '';

  // Pre-process the text to break "squished" inline lists into actual new lines
  let processedText = text.replace(/([:;?!.])\s+(?=\d+\.\s+[A-Z])/g, '$1\n');

  const lines = processedText.split('\n');
  const result = [];
  let i = 0;
  const total = lines.length;

  const getHeadingLevel = (line) => {
    const match = line.match(/^(#{1,6})\s+(.*)/);
    return match ? { level: match[1].length, content: match[2] } : null;
  };

  const getBlockquote = (line) => line.startsWith('> ') ? line.substring(2) : null;
  const isHorizontalRule = (line) => /^(\*{3,}|-{3,}|_{3,})$/.test(line.trim());

  let inCodeBlock = false;
  let codeBlockContent = [];
  let codeBlockLang = '';

  const flushCodeBlock = () => {
    if (codeBlockContent.length > 0) {
      result.push(`<pre><code class="language-${codeBlockLang}">${escapeHtml(codeBlockContent.join('\n'))}</code></pre>`);
      codeBlockContent = [];
      codeBlockLang = '';
    }
    inCodeBlock = false;
  };

  while (i < total) {
    const line = lines[i];

    if (line.trim().startsWith('```')) {
      if (!inCodeBlock) {
        inCodeBlock = true;
        codeBlockLang = line.trim().substring(3).trim();
        i++;
        continue;
      } else {
        flushCodeBlock();
        i++;
        continue;
      }
    }

    if (inCodeBlock) {
      codeBlockContent.push(line);
      i++;
      continue;
    }

    const heading = getHeadingLevel(line);
    if (heading) {
      result.push(`<h${heading.level}>${formatInline(heading.content)}</h${heading.level}>`);
      i++;
      continue;
    }

    if (isHorizontalRule(line)) {
      result.push('<hr/>');
      i++;
      continue;
    }

    const blockquoteContent = getBlockquote(line);
    if (blockquoteContent !== null) {
      let quoteText = blockquoteContent;
      let j = i + 1;
      while (j < total && lines[j].startsWith('> ')) {
        quoteText += '\n' + lines[j].substring(2);
        j++;
      }
      result.push(`<blockquote>${formatText(quoteText)}</blockquote>`);
      i = j;
      continue;
    }

    const bulletMatch = line.match(/^\s*(\*|\-)\s+(.*)/);
    const numberMatch = line.match(/^\s*(\d+)\.\s+(.*)/);
    if (bulletMatch || numberMatch) {
      const isOrdered = !!numberMatch;
      const listItems = [];
      while (i < total) {
        const currentLine = lines[i];
        const bullet = currentLine.match(/^\s*(\*|\-)\s+(.*)/);
        const number = currentLine.match(/^\s*(\d+)\.\s+(.*)/);
        if (bullet || number) {
          const content = bullet ? bullet[2] : number[2];
          listItems.push(`<li>${formatInline(content)}</li>`);
          i++;
        } else {
          break;
        }
      }
      const listTag = isOrdered ? 'ol' : 'ul';
      result.push(`<${listTag}>${listItems.join('')}</${listTag}>`);
      continue;
    }

    if (line.trim() === '') {
      result.push('<br/>');
      i++;
      continue;
    }

    result.push(`<p>${formatInline(line)}</p>`);
    i++;
  }

  if (inCodeBlock) flushCodeBlock();
  return result.join('');
};

// ------------------------------------------------------------------
// Portal-based Delete Confirmation Modal
// ------------------------------------------------------------------
const DeleteConfirmationModal = ({ isOpen, onClose, onConfirm, roadmapTitle, theme, isLoading }) => {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    document.body.style.overflow = isOpen ? 'hidden' : 'unset';
    return () => { document.body.style.overflow = 'unset'; };
  }, [isOpen]);

  if (!isOpen || !mounted) return null;

  return ReactDOM.createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)' }}>
      <div className="absolute inset-0" onClick={onClose} aria-hidden="true" />
      {/* 🌟 FIX: Stripped blur, applied strict palette matching */}
      <div className={`relative max-w-md w-full rounded-2xl lg:rounded-3xl shadow-2xl overflow-hidden transform transition-all duration-300 border ${
              theme === 'dark' ? 'bg-[#1A162F]/95 border-white/10' : 'bg-white/95 border-slate-200/80'
            }`}>
        <div className="p-6 lg:p-8 text-center">
          <div className="mx-auto w-14 h-14 lg:w-16 lg:h-16 mb-4 lg:mb-6 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
            <AlertTriangle className="w-7 h-7 lg:w-8 lg:h-8 text-red-600 dark:text-red-400"/>
          </div>
          <h3 className="text-xl lg:text-2xl font-poppins font-bold tracking-tight mb-2 text-slate-800 dark:text-gray-100">Delete Roadmap</h3>
          <p className="text-sm lg:text-base text-slate-600 dark:text-gray-300 mb-2">
            Are you sure you want to delete <strong className="font-semibold text-slate-800 dark:text-white">"{roadmapTitle}"</strong>?
          </p>
          <p className="text-xs lg:text-sm text-slate-500 dark:text-gray-400 mb-6 lg:mb-8 font-medium">This action cannot be undone.</p>
          <div className="flex justify-center gap-3">
            <button
               onClick={onConfirm}
               disabled={isLoading}
               className="px-6 py-2.5 lg:py-3 rounded-full bg-red-600 hover:bg-red-700 text-white font-bold transition-all active:scale-95 shadow-md disabled:opacity-50 flex items-center justify-center min-w-[120px]"
             >
               {isLoading ? <Loader className="w-5 h-5 animate-spin"/> : 'Delete'}
             </button>
            <button
              onClick={onClose}
              disabled={isLoading}
              className={`px-6 py-2.5 lg:py-3 rounded-full font-bold transition-all active:scale-95 disabled:opacity-50 ${theme === 'dark' ? 'bg-black/20 text-gray-200 hover:bg-black/40 border border-white/10' : 'bg-slate-200 text-slate-800 hover:bg-slate-300'}`}
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>,
    document.body
  );
};

// ------------------------------------------------------------------
// Portal-based Task Detail Modal
// ------------------------------------------------------------------
const TaskDetailModal = ({ task, isOpen, onClose, onElaborate, isElaborating }) => {
  const { theme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    document.body.style.overflow = isOpen ? 'hidden' : 'unset';
    return () => { document.body.style.overflow = 'unset'; };
  }, [isOpen]);

  if (!isOpen || !task || !mounted) return null;

  const hasDetails = task.details && task.details.trim().length > 0;
  const hasSubtasks = task.subtasks && task.subtasks.length > 0;

  // 🌟 FIX: Stripped blur, applied strict palette matching
  const bgClass = theme === 'dark' ? 'bg-[#1A162F]/95' : 'bg-white/95';
  const borderClass = theme === 'dark' ? 'border-white/10' : 'border-slate-200/80';
  const sectionBgClass = theme === 'dark' ? 'bg-[#131127]/80' : 'bg-slate-50/80';

  return ReactDOM.createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)' }}>
      <div className="absolute inset-0" onClick={onClose} aria-hidden="true" />
      <div className={`relative max-w-2xl w-full rounded-2xl lg:rounded-3xl ${bgClass} border ${borderClass} shadow-2xl overflow-hidden transform transition-all duration-300 scale-100`}>
        <div className={`flex justify-between items-center p-5 lg:p-6 border-b ${borderClass} ${sectionBgClass}`}>
          <h3 className="text-lg lg:text-xl font-poppins font-extrabold bg-gradient-to-r from-purple-600 to-teal-600 dark:from-purple-400 dark:to-teal-400 bg-clip-text text-transparent pr-4 leading-tight">
            {task.description}
          </h3>
          <button onClick={onClose} className="p-2 rounded-full hover:bg-slate-200 dark:hover:bg-white/10 transition shrink-0">
            <svg className="w-5 h-5 text-slate-500 dark:text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="p-6 lg:p-8 space-y-6 max-h-[70vh] overflow-y-auto custom-scrollbar">

          {!hasDetails && !hasSubtasks ? (
            <div className="text-center py-10">
              <div className="w-16 h-16 lg:w-20 lg:h-20 mx-auto mb-4 rounded-full bg-purple-100 dark:bg-purple-900/30 flex items-center justify-center">
                <BookOpen className="w-8 h-8 lg:w-10 lg:h-10 text-purple-500 dark:text-teal-400"/>
              </div>
              <p className="text-slate-500 dark:text-gray-400 mb-6 font-medium text-sm lg:text-base">This task doesn't have detailed instructions yet.</p>
              <button
                onClick={() => onElaborate(task.id, false)}
                disabled={isElaborating}
                className="px-6 py-3 rounded-full bg-gradient-to-r from-purple-500 to-teal-500 text-white font-bold hover:shadow-lg transition disabled:opacity-50 flex items-center justify-center mx-auto gap-2 active:scale-95"
              >
                {isElaborating ? <Loader className="w-5 h-5 animate-spin"/> : <Sparkles className="w-5 h-5"/>}
                {isElaborating ? 'Generating...' : 'Generate Detailed Guide'}
              </button>
            </div>
          ) : (
            <>
              {hasDetails && (
                <div className={`${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-purple-50/50 border-purple-100'} rounded-2xl p-5 lg:p-6 shadow-inner border`}>
                  <h4 className="font-poppins font-bold flex items-center gap-2 mb-4 text-purple-700 dark:text-teal-400">
                    <BookOpen className="w-5 h-5"/> AI Details & Guide
                  </h4>
                  <div className="roadmap-details text-slate-800 dark:text-gray-200" dangerouslySetInnerHTML={{ __html: formatText(task.details) }} />
                </div>
              )}
              {hasSubtasks && (
                <div className={`${theme === 'dark' ? 'bg-black/20 border-white/5' : 'bg-teal-50/50 border-teal-100'} rounded-2xl p-5 lg:p-6 shadow-inner border`}>
                  <h4 className="font-poppins font-bold flex items-center gap-2 mb-4 text-teal-700 dark:text-teal-400">
                    <ListChecks className="w-5 h-5"/> Actionable Subtasks
                  </h4>
                  <ul className="list-disc list-outside ml-4 space-y-2 text-sm lg:text-base text-slate-700 dark:text-gray-300 marker:text-purple-500 dark:marker:text-teal-400">
                    {task.subtasks.map((sub, idx) => (
                      <li key={idx} className="pl-1" dangerouslySetInnerHTML={{ __html: formatInline(sub) }} />
                    ))}
                  </ul>
                </div>
              )}
              <div className={`flex flex-wrap justify-end gap-3 pt-4 border-t ${borderClass} mt-6`}>
                <button
                  onClick={() => onElaborate(task.id, false)}
                  disabled={isElaborating}
                  className={`px-5 py-2.5 text-sm lg:text-base font-bold rounded-full transition-all active:scale-95 disabled:opacity-50 ${theme === 'dark' ? 'bg-black/20 text-gray-200 hover:bg-black/40 border border-white/10' : 'bg-slate-200 text-slate-800 hover:bg-slate-300'}`}
                >
                  Regenerate
                </button>
                <button
                  onClick={() => onElaborate(task.id, true)}
                  disabled={isElaborating}
                  className="px-5 py-2.5 text-sm lg:text-base font-bold rounded-full bg-gradient-to-r from-purple-500 to-teal-500 text-white hover:shadow-md active:scale-95 transition-all disabled:opacity-50 flex items-center gap-2"
                >
                  {isElaborating && <Loader className="w-4 h-4 animate-spin"/>}
                  {isElaborating ? 'Enhancing...' : 'Enhance (More detail)'}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
};

// ------------------------------------------------------------------
// Main RoadmapPlanner Component
// ------------------------------------------------------------------
function RoadmapPlanner() {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';
  const { data: roadmaps, isLoading, isError, refetch } = useRoadmaps();
  const generateMutation = useGenerateRoadmap();
  const deleteMutation = useDeleteRoadmap();
  const importTaskMutation = useImportTaskToMilestone();
  const toggleTaskMutation = useToggleTaskCompletion();
  const continueMutation = useContinueRoadmap();
  const elaborateMutation = useElaborateTask();
  const rescheduleMutation = useRescheduleRoadmap();
  const continueBatchMutation = useContinueRoadmapBatch();
  const queryClient = useQueryClient();

  const { data: profile } = useUserFullProfile();
  const roadmapPreferences = profile?.roadmapPreferences;

  const [timeframeValue, setTimeframeValue] = useState(4);
  const [timeframeUnit, setTimeframeUnit] = useState('WEEKS');
  const [overridePreferences, setOverridePreferences] = useState(false);
  const [difficulty, setDifficulty] = useState('BEGINNER');
  const [language, setLanguage] = useState('en');
  const [learningStyle, setLearningStyle] = useState('READING');
  const [hoursPerWeek, setHoursPerWeek] = useState(10);
  const [avoidWeekends, setAvoidWeekends] = useState(false);

  const [goal, setGoal] = useState('');
  const [showForm, setShowForm] = useState(false);
  // 🌟 NEW: Localized error state for the form
    const [formErrors, setFormErrors] = useState({});
  const [expandedTaskId, setExpandedTaskId] = useState(null);
  const [expandedWeeks, setExpandedWeeks] = useState({});
  const [selectedTask, setSelectedTask] = useState(null);
  const [completedRoadmapIds, setCompletedRoadmapIds] = useState(new Set());
  const [deleteModal, setDeleteModal] = useState({ isOpen: false, roadmapId: null, roadmapTitle: '' });
  const [continuingRoadmapId, setContinuingRoadmapId] = useState(null);

  const [viewModeMap, setViewModeMap] = useState({});
  const [visibleWeeksMap, setVisibleWeeksMap] = useState({});
  const [isDeleting, setIsDeleting] = useState(false);


// ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (3-Layer Architecture)
  // ==========================================================================
  // Layer 1: Base Cards (Header, Form Wrapper, Individual Roadmap Cards)
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';

  // Layer 2: Inner Sections (Progress Dashboard, Override Panel, Resources)
  const sectionBg = isDarkMode ? 'bg-[#131127]/80 shadow-inner' : 'bg-slate-50/80 shadow-inner';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';

  // Layer 3: Deep Content (Tasks)
  const innerContentBg = isDarkMode ? 'bg-black/20' : 'bg-white';

  // Universal Elements
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

// Input specific layers
  // 🌟 FIX: Layer 1 inputs use slate-50 so they contrast against the white card!
  const inputBgLayer1 = isDarkMode ? 'bg-[#131127]/80' : 'bg-slate-50';

  // 🌟 FIX: Layer 2 inputs use pure white so they contrast against the slate-50 panel!
  const inputBgLayer2 = isDarkMode ? 'bg-black/20' : 'bg-white';

  const inputBorder = isDarkMode ? 'border-white/10' : 'border-slate-300';
  const inputFocusRing = 'focus:ring-2 focus:ring-purple-500 focus:border-purple-500 dark:focus:ring-teal-400 dark:focus:border-teal-400';
  const toggleViewMode = (roadmapId) => {
    setViewModeMap(prev => ({
      ...prev,
      [roadmapId]: prev[roadmapId] === 'timeline' ? 'list' : 'timeline'
    }));
  };

  useEffect(() => {
    if (selectedTask && roadmaps) {
      for (const roadmap of roadmaps) {
        const updatedTask = roadmap.tasks?.find(t => t.id === selectedTask.id);
        if (updatedTask) {
          setSelectedTask(updatedTask);
          break;
        }
      }
    }
  }, [roadmaps, selectedTask?.id]);

  useEffect(() => {
    if (roadmaps) {
      roadmaps.forEach(roadmap => {
        const totalTasks = roadmap.tasks?.length || 0;
        const completedTasks = roadmap.tasks?.filter(t => t.completed).length || 0;
        if (totalTasks > 0 && completedTasks === totalTasks && !completedRoadmapIds.has(roadmap.id)) {
          setCompletedRoadmapIds(prev => new Set(prev).add(roadmap.id));
          toast.success(`🎉 Congratulations! You've completed "${roadmap.title}"!`, {
            duration: 5000,
            icon: '🏆',
          });
        }
      });
    }
  }, [roadmaps, completedRoadmapIds]);

  useEffect(() => {
    if (roadmapPreferences && !overridePreferences) {
      setDifficulty(roadmapPreferences.difficulty || 'BEGINNER');
      setLanguage(roadmapPreferences.languagePreference || 'en');
      setLearningStyle(roadmapPreferences.learningStyle || 'READING');
      setHoursPerWeek(roadmapPreferences.hoursPerWeek || 10);
      setAvoidWeekends(roadmapPreferences.avoidWeekends || false);
    }
  }, [roadmapPreferences, overridePreferences]);

  useEffect(() => {
    if (roadmaps) {
      const newVisibleMap = { ...visibleWeeksMap };
      roadmaps.forEach(roadmap => {
        if (!newVisibleMap[roadmap.id]) {
          const initialVisible = roadmap.generatedWeeks
              ? Math.min(roadmap.generatedWeeks, roadmap.durationWeeks)
              : roadmap.durationWeeks;
          newVisibleMap[roadmap.id] = initialVisible;
        }
      });
      setVisibleWeeksMap(newVisibleMap);
    }
  }, [roadmaps]);

const handleGenerate = useCallback((e) => {
     e.preventDefault();
     setFormErrors({}); // 🌟 Clear old errors

     let errors = {};
     const parsedTimeframe = parseInt(timeframeValue);

     // 1. Validate Goal
     if (!goal.trim()) errors.goal = 'Please enter a goal to achieve.';

     // 2. Validate Duration
     if (isNaN(parsedTimeframe) || parsedTimeframe < 1) {
         errors.timeframeValue = 'Must be at least 1.';
     }

     // 3. Validate Hours (if overriding)
     if (overridePreferences) {
         const hrs = parseInt(hoursPerWeek);
         if (isNaN(hrs) || hrs < 1 || hrs > 112) {
             errors.hoursPerWeek = 'Must be between 1 and 112 hours.';
         }
     }

     // 🌟 If any errors exist, highlight the inputs and stop!
     if (Object.keys(errors).length > 0) {
         setFormErrors(errors);
         return;
     }

     const payload = {
       goal: goal.trim(),
       timeframeValue: parsedTimeframe,
       timeframeUnit: timeframeUnit,
       difficulty: difficulty,
       language: language,
       learningStyle: learningStyle,
       hoursPerWeek: parseInt(hoursPerWeek),
       avoidWeekends: avoidWeekends,
     };

     if (timeframeUnit === 'WEEKS') {
       payload.timeframeWeeks = parsedTimeframe;
     }

     generateMutation.mutate(payload, {
       onSuccess: () => {
         setGoal('');
         setTimeframeValue(4);
         setTimeframeUnit('WEEKS');
         setShowForm(false);
         setFormErrors({}); // Clear errors on success
         refetch();
         toast.success('Roadmap generated successfully!');
       },
     });
   }, [goal, timeframeValue, timeframeUnit, difficulty, language, learningStyle, hoursPerWeek, avoidWeekends, overridePreferences, generateMutation, refetch]);
  const handleDeleteClick = useCallback((id, title) => {
      setDeleteModal({ isOpen: true, roadmapId: id, roadmapTitle: title });
  }, []);

  const confirmDelete = () => {
    setIsDeleting(true);
    deleteMutation.mutate(deleteModal.roadmapId, {
      onSuccess: () => {
        toast.success('Roadmap deleted');
        setDeleteModal({ isOpen: false, roadmapId: null, roadmapTitle: '' });
        setIsDeleting(false);
      },
      onError: () => {
//         toast.error('Failed to delete roadmap');
        setDeleteModal({ isOpen: false, roadmapId: null, roadmapTitle: '' });
        setIsDeleting(false);
      },
    });
  };

  const handleElaborate = async (taskId, enhance = false) => {
    try {
      await elaborateMutation.mutateAsync({ taskId, enhance });
      await refetch();
      toast.success(enhance ? 'Task enhanced with more details!' : 'Task details regenerated!');
    } catch {
//       toast.error('Failed to generate details');
    }
  };

  const importTaskToMilestone = (roadmapId, taskId, taskDescription) => {
    importTaskMutation.mutate({ roadmapId, taskId }, {
      onSuccess: () => toast.success(`✨ "${taskDescription}" added to Milestones`),
//       onError: (error) => toast.error(`Failed to add task: ${error.message}`)
    });
  };

  const toggleTaskExpand = (taskId) => {
    setExpandedTaskId(expandedTaskId === taskId ? null : taskId);
  };

  const toggleWeekExpand = (roadmapId, week) => {
    const key = `${roadmapId}-${week}`;
    setExpandedWeeks(prev => ({ ...prev, [key]: !prev[key] }));
  };

// 🌟 ARCHITECTURE FIX: Instantly renders the full-page ghost UI
  if (isLoading) {
    return <SkeletonRoadmap />;
  }

  if (isError) {
    return (
      <div className="text-center py-16 lg:py-24 max-w-2xl mx-auto">
        <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-red-100 dark:bg-red-900/30 mb-6">
          <AlertTriangle className="w-10 h-10 text-red-500"/>
        </div>
        <p className="text-red-500 font-bold text-lg lg:text-xl">Failed to load roadmaps.</p>
        <p className="text-gray-500 dark:text-gray-400 mt-2 mb-6">Please check your connection and try again.</p>
        <button onClick={() => refetch()} className="px-6 py-3 rounded-full font-bold bg-purple-500 hover:bg-purple-600 text-white transition shadow-md">
          Retry Connection
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-8 lg:space-y-10 w-full">
   <FadeIn delay={0.1} direction="down" fullWidth>
           {/* Header + New Roadmap Button */}
           <div className={`flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 lg:gap-6 p-6 lg:p-8 rounded-2xl lg:rounded-3xl border ${cardBg} ${cardBorder}`}>
             <div>
               <h2 className="text-2xl lg:text-3xl font-poppins font-extrabold tracking-tight bg-gradient-to-r from-purple-500 to-teal-500 dark:from-purple-400 dark:to-teal-400 bg-clip-text text-transparent leading-tight">
                 Personal Growth Roadmaps
               </h2>
               <p className={`text-sm lg:text-base mt-1.5 font-medium ${textSecondary}`}>Your AI-powered journey to success</p>
             </div>
             <button
               onClick={() => setShowForm(!showForm)}
               className="group px-6 py-3 lg:px-8 lg:py-3.5 rounded-full bg-gradient-to-r from-purple-500 to-teal-500 text-white font-bold text-sm lg:text-base shadow-md hover:shadow-lg active:scale-95 transition-all duration-300 flex items-center justify-center gap-2 shrink-0 w-full sm:w-auto"
             >
               <Sparkles className="w-5 h-5 group-hover:rotate-12 transition"/>
               {showForm ? 'Cancel Creation' : '+ New Roadmap'}
             </button>
           </div>
         </FadeIn>

         {/* Generation Form */}
         {showForm && (
           <FadeIn delay={0.2} direction="up" fullWidth>
             <div className={`p-6 lg:p-8 rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} shadow-xl transition-shadow duration-300`}>
               <h3 className={`text-xl lg:text-2xl font-poppins font-extrabold mb-6 lg:mb-8 flex items-center gap-3 ${textPrimary}`}>
                 <Sparkles className="w-6 h-6 text-purple-500 dark:text-teal-400"/>
                 Create Your Personalized Roadmap
               </h3>

               <form onSubmit={handleGenerate} className="space-y-5 lg:space-y-6" noValidate>
                 <PremiumInput
                     label="What do you want to achieve?"
                     required={true}
                     inputBgClass={inputBgLayer1}
                     placeholder="e.g., Learn React Native, Run a marathon, Start a blog"
                     value={goal}
                     onChange={(e) => {
                         setGoal(e.target.value);
                         if (formErrors.goal) setFormErrors(prev => ({ ...prev, goal: null }));
                     }}
                     error={formErrors.goal}
                     showError={!!formErrors.goal}
                 />

                 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 lg:gap-6 items-start">
                   <PremiumInput
                       type="number"
                       min="1"
                       label="Duration"
                       inputBgClass={inputBgLayer1}
                       value={timeframeValue}
                       onChange={(e) => {
                           setTimeframeValue(e.target.value);
                           if (formErrors.timeframeValue) setFormErrors(prev => ({ ...prev, timeframeValue: null }));
                       }}
                       error={formErrors.timeframeValue}
                       showError={!!formErrors.timeframeValue}
                   />

                   <div className="w-full space-y-1.5 lg:space-y-2">
                     <label className={`block text-[10px] lg:text-xs font-bold uppercase tracking-wider ml-1 ${textSecondary}`}>Unit</label>
                     <div className="relative">
                       <select
                         value={timeframeUnit}
                         onChange={(e) => setTimeframeUnit(e.target.value)}
                         className={`w-full h-[46px] lg:h-[58px] p-3 lg:p-4 pr-10 appearance-none rounded-xl border ${inputBorder} ${inputBgLayer1} ${textPrimary} focus:outline-none transition-colors text-sm lg:text-base shadow-sm ${inputFocusRing}`}
                       >
                         <option value="DAYS">Days</option>
                         <option value="WEEKS">Weeks</option>
                         <option value="MONTHS">Months</option>
                         <option value="YEARS">Years</option>
                       </select>
                       <ChevronDown className={`absolute right-3 lg:right-4 top-1/2 -translate-y-1/2 w-4 h-4 lg:w-5 lg:h-5 pointer-events-none ${textSecondary}`} />
                     </div>
                   </div>
                 </div>
                 <div className={`border-t ${sectionBorder} mt-6 pt-6`}>
                   <div className="flex items-center gap-3 mb-4 lg:mb-5">
                     <input
                       type="checkbox"
                       id="overridePrefs"
                       checked={overridePreferences}
                       onChange={(e) => setOverridePreferences(e.target.checked)}
                       className="w-5 h-5 rounded border-gray-300 text-purple-500 focus:ring-purple-500 dark:focus:ring-teal-400 transition-colors"
                     />
                     <label htmlFor="overridePrefs" className={`text-sm lg:text-base font-bold ${textPrimary}`}>
                       Use custom preferences for this roadmap <span className="font-normal opacity-70">(override saved defaults)</span>
                     </label>
                   </div>

                   {overridePreferences && (
                     // 🌟 FIX: Applied Layer 2 styling to the Override Panel
                     <div className={`grid grid-cols-1 sm:grid-cols-2 gap-4 lg:gap-6 mt-4 p-5 lg:p-6 ${sectionBg} rounded-xl lg:rounded-2xl border ${sectionBorder} items-start`}>

                       <div className="w-full space-y-1.5 lg:space-y-2">
                         <label className={`block text-[10px] lg:text-xs font-bold uppercase tracking-wider ml-1 ${textSecondary}`}>Difficulty</label>
                         <div className="relative">
                           <select value={difficulty} onChange={(e) => setDifficulty(e.target.value)} className={`w-full h-[46px] lg:h-[58px] p-3 lg:p-4 pr-10 appearance-none rounded-xl border ${inputBorder} ${inputBgLayer2} ${textPrimary} focus:outline-none transition-colors text-sm lg:text-base shadow-sm ${inputFocusRing}`}>
                             <option value="BEGINNER">Beginner</option>
                             <option value="INTERMEDIATE">Intermediate</option>
                             <option value="ADVANCED">Advanced</option>
                           </select>
                           <ChevronDown className={`absolute right-3 lg:right-4 top-1/2 -translate-y-1/2 w-4 h-4 lg:w-5 lg:h-5 pointer-events-none ${textSecondary}`} />
                         </div>
                       </div>

                       <div className="w-full space-y-1.5 lg:space-y-2">
                         <label className={`block text-[10px] lg:text-xs font-bold uppercase tracking-wider ml-1 ${textSecondary}`}>Language</label>
                         <div className="relative">
                           <select value={language} onChange={(e) => setLanguage(e.target.value)} className={`w-full h-[46px] lg:h-[58px] p-3 lg:p-4 pr-10 appearance-none rounded-xl border ${inputBorder} ${inputBgLayer2} ${textPrimary} focus:outline-none transition-colors text-sm lg:text-base shadow-sm ${inputFocusRing}`}>
                             <option value="en">English</option>
                             <option value="hi">Hindi</option>
                             <option value="es">Spanish</option>
                             <option value="fr">French</option>
                             <option value="de">German</option>
                           </select>
                           <ChevronDown className={`absolute right-3 lg:right-4 top-1/2 -translate-y-1/2 w-4 h-4 lg:w-5 lg:h-5 pointer-events-none ${textSecondary}`} />
                         </div>
                       </div>

                       <div className="w-full space-y-1.5 lg:space-y-2">
                         <label className={`block text-[10px] lg:text-xs font-bold uppercase tracking-wider ml-1 ${textSecondary}`}>Learning Style</label>
                         <div className="relative">
                           <select value={learningStyle} onChange={(e) => setLearningStyle(e.target.value)} className={`w-full h-[46px] lg:h-[58px] p-3 lg:p-4 pr-10 appearance-none rounded-xl border ${inputBorder} ${inputBgLayer2} ${textPrimary} focus:outline-none transition-colors text-sm lg:text-base shadow-sm ${inputFocusRing}`}>
                             <option value="READING">Reading</option>
                             <option value="VISUAL">Visual</option>
                             <option value="HANDS_ON">Hands-on</option>
                           </select>
                           <ChevronDown className={`absolute right-3 lg:right-4 top-1/2 -translate-y-1/2 w-4 h-4 lg:w-5 lg:h-5 pointer-events-none ${textSecondary}`} />
                         </div>
                       </div>

                       <PremiumInput
                           type="number"
                           min="1"
                           max="112"
                           label="Hours per Week"
                           inputBgClass={inputBgLayer2}
                           value={hoursPerWeek}
                           onChange={(e) => {
                               setHoursPerWeek(e.target.value);
                               if (formErrors.hoursPerWeek) setFormErrors(prev => ({ ...prev, hoursPerWeek: null }));
                           }}
                           error={formErrors.hoursPerWeek}
                           showError={!!formErrors.hoursPerWeek}
                       />

                       <div className={`flex items-center gap-3 col-span-full mt-2 lg:mt-4 p-4 rounded-xl border ${sectionBorder} ${innerContentBg} shadow-sm transition-colors hover:border-purple-300 dark:hover:border-teal-500/50`}>
                         <input
                           type="checkbox"
                           id="avoidWeekendsLocal"
                           checked={avoidWeekends}
                           onChange={(e) => setAvoidWeekends(e.target.checked)}
                           className="w-5 h-5 rounded text-purple-500 focus:ring-purple-500 dark:focus:ring-teal-400 transition-colors cursor-pointer"
                         />
                         <label htmlFor="avoidWeekendsLocal" className={`text-sm font-bold ${textPrimary} cursor-pointer select-none`}>Avoid scheduling tasks on weekends</label>
                       </div>
                     </div>
                   )}
                 </div>

                 <button
                   type="submit"
                   disabled={generateMutation.isPending}
                   className="w-full mt-6 py-4 rounded-xl bg-gradient-to-r from-purple-500 to-teal-500 hover:from-purple-600 hover:to-teal-600 text-white font-extrabold text-base lg:text-lg shadow-lg hover:shadow-xl active:scale-[0.98] active:shadow-sm transition-all duration-300 disabled:opacity-50 flex items-center justify-center gap-3"
                 >
                   {generateMutation.isPending ? <Loader className="animate-spin w-6 h-6"/> : <Sparkles className="w-6 h-6"/>}
                   {generateMutation.isPending ? 'Generating Roadmap...' : 'Generate Roadmap'}
                 </button>
               </form>
               {generateMutation.isError && (
                 <div className="mt-4 p-4 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-500/30 text-red-700 dark:text-red-400 text-sm font-medium flex items-center gap-2">
{/*                    <AlertTriangle className="w-5 h-5"/> Error: {generateMutation.error.message} */}
<AlertTriangle className="w-5 h-5"/> Error: {generateMutation.error?.response?.data?.message || 'Failed to generate roadmap. Please try again.'}
                 </div>
               )}
             </div>
           </FadeIn>
         )}

{/* Empty State */}
      {roadmaps?.length === 0 && !showForm && (
        <FadeIn delay={0.2} direction="up" fullWidth>
          <div className={`text-center py-16 lg:py-24 rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} shadow-md flex flex-col items-center justify-center`}>
            <div className="w-20 h-20 lg:w-24 lg:h-24 mb-6 rounded-full bg-gradient-to-br from-purple-100 to-teal-100 dark:from-purple-900/20 dark:to-teal-900/20 flex items-center justify-center">
              <Target className="w-10 h-10 lg:w-12 lg:h-12 text-purple-400 dark:text-teal-400 opacity-80"/>
            </div>
            <h3 className={`text-xl lg:text-2xl font-poppins font-bold ${textPrimary} mb-2`}>No roadmaps yet.</h3>
            <p className={`text-sm lg:text-base ${textSecondary} mb-8 max-w-md`}>Break down your big goals into actionable steps with an AI-generated roadmap.</p>
            <button
              onClick={() => setShowForm(true)}
              className="px-6 py-3 lg:px-8 lg:py-4 rounded-full bg-gradient-to-r from-purple-500 to-teal-500 text-white font-bold text-sm lg:text-base shadow-md hover:shadow-lg transition-all"
            >
              Create your first roadmap
            </button>
          </div>
        </FadeIn>
      )}

      {/* Roadmaps List */}
      <div className="grid grid-cols-1 gap-6 lg:gap-8">
        {roadmaps?.map((roadmap, idx) => {
          const totalTasks = roadmap.tasks?.length || 0;
          const completedTasks = roadmap.tasks?.filter(t => t.completed).length || 0;
          const completionPercent = totalTasks === 0 ? 0 : (completedTasks / totalTasks) * 100;
          const remainingWeeks = Math.max(0, (roadmap.durationWeeks || 1) - new Set(roadmap.tasks?.filter(t => t.completed).map(t => t.weekNumber)).size);
          const remainingTasksCount = totalTasks - completedTasks;
          const isCompleted = completionPercent === 100 && totalTasks > 0;
          const currentViewMode = viewModeMap[roadmap.id] || 'list';

          const totalWeeks = roadmap.durationWeeks || 1;
          const generatedWeeks = roadmap.generatedWeeks || 0;
          const highestTaskWeek = roadmap.tasks?.reduce((max, task) => Math.max(max, task.weekNumber || 1), 0) || 0;
          const displayLimit = Math.max(highestTaskWeek, generatedWeeks, Math.min(12, totalWeeks));
          const sortedWeeks = Array.from({ length: displayLimit }, (_, i) => i + 1);
          const canLoadMore = generatedWeeks > 0 && generatedWeeks < totalWeeks && highestTaskWeek < totalWeeks;

          const tasksByWeek = {};
          roadmap.tasks?.forEach(task => {
            const week = task.weekNumber || 1;
            if (!tasksByWeek[week]) tasksByWeek[week] = [];
            tasksByWeek[week].push(task);
          });

          Object.keys(tasksByWeek).forEach(week => {
            tasksByWeek[week].sort((a, b) => {
              if (a.dayNumber === null && b.dayNumber !== null) return 1;
              if (a.dayNumber !== null && b.dayNumber === null) return -1;
              if (a.dayNumber === null && b.dayNumber === null) return 0;
              return (a.dayNumber || 999) - (b.dayNumber || 999);
            });
          });

          for (let i = 1; i <= totalWeeks; i++) {
            if (!tasksByWeek[i]) tasksByWeek[i] = [];
          }

          const handleExpandAll = () => {
            const newExpanded = {};
            sortedWeeks.forEach(week => {
              newExpanded[`${roadmap.id}-${week}`] = true;
            });
            setExpandedWeeks(prev => ({ ...prev, ...newExpanded }));
          };

          const handleCollapseAll = () => {
            const newExpanded = { ...expandedWeeks };
            sortedWeeks.forEach(week => {
              delete newExpanded[`${roadmap.id}-${week}`];
            });
            setExpandedWeeks(newExpanded);
          };

          return (
            <FadeIn delay={0.1 * (idx % 3)} direction="up" fullWidth key={roadmap.id}>
              <div className={`relative rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} transition duration-300 overflow-hidden group`}>

                {/* Gradient top bar */}
                <div className="absolute top-0 left-0 right-0 h-1.5 lg:h-2 bg-gradient-to-r from-purple-500 to-teal-500"></div>

                {/* Delete & View Toggle Buttons */}
                <div className="absolute top-4 right-4 lg:top-5 lg:right-5 flex items-center gap-2 z-10">
                  <button
                      onClick={() => toggleViewMode(roadmap.id)}
                      className="p-2 lg:p-2.5 rounded-full bg-slate-50/90 dark:bg-black/30 border border-slate-200/80 dark:border-white/5 hover:bg-slate-200 dark:hover:bg-black/50 text-slate-600 dark:text-gray-300 shadow-sm backdrop-blur-sm transition-all hover:scale-105"
                      title={currentViewMode === 'list' ? 'Switch to Timeline View' : 'Switch to List View'}
                  >
                      {currentViewMode === 'list' ? <BarChart2 className="w-4 h-4 lg:w-5 lg:h-5"/> : <List className="w-4 h-4 lg:w-5 lg:h-5"/>}
                  </button>
                  <button
                      onClick={() => handleDeleteClick(roadmap.id, roadmap.title)}
                      disabled={deleteMutation.isPending}
                      className="p-2 lg:p-2.5 rounded-full bg-slate-50/90 dark:bg-black/30 border border-slate-200/80 dark:border-white/5 hover:bg-red-50 dark:hover:bg-red-900/30 text-slate-400 hover:text-red-500 dark:hover:text-red-400 shadow-sm backdrop-blur-sm transition-all disabled:opacity-50 hover:scale-105"
                      title="Delete Roadmap"
                  >
                      <Trash2 className="w-4 h-4 lg:w-5 lg:h-5"/>
                  </button>
                </div>

                <div className="p-5 sm:p-6 lg:p-8 pt-10 sm:pt-8 lg:pt-10">

                  {/* Header Section */}
                  <div className="mb-6 lg:mb-8 pr-24 sm:pr-28">
                    <h3 className="text-xl sm:text-2xl lg:text-3xl font-poppins font-extrabold bg-gradient-to-r from-purple-600 to-teal-600 dark:from-purple-400 dark:to-teal-400 bg-clip-text text-transparent leading-tight mb-2">
                      {roadmap.title}
                    </h3>

                    <div className={`flex flex-wrap items-center gap-2 lg:gap-3 text-xs lg:text-sm ${textSecondary} font-medium mb-4`}>
                      <span className="flex items-center gap-1.5"><Calendar className="w-3.5 h-3.5"/> {new Date(roadmap.createdAt).toLocaleDateString()}</span>
                      <span className="hidden sm:inline opacity-30">•</span>
                      <span className="flex items-center gap-1.5"><Clock className="w-3.5 h-3.5"/> {roadmap.durationWeeks} weeks</span>
                    </div>

                    <span className={`inline-flex px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${
                      isCompleted
                        ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-500/30'
                        : roadmap.status === 'ACTIVE'
                        ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300 border border-blue-200 dark:border-blue-500/30'
                        : 'bg-slate-100 text-slate-800 dark:bg-black/30 dark:text-gray-300 border border-slate-200 dark:border-white/10'
                    }`}>
                      {isCompleted ? '🏆 Completed' : roadmap.status}
                    </span>
                  </div>

                  {/* Progress Dashboard (Layer 2) */}
                  <div className={`mb-8 lg:mb-10 p-5 lg:p-6 rounded-2xl ${sectionBg} border ${sectionBorder}`}>
                    <div className="flex justify-between items-end mb-3">
                      <span className={`text-sm lg:text-base font-bold flex items-center gap-2 ${textPrimary}`}>
                          <TrendingUp className="w-4 h-4 lg:w-5 lg:h-5 text-teal-500"/> Overall Progress
                      </span>
                      <span className={`text-xl lg:text-2xl font-poppins font-extrabold ${textPrimary}`}>
                          {Math.round(completionPercent)}%
                      </span>
                    </div>

                    <div className={`w-full ${isDarkMode ? 'bg-black/40' : 'bg-slate-200'} rounded-full h-3 lg:h-4 overflow-hidden border border-transparent dark:border-white/5 shadow-inner mb-4`}>
                      <div
                        className="bg-gradient-to-r from-purple-500 to-teal-500 h-full rounded-full transition-all duration-1000 ease-out shadow-[0_0_10px_rgba(20,184,166,0.5)]"
                        style={{ width: `${completionPercent}%` }}
                      ></div>
                    </div>

                    <div className={`flex flex-wrap justify-between gap-4 text-xs lg:text-sm font-medium ${textSecondary}`}>
                      <span className="flex items-center gap-1.5"><CheckCircle className="w-4 h-4 text-emerald-500"/> Completed: {completedTasks}/{totalTasks} tasks</span>
                      <span className="flex items-center gap-1.5"><Clock className="w-4 h-4 text-amber-500"/> Est. remaining: ~{remainingWeeks} weeks</span>
                    </div>

                    {generatedWeeks > 0 && generatedWeeks < totalWeeks && (
                      <div className={`mt-4 text-xs lg:text-sm font-bold text-purple-600 dark:text-teal-400 flex flex-wrap justify-between items-center border-t ${sectionBorder} pt-4`}>
                        <span className="flex items-center gap-1.5"><ListChecks className="w-4 h-4"/> Weeks generated: {generatedWeeks}/{totalWeeks}</span>
                        <span className={`font-medium ${textSecondary}`}>Click below to generate next weeks</span>
                      </div>
                    )}
                  </div>

                  {/* Conditional Rendering: List View or Timeline View */}
            {/* Conditional Rendering: List View or Timeline View */}
                              {currentViewMode === 'list' ? (
                                sortedWeeks.length > 0 && (
                                  <div className="mb-8 lg:mb-10">

                                    <div className="flex flex-wrap justify-between items-end gap-4 mb-5 lg:mb-6">
                                      <h4 className="text-lg lg:text-xl font-poppins font-extrabold flex items-center gap-2 text-purple-700 dark:text-teal-400">
                                        <Target className="w-5 h-5 lg:w-6 lg:h-6"/> Actionable Tasks
                                      </h4>
                                      <div className="flex gap-2">
                                        <button
                                          onClick={handleExpandAll}
                                          className="text-xs lg:text-sm font-bold px-3 py-1.5 lg:px-4 lg:py-2 rounded-lg bg-purple-100 dark:bg-teal-900/30 text-purple-700 dark:text-teal-300 hover:bg-purple-200 dark:hover:bg-teal-800/40 transition"
                                        >
                                          Expand All
                                        </button>
                                        <button
                                          onClick={handleCollapseAll}
                                          className={`text-xs lg:text-sm font-bold px-3 py-1.5 lg:px-4 lg:py-2 rounded-lg bg-slate-100 dark:bg-white/5 ${textPrimary} hover:bg-slate-200 dark:hover:bg-white/10 transition`}
                                        >
                                          Collapse All
                                        </button>
                                      </div>
                                    </div>

                                    <div className="space-y-4 lg:space-y-6">
                                      {sortedWeeks.map((week) => {
                                        const tasks = tasksByWeek[week];
                                        const isExpanded = expandedWeeks[`${roadmap.id}-${week}`];
                                        const weekCompleted = tasks.length > 0 && tasks.every(t => t.completed);
                                        return (
                                          <div key={week} className="border-l-4 border-purple-300 dark:border-teal-600/50 pl-4 lg:pl-6 ml-1 lg:ml-2 transition-all">
                                            <button
                                              onClick={() => toggleWeekExpand(roadmap.id, week)}
                                              className={`flex items-center gap-2 text-base lg:text-lg font-bold ${textPrimary} mb-3 hover:text-purple-600 dark:hover:text-teal-400 transition group`}
                                            >
                                              <span className="p-1 rounded-md bg-slate-100 dark:bg-white/5 group-hover:bg-purple-100 dark:group-hover:bg-teal-900/30 transition">
                                                  {isExpanded ? <ChevronUp className="w-4 h-4 lg:w-5 lg:h-5"/> : <ChevronDown className="w-4 h-4 lg:w-5 lg:h-5"/>}
                                              </span>
                                              Week {week}
                                              {weekCompleted && <span className="ml-2 text-[10px] lg:text-xs font-bold uppercase tracking-wider bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 px-2 py-0.5 rounded-full flex items-center gap-1"><CheckCircle className="w-3 h-3"/> Done</span>}
                                            </button>

                                            {isExpanded && (
                                              <div className="space-y-3 lg:space-y-4 mt-2">
                                              {tasks.length === 0 && (
                                                  <div className={`italic text-sm lg:text-base py-4 px-4 ${sectionBg} rounded-xl border ${sectionBorder} ${textSecondary}`}>
                                                    No tasks yet. Click the "Load Next Weeks" button below to generate your tasks.
                                                  </div>
                                              )}

                                                {tasks.map((task) => (
                                                  // 🌟 FIX: Applied Layer 3 to the task rows
                                                  <div key={task.id} className={`group ${innerContentBg} rounded-xl lg:rounded-2xl p-4 lg:p-5 border ${sectionBorder} hover:border-purple-300 dark:hover:border-teal-500/50 transition-colors shadow-sm`}>
                                                    <div className="flex items-start gap-3 lg:gap-4">
                                                      <button
                                                        onClick={() => toggleTaskMutation.mutate(task.id)}
                                                        className="focus:outline-none mt-0.5 lg:mt-1 hover:scale-110 active:scale-90 transition-transform shrink-0"
                                                        disabled={toggleTaskMutation.isPending}
                                                      >
                                                        {task.completed ? (
                                                          <CheckCircle className="w-5 h-5 lg:w-6 lg:h-6 text-emerald-500"/>
                                                        ) : (
                                                          <Circle className={`w-5 h-5 lg:w-6 lg:h-6 ${textSecondary} group-hover:text-purple-400 transition-colors`}/>
                                                        )}
                                                      </button>

                                                      <div className="flex-1 min-w-0">
                                                        <div className="flex flex-col sm:flex-row justify-between items-start gap-3">

                                                          <button
                                                            onClick={() => setSelectedTask(task)}
                                                            className="text-left font-semibold text-sm lg:text-base hover:text-purple-600 dark:hover:text-teal-400 transition-colors"
                                                          >
                                                            <span className={task.completed ? `line-through ${textSecondary}` : textPrimary}>
                                                              {task.description}
                                                              {task.dayNumber && <span className="text-xs font-medium text-purple-600 dark:text-teal-400 ml-2 bg-purple-50 dark:bg-teal-900/20 px-2 py-0.5 rounded-full inline-block mt-1 sm:mt-0">Day {task.dayNumber}</span>}
                                                            </span>
                                                          </button>

                                                          <div className="flex flex-wrap items-center gap-2 shrink-0 w-full sm:w-auto">
                                                            {task.importedToMilestone && (
                                                              <span className="text-[10px] lg:text-xs font-bold uppercase tracking-wider bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 px-2 py-1 rounded-full border border-emerald-200 dark:border-emerald-500/30 flex items-center gap-1">
                                                                <CheckCircle className="w-3 h-3"/> in Milestones
                                                              </span>
                                                            )}

                                                            <div className={`flex items-center gap-1.5 ml-auto sm:ml-0 bg-slate-50/50 dark:bg-black/20 p-1 rounded-lg border ${sectionBorder}`}>
                                                              {(task.details || (task.subtasks && task.subtasks.length > 0)) && (
                                                                  <button
                                                                  onClick={() => toggleTaskExpand(task.id)}
                                                                  className={`p-1.5 rounded-md transition-colors ${expandedTaskId === task.id ? 'bg-purple-500 dark:bg-teal-500 text-white' : `hover:bg-slate-200 dark:hover:bg-white/10 ${textSecondary}`}`}
                                                                  title={expandedTaskId === task.id ? 'Hide Details' : 'View Details'}
                                                                  >
                                                                  {expandedTaskId === task.id ? <ChevronUp className="w-4 h-4"/> : <ChevronDown className="w-4 h-4"/>}
                                                                  </button>
                                                              )}
                                                              <button
                                                                  onClick={() => importTaskToMilestone(roadmap.id, task.id, task.description)}
                                                                  className="text-xs font-bold bg-blue-500/10 hover:bg-blue-500/20 text-blue-600 dark:text-blue-400 px-3 py-1.5 rounded-md transition flex items-center gap-1"
                                                                  title="Add to Milestones"
                                                              >
                                                                  <Plus className="w-3 h-3"/> Add
                                                              </button>
                                                            </div>
                                                          </div>

                                                        </div>

                                                        {expandedTaskId === task.id && (
                                                          <motion.div
                                                            initial={{ opacity: 0 }}
                                                            animate={{ opacity: 1 }}
                                                            transition={{ duration: 0.25, ease: "easeOut" }}
                                                            className={`mt-4 pt-4 border-t ${sectionBorder} text-sm lg:text-base ${textPrimary}`}
                                                          >
                                                            <div className="roadmap-details" dangerouslySetInnerHTML={{ __html: formatText(task.details) }} />
                                                            {task.subtasks && task.subtasks.length > 0 && (
                                                              <ul className="list-disc list-outside ml-4 mt-3 space-y-1.5 marker:text-purple-500 dark:marker:text-teal-400">
                                                                {task.subtasks.map((sub, idx) => (
                                                                    <li key={idx} className="pl-1" dangerouslySetInnerHTML={{ __html: formatInline(sub) }} />
                                                                ))}
                                                              </ul>
                                                            )}
                                                          </motion.div>
                                                        )}

                                                      </div>
                                                    </div>
                                                  </div>
                                                ))}
                                              </div>
                                            )}
                                          </div>
                                        );
                                      })}
                                    </div>

                                    {canLoadMore && (
                                          <div className="mt-8 text-center">
                                          <button
                                              onClick={() => {
                                              setContinuingRoadmapId(roadmap.id);
                                              continueBatchMutation.mutate(
                                                  { roadmapId: roadmap.id, weeksToGenerate: 6 },
                                                  {
                                                  onSuccess: (updatedRoadmap) => {
                                                      queryClient.setQueryData(['roadmaps'], old =>
                                                      old.map(r => r.id === updatedRoadmap.id ? updatedRoadmap : r)
                                                      );
                                                      const newVisible = Math.min(updatedRoadmap.generatedWeeks, totalWeeks);
                                                      setVisibleWeeksMap(prev => ({ ...prev, [roadmap.id]: newVisible }));
                                                      toast.success(`Loaded weeks up to ${newVisible}`);
                                                  },
//                                               onError: (error) => {
//                                                           toast.error(`Continuation failed: ${error.response?.data?.message || error.message}`);
//                                                       },
                                                  onSettled: () => setContinuingRoadmapId(null)
                                                  }
                                              );
                                              }}
                                              disabled={continueBatchMutation.isPending && continuingRoadmapId === roadmap.id}
                                              className="px-6 py-3 rounded-full bg-gradient-to-r from-indigo-500 to-purple-500 hover:from-indigo-600 hover:to-purple-600 text-white font-bold text-sm lg:text-base shadow-md hover:shadow-lg transition-all disabled:opacity-50 flex items-center justify-center gap-2 mx-auto"
                                          >
                                              {continueBatchMutation.isPending && continuingRoadmapId === roadmap.id ? (
                                              <Loader className="animate-spin w-5 h-5"/>
                                              ) : (
                                              <Calendar className="w-5 h-5"/>
                                              )}
                                              Load Next 6 Weeks ({displayLimit}/{totalWeeks})
                                          </button>
                                          </div>
                                      )}
                                  </div>
                                )
                              ) : (
                                <div className="mb-8 lg:mb-10">
                                  <h4 className="text-lg lg:text-xl font-poppins font-extrabold flex items-center gap-2 text-teal-700 dark:text-teal-400 mb-6">
                                    <BarChart2 className="w-5 h-5 lg:w-6 lg:h-6"/> Timeline View
                                  </h4>
                                <RoadmapTimeline
                                  tasks={roadmap.tasks || []}
                                  durationWeeks={roadmap.durationWeeks || 1}
                                />
                                </div>
                              )}

                              {/* Resources (Layer 2) */}
                              {roadmap.resources && roadmap.resources.length > 0 && (
                                <div className={`mb-6 lg:mb-8 ${sectionBg} p-5 lg:p-6 rounded-2xl border ${sectionBorder}`}>
                                  <h4 className="text-sm lg:text-base font-bold uppercase tracking-wider mb-4 text-purple-700 dark:text-teal-400 flex items-center gap-2">
                                    <ExternalLink className="w-4 h-4 lg:w-5 lg:h-5"/> Recommended Resources
                                  </h4>
                                  <div className="flex flex-wrap gap-2 lg:gap-3">
                                    {roadmap.resources.map((res) => {
                                      let IconComponent;
                                      switch (res.type?.toLowerCase()) {
                                        case 'video': IconComponent = Video; break;
                                        case 'article': IconComponent = FileText; break;
                                        case 'course': IconComponent = GraduationCap; break;
                                        default: IconComponent = ExternalLink;
                                      }
                                      return (
                                        <a
                                          key={res.id}
                                          href={res.url}
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          className={`inline-flex items-center gap-1.5 lg:gap-2 px-4 py-2 rounded-xl ${innerContentBg} border ${sectionBorder} ${textPrimary} text-xs lg:text-sm font-medium hover:border-purple-400 dark:hover:border-teal-400 hover:text-purple-600 dark:hover:text-teal-400 transition-colors shadow-sm group`}
                                          title={res.type || 'resource'}
                                        >
                                          <IconComponent className="w-3.5 h-3.5 lg:w-4 lg:h-4 text-purple-400 dark:text-teal-500 group-hover:scale-110 transition-transform"/>
                                          <span>{res.name}</span>
                                        </a>
                                      );
                                    })}
                                  </div>
                                </div>
                              )}

                              {/* Milestones (Layer 2) */}
                              {roadmap.milestones && roadmap.milestones.length > 0 && (
                                <div className={`mb-6 lg:mb-8 ${sectionBg} p-5 lg:p-6 rounded-2xl border ${sectionBorder}`}>
                                  <h4 className="text-sm lg:text-base font-bold uppercase tracking-wider mb-4 text-amber-600 dark:text-amber-400 flex items-center gap-2">
                                    <Award className="w-4 h-4 lg:w-5 lg:h-5"/> Key Milestones
                                  </h4>
                                  <div className="flex flex-wrap gap-2 lg:gap-3">
                                    {roadmap.milestones.map((milestone) => (
                                      <span key={milestone.id} className={`px-4 py-2 rounded-xl ${innerContentBg} border border-amber-200/50 dark:border-amber-500/20 ${textPrimary} text-xs lg:text-sm font-medium flex items-center gap-2 shadow-sm`}>
                                        <Target className="w-3.5 h-3.5 lg:w-4 lg:h-4 text-amber-500"/>
                                        {milestone.name}
                                        {milestone.weekNumber && <span className={`opacity-60 text-[10px] lg:text-xs bg-slate-100 dark:bg-white/5 px-2 py-0.5 rounded-full ml-1`}>Week {milestone.weekNumber}</span>}
                                      </span>
                                    ))}
                                  </div>
                                </div>
                              )}

                              {/* Bottom Action Buttons */}
                              <div className={`flex flex-col sm:flex-row gap-3 lg:gap-4 mt-8 pt-6 border-t ${sectionBorder}`}>
                                  <button
                                    onClick={() => continueMutation.mutate(roadmap.id)}
                                    disabled={continueMutation.isPending || completedTasks === 0}
                                    className={`flex-1 py-3 lg:py-4 rounded-xl font-bold text-sm lg:text-base transition-all duration-300 active:scale-[0.98] flex items-center justify-center gap-2 ${
                                        completedTasks === 0
                                        ? 'bg-slate-100 dark:bg-white/5 text-gray-400 dark:text-gray-500 cursor-not-allowed border border-transparent dark:border-white/5'
                                        : 'bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-600 hover:to-teal-600 text-white shadow-md hover:shadow-lg'
                                    }`}
                                  >
                                    {continueMutation.isPending ? <Loader className="animate-spin w-5 h-5"/> : <Sparkles className="w-5 h-5"/>}
                                    {continueMutation.isPending ? 'Generating Next Steps...' : 'Generate Next Steps'}
                                  </button>

                                  <button
                                    onClick={() => rescheduleMutation.mutate(roadmap.id)}
                                    disabled={rescheduleMutation.isPending || remainingTasksCount === 0}
                                    className={`flex-1 py-3 lg:py-4 rounded-xl font-bold text-sm lg:text-base transition-all duration-300 active:scale-[0.98] flex items-center justify-center gap-2 ${
                                        remainingTasksCount === 0
                                        ? 'bg-slate-100 dark:bg-white/5 text-gray-400 dark:text-gray-500 cursor-not-allowed border border-transparent dark:border-white/5'
                                        : 'bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 text-white shadow-md hover:shadow-lg'
                                    }`}
                                  >
                                    {rescheduleMutation.isPending ? <Loader className="animate-spin w-5 h-5"/> : <Calendar className="w-5 h-5"/>}
                                    {rescheduleMutation.isPending ? 'Rescheduling...' : 'Smart Reschedule'}
                                  </button>
                              </div>
                            </div>
                          </div>
                        </FadeIn>
                      );
                    })}
                  </div>

                  <TaskDetailModal
                          isOpen={!!selectedTask}
                          onClose={() => setSelectedTask(null)}
                          task={selectedTask}
                          onElaborate={handleElaborate}
                          isElaborating={elaborateMutation.isPending}
                        />

                        <DeleteConfirmationModal
                          isOpen={deleteModal.isOpen}
                          onClose={() => setDeleteModal({ isOpen: false, roadmapId: null, roadmapTitle: '' })}
                          onConfirm={confirmDelete}
                          roadmapTitle={deleteModal.roadmapTitle}
                          theme={theme}
                          isLoading={isDeleting}
                        />

                </div>
              );
            }

            export default RoadmapPlanner;