import React, { useMemo, useRef, useState, useEffect } from 'react';
import { Radar } from 'react-chartjs-2';
import { SkeletonRadarChart } from './Skeleton';
import { useTheme } from '../contexts/ThemeContext';
import DownloadChartButton from './DownloadChartButton';
import { Activity, TrendingUp, Radar as RadarIcon } from 'lucide-react';
import {
  Chart as ChartJS,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js';

ChartJS.register(
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend
);

const EMOTION_ORDER = [
  'joy', 'sadness', 'anger', 'fear', 'surprise',
  'love', 'anxiety', 'relief', 'neutral', 'excitement',
  'contentment', 'frustration', 'gratitude', 'hope'
];

const EMOTION_LABELS = {
  joy: 'Joy', sadness: 'Sadness', anger: 'Anger', fear: 'Fear',
  surprise: 'Surprise', love: 'Love', anxiety: 'Anxiety', relief: 'Relief',
  neutral: 'Neutral', excitement: 'Excitement', contentment: 'Contentment',
  frustration: 'Frustration', gratitude: 'Gratitude', hope: 'Hope'
};

const EMOTION_COLORS_VIVID = {
  joy: '#FFE600', sadness: '#3B82F6', anger: '#FF3333', fear: '#9D4EDD',
  surprise: '#FF8A00', love: '#FF42A1', anxiety: '#D97706', relief: '#14B8A6',
  neutral: '#A8A29E', excitement: '#00D4FF', contentment: '#22C55E',
  frustration: '#E11D48', gratitude: '#84CC16', hope: '#0EA5E9'
};

const EmotionRadarChart = ({ entries, isLoading }) => {
  const { theme } = useTheme();
  const isDarkMode = theme === 'dark';
  const chartContainerRef = useRef(null);
  const [isMobile, setIsMobile] = useState(window.innerWidth < 640);

  useEffect(() => {
      const handleResize = () => setIsMobile(window.innerWidth < 640);
      window.addEventListener('resize', handleResize);
      return () => window.removeEventListener('resize', handleResize);
  }, []);

  // ==========================================================================
  // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
  // ==========================================================================
  const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
  const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
  const sectionBg = isDarkMode ? 'bg-[#131127]/80' : 'bg-slate-50/80';
  const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
  const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
  const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

  const chartDataRaw = useMemo(() => {
    if (!entries || entries.length === 0) return [];

    const emotionSums = {};
    const emotionCounts = {};

    entries.forEach(entry => {
      let emotions = {};
      try {
        emotions = typeof entry.emotions === 'string' ? JSON.parse(entry.emotions) : entry.emotions;
        if (typeof emotions !== 'object' || emotions === null) emotions = {};
      } catch (e) {
        emotions = {};
      }
      Object.entries(emotions).forEach(([emotion, score]) => {
        const numericScore = parseFloat(score);
        if (!isNaN(numericScore)) {
          emotionSums[emotion] = (emotionSums[emotion] || 0) + numericScore;
          emotionCounts[emotion] = (emotionCounts[emotion] || 0) + 1;
        }
      });
    });

    return EMOTION_ORDER.map(emotion => {
      const avg = emotionSums[emotion] ? emotionSums[emotion] / emotionCounts[emotion] : 0;
      return {
        emotion: EMOTION_LABELS[emotion] || emotion,
        intensity: avg,
        fullName: emotion
      };
    });
  }, [entries]);

  const { chartData, chartOptions } = useMemo(() => {
    if (!chartDataRaw || chartDataRaw.length === 0) return { chartData: null, chartOptions: null };

    const textColor = isDarkMode ? '#E0E0E0' : '#1E1A3E';
    const axisColor = isDarkMode ? '#94A3B8' : '#475569';
    const gridColor = isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)';
    const radarColorStr = isDarkMode ? '141, 226, 221' : '179, 153, 212';
    const radarColorHex = isDarkMode ? '#8DE2DD' : '#B399D4';

    // 🌟 FIX: Match point cutout colors to the new background
    const pointCutoutColor = isDarkMode ? '#1A162F' : '#ffffff';
    const pointColors = chartDataRaw.map(d => EMOTION_COLORS_VIVID[d.fullName] || radarColorHex);

    const data = {
      labels: chartDataRaw.map(d => d.emotion),
      datasets: [
        {
          label: 'Emotion Intensity',
          data: chartDataRaw.map(d => d.intensity),
          backgroundColor: (context) => {
            const chart = context.chart;
            const { ctx, chartArea } = chart;
            if (!chartArea) return `rgba(${radarColorStr}, 0.2)`;

            const centerX = (chartArea.left + chartArea.right) / 2;
            const centerY = (chartArea.top + chartArea.bottom) / 2;
            const r = Math.min(chartArea.right - chartArea.left, chartArea.bottom - chartArea.top) / 2;

            const gradient = ctx.createRadialGradient(centerX, centerY, 0, centerX, centerY, r);
            gradient.addColorStop(0, `rgba(${radarColorStr}, 0.5)`);
            gradient.addColorStop(1, `rgba(${radarColorStr}, 0.05)`);
            return gradient;
          },
          borderColor: radarColorHex,
          borderWidth: 2,
          pointBackgroundColor: pointColors,
          pointBorderColor: pointCutoutColor,
          pointBorderWidth: 2,
          pointRadius: isMobile ? 4 : 5,
          pointHoverBackgroundColor: isDarkMode ? '#ffffff' : '#1f2937',
          pointHoverBorderColor: pointColors,
          pointHoverBorderWidth: 3,
          pointHoverRadius: isMobile ? 6 : 8,
          pointHitRadius: 15,
          fill: true,
        }
      ]
    };

    const options = {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'nearest',
        intersect: true,
      },
      plugins: {
        legend: { display: false },
        title: { display: false },
        tooltip: {
          backgroundColor: isDarkMode ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.95)',
          titleColor: textColor,
          bodyColor: axisColor,
          borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
          borderWidth: 1,
          padding: 12,
          boxPadding: 6,
          usePointStyle: true,
          callbacks: {
            label: (ctx) => `Intensity: ${ctx.parsed.r.toFixed(3)}`,
          },
        },
      },
      scales: {
        r: {
          angleLines: { color: gridColor, borderDash: [4, 4] },
          grid: { color: gridColor, circular: false, borderDash: [4, 4] },
          pointLabels: {
            color: axisColor,
            font: { family: 'Inter', size: isMobile ? 10 : 12, weight: 600 },
          },
          ticks: {
            display: true,
            color: isDarkMode ? 'rgba(148, 163, 184, 0.8)' : 'rgba(71, 85, 105, 0.8)',
            backdropColor: 'transparent',
            max: 1.0,
            min: 0,
            stepSize: 0.25,
            z: 1,
            font: { family: 'Inter', size: isMobile ? 9 : 10, weight: 500 }
          },
        },
      },
    };

    return { chartData: data, chartOptions: options };
  }, [chartDataRaw, isDarkMode, isMobile]);

  if (isLoading) return <SkeletonRadarChart />;

  if (!chartDataRaw || chartDataRaw.length === 0 || chartDataRaw.every(d => d.intensity === 0)) {
    return (
      <div className={`w-full rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-sm ring-1 ring-black/5 dark:ring-white/5 overflow-hidden ${cardBg} flex flex-col h-full`}>
          <div className={`flex flex-wrap justify-between items-center gap-4 p-4 lg:p-6 border-b ${sectionBorder} ${sectionBg}`}>
              <div className="flex-1 min-w-[200px]">
                  <h3 className={`text-lg lg:text-xl font-poppins font-extrabold ${textPrimary} tracking-tight flex items-center gap-3`}>
                      {/* 🌟 RESTORED: Indigo Jewel Icon */}
                      <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-indigo-100 to-violet-50 dark:from-indigo-900/40 dark:to-violet-800/20 text-indigo-500 dark:text-indigo-400 shrink-0 shadow-sm border border-indigo-200/50 dark:border-indigo-700/30">
                          <RadarIcon className="w-5 h-5 lg:w-6 lg:h-6" />
                      </div>
                      Emotion Radar
                  </h3>
              </div>
              <DownloadChartButton
                  chartRef={chartContainerRef}
                  filename="emotion_radar_chart"
                  darkMode={isDarkMode}
                  className="opacity-50 pointer-events-none mt-2 sm:mt-0 shrink-0"
              />
          </div>
          <div className="w-full flex-grow flex flex-col items-center justify-center p-6 lg:p-10 text-center min-h-[300px]">
             <div className="relative mb-4 lg:mb-6">
                <div className="absolute inset-0 bg-gradient-to-r from-purple-400/20 to-teal-400/20 rounded-full blur-2xl" />
                <Activity className="w-12 h-12 lg:w-16 lg:h-16 relative text-purple-400 dark:text-teal-400 animate-pulse" />
              </div>
              <p className={`text-lg lg:text-xl font-bold ${textPrimary}`}>Not enough emotional data yet</p>
              <p className={`text-sm lg:text-base text-center max-w-md mt-2 ${textSecondary}`}>
                Your emotion radar will appear here once you have journal entries with detected emotions.
              </p>
              <div className="flex items-center justify-center gap-2 text-xs lg:text-sm font-medium text-purple-500 mt-4 lg:mt-6">
                <TrendingUp className="w-4 h-4" />
                <span>The more you journal, the clearer your patterns become</span>
              </div>
          </div>
      </div>
    );
  }

  return (
    <div className={`w-full rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-sm ring-1 ring-black/5 dark:ring-white/5 overflow-hidden ${cardBg} flex flex-col h-full transition-all duration-300 hover:shadow-md hover:-translate-y-0.5`}>
      {/* Header */}
      <div className={`flex flex-wrap justify-between items-center gap-4 p-4 lg:p-6 border-b ${sectionBorder} ${sectionBg}`}>
          <div className="flex-1 min-w-[200px]">
              <h3 className={`text-lg lg:text-xl font-poppins font-extrabold ${textPrimary} tracking-tight flex items-center gap-3`}>
                  {/* 🌟 RESTORED: Indigo Jewel Icon */}
                  <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-indigo-100 to-violet-50 dark:from-indigo-900/40 dark:to-violet-800/20 text-indigo-500 dark:text-indigo-400 shrink-0 shadow-sm border border-indigo-200/50 dark:border-indigo-700/30">
                      <RadarIcon className="w-5 h-5 lg:w-6 lg:h-6" />
                  </div>
                  Emotion Radar
              </h3>
              <p className={`text-[11px] lg:text-xs mt-0.5 lg:mt-1 font-medium ${textSecondary}`}>
                  The interconnected web of your emotional state.
              </p>
          </div>

          <DownloadChartButton
              chartRef={chartContainerRef}
              filename="emotion_radar_chart"
              darkMode={isDarkMode}
              className="hover:scale-105 transition-transform shrink-0"
          />
      </div>

      <div
        ref={chartContainerRef}
        className="w-full flex-grow flex items-center justify-center p-4 sm:p-6 lg:p-8"
      >
        <div className="relative w-full max-w-3xl mx-auto h-[300px] sm:h-[400px] lg:h-[450px]">
          <Radar data={chartData} options={chartOptions} />
        </div>
      </div>

    </div>
  );
};

export default React.memo(EmotionRadarChart);