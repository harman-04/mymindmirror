import React, { useState } from 'react';
import { Download, Loader2 } from 'lucide-react';
import { downloadChartAsPng } from '../utils/downloadChart';
import { toast } from 'sonner';

const DownloadChartButton = ({
  chartRef,
  filename,
  darkMode = false,
  className = '',
  onClick,
  isCustomDownloading
}) => {
  const [isInternalDownloading, setIsInternalDownloading] = useState(false);

  const isDownloading = isCustomDownloading !== undefined ? isCustomDownloading : isInternalDownloading;

  const handleDownload = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (isDownloading) return;

    if (onClick) {
      onClick(e);
      return;
    }

    if (chartRef?.current) {
      setIsInternalDownloading(true);
      try {
        await downloadChartAsPng(chartRef.current, filename, darkMode);
        toast.success("Chart downloaded successfully!");
      } catch (error) {
        toast.error("Failed to download chart.");
      } finally {
        setIsInternalDownloading(false);
      }
    }
  };

  return (
      <button
        onClick={handleDownload}
        disabled={isDownloading}
        // 🌟 UX UPGRADE: Matches the Master Palette's deepest layer so it looks like a clean, clickable dimple on the header.
        className={`p-2 flex items-center justify-center rounded-full bg-white dark:bg-black/20 hover:bg-slate-100 dark:hover:bg-black/40 active:scale-95 transition-all duration-200 shadow-sm border border-slate-200/80 dark:border-white/10 disabled:opacity-50 group ${className}`}
        title="Download as PNG"
        aria-label={`Download ${filename} as PNG`}
      >
        {isDownloading ? (
          <Loader2 size={16} className="text-purple-500 dark:text-teal-400 animate-spin" />
        ) : (
          <Download size={16} className="text-slate-600 dark:text-gray-300 group-hover:-translate-y-0.5 transition-transform" />
        )}
      </button>
    );
};

export default React.memo(DownloadChartButton);