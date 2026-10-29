package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.notification.internal.delivery.domain.OutOfAppContent;
import com.eldercare.backend.notification.internal.delivery.domain.SmsBatching;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * FR-016b, FR-020 (009 T023, tên logic {@code KhungGopTest}): khung gộp cố định, không có khung mở thì gửi ngay và mở
 * khung; nội dung rút gọn có khóa thuộc danh mục trường sức khỏe thì không hợp lệ ở kênh ngoài.
 */
class SmsBatchingTest {

    private static final Instant NOW = Instant.parse("2030-03-04T03:00:00Z");

    @Test
    void FR_016b_noOpenWindow_sendNow_openWindow_batches() {
        assertThat(SmsBatching.batch(Optional.empty(), NOW)).isFalse();
        assertThat(SmsBatching.batch(Optional.of(NOW.plusSeconds(60)), NOW)).isTrue();
        assertThat(SmsBatching.batch(Optional.of(NOW), NOW)).isFalse();
    }

    @Test
    void FR_016b_digest_countOnly_noResidentName() {
        assertThat(SmsBatching.digest(3)).contains("3 thông báo mới").doesNotContain("Nguyễn");
    }

    @Test
    void FR_016b_staffDigest_listsFloorsAndShifts_withoutPlacesSameAsCountOnly() {
        assertThat(SmsBatching.digest(2, List.of("Tầng 2, ca 07:00–15:00", "Tầng 3")))
                .isEqualTo("Bạn có 2 thông báo mới (Tầng 2, ca 07:00–15:00; Tầng 3). Vui lòng mở ứng dụng để xem.");
        assertThat(SmsBatching.digest(2, List.of())).isEqualTo(SmsBatching.digest(2));
    }

    @Test
    void FR_020_healthKeyOrMissingShortContent_invalid() {
        assertThat(OutOfAppContent.invalid(Map.of("room", "101"), Optional.of("Có thông báo mới")))
                .isFalse();
        assertThat(OutOfAppContent.invalid(Map.of("diagnosis", "x"), Optional.of("Có thông báo mới")))
                .isTrue();
        assertThat(OutOfAppContent.invalid(Map.of(), Optional.empty())).isTrue();
        assertThat(OutOfAppContent.invalid(null, Optional.of(" "))).isTrue();
    }
}
