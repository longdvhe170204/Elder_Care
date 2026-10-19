package com.cambae.backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.VersionMismatchException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FR-021, FR-040 (000 T018): ETag là số phiên bản; If-Match cũ thì 412 kèm giá trị hiện hành. */
class VersionEtagTest {

    @Test
    void FR_021_etag_isQuotedVersion() {
        assertThat(VersionEtag.of(3)).isEqualTo("\"3\"");
    }

    @Test
    void FR_021_matchingIfMatch_passes() {
        assertThatCode(() -> VersionEtag.requireMatch("\"3\"", 3, Map.of())).doesNotThrowAnyException();
        assertThatCode(() -> VersionEtag.requireMatch("W/\"3\"", 3, Map.of())).doesNotThrowAnyException();
        assertThatCode(() -> VersionEtag.requireMatch("3", 3, Map.of())).doesNotThrowAnyException();
    }

    @Test
    void FR_021_staleIfMatch_throwsWithCurrentValue() {
        Map<String, Object> current = Map.of("currentValue", 20);

        assertThatThrownBy(() -> VersionEtag.requireMatch("\"2\"", 3, current))
                .isInstanceOfSatisfying(
                        VersionMismatchException.class,
                        e -> assertThat(e.currentValue()).isEqualTo(current));
    }

    @Test
    void FR_021_unreadableIfMatch_isTreatedAsStale() {
        assertThatThrownBy(() -> VersionEtag.requireMatch("abc", 3, Map.of()))
                .isInstanceOf(VersionMismatchException.class);
    }
}
