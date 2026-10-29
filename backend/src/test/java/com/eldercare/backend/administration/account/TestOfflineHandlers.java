package com.eldercare.backend.administration.account;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.offline.SyncOutcome;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link OfflineHandler} của test đồng bộ ngoại tuyến (002 T050): ghi chú chăm sóc gắn người cao tuổi hoặc tầng, lưu
 * trong bộ nhớ; loại {@code TEST.EMERGENCY} bắt buộc trực tuyến (Q-01).
 */
@TestConfiguration
class TestOfflineHandlers {

    static final String CARE_NOTE = "TEST.CARE_NOTE";
    static final String EMERGENCY = "TEST.EMERGENCY";

    record CareNote(Long residentId, Long floorAreaId, String note) {}

    record Applied(CareNote content, SyncOutcome outcome, Instant effectiveAt) {}

    static class CareNotes implements OfflineHandler<CareNote> {

        final List<Applied> applied = new CopyOnWriteArrayList<>();

        @Override
        public String recordType() {
            return CARE_NOTE;
        }

        @Override
        public PermissionKey permissionKey() {
            return new PermissionKey(Function.TASK_CHECKLIST, Operation.CREATE);
        }

        @Override
        public Target target(CareNote content) {
            return new Target(CARE_NOTE, null, content.residentId(), content.floorAreaId());
        }

        @Override
        public void validate(CareNote content) {
            if (content.note() == null || content.note().isBlank()) {
                throw new InvalidValueException("Thiếu nội dung ghi chú");
            }
        }

        @Override
        public String apply(CareNote content, SyncOutcome outcome, Instant effectiveAt) {
            applied.add(new Applied(content, outcome, effectiveAt));
            return CARE_NOTE + "/" + applied.size();
        }

        /** Các lần ghi thật của ghi chú có nội dung đã cho. */
        List<Applied> appliedWith(String note) {
            return applied.stream().filter(a -> note.equals(a.content().note())).toList();
        }
    }

    static class Emergency extends CareNotes {

        @Override
        public String recordType() {
            return EMERGENCY;
        }

        @Override
        public boolean requiresOnline() {
            return true;
        }
    }

    @Bean
    CareNotes careNotes() {
        return new CareNotes();
    }

    @Bean
    Emergency emergency() {
        return new Emergency();
    }
}
