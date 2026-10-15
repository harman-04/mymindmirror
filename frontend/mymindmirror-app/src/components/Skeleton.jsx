// src/components/Skeleton.jsx
import React from 'react';
import { Brain, Heart, Lightbulb } from 'lucide-react';

// Common wrapper for smooth pulsating
const PulseWrapper = ({ children, className = "" }) => (
  <div className={`animate-pulse ${className}`}>
    {children}
  </div>
);

export const SkeletonText = ({ lines = 1, className = '' }) => (
  <PulseWrapper className={`space-y-3 ${className}`}>
    {Array(lines).fill(0).map((_, i) => (
      <div
        key={i}
        className="h-4 bg-slate-200/80 dark:bg-white/5 rounded-full"
        style={{ width: `${Math.max(60, 100 - (i * 15))}%` }}
      />
    ))}
  </PulseWrapper>
);

export const SkeletonCard = ({ count = 1 }) => (
  <PulseWrapper className="space-y-4 lg:space-y-6">
    {Array(count).fill(0).map((_, i) => (
      <div
        key={i}
        className="p-5 lg:p-6 rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/80 border border-slate-200/80 dark:border-white/10 shadow-sm"
      >
        <div className="flex justify-between items-center mb-4 lg:mb-5">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 lg:h-12 lg:w-12 rounded-full bg-slate-200/80 dark:bg-white/5" />
            <div className="h-5 lg:h-6 w-32 lg:w-48 bg-slate-200/80 dark:bg-white/5 rounded-full" />
          </div>
          <div className="h-6 w-20 lg:w-24 bg-slate-200/80 dark:bg-white/5 rounded-full" />
        </div>
        <div className="space-y-3">
          <div className="h-4 w-full bg-slate-200/80 dark:bg-white/5 rounded-full" />
          <div className="h-4 w-5/6 bg-slate-200/80 dark:bg-white/5 rounded-full" />
        </div>
      </div>
    ))}
  </PulseWrapper>
);

// ------------------------------------------------------------------
// 🌟 BESPOKE CHART SKELETONS (The Enterprise Aesthetic)
// ------------------------------------------------------------------

const ChartBase = ({ children }) => (
    <PulseWrapper className="h-[250px] sm:h-[300px] lg:h-[350px] w-full rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/80 border border-slate-200/80 dark:border-white/10 flex flex-col shadow-sm overflow-hidden">
        {/* 🌟 Ghost Header */}
        <div className="flex justify-between items-center p-4 lg:p-6 border-b border-slate-200/60 dark:border-gray-700/50 bg-slate-50/50 dark:bg-black/10 shrink-0 w-full">
            <div className="flex items-center gap-3">
                <div className="w-5 h-5 lg:w-6 lg:h-6 rounded-md bg-slate-300/80 dark:bg-white/10 shrink-0"></div>
                <div className="flex flex-col gap-2">
                    <div className="h-4 lg:h-5 w-32 sm:w-48 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
                    <div className="h-2 lg:h-2.5 w-48 sm:w-64 bg-slate-200 dark:bg-white/5 rounded-full hidden sm:block"></div>
                </div>
            </div>
            <div className="w-8 h-8 rounded-full bg-slate-200 dark:bg-white/10 shrink-0"></div>
        </div>
        {/* Chart Body */}
        <div className="flex-grow relative w-full">
            {children}
        </div>
    </PulseWrapper>
);

// 📊 1. BAR CHART
export const SkeletonBarChart = () => (
  <ChartBase>
     <div className="absolute inset-0 px-6 lg:px-10 pt-10 pb-6 flex justify-between items-end opacity-30 dark:opacity-20 gap-2 sm:gap-4">
        {[40, 70, 30, 90, 50, 80, 60, 45].map((h, i) => (
            <div key={i} className={`flex-1 bg-slate-400 dark:bg-white rounded-t-md ${i > 5 ? 'hidden sm:block' : ''}`} style={{ height: `${h}%` }}></div>
        ))}
     </div>
     <div className="absolute bottom-6 left-6 right-6 lg:left-10 lg:right-10 h-1 bg-slate-200 dark:bg-white/10 rounded-full"></div>
  </ChartBase>
);

// 🍩 2. DOUGHNUT CHART
export const SkeletonDoughnutChart = () => (
  <ChartBase>
     <div className="absolute inset-0 flex items-center justify-center pb-4">
        <div className="w-36 h-36 sm:w-48 sm:h-48 rounded-full border-[16px] sm:border-[24px] border-slate-300/40 dark:border-white/10"></div>
     </div>
  </ChartBase>
);

// 📈 3. LINE / WAVE CHART
export const SkeletonLineChart = () => (
  <ChartBase>
      <div className="absolute inset-0 flex items-center justify-center opacity-30 dark:opacity-20 pointer-events-none pb-6 pt-4">
          <svg className="w-[90%] h-[80%]" preserveAspectRatio="none" viewBox="0 0 100 100">
              <path d="M0,80 Q15,20 30,60 T60,40 T90,70 T100,50 L100,100 L0,100 Z" fill="currentColor" className="text-slate-200 dark:text-white/5" />
              <path d="M0,80 Q15,20 30,60 T60,40 T90,70 T100,50" fill="none" stroke="currentColor" strokeWidth="2" className="text-slate-400 dark:text-white/20" />
          </svg>
      </div>
      <div className="absolute bottom-6 left-6 right-6 lg:left-10 lg:right-10 h-1 bg-slate-200 dark:bg-white/10 rounded-full"></div>
  </ChartBase>
);

// 🕸️ 4. RADAR CHART
export const SkeletonRadarChart = () => (
  <ChartBase>
      <div className="absolute inset-0 flex items-center justify-center opacity-40 dark:opacity-20 pb-4">
          <svg className="w-40 h-40 sm:w-56 sm:h-56" viewBox="0 0 100 100">
              <polygon points="50,5 95,27 95,73 50,95 5,73 5,27" fill="none" stroke="currentColor" strokeWidth="1" className="text-slate-400 dark:text-white/20" />
              <polygon points="50,20 80,35 80,65 50,80 20,65 20,35" fill="none" stroke="currentColor" strokeWidth="1" className="text-slate-400 dark:text-white/20" />
              <polygon points="50,35 65,45 65,55 50,65 35,55 35,45" fill="none" stroke="currentColor" strokeWidth="1" className="text-slate-400 dark:text-white/20" />
              <line x1="50" y1="50" x2="50" y2="5" stroke="currentColor" strokeWidth="1" className="text-slate-300 dark:text-white/10" />
              <line x1="50" y1="50" x2="95" y2="27" stroke="currentColor" strokeWidth="1" className="text-slate-300 dark:text-white/10" />
              <line x1="50" y1="50" x2="95" y2="73" stroke="currentColor" strokeWidth="1" className="text-slate-300 dark:text-white/10" />
              <line x1="50" y1="50" x2="50" y2="95" stroke="currentColor" strokeWidth="1" className="text-slate-300 dark:text-white/10" />
              <line x1="50" y1="50" x2="5" y2="73" stroke="currentColor" strokeWidth="1" className="text-slate-300 dark:text-white/10" />
              <line x1="50" y1="50" x2="5" y2="27" stroke="currentColor" strokeWidth="1" className="text-slate-300 dark:text-white/10" />
          </svg>
      </div>
  </ChartBase>
);

// 🌡️ 5. HEATMAP
export const SkeletonHeatmap = () => (
  <ChartBase>
      <div className="absolute inset-0 flex items-center justify-center p-6 lg:p-10">
          <div className="flex flex-wrap gap-1 sm:gap-1.5 opacity-50 dark:opacity-30 justify-center overflow-hidden w-[90%]">
              {Array(150).fill(0).map((_, i) => (
                  <div key={i} className="w-2.5 h-2.5 sm:w-3 sm:h-3 rounded-[2px] bg-slate-300 dark:bg-white/20"></div>
              ))}
          </div>
      </div>
  </ChartBase>
);

// ✨ 6. SCATTER PLOT
export const SkeletonScatterChart = () => (
  <ChartBase>
      <div className="absolute inset-0 px-6 lg:px-10 py-10 opacity-40 dark:opacity-30">
          <div className="absolute top-1/2 left-6 right-6 lg:left-10 lg:right-10 h-0.5 bg-slate-400 dark:bg-white/30 border-dashed border-t-2"></div>
          <div className="absolute top-[30%] left-[20%] w-3 h-3 rounded-full bg-slate-400 dark:bg-white/40"></div>
          <div className="absolute top-[60%] left-[35%] w-3 h-3 rounded-full bg-slate-400 dark:bg-white/40"></div>
          <div className="absolute top-[25%] left-[50%] w-3 h-3 rounded-full bg-slate-400 dark:bg-white/40"></div>
          <div className="absolute top-[75%] left-[65%] w-3 h-3 rounded-full bg-slate-400 dark:bg-white/40"></div>
          <div className="absolute top-[40%] left-[80%] w-3 h-3 rounded-full bg-slate-400 dark:bg-white/40"></div>
      </div>
      <div className="absolute bottom-6 left-6 right-6 lg:left-10 lg:right-10 h-1 bg-slate-200 dark:bg-white/10 rounded-full"></div>
  </ChartBase>
);

// 📝 7. TEXT / QUOTE SKELETON
export const SkeletonReflection = () => (
  <PulseWrapper className="h-full w-full rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/80 border border-slate-200/80 dark:border-white/10 p-5 sm:p-6 lg:p-8 flex flex-col shadow-sm min-h-[250px] lg:min-h-[300px]">
     <div className="flex justify-between items-center mb-6 lg:mb-10">
        <div className="flex items-center gap-3 w-full">
           <div className="w-5 h-5 lg:w-6 lg:h-6 rounded-md bg-slate-300 dark:bg-white/10 shrink-0"></div>
           <div className="h-6 lg:h-8 w-40 lg:w-56 bg-slate-300 dark:bg-white/10 rounded-full"></div>
        </div>
        <div className="w-8 h-8 lg:w-10 lg:h-10 rounded-full bg-slate-300 dark:bg-white/10 shrink-0"></div>
     </div>
     <div className="flex-grow flex flex-col justify-center space-y-3 lg:space-y-4 px-2 lg:px-6">
        <div className="h-4 lg:h-5 bg-slate-200 dark:bg-white/5 rounded-full w-full"></div>
        <div className="h-4 lg:h-5 bg-slate-200 dark:bg-white/5 rounded-full w-[90%]"></div>
        <div className="h-4 lg:h-5 bg-slate-200 dark:bg-white/5 rounded-full w-[75%]"></div>
        <div className="h-4 lg:h-5 bg-slate-200 dark:bg-white/5 rounded-full w-[40%]"></div>
     </div>
  </PulseWrapper>
);

// ☁️ 8. WORD CLOUD SKELETON
export const SkeletonCloud = () => (
  <ChartBase>
      <div className="absolute inset-0 flex items-center justify-center p-6 lg:p-10 opacity-60 dark:opacity-40">
          <div className="flex flex-wrap justify-center items-center gap-3 sm:gap-4 lg:gap-6 w-full lg:w-[90%]">
              {Array(15).fill(0).map((_, i) => (
                  <div
                      key={i}
                      className="bg-slate-300 dark:bg-white/20 rounded-full"
                      style={{
                          width: `${Math.floor(Math.random() * 60 + 60)}px`,
                          height: `${Math.floor(Math.random() * 15 + 20)}px`,
                          opacity: 1 - (i * 0.04),
                      }}
                  />
              ))}
          </div>
      </div>
  </ChartBase>
);

// 🚨 9. ANOMALY ALERTS SKELETON
export const SkeletonAnomalyList = () => (
  <PulseWrapper className="w-full rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/80 border border-slate-200/80 dark:border-white/10 p-4 sm:p-6 lg:p-8 flex flex-col shadow-sm">
     <div className="flex justify-between items-center mb-5 lg:mb-6 gap-4">
        <div className="flex items-center gap-3 lg:gap-4">
           <div className="w-10 h-10 lg:w-12 lg:h-12 rounded-full bg-slate-300/80 dark:bg-white/10 shrink-0"></div>
           <div className="flex flex-col gap-2">
              <div className="h-5 lg:h-6 w-40 sm:w-56 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
              <div className="h-3 lg:h-4 w-32 sm:w-48 bg-slate-200 dark:bg-white/5 rounded-full"></div>
           </div>
        </div>
        <div className="w-24 lg:w-28 h-8 lg:h-9 rounded-full bg-slate-300/80 dark:bg-white/10 shrink-0 hidden sm:block"></div>
     </div>

     <div className="space-y-3 lg:space-y-4">
        {[1, 2, 3].map((_, i) => (
          <div key={i} className="relative p-3.5 sm:p-4 lg:p-5 rounded-xl lg:rounded-2xl border border-slate-200/50 dark:border-white/5 bg-slate-50/50 dark:bg-black/10 flex items-start gap-3 sm:gap-4 lg:gap-5">
             <div className="mt-1 shrink-0 w-8 h-8 lg:w-9 lg:h-9 rounded-full bg-slate-300/80 dark:bg-white/10"></div>
             <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-2 lg:mb-3">
                   <div className="w-20 lg:w-24 h-5 lg:h-6 rounded-md bg-slate-300/80 dark:bg-white/10"></div>
                   <div className="w-16 lg:w-20 h-5 lg:h-6 rounded-md bg-slate-300/80 dark:bg-white/10"></div>
                </div>
                <div className="h-4 lg:h-5 w-full sm:w-[90%] bg-slate-200 dark:bg-white/5 rounded-full mb-3 lg:mb-4"></div>
                <div className="w-12 lg:w-16 h-5 lg:h-6 rounded-full bg-slate-300/80 dark:bg-white/10"></div>
             </div>
             <div className="shrink-0 w-7 h-7 lg:w-8 lg:h-8 rounded-full bg-slate-200 dark:bg-white/5"></div>
          </div>
        ))}
     </div>
     <div className="flex justify-center mt-5 lg:mt-6 pt-4 lg:pt-5 border-t border-slate-200/50 dark:border-gray-700/50">
        <div className="w-32 lg:w-40 h-8 lg:h-10 rounded-full bg-slate-300/80 dark:bg-white/10"></div>
     </div>
  </PulseWrapper>
);

// 📥 10. EXPORT BUTTONS SKELETON
export const SkeletonExportButtons = () => (
  <PulseWrapper className="flex items-center gap-2 lg:gap-3">
      <div className="hidden lg:flex items-center gap-2 px-2 mr-1">
          <div className="w-2 h-2 rounded-full bg-slate-300 dark:bg-white/10" />
          <div className="w-12 h-3 rounded-full bg-slate-200 dark:bg-white/5" />
      </div>
      <div className="w-[80px] lg:w-[100px] h-[36px] lg:h-[44px] rounded-xl bg-slate-200/80 dark:bg-white/5" />
      <div className="w-[80px] lg:w-[100px] h-[36px] lg:h-[44px] rounded-xl bg-slate-200/80 dark:bg-white/5" />
  </PulseWrapper>
);

// 🎯 11. MILESTONE TRACKER SKELETON
export const SkeletonMilestoneTracker = () => (
  <PulseWrapper className="space-y-6 lg:space-y-8 w-full">
     <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 lg:gap-6 bg-white/95 dark:bg-[#1A162F]/80 p-6 lg:p-8 rounded-2xl lg:rounded-3xl border border-slate-200/80 dark:border-white/10 shadow-sm">
        <div className="space-y-2">
            <div className="h-8 lg:h-10 w-48 sm:w-64 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
            <div className="h-4 w-40 bg-slate-200 dark:bg-white/5 rounded-full"></div>
        </div>
        <div className="h-12 w-full sm:w-48 bg-slate-300/80 dark:bg-white/10 rounded-full shrink-0"></div>
     </div>

     <div className="space-y-6 lg:space-y-8">
        {[1, 2, 3].map((_, i) => (
          <div key={i} className="rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/80 border border-slate-200/80 dark:border-white/10 p-5 sm:p-6 lg:p-8 flex flex-col sm:flex-row justify-between gap-5 lg:gap-6 shadow-sm">
             <div className="flex-1 space-y-3">
                <div className="flex gap-2 mb-1">
                   <div className="w-16 h-5 rounded-full bg-slate-300/80 dark:bg-white/10"></div>
                   <div className="w-20 h-5 rounded-full bg-slate-200 dark:bg-white/5 hidden sm:block"></div>
                </div>
                <div className="h-6 lg:h-8 w-3/4 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
                <div className="space-y-2 pt-1">
                   <div className="h-4 w-full bg-slate-200 dark:bg-white/5 rounded-full"></div>
                   <div className="h-4 w-5/6 bg-slate-200 dark:bg-white/5 rounded-full hidden sm:block"></div>
                </div>
                <div className="pt-2">
                   <div className="w-24 h-4 rounded-full bg-slate-200 dark:bg-white/5"></div>
                </div>
             </div>
             <div className="flex flex-row sm:flex-col items-center sm:items-end justify-between sm:justify-start gap-4 shrink-0 border-t sm:border-t-0 border-slate-200/60 dark:border-white/5 pt-5 sm:pt-0 mt-2 sm:mt-0">
                <div className="w-14 h-14 lg:w-20 lg:h-20 rounded-full border-4 sm:border-8 border-slate-300/40 dark:border-white/10"></div>
                <div className="flex gap-2">
                   <div className="w-9 h-9 lg:w-11 lg:h-11 rounded-xl bg-slate-200 dark:bg-white/5 hidden sm:block"></div>
                   <div className="w-9 h-9 lg:w-11 lg:h-11 rounded-xl bg-slate-200 dark:bg-white/5 hidden sm:block"></div>
                   <div className="w-9 h-9 lg:w-11 lg:h-11 rounded-xl bg-slate-200 dark:bg-white/5"></div>
                   <div className="w-9 h-9 lg:w-11 lg:h-11 rounded-xl bg-slate-300/80 dark:bg-white/10"></div>
                </div>
             </div>
          </div>
        ))}
     </div>
  </PulseWrapper>
);

// 🗺️ 12. ROADMAP PLANNER SKELETON
export const SkeletonRoadmap = () => (
  <PulseWrapper className="space-y-8 lg:space-y-10 w-full">
    <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 lg:gap-6 bg-white/95 dark:bg-[#1A162F]/80 p-6 lg:p-8 rounded-2xl lg:rounded-3xl border border-slate-200/80 dark:border-white/10 shadow-sm">
       <div className="space-y-3 w-full sm:w-auto">
          <div className="h-8 lg:h-10 w-64 sm:w-80 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
          <div className="h-4 w-48 bg-slate-200 dark:bg-white/5 rounded-full"></div>
       </div>
       <div className="h-12 w-full sm:w-48 bg-slate-300/80 dark:bg-white/10 rounded-full shrink-0"></div>
    </div>

    <div className="grid grid-cols-1 gap-6 lg:gap-8">
       {[1, 2].map((_, i) => (
         <div key={i} className="relative rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/80 border border-slate-200/80 dark:border-white/10 overflow-hidden shadow-sm">
            <div className="absolute top-0 left-0 right-0 h-1.5 lg:h-2 bg-slate-300/80 dark:bg-white/10"></div>
            <div className="p-5 sm:p-6 lg:p-8 pt-10 sm:pt-8 lg:pt-10">
               <div className="mb-6 lg:mb-8 pr-24 sm:pr-28 space-y-4">
                  <div className="h-8 lg:h-10 w-3/4 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
                  <div className="flex gap-3">
                     <div className="w-24 h-4 bg-slate-200 dark:bg-white/5 rounded-full"></div>
                     <div className="w-24 h-4 bg-slate-200 dark:bg-white/5 rounded-full"></div>
                  </div>
                  <div className="w-28 h-6 bg-slate-300/80 dark:bg-white/10 rounded-full mt-2"></div>
               </div>

               <div className="mb-8 lg:mb-10 p-5 lg:p-6 rounded-2xl bg-slate-50/80 dark:bg-[#131127]/50 border border-slate-200/60 dark:border-white/5 shadow-inner space-y-4">
                  <div className="flex justify-between items-end">
                     <div className="w-32 h-5 bg-slate-200 dark:bg-white/5 rounded-full"></div>
                     <div className="w-12 h-6 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
                  </div>
                  <div className="w-full h-3 lg:h-4 bg-slate-200 dark:bg-white/5 rounded-full"></div>
                  <div className="flex justify-between">
                     <div className="w-32 h-4 bg-slate-200 dark:bg-white/5 rounded-full hidden sm:block"></div>
                     <div className="w-32 h-4 bg-slate-200 dark:bg-white/5 rounded-full hidden sm:block"></div>
                  </div>
               </div>

               <div className="space-y-4">
                  <div className="w-40 h-6 bg-slate-300/80 dark:bg-white/10 rounded-full mb-6"></div>
                  <div className="h-16 w-full bg-slate-50/80 dark:bg-[#131127]/30 border border-slate-200/60 dark:border-white/5 rounded-xl"></div>
                  <div className="h-16 w-full bg-slate-50/80 dark:bg-[#131127]/30 border border-slate-200/60 dark:border-white/5 rounded-xl"></div>
               </div>
            </div>
         </div>
       ))}
    </div>
  </PulseWrapper>
);

// 📅 13. SCHEDULE PAGE SKELETON
export const SkeletonSchedule = () => (
   <PulseWrapper className="w-full space-y-6 sm:space-y-8 pb-10 mt-4">

     {/* Ghost Header - 🌟 FIX: Synced to Layer 1 (dark:bg-[#1A162F]/95) */}
     <div className="rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/95 border border-slate-200/80 dark:border-white/10 p-6 lg:p-8 shadow-sm">
       <div className="flex flex-col xl:flex-row xl:items-center xl:justify-between gap-6">
         <div className="space-y-3">
           <div className="h-8 lg:h-10 w-48 sm:w-64 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
           <div className="h-4 w-40 bg-slate-200 dark:bg-white/5 rounded-full"></div>
         </div>
         <div className="flex flex-col sm:flex-row flex-wrap gap-3 lg:gap-4 w-full xl:w-auto">
           <div className="h-10 lg:h-12 w-full sm:w-32 rounded-full bg-slate-300/80 dark:bg-white/10"></div>
           <div className="h-10 lg:h-12 w-full sm:w-36 rounded-full bg-slate-300/80 dark:bg-white/10"></div>
           <div className="h-10 lg:h-12 w-full sm:w-40 rounded-full bg-slate-200 dark:bg-white/5"></div>
           <div className="h-10 lg:h-12 w-full sm:w-32 rounded-full bg-slate-200 dark:bg-white/5"></div>
         </div>
       </div>
     </div>

     {/* Ghost Filters - 🌟 FIX: Synced to Layer 1 */}
     <div className="flex flex-col xl:flex-row xl:items-center justify-between gap-4 p-3 sm:p-4 lg:p-5 rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/95 border border-slate-200/80 dark:border-white/10 shadow-sm">
       <div className="flex gap-2 w-full sm:w-auto overflow-hidden">
          <div className="h-8 lg:h-9 w-20 rounded-xl bg-slate-300/80 dark:bg-white/10"></div>
          <div className="h-8 lg:h-9 w-16 rounded-xl bg-slate-200 dark:bg-white/5"></div>
          <div className="h-8 lg:h-9 w-16 rounded-xl bg-slate-200 dark:bg-white/5"></div>
          <div className="h-8 lg:h-9 w-16 rounded-xl bg-slate-200 dark:bg-white/5"></div>
       </div>
       <div className="flex gap-2 w-full xl:w-auto">
          <div className="h-8 lg:h-9 w-16 lg:w-20 rounded-xl bg-slate-300/80 dark:bg-white/10"></div>
          <div className="h-8 lg:h-9 w-20 lg:w-24 rounded-xl bg-slate-200 dark:bg-white/5"></div>
          <div className="h-8 lg:h-9 w-20 lg:w-24 rounded-xl bg-slate-200 dark:bg-white/5"></div>
       </div>
     </div>

     {/* Ghost Calendar Grid - 🌟 FIX: Synced to Layer 1 */}
     <div className="h-[600px] md:h-[700px] rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/95 border border-slate-200/80 dark:border-white/10 shadow-sm flex flex-col overflow-hidden">
        <div className="flex border-b border-slate-200/60 dark:border-white/5 h-12 bg-slate-50/80 dark:bg-[#131127]/50">
           <div className="w-16 border-r border-slate-200/60 dark:border-white/5 shrink-0"></div>
           {[1,2,3,4,5,6,7].map(i => (
              <div key={i} className="flex-1 border-r border-slate-200/60 dark:border-white/5 flex items-center justify-center">
                  <div className="w-12 h-3 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
              </div>
           ))}
        </div>
        <div className="flex-1 flex relative">
           <div className="w-16 border-r border-slate-200/60 dark:border-white/5 shrink-0"></div>
           <div className="flex-1 relative">
              <div className="absolute top-[20%] left-[10%] w-[12%] h-[15%] bg-slate-300/60 dark:bg-white/10 rounded-lg"></div>
              <div className="absolute top-[40%] left-[40%] w-[12%] h-[10%] bg-slate-300/60 dark:bg-white/10 rounded-lg"></div>
              <div className="absolute top-[60%] left-[70%] w-[12%] h-[20%] bg-slate-300/60 dark:bg-white/10 rounded-lg"></div>
           </div>
        </div>
     </div>
   </PulseWrapper>
 );
// 🏆 14. ACHIEVEMENTS PAGE SKELETON
export const SkeletonAchievements = () => (
  <PulseWrapper className="relative w-full max-w-7xl mx-auto flex-grow flex flex-col space-y-8 lg:space-y-12 z-10 pb-10 pt-4 lg:pt-6">
    <div className="flex flex-col items-center justify-center">
        <div className="w-14 h-14 lg:w-16 lg:h-16 rounded-2xl lg:rounded-3xl bg-slate-300/80 dark:bg-white/10 mb-4 lg:mb-6"></div>
        <div className="h-8 lg:h-10 w-64 lg:w-96 bg-slate-300/80 dark:bg-white/10 rounded-full mb-3"></div>
        <div className="h-4 lg:h-5 w-48 lg:w-64 bg-slate-200 dark:bg-white/5 rounded-full"></div>
    </div>

    <div className="rounded-3xl lg:rounded-[2rem] bg-white/95 dark:bg-[#1A162F]/80 border border-slate-200/80 dark:border-white/10 shadow-sm p-8 sm:p-10 lg:p-12 flex flex-col md:flex-row items-center gap-8 lg:gap-12">
        <div className="w-32 h-32 lg:w-40 lg:h-40 rounded-full border-4 border-slate-200 dark:border-white/5 bg-slate-300/80 dark:bg-white/10 shrink-0"></div>
        <div className="flex-1 w-full space-y-4">
            <div className="h-6 lg:h-8 w-48 lg:w-64 bg-slate-300/80 dark:bg-white/10 rounded-full mx-auto md:mx-0"></div>
            <div className="h-4 lg:h-5 w-full max-w-xl bg-slate-200 dark:bg-white/5 rounded-full mx-auto md:mx-0"></div>
            <div className="h-4 lg:h-5 w-3/4 max-w-md bg-slate-200 dark:bg-white/5 rounded-full mx-auto md:mx-0 mb-6"></div>
            <div className="flex justify-between items-end mb-2.5">
                <div className="h-4 w-20 bg-slate-200 dark:bg-white/5 rounded-full"></div>
                <div className="h-3 w-32 bg-slate-200 dark:bg-white/5 rounded-full"></div>
            </div>
            <div className="w-full bg-slate-200 dark:bg-white/5 rounded-full h-3 lg:h-4"></div>
        </div>
    </div>

    <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-6">
        {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="p-4 sm:p-5 lg:p-6 rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/80 border border-slate-200/80 dark:border-white/10 shadow-sm flex flex-col sm:flex-row items-center sm:items-start lg:items-center text-center sm:text-left gap-3 sm:gap-4 lg:gap-5">
                <div className="w-12 h-12 lg:w-14 lg:h-14 rounded-xl lg:rounded-2xl bg-slate-300/80 dark:bg-white/10 shrink-0"></div>
                <div className="space-y-2">
                    <div className="h-3 w-20 sm:w-24 bg-slate-200 dark:bg-white/5 rounded-full mx-auto sm:mx-0"></div>
                    <div className="h-6 lg:h-8 w-16 sm:w-20 bg-slate-300/80 dark:bg-white/10 rounded-full mx-auto sm:mx-0"></div>
                </div>
            </div>
        ))}
    </div>

    <div className="rounded-3xl lg:rounded-[2rem] bg-white/95 dark:bg-[#1A162F]/80 border border-slate-200/80 dark:border-white/10 shadow-sm p-6 sm:p-8 lg:p-10">
        <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-8 lg:mb-10 gap-6 border-b border-slate-200/60 dark:border-white/5 pb-6 lg:pb-8">
            <div className="space-y-3">
                <div className="h-8 lg:h-10 w-48 sm:w-64 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
                <div className="h-4 w-64 sm:w-80 bg-slate-200 dark:bg-white/5 rounded-full"></div>
            </div>
            <div className="w-full md:w-72 h-16 rounded-2xl bg-slate-200 dark:bg-white/5 shrink-0"></div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 lg:gap-6">
            {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
                <div key={i} className="p-5 lg:p-6 rounded-2xl lg:rounded-3xl bg-slate-50/50 dark:bg-[#131127]/60 border border-slate-200/60 dark:border-white/5">
                    <div className="flex justify-between items-start mb-4 lg:mb-5">
                        <div className="w-12 h-12 lg:w-14 lg:h-14 rounded-xl lg:rounded-2xl bg-slate-300/80 dark:bg-white/10"></div>
                        <div className="w-16 h-4 rounded-full bg-slate-200 dark:bg-white/5"></div>
                    </div>
                    <div className="space-y-3">
                        <div className="h-5 lg:h-6 w-3/4 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
                        <div className="h-3 w-full bg-slate-200 dark:bg-white/5 rounded-full"></div>
                        <div className="h-3 w-5/6 bg-slate-200 dark:bg-white/5 rounded-full"></div>
                    </div>
                    <div className="mt-6 pt-4 border-t border-slate-200/60 dark:border-white/5">
                        <div className="h-3 w-1/2 bg-slate-200 dark:bg-white/5 rounded-full"></div>
                    </div>
                </div>
            ))}
        </div>
    </div>
  </PulseWrapper>
);

// 👤 15. PROFILE PAGE SKELETON
export const SkeletonProfile = () => (
  <PulseWrapper className="w-full max-w-7xl mx-auto space-y-6 sm:space-y-8 pb-10 mt-4">
     {/* Ghost Profile Header (Layer 1) */}
     {/* 🌟 FIX: Synced to Layer 1 (dark:bg-[#1A162F]/95) */}
     <div className="rounded-2xl lg:rounded-3xl bg-white/95 dark:bg-[#1A162F]/95 border border-slate-200/80 dark:border-white/10 p-6 lg:p-8 flex flex-col sm:flex-row items-center gap-5 shadow-sm">
        <div className="w-16 h-16 lg:w-20 lg:h-20 rounded-2xl bg-slate-300/80 dark:bg-white/10 shrink-0"></div>
        <div className="space-y-3 w-full sm:w-auto flex flex-col items-center sm:items-start">
           <div className="w-48 lg:w-64 h-8 lg:h-10 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
           <div className="w-64 lg:w-80 h-4 bg-slate-200 dark:bg-white/5 rounded-full"></div>
        </div>
     </div>

     {/* Ghost Account Details (Layer 2) */}
     {/* 🌟 FIX: Synced to Layer 2 (bg-slate-50/80 dark:bg-[#131127]/80) */}
     <div className="rounded-2xl lg:rounded-3xl bg-slate-50/80 dark:bg-[#131127]/80 border border-slate-200/60 dark:border-white/5 p-6 sm:p-8 shadow-sm">
        <div className="flex items-center gap-4 mb-6 lg:mb-8 pb-5 border-b border-slate-200/60 dark:border-white/5">
           <div className="w-12 h-12 rounded-2xl bg-slate-300/80 dark:bg-white/10 shrink-0"></div>
           <div className="w-40 h-6 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
           {/* Layer 3 Inner Inputs */}
           <div className="space-y-2"><div className="w-20 h-3 bg-slate-200 dark:bg-white/5 rounded-full"></div><div className="w-full h-12 bg-white/60 dark:bg-black/20 rounded-xl border border-slate-200 dark:border-white/10"></div></div>
           <div className="space-y-2"><div className="w-20 h-3 bg-slate-200 dark:bg-white/5 rounded-full"></div><div className="w-full h-12 bg-white/60 dark:bg-black/20 rounded-xl border border-slate-200 dark:border-white/10"></div></div>
        </div>
        <div className="flex justify-end mt-6 pt-4 border-t border-slate-200/60 dark:border-white/5">
           <div className="w-40 h-10 lg:h-12 bg-slate-300/80 dark:bg-white/10 rounded-xl lg:rounded-2xl"></div>
        </div>
     </div>

     {/* Grid: Password & API Key (Layer 2) */}
     <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 sm:gap-8">
        {/* Password */}
        {/* 🌟 FIX: Synced to Layer 2 */}
        <div className="rounded-2xl lg:rounded-3xl bg-slate-50/80 dark:bg-[#131127]/80 border border-slate-200/60 dark:border-white/5 p-6 sm:p-8 shadow-sm flex flex-col">
           <div className="flex items-center gap-4 mb-6 pb-5 border-b border-slate-200/60 dark:border-white/5">
               <div className="w-12 h-12 rounded-2xl bg-slate-300/80 dark:bg-white/10 shrink-0"></div>
               <div className="w-40 h-6 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
           </div>
           <div className="space-y-5 flex-grow">
              <div className="space-y-2"><div className="w-24 h-3 bg-slate-200 dark:bg-white/5 rounded-full"></div><div className="w-full h-12 bg-white/60 dark:bg-black/20 rounded-xl border border-slate-200 dark:border-white/10"></div></div>
              <div className="space-y-2"><div className="w-24 h-3 bg-slate-200 dark:bg-white/5 rounded-full"></div><div className="w-full h-12 bg-white/60 dark:bg-black/20 rounded-xl border border-slate-200 dark:border-white/10"></div></div>
              <div className="space-y-2"><div className="w-24 h-3 bg-slate-200 dark:bg-white/5 rounded-full"></div><div className="w-full h-12 bg-white/60 dark:bg-black/20 rounded-xl border border-slate-200 dark:border-white/10"></div></div>
           </div>
           <div className="mt-6 pt-6 border-t border-slate-200/60 dark:border-white/5">
              <div className="w-full h-12 lg:h-14 bg-slate-300/80 dark:bg-white/10 rounded-xl lg:rounded-2xl"></div>
           </div>
        </div>

        {/* API Key */}
        {/* 🌟 FIX: Synced to Layer 2 */}
        <div className="rounded-2xl lg:rounded-3xl bg-slate-50/80 dark:bg-[#131127]/80 border border-slate-200/60 dark:border-white/5 p-6 sm:p-8 shadow-sm flex flex-col">
           <div className="flex items-center gap-4 mb-6 pb-5 border-b border-slate-200/60 dark:border-white/5">
               <div className="w-12 h-12 rounded-2xl bg-slate-300/80 dark:bg-white/10 shrink-0"></div>
               <div className="w-40 h-6 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
           </div>
           <div className="space-y-6 flex-grow">
              <div className="w-full h-16 bg-slate-200 dark:bg-white/5 rounded-xl"></div>
              <div className="space-y-2"><div className="w-24 h-3 bg-slate-200 dark:bg-white/5 rounded-full"></div><div className="w-full h-12 bg-white/60 dark:bg-black/20 rounded-xl border border-slate-200 dark:border-white/10"></div></div>
           </div>
           <div className="mt-6 pt-6 border-t border-slate-200/60 dark:border-white/5 space-y-4">
              <div className="w-full h-12 lg:h-14 bg-slate-300/80 dark:bg-white/10 rounded-xl lg:rounded-2xl"></div>
              <div className="w-full h-10 bg-slate-200 dark:bg-white/5 rounded-lg"></div>
           </div>
        </div>
     </div>
  </PulseWrapper>
);
// 🧠 16. JOURNAL ANALYSIS SKELETON
export const SkeletonAnalysis = ({ sectionBg, cardBorder }) => (
  <PulseWrapper className="space-y-4 lg:space-y-6">
    <div className={`rounded-xl lg:rounded-2xl p-3 lg:p-6 ${sectionBg} border ${cardBorder}`}>
      <div className="flex items-center gap-2 mb-2 lg:mb-3">
        <Brain className="w-4 h-4 lg:w-5 lg:h-5 text-teal-500/40 dark:text-teal-400/40" />
        <div className="h-3 lg:h-4 w-24 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
      </div>
      <div className="space-y-2.5 lg:space-y-3 mt-4">
        <div className="h-3 lg:h-4 bg-slate-200 dark:bg-white/5 rounded-full w-full"></div>
        <div className="h-3 lg:h-4 bg-slate-200 dark:bg-white/5 rounded-full w-11/12"></div>
        <div className="h-3 lg:h-4 bg-slate-200 dark:bg-white/5 rounded-full w-4/5"></div>
      </div>
    </div>

    <div className={`rounded-xl lg:rounded-2xl p-3 lg:p-6 ${sectionBg} border ${cardBorder}`}>
      <div className="flex flex-wrap gap-2 justify-between items-center">
         <div className="h-3 lg:h-4 w-24 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
         <div className="h-6 lg:h-8 w-32 bg-slate-200 dark:bg-white/5 rounded-full"></div>
      </div>
    </div>

    <div className={`rounded-xl lg:rounded-2xl p-3 lg:p-6 ${sectionBg} border ${cardBorder}`}>
      <div className="flex items-center gap-2 mb-3 lg:mb-4">
        <Heart className="w-4 h-4 lg:w-5 lg:h-5 text-pink-500/40 dark:text-pink-400/40" />
        <div className="h-3 lg:h-4 w-20 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
      </div>
      <div className="flex flex-wrap gap-2 lg:gap-3">
         <div className="h-7 lg:h-9 w-20 rounded-full bg-slate-200 dark:bg-white/5"></div>
         <div className="h-7 lg:h-9 w-24 rounded-full bg-slate-200 dark:bg-white/5"></div>
         <div className="h-7 lg:h-9 w-16 rounded-full bg-slate-200 dark:bg-white/5"></div>
         <div className="h-7 lg:h-9 w-28 rounded-full bg-slate-200 dark:bg-white/5"></div>
      </div>
    </div>

    <div className={`rounded-xl lg:rounded-2xl p-3 lg:p-6 ${sectionBg} border ${cardBorder}`}>
      <div className="flex items-center gap-2 mb-4 lg:mb-5">
         <Lightbulb className="w-4 h-4 lg:w-5 lg:h-5 text-amber-500/40 dark:text-amber-400/40" />
         <div className="h-3 lg:h-4 w-40 bg-slate-300/80 dark:bg-white/10 rounded-full"></div>
      </div>
      <div className="bg-white/40 dark:bg-black/20 p-4 lg:p-5 rounded-xl border border-slate-100 dark:border-white/5 space-y-3">
         <div className="h-3 lg:h-4 bg-slate-200 dark:bg-white/5 rounded-full w-full"></div>
         <div className="h-3 lg:h-4 bg-slate-200 dark:bg-white/5 rounded-full w-5/6"></div>
      </div>
    </div>
  </PulseWrapper>
);