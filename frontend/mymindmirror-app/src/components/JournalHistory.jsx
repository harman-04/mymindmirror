// src/components/JournalHistory.js
import React, { useState, useEffect, useRef, useMemo } from "react";
import ReactDOM from "react-dom";
import { format, parseISO } from "date-fns";
import { Doughnut } from "react-chartjs-2";
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from "chart.js";
import { useTheme } from "../contexts/ThemeContext";
import { useUpdateJournalEntry, useDeleteJournalEntry,  useImportGrowthTip, useJournalEntryById  } from '../hooks/useJournalData';
import { SkeletonCard, SkeletonAnalysis } from './Skeleton';
import { AlertTriangle, ChevronDown, ChevronUp, Edit, Trash2, BookOpen, Lightbulb, Heart, Brain, Target, Clock, Plus, Loader, Sparkles } from 'lucide-react';
import { downloadChartAsPng } from '../utils/downloadChart';
import DownloadChartButton from './DownloadChartButton';
import { useWindowVirtualizer } from '@tanstack/react-virtual';
import { motion } from 'framer-motion';
import PremiumInput from './PremiumInput'; // 🌟 NEW: Import Design System
import { toast } from 'sonner'; // 🌟 NEW: Import Toasts for mutations
// 💡 UPGRADED: Enterprise Markdown Parser
const formatText = (text) => {
  if (!text) return '';

  // Prevent AI from squishing numbered lists into a single paragraph
  let processedText = text.replace(/([:;?!.])\s+(?=\d+\.\s+[A-Z])/g, '$1\n');

  const escapeHtml = (str) => {
    return str.replace(/[&<>]/g, (m) => {
      if (m === '&') return '&amp;';
      if (m === '<') return '&lt;';
      if (m === '>') return '&gt;';
      return m;
    });
  };

  const applyMarkdown = (str) => {
    let formatted = escapeHtml(str);

    // 💡 NEW: Inline Code
    formatted = formatted.replace(/`([^`]+)`/g, '<code class="guide-code">$1</code>');

    // 💡 NEW: @Annotations (Uses our new index.css class!)
    formatted = formatted.replace(/(^|[\s([{])(@[A-Za-z0-9_]+)/g, '$1<span class="code-annotation">$2</span>');

    // Bold
    formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // Italics
    formatted = formatted.replace(/\*(.*?)\*/g, '<em>$1</em>');

    // Links with SVG icon
    formatted = formatted.replace(
      /\[([^\]]+)\]\(([^)]+)\)/g,
      `<a href="$2" target="_blank" rel="noopener noreferrer" class="resource-link">
        $1 <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display:inline; margin-left:2px; vertical-align:middle;"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path><polyline points="15 3 21 3 21 9"></polyline><line x1="10" y1="14" x2="21" y2="3"></line></svg>
      </a>`
    );

    return formatted;
  };

  const lines = processedText.split('\n');
  const result = [];
  let i = 0;
  const total = lines.length;

  const processBlockquote = (startIdx) => {
    const quoteLines = [];
    let j = startIdx;
    while (j < total && lines[j].startsWith('> ')) {
      quoteLines.push(lines[j].substring(2));
      j++;
    }
    const innerHtml = formatText(quoteLines.join('\n'));
    result.push(`<blockquote class="guide-blockquote">${innerHtml}</blockquote>`);
    return j;
  };

  while (i < total) {
    const line = lines[i];

if (/^(\*{3,}|-{3,}|_{3,})$/.test(line.trim())) {
      result.push('<hr class="guide-hr border-gray-200 dark:border-gray-700/50 my-4" />');
      i++;
      continue;
    }

   // 🌟 THE ULTIMATE HEADING PARSER (AI-Hallucination Proof)
       let cleanLine = line.trim();

       // 1. Match 1-6 hashes, even if they are wrapped in bold/italic stars
       const headingMatch = cleanLine.match(/^[\s*_]*(#{1,6})\s*(.*)/);

       if (headingMatch) {
         const level = headingMatch[1].length;

         // 2. Extract the text after the primary hashes
         let headingText = headingMatch[2];

         // 3. Strip any bold/italic markers that might have been INSIDE the hashes (e.g. ## **Title**)
         headingText = headingText.replace(/^[\s*_]+/, '');

         // 4. THE FIX: Catch the edge case where the AI double-hashes (e.g. ## ## Title or ### ## Title)
         // This specifically requires a space after the hash so we don't accidentally delete "#1"
         headingText = headingText.replace(/^(#{1,6})\s+/, '');

         // 5. Strip trailing bold/italic markers
         headingText = headingText.replace(/[\s*_]+$/, '').trim();

         // 6. Force premium Enterprise styling on the heading
         result.push(`<h${level} class="guide-heading mt-6 mb-3 font-poppins font-bold text-gray-900 dark:text-gray-100">${applyMarkdown(headingText)}</h${level}>`);
         i++;
         continue;
       }
    if (line.startsWith('> ')) {
      i = processBlockquote(i);
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
          listItems.push(`<li>${applyMarkdown(content)}</li>`);
          i++;
        } else {
          break;
        }
      }
      result.push(`<${isOrdered ? 'ol' : 'ul'} class="guide-list">${listItems.join('')}</${isOrdered ? 'ol' : 'ul'}>`);
      continue;
    }

    if (line.trim()) {
      result.push(`<p class="guide-paragraph">${applyMarkdown(line)}</p>`);
    } else if (line === '') {
      result.push('<br/>');
    }
    i++;
  }
  return result.join('');
};

const TRUNCATION_LENGTH = 150;

ChartJS.register(ArcElement, Tooltip, Legend);

const EMOTION_CHART_COLORS = {
  joy: "#5CC8C2", sadness: "#B399D4", anger: "#FF8A7A", fear: "#A93226",
  surprise: "#85C1E9", neutral: "#E0E0E0", love: "#E74C3C", disgust: "#6C3483",
  anxiety: "#F7DC6F", optimism: "#F1C40F", relief: "#58D68D", caring: "#2ECC71",
  curiosity: "#AF7AC5", embarrassment: "#D35400", pride: "#F39C12", remorse: "#7F8C8D",
  annoyance: "#E67E22", disappointment: "#283747", grief: "#17202A", excitement: "#FFD700",
  contentment: "#90EE90", frustration: "#FF4500", gratitude: "#ADFF2F", hope: "#ADD8E6",
};

const EMOTION_CHART_COLORS_DARK = {
  joy: "#8DE2DD", sadness: "#C7B3E6", anger: "#FFB0A4", fear: "#D45E4D",
  surprise: "#B0D9F7", neutral: "#A0A0A0", love: "#FF7F7F", disgust: "#9B6EB4",
  anxiety: "#FFF0B3", optimism: "#FFD750", relief: "#8CE0B0", caring: "#58D68D",
  curiosity: "#C79BE0", embarrassment: "#FF8C40", pride: "#FFC050", remorse: "#B0B8B8",
  annoyance: "#FFAB66", disappointment: "#506A80", grief: "#404040", excitement: "#FFE680",
  contentment: "#C0FFC0", frustration: "#FF7F50", gratitude: "#D0FF80", hope: "#C0E0FF",
};

const emotionChipColors = {
  joy: "bg-green-500", sadness: "bg-blue-500", anger: "bg-red-500", fear: "bg-purple-500",
  surprise: "bg-yellow-500", disgust: "bg-indigo-500", love: "bg-pink-500", anxiety: "bg-orange-500",
  relief: "bg-teal-500", neutral: "bg-gray-500", excitement: "bg-lime-500", contentment: "bg-emerald-500",
  frustration: "bg-rose-500", gratitude: "bg-amber-500", hope: "bg-cyan-500",
};

const DeleteConfirmationModal = ({ isOpen, onClose, onConfirm, theme, isDeleting }) => {
  const [mounted, setMounted] = useState(false);
  useEffect(() => {
    setMounted(true);
    if (isOpen) document.body.style.overflow = 'hidden';
    else document.body.style.overflow = 'unset';
    return () => { document.body.style.overflow = 'unset'; };
  }, [isOpen]);
  if (!isOpen || !mounted) return null;
  return ReactDOM.createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4" style={{ backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)' }}>
      <div className="absolute inset-0" onClick={onClose} aria-hidden="true" />
      <div className={`relative max-w-md w-full rounded-2xl lg:rounded-3xl shadow-2xl overflow-hidden transform transition-all duration-300 border ${
              theme === 'dark' ? 'bg-[#1A162F]/95 border-white/10' : 'bg-white/95 border-slate-200/80'
            }`}>
        <div className="p-6 lg:p-8 text-center">
          <div className="mx-auto w-14 h-14 lg:w-16 lg:h-16 mb-4 lg:mb-6 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
            <AlertTriangle className="w-7 h-7 lg:w-8 lg:h-8 text-red-600 dark:text-red-400" />
          </div>
          <h3 className="text-xl lg:text-2xl font-poppins font-bold mb-2 tracking-tight text-gray-800 dark:text-gray-100">Delete Entry?</h3>
          <p className="text-sm lg:text-base text-gray-600 dark:text-gray-400 mb-6 lg:mb-8">Are you sure you want to delete this entry? This action cannot be undone.</p>
          <div className="flex justify-center gap-3">
            <button
                onClick={onConfirm}
                disabled={isDeleting}
                className="px-6 py-2.5 lg:py-3 rounded-full bg-red-600 hover:bg-red-700 text-white flex items-center gap-2 font-medium disabled:opacity-50 transition-colors shadow-sm"
            >
                {isDeleting ? <Loader size={18} className="animate-spin" /> : null}
                {isDeleting ? "Deleting..." : "Delete"}
            </button>
            <button
                onClick={onClose}
                disabled={isDeleting}
                className="px-6 py-2.5 lg:py-3 rounded-full bg-gray-200 hover:bg-gray-300 dark:bg-gray-800 dark:hover:bg-gray-700 text-gray-800 dark:text-gray-200 font-medium disabled:opacity-50 transition-colors shadow-sm"
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

const ExpandedEntryContent = ({ entry, isDarkMode, getMoodColorClass, getMoodLabel, getChipStyle, emotionChartOptions, importGrowthTipMutation, isPending }) => {

  // 🌟 NESTED ELEVATION PALETTE (Layer 3)
  // Because this sits inside the row (Layer 2), it alternates back to pure white (Light) or deep black (Dark)
  const innerContentBg = isDarkMode ? 'bg-black/20 shadow-inner' : 'bg-white shadow-sm';
  const innerContentBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  const parsedEmotions = entry.emotions && typeof entry.emotions === "string" ? JSON.parse(entry.emotions) : entry.emotions || {};
  const parsedCoreConcerns = entry.coreConcerns && typeof entry.coreConcerns === "string" ? JSON.parse(entry.coreConcerns) : entry.coreConcerns || [];
  const parsedGrowthTips = entry.growthTips && typeof entry.growthTips === "string" ? JSON.parse(entry.growthTips) : entry.growthTips || [];
  const parsedKeyPhrases = Array.isArray(entry.keyPhrases) ? entry.keyPhrases : [];

  const localChartRef = useRef(null);

  const chartData = (() => {
    if (!entry.emotions) return null;
    try {
      const emotions = typeof entry.emotions === "string" ? JSON.parse(entry.emotions) : entry.emotions;
      if (!emotions || Object.keys(emotions).length === 0) return null;
      const labels = Object.keys(emotions);
      const data = Object.values(emotions);
      const filteredLabels = [];
      const filteredData = [];
      labels.forEach((label, idx) => {
        if (data[idx] > 0.01) {
          filteredLabels.push(label.charAt(0).toUpperCase() + label.slice(1));
          filteredData.push(data[idx]);
        }
      });
      if (filteredLabels.length === 0) return null;
      const palette = isDarkMode ? EMOTION_CHART_COLORS_DARK : EMOTION_CHART_COLORS;
      const backgroundColors = filteredLabels.map(l => palette[l.toLowerCase()] || "#CCCCCC");
      return {
        labels: filteredLabels,
        datasets: [{
          data: filteredData,
          backgroundColor: backgroundColors,
          // Cutout matches the deep background perfectly
          borderColor: isDarkMode ? '#0B0914' : '#ffffff',
          borderWidth: isDarkMode ? 1 : 2,
          hoverOffset: 6
        }],
      };
    } catch (e) {
      return null;
    }
  })();

  return (
    <div className="space-y-4 lg:space-y-6">
      {/* Raw Text */}
      <div className={`rounded-xl lg:rounded-2xl p-4 lg:p-6 ${innerContentBg} border ${innerContentBorder}`}>
        <div className="flex items-center gap-2 mb-3 lg:mb-4">
          {/* 🌟 FIX: Clean, borderless icons for deep content */}
          <BookOpen className="w-5 h-5 text-purple-500 dark:text-purple-400 shrink-0" />
          <span className={`text-xs lg:text-sm font-bold uppercase tracking-wider ${textSecondary}`}>Journal Entry</span>
        </div>
        <p className={`text-sm lg:text-base ${textPrimary} leading-relaxed lg:leading-loose whitespace-pre-wrap`}>{entry.rawText}</p>
      </div>

      {/* Summary */}
      {entry.summary && (
        <div className={`rounded-xl lg:rounded-2xl p-4 lg:p-6 ${innerContentBg} border ${innerContentBorder}`}>
          <div className="flex items-center gap-2 mb-3 lg:mb-4">
            <Brain className="w-5 h-5 text-teal-500 dark:text-teal-400 shrink-0" />
            <span className={`text-xs lg:text-sm font-bold uppercase tracking-wider ${textSecondary}`}>Summary</span>
          </div>
          <p className={`text-sm lg:text-base ${textPrimary} leading-relaxed`}>{entry.summary}</p>
        </div>
      )}

      {/* Mood Score */}
      <div className={`rounded-xl lg:rounded-2xl p-4 lg:p-6 ${innerContentBg} border ${innerContentBorder}`}>
        <div className="flex flex-wrap gap-2 justify-between items-center">
          <span className={`text-xs lg:text-sm font-bold uppercase tracking-wider ${textSecondary}`}>Mood Score</span>
          <span className={`font-bold text-lg lg:text-2xl ${getMoodColorClass(entry.moodScore)}`}>
            {entry.moodScore?.toFixed(2) ?? "N/A"} <span className="text-sm lg:text-lg opacity-80">({getMoodLabel(entry.moodScore)})</span>
          </span>
        </div>
      </div>

      {/* Emotions */}
      {Object.keys(parsedEmotions).length > 0 && (
        <div className={`rounded-xl lg:rounded-2xl p-4 lg:p-6 ${innerContentBg} border ${innerContentBorder}`}>
          <div className="flex items-center gap-2 mb-4 lg:mb-5">
            <Heart className="w-5 h-5 text-pink-500 dark:text-pink-400 shrink-0" />
            <span className={`text-xs lg:text-sm font-bold uppercase tracking-wider ${textSecondary}`}>Emotions</span>
          </div>
          <div className="flex flex-wrap gap-2 lg:gap-3">
            {Object.entries(parsedEmotions).filter(([,score]) => score > 0).sort((a,b)=>b[1]-a[1]).map(([emotion, score]) => (
              <span key={emotion} className={`${getChipStyle(emotion)} inline-flex items-center gap-1.5 px-3 lg:px-4 py-1.5 lg:py-2 rounded-full text-xs lg:text-sm font-medium shadow-sm hover:-translate-y-0.5 transition-transform`}>
                {emotion} <span className="opacity-80 text-[10px] lg:text-xs">({score.toFixed(2)})</span>
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Core Concerns & Key Phrases Group */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 lg:gap-6">
        {parsedCoreConcerns.length > 0 && (
          <div className={`rounded-xl lg:rounded-2xl p-4 lg:p-6 ${innerContentBg} border ${innerContentBorder}`}>
            <div className="flex items-center gap-2 mb-4 lg:mb-5">
              <Target className="w-5 h-5 text-blue-500 dark:text-cyan-400 shrink-0" />
              <span className={`text-xs lg:text-sm font-bold uppercase tracking-wider ${textSecondary}`}>Core Concerns</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {parsedCoreConcerns.map((c, idx) => (
                <span key={idx} className="bg-blue-50 dark:bg-blue-500/10 text-blue-700 dark:text-blue-300 px-3 py-1.5 rounded-lg text-xs font-bold border border-blue-200 dark:border-blue-500/20">
                  {c}
                </span>
              ))}
            </div>
          </div>
        )}

        {parsedKeyPhrases.length > 0 && (
          <div className={`rounded-xl lg:rounded-2xl p-4 lg:p-6 ${innerContentBg} border ${innerContentBorder}`}>
            <div className="flex items-center gap-2 mb-4 lg:mb-5">
              <Target className="w-5 h-5 text-indigo-500 dark:text-indigo-400 shrink-0" />
              <span className={`text-xs lg:text-sm font-bold uppercase tracking-wider ${textSecondary}`}>Key Phrases</span>
            </div>
            <div className="flex flex-wrap gap-2">
              {parsedKeyPhrases.map((p, idx) => (
                <span key={idx} className="bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 px-3 py-1.5 rounded-lg text-xs font-bold border border-indigo-200 dark:border-indigo-500/20">
                  {p}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Growth Tips Section */}
      {parsedGrowthTips.length > 0 && (
        <div className={`rounded-xl lg:rounded-2xl p-4 lg:p-6 ${innerContentBg} border ${innerContentBorder}`}>
          <div className="flex items-center gap-2 mb-4 lg:mb-5">
            <Lightbulb className="w-5 h-5 text-amber-500 dark:text-amber-400 shrink-0" />
            <span className={`text-xs lg:text-sm font-bold uppercase tracking-wider ${textSecondary}`}>Growth Tips & Resources</span>
          </div>
          <div className="space-y-4 lg:space-y-6">
            {parsedGrowthTips.map((tip, idx) => (
              <div key={idx} className={`relative group bg-slate-50/50 dark:bg-[#131127]/50 p-4 lg:p-6 rounded-xl border border-slate-200/50 dark:border-white/5`}>
                <div className={`pr-10 lg:pr-14 prose prose-sm lg:prose-base dark:prose-invert max-w-none ${textPrimary}`}>
                  <div className="growth-tip-content" dangerouslySetInnerHTML={{ __html: formatText(tip) }} />
                </div>
                <button
                  onClick={() => importGrowthTipMutation.mutate(tip)}
                  disabled={isPending}
                  className="absolute top-4 right-4 p-2.5 rounded-full bg-blue-50 hover:bg-blue-100 dark:bg-blue-500/10 dark:hover:bg-blue-500/20 text-blue-600 dark:text-blue-400 transition-all disabled:opacity-50 flex items-center justify-center shadow-sm"
                  title="Add to Milestones"
                >
                  <Plus className="w-4 h-4 lg:w-5 lg:h-5" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Emotion Breakdown Chart */}
      {chartData && chartData.datasets[0].data.length > 0 ? (
        <div className={`rounded-xl lg:rounded-2xl ${innerContentBg} border ${innerContentBorder} overflow-hidden`}>
          <div className={`flex flex-wrap gap-3 justify-between items-center p-4 lg:p-6 border-b ${innerContentBorder} bg-slate-50/50 dark:bg-[#131127]/50`}>
            <span className={`text-sm lg:text-base font-bold tracking-tight ${textPrimary}`}>Emotion Breakdown</span>
            <DownloadChartButton
              chartRef={localChartRef}
              filename={`entry_emotion_breakdown_${entry.id}`}
              darkMode={isDarkMode}
              className="hover:scale-105 transition-transform shrink-0"
            />
          </div>

          <div ref={localChartRef} className="p-4 sm:p-6 lg:p-8 flex flex-col items-center">
            <div className={`text-center mb-4 text-xs lg:text-sm font-medium ${textSecondary}`}>
              {format(parseISO(entry.entryDate), 'EEEE, MMMM d, yyyy')}
              {entry.creationTimestamp && <span className="ml-2 opacity-70">• {format(parseISO(entry.creationTimestamp), 'h:mm a')}</span>}
            </div>
            <div className="relative w-full h-[220px] sm:h-[260px] lg:h-[300px] flex items-center justify-center">
               <Doughnut key={`doughnut-${entry.id}`} data={chartData} options={emotionChartOptions} />
            </div>
          </div>
        </div>
      ) : (
        // 🌟 FIX: Restored Empty Data State!
        <div className={`text-center p-6 lg:p-8 rounded-xl lg:rounded-2xl ${innerContentBg} border ${innerContentBorder} ${textSecondary} italic text-sm lg:text-base`}>
          No detailed emotion data for this entry.
        </div>
      )}
    </div>
  );
};


function JournalHistory({ entries, filterPhrase, isLoading, searchType }) {
  const [expandedEntryId, setExpandedEntryId] = useState(null);
  const [editingEntryId, setEditingEntryId] = useState(null);
  const [editedText, setEditedText] = useState("");
  const [editError, setEditError] = useState("");
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteEntryId, setDeleteEntryId] = useState(null);
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  const updateMutation = useUpdateJournalEntry();
  const deleteMutation = useDeleteJournalEntry();
  const importGrowthTipMutation = useImportGrowthTip();

  const isProcessing = (entry) => entry && entry.moodScore === null;

  const { data: fetchedEntry, isLoading: isFetchingEntry } = useJournalEntryById(
    expandedEntryId,
    expandedEntryId !== null &&
      editingEntryId !== expandedEntryId &&
      isProcessing(entries?.find(e => e.id === expandedEntryId))
  );

// ==========================================================================
  // 🌟 NESTED ELEVATION PALETTE (Layer 2 context)
  // ==========================================================================
  // 🌟 UX UPGRADE: Split into a Two-Tone palette for beautiful collapsed contrast!
  const rowBaseBg = isDarkMode ? 'bg-[#131127]/90' : 'bg-white';
  const rowHeaderBg = isDarkMode ? 'bg-black/30' : 'bg-slate-50/80';
  const rowBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  const [isMobile, setIsMobile] = useState(window.innerWidth < 640);
  useEffect(() => {
      const handleResize = () => setIsMobile(window.innerWidth < 640);
      window.addEventListener('resize', handleResize);
      return () => window.removeEventListener('resize', handleResize);
  }, []);

  const emotionChartOptions = {
//     animation: false,
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: isMobile ? "bottom" : "right",
        labels: {
          color: isDarkMode ? '#E0E0E0' : '#2D3748',
          font: { family: "Inter", size: isMobile ? 10 : 12 },
          boxWidth: isMobile ? 8 : 12,
          padding: isMobile ? 12 : 16,
          usePointStyle: true,
        },
      },
      tooltip: {
        backgroundColor: isDarkMode ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.95)',
        titleColor: isDarkMode ? '#E0E0E0' : '#1E1A3E',
        bodyColor: isDarkMode ? '#CBD5E1' : '#475569',
        borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
        borderWidth: 1,
        padding: 12,
        boxPadding: 6,
        usePointStyle: true,
        callbacks: {
          label: (ctx) => `${ctx.label}: ${(ctx.parsed * 100).toFixed(1)}%`,
        },
      },
    },
    cutout: isMobile ? "60%" : "65%",
  };

//   if (isLoading) return <SkeletonCard count={3} />;

  let filteredEntries = entries;
  if (filterPhrase) {
    const lowerPhrase = filterPhrase.toLowerCase();
    filteredEntries = filteredEntries.filter(entry =>
      Array.isArray(entry.keyPhrases) && entry.keyPhrases.some(phrase => phrase.toLowerCase().includes(lowerPhrase))
    );
  }

  let groupedEntries = {};
  let sortedDates = [];

  if (searchType === 'semantic') {
    const semanticKey = "Matches Ranked by AI Relevance";
    groupedEntries[semanticKey] = filteredEntries;
    sortedDates = [semanticKey];
  } else {
    groupedEntries = filteredEntries.reduce((acc, entry) => {
      const dateKey = format(parseISO(entry.entryDate), "EEEE, MMMM dd, yyyy");
      if (!acc[dateKey]) acc[dateKey] = [];
      acc[dateKey].push(entry);
      acc[dateKey].sort((a, b) => parseISO(b.creationTimestamp) - parseISO(a.creationTimestamp));
      return acc;
    }, {});
    sortedDates = Object.keys(groupedEntries).sort((a, b) => {
      const dateA = parseISO(a.split(", ")[1] + ", " + a.split(", ")[2]);
      const dateB = parseISO(b.split(", ")[1] + ", " + b.split(", ")[2]);
      return dateB - dateA;
    });
  }

  const flattenedItems = useMemo(() => {
    const items = [];
    sortedDates.forEach(dateKey => {
      items.push({ type: 'header', id: `header-${dateKey}`, data: dateKey });
      groupedEntries[dateKey].forEach(entry => {
        items.push({ type: 'entry', id: `entry-${entry.id}`, data: entry });
      });
    });
    return items;
  }, [sortedDates, groupedEntries]);

  const listRef = useRef(null);
  const [listOffset, setListOffset] = useState(0);

  useEffect(() => {
    if (listRef.current) {
      setListOffset(listRef.current.getBoundingClientRect().top + window.scrollY);
    }
  }, [flattenedItems.length]);

  const virtualizer = useWindowVirtualizer({
    count: flattenedItems.length,
    estimateSize: () => 180,
    overscan: 4,
    scrollMargin: listOffset,
  });

  const toggleExpand = (id) => {
    setExpandedEntryId(expandedEntryId === id ? null : id);
    if (expandedEntryId === id && editingEntryId === id) handleCancelEdit();
  };

  const scrollToEditArea = (entryId) => {
    setTimeout(() => {
      const el = document.getElementById(`edit-area-${entryId}`);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 100);
  };

  const handleEditClick = (entry) => {
    setEditingEntryId(entry.id);
    setEditedText(entry.rawText);
    setEditError("");
    setExpandedEntryId(entry.id);
    scrollToEditArea(entry.id);
  };

const handleSaveEdit = async (entryId) => {
    if (!editedText.trim()) {
      setEditError("Journal entry cannot be empty.");
      return;
    }
    try {
      await updateMutation.mutateAsync({ entryId, updatedText: editedText });
      setEditingEntryId(null);
      setEditedText("");
      setEditError("");
      toast.success("Entry updated successfully!"); // 🌟 UX UPGRADE: Toast Success
    } catch (err) {
      setEditError("Failed to update entry. Please try again.");
    }
  };

  const handleCancelEdit = () => {
    setEditingEntryId(null);
    setEditedText("");
    setEditError("");
  };

  const handleDeleteClick = (id) => {
    setDeleteEntryId(id);
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    try {
      const idToDelete = deleteEntryId;
      await deleteMutation.mutateAsync(idToDelete);
      if (expandedEntryId === idToDelete) {
        setExpandedEntryId(null);
      }
      setShowDeleteConfirm(false);
      setDeleteEntryId(null);
      toast.success("Entry deleted successfully!"); // 🌟 UX UPGRADE: Toast Success
    } catch (err) {
      setShowDeleteConfirm(false);
      setDeleteEntryId(null);
//       toast.error("Failed to delete entry."); // 🌟 UX UPGRADE: Toast Error
    }
  };
  const cancelDelete = () => {
    setShowDeleteConfirm(false);
    setDeleteEntryId(null);
  };

  const getMoodColorClass = (moodScore) => {
    if (moodScore === null || isNaN(moodScore)) return "text-gray-500 dark:text-gray-400";
    if (moodScore >= 0.7) return "text-green-500 dark:text-green-400";
    if (moodScore >= 0.3) return "text-lime-500 dark:text-lime-400";
    if (moodScore > -0.3 && moodScore < 0.3) return "text-yellow-500 dark:text-yellow-400";
    if (moodScore <= -0.7) return "text-red-500 dark:text-red-400";
    return "text-orange-500 dark:text-orange-400";
  };

  const getMoodLabel = (moodScore) => {
    if (moodScore === null || isNaN(moodScore)) return "N/A";
    if (moodScore >= 0.7) return "Very Positive";
    if (moodScore >= 0.3) return "Positive";
    if (moodScore > -0.3 && moodScore < 0.3) return "Neutral";
    if (moodScore <= -0.7) return "Very Negative";
    return "Negative";
  };

  const getChipStyle = (emotion) => {
    const base = emotionChipColors[emotion.toLowerCase()] || "bg-gray-500";
    return `${base} text-white text-xs px-2 py-1 rounded-full`;
  };

  // 💡 NEW: The Smart Rendering Threshold
  // If there are less than 20 items (Today/Weekly tabs), bypass the virtualizer
  // to prevent the mathematical layout positioning bug.
  const shouldVirtualize = flattenedItems.length > 20;

if (isLoading) return <SkeletonCard count={3} />;
  // 💡 NEW: Extracted UI logic so we can render it safely in both modes
  const renderItemContent = (item) => {
      // 🌟 LAYER 3 PALETTE (For the Skeleton Loader inside the expanded row)
            const innerContentBg = isDarkMode ? 'bg-black/20' : 'bg-white';
            const innerContentBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
      if (item.type === 'header') {
          return (
            <h3 className="text-xl lg:text-2xl font-poppins font-bold text-purple-600 dark:text-teal-400 mb-2 pb-2 border-b border-purple-200 dark:border-teal-800/50 tracking-tight">
              {searchType === 'semantic' ? (
                  <span className="flex items-center gap-2 lg:gap-3">
                      <Sparkles className="w-5 h-5 lg:w-6 lg:h-6 text-purple-600 dark:text-teal-400" /> {item.data}
                  </span>
              ) : item.data}
            </h3>
          );
      }

      const entry = item.data;
      const isThisEntryExpanded = expandedEntryId === entry.id;
      const displayEntry = (isThisEntryExpanded && fetchedEntry && fetchedEntry.id === entry.id)
          ? fetchedEntry : entry;

      const isProcessingEntry = displayEntry.moodScore === null;
      const showSkeleton = isThisEntryExpanded && isProcessingEntry;

    return (
            <div className={`rounded-2xl lg:rounded-3xl ${rowBaseBg} border ${rowBorder} shadow-sm ring-1 ring-black/5 dark:ring-white/5 transition-all duration-300 overflow-hidden hover:shadow-md hover:-translate-y-0.5 flex flex-col`}>

              {/* 🌟 UX UPGRADE: The Two-Tone Header! It is now distinctly darker/lighter than the body. */}
              <div className={`p-3.5 lg:p-5 cursor-pointer flex flex-wrap justify-between items-center ${rowHeaderBg} border-b ${rowBorder} hover:brightness-95 dark:hover:brightness-110 transition-all gap-3`} onClick={() => toggleExpand(entry.id)}>
                <div className="flex flex-wrap items-center gap-3 lg:gap-4">
                    <Clock className="w-4 h-4 lg:w-5 lg:h-5 text-purple-500 dark:text-teal-400" />
                    <span className={`text-sm lg:text-base font-semibold ${textPrimary}`}>
                        {searchType === 'semantic' && entry.creationTimestamp
                            ? format(parseISO(entry.creationTimestamp), "MMM d, yyyy • h:mm a")
                            : entry.creationTimestamp ? format(parseISO(entry.creationTimestamp), "h:mm a") : "N/A"}
                    </span>
                   <span className={`text-sm lg:text-base font-bold flex items-center gap-2 ${isProcessingEntry ? 'text-purple-500' : getMoodColorClass(displayEntry.moodScore)}`}>
                     {isProcessingEntry ? (
                       <span className="inline-flex items-center gap-1.5 bg-purple-500/10 px-2.5 py-1 rounded-md animate-pulse">
                         <Loader className="w-3.5 h-3.5 lg:w-4 lg:h-4 animate-spin" /> Analyzing...
                       </span>
                     ) : (
                       <>
                         {getMoodLabel(displayEntry.moodScore)}
                         <span className="text-xs lg:text-sm font-medium opacity-70 hidden sm:inline-block">({displayEntry.moodScore?.toFixed(2)})</span>
                       </>
                     )}
                   </span>
                </div>
                <div className={`${textSecondary} shrink-0 ml-auto sm:ml-2`}>
                    {expandedEntryId === entry.id ? <ChevronUp className="w-5 h-5 lg:w-6 lg:h-6" /> : <ChevronDown className="w-5 h-5 lg:w-6 lg:h-6" />}
                </div>
              </div>

              {/* 🌟 UX UPGRADE: Collapsed Preview with refined padding and cleaner truncation */}
              {expandedEntryId !== entry.id && (
                  <div className={`p-4 lg:p-6 text-sm lg:text-base ${textSecondary} leading-relaxed`}>
                      {entry.rawText.length > TRUNCATION_LENGTH ? `${entry.rawText.slice(0, TRUNCATION_LENGTH)}...` : entry.rawText}
                  </div>
              )}

              {/* Expanded Content */}
              {expandedEntryId === entry.id && (
                  <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.25, ease: "easeOut" }} className="p-4 lg:p-6 pt-3 space-y-4 lg:space-y-6">
                     {editingEntryId === entry.id ? (
                         <div id={`edit-area-${entry.id}`} className="space-y-3 lg:space-y-4 pt-3">
                           <PremiumInput
                               multiline={true}
                               rows={6}
                               value={editedText}
                               onChange={(e) => {
                                   setEditedText(e.target.value);
                                   if (editError) setEditError("");
                               }}
                               placeholder="Edit your journal entry..."
                               error={editError}
                               showError={!!editError}
                               disabled={updateMutation.isPending}
                           />
                           <div className="flex flex-wrap justify-end gap-3 mt-3">
                              <button onClick={handleCancelEdit} className="px-5 py-2 lg:py-2.5 rounded-full bg-slate-200 dark:bg-white/10 hover:bg-slate-300 dark:hover:bg-white/20 transition-colors text-sm lg:text-base font-semibold text-slate-800 dark:text-gray-200">Cancel</button>
                              <button onClick={() => handleSaveEdit(entry.id)} disabled={updateMutation.isPending} className="px-5 py-2 lg:py-2.5 rounded-full bg-gradient-to-r from-purple-500 to-teal-500 hover:from-purple-600 hover:to-teal-600 text-white font-semibold shadow-md hover:shadow-lg transition-all text-sm lg:text-base disabled:opacity-50">
                                {updateMutation.isPending ? "Saving..." : "Save Changes"}
                              </button>
                            </div>
                          </div>
                       ) : showSkeleton ? (
                           <div className="pt-3">
                               <SkeletonAnalysis sectionBg={innerContentBg} cardBorder={innerContentBorder} />
                           </div>
                       ) : (
                           <>
                              <ExpandedEntryContent
                                  entry={displayEntry} isDarkMode={isDarkMode}
                                  getMoodColorClass={getMoodColorClass} getMoodLabel={getMoodLabel} getChipStyle={getChipStyle}
                                  emotionChartOptions={emotionChartOptions} importGrowthTipMutation={importGrowthTipMutation} isPending={importGrowthTipMutation.isPending}
                              />
                             <div className={`flex flex-wrap justify-end gap-2 lg:gap-3 pt-4 lg:pt-5 border-t ${rowBorder} mt-4 lg:mt-5`}>
                                <button onClick={() => handleEditClick(entry)} className="px-5 py-2 lg:py-2.5 rounded-full bg-blue-50 hover:bg-blue-100 dark:bg-blue-500/10 dark:hover:bg-blue-500/20 text-blue-600 dark:text-blue-400 transition-colors flex items-center gap-1.5 text-xs lg:text-sm font-bold shadow-sm border border-blue-200/50 dark:border-blue-500/20">
                                  <Edit className="w-4 h-4" /> Edit
                                </button>
                                <button onClick={() => handleDeleteClick(entry.id)} className="px-5 py-2 lg:py-2.5 rounded-full bg-rose-50 hover:bg-rose-100 dark:bg-rose-500/10 dark:hover:bg-rose-500/20 text-rose-600 dark:text-rose-400 transition-colors flex items-center gap-1.5 text-xs lg:text-sm font-bold shadow-sm border border-rose-200/50 dark:border-rose-500/20">
                                  <Trash2 className="w-4 h-4" /> Delete
                                </button>
                              </div>
                          </>
                      )}
                  </motion.div>
              )}
            </div>
          );
  };

  if (filteredEntries.length === 0) {
    let msg = "No journal entries yet. Start writing your first reflection!";
    if (filterPhrase) msg = `No entries found containing the phrase "${filterPhrase}".`;
    return <div className="text-center py-12 lg:py-16 text-gray-500 dark:text-gray-400 font-inter text-base lg:text-lg">{msg}</div>;
  }

  return (
    <div className="font-inter w-full relative" ref={listRef}>

      {/* 💡 THE FIX: Smart Routing Engine */}
      {shouldVirtualize ? (
          // IF DATA > 20 ITEMS: Route to the High-Performance DOM Virtualizer (History Tab)
          <div style={{ height: `${virtualizer.getTotalSize()}px`, width: '100%', position: 'relative' }}>
            {virtualizer.getVirtualItems().map((virtualItem) => {
              const item = flattenedItems[virtualItem.index];
              return (
                <div key={item.id} data-index={virtualItem.index} ref={virtualizer.measureElement}
                  style={{ position: 'absolute', top: 0, left: 0, width: '100%', transform: `translateY(${virtualItem.start - virtualizer.options.scrollMargin}px)`, paddingBottom: '24px' }}
                >
                  {renderItemContent(item)}
                </div>
              );
            })}
          </div>
      ) : (
          // IF DATA < 20 ITEMS: Route to standard HTML mapping (Today & Weekly Tabs). Zero mathematical positioning bugs!
          <div className="flex flex-col space-y-6">
             {flattenedItems.map((item) => (
                 <div key={item.id}>
                     {renderItemContent(item)}
                 </div>
             ))}
          </div>
      )}

      <DeleteConfirmationModal isOpen={showDeleteConfirm} onClose={cancelDelete} onConfirm={confirmDelete} theme={theme} isDeleting={deleteMutation.isPending} />

    </div>
  );
}

export default React.memo(JournalHistory);