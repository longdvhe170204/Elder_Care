package com.eldercare.backend.family.internal.feedback.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.family.internal.feedback.domain.Feedback;
import com.eldercare.backend.family.internal.feedback.persistence.FeedbackStore;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ phản hồi ({@code LuuTruProvider}; research F-17): theo hồ sơ người cao tuổi (CFG-M15-03), tính từ lúc người đó
 * sang trạng thái cuối. Mục nội dung gắn {@code M10.FEEDBACK} do job xóa cùng chuỗi.
 */
@Component
class FeedbackRetention implements RetentionProvider {

    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final FeedbackStore store;
    private final RetentionDatabase database;
    private final ResidentApi residents;

    FeedbackRetention(FeedbackStore store, RetentionDatabase database, ResidentApi residents) {
        this.store = store;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return Feedback.ENTITY_TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        List<Candidate> candidates = new ArrayList<>();
        for (long residentId : residents.byStatus(FINAL_STATUSES)) {
            if (candidates.size() >= batch) {
                break;
            }
            residents
                    .finalStatusAt(residentId)
                    .filter(at -> at.isBefore(cutoff))
                    .ifPresent(at -> store.ofResident(residentId).stream()
                            .limit(batch - candidates.size())
                            .forEach(id -> candidates.add(new Candidate(id, at))));
        }
        return candidates.stream();
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return Set.of();
    }

    @Override
    public void delete(Collection<Long> ids) {
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        database.jdbc().update("DELETE FROM feedback WHERE id IN " + in);
    }
}
