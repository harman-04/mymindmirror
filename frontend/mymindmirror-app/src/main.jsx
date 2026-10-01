import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import './index.css';
import { ThemeProvider } from './contexts/ThemeContext';
import { BrowserRouter as Router } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'sonner';
import './utils/axiosConfig';
import InstallPrompt from './components/InstallPrompt';
import OfflineBanner from './components/OfflineBanner';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,     // Data is fresh for 5 minutes
      gcTime: 10 * 60 * 1000,       //  Renamed from cacheTime for React Query v5+
      refetchOnWindowFocus: true,
      refetchOnMount: true,
      // Retry only on transient failures. 4xx errors are user mistakes — no point retrying.
      retry: (failureCount, error) => {
        const status = error?.response?.status;

        // Don't retry 503 — service is already unavailable
        if (status === 503) return false;

        // Don't retry client errors (4xx)
        if (status && status >= 400 && status < 500) return false;
        // Retry server / network errors once
        return failureCount < 1;
      },
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')).render(
  //  Added StrictMode to catch hidden memory leaks in development
  <React.StrictMode>
    <ThemeProvider>
      <Router>
        <QueryClientProvider client={queryClient}>
          <App />

          {/* Global UI Overlays */}
          <Toaster position="top-right" richColors closeButton />
          <OfflineBanner />
          <InstallPrompt />

        </QueryClientProvider>
      </Router>
    </ThemeProvider>
  </React.StrictMode>
);