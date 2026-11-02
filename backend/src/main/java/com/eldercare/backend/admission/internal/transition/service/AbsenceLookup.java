package com.eldercare.backend.admission.internal.transition.service;

import com.eldercare.backend.admission.internal.absence.service.AbsenceService;
import com.eldercare.backend.resident.api.dto.OpenAbsence;
import com.eldercare.backend.resident.api.spi.AbsenceApi;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Lượt Tạm vắng đang mở cho nhắc quá hạn của 001 (FR-050; SPI {@link AbsenceApi} do 001 khai báo, M02 cài). */
@Component
class AbsenceLookup implements AbsenceApi {

    private final AbsenceService absences;

    AbsenceLookup(AbsenceService absences) {
        this.absences = absences;
    }

    @Override
    public Optional<OpenAbsence> open(long residentId) {
        return absences.openTemporary(residentId)
                .map(absence -> new OpenAbsence(absence.id(), absence.expectedReturnAt()));
    }
}
