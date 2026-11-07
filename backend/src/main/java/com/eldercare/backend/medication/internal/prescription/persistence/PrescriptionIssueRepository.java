package com.eldercare.backend.medication.internal.prescription.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionIssue;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionIssueContent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Vấn đề của đơn (FR-007) trên {@code record_event}, ghi cùng lệnh lập đơn (006 T069, {@code V006_98}). */
@Repository
@Transactional
public class PrescriptionIssueRepository {

    private final RecordEventApi events;

    public PrescriptionIssueRepository(RecordEventApi events) {
        this.events = events;
    }

    public List<PrescriptionIssue> findByPrescriptionId(Long prescriptionId) {
        return events.list(PrescriptionIssueContent.KIND, new EntityRef("M07.PRESCRIPTION", prescriptionId)).stream()
                .map(e -> e.content().toDomain(e.id()))
                .toList();
    }

    public List<PrescriptionIssue> saveAll(Iterable<PrescriptionIssue> issues) {
        List<PrescriptionIssue> saved = new ArrayList<>();
        for (PrescriptionIssue issue : issues) {
            RecordEvent<PrescriptionIssueContent> event = events.add(
                    PrescriptionIssueContent.KIND,
                    new EntityRef("M07.PRESCRIPTION", issue.getPrescriptionId()),
                    new NewRecordEvent<>(
                            PrescriptionIssueContent.of(issue),
                            null,
                            issue.getConfirmedById(),
                            false,
                            issue.getOccurredAt(),
                            null,
                            issue.getOccurredAt(),
                            null,
                            null));
            issue.assignId(event.id());
            saved.add(issue);
        }
        return saved;
    }
}
