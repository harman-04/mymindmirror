package com.mymindmirror.backend.util;

import java.time.LocalDate;

/**
 * Immutable container for a date range.
 */
public record DateRange(LocalDate start, LocalDate end) {}