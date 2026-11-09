package com.eldercare.backend.medication.internal.prescription.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionBasis;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionBasisContent;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Căn cứ cơ sở kê (BR-M07-16) trên {@code record_event}; ghi thêm khi gia hạn, thay thế (006 T069, {@code V006_98}). */
@Repository
@Transactional
public class PrescriptionBasisRepository {

    private final RecordEventApi events;

    public PrescriptionBasisRepository(RecordEventApi events) {
        this.events = events;
    }

    public List<PrescriptionBasis> findByPrescriptionId(Long prescriptionId) {
        return events.list(PrescriptionBasisContent.KIND, new EntityRef("M07.PRESCRIPTION", prescriptionId)).stream()
                .map(e -> e.content().toDomain(e.id()))
                .toList();
    }

    public PrescriptionBasis save(PrescriptionBasis basis) {
        RecordEvent<PrescriptionBasisContent> event = events.add(
                PrescriptionBasisContent.KIND,
                new EntityRef("M07.PRESCRIPTION", basis.getPrescriptionId()),
                new NewRecordEvent<>(
                        PrescriptionBasisContent.of(basis),
                        null,
                        basis.getRecorderId(),
                        false,
                        basis.getRecordedAt(),
                        null,
                        basis.getRecordedAt(),
                        null,
                        null));
        basis.assignId(event.id());
        return basis;
    }
}
