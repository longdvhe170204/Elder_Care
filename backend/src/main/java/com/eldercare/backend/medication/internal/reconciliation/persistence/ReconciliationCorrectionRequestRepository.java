package com.eldercare.backend.medication.internal.reconciliation.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationCorrectionRequest;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationCorrectionRequestContent;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationCorrectionStatus;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Đề nghị đính chính phiếu trên {@code simple_record} (006 T069, {@code V006_98}); đối tượng cha là phiếu đối chiếu. */
@Repository
@Transactional
public class ReconciliationCorrectionRequestRepository {

    private final SimpleRecordApi rows;

    public ReconciliationCorrectionRequestRepository(SimpleRecordApi rows) {
        this.rows = rows;
    }

    private static ReconciliationCorrectionRequest toDomain(SimpleRecord<ReconciliationCorrectionRequestContent> row) {
        ReconciliationCorrectionRequest request = ReconciliationCorrectionRequest.restore(
                ReconciliationCorrectionStatus.valueOf(row.status()), row.createdAt(), row.content());
        request.restoreRow(row.id(), row.version());
        return request;
    }

    public ReconciliationCorrectionRequest save(ReconciliationCorrectionRequest request) {
        SimpleRecord<ReconciliationCorrectionRequestContent> row = request.getId() == null
                ? rows.create(
                        ReconciliationCorrectionRequestContent.KIND,
                        new NewSimpleRecord<>(
                                null,
                                new EntityRef("M07.RECONCILIATION", request.getReconciliationId()),
                                request.getStatus().name(),
                                request.content(),
                                null,
                                null,
                                null,
                                request.getCreatedById(),
                                false))
                : rows.change(
                                ReconciliationCorrectionRequestContent.KIND,
                                request.getId(),
                                request.getVersion(),
                                new RecordChange<>(request.getStatus().name(), request.content(), null, null, false))
                        .after();
        request.restoreRow(row.id(), row.version());
        return request;
    }

    public Optional<ReconciliationCorrectionRequest> findById(Long id) {
        try {
            return Optional.of(toDomain(rows.require(ReconciliationCorrectionRequestContent.KIND, id)));
        } catch (NotFoundException missing) {
            return Optional.empty();
        }
    }
}
