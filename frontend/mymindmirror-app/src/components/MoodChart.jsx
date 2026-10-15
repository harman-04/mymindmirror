import React, { useMemo, useRef, useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import { SkeletonLineChart } from './Skeleton';
import { useTheme } from '../contexts/ThemeContext';
import DownloadChartButton from './DownloadChartButton';
import { Activity } from 'lucide-react';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
    Filler,
} from 'chart.js';
import zoomPlugin from 'chartjs-plugin-zoom';

ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend,
    Filler,
    zoomPlugin
);

const EMOTION_COLORS_LIGHT = {
    'overall mood score': '#9333EA',
    'joy': '#5CC8C2', 'sadness': '#FF8A7A', 'anger': '#A93226', 'anxiety': '#F7DC6F',
    'fear': '#6C3483', 'surprise': '#85C1E9', 'neutral': '#E0E0E0', 'disgust': '#D35400',
    'disappointment': '#283747', 'remorse': '#7F8C8D', 'grief': '#17202A', 'optimism': '#F1C40F',
    'caring': '#2ECC71', 'curiosity': '#AF7AC5', 'relief': '#58D68D', 'love': '#E74C3C',
    'pride': '#F39C12', 'annoyance': '#E67E22', 'excitement': '#FFD700', 'contentment': '#90EE90',
    'frustration': '#FF4500', 'gratitude': '#ADFF2F', 'hope': '#ADD8E6',
};

const EMOTION_COLORS_DARK = {
    'overall mood score': '#C084FC',
    'joy': '#8DE2DD', 'sadness': '#C7B3E6', 'anger': '#FFB0A4', 'fear': '#D45E4D',
    'surprise': '#B0D9F7', 'neutral': '#A0A0A0', 'love': '#FF7F7F', 'disgust': '#9B6EB4',
    'anxiety': '#FFF0B3', 'optimism': '#FFD750', 'relief': '#8CE0B0', 'caring': '#58D68D',
    'curiosity': '#C79BE0', 'embarrassment': '#FF8C40', 'pride': '#FFC050', 'remorse': '#B0B8B8',
    'annoyance': '#FFAB66', 'disappointment': '#506A80', 'grief': '#404040', 'excitement': '#FFE680',
    'contentment': '#C0FFC0', 'frustration': '#FF7F50', 'gratitude': '#D0FF80', 'hope': '#C0E0FF',
};

const MoodChart = ({ entries, isLoading }) => {
    const { theme } = useTheme();
    const isDarkMode = theme === 'dark';
    const emotionColors = isDarkMode ? EMOTION_COLORS_DARK : EMOTION_COLORS_LIGHT;
    const chartContainerRef = useRef(null);

    const [isMobile, setIsMobile] = useState(window.innerWidth < 1024);

    useEffect(() => {
        const handleResize = () => setIsMobile(window.innerWidth < 1024);
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

    const { moodData, emotionTrendData } = useMemo(() => {
        if (!entries || entries.length === 0) return { moodData: [], emotionTrendData: {} };

        const moodPoints = entries
            .filter(entry => entry.moodScore !== null && entry.moodScore !== undefined)
            .map(entry => ({ date: entry.entryDate, moodScore: entry.moodScore }))
            .sort((a, b) => new Date(a.date) - new Date(b.date));

        const emotionMap = {};
        entries.forEach(entry => {
            if (entry.emotions) {
                let parsedEmotions = entry.emotions;
                if (typeof parsedEmotions === 'string') {
                    try { parsedEmotions = JSON.parse(parsedEmotions); } catch (e) { parsedEmotions = {}; }
                }
                if (typeof parsedEmotions === 'object' && parsedEmotions !== null) {
                    emotionMap[entry.entryDate] = parsedEmotions;
                }
            }
        });

        return { moodData: moodPoints, emotionTrendData: emotionMap };
    }, [entries]);

    const { chartData, chartOptions, calculatedWidth } = useMemo(() => {
        if (!moodData.length && Object.keys(emotionTrendData).length === 0) {
            return { chartData: { labels: [], datasets: [] }, chartOptions: {}, calculatedWidth: '100%' };
        }

        const allDates = [...new Set([
            ...moodData.map(d => d.date),
            ...Object.keys(emotionTrendData)
        ])].sort((a, b) => new Date(a) - new Date(b));

        const emotionDatasets = [];
        const allEmotionLabels = new Set();
        Object.values(emotionTrendData).forEach(dayEmotions => {
            Object.keys(dayEmotions).forEach(label => allEmotionLabels.add(label));
        });

        const emotionsToShow = ['joy', 'sadness', 'anger', 'anxiety', 'fear', 'neutral'];

        // 🌟 FIX: Match point cutout colors to the new background
        const pointCutoutColor = isDarkMode ? '#1A162F' : '#ffffff';

        emotionsToShow.forEach(emotionLabel => {
            if (allEmotionLabels.has(emotionLabel)) {
                emotionDatasets.push({
                    label: emotionLabel.charAt(0).toUpperCase() + emotionLabel.slice(1),
                    data: allDates.map(date => emotionTrendData[date]?.[emotionLabel] || 0),
                    borderColor: emotionColors[emotionLabel] || '#CCCCCC',
                    backgroundColor: 'transparent',
                    borderWidth: 2,
                    borderDash: [5, 5],
                    tension: 0.4,
                    pointBackgroundColor: emotionColors[emotionLabel] || '#CCCCCC',
                    pointBorderColor: pointCutoutColor,
                    pointBorderWidth: 1,
                    pointRadius: 3,
                    pointHoverRadius: 6,
                    fill: false,
                    hidden: true,
                });
            }
        });

        const moodDataset = {
            label: 'Overall Mood Score',
            data: allDates.map(date => {
                const entry = moodData.find(d => d.date === date);
                return entry ? entry.moodScore : null;
            }),
            borderColor: emotionColors['overall mood score'],
            backgroundColor: (context) => {
                const chart = context.chart;
                const { ctx, chartArea } = chart;
                if (!chartArea) return isDarkMode ? 'rgba(192, 132, 252, 0.2)' : 'rgba(147, 51, 234, 0.2)';

                const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
                if (isDarkMode) {
                    gradient.addColorStop(0, 'rgba(192, 132, 252, 0.5)');
                    gradient.addColorStop(1, 'rgba(192, 132, 252, 0.0)');
                } else {
                    gradient.addColorStop(0, 'rgba(147, 51, 234, 0.5)');
                    gradient.addColorStop(1, 'rgba(147, 51, 234, 0.0)');
                }
                return gradient;
            },
            borderWidth: window.innerWidth < 640 ? 3 : 4,
            tension: 0.4,
            pointBackgroundColor: emotionColors['overall mood score'],
            pointBorderColor: pointCutoutColor,
            pointBorderWidth: 2,
            pointRadius: window.innerWidth < 640 ? 4 : 5,
            pointHoverRadius: window.innerWidth < 640 ? 6 : 8,
            fill: true,
            spanGaps: true,
        };

        const datasets = [moodDataset, ...emotionDatasets];

        const textColor = isDarkMode ? '#E0E0E0' : '#1E1A3E';
        const axisColor = isDarkMode ? '#94A3B8' : '#475569';
        const gridColor = isDarkMode ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

        const minWidthPerPoint = isMobile ? 45 : 60;
        const totalMinWidth = allDates.length * minWidthPerPoint;
        const finalWidth = `max(100%, ${totalMinWidth}px)`;

        const options = {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        font: { family: 'Inter', size: isMobile ? 10 : 12 },
                        color: textColor,
                        usePointStyle: true,
                        padding: isMobile ? 10 : 20
                    },
                },
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
                        label: (ctx) => `${ctx.dataset.label}: ${ctx.parsed.y?.toFixed(3) ?? 'N/A'}`,
                    },
                },
                zoom: isMobile ? {} : {
                    pan: { enabled: true, mode: 'x', modifierKey: 'ctrl' },
                    zoom: { wheel: { enabled: true, modifierKey: 'ctrl' }, pinch: { enabled: false }, mode: 'x' }
                }
            },
            scales: {
                x: {
                    title: { display: false },
                    ticks: { color: axisColor, maxRotation: 45, minRotation: 45, font: { size: isMobile ? 10 : 12 } },
                    grid: { color: gridColor, drawBorder: false },
                },
                y: {
                    min: -1,
                    max: 1,
                    title: { display: false },
                    ticks: { color: axisColor, stepSize: 0.5, font: { size: isMobile ? 10 : 12 } },
                    grid: { color: gridColor, drawBorder: false },
                    border: { dash: [4, 4] }
                },
            },
        };

        return { chartData: { labels: allDates, datasets }, chartOptions: options, calculatedWidth: finalWidth };
    }, [moodData, emotionTrendData, emotionColors, isDarkMode, isMobile]);

    if (isLoading) return <SkeletonLineChart />;

    if (!chartData?.labels?.length) {
        return (
            <div className={`rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-sm overflow-hidden ${cardBg} flex flex-col h-full`}>
                <div className={`flex flex-wrap justify-between items-center gap-4 p-4 lg:p-6 border-b ${sectionBorder} ${sectionBg}`}>
                    <div className="flex-1 min-w-[200px]">
                        <h3 className={`text-lg lg:text-xl font-poppins font-extrabold ${textPrimary} tracking-tight flex items-center gap-3`}>
                            {/* 🌟 RESTORED: Purple Jewel Icon */}
                            <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-fuchsia-50 dark:from-purple-900/40 dark:to-fuchsia-800/20 text-purple-600 dark:text-purple-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-purple-700/30">
                                <Activity className="w-5 h-5 lg:w-6 lg:h-6" />
                            </div>
                            Mood & Emotion Trends
                        </h3>
                    </div>
                    <DownloadChartButton
                        chartRef={chartContainerRef}
                        filename="mood_trend_chart"
                        darkMode={isDarkMode}
                        className="opacity-50 pointer-events-none mt-2 sm:mt-0 shrink-0"
                    />
                </div>
                <div className="p-6 lg:p-10 flex-grow flex flex-col items-center justify-center text-center min-h-[260px]">
                    <div className="p-4 rounded-full bg-slate-100 dark:bg-[#131127] border border-slate-200/80 dark:border-white/5 mb-4 shadow-inner">
                       <Activity className={`w-8 h-8 lg:w-10 lg:h-10 ${textSecondary}`} />
                    </div>
                    <p className={`text-sm lg:text-base font-medium ${textSecondary}`}>
                        Not enough data to display chart. Keep journaling!
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className={`rounded-2xl lg:rounded-3xl border ${cardBorder} shadow-sm ring-1 ring-black/5 dark:ring-white/5 overflow-hidden ${cardBg} h-full flex flex-col transition-all duration-300 hover:shadow-md hover:-translate-y-0.5`}>
            {/* Header */}
            <div className={`flex flex-wrap justify-between items-center gap-4 p-4 lg:p-6 border-b ${sectionBorder} ${sectionBg}`}>
                <div className="flex-1 min-w-[200px]">
                    <h3 className={`text-lg lg:text-xl font-poppins font-extrabold ${textPrimary} tracking-tight flex items-center gap-3`}>
                        {/* 🌟 RESTORED: Purple Jewel Icon */}
                        <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-purple-100 to-fuchsia-50 dark:from-purple-900/40 dark:to-fuchsia-800/20 text-purple-600 dark:text-purple-400 shrink-0 shadow-sm border border-purple-200/50 dark:border-purple-700/30">
                            <Activity className="w-5 h-5 lg:w-6 lg:h-6" />
                        </div>
                        Mood & Emotion Trends
                    </h3>
                    <p className={`text-[11px] lg:text-xs mt-0.5 lg:mt-1 font-medium ${textSecondary}`}>
                        {isMobile
                            ? "Swipe horizontally to see history."
                            : "Scroll horizontally to see history. Hold Ctrl/Cmd + Scroll to Zoom."}
                    </p>
                </div>
                <DownloadChartButton
                    chartRef={chartContainerRef}
                    filename="mood_trend_chart"
                    darkMode={isDarkMode}
                    className="hover:scale-105 transition-transform shrink-0"
                />
            </div>

            <div
                ref={chartContainerRef}
                className="w-full flex-grow overflow-x-auto custom-scrollbar"
            >
                <div className="h-[300px] sm:h-[350px] lg:h-[450px] p-4 pr-6 lg:pr-8 pb-4 lg:pb-6" style={{ width: calculatedWidth }}>
                    <Line data={chartData} options={chartOptions} />
                </div>
            </div>
        </div>
    );
};

export default React.memo(MoodChart);