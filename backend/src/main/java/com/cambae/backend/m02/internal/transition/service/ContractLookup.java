package com.cambae.backend.m02.internal.transition.service;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidenceTypePeriod;
import com.cambae.backend.m01.api.spi.ContractApi;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Loại lưu trú theo hợp đồng cho 001 (FR-009, FR-052, FR-053; SPI {@link ContractApi} do 001 khai báo, M02 cài). Khung:
 * bảng {@code contract} thuộc user story hợp đồng nên tới lúc đó chưa có loại lưu trú nào; US hợp đồng hoàn thiện.
 */
@Component
class ContractLookup implements ContractApi {

    @Override
    public Optional<ResidenceType> currentResidenceType(long residentId, LocalDate date) {
        return Optional.empty();
    }

    @Override
    public List<ResidenceTypePeriod> residenceTypeHistory(long residentId) {
        return List.of();
    }
}
