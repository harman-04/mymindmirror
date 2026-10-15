// src/utils/axiosConfig.js
import axios from 'axios';
import { toast } from 'sonner';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
axios.defaults.baseURL = API_BASE_URL;

// Request interceptor – add auth token automatically
axios.interceptors.request.use(
  (config) => {
    const isOurBackend = config.url?.startsWith(API_BASE_URL) || config.url?.startsWith('/');
    if (isOurBackend) {
      const token = localStorage.getItem('jwtToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor – handle common errors globally
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const { response, request } = error;

 // ✅ NEW: Centralized logging — one log for every API failure
    console.error('[API Error]', {
      url: error.config?.url,
      method: error.config?.method?.toUpperCase(),
      status: response?.status ?? 'no-response',
      message: response?.data?.message ?? error.message,
      fullError: error,
    });

    // Network error (no response from server)
    if (!response) {
      if (request) {
        toast.error('Network error. Cannot reach the server.', {
          id: 'network-error',
          description: 'Please check your internet connection and verify the backend is running.',
        });
      } else {
        toast.error('Request setup error', {
        id: 'request-setup-error',
          description: error.message,
        });
      }
      return Promise.reject(error);
    }

    const { status, data } = response;
    const errorMessage = data?.message || data?.error || 'An unexpected error occurred.';

// Dedupe by status + message content: same message → 1 toast, different → separate toasts
    const toastId = `error-${status}-${errorMessage}`;

    switch (status) {
      case 400:
//        toast.error('Bad Request', { description: errorMessage });
        toast.error('Bad Request', { id: toastId, description: errorMessage });

        break;

      case 401:
        if (/^\/login(\/|$)/.test(window.location.pathname)) {
//          toast.error('Login failed', {
//            description: errorMessage || 'Invalid username or password.',
//          });

 toast.error('Login failed', { id: toastId, description: errorMessage || 'Invalid username or password.' });

        } else {
//          toast.error('Session expired', {
//            description: 'Please log in again.',
//          });

          toast.error('Session expired', { id: 'session-expired', description: 'Please log in again.' });


          localStorage.removeItem('jwtToken');
          window.location.href = '/login';
        }
        break;

      case 403:
//        toast.error('Access Denied', { description: errorMessage });
        toast.error('Access Denied', { id: toastId, description: errorMessage });
        break;

      case 404:
//        toast.error('Not Found', { description: errorMessage });
        toast.error('Not Found', { id: toastId, description: errorMessage });

        break;

      case 429:
//        toast.error('Rate limit exceeded', {
//          description: errorMessage || 'Too many requests. Please wait a moment and try again.',
//        });
 toast.error('Rate limit exceeded', { id: toastId, description: errorMessage || 'Too many requests. Please wait a moment and try again.' });
        break;

      case 500:
//        toast.error('Server Error', {
//          description: errorMessage || 'Something went wrong on our end. Please try again.',
//        });
 toast.error('Server Error', { id: toastId, description: errorMessage || 'Something went wrong on our end. Please try again.' });
        break;

      case 503:
//        toast.error('Service Unavailable', {
//          description: errorMessage || 'Our servers are temporarily overloaded. Please try again in a moment.',
//        });
 toast.error('Service Unavailable', { id: toastId, description: errorMessage || 'Our servers are temporarily overloaded. Please try again in a moment.' });
        break;

      default:
//        toast.error(`Error ${status}`, { description: errorMessage });
        toast.error(`Error ${status}`, { id: toastId, description: errorMessage });

    }
    return Promise.reject(error);
  }
);

export default axios;