package org.example.rediscartservice.infrastructure.redis;

/**
 * Minimal RediSearch query sanitizer:
 * - null-safe trimming (null -> "", "  x  " -> "x")
 * - escapes characters with special meaning in RediSearch query syntax.
 */
public final class SearchSanitizer {

    private SearchSanitizer() {}

    public static String sanitize(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        // Escape: - [ ] { } ( ) " ~ * ? : \ /
        return trimmed.replaceAll("([\\-\\[\\]\\{\\}\\(\\)\"~*?:\\\\/])", "\\\\$1");
    }
}
