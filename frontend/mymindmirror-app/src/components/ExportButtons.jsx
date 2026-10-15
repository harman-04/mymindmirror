import React, { useState } from 'react';
import { FileText, FileSpreadsheet, Loader2 } from 'lucide-react';
import { useTheme } from '../contexts/ThemeContext';
import { useJournalEntries } from '../hooks/useJournalData';
import { saveAs } from 'file-saver';
import jsPDF from 'jspdf';
import { toast } from 'sonner';
import { SkeletonExportButtons } from './Skeleton';

function ExportButtons() {
    const { theme } = useTheme();
    const isDarkMode = theme === 'dark';
    const { data: entries, isLoading } = useJournalEntries();
    const [isExportingCSV, setIsExportingCSV] = useState(false);
    const [isExportingPDF, setIsExportingPDF] = useState(false);

    const formatEmotions = (emotions) => {
        if (!emotions) return '';
        if (typeof emotions === 'string') {
            try { emotions = JSON.parse(emotions); } catch (e) { return emotions; }
        }
        if (typeof emotions === 'object') {
            return Object.entries(emotions)
                .filter(([, score]) => score > 0.01)
                .map(([emotion, score]) => `${emotion} (${(score * 100).toFixed(1)}%)`)
                .join(', ');
        }
        return String(emotions);
    };

    const cleanMarkdown = (rawText) => {
        if (!rawText) return '';
        return rawText
            .replace(/##\s*(.*)/g, (match, p1) => p1.toUpperCase())
            .replace(/^>\s*(.*)/gm, '  "$1"')
            .replace(/\*\*(.*?)\*\*/g, '$1')
            .replace(/\*(.*?)\*/g, '$1')
            .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1');
    };

    const exportToCSV = () => {
        if (!entries || entries.length === 0) {
            toast.info("No entries to export yet.");
            return;
        }
        setIsExportingCSV(true);

        setTimeout(() => {
            try {
                const headers = [
                    'Date', 'Creation Time', 'Raw Text', 'Mood Score',
                    'Emotions (readable)', 'Core Concerns', 'Summary',
                    'Growth Tips', 'Key Phrases'
                ];

                const rows = entries.map(entry => [
                    entry.entryDate,
                    entry.creationTimestamp,
                    `"${cleanMarkdown(entry.rawText).replace(/"/g, '""')}"`,
                    entry.moodScore ?? '',
                    `"${formatEmotions(entry.emotions).replace(/"/g, '""')}"`,
                    `"${(entry.coreConcerns || []).join('; ').replace(/"/g, '""')}"`,
                    `"${cleanMarkdown(entry.summary).replace(/"/g, '""')}"`,
                    `"${(entry.growthTips || []).map(cleanMarkdown).join('; ').replace(/"/g, '""')}"`,
                    `"${(entry.keyPhrases || []).join('; ').replace(/"/g, '""')}"`
                ]);

                const csvContent = [headers, ...rows].map(row => row.join(',')).join('\n');
                const blob = new Blob(["\uFEFF" + csvContent], { type: 'text/csv;charset=utf-8;' });
                saveAs(blob, `MyMindMirror_Journal_${new Date().toISOString().slice(0,10)}.csv`);

                toast.success("CSV exported successfully!");
            } catch (error) {
                console.error("CSV Export failed:", error);
                toast.error("Failed to generate CSV. Please try again.");
            } finally {
                setIsExportingCSV(false);
            }
        }, 50);
    };

    const exportToPDF = () => {
        if (!entries || entries.length === 0) {
            toast.info("No entries to export yet.");
            return;
        }
        setIsExportingPDF(true);

        setTimeout(() => {
            try {
                const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
                const pageWidth = doc.internal.pageSize.getWidth();
                const pageHeight = doc.internal.pageSize.getHeight();
                const leftMargin = 14;
                const rightMargin = 14;
                const topMargin = 20;
                const bottomMargin = 20;
                const maxTextWidth = pageWidth - leftMargin - rightMargin;
                let yOffset = topMargin;
                let currentPage = 1;

                const needsNewPage = (requiredHeight) => requiredHeight > (pageHeight - yOffset - bottomMargin);

                const addNewPage = () => {
                    doc.addPage();
                    currentPage++;
                    yOffset = topMargin;
                };

                const renderTextBlock = (text, fontSize, isBold = false, gapAfter = 2) => {
                    if (!text) return 0;
                    doc.setFontSize(fontSize);
                    doc.setFont(undefined, isBold ? 'bold' : 'normal');

                    const lines = doc.splitTextToSize(text, maxTextWidth);
                    const textDimensions = doc.getTextDimensions(lines.join('\n'), { maxWidth: maxTextWidth });
                    const blockHeight = textDimensions.h;

                    if (needsNewPage(blockHeight + gapAfter)) addNewPage();

                    doc.text(lines, leftMargin, yOffset);
                    yOffset += blockHeight + gapAfter;
                    return blockHeight + gapAfter;
                };

                const renderSection = (label, content, fontSize = 10, labelBold = true, gapAfter = 3) => {
                    if (!content) return;
                    renderTextBlock(labelBold ? `${label}:` : label, fontSize, true, 1);
                    renderTextBlock(cleanMarkdown(content), fontSize, false, gapAfter);
                };

                const renderBulletList = (label, items, fontSize = 10, gapAfter = 3) => {
                    if (!items || items.length === 0) return;
                    renderTextBlock(`${label}:`, fontSize, true, 1);
                    for (const item of items) {
                        renderTextBlock(`• ${cleanMarkdown(item)}`, fontSize, false, 1.5);
                    }
                    yOffset += gapAfter;
                };

                const renderComplexList = (label, items, fontSize = 9, gapAfter = 4) => {
                    if (!items || items.length === 0) return;
                    renderTextBlock(`${label}:`, 10, true, 1.5);

                    items.forEach(item => {
                        const cleanedItem = cleanMarkdown(item);
                        const lines = cleanedItem.split('\n');
                        lines.forEach((line, index) => {
                            if (line.trim() === '') return;
                            const prefix = index === 0 ? '• ' : '   ';
                            renderTextBlock(`${prefix}${line.trim()}`, fontSize, false, 1.5);
                        });
                        yOffset += 2;
                    });
                    yOffset += gapAfter;
                };

                const addFooter = (pageNumber) => {
                    doc.setFontSize(8);
                    doc.setTextColor(150, 150, 150);
                    doc.text(`Page ${pageNumber}`, pageWidth / 2, pageHeight - 10, { align: 'center' });
                };

                doc.setFontSize(18);
                doc.setTextColor(139, 92, 246);
                doc.text('MyMindMirror Journal Export', leftMargin, yOffset);
                yOffset += 8;
                doc.setFontSize(10);
                doc.setTextColor(100, 100, 100);
                doc.text(`Generated: ${new Date().toLocaleString()}`, leftMargin, yOffset);
                yOffset += 12;

                for (let i = 0; i < entries.length; i++) {
                    const entry = entries[i];

                    if (needsNewPage(15)) addNewPage();

                    doc.setFontSize(12);
                    doc.setTextColor(0, 0, 0);
                    doc.setFont(undefined, 'bold');
                    doc.text(`Entry ${i + 1}: ${entry.entryDate}`, leftMargin, yOffset);
                    yOffset += 6;

                    doc.setFont(undefined, 'normal');
                    doc.setFontSize(9);
                    doc.setTextColor(80, 80, 80);
                    doc.text(`Created: ${entry.creationTimestamp}`, leftMargin, yOffset);
                    yOffset += 6;

                    doc.setTextColor(0, 0, 0);
                    doc.text(`Mood Score: ${entry.moodScore?.toFixed(2) ?? 'N/A'}`, leftMargin, yOffset);
                    yOffset += 6;

                    if (entry.summary) renderSection('Summary', entry.summary, 10, true, 4);
                    if (entry.rawText) renderSection('Raw Text', entry.rawText, 9, true, 5);

                    const emotionsStr = formatEmotions(entry.emotions);
                    if (emotionsStr) renderSection('Emotions', emotionsStr, 9, true, 3);

                    renderBulletList('Core Concerns', entry.coreConcerns, 9, 4);
                    renderComplexList('Growth Tips', entry.growthTips, 9, 4);

                    if (entry.keyPhrases?.length) renderSection('Key Phrases', entry.keyPhrases.join(', '), 9, true, 3);

                    if (i < entries.length - 1) {
                        if (needsNewPage(5)) addNewPage();
                        doc.setDrawColor(220, 220, 220);
                        doc.line(leftMargin, yOffset, pageWidth - rightMargin, yOffset);
                        yOffset += 6;
                    }
                }

                const totalPages = doc.getNumberOfPages();
                for (let p = 1; p <= totalPages; p++) {
                    doc.setPage(p);
                    addFooter(p);
                }

                doc.save(`MyMindMirror_Journal_${new Date().toISOString().slice(0,10)}.pdf`);
                toast.success("PDF exported successfully!");

            } catch (error) {
                console.error("PDF Export failed:", error);
                toast.error("Failed to generate PDF. Please try again.");
            } finally {
                setIsExportingPDF(false);
            }
        }, 50);
    };

    if (isLoading) {
        return <SkeletonExportButtons />;
    }

    if (!entries || entries.length === 0) return null;

    return (
        <div className="flex items-center gap-2 lg:gap-3">
            <div className="flex items-center gap-2 px-2 hidden lg:flex mr-1">
                <div className="w-2 h-2 rounded-full bg-purple-500 dark:bg-teal-400 animate-pulse" />
                <span className="text-[10px] uppercase tracking-[0.2em] font-bold text-slate-500 dark:text-slate-400 font-poppins">
                    Export
                </span>
            </div>

            <button
                onClick={exportToCSV}
                disabled={isExportingCSV || isExportingPDF}
                // 🌟 UX UPGRADE: Jewel Button treatment!
                className={`group relative flex items-center justify-center gap-2 px-4 py-2 lg:px-5 lg:py-2.5 rounded-xl transition-all duration-200 min-w-[80px] lg:min-w-[100px] outline-none shadow-sm border
                            ${isDarkMode
                                ? 'bg-teal-500/10 text-teal-400 border-teal-500/20 hover:bg-teal-500/20'
                                : 'bg-emerald-50 text-emerald-600 border-emerald-200/80 hover:bg-emerald-100'}
                            disabled:opacity-50 active:scale-95`}
            >
                {isExportingCSV ? (
                    <Loader2 className="w-4 h-4 lg:w-5 lg:h-5 animate-spin" />
                ) : (
                    <>
                        <FileSpreadsheet className="w-4 h-4 lg:w-5 lg:h-5 group-hover:-translate-y-0.5 group-hover:rotate-6 transition-transform duration-300" />
                        <span className="font-poppins font-semibold text-xs lg:text-sm">CSV</span>
                    </>
                )}
            </button>

            <button
                onClick={exportToPDF}
                disabled={isExportingCSV || isExportingPDF}
                // 🌟 UX UPGRADE: Jewel Button treatment!
                className={`group relative flex items-center justify-center gap-2 px-4 py-2 lg:px-5 lg:py-2.5 rounded-xl transition-all duration-200 min-w-[80px] lg:min-w-[100px] outline-none shadow-sm border
                            ${isDarkMode
                                ? 'bg-purple-500/10 text-purple-400 border-purple-500/20 hover:bg-purple-500/20'
                                : 'bg-rose-50 text-rose-600 border-rose-200/80 hover:bg-rose-100'}
                            disabled:opacity-50 active:scale-95`}
            >
                {isExportingPDF ? (
                    <Loader2 className="w-4 h-4 lg:w-5 lg:h-5 animate-spin" />
                ) : (
                    <>
                        <FileText className="w-4 h-4 lg:w-5 lg:h-5 group-hover:-translate-y-0.5 group-hover:-rotate-6 transition-transform duration-300" />
                        <span className="font-poppins font-semibold text-xs lg:text-sm">PDF</span>
                    </>
                )}
            </button>
        </div>
    );
}

export default React.memo(ExportButtons);