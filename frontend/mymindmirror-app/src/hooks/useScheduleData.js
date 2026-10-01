// src/hooks/useScheduleData.js
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import axios from 'axios';
import { format } from 'date-fns';
import { toast } from 'sonner';

const API_BASE_URL = 'http://localhost:8080/api';

const getAuthHeader = () => {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Not authenticated');
    return { Authorization: `Bearer ${token}` };
};

// --- QUERIES ---

export const useScheduledTasks = (startDate, endDate) => {
    return useQuery({
        queryKey: ['scheduledTasks', startDate, endDate],
        queryFn: async () => {
            const response = await axios.get(`${API_BASE_URL}/schedule/tasks`, {
                params: {
                    startDate: format(startDate, 'yyyy-MM-dd'),
                    endDate: format(endDate, 'yyyy-MM-dd')
                },
                headers: getAuthHeader(),
            });
            return response.data;
        },
        enabled: !!startDate && !!endDate,
        staleTime: 5 * 60 * 1000,
        placeholderData: keepPreviousData,
    });
};

export const useCustomTasks = () => {
    return useQuery({
        queryKey: ['customTasks'],
        queryFn: async () => {
            const response = await axios.get(`${API_BASE_URL}/custom-tasks`, { headers: getAuthHeader() });
            return response.data;
        },
        staleTime: 5 * 60 * 1000,
    });
};

// --- MUTATIONS ---

export const useGenerateSchedule = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (mode) => {
            await axios.post(`${API_BASE_URL}/schedule/generate?mode=${mode}`, {}, { headers: getAuthHeader() });
        },
        onSuccess: (_, mode) => {
            queryClient.invalidateQueries({ queryKey: ['scheduledTasks'] });
            queryClient.invalidateQueries({ queryKey: ['gamificationStats'] });
            toast.success(`Schedule generated (${mode === 'custom' ? 'custom only' : 'all tasks'})!`);
        },
//        onError: () => toast.error('Generation failed'),
    });
};

export const useReoptimizeSchedule = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async () => {
            await axios.post(`${API_BASE_URL}/schedule/reoptimize`, {}, { headers: getAuthHeader() });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['scheduledTasks'] });
            queryClient.invalidateQueries({ queryKey: ['gamificationStats'] });
            toast.success('Today re-optimized successfully! ⚡');
        },
//        onError: () => toast.error('Re-optimization failed'),
    });
};

export const useMoveScheduledTask = (dateRange) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async ({ taskId, date, startTime, endTime }) => {
            await axios.put(`${API_BASE_URL}/schedule/task/${taskId}/move`,
                { date, startTime, endTime },
                { headers: getAuthHeader() }
            );
        },
        onMutate: async ({ taskId, date, startTime, endTime }) => {
            await queryClient.cancelQueries({ queryKey: ['scheduledTasks'] });
            const previousEvents = queryClient.getQueryData(['scheduledTasks', dateRange[0], dateRange[1]]);
            queryClient.setQueryData(['scheduledTasks', dateRange[0], dateRange[1]], (old) =>
                old?.map(task => task.id === taskId ? { ...task, scheduledDate: date, startTime, endTime } : task)
            );
            return { previousEvents };
        },
        onError: (err, vars, context) => {
            queryClient.setQueryData(['scheduledTasks', dateRange[0], dateRange[1]], context.previousEvents);
//            toast.error('Move failed');
        },
        onSuccess: () => toast.success('Task moved'),
        onSettled: () => queryClient.invalidateQueries({ queryKey: ['scheduledTasks'] }),
    });
};

export const useCompleteScheduledTask = (dateRange) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (taskId) => {
            await axios.patch(`${API_BASE_URL}/schedule/task/${taskId}/complete`, {}, { headers: getAuthHeader() });
        },
        onMutate: async (taskId) => {
            await queryClient.cancelQueries({ queryKey: ['scheduledTasks'] });
            const previousEvents = queryClient.getQueryData(['scheduledTasks', dateRange[0], dateRange[1]]);
            queryClient.setQueryData(['scheduledTasks', dateRange[0], dateRange[1]], (old) =>
                old?.map(task => task.id === taskId ? { ...task, completed: true } : task)
            );
            return { previousEvents };
        },
        onError: (err, taskId, context) => {
            queryClient.setQueryData(['scheduledTasks', dateRange[0], dateRange[1]], context.previousEvents);
//            toast.error('Failed to complete');
        },
        onSuccess: () => toast.success('Completed! 🎉'),
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: ['scheduledTasks'] });
            queryClient.invalidateQueries({ queryKey: ['customTasks'] });
            queryClient.invalidateQueries({ queryKey: ['gamificationStats'] });
        },
    });
};

export const useScheduleCustomTask = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (taskData) => {
            await axios.post(`${API_BASE_URL}/schedule/task/custom`, taskData, { headers: getAuthHeader() });
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['scheduledTasks'] });
            toast.success('Task scheduled successfully!');
        },
//        onError: () => toast.error('Failed to schedule task'),
    });
};

// Custom Task CRUD Mutations
export const useCreateCustomTask = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (task) => await axios.post(`${API_BASE_URL}/custom-tasks`, task, { headers: getAuthHeader() }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['customTasks'] });
            queryClient.invalidateQueries({ queryKey: ['gamificationStats'] });
            toast.success('Task added');
        },
    });
};

export const useUpdateCustomTask = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async ({ id, task }) => await axios.put(`${API_BASE_URL}/custom-tasks/${id}`, task, { headers: getAuthHeader() }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['customTasks'] });
            queryClient.invalidateQueries({ queryKey: ['scheduledTasks'] });
            toast.success('Task updated');
        },
    });
};

export const useDeleteCustomTask = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (id) => await axios.delete(`${API_BASE_URL}/custom-tasks/${id}`, { headers: getAuthHeader() }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['customTasks'] });
            queryClient.invalidateQueries({ queryKey: ['scheduledTasks'] });
            toast.success('Task deleted');
        },
    });
};