// src/hooks/useJournalData.js
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { format } from 'date-fns';
import { useState } from 'react';
import { toast } from 'sonner';
import { useInfiniteQuery } from '@tanstack/react-query';

const API_BASE_URL = 'http://localhost:8080/api';

const getToken = () => localStorage.getItem('jwtToken');

const clearTodayReflectionCache = () => {
  const todayStr = format(new Date(), 'yyyy-MM-dd');
  localStorage.removeItem(`reflection_${todayStr}`);
};

// --- Query for All Journal Entries ---
const fetchAllJournalEntries = async () => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated.');

  const response = await axios.get(`${API_BASE_URL}/journal/history`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  // Ensure entries are sorted by creationTimestamp descending for consistency
  return response.data.sort(
    (a, b) => new Date(b.creationTimestamp).getTime() - new Date(a.creationTimestamp).getTime()
  );
};

export const useJournalEntries = () => {
  const queryClient = useQueryClient();

  return useQuery({
    queryKey: ['journalEntries'],
    queryFn: fetchAllJournalEntries,
    staleTime: 5000,
    refetchOnMount: true,
    // 💡 SMART POLLING:
    // Checks every 3 seconds ONLY if an entry is still being analyzed.
    refetchInterval: (query) => {
      const isAnalyzing = query.state.data?.some(entry => entry.moodScore === null);
      return isAnalyzing ? 3000 : false;
    },
  });
};

// --- Query for Today's Reflection (Gemini Call) ---
const generateTodaysReflection = async (todayEntries, forceRefresh = false) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated to generate reflection.');

  if (!todayEntries || todayEntries.length === 0) {
    return "Journal an entry today to get your daily reflection!";
  }

  const todayStr = format(new Date(), 'yyyy-MM-dd');
  const cacheKey = `reflection_${todayStr}`;

  if (!forceRefresh) {
    const cached = localStorage.getItem(cacheKey);
    if (cached) return cached;
  }

  try {
    // Look how clean this is! We just ask the backend to do it securely.
    const springBootResponse = await axios.post(`${API_BASE_URL}/reflection/generate`, {}, {
      headers: { Authorization: `Bearer ${token}` }
    });

    const reflection = springBootResponse.data?.reflection || "Couldn't generate a reflection today. Keep journaling!";
    localStorage.setItem(cacheKey, reflection);
    return reflection;
//  } catch (err) {
//    console.error('Error generating reflection:', err.response ? err.response.data : err.message);
//    throw new Error('Failed to generate reflection.');
//  }
} catch (err) {
    console.error('Error generating reflection:', err.response ? err.response.data : err.message);
    const backendMessage = err.response?.data?.message;
    throw new Error(backendMessage || 'Failed to generate reflection.');
}
};

export const useTodaysReflection = (todayEntries) => {
  const [forceRefresh, setForceRefresh] = useState(false);

  const queryKey = ['todaysReflection', format(new Date(), 'yyyy-MM-dd'), forceRefresh];

  const query = useQuery({
    queryKey: queryKey,
    queryFn: () => generateTodaysReflection(todayEntries, forceRefresh),
    enabled: !!todayEntries && todayEntries.length > 0,
    staleTime: Infinity,
    cacheTime: 30 * 60 * 1000,
    refetchOnWindowFocus: false,
    refetchOnMount: false,
    onSettled: () => setForceRefresh(false),
  });

  const refresh = () => {
    setForceRefresh(true);
    // Reset after the query completes
  };

  return { ...query, refresh };
};


// --- Mutations (Add, Update, Delete) ---
// --- Updated useAddJournalEntry ---
export const useAddJournalEntry = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (newEntryData) => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated.');
      return axios.post(`${API_BASE_URL}/journal`, newEntryData, {
        headers: { Authorization: `Bearer ${token}` },
      });
    },
    onSuccess: (data) => {
      const newEntry = data.data;

      // 1. Update local cache immediately
      queryClient.setQueryData(['journalEntries'], (old) => [newEntry, ...(old || [])]);

      // 2. 💡 CRITICAL: Force an immediate refetch to start the polling "loop"
      queryClient.invalidateQueries({ queryKey: ['journalEntries'] });

      clearTodayReflectionCache();
      queryClient.invalidateQueries({ queryKey: ['todaysReflection'] });
      queryClient.invalidateQueries({ queryKey: ['gamificationStats'] });

      // 3. Toast Promise (remains the same)
      toast.promise(
        new Promise((resolve) => {
          const check = () => {
            const entries = queryClient.getQueryData(['journalEntries']);
            const updated = entries?.find(e => e.id === newEntry.id);
            if (updated && updated.moodScore !== null) resolve(updated);
            else setTimeout(check, 2000);
          };
          check();
        }),
        {
          loading: 'AI is reading your thoughts...',
          success: 'Analysis complete!',
          error: 'Analysis took a bit longer, checking back...',
        }
      );
    },
  });
};

// --- Updated useUpdateJournalEntry ---
export const useUpdateJournalEntry = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ entryId, updatedText }) => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated.');
      return axios.put(`${API_BASE_URL}/journal/${entryId}`, { rawText: updatedText }, {
        headers: { Authorization: `Bearer ${token}` },
      });
    },
    onSuccess: (data, variables) => {
      // 1. Refresh cache
      queryClient.invalidateQueries({ queryKey: ['journalEntries'] });
      queryClient.invalidateQueries({ queryKey: ['journalEntry', variables.entryId] });
      clearTodayReflectionCache();
      queryClient.invalidateQueries({ queryKey: ['todaysReflection'] });

      // 2. 💡 ANALYSIS NOTIFICATION (Promise version)
      if (data.data.moodScore === null) {
        toast.promise(
          new Promise((resolve) => {
            const check = () => {
              const entries = queryClient.getQueryData(['journalEntries']);
              const updated = entries?.find(e => e.id === variables.entryId);
              if (updated && updated.moodScore !== null) resolve(updated);
              else setTimeout(check, 2000);
            };
            check();
          }),
          {
            loading: 'Re-analyzing your updated entry...',
            success: 'Update analysis complete!',
            error: 'Analysis taking longer than expected.',
          }
        );
      } else {
        toast.success("Entry updated!");
      }
    },
  });
};
export const useDeleteJournalEntry = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (entryId) => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated.');
      return axios.delete(`${API_BASE_URL}/journal/${entryId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
    },
    onSuccess: (data, entryIdToDelete) => {
      // 1. Remove from the main history list cache
      queryClient.setQueryData(['journalEntries'], (old) =>
        old ? old.filter((e) => e.id !== entryIdToDelete) : []
      );

      // 2. Remove from paginated cache (if you use infinite scroll)
      queryClient.setQueryData(['journalEntries', 'paginated'], (old) => {
        if (!old) return old;
        return {
          ...old,
          pages: old.pages.map(page => ({
            ...page,
            content: page.content.filter(e => e.id !== entryIdToDelete)
          }))
        };
      });

      // 3. Wipe single entry cache
      queryClient.removeQueries({ queryKey: ['journalEntry', entryIdToDelete] });

      // 4. Force a background refresh of everything to keep counts in sync
      queryClient.invalidateQueries({ queryKey: ['journalEntries'] });
      clearTodayReflectionCache();
      queryClient.invalidateQueries({ queryKey: ['todaysReflection'] });
    },
  });
};



export const useSearchJournalEntries = (searchParams) => {
  return useQuery({
    queryKey: ['journalSearchResults', searchParams],
    queryFn: async () => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated.');

      let url = `${API_BASE_URL}/journal/history`; // Default for date search
      const params = {};

      if (searchParams.searchType === 'keyword') {
        if (!searchParams.keyword.trim()) throw new Error('Please enter a keyword to search.');
        url = `${API_BASE_URL}/journal/search/keyword`;
        params.keyword = searchParams.keyword.trim();

      } else if (searchParams.searchType === 'semantic') {
        // NEW: Semantic Concept Search routing
        if (!searchParams.concept.trim()) throw new Error('Please enter a concept to search for.');
        url = `${API_BASE_URL}/journal/search/semantic`;
        params.concept = searchParams.concept.trim();

      } else if (searchParams.searchType === 'mood') {
        const parsedMinMood = searchParams.minMood === '' ? null : parseFloat(searchParams.minMood);
        const parsedMaxMood = searchParams.maxMood === '' ? null : parseFloat(searchParams.maxMood);

        if (isNaN(parsedMinMood) && isNaN(parsedMaxMood)) {
          throw new Error('Please enter at least a minimum or maximum mood score.');
        }
        if (parsedMinMood !== null && parsedMaxMood !== null && parsedMinMood > parsedMaxMood) {
          throw new Error('Minimum mood score cannot be greater than maximum mood score.');
        }
        url = `${API_BASE_URL}/journal/search/mood`;
        if (parsedMinMood !== null) params.minMood = parsedMinMood;
        if (parsedMaxMood !== null) params.maxMood = parsedMaxMood;

      } else if (searchParams.searchType === 'date') {
        if (!searchParams.startDate && !searchParams.endDate) {
          throw new Error('Please select at least a start date or an end date.');
        }
        if (searchParams.startDate && searchParams.endDate && new Date(searchParams.startDate) > new Date(searchParams.endDate)) {
          throw new Error('Start date cannot be after end date.');
        }
        if (searchParams.startDate) params.startDate = searchParams.startDate;
        if (searchParams.endDate) params.endDate = searchParams.endDate;
      } else {
        throw new Error('Invalid search type.');
      }

      const response = await axios.get(url, {
        params: params,
        headers: { Authorization: `Bearer ${token}` },
      });
      return response.data;
    },
    // Only enable the query if searchParams indicate a valid search is intended
    enabled: !!searchParams && (
      (searchParams.searchType === 'keyword' && !!searchParams.keyword.trim()) ||
      (searchParams.searchType === 'semantic' && !!searchParams.concept.trim()) ||
      (searchParams.searchType === 'mood' && (searchParams.minMood !== '' || searchParams.maxMood !== '')) ||
      (searchParams.searchType === 'date' && (searchParams.startDate !== '' || searchParams.endDate !== ''))
    ),
    staleTime: 5 * 60 * 1000, // Cache search results for 5 minutes
    cacheTime: 10 * 60 * 1000,
  });
};

export const useKeyPhraseFrequencies = () => {
    return useQuery({
        queryKey: ['keyPhraseFrequencies'],
        queryFn: async () => {
            const token = getToken();
            if (!token) throw new Error('Not authenticated');
            const response = await axios.get(`${API_BASE_URL}/journal/key-phrases`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            return response.data;
        },
        staleTime: 10 * 60 * 1000,
    });
};




export const usePaginatedJournalEntries = (pageSize = 20, options = {}) => {
  return useInfiniteQuery({
    queryKey: ['journalEntries', 'paginated', pageSize],
    queryFn: async ({ pageParam = 0 }) => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated');
      const response = await axios.get(
        `${API_BASE_URL}/journal/history/paginated?page=${pageParam}&size=${pageSize}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      return response.data; // { content: [], pageNumber, totalPages, ... }
    },
    getNextPageParam: (lastPage) => {
      // Use lastPage.pageNumber and lastPage.totalPages directly
      if (lastPage.pageNumber < lastPage.totalPages - 1) {
        return lastPage.pageNumber + 1;
      }
      return undefined;
    },
    staleTime: 10 * 60 * 1000,
    cacheTime: 15 * 60 * 1000,
    ...options,
  });
};

/**
 * Fetches only today's journal entries.
 */
export const useTodayEntries = () => {
  const today = format(new Date(), 'yyyy-MM-dd');
  return useQuery({
    queryKey: ['journalEntries', 'today', today],
    queryFn: async () => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated');
      const response = await axios.get(`${API_BASE_URL}/journal/history?startDate=${today}&endDate=${today}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      return response.data; // already sorted descending
    },
    staleTime: 5 * 60 * 1000,
  });
};

/**
 * Fetches journal entries for a specific week (date range).
 * @param {string} startDate - YYYY-MM-DD
 * @param {string} endDate - YYYY-MM-DD
 */
export const useWeeklyEntries = (startDate, endDate) => {
  return useQuery({
    queryKey: ['journalEntries', 'weekly', startDate, endDate],
    queryFn: async () => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated');
      const response = await axios.get(`${API_BASE_URL}/journal/history?startDate=${startDate}&endDate=${endDate}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      return response.data;
    },
    staleTime: 5 * 60 * 1000,
    enabled: !!startDate && !!endDate,
  });
};


export const useImportGrowthTip = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (tipText) => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated');
      const response = await axios.post(
        `${API_BASE_URL}/milestones/import-growth-tip`,
        { tipText },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      return response.data;
    },
    onSuccess: (data) => {
      const message = data?.message || 'Growth tip added to Milestones!';
      toast.success(message);
      queryClient.invalidateQueries({ queryKey: ['milestones'] });
    },
//    onError: (error) => {
//      toast.error('Failed to add growth tip: ' + (error.response?.data?.message || error.message));
//    },
  });
};

// Fetch a single journal entry by ID
const fetchJournalEntryById = async (entryId) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated.');
  const response = await axios.get(`${API_BASE_URL}/journal/${entryId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
};

// --- FIND THIS AT THE BOTTOM OF useJournalData.js ---
export const useJournalEntryById = (entryId, enabled = true) => {
  return useQuery({
    queryKey: ['journalEntry', entryId],
    queryFn: () => fetchJournalEntryById(entryId),
    // Ensure we don't fetch if the ID is missing or explicitly disabled
    enabled: !!entryId && enabled,

    refetchInterval: (data) => {
      // Only poll if we have data AND the AI is still processing
      return (data && data.moodScore === null) ? 2000 : false;
    },

    // 💡 NEW: Stop the loop if the server says the entry is gone (404)
    retry: (failureCount, error) => {
      if (error?.response?.status === 404) return false; // Stop immediately on 404
      return failureCount < 3; // Otherwise retry 3 times
    },

    staleTime: 0,
    cacheTime: 5 * 60 * 1000,
  });
};

