// src/hooks/useUserProfile.js
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const getAuthHeader = () => {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Not authenticated');
    return { Authorization: `Bearer ${token}` };
};

// --- QUERIES ---

export const useUserFullProfile = () => {
    return useQuery({
        queryKey: ['userFullProfile'],
        queryFn: async () => {
            const { data } = await axios.get(`${API_BASE_URL}/users/profile-full`, { headers: getAuthHeader() });
            return data;
        },
        staleTime: 5 * 60 * 1000,
        gcTime: 10 * 60 * 1000,
    });
};

export const useApiKeyStatus = () => {
    return useQuery({
        queryKey: ['apiKeyStatus'],
        queryFn: async () => {
            const { data } = await axios.get(`${API_BASE_URL}/users/api-key/status`, { headers: getAuthHeader() });
            return data;
        },
        staleTime: 5 * 60 * 1000,
        refetchOnWindowFocus: false,
    });
};

// --- MUTATIONS ---

export const useUpdateUserProfile = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (updatedData) => {
            const { data } = await axios.put(`${API_BASE_URL}/users/profile`, updatedData, { headers: getAuthHeader() });
            return data;
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['userFullProfile'] }),
    });
};

export const useChangeUserPassword = () => {
    return useMutation({
        mutationFn: async (passwordData) => {
            const { data } = await axios.put(`${API_BASE_URL}/users/profile/password`, passwordData, { headers: getAuthHeader() });
            return data;
        },
    });
};

export const useDeleteUserProfile = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async () => {
            const { data } = await axios.delete(`${API_BASE_URL}/users/profile`, { headers: getAuthHeader() });
            return data;
        },
        onSuccess: () => {
            queryClient.removeQueries({ queryKey: ['userFullProfile'] });
            localStorage.removeItem('jwtToken');
            window.location.href = '/login';
        },
    });
};

export const useUpdateApiKey = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (apiKey) => {
            const { data } = await axios.put(
                `${API_BASE_URL}/users/api-key`,
                { apiKey },
                { headers: getAuthHeader() }
            );
            return data; // { status, message }
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['userFullProfile'] });
            queryClient.invalidateQueries({ queryKey: ['apiKeyStatus'] });
        },
    });
};

export const useUpdateRoadmapPreferences = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (preferences) => {
            const { data } = await axios.put(`${API_BASE_URL}/users/roadmap-preferences`, preferences, { headers: getAuthHeader() });
            return data;
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['userFullProfile'] }),
    });
};

export const useUpdateUserPreferences = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: async (preferencesData) => {
            const { data } = await axios.put(`${API_BASE_URL}/users/preferences`, preferencesData, { headers: getAuthHeader() });
            return data;
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['userFullProfile'] }),
    });
};