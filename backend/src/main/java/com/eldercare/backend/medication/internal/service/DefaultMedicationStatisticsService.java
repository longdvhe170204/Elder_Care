package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.medication.api.MedicationStatisticsApi;
import com.eldercare.backend.medication.api.dto.DoseReportView;
import com.eldercare.backend.medication.api.dto.DoseStatus;
import com.eldercare.backend.medication.api.dto.ReconciliationReportView;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import com.eldercare.backend.medication.internal.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationReconciliationRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultMedicationStatisticsService implements MedicationStatisticsApi {

    private final MedicationDoseRepository doseRepo;
    private final MedicationReconciliationRepository reconciliationRepo;

    public DefaultMedicationStatisticsService(
            MedicationDoseRepository doseRepo, MedicationReconciliationRepository reconciliationRepo) {
        this.doseRepo = doseRepo;
        this.reconciliationRepo = reconciliationRepo;
    }

    @Override
    public Stream<DoseReportView> dosesInPeriod(ScopeFilter scopeFilter, Instant from, Instant to) {
        List<MedicationDose> doses = doseRepo.findAll();
        return doses.stream()
                .filter(d -> d.getScheduledTime() != null
                        && !d.getScheduledTime().isBefore(from)
                        && !d.getScheduledTime().isAfter(to))
                .map(d -> {
                    DoseStatus apiStatus;
                    try {
                        apiStatus = DoseStatus.valueOf(d.getStatus().name());
                    } catch (Exception e) {
                        apiStatus = DoseStatus.SCHEDULED;
                    }

                    boolean floorShared =
                            d.getClosureRule() != null && d.getClosureRule().contains("CHUNG_TANG");
                    boolean dispensed = d.getStatus()
                            == com.eldercare.backend.medication.internal.domain.DoseStatus.LEAVE_MEDICATION;
                    boolean late = d.getLateAt() != null;
                    boolean offline = false;
                    String doseKey = d.getPrescriptionId() + "-" + d.getId();

                    return new DoseReportView(
                            doseKey,
                            d.getResidentId(),
                            d.getFloorAreaId() != null ? d.getFloorAreaId() : 0L,
                            d.getScheduledTime(),
                            apiStatus,
                            d.getClosedById(),
                            d.getClosureRule(),
                            floorShared,
                            dispensed,
                            late,
                            offline,
                            null);
                });
    }

    @Override
    public Stream<ReconciliationReportView> reconciliationsInPeriod(ScopeFilter scopeFilter, Instant from, Instant to) {
        List<MedicationReconciliation> list = reconciliationRepo.findAll();
        return list.stream()
                .filter(r -> r.getCreatedAt() != null
                        && !r.getCreatedAt().isBefore(from)
                        && !r.getCreatedAt().isAfter(to))
                .map(r -> new ReconciliationReportView(
                        r.getId(),
                        r.getResidentId(),
                        r.getReason().name(),
                        r.getStatus().name(),
                        r.getDeadlineAt(),
                        r.getOverdueAt() != null));
    }
}
