package com.eldercare.backend.medication.internal.reconciliation.service;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.events.AdmissionMedicationChanged;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationReason;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationStatus;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.resident.api.events.InitialHealthRecordChanged;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Nghe {@code resident.api.events.InitialHealthRecordChanged} (FR-034a, BR-M07-22): báo người đã lập, nộp hoặc xác nhận
 * phiếu đối chiếu "tiếp nhận" của người đó rằng thuốc đang dùng khi tiếp nhận đã đổi. Không đổi phiếu; dấu "thuốc đã bỏ"
 * tính lúc đọc. Phiếu đã hủy không báo.
 */
@Component
public class InitialHealthRecordListener {

    private final MedicationReconciliationRepository tickets;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public InitialHealthRecordListener(
            MedicationReconciliationRepository tickets, ApplicationEventPublisher events, BusinessTime time) {
        this.tickets = tickets;
        this.events = events;
        this.time = time;
    }

    /** 001 phát sau khi lệnh commit; lỗi ở đây không hủy lệnh của 001. */
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInitialHealthRecordChanged(InitialHealthRecordChanged event) {
        for (MedicationReconciliation ticket : tickets.findByResidentId(event.residentId())) {
            if (ticket.getReason() != ReconciliationReason.ADMISSION
                    || ticket.getStatus() == ReconciliationStatus.CANCELLED) {
                continue;
            }
            Set<Long> staff = new LinkedHashSet<>();
            staff.add(ticket.getCreatedById());
            if (ticket.getStatus() != ReconciliationStatus.OPEN) {
                staff.add(ticket.getSubmittedById());
                staff.add(ticket.getConfirmedById());
            }
            List<Long> staffIds = staff.stream().filter(Objects::nonNull).toList();
            events.publishEvent(
                    new AdmissionMedicationChanged(ticket.getId(), event.residentId(), staffIds, time.now()));
        }
    }
}
