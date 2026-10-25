package com.cambae.backend.m02.internal.transition.service;

import com.cambae.backend.m01.api.dto.OpenAbsence;
import com.cambae.backend.m01.api.spi.AbsenceApi;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Lượt vắng đang mở cho nhắc quá hạn Tạm vắng của 001 (FR-050; SPI {@link AbsenceApi} do 001 khai báo, M02 cài). Khung:
 * bảng {@code absence} thuộc user story lượt vắng nên tới lúc đó chưa có lượt vắng nào; US lượt vắng hoàn thiện hàm này.
 */
@Component
class AbsenceLookup implements AbsenceApi {

    @Override
    public Optional<OpenAbsence> open(long residentId) {
        return Optional.empty();
    }
}
