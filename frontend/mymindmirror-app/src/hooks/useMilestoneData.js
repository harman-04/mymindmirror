// src/hooks/useMilestoneData.js
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { parseISO } from 'date-fns';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const getAuthHeader = () => {
  const token = localStorage.getItem('jwtToken');
  if (!token) throw new Error('Not authenticated');
  return { Authorization: `Bearer ${token}` };
};

// --- QUERIES ---
export const useMilestones = (userId) => {
  return useQuery({
    queryKey: ['milestones', userId],
    queryFn: async () => {
      const { data } = await axios.get(`${API_BASE_URL}/milestones`, { headers: getAuthHeader() });
      return data.sort((a, b) => parseISO(b.creationDate) - parseISO(a.creationDate));
    },
    enabled: !!userId,
    staleTime: 5 * 60 * 1000,
  });
};

export const useMilestoneTasks = (userId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (milestoneId) => {
      const { data } = await axios.get(`${API_BASE_URL}/milestones/${milestoneId}/tasks`, { headers: getAuthHeader() });
      return { milestoneId, tasks: data };
    },
    onSuccess: ({ milestoneId, tasks }) => {
      queryClient.setQueryData(['milestones', userId], (old) =>
        old ? old.map(m => m.id === milestoneId ? { ...m, tasks } : m) : old
      );
    }
  });
};

// --- MUTATIONS ---
export const useCreateMilestone = (userId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (newMilestone) => axios.post(`${API_BASE_URL}/milestones`, newMilestone, { headers: getAuthHeader() }),
    onMutate: async (newMilestone) => {
      await queryClient.cancelQueries(['milestones', userId]);
      const previous = queryClient.getQueryData(['milestones', userId]) || [];
      const optimistic = {
        id: `temp-${Date.now()}`, title: newMilestone.title, description: newMilestone.description || '',
        dueDate: newMilestone.dueDate || null, creationDate: new Date().toISOString(), status: 'PENDING',
        completionPercentage: 0, tasks: [],
      };
      queryClient.setQueryData(['milestones', userId], [optimistic, ...previous]);
      return { previous };
    },
    onError: (err, variables, context) => queryClient.setQueryData(['milestones', userId], context.previous),
    onSettled: () => {
      queryClient.invalidateQueries(['milestones', userId]);
      queryClient.invalidateQueries(['gamificationStats']); //  Gamification Trigger
    },
  });
};

export const useUpdateMilestone = (userId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }) => axios.put(`${API_BASE_URL}/milestones/${id}`, data, { headers: getAuthHeader() }),
    onMutate: async ({ id, data }) => {
      await queryClient.cancelQueries(['milestones', userId]);
      const previous = queryClient.getQueryData(['milestones', userId]);
      queryClient.setQueryData(['milestones', userId], (old) => old ? old.map(m => m.id === id ? { ...m, ...data } : m) : old);
      return { previous };
    },
    onError: (err, variables, context) => queryClient.setQueryData(['milestones', userId], context.previous),
    onSettled: () => queryClient.invalidateQueries(['milestones', userId]),
  });
};

export const useDeleteMilestone = (userId) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id) => axios.delete(`${API_BASE_URL}/milestones/${id}`, { headers: getAuthHeader() }),
    onMutate: async (id) => {
      await queryClient.cancelQueries(['milestones', userId]);
      const previous = queryClient.getQueryData(['milestones', userId]);
      queryClient.setQueryData(['milestones', userId], (old) => old ? old.filter(m => m.id !== id) : old);
      return { previous };
    },
    onError: (err, variables, context) => queryClient.setQueryData(['milestones', userId], context.previous),
    onSettled: () => queryClient.invalidateQueries(['milestones', userId]),
  });
};

export const useCreateMilestoneTask = (userId, expandedMilestoneId) => {
  const queryClient = useQueryClient();
  const fetchTasks = useMilestoneTasks(userId);
  return useMutation({
    mutationFn: ({ milestoneId, taskData }) => axios.post(`${API_BASE_URL}/milestones/${milestoneId}/tasks`, taskData, { headers: getAuthHeader() }),
    onMutate: async ({ milestoneId, taskData }) => {
      await queryClient.cancelQueries(['milestones', userId]);
      const previous = queryClient.getQueryData(['milestones', userId]);
      const optimisticTask = {
        id: `temp-task-${Date.now()}`, description: taskData.description, dueDate: taskData.dueDate || null,
        status: 'PENDING', creationTimestamp: new Date().toISOString(),
      };
      queryClient.setQueryData(['milestones', userId], (old) => old ? old.map(m => m.id === milestoneId ? { ...m, tasks: [...(m.tasks || []), optimisticTask] } : m) : old);
      return { previous };
    },
    onError: (err, variables, context) => queryClient.setQueryData(['milestones', userId], context.previous),
    onSettled: () => {
      queryClient.invalidateQueries(['milestones', userId]);
      queryClient.invalidateQueries(['gamificationStats']); //  Gamification Trigger
      if (expandedMilestoneId) fetchTasks.mutate(expandedMilestoneId);
    },
  });
};

export const useUpdateMilestoneTask = (userId, expandedMilestoneId) => {
  const queryClient = useQueryClient();
  const fetchTasks = useMilestoneTasks(userId);
  return useMutation({
    mutationFn: ({ milestoneId, taskId, data }) => axios.put(`${API_BASE_URL}/milestones/${milestoneId}/tasks/${taskId}`, data, { headers: getAuthHeader() }),
    onMutate: async ({ milestoneId, taskId, data }) => {
      await queryClient.cancelQueries(['milestones', userId]);
      const previous = queryClient.getQueryData(['milestones', userId]);
      queryClient.setQueryData(['milestones', userId], (old) => old ? old.map(m => m.id === milestoneId ? { ...m, tasks: m.tasks.map(t => (t.id === taskId ? { ...t, ...data } : t)) } : m) : old);
      return { previous };
    },
    onError: (err, variables, context) => queryClient.setQueryData(['milestones', userId], context.previous),
    onSettled: () => {
      queryClient.invalidateQueries(['milestones', userId]);
      queryClient.invalidateQueries(['gamificationStats']); //  Gamification Trigger
      if (expandedMilestoneId) fetchTasks.mutate(expandedMilestoneId);
    },
  });
};

export const useDeleteMilestoneTask = (userId, expandedMilestoneId) => {
  const queryClient = useQueryClient();
  const fetchTasks = useMilestoneTasks(userId);
  return useMutation({
    mutationFn: ({ milestoneId, taskId }) => axios.delete(`${API_BASE_URL}/milestones/${milestoneId}/tasks/${taskId}`, { headers: getAuthHeader() }),
    onMutate: async ({ milestoneId, taskId }) => {
      await queryClient.cancelQueries(['milestones', userId]);
      const previous = queryClient.getQueryData(['milestones', userId]);
      queryClient.setQueryData(['milestones', userId], (old) => old ? old.map(m => m.id === milestoneId ? { ...m, tasks: m.tasks.filter(t => t.id !== taskId) } : m) : old);
      return { previous };
    },
    onError: (err, variables, context) => queryClient.setQueryData(['milestones', userId], context.previous),
    onSettled: () => {
      queryClient.invalidateQueries(['milestones', userId]);
      if (expandedMilestoneId) fetchTasks.mutate(expandedMilestoneId);
    },
  });
};

export const useMilestoneInsights = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (milestoneId) => {
      const { data } = await axios.get(`${API_BASE_URL}/milestones/${milestoneId}/insights`, { headers: getAuthHeader() });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['gamificationStats']); //  Gamification Trigger
    }
  });
};