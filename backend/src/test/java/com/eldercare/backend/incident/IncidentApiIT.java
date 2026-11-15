package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.dto.IncidentView;
import com.eldercare.backend.incident.internal.incident.service.DefaultIncidentApi;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bản thật {@code IncidentApi} (module-interfaces 007 mục 1.7; 013 FR-021, FR-025): loại hiện hành, mức, trạng thái theo
 * enum {@code IncidentStatus} của api-007 (Đã hủy là {@code VOIDED}), lọc theo người và khoảng nửa mở, còn mục mở.
 */
class IncidentApiIT extends IncidentHttpTestBase {
    @Autowired
    DefaultIncidentApi api;

    @Test
    void FR_021_viewAndByResidentExposeCurrentTypeStatusAndVoided() {
        long open = incident("OTHER_INJURY", "MEDIUM", "NEW", null, nurse.accountId());
        long cancelled = incident("OTHER_INJURY", "LOW", "CANCELLED", null, nurse.accountId());

        var view = api.view(open).orElseThrow();
        assertThat(view.residentId()).isEqualTo(resident);
        assertThat(view.type()).isEqualTo("OTHER_INJURY");
        assertThat(view.level()).isEqualTo("MEDIUM");
        assertThat(view.status()).isEqualTo("NEW");
        assertThat(view.floorAreaId()).isEqualTo(floor);
        assertThat(view.assigneeStaffId()).isNull();
        assertThat(api.view(cancelled).orElseThrow().status()).isEqualTo("VOIDED");
        assertThat(api.view(-1)).isEmpty();

        // 013 FR-011a: người xử lý sự cố là mã nhân viên của tài khoản được giao (D chốt "Đổi api" 2026-11-08).
        long handled = incident("OTHER_INJURY", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        assertThat(api.view(handled).orElseThrow().assigneeStaffId()).isEqualTo(nurse.staffId());

        var from = time.now().minus(Duration.ofMinutes(1));
        var to = time.now().plus(Duration.ofMinutes(1));
        assertThat(api.byResident(resident, from, to, true))
                .extracting(IncidentView::id)
                .containsExactlyInAnyOrder(open, handled);
        assertThat(api.byResident(resident, from, to, false))
                .extracting(IncidentView::id)
                .containsExactlyInAnyOrder(open, cancelled, handled);
        assertThat(api.byResident(resident, to, to.plusSeconds(60), false)).isEmpty();
        assertThat(api.hasOpenItem(resident)).isTrue();
        var flag = api.monitoringFlag(resident, time.now());
        assertThat(flag.suspectedInfection()).isFalse();
        assertThat(flag.postFall()).isFalse();
    }
}
