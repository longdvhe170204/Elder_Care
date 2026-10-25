package com.cambae.backend.m02.internal.application.service;

import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.domain.AdmissionOpinion;
import com.cambae.backend.m02.internal.application.domain.ApplicationStatus;
import com.cambae.backend.m02.internal.application.domain.OpinionChoice;
import com.cambae.backend.m02.internal.application.persistence.AdmissionApplicationStore;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Cảnh báo về ý kiến của người cao tuổi khi Hoàn tất tiếp nhận (FR-004a, Q-356): chưa ghi ý kiến thì {@code
 * ADMISSION_OPINION_MISSING}; ý kiến hiện hành là Không đồng ý thì {@code RESIDENT_DISAGREES_WITH_ADMISSION} kèm nguồn,
 * người ghi, thời điểm. Cảnh báo không chặn lệnh. Lượt đã Đã tiếp nhận tính theo ý kiến đã ghi vào lượt lúc đó.
 */
@Component
public class AdmissionOpinionWarnings {

    public static final String MISSING = "ADMISSION_OPINION_MISSING";
    public static final String DISAGREES = "RESIDENT_DISAGREES_WITH_ADMISSION";

    private final AdmissionApplicationStore store;
    private final M02ActorNames names;

    AdmissionOpinionWarnings(AdmissionApplicationStore store, M02ActorNames names) {
        this.store = store;
        this.names = names;
    }

    /** Ý kiến tính cho lượt: ý kiến Hiệu lực, hoặc ý kiến đã ghi vào lượt khi Hoàn tất tiếp nhận. */
    public Optional<AdmissionOpinion> opinionOf(AdmissionApplication application) {
        if (application.status() == ApplicationStatus.ADMITTED) {
            return Optional.ofNullable(application.opinionAtAdmissionId()).flatMap(store::findOpinion);
        }
        return store.findActiveOpinion(application.id());
    }

    public List<Warning> of(Optional<AdmissionApplication> application) {
        Optional<AdmissionOpinion> opinion = application.flatMap(this::opinionOf);
        if (opinion.isEmpty()) {
            return List.of(new Warning(MISSING, "Chưa ghi ý kiến của người cao tuổi về việc vào ở"));
        }
        AdmissionOpinion current = opinion.get();
        if (current.opinion() != OpinionChoice.DISAGREE) {
            return List.of();
        }
        return List.of(new Warning(
                DISAGREES,
                "Người cao tuổi không đồng ý vào ở (nguồn " + current.source().type() + ", ghi bởi "
                        + names.of(current.recordedBy()) + " lúc " + current.recordedAt()
                        + (current.note() == null ? "" : ": " + current.note()) + ")"));
    }
}
