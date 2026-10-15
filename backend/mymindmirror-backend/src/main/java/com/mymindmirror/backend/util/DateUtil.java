package com.mymindmirror.backend.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Centralised utility for parsing date strings into LocalDate.
 * All methods are static – no dependency injection needed.
 */
@Slf4j
public final class DateUtil {

    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);

    private DateUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Parses a date string in yyyy-MM-dd format.
     * Returns null if input is null or blank.
     *
     * @param dateStr the date string (nullable)
     * @return the parsed LocalDate, or null if input is null/blank
     * @throws IllegalArgumentException if the format is invalid
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format: '{}'", dateStr);
            throw new IllegalArgumentException("Invalid date format: '" + dateStr + "'. Use yyyy-MM-dd.");
        }
    }

    /**
     * Parses a date or returns a fallback if input is null/blank.
     */
    private static LocalDate parseDateOrDefault(String dateStr, LocalDate fallback) {
        LocalDate parsed = parseDate(dateStr);
        return parsed != null ? parsed : fallback;
    }

    /**
     * Validates that start is not after end.
     */
    private static void validateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date (" + start + ") cannot be after end date (" + end + ").");
        }
    }

    /**
     * Parses start and end dates with a default range (now - N days to now).
     * If start is null, uses now().minusDays(defaultDaysBack).
     * If end is null, uses now().
     *
     * @param startStr        start date string (nullable)
     * @param endStr          end date string (nullable)
     * @param defaultDaysBack number of days to go back from today if startStr is null
     * @return DateRange with start and end (never null)
     * @throws IllegalArgumentException if start > end
     */
    public static DateRange parseDateRange(String startStr, String endStr, int defaultDaysBack) {
        LocalDate end = parseDateOrDefault(endStr, LocalDate.now());
        LocalDate start = parseDateOrDefault(startStr, LocalDate.now().minusDays(defaultDaysBack));
        validateRange(start, end);
        return new DateRange(start, end);
    }

    /**
     * Specialised method for /history endpoint.
     * If both start and end are null/blank → all time (1900-01-01 to 2100-12-31).
     * If only start is provided → end = now().
     * If only end is provided → start = now().minusDays(30).
     * Empty strings are treated the same as null.
     *
     * @param startStr start date string (nullable)
     * @param endStr   end date string (nullable)
     * @return DateRange with start and end (never null)
     * @throws IllegalArgumentException if dates are invalid or start > end
     */
    public static DateRange parseHistoryDateRange(String startStr, String endStr) {
        boolean startBlank = startStr == null || startStr.isBlank();
        boolean endBlank = endStr == null || endStr.isBlank();

        if (startBlank && endBlank) {
            return new DateRange(MIN_DATE, MAX_DATE);
        }

        LocalDate start = startBlank ? LocalDate.now().minusDays(30) : parseDate(startStr);
        LocalDate end = endBlank ? LocalDate.now() : parseDate(endStr);
        validateRange(start, end);
        return new DateRange(start, end);
    }

    /**
     * Strict parser for endpoints where both dates are required.
     * Throws IllegalArgumentException if either date is null/blank or if start > end.
     *
     * @param startStr start date string (required)
     * @param endStr   end date string (required)
     * @return DateRange with start and end (never null)
     * @throws IllegalArgumentException if dates are missing, invalid, or out of order
     */
    public static DateRange parseStrictDateRange(String startStr, String endStr) {
        if (startStr == null || startStr.isBlank()) {
            throw new IllegalArgumentException("Start date is required.");
        }
        if (endStr == null || endStr.isBlank()) {
            throw new IllegalArgumentException("End date is required.");
        }
        LocalDate start = parseDate(startStr);
        LocalDate end = parseDate(endStr);
        validateRange(start, end);
        return new DateRange(start, end);
    }
}