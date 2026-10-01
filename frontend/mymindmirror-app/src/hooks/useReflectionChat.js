// src/hooks/useReflectionChat.js
import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { toast } from 'sonner';

const API_BASE_URL = 'http://localhost:8080/api';

const getToken = () => localStorage.getItem('jwtToken');

// 1. Hook to ask the AI for a new reflective question
export const useSuggestQuestion = () => {
  return useMutation({
    mutationFn: async () => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated');
      const response = await axios.post(
        `${API_BASE_URL}/chat/suggest-question`,
        {},
        { headers: { Authorization: `Bearer ${token}` } }
      );
      return response.data?.answer || "What's one thing you've learned about yourself recently?";
    },
//    onError: () => {
//      toast.error("Failed to generate a new question.");
//    }
  });
};

// 2. Hook to send a chat message to the AI
export const useSendChatMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ query, sessionId, rememberChat }) => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated.');

      const response = await axios.post(
        `${API_BASE_URL}/chat/reflect`,
        { query, sessionId, rememberChat },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      return response.data.answer;
    },
    onSuccess: () => {
      // 💡 Reward XP instantly when they chat!
      queryClient.invalidateQueries({ queryKey: ['gamificationStats'] });
    }
  });
};

// 3. Hook to clear the AI's Redis memory for this session
export const useClearChatMemory = () => {
  return useMutation({
    mutationFn: async (sessionId) => {
      const token = getToken();
      if (!token) throw new Error('Not authenticated.');
      if (!sessionId) return;

      return axios.delete(`${API_BASE_URL}/chat/clear-memory`, {
        params: { sessionId },
        headers: { Authorization: `Bearer ${token}` }
      });
    },
    onSuccess: () => {
      toast.success("AI memory cleared successfully.");
    },
//    onError: () => {
//      toast.error("Failed to clear AI memory.");
//    }
  });
};