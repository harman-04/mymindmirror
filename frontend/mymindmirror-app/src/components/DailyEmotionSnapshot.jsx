import React, { useEffect, useRef, useState } from 'react';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { Doughnut } from 'react-chartjs-2';
import { useTheme } from '../contexts/ThemeContext';
import DownloadChartButton from './DownloadChartButton';
import { Smile } from 'lucide-react';
import { SkeletonDoughnutChart } from './Skeleton';
ChartJS.register(ArcElement, Tooltip, Legend);

const EMOTION_DOUGHNUT_COLORS = {
    joy: '#5CC8C2', sadness: '#B399D4', anger: '#FF8A7A', fear: '#A93226',
    surprise: '#85C1E9', neutral: '#E0E0E0', love: '#E74C3C', disgust: '#6C3483',
    anxiety: '#F7DC6F', optimism: '#F1C40F', relief: '#58D68D', caring: '#2ECC71',
    curiosity: '#AF7AC5', embarrassment: '#D35400', pride: '#F39C12', remorse: '#7F8C8D',
    annoyance: '#E67E22', disappointment: '#283747', grief: '#17202A', excitement: '#FFD700',
    contentment: '#90EE90', frustration: '#FF4500', gratitude: '#ADFF2F', hope: '#ADD8E6',
};

const EMOTION_DOUGHNUT_COLORS_DARK = {
    joy: '#8DE2DD', sadness: '#C7B3E6', anger: '#FFB0A4', fear: '#D45E4D',
    surprise: '#B0D9F7', neutral: '#A0A0A0', love: '#FF7F7F', disgust: '#9B6EB4',
    anxiety: '#FFF0B3', optimism: '#FFD750', relief: '#8CE0B0', caring: '#58D68D',
    curiosity: '#C79BE0', embarrassment: '#FF8C40', pride: '#FFC050', remorse: '#B0B8B8',
    annoyance: '#FFAB66', disappointment: '#506A80', grief: '#404040', excitement: '#FFE680',
    contentment: '#C0FFC0', frustration: '#FF7F50', gratitude: '#D0FF80', hope: '#C0E0FF',
};

function DailyEmotionSnapshot({ todayEntries , isLoading}) {
    const { theme } = useTheme();
    const isDarkMode = theme === 'dark';
    const chartContainerRef = useRef(null);
    const [chartData, setChartData] = useState({ labels: [], datasets: [] });
    const [isMobile, setIsMobile] = useState(window.innerWidth < 640);

    // ==========================================================================
    // 🌟 MASTER ELEVATION PALETTE (Single Source of Truth)
    // ==========================================================================
    const cardBg = isDarkMode ? 'bg-[#1A162F]/95 shadow-sm' : 'bg-white/95 shadow-sm';
    const cardBorder = isDarkMode ? 'border-white/10' : 'border-slate-200/80';
    const sectionBg = isDarkMode ? 'bg-[#131127]/80' : 'bg-slate-50/80';
    const sectionBorder = isDarkMode ? 'border-white/5' : 'border-slate-200/60';
    const textPrimary = isDarkMode ? 'text-gray-100' : 'text-slate-900';
    const textSecondary = isDarkMode ? 'text-gray-400' : 'text-slate-500';

    useEffect(() => {
        const handleResize = () => setIsMobile(window.innerWidth < 640);
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, []);

    useEffect(() => {
        if (!todayEntries || todayEntries.length === 0) {
            setChartData({ labels: [], datasets: [] });
            return;
        }

        const aggregated = {};
        let totalEntries = todayEntries.length;

        todayEntries.forEach(entry => {
            let emotions = {};
            try {
                emotions = typeof entry.emotions === 'string' ? JSON.parse(entry.emotions) : entry.emotions;
                if (typeof emotions !== 'object' || emotions === null) emotions = {};
            } catch (e) {
                emotions = {};
            }
            Object.entries(emotions).forEach(([emotion, score]) => {
                aggregated[emotion] = (aggregated[emotion] || 0) + score;
            });
        });

        Object.keys(aggregated).forEach(emotion => {
            aggregated[emotion] = aggregated[emotion] / totalEntries;
        });

        const labels = [];
        const data = [];
        Object.entries(aggregated).forEach(([emotion, score]) => {
            if (score > 0.01) {
                labels.push(emotion.charAt(0).toUpperCase() + emotion.slice(1));
                data.push(score);
            }
        });

        if (labels.length === 0) {
            setChartData({ labels: [], datasets: [] });
            return;
        }

        const palette = isDarkMode ? EMOTION_DOUGHNUT_COLORS_DARK : EMOTION_DOUGHNUT_COLORS;
        const backgroundColors = labels.map(label => palette[label.toLowerCase()] || '#CCCCCC');

        setChartData({
            labels,
            datasets: [{
                label: 'Emotion Intensity',
                data,
                backgroundColor: backgroundColors,
                // 🌟 FIX: Border color perfectly matches the new cardBg so the cutout looks seamless!
                borderColor: isDarkMode ? '#1A162F' : '#ffffff',
                borderWidth: isDarkMode ? 2 : 2,
                hoverOffset: 6
            }],
        });
    }, [todayEntries, isDarkMode]);

    const options = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: isMobile ? 'bottom' : 'right',
                labels: {
                    color: isDarkMode ? '#E0E0E0' : '#2D3748',
                    font: { family: 'Inter', size: isMobile ? 10 : 12 },
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
                    label: ctx => `${ctx.label}: ${(ctx.parsed * 100).toFixed(1)}%`,
                },
            },
            title: { display: false },
        },
        cutout: isMobile ? '60%' : '65%',
    };

    if (isLoading) {
        return <SkeletonDoughnutChart />;
    }
    const hasData = chartData.datasets.length > 0 && chartData.datasets[0].data.some(v => v > 0);

    if (!hasData) {
        return (
            <div className={`rounded-2xl lg:rounded-3xl border ${cardBorder} overflow-hidden ${cardBg} h-full flex flex-col transition-shadow duration-300 hover:shadow-lg`}>
                <div className={`flex flex-wrap justify-between items-center p-4 lg:p-6 border-b ${sectionBorder} ${sectionBg}`}>
                    <div className="flex-1 min-w-[200px]">
                        <h3 className={`text-lg sm:text-xl lg:text-2xl font-poppins font-bold tracking-tight ${textPrimary} flex items-center gap-3`}>
                            {/* 🌟 RESTORED: The Beautiful Gradient Jewel Icon */}
                            <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-pink-100 to-rose-50 dark:from-pink-900/40 dark:to-rose-800/20 text-pink-500 dark:text-pink-400 shrink-0 shadow-sm border border-pink-200/50 dark:border-pink-700/30">
                                <Smile className="w-5 h-5 lg:w-6 lg:h-6" />
                            </div>
                            Today's Emotion Breakdown
                        </h3>
                    </div>
                    <DownloadChartButton
                        chartRef={chartContainerRef}
                        filename="daily_emotion_snapshot"
                        darkMode={isDarkMode}
                        className="opacity-50 pointer-events-none mt-2 sm:mt-0 shrink-0"
                    />
                </div>
                {/* 🌟 FIX: Removed hardcoded background so it sits transparently over the cardBg */}
                <div
                    ref={chartContainerRef}
                    className="p-6 lg:p-10 flex-grow flex flex-col items-center justify-center text-center min-h-[260px]"
                >
                    <div className="p-4 rounded-full bg-slate-100 dark:bg-[#131127] border border-slate-200/80 dark:border-white/5 mb-4 shadow-inner">
                       <Smile className={`w-8 h-8 lg:w-10 lg:h-10 ${textSecondary}`} />
                    </div>
                    <p className={`font-bold text-sm lg:text-base ${textPrimary}`}>No emotion data available for today</p>
                    <p className={`text-xs lg:text-sm mt-2 ${textSecondary}`}>
                        Journal an entry to see your emotion breakdown!
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className={`rounded-2xl lg:rounded-3xl border ${cardBorder} overflow-hidden ${cardBg} h-full flex flex-col transition-all duration-300 hover:shadow-lg hover:-translate-y-0.5`}>
            {/* Header */}
            <div className={`flex flex-wrap justify-between items-start gap-4 p-4 lg:p-6 border-b ${sectionBorder} ${sectionBg}`}>
                <div className="flex-1 min-w-[200px]">
                    <h3 className={`text-lg sm:text-xl lg:text-2xl font-poppins font-bold tracking-tight ${textPrimary} flex items-center gap-3`}>
                        {/* 🌟 RESTORED: The Beautiful Gradient Jewel Icon */}
                        <div className="p-2 lg:p-2.5 rounded-xl lg:rounded-2xl bg-gradient-to-br from-pink-100 to-rose-50 dark:from-pink-900/40 dark:to-rose-800/20 text-pink-500 dark:text-pink-400 shrink-0 shadow-sm border border-pink-200/50 dark:border-pink-700/30">
                            <Smile className="w-5 h-5 lg:w-6 lg:h-6" />
                        </div>
                        Today's Emotion Breakdown
                    </h3>
                    <p className={`text-[11px] lg:text-xs mt-1 font-medium ${textSecondary}`}>
                        Aggregated distribution of emotions from today's inputs.
                    </p>
                </div>
                <DownloadChartButton
                    chartRef={chartContainerRef}
                    filename="daily_emotion_snapshot"
                    darkMode={isDarkMode}
                    className="hover:scale-105 transition-transform shrink-0"
                />
            </div>
            {/* Chart container */}
            {/* 🌟 FIX: Removed hardcoded background so it sits transparently over the cardBg */}
            <div
                ref={chartContainerRef}
                className="p-4 sm:p-6 flex-grow flex items-center justify-center"
            >
                <div className="relative w-full h-[220px] sm:h-[260px] lg:h-[300px]">
                    <Doughnut data={chartData} options={options} />
                </div>
            </div>
        </div>
    );
}

export default React.memo(DailyEmotionSnapshot);