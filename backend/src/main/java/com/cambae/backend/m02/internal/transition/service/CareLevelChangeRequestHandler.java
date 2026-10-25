package com.cambae.backend.m02.internal.transition.service;

import com.cambae.backend.m01.api.spi.CareLevelChangeRequestApi;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yêu cầu đổi mức chăm sóc cho 001 (FR-036, FR-036a; SPI {@link CareLevelChangeRequestApi} do 001 khai báo, M02 cài).
 * Khung: yêu cầu thay đổi lưu trú thuộc user story thay đổi nên tới lúc đó chưa có yêu cầu nào đang mở; US thay đổi
 * hoàn thiện (chuyển yêu cầu sang {@code SUPERSEDED} qua {@code chuyenMoRong} của 000).
 */
@Component
class CareLevelChangeRequestHandler implements CareLevelChangeRequestApi {

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Long> supersedeByAssessment(long residentId, long assessmentId) {
        return List.of();
    }

    @Override
    public boolean hasOpenRequest(long residentId) {
        return false;
    }
}
