package com.eldercare.backend.notification.internal.quiethours.service;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Người cao tuổi sang Qua đời ({@code NguoiCaoTuoiDaChuyenTrangThai(GHI_NHAN_QUA_DOI)}; FR-015, FR-025, research B-06):
 * thông báo người thân đang giữ về người đó chuyển Không gửi ngay. Việc giải phóng vẫn kiểm lại nên chạy lại không sao.
 */
@Component
class ResidentDeathListener {

    private final QuietHoursRelease release;

    ResidentDeathListener(QuietHoursRelease release) {
        this.release = release;
    }

    @ApplicationModuleListener
    void on(ResidentStatusChanged event) {
        if (event.to() == ResidentStatus.DECEASED) {
            release.cancelForDeath(event.residentId());
        }
    }
}
