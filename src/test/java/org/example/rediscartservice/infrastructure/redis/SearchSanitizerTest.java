package org.example.rediscartservice.infrastructure.redis;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SearchSanitizerTest {

    static Stream<org.junit.jupiter.params.provider.Arguments> cases() {
        return Stream.of(
                // null/blank handling
                org.junit.jupiter.params.provider.Arguments.of(null, ""),
                org.junit.jupiter.params.provider.Arguments.of("", ""),
                org.junit.jupiter.params.provider.Arguments.of("   ", ""),
                org.junit.jupiter.params.provider.Arguments.of("  Mug  ", "Mug"),

                // special characters escaping
                org.junit.jupiter.params.provider.Arguments.of("Mug*", "Mug\\*"),
                org.junit.jupiter.params.provider.Arguments.of("Mug?", "Mug\\?"),
                org.junit.jupiter.params.provider.Arguments.of("stoneware (large)", "stoneware \\(large\\)"),
                org.junit.jupiter.params.provider.Arguments.of("summer [2025]", "summer \\[2025\\]"),
                org.junit.jupiter.params.provider.Arguments.of("foo/bar", "foo\\/bar"),
                org.junit.jupiter.params.provider.Arguments.of("key:value", "key\\:value"),
                org.junit.jupiter.params.provider.Arguments.of("say \"hi\"", "say \\\"hi\\\""),
                org.junit.jupiter.params.provider.Arguments.of("path\\to", "path\\\\to"),
                org.junit.jupiter.params.provider.Arguments.of("{json}", "\\{json\\}"),
                org.junit.jupiter.params.provider.Arguments.of("wave~form", "wave\\~form"),
                org.junit.jupiter.params.provider.Arguments.of("dash-like-this", "dash\\-like\\-this")
        );
    }

    @ParameterizedTest
    @MethodSource("cases")
    void sanitize_behaves_as_expected(String input, String expected) {
        assertThat(SearchSanitizer.sanitize(input)).isEqualTo(expected);
    }
}