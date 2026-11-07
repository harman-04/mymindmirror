import { useQuery } from '@tanstack/react-query';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const getToken = () => localStorage.getItem('jwtToken');

const fetchGamificationStats = async () => {
  const token = getToken();
  if (!token) throw new Error('Not authenticated');
  const response = await axios.get(`${API_BASE_URL}/gamification/stats`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
};

export const useGamificationStats = () => {
  return useQuery({
    queryKey: ['gamificationStats'],
    queryFn: fetchGamificationStats,
    staleTime: 5 * 60 * 1000,
  });
};