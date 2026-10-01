// src/hooks/useAnomalyData.js
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { format, subDays } from 'date-fns';

const API_BASE_URL = 'http://localhost:8080/api/ml';

// Sync with backend: app.anomaly.min-days in application.properties
const MIN_DAYS_FOR_ANOMALY = 7;

const getAuthHeader = () => {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Not authenticated');
    return { Authorization: `Bearer ${token}` };
};

export const useAnomalies = () => {
    return useQuery({
        queryKey: ['anomalies'],
        queryFn: async () => {
            const today = format(new Date(), 'yyyy-MM-dd');
            const thirtyDaysAgo = format(subDays(new Date(), 30), 'yyyy-MM-dd');

            const aggregatedDataResponse = await axios.get(
                `${API_BASE_URL}/daily-aggregated-data?startDate=${thirtyDaysAgo}&endDate=${today}`,
                { headers: getAuthHeader() }
            );
            const aggregatedData = aggregatedDataResponse.data;

            // Not enough data yet — tell the UI why
            if (!aggregatedData || aggregatedData.length < MIN_DAYS_FOR_ANOMALY) {
                return { anomalies: [], insufficientData: true, daysAvailable: aggregatedData?.length ?? 0 };
            }
//            if (!aggregatedData || aggregatedData.length < 7) {
//                return {
//                    anomalies: [],
//                    insufficientData: true,
//                    daysAvailable: aggregatedData?.length ?? 0,
//                };
//            }


            const anomalyResponse = await axios.post(
                `${API_BASE_URL}/anomaly-detection`,
                { data: aggregatedData },          // ← wrapper record shape
                { headers: getAuthHeader() }
            );

            if (!anomalyResponse.data || !anomalyResponse.data.anomalies) {
                throw new Error("Anomaly detection response was malformed.");
            }

            return {
                anomalies: anomalyResponse.data.anomalies,
                insufficientData: false,
                daysAvailable: aggregatedData.length,
            };
        },
        staleTime: 30 * 60 * 1000,
        refetchOnWindowFocus: false,
    });
};

//export const useAnomalies = () => {
//    return useQuery({
//        queryKey: ['anomalies'],
//        queryFn: async () => {
//            const today = format(new Date(), 'yyyy-MM-dd');
//            const thirtyDaysAgo = format(subDays(new Date(), 30), 'yyyy-MM-dd');
//
//            // 1. Fetch aggregated data
//            const aggregatedDataResponse = await axios.get(
//                `${API_BASE_URL}/daily-aggregated-data?startDate=${thirtyDaysAgo}&endDate=${today}`,
//                { headers: getAuthHeader() }
//            );
//            const aggregatedData = aggregatedDataResponse.data;
//
//            // If not enough data, return empty array safely
//            if (!aggregatedData || aggregatedData.length < 7) {
//                return [];
//            }
//
//            // 2. Send to ML Service for Anomaly Detection
//            const anomalyResponse = await axios.post(
//                `${API_BASE_URL}/anomaly-detection`,
//                 { data: aggregatedData },
//                { headers: getAuthHeader() }
//            );
//
//            if (!anomalyResponse.data || !anomalyResponse.data.anomalies) {
//                throw new Error("Anomaly detection response was malformed.");
//            }
//
//            return anomalyResponse.data.anomalies;
//        },
//        // 💡 OPTIMIZATION: Anomalies don't change every minute.
//        // Cache this for 30 minutes to save ML Server compute power!
//        staleTime: 30 * 60 * 1000,
//        refetchOnWindowFocus: false,
//    });
//};