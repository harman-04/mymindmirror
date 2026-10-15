// src/components/ReflectionChat.jsx
import React, { useState, useRef, useEffect } from 'react';
import ReactDOM from 'react-dom';
import { useTheme } from '../contexts/ThemeContext';
import { useSuggestQuestion, useSendChatMessage, useClearChatMemory } from '../hooks/useReflectionChat';
import {
  Send, Bot, User as UserIcon, Sparkles, RefreshCw,
  Lightbulb, ArrowDown, Copy, Check, Trash2, Repeat, Plus, Brain, AlertTriangle, Loader
} from 'lucide-react';

const CACHE_KEY = 'reflection_last_question';
const CACHE_EXPIRY = 60 * 60 * 1000;

const cacheQuestion = (question) => {
  try {
    sessionStorage.setItem(CACHE_KEY, JSON.stringify({ question, timestamp: Date.now() }));
  } catch (e) {}
};

const getCachedQuestion = () => {
  try {
    const cached = sessionStorage.getItem(CACHE_KEY);
    if (cached) {
      const { question, timestamp } = JSON.parse(cached);
      if (Date.now() - timestamp < CACHE_EXPIRY) return question;
    }
  } catch (e) {}
  return null;
};

const formatMarkdown = (text) => {
  if (!text) return '';
  const escapeHtml = (str) => str.replace(/[&<>]/g, (m) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;' }[m]));

  let processed = text;
  const codeBlocks = [];

  processed = processed.replace(/```([\s\S]*?)```/g, (match, code) => {
    const idx = codeBlocks.length;
    // 🌟 FIX: Updated backgrounds and borders to Master Palette
    codeBlocks.push(`<pre class="bg-slate-50 dark:bg-black/40 p-3 lg:p-4 rounded-xl overflow-x-auto text-xs lg:text-sm my-3 border border-slate-200/80 dark:border-white/10"><code class="font-mono text-pink-600 dark:text-teal-400">${escapeHtml(code)}</code></pre>`);
    return `__CODEBLOCK_${idx}__`;
  });

  const formatInline = (str) => {
    let formatted = escapeHtml(str);
    // 🌟 FIX: Slate backgrounds for inline code
    formatted = formatted.replace(/`([^`]+)`/g, '<code class="bg-slate-100 dark:bg-black/40 px-1.5 py-0.5 rounded-md text-pink-600 dark:text-teal-400 font-mono text-[0.9em]">$1</code>');
    formatted = formatted.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-purple-600 dark:text-teal-400 hover:underline font-bold">$1</a>');
    // 🌟 FIX: Replaced gray with slate for crisp typography
    formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong class="font-bold text-slate-900 dark:text-gray-100">$1</strong>');
    formatted = formatted.replace(/\*(.*?)\*/g, '<em class="italic text-slate-700 dark:text-gray-300">$1</em>');
    return formatted;
  };

  const lines = processed.split('\n');
  const result = [];
  let i = 0;
  const total = lines.length;

  while (i < total) {
    const line = lines[i];

    const headingMatch = line.match(/^(#{1,6})\s+(.*)/);
    if (headingMatch) {
      const level = headingMatch[1].length;
      result.push(`<h${level} class="font-poppins font-bold text-slate-900 dark:text-gray-100 mt-4 mb-2 tracking-tight ${level === 1 ? 'text-lg lg:text-xl' : 'text-base lg:text-lg'}">${formatInline(headingMatch[2])}</h${level}>`);
      i++; continue;
    }

    const bulletMatch = line.match(/^\s*[\*\-]\s+(.*)/);
    if (bulletMatch) {
      const items = [];
      while (i < total && lines[i].match(/^\s*[\*\-]\s+(.*)/)) {
        items.push(`<li class="mb-1">${formatInline(lines[i].match(/^\s*[\*\-]\s+(.*)/)[1])}</li>`);
        i++;
      }
      result.push(`<ul class="list-disc list-outside ml-4 lg:ml-5 my-2 space-y-1 marker:text-purple-500 dark:marker:text-teal-500 text-slate-800 dark:text-gray-200">${items.join('')}</ul>`);
      continue;
    }

    const numberMatch = line.match(/^\s*\d+\.\s+(.*)/);
    if (numberMatch) {
      const items = [];
      while (i < total && lines[i].match(/^\s*\d+\.\s+(.*)/)) {
        items.push(`<li class="mb-1">${formatInline(lines[i].match(/^\s*\d+\.\s+(.*)/)[1])}</li>`);
        i++;
      }
      result.push(`<ol class="list-decimal list-outside ml-4 lg:ml-5 my-2 space-y-1 font-medium text-slate-800 dark:text-gray-200">${items.join('')}</ol>`);
      continue;
    }

    if (line.startsWith('> ')) {
      let quote = line.substring(2);
      let j = i + 1;
      while (j < total && lines[j].startsWith('> ')) { quote += '\n' + lines[j].substring(2); j++; }
      result.push(`<blockquote class="border-l-4 border-purple-500 dark:border-teal-500 bg-purple-50 dark:bg-teal-900/10 p-3 lg:p-4 rounded-r-xl my-3 italic text-slate-700 dark:text-gray-300 text-sm lg:text-base">${formatMarkdown(quote)}</blockquote>`);
      i = j; continue;
    }

    if (/^(\*{3,}|-{3,}|_{3,})$/.test(line.trim())) { result.push('<hr class="border-slate-200/80 dark:border-white/10 my-4 lg:my-6"/>'); i++; continue; }
    if (line.trim() === '') { result.push('<br/>'); i++; continue; }

    result.push(`<p class="mb-2 lg:mb-3 leading-relaxed text-slate-800 dark:text-gray-200 text-sm lg:text-base">${formatInline(line)}</p>`);
    i++;
  }

  let finalHtml = result.join('');
  codeBlocks.forEach((block, idx) => { finalHtml = finalHtml.replace(`__CODEBLOCK_${idx}__`, block); });
  return finalHtml;
};

// ------------------------------------------------------------------
// UI Sub-components
// ------------------------------------------------------------------
const TypingIndicator = () => (
  <div className="flex gap-1.5 items-center py-2 px-3">
    <div className="w-2 h-2 bg-purple-500 dark:bg-teal-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
    <div className="w-2 h-2 bg-purple-500 dark:bg-teal-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
    <div className="w-2 h-2 bg-purple-500 dark:bg-teal-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
  </div>
);

// 💡 FIX: Positioned beautifully inside the messages container
const ScrollToBottom = ({ onClick, visible }) => (
  visible ? (
    <button
      onClick={onClick}
      className="absolute bottom-4 right-6 lg:right-8 p-2.5 lg:p-3 rounded-full bg-purple-500/90 dark:bg-teal-600/90 backdrop-blur-sm text-white shadow-lg hover:bg-purple-600 dark:hover:bg-teal-500 transition-all hover:scale-105 z-10"
      aria-label="Scroll to bottom"
    >
      <ArrowDown className="w-4 h-4 lg:w-5 lg:h-5" />
    </button>
  ) : null
);

const ConfirmModal = ({ isOpen, onClose, onConfirm, isLoading }) => {
  const { theme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    if (isOpen) document.body.style.overflow = 'hidden';
    else document.body.style.overflow = 'unset';
    return () => { document.body.style.overflow = 'unset'; };
  }, [isOpen]);

  if (!isOpen || !mounted) return null;

  // 🌟 FIX: Stripped blur, applied Master Palette
  const bgClass = theme === 'dark' ? 'bg-[#1A162F]/95' : 'bg-white/95';
  const borderClass = theme === 'dark' ? 'border-white/10' : 'border-slate-200/80';

  return ReactDOM.createPortal(
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4" style={{ backgroundColor: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)' }}>
      <div className="absolute inset-0" onClick={!isLoading ? onClose : undefined} aria-hidden="true" />
      <div className={`relative max-w-sm w-full rounded-2xl lg:rounded-3xl ${bgClass} border ${borderClass} shadow-2xl p-6 lg:p-8 transform transition-all duration-300 animate-in fade-in zoom-in`}>
        <div className="flex flex-col items-center text-center">
          <div className="w-14 h-14 lg:w-16 lg:h-16 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center mb-4 lg:mb-6">
            <AlertTriangle className="text-red-600 dark:text-red-400 w-7 h-7 lg:w-8 lg:h-8" />
          </div>
          <h3 className="text-xl lg:text-2xl font-poppins font-bold text-gray-900 dark:text-gray-100 tracking-tight mb-2">Clear Memory?</h3>
          <p className="text-sm lg:text-base text-gray-500 dark:text-gray-400 mb-6 lg:mb-8 font-medium">
            Are you sure you want to delete this conversation and wipe the AI's memory? This cannot be undone.
          </p>
 <div className="flex w-full gap-3">
             {/* 🌟 FIX: Standardized Cancel Button to Master Palette */}
             <button onClick={onClose} disabled={isLoading} className={`flex-1 py-2.5 lg:py-3 rounded-full font-bold transition-all disabled:opacity-50 active:scale-95 ${theme === 'dark' ? 'bg-black/20 text-gray-200 hover:bg-black/40 border border-white/10' : 'bg-slate-200 text-slate-800 hover:bg-slate-300'}`}>Cancel</button>
             <button onClick={onConfirm} disabled={isLoading} className="flex-1 py-2.5 lg:py-3 rounded-full bg-red-600 text-white font-bold hover:bg-red-700 active:scale-95 transition-all flex items-center justify-center gap-2 disabled:opacity-70 shadow-md">
               {isLoading ? <Loader className="w-4 h-4 lg:w-5 lg:h-5 animate-spin" /> : <Trash2 className="w-4 h-4 lg:w-5 lg:h-5" />}
               {isLoading ? 'Clearing...' : 'Clear'}
             </button>
           </div>
        </div>
      </div>
    </div>,
    document.body
  );
};

// ------------------------------------------------------------------
// Main Component
// ------------------------------------------------------------------
function ReflectionChat() {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';

  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isInitializing, setIsInitializing] = useState(true);
  const [showScrollButton, setShowScrollButton] = useState(false);
  const [replaceMode, setReplaceMode] = useState(false);
  const [copiedMessageId, setCopiedMessageId] = useState(null);
  const [refreshInBackground, setRefreshInBackground] = useState(false);

  const [showClearModal, setShowClearModal] = useState(false);
  const [rememberChat, setRememberChat] = useState(true);
  const [sessionId] = useState(() => crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36));

  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const inputRef = useRef(null);

  const suggestQuestionMutation = useSuggestQuestion();
  const sendChatMutation = useSendChatMessage();
  const clearMemoryMutation = useClearChatMemory();

// ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Layer 1 & 2 Architecture)
  // ==========================================================================
  // Layer 1: The Chat Window Container
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';

  // Layer 2: The Chat Header & Input Area
  const sectionBg = isDarkMode ? 'bg-[#131127]/80' : 'bg-slate-50/80';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';

  // Universal
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  // 🌟 FIX: Clean Slate Buttons
  const inactivePillClass = `bg-white/60 dark:bg-black/20 ${textSecondary} border ${sectionBorder} hover:bg-slate-50/90 dark:hover:bg-black/40`;

  // Bubbles
  const userBubbleClass = 'bg-gradient-to-r from-purple-500 to-teal-500 dark:from-purple-600 dark:to-teal-600 text-white border border-transparent shadow-md';
  const assistantBubbleClass = isDarkMode ? 'bg-[#131127]/80 border border-white/5 text-gray-200 shadow-sm' : 'bg-white border border-slate-200/80 text-gray-800 shadow-sm';

  useEffect(() => {
    const loadMessages = async () => {
      setIsInitializing(true);
      const cachedQuestion = getCachedQuestion();

      if (cachedQuestion) {
        setMessages([{
          id: 'welcome', role: 'assistant',
          content: `Hi! I'm your AI reflection coach. I've read your recent journal entries.\n\nHere's a reflective question to start: **${cachedQuestion}**\n\nFeel free to answer, ask anything, or click the refresh button for another question.`,
          timestamp: new Date(),
        }]);
        setIsInitializing(false);
        setRefreshInBackground(true);
        try {
          const freshQuestion = await suggestQuestionMutation.mutateAsync();
          if (freshQuestion && freshQuestion !== cachedQuestion) {
            cacheQuestion(freshQuestion);
            setMessages([{
              id: 'welcome', role: 'assistant',
              content: `Hi! I'm your AI reflection coach. I've read your recent journal entries.\n\nHere's a reflective question to start: **${freshQuestion}**\n\nFeel free to answer, ask anything, or click the refresh button for another question.`,
              timestamp: new Date(),
            }]);
          }
        } catch (error) {
          console.error('Background refresh failed', error);
        } finally {
          setRefreshInBackground(false);
        }
      } else {
        try {
          const freshQuestion = await suggestQuestionMutation.mutateAsync();
          cacheQuestion(freshQuestion);
          setMessages([{
            id: 'welcome', role: 'assistant',
            content: `Hi! I'm your AI reflection coach. I've read your recent journal entries.\n\nHere's a reflective question to start: **${freshQuestion}**\n\nFeel free to answer, ask anything, or click the refresh button for another question.`,
            timestamp: new Date(),
          }]);
        } catch (error) {
            setMessages([{
                id: 'welcome', role: 'assistant',
                content: "I couldn't fetch a fresh question right now. You can still ask me anything about your journal entries.",
                timestamp: new Date(),
            }]);
        } finally {
          setIsInitializing(false);
        }
      }
    };
    loadMessages();
  }, []);

  const handleNewQuestion = async () => {
    setIsLoading(true);
    try {
      const question = await suggestQuestionMutation.mutateAsync();
      cacheQuestion(question);
      const newQuestionMsg = {
        id: Date.now().toString(),
        role: 'assistant',
        content: `Here's another reflective question for you:\n\n**${question}**`,
        timestamp: new Date(),
      };

      if (replaceMode) {
        setMessages((prev) => {
          const lastIndex = prev.length - 1;
          if (lastIndex >= 0 && prev[lastIndex].role === 'assistant') {
            const newMessages = [...prev];
            newMessages[lastIndex] = newQuestionMsg;
            return newMessages;
          }
          return [...prev, newQuestionMsg];
        });
      } else {
        setMessages((prev) => [...prev, newQuestionMsg]);
      }
    } catch (error) {
      console.error('Failed to fetch question', error);
    } finally {
      setIsLoading(false);
    }
  };

  const confirmClearConversation = () => {
    clearMemoryMutation.mutate(sessionId, {
      onSuccess: () => {
        setShowClearModal(false);
        setMessages([{
          id: 'welcome', role: 'assistant',
          content: "Conversation and memory cleared. Ask me anything about your journal entries!",
          timestamp: new Date(),
        }]);
      }
    });
  };

  const handleCopyMessage = (content, id) => {
    navigator.clipboard.writeText(content);
    setCopiedMessageId(id);
    setTimeout(() => setCopiedMessageId(null), 2000);
  };

  const handleSend = () => {
    if (!input.trim() || isLoading || isInitializing) return;

    const userMessage = {
      id: Date.now().toString(), role: 'user',
      content: input.trim(), timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    sendChatMutation.mutate(
      { query: userMessage.content, sessionId, rememberChat },
      {
        onSuccess: (data) => {
          const aiResponse = {
            id: Date.now().toString(), role: 'assistant',
            content: data, timestamp: new Date(),
          };

          if (replaceMode) {
            setMessages((prev) => {
              const lastIndex = prev.length - 1;
              if (lastIndex >= 0 && prev[lastIndex].role === 'assistant') {
                const newMessages = [...prev];
                newMessages[lastIndex] = aiResponse;
                return newMessages;
              }
              return [...prev, aiResponse];
            });
          } else {
            setMessages((prev) => [...prev, aiResponse]);
          }
          setIsLoading(false);
        },
        onError: () => {
          setMessages((prev) => [
            ...prev,
            { id: Date.now().toString(), role: 'assistant', content: "Sorry, I'm having trouble connecting. Please try again later.", timestamp: new Date() }
          ]);
          setIsLoading(false);
        }
      }
    );
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleInputChange = (e) => {
    setInput(e.target.value);
    const textarea = e.target;
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
  };

const scrollToBottom = () => messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });

  // 💡 THE FIX: The missing function to make the Suggestion Chips actually work!
  const handleSuggestionClick = (chipText) => {
    setInput(chipText);
    inputRef.current?.focus(); // Automatically puts the typing cursor into the box
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isInitializing]);

  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container) return;
    const handleScroll = () => {
      const { scrollTop, scrollHeight, clientHeight } = container;
      const isNearBottom = scrollHeight - scrollTop - clientHeight < 100;
      setShowScrollButton(!isNearBottom);
    };
    container.addEventListener('scroll', handleScroll);
    return () => container.removeEventListener('scroll', handleScroll);
  }, []);

  const suggestionChips = [
    "Tell me more about that", "What can I do to feel better?",
    "I'm grateful for...", "One small step I can take today is...",
    "That's interesting, why?", "How can I apply this to my life?",
  ];

  return (
      <div className={`relative rounded-2xl lg:rounded-3xl ${cardBg} border ${cardBorder} overflow-hidden flex flex-col h-[600px] lg:h-[700px] shadow-sm hover:shadow-md transition-shadow duration-300 w-full`}>

        {/* Header */}
        <div className={`shrink-0 p-4 lg:p-6 border-b ${sectionBorder} flex flex-wrap justify-between items-center gap-4 ${sectionBg}`}>
          <div className="flex items-center gap-3 lg:gap-4">
            {/* 🌟 RESTORED: Jewel Icon! */}
            <div className="w-10 h-10 lg:w-12 lg:h-12 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-500 to-teal-500 flex items-center justify-center shadow-md shrink-0 border border-purple-400/50 dark:border-teal-400/50">
              <Sparkles className="w-5 h-5 lg:w-6 lg:h-6 text-white" />
            </div>
            <div>
              <h3 className={`font-poppins font-extrabold text-lg lg:text-xl ${textPrimary} tracking-tight leading-tight`}>AI Reflection Coach</h3>
              <p className={`text-[11px] lg:text-xs font-medium ${textSecondary} mt-0.5`}>
                Based on your journal entries
                {refreshInBackground && (
                  <span className="ml-2 inline-flex items-center gap-1 text-teal-500 dark:text-teal-400">
                    <RefreshCw className="w-3 h-3 animate-spin" /> <span className="hidden sm:inline">refreshing...</span>
                  </span>
                )}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 lg:gap-3 shrink-0 w-full sm:w-auto justify-end">
            <button
              onClick={() => setRememberChat(!rememberChat)}
              className={`px-3 py-1.5 lg:px-4 lg:py-2 rounded-full transition-all flex items-center gap-1.5 text-xs lg:text-sm font-bold border shadow-sm hover:scale-105 active:scale-95 ${
                rememberChat
                  ? 'bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-teal-400 border-purple-200 dark:border-teal-500/30'
                  : inactivePillClass
              }`}
              title={rememberChat ? "AI remembers this conversation" : "AI forgets previous messages"}
            >
              <Brain className={`w-3.5 h-3.5 lg:w-4 lg:h-4 ${rememberChat ? 'animate-pulse text-purple-500 dark:text-teal-400' : 'opacity-50'}`} />
              <span className="hidden md:inline">{rememberChat ? 'Memory ON' : 'Memory OFF'}</span>
            </button>

            <button
              onClick={() => setReplaceMode(!replaceMode)}
              className={`px-3 py-1.5 lg:px-4 lg:py-2 rounded-full transition-all flex items-center gap-1.5 text-xs lg:text-sm font-bold border shadow-sm hover:scale-105 active:scale-95 ${
                  replaceMode
                      ? 'bg-gradient-to-r from-purple-500 to-teal-500 text-white border-transparent'
                      : inactivePillClass
              }`}
              title={replaceMode ? "Replace last response" : "Append new response"}
            >
              {replaceMode ? <Repeat className="w-3.5 h-3.5 lg:w-4 lg:h-4" /> : <Plus className="w-3.5 h-3.5 lg:w-4 lg:h-4" />}
              <span className="hidden md:inline">{replaceMode ? 'Replace' : 'Append'}</span>
            </button>

            <button
              onClick={() => setShowClearModal(true)}
              className={`p-1.5 lg:p-2 rounded-xl transition-all shadow-sm hover:text-red-500 dark:hover:text-red-400 hover:scale-105 active:scale-95 ${inactivePillClass}`}
              title="Clear conversation & memory"
            >
              <Trash2 className="w-4 h-4 lg:w-5 lg:h-5" />
            </button>

            <button
              onClick={handleNewQuestion}
              disabled={isLoading || isInitializing || suggestQuestionMutation.isPending}
              className={`p-1.5 lg:p-2 rounded-xl transition-all shadow-sm hover:text-purple-600 dark:hover:text-teal-400 disabled:opacity-50 hover:scale-105 active:scale-95 ${inactivePillClass}`}
              title="Generate new reflective question"
            >
              <RefreshCw className={`w-4 h-4 lg:w-5 lg:h-5 ${(isLoading || suggestQuestionMutation.isPending) ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        <div className="relative flex-1 flex flex-col min-h-0">
            <div ref={messagesContainerRef} className="flex-1 overflow-y-auto p-4 lg:p-6 space-y-5 lg:space-y-6 custom-scrollbar scroll-smooth">
              {messages.map((msg) => (
                <div key={msg.id} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'} animate-in fade-in slide-in-from-bottom-2 duration-300 group`}>
                  <div className={`flex gap-3 lg:gap-4 max-w-[90%] lg:max-w-[80%] ${msg.role === 'user' ? 'flex-row-reverse' : 'flex-row'}`}>

     <div className="flex-shrink-0 mt-auto mb-5">
                         {msg.role === 'user' ? (
                           <div className="w-8 h-8 lg:w-10 lg:h-10 rounded-full bg-purple-100 dark:bg-teal-900/30 border border-purple-200 dark:border-teal-500/30 flex items-center justify-center shadow-sm">
                             <UserIcon className="w-4 h-4 lg:w-5 lg:h-5 text-purple-600 dark:text-teal-400" />
                           </div>
                         ) : (
                           <div className="w-8 h-8 lg:w-10 lg:h-10 rounded-full bg-gradient-to-br from-purple-500 to-teal-500 flex items-center justify-center shadow-md border border-purple-400/50 dark:border-teal-400/50">
                             <Sparkles className="w-4 h-4 lg:w-5 lg:h-5 text-white" />
                           </div>
                         )}
                       </div>

                    <div className="relative">
                      <div className={`px-5 py-3 lg:px-6 lg:py-4 shadow-sm ${msg.role === 'user' ? `${userBubbleClass} rounded-2xl rounded-br-sm` : `${assistantBubbleClass} rounded-2xl rounded-bl-sm`}`}>
                        {msg.role === 'assistant' ? (
                          <div className="chat-content" dangerouslySetInnerHTML={{ __html: formatMarkdown(msg.content) }} />
                        ) : (
                          <p className="text-sm lg:text-base font-medium whitespace-pre-wrap">{msg.content}</p>
                        )}
                      </div>
                      <div className={`flex items-center mt-1.5 px-1 gap-2 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                        <p className={`text-[10px] lg:text-xs font-bold uppercase tracking-wider ${textSecondary}`}>
                          {msg.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </p>
                        <button
                          onClick={() => handleCopyMessage(msg.content, msg.id)}
                          className={`opacity-0 group-hover:opacity-100 transition-opacity p-1 rounded-md hover:bg-slate-200 dark:hover:bg-white/10`}
                          title="Copy message"
                        >
                          {copiedMessageId === msg.id ? <Check className="w-3 h-3 lg:w-4 lg:h-4 text-emerald-500" /> : <Copy className={`w-3 h-3 lg:w-4 lg:h-4 ${textSecondary}`} />}
                        </button>
                      </div>
                    </div>

                  </div>
                </div>
              ))}

              {isInitializing && messages.length === 0 && (
                <div className="flex justify-start animate-in fade-in duration-300">
                  <div className="flex gap-3 lg:gap-4 max-w-[80%]">
                    <div className="flex-shrink-0 mt-auto mb-5">
                        <div className="w-8 h-8 lg:w-10 lg:h-10 rounded-full bg-gradient-to-br from-purple-500 to-teal-500 flex items-center justify-center shadow-md border border-purple-400/50 dark:border-teal-400/50">
                            <Sparkles className="w-4 h-4 lg:w-5 lg:h-5 text-white" />
                        </div>
                    </div>
                    <div className={`px-5 py-3 lg:px-6 lg:py-4 rounded-2xl rounded-bl-sm ${assistantBubbleClass} flex items-center gap-3`}>
                      <TypingIndicator />
                      <span className={`text-xs lg:text-sm font-bold uppercase tracking-wider ${textSecondary}`}>Reviewing your journal...</span>
                    </div>
                  </div>
                </div>
              )}

              {isLoading && sendChatMutation.isPending && (
                <div className="flex justify-start animate-in fade-in duration-300">
                  <div className="flex gap-3 lg:gap-4 max-w-[80%]">
                     <div className="flex-shrink-0 mt-auto mb-5">
                        <div className="w-8 h-8 lg:w-10 lg:h-10 rounded-full bg-gradient-to-br from-purple-500 to-teal-500 flex items-center justify-center shadow-md border border-purple-400/50 dark:border-teal-400/50">
                            <Sparkles className="w-4 h-4 lg:w-5 lg:h-5 text-white" />
                        </div>
                    </div>
                    <div className={`px-5 py-3 lg:px-6 lg:py-4 rounded-2xl rounded-bl-sm ${assistantBubbleClass}`}>
                        <TypingIndicator />
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} className="h-4 shrink-0" />
            </div>

            <ScrollToBottom onClick={scrollToBottom} visible={showScrollButton} />
        </div>

   {/* Suggestion Chips */}
         {messages.length > 0 && !isLoading && !isInitializing && (
           <div className="shrink-0 px-4 lg:px-6 mb-3 lg:mb-4 flex gap-2 overflow-x-auto custom-scrollbar">
             {suggestionChips.map((chip, idx) => (
               <button key={idx} onClick={() => handleSuggestionClick(chip)} className="flex-shrink-0 px-3 lg:px-4 py-1.5 lg:py-2 text-xs lg:text-sm font-bold rounded-full bg-purple-50 dark:bg-purple-900/30 text-purple-700 dark:text-teal-400 border border-purple-200 dark:border-teal-500/30 hover:bg-purple-100 dark:hover:bg-teal-900/50 transition-colors shadow-sm hover:shadow-md hover:-translate-y-0.5 active:scale-95 flex items-center gap-1.5">
                 <Lightbulb className="w-3 h-3 lg:w-4 lg:h-4" />{chip}
               </button>
             ))}
           </div>
         )}

         {/* Input Area */}
         <div className={`shrink-0 p-4 lg:p-6 border-t ${sectionBorder} ${sectionBg}`}>
           <div className="flex gap-2 lg:gap-3 items-end relative">
             <textarea
               ref={inputRef} value={input} onChange={handleInputChange} onKeyDown={handleKeyPress}
               disabled={isInitializing}
               placeholder={isInitializing ? "AI is reading your journal..." : "Answer the question or share your thoughts..."}
               rows={1}
               className={`flex-1 p-3 lg:p-4 rounded-xl lg:rounded-2xl border ${isDarkMode ? 'bg-[#131127]/80 border-white/10 focus:border-teal-400 focus:ring-teal-400 text-gray-100 placeholder-gray-500' : 'bg-white border-slate-300 focus:border-purple-500 focus:ring-purple-500 text-slate-800 placeholder-slate-400'} resize-none focus:outline-none focus:ring-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-inner text-sm lg:text-base custom-scrollbar`}
               style={{ minHeight: '52px', maxHeight: '150px' }}
             />
             <button
               onClick={handleSend} disabled={!input.trim() || isLoading || isInitializing}
               className="p-3 lg:p-4 rounded-xl lg:rounded-2xl bg-gradient-to-r from-purple-500 to-teal-500 text-white font-bold shadow-md hover:shadow-lg active:scale-95 transition-all disabled:opacity-50 disabled:hover:shadow-none shrink-0"
             >
               <Send className="w-5 h-5 lg:w-6 lg:h-6" />
             </button>
           </div>
           <p className={`text-[10px] lg:text-xs font-bold uppercase tracking-wider ${textSecondary} mt-2 lg:mt-3 text-center`}>
             Press Enter to send, Shift+Enter for new line
           </p>
         </div>

         <ConfirmModal
           isOpen={showClearModal}
           onClose={() => setShowClearModal(false)}
           onConfirm={confirmClearConversation}
           isLoading={clearMemoryMutation.isPending}
         />
       </div>
     );
   }

   export default ReflectionChat;