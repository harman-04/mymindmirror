import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { toast } from 'sonner';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const getToken = () => localStorage.getItem('jwtToken');

const fetchRoadmaps = async () => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  const response = await axios.get(`${API_BASE_URL}/roadmap`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
};

const generateRoadmap = async ({ goal, timeframeValue, timeframeUnit, difficulty, language, learningStyle, hoursPerWeek, avoidWeekends, timeframeWeeks }) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  const payload = {
    goal,
    timeframeValue,
    timeframeUnit,
    difficulty,
    language,
    learningStyle,
    hoursPerWeek,
    avoidWeekends,
  };
  // For backward compatibility
  if (timeframeWeeks) payload.timeframeWeeks = timeframeWeeks;
  const response = await axios.post(
    `${API_BASE_URL}/roadmap/generate`,
    payload,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return response.data;
};

export const useRoadmaps = () => {
  return useQuery({
    queryKey: ['roadmaps'],
    queryFn: fetchRoadmaps,
    staleTime: 5 * 60 * 1000,
  });
};

export const useGenerateRoadmap = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: generateRoadmap,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] });
      queryClient.invalidateQueries({ queryKey: ['gamificationStats'] });
    },
  });
};

const deleteRoadmap = async (id) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  await axios.delete(`${API_BASE_URL}/roadmap/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
};

export const useDeleteRoadmap = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteRoadmap,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] });
    },
  });
};

const importTaskToMilestone = async ({ roadmapId, taskId }) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  await axios.post(
    `${API_BASE_URL}/roadmap/import-task`,
    { roadmapId, taskId },
    { headers: { Authorization: `Bearer ${token}` } }
  );
};

export const useImportTaskToMilestone = () => {
  return useMutation({
    mutationFn: importTaskToMilestone,
    onSuccess: () => {
      // Optionally show a toast notification
      console.log('Task imported to milestones');
      toast.success('Task added to Milestones');

    },
  });
};

const toggleTaskCompletion = async (taskId) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  await axios.patch(
    `${API_BASE_URL}/roadmap/task/${taskId}/toggle`,
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
};

export const useToggleTaskCompletion = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: toggleTaskCompletion,
    onMutate: async (taskId) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['roadmaps'] });
      // Snapshot previous value
      const previousRoadmaps = queryClient.getQueryData(['roadmaps']);
      // Optimistically update the cache: find the task and flip its completed flag
      if (previousRoadmaps) {
        const updatedRoadmaps = previousRoadmaps.map(roadmap => ({
          ...roadmap,
          tasks: roadmap.tasks.map(task =>
            task.id === taskId ? { ...task, completed: !task.completed } : task
          )
        }));
        queryClient.setQueryData(['roadmaps'], updatedRoadmaps);
      }
      return { previousRoadmaps };
    },
    onError: (err, taskId, context) => {
      // Rollback on error
      if (context?.previousRoadmaps) {
        queryClient.setQueryData(['roadmaps'], context.previousRoadmaps);
      }
    },
    onSettled: () => {
      // Refetch in background to sync with server
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] });
      queryClient.invalidateQueries({ queryKey: ['gamificationStats'] });
    },
  });
};

const continueRoadmap = async (roadmapId) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  const response = await axios.post(
    `${API_BASE_URL}/roadmap/${roadmapId}/continue`,
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return response.data;
};

export const useContinueRoadmap = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: continueRoadmap,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] });
    },
  });
};

const elaborateTask = async ({ taskId, enhance }) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  const response = await axios.post(
    `${API_BASE_URL}/roadmap/task/${taskId}/elaborate?enhance=${enhance}`,
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return response.data;
};

export const useElaborateTask = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: elaborateTask,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] });
      queryClient.invalidateQueries({ queryKey: ['gamificationStats'] });
    },
  });
};

const rescheduleRoadmap = async (roadmapId) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  const response = await axios.post(
    `${API_BASE_URL}/roadmap/${roadmapId}/reschedule`,
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return response.data;
};

export const useRescheduleRoadmap = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: rescheduleRoadmap,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] });
    },
  });
};


const continueRoadmapBatch = async ({ roadmapId, weeksToGenerate }) => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  const params = weeksToGenerate ? `?weeksToGenerate=${weeksToGenerate}` : '';
  const response = await axios.post(
    `${API_BASE_URL}/roadmap/${roadmapId}/continue-batch${params}`,
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return response.data;
};

export const useContinueRoadmapBatch = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: continueRoadmapBatch,
    onSuccess: (data, variables) => {
      // Invalidate the roadmaps query to refetch the updated roadmap list
      queryClient.invalidateQueries({ queryKey: ['roadmaps'] });
    },
  });
};