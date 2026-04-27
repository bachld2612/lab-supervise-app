package com.bachld.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Util {

    private static final DateTimeFormatter OUTPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Formats a date string to dd/MM/yyyy.
     * Accepts: yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy.
     * Returns the original string if parsing fails.
     */
    public static String formatDate(String date) {
        if (date == null || date.isBlank()) return "";

        DateTimeFormatter[] inputs = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        for (DateTimeFormatter fmt : inputs) {
            try {
                return LocalDate.parse(date.trim(), fmt).format(OUTPUT);
            } catch (DateTimeParseException ignored) {
            }
        }

        return date;
    }
}