package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import org.junit.jupiter.api.Test;

/**
 * FR-032, research S-09 (006 → 007): nguồn của cảnh báo "chưa đối chiếu thuốc" đã được xử lý thì cảnh báo đang mở của
 * người đó Đã đóng, người đóng Hệ thống; không có cảnh báo mở hoặc cảnh báo đã đóng thì không làm gì; cảnh báo khác khóa
 * loại không bị đóng.
 */
class BR_M07_CloseBySystemIT extends AlertRequestTestBase {

    @Test
    void FR_032_openAlertOfTheLockIsClosedBySystemAndOthersAreKept() {
        String lock = TypeLock.forMedication("CHUA_DOI_CHIEU", resident);
        var unreconciled = request(lock, IncidentLevel.MEDIUM, T0, resident);
        var other = request(TypeLock.forMedication("BO_LO_LIEU", 5), IncidentLevel.MEDIUM, T0, 5);

        Boolean closed = transaction.execute(
                status -> service.closeBySystem(resident, lock, "Nguồn đã được xử lý: phiếu đã xác nhận", T0));

        assertThat(closed).isTrue();
        var stored = alerts.findById(unreconciled.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(AlertStatus.CLOSED);
        assertThat(stored.closedBySystem()).isTrue();
        assertThat(stored.closedById()).isNull();
        assertThat(stored.resolution()).startsWith("Nguồn đã được xử lý");
        assertThat(alerts.findById(other.id()).orElseThrow().status()).isNotEqualTo(AlertStatus.CLOSED);
        Boolean again = transaction.execute(status -> service.closeBySystem(resident, lock, "x", T0));
        assertThat(again).as("đã đóng thì không đóng lần nữa").isFalse();
    }
}
