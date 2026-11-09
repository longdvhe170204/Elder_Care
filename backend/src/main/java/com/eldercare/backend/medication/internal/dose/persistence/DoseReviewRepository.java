package com.eldercare.backend.medication.internal.dose.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.medication.internal.dose.domain.DoseReview;
import com.eldercare.backend.medication.internal.dose.domain.DoseReviewContent;
import com.eldercare.backend.medication.internal.dose.domain.DoseReviewStatus;
import com.eldercare.backend.medication.internal.governance.persistence.RecordActor;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản ghi chờ xem lại liều trên {@code simple_record} loại {@code M07.DOSE_REVIEW} (006 T069, {@code V006_98}). Một bản
 * ghi mỗi kết quả liều qua {@code unique_key} {@code M07.DR:<doseResultId>} (thay {@code UNIQUE (dose_result_id)}).
 */
@Repository
@Transactional
public class DoseReviewRepository {

    private final SimpleRecordApi rows;
    private final RecordActor actor;

    public DoseReviewRepository(SimpleRecordApi rows, RecordActor actor) {
        this.rows = rows;
        this.actor = actor;
    }

    private static String key(DoseReview review) {
        return review.getDoseResultId() == null ? null : "M07.DR:" + review.getDoseResultId();
    }

    private static EntityRef parent(DoseReview review) {
        if (review.getDoseResultId() != null) {
            return new EntityRef("M07.DOSE_RESULT", review.getDoseResultId());
        }
        return review.getPrnUsageId() == null ? null : new EntityRef("M07.PRN_ADMINISTRATION", review.getPrnUsageId());
    }

    private static DoseReview toDomain(SimpleRecord<DoseReviewContent> row) {
        DoseReview review = DoseReview.restore(DoseReviewStatus.valueOf(row.status()), row.content());
        review.restoreRow(row.id(), row.version(), row.createdAt());
        return review;
    }

    public DoseReview save(DoseReview review) {
        SimpleRecord<DoseReviewContent> row = review.getId() == null
                ? rows.create(
                        DoseReviewContent.KIND,
                        new NewSimpleRecord<>(
                                null,
                                parent(review),
                                review.getStatus().name(),
                                review.content(),
                                null,
                                null,
                                key(review),
                                actor.accountId(),
                                false))
                : rows.change(
                                DoseReviewContent.KIND,
                                review.getId(),
                                review.getVersion(),
                                new RecordChange<>(
                                        review.getStatus().name(), review.content(), null, key(review), false))
                        .after();
        review.restoreRow(row.id(), row.version(), row.createdAt());
        return review;
    }

    public Optional<DoseReview> findById(Long id) {
        try {
            return Optional.of(toDomain(rows.require(DoseReviewContent.KIND, id)));
        } catch (NotFoundException missing) {
            return Optional.empty();
        }
    }

    public Optional<DoseReview> findByDoseResultId(Long doseResultId) {
        return rows.findByUniqueKey(DoseReviewContent.KIND, "M07.DR:" + doseResultId)
                .map(DoseReviewRepository::toDomain);
    }

    public List<DoseReview> findByDoseResultIdIn(Collection<Long> doseResultIds) {
        return doseResultIds.stream()
                .distinct()
                .map(this::findByDoseResultId)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<DoseReview> findByStatus(DoseReviewStatus status) {
        return rows.listByStatus(DoseReviewContent.KIND, Set.of(status.name())).stream()
                .map(DoseReviewRepository::toDomain)
                .toList();
    }

    public List<DoseReview> findByFloorAreaIdAndStatus(Long floorAreaId, DoseReviewStatus status) {
        return findByStatus(status).stream()
                .filter(review -> Objects.equals(review.getFloorAreaId(), floorAreaId))
                .toList();
    }

    /** Mọi trạng thái, theo id. */
    public List<DoseReview> findAll() {
        return rows
                .listByStatus(
                        DoseReviewContent.KIND,
                        Arrays.stream(DoseReviewStatus.values()).map(Enum::name).collect(Collectors.toSet()))
                .stream()
                .map(DoseReviewRepository::toDomain)
                .toList();
    }
}
