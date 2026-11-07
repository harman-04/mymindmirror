// src/hooks/useAuth.js
import { useMutation } from '@tanstack/react-query';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const useRegister = () => {
    return useMutation({
        mutationFn: async ({ username, email, password }) => {
            const response = await axios.post(`${API_BASE_URL}/auth/register`, {
                username,
                email,
                password,
            });
            return response.data;
        },
    });
};

export const useLogin = () => {
    return useMutation({
        mutationFn: async ({ username, password }) => {
            const response = await axios.post(`${API_BASE_URL}/auth/login`, {
                username,
                password,
            });
            return response.data;
        },
    });
};