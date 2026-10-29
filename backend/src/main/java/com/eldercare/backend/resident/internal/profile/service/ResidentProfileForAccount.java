package com.eldercare.backend.resident.internal.profile.service;

import com.eldercare.backend.administration.account.api.spi.ResidentProfileApi;
import com.eldercare.backend.administration.account.api.spi.ResidentProfileView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.profile.persistence.ResidentRepository;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản cài SPI {@link ResidentProfileApi} của 002 (tài khoản người cao tuổi; 002 T091, FR-067, FR-073): họ tên, Đang lưu
 * trú, trạng thái cuối. 002 khai báo SPI để tránh vòng M15 ↔ M01.
 */
@Service
@Transactional(readOnly = true)
class ResidentProfileForAccount implements ResidentProfileApi {

    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final ResidentRepository residents;

    ResidentProfileForAccount(ResidentRepository residents) {
        this.residents = residents;
    }

    @Override
    public Optional<ResidentProfileView> profile(long residentId) {
        return residents
                .findById(residentId)
                .map(r -> new ResidentProfileView(
                        r.getFullName(),
                        r.getStatus() == ResidentStatus.IN_RESIDENCE,
                        FINAL_STATUSES.contains(r.getStatus())));
    }
}
