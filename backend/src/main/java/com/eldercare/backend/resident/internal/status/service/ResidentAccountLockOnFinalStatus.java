package com.eldercare.backend.resident.internal.status.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.util.Set;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Người cao tuổi sang trạng thái cuối thì khóa tài khoản người cao tuổi ngay (002 T094, FR-072 (a), BR-M15-23; research
 * A-16): M01 nghe sự kiện của chính nó rồi gọi {@link AccountApi} vì 002 không nghe được sự kiện của M01 mà không tạo
 * vòng M15 ↔ M01. Căn cứ là dòng lịch sử trạng thái; 002 bỏ qua khi tài khoản đã khóa vì kết thúc lưu trú.
 */
@Component
class ResidentAccountLockOnFinalStatus {

    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final AccountApi accounts;

    ResidentAccountLockOnFinalStatus(AccountApi accounts) {
        this.accounts = accounts;
    }

    @ApplicationModuleListener
    void on(ResidentStatusChanged event) {
        if (FINAL_STATUSES.contains(event.to())) {
            accounts.lockResidentOnFinalStatus(
                    event.residentId(), new Basis("BR-M15-23", null, event.historyId(), null));
        }
    }
}
