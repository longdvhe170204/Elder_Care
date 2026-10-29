package com.eldercare.backend.resident.internal.profile.service;

import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.IdentityView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.profile.domain.Resident;
import com.eldercare.backend.resident.internal.profile.persistence.ResidentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tra cứu chỉ đọc hồ sơ người cao tuổi cho module khác (contracts/module-interfaces.md mục 1.1). */
@Service
@Transactional(readOnly = true)
class ResidentApiImpl implements ResidentApi {

    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final ResidentRepository residents;

    ResidentApiImpl(ResidentRepository residents) {
        this.residents = residents;
    }

    @Override
    public ResidentStatus status(long residentId) {
        return require(residentId).getStatus();
    }

    @Override
    public IdentityView identity(long residentId) {
        Resident r = require(residentId);
        return new IdentityView(
                r.getId(), r.getProfileCode(), r.getFullName(), Optional.ofNullable(r.getPhotoFileId()));
    }

    @Override
    public Optional<Instant> finalStatusAt(long residentId) {
        return residents
                .findById(residentId)
                .filter(r -> FINAL_STATUSES.contains(r.getStatus()))
                .map(Resident::getStatusSince);
    }

    @Override
    public boolean isFinalStatus(long residentId) {
        return finalStatusAt(residentId).isPresent();
    }

    @Override
    public Optional<Gender> gender(long residentId) {
        return residents.findById(residentId).map(Resident::getGender);
    }

    @Override
    public List<Long> byStatus(Set<ResidentStatus> statuses) {
        return statuses.isEmpty() ? List.of() : residents.findIdsByStatusIn(statuses);
    }

    private Resident require(long residentId) {
        return residents.findById(residentId).orElseThrow(NotFoundException::new);
    }
}
