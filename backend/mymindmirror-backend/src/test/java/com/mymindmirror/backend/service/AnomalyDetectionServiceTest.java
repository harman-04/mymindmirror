package com.mymindmirror.backend.service;

import com.mymindmirror.backend.payload.response.Anomaly;
import com.mymindmirror.backend.payload.response.AnomalyDetectionResponse;
import com.mymindmirror.backend.payload.response.DailyAggregatedDataResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        service = new AnomalyDetectionService();
        setField(service, "ewmaSpan", 7);
        setField(service, "moodThresholdStd", 0.6);
        setField(service, "wordsThresholdStd", 0.8);
        setField(service, "minDaysForAnomaly", 7);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- Helpers (unchanged) ----

    private List<DailyAggregatedDataResponse> createStableData(int days) {
        List<DailyAggregatedDataResponse> list = new ArrayList<>();
        LocalDate base = LocalDate.now();
        for (int i = 0; i < days; i++) {
            list.add(new DailyAggregatedDataResponse(base.minusDays(days - 1 - i), 0.5, 100L));
        }
        return list;
    }

    private List<DailyAggregatedDataResponse> createDataWithMoodDrop(int days) {
        List<DailyAggregatedDataResponse> list = createStableData(days);
        list.set(7, new DailyAggregatedDataResponse(list.get(7).date(), -0.8, list.get(7).totalWords()));
        return list;
    }

    private List<DailyAggregatedDataResponse> createDataWithWordsSpike(int days) {
        List<DailyAggregatedDataResponse> list = createStableData(days);
        list.set(7, new DailyAggregatedDataResponse(list.get(7).date(), list.get(7).averageMood(), 500L));
        return list;
    }

    private List<DailyAggregatedDataResponse> createDataWithBothAnomalies(int days) {
        List<DailyAggregatedDataResponse> list = createStableData(days);
        list.set(7, new DailyAggregatedDataResponse(list.get(7).date(), -0.9, 600L));
        return list;
    }

    private List<DailyAggregatedDataResponse> createDataWithNullEntry(int days) {
        List<DailyAggregatedDataResponse> list = createStableData(days);
        list.set(4, new DailyAggregatedDataResponse(list.get(4).date(), null, null));
        return list;
    }

    // ========== Tests ==========

    @Test
    void detectAnomalies_shouldReturnNoDataMessage_whenInputIsNull() {
        AnomalyDetectionResponse result = service.detectAnomalies(null);
        assertThat(result.message()).isEqualTo("No data provided for anomaly detection.");
        assertThat(result.anomalies()).isEmpty();
    }

    @Test
    void detectAnomalies_shouldReturnNoDataMessage_whenInputIsEmpty() {
        AnomalyDetectionResponse result = service.detectAnomalies(List.of());
        assertThat(result.message()).isEqualTo("No data provided for anomaly detection.");
        assertThat(result.anomalies()).isEmpty();
    }

    @Test
    void detectAnomalies_shouldReturnInsufficientData_whenLessThanMinDays() {
        List<DailyAggregatedDataResponse> data = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            data.add(new DailyAggregatedDataResponse(LocalDate.now().minusDays(i), 0.5, 100L));
        }
        AnomalyDetectionResponse result = service.detectAnomalies(data);
        assertThat(result.message())
                .isEqualTo("Insufficient data (4 days). Need at least 7 days for anomaly detection.");
        assertThat(result.anomalies()).isEmpty();
    }

    @Test
    void detectAnomalies_shouldReturnNoAnomalies_whenDataIsStable() {
        AnomalyDetectionResponse result = service.detectAnomalies(createStableData(10));
        assertThat(result.message())
                .isEqualTo("No significant anomalies detected in your recent journaling patterns.");
        assertThat(result.anomalies()).isEmpty();
    }

    @Test
    void detectAnomalies_shouldDetectMoodAnomaly() {
        AnomalyDetectionResponse result = service.detectAnomalies(createDataWithMoodDrop(10));
        assertThat(result.message()).isEqualTo("Detected 1 unusual journaling patterns.");
        List<Anomaly> anomalies = result.anomalies();
        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.get(0).type()).contains("mood");
    }

    @Test
    void detectAnomalies_shouldDetectWordsAnomaly() {
        AnomalyDetectionResponse result = service.detectAnomalies(createDataWithWordsSpike(10));
        assertThat(result.message()).isEqualTo("Detected 1 unusual journaling patterns.");
        List<Anomaly> anomalies = result.anomalies();
        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.get(0).type()).contains("words");
    }

    @Test
    void detectAnomalies_shouldDetectBothMoodAndWordsAnomalies() {
        AnomalyDetectionResponse result = service.detectAnomalies(createDataWithBothAnomalies(10));
        assertThat(result.message()).isEqualTo("Detected 1 unusual journaling patterns.");
        List<Anomaly> anomalies = result.anomalies();
        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.get(0).type()).containsExactlyInAnyOrder("mood", "words");
    }

    @Test
    void detectAnomalies_shouldSkipEntriesWithNullMoodOrWords() {
        AnomalyDetectionResponse result = service.detectAnomalies(createDataWithNullEntry(10));
        assertThat(result.message())
                .isEqualTo("No significant anomalies detected in your recent journaling patterns.");
        assertThat(result.anomalies()).isEmpty();
    }

    @Test
    void detectAnomalies_shouldDetectMoodIncrease() {
        List<DailyAggregatedDataResponse> data = createStableData(10);
        data.set(7, new DailyAggregatedDataResponse(
                data.get(7).date(), 0.9, data.get(7).totalWords()));
        AnomalyDetectionResponse result = service.detectAnomalies(data);
        assertThat(result.message()).isEqualTo("Detected 1 unusual journaling patterns.");
        List<Anomaly> anomalies = result.anomalies();
        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.get(0).type()).contains("mood");
        assertThat(anomalies.get(0).message()).contains("higher");
    }

    @Test
    void detectAnomalies_shouldDetectWordsDecrease() {
        List<DailyAggregatedDataResponse> data = createStableData(10);
        data.set(7, new DailyAggregatedDataResponse(
                data.get(7).date(), data.get(7).averageMood(), 10L));
        AnomalyDetectionResponse result = service.detectAnomalies(data);
        assertThat(result.message()).isEqualTo("Detected 1 unusual journaling patterns.");
        List<Anomaly> anomalies = result.anomalies();
        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.get(0).type()).contains("words");
        assertThat(anomalies.get(0).message()).contains("less");
    }

    @Test
    void detectAnomalies_shouldHandleNullsInFirstElement() {
        List<DailyAggregatedDataResponse> data = new ArrayList<>();
        data.add(new DailyAggregatedDataResponse(LocalDate.now().minusDays(10), null, null));
        for (int i = 1; i < 10; i++) {
            data.add(new DailyAggregatedDataResponse(LocalDate.now().minusDays(10 - i), 0.5, 100L));
        }
        AnomalyDetectionResponse result = service.detectAnomalies(data);
        assertThat(result.message())
                .isEqualTo("No significant anomalies detected in your recent journaling patterns.");
        assertThat(result.anomalies()).isEmpty();
    }

    @Test
    void detectAnomalies_shouldHandleMoodPresentButWordsNull() {
        List<DailyAggregatedDataResponse> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (i == 4) {
                data.add(new DailyAggregatedDataResponse(LocalDate.now().minusDays(10 - i), 0.5, null));
            } else {
                data.add(new DailyAggregatedDataResponse(LocalDate.now().minusDays(10 - i), 0.5, 100L));
            }
        }
        AnomalyDetectionResponse result = service.detectAnomalies(data);
        assertThat(result.message())
                .isEqualTo("No significant anomalies detected in your recent journaling patterns.");
        assertThat(result.anomalies()).isEmpty();
    }
}