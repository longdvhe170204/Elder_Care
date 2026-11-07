package com.eldercare.backend.admission.internal.registration.service;

import com.eldercare.backend.admission.api.ResidenceRegistrationStatisticsApi;
import com.eldercare.backend.admission.api.dto.RegistrationSummaryView;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationObligation;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationStatus;
import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationObligationStore;
import com.eldercare.backend.admission.internal.registration.persistence.ResidenceRegistrationStore;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài {@link ResidenceRegistrationStatisticsApi} (FR-011): đếm theo người từ cùng nguồn với danh sách cần làm. */
@Service
class RegistrationStatistics implements ResidenceRegistrationStatisticsApi {

    private final RegistrationObligationStore obligations;
    private final ResidenceRegistrationStore registrations;
    private final RegistrationDeregistration deregistration;
    private final BusinessTime time;

    RegistrationStatistics(
            RegistrationObligationStore obligations,
            ResidenceRegistrationStore registrations,
            RegistrationDeregistration deregistration,
            BusinessTime time) {
        this.obligations = obligations;
        this.registrations = registrations;
        this.deregistration = deregistration;
        this.time = time;
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationSummaryView summary() {
        Instant todayStart = time.today().atStartOfDay(BusinessTime.ZONE).toInstant();
        Set<Long> toCreate = new HashSet<>();
        Set<Long> overdue = new HashSet<>();
        for (RegistrationObligation obligation : obligations.pending()) {
            (obligation.effectiveDueAt().isBefore(todayStart) ? overdue : toCreate).add(obligation.residentId());
        }
        for (ResidenceRegistration renewal : registrations.renewalsToSubmit()) {
            (renewal.dueAt().isBefore(todayStart) ? overdue : toCreate).add(renewal.residentId());
        }
        Set<Long> expired = new HashSet<>();
        for (ResidenceRegistration registration : registrations.confirmedOrExpired()) {
            if (registration.status() == RegistrationStatus.EXPIRED) {
                expired.add(registration.residentId());
            }
        }
        Set<Long> toDeregister = new HashSet<>();
        deregistration.due().forEach(registration -> toDeregister.add(registration.residentId()));
        return new RegistrationSummaryView(toCreate.size(), overdue.size(), expired.size(), toDeregister.size());
    }
}
