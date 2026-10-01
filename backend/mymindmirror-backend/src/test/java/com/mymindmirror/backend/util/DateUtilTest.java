package com.mymindmirror.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateUtilTest {

    private static final LocalDate TODAY = LocalDate.now();

    // ============================================================
    // parseDate()
    // ============================================================

    @Nested
    @DisplayName("parseDate()")
    class ParseDateTests {

        @Test
        @DisplayName("should parse valid yyyy-MM-dd string")
        void shouldParseValidDate() {
            assertThat(DateUtil.parseDate("2026-01-15")).isEqualTo(LocalDate.of(2026, 1, 15));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("should return null for null or blank input")
        void shouldReturnNullForBlankInput(String input) {
            assertThat(DateUtil.parseDate(input)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "15-01-2026",
                "2026/01/15",
                "not-a-date",
                "2026-13-01",
                "2026-02-30"
        })
        @DisplayName("should throw IllegalArgumentException for invalid format")
        void shouldThrowForInvalidFormat(String invalid) {
            assertThatThrownBy(() -> DateUtil.parseDate(invalid))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid date format");
        }
    }

    // ============================================================
    // parseDateRange()
    // ============================================================

    @Nested
    @DisplayName("parseDateRange()")
    class ParseDateRangeTests {

        @Test
        @DisplayName("should return custom range when both dates provided")
        void shouldUseBothCustomDates() {
            DateRange range = DateUtil.parseDateRange("2026-01-01", "2026-01-31", 30);

            assertThat(range.start()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(range.end()).isEqualTo(LocalDate.of(2026, 1, 31));
        }

        @Test
        @DisplayName("should use defaultDaysBack for start when start is null")
        void shouldUseDefaultDaysBackForStart() {
            // Use an end date that is AFTER now - 30 days
            String end = TODAY.plusDays(1).toString();
            DateRange range = DateUtil.parseDateRange(null, end, 30);

            assertThat(range.start()).isEqualTo(TODAY.minusDays(30));
            assertThat(range.end()).isEqualTo(TODAY.plusDays(1));
        }

        @Test
        @DisplayName("should use today for end when end is null")
        void shouldUseTodayForEnd() {
            DateRange range = DateUtil.parseDateRange("2026-01-01", null, 30);

            assertThat(range.start()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(range.end()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("should use both defaults when both null")
        void shouldUseBothDefaults() {
            DateRange range = DateUtil.parseDateRange(null, null, 7);

            assertThat(range.start()).isEqualTo(TODAY.minusDays(7));
            assertThat(range.end()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("should treat blank strings as null")
        void shouldTreatBlankAsNull() {
            DateRange range = DateUtil.parseDateRange("   ", "", 14);

            assertThat(range.start()).isEqualTo(TODAY.minusDays(14));
            assertThat(range.end()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("should throw when start > end")
        void shouldThrowWhenStartAfterEnd() {
            assertThatThrownBy(() -> DateUtil.parseDateRange("2026-12-31", "2026-01-01", 30))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be after");
        }

        @Test
        @DisplayName("should allow start == end")
        void shouldAllowSameDay() {
            DateRange range = DateUtil.parseDateRange("2026-01-15", "2026-01-15", 30);

            assertThat(range.start()).isEqualTo(range.end());
        }
    }

    // ============================================================
    // parseHistoryDateRange()
    // ============================================================

    @Nested
    @DisplayName("parseHistoryDateRange()")
    class ParseHistoryDateRangeTests {

        @Test
        @DisplayName("should return ALL TIME range when both null (1900 → 2100)")
        void shouldReturnAllTimeWhenBothNull() {
            DateRange range = DateUtil.parseHistoryDateRange(null, null);

            assertThat(range.start()).isEqualTo(LocalDate.of(1900, 1, 1));
            assertThat(range.end()).isEqualTo(LocalDate.of(2100, 12, 31));
        }

        @Test
        @DisplayName("should use default 30 days for start when only start is null")
        void shouldUseDefaultForNullStart() {
            // Use an end date in the future relative to TODAY - 30 days
            String end = TODAY.plusDays(1).toString();
            DateRange range = DateUtil.parseHistoryDateRange(null, end);

            assertThat(range.start()).isEqualTo(TODAY.minusDays(30));
            assertThat(range.end()).isEqualTo(TODAY.plusDays(1));
        }

        @Test
        @DisplayName("should use today for end when only end is null")
        void shouldUseTodayForNullEnd() {
            DateRange range = DateUtil.parseHistoryDateRange("2026-01-01", null);

            assertThat(range.start()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(range.end()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("should use custom range when both provided")
        void shouldUseCustomRange() {
            DateRange range = DateUtil.parseHistoryDateRange("2026-01-01", "2026-06-30");

            assertThat(range.start()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(range.end()).isEqualTo(LocalDate.of(2026, 6, 30));
        }

        @Test
        @DisplayName("should treat blank strings as null (fallback to all time)")
        void shouldTreatBlankAsAllTime() {
            DateRange range = DateUtil.parseHistoryDateRange("   ", "");

            assertThat(range.start()).isEqualTo(LocalDate.of(1900, 1, 1));
            assertThat(range.end()).isEqualTo(LocalDate.of(2100, 12, 31));
        }

        @Test
        @DisplayName("should throw when start > end")
        void shouldThrowWhenStartAfterEnd() {
            assertThatThrownBy(() -> DateUtil.parseHistoryDateRange("2026-12-31", "2026-01-01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be after");
        }
    }

    // ============================================================
    // parseStrictDateRange()
    // ============================================================

    @Nested
    @DisplayName("parseStrictDateRange()")
    class ParseStrictDateRangeTests {

        @Test
        @DisplayName("should parse both required dates")
        void shouldParseBothDates() {
            DateRange range = DateUtil.parseStrictDateRange("2026-01-01", "2026-12-31");

            assertThat(range.start()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(range.end()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("should throw when start is null or blank")
        void shouldThrowWhenStartMissing(String start) {
            assertThatThrownBy(() -> DateUtil.parseStrictDateRange(start, "2026-12-31"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Start date is required");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("should throw when end is null or blank")
        void shouldThrowWhenEndMissing(String end) {
            assertThatThrownBy(() -> DateUtil.parseStrictDateRange("2026-01-01", end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date is required");
        }

        @Test
        @DisplayName("should throw when start > end")
        void shouldThrowWhenStartAfterEnd() {
            assertThatThrownBy(() -> DateUtil.parseStrictDateRange("2026-12-31", "2026-01-01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be after");
        }

        @Test
        @DisplayName("should throw for invalid format")
        void shouldThrowForInvalidFormat() {
            assertThatThrownBy(() -> DateUtil.parseStrictDateRange("not-a-date", "2026-01-01"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid date format");
        }
    }

    // ============================================================
    // DateRange record
    // ============================================================

    @Nested
    @DisplayName("DateRange record")
    class DateRangeTests {

        @Test
        @DisplayName("should store start and end values")
        void shouldStoreStartAndEnd() {
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 12, 31);
            DateRange range = new DateRange(start, end);

            assertThat(range.start()).isEqualTo(start);
            assertThat(range.end()).isEqualTo(end);
        }

        @Test
        @DisplayName("should have working equals & hashCode")
        void shouldSupportEqualsAndHashCode() {
            DateRange a = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
            DateRange b = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }
}