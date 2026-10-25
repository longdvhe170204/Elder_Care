package com.cambae.backend.m03.internal.allocation.service;

import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m03.api.dto.GenderPolicy;
import com.cambae.backend.m03.api.spi.AllocationResidencySource;
import com.cambae.backend.m03.api.spi.CareLevelCode;
import com.cambae.backend.m03.internal.allocation.domain.CareLevelSource;
import com.cambae.backend.m03.internal.allocation.domain.CheckedCareLevel;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Dữ liệu M01 qua API; dùng chung FR-006 và FR-018, không đọc bảng module khác. */
@Service
public class ResidentAllocationFacts {
    private final ResidentApi residents;
    private final CareLevelApi levels;
    private final ObjectProvider<AllocationResidencySource> residency;

    public ResidentAllocationFacts(
            ResidentApi residents, CareLevelApi levels, ObjectProvider<AllocationResidencySource> residency) {
        this.residents = residents;
        this.levels = levels;
        this.residency = residency;
    }

    public Optional<CheckedCareLevel> careLevel(long residentId) {
        var current = levels.current(residentId);
        if (current.isPresent()) {
            CareLevelCode level = switch (current.get()) {
                case BASIC -> CareLevelCode.BASIC;
                case REGULAR -> CareLevelCode.FREQUENT;
                case SPECIAL -> CareLevelCode.SPECIAL;
            };
            return Optional.of(new CheckedCareLevel(level, CareLevelSource.CURRENT));
        }
        if (residents.status(residentId) != ResidentStatus.ADMISSION_IN_PROGRESS) return Optional.empty();
        var source = residency.getIfAvailable();
        if (source == null) return Optional.empty();
        return source.careLevelFromContract(residentId)
                .map(level -> new CheckedCareLevel(level, CareLevelSource.CONTRACT))
                .or(() -> source.careLevelFromRegistration(residentId)
                        .map(level -> new CheckedCareLevel(level, CareLevelSource.REGISTRATION)));
    }

    public boolean genderAllowed(long residentId, GenderPolicy policy) {
        if (policy == GenderPolicy.UNRESTRICTED) return true;
        return residents
                .gender(residentId)
                .map(gender -> gender.name().equals(policy.name()))
                .orElse(false);
    }
}
