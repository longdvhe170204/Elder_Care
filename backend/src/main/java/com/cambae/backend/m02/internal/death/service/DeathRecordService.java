package com.cambae.backend.m02.internal.death.service;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m02.internal.death.domain.DeathRecord;
import com.cambae.backend.m02.internal.death.persistence.DeathStore;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CurrentValue;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc bản ghi qua đời với giá trị hiện hành (FR-069): bản gốc nhóm 3 không đổi, nguyên nhân bổ sung và các trường sửa sai nằm
 * ở đính chính {@code M02.DEATH_RECORD} (000). Giới hạn trường nguyên nhân (nhóm sức khỏe, BR-M02-21) do 002 áp khi tuần tự
 * hóa phản hồi, không phải ở đây.
 */
@Service
public class DeathRecordService {

    /** Giá trị hiện hành của bản ghi; {@code corrected} khi có đính chính. */
    public record Effective(
            long id,
            long residentId,
            java.time.Instant deathAt,
            String place,
            String discoveredBy,
            long confirmedById,
            String cause,
            Long evidenceFileId,
            boolean corrected) {}

    private final DeathStore store;
    private final ObjectProvider<CorrectionApi> corrections;

    DeathRecordService(DeathStore store, ObjectProvider<CorrectionApi> corrections) {
        this.store = store;
        this.corrections = corrections;
    }

    public static Target target(long recordId, long residentId) {
        return new Target(PostDeathTaskService.ENTITY_TYPE, String.valueOf(recordId), residentId, null);
    }

    public static Target targetForResident(long residentId) {
        return new Target(PostDeathTaskService.ENTITY_TYPE, null, residentId, null);
    }

    /** Đối tượng kiểm quyền của một bản ghi; không có thì 404. */
    @Transactional(readOnly = true)
    public Target targetOf(long recordId) {
        DeathRecord record = store.find(recordId).orElseThrow(NotFoundException::new);
        return target(record.id(), record.residentId());
    }

    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Effective get(Target target, long residentId) {
        return effective(store.ofResident(residentId).orElseThrow(NotFoundException::new));
    }

    private Effective effective(DeathRecord record) {
        CorrectionApi api = corrections.getIfAvailable();
        CurrentValue value = api == null
                ? null
                : api.current(PostDeathTaskService.ENTITY_TYPE, List.of(record.id()))
                        .get(record.id());
        String place = record.place();
        String discoveredBy = record.discoveredBy();
        long confirmedById = record.confirmedById();
        String cause = record.causeOfDeath();
        boolean corrected = value != null && value.corrected() && value.content() instanceof Map<?, ?>;
        if (corrected) {
            Map<?, ?> content = (Map<?, ?>) value.content();
            place = text(content, "place", place);
            discoveredBy = text(content, "discoveredBy", discoveredBy);
            cause = text(content, "cause", cause);
            if (content.get("confirmedById") != null) {
                confirmedById = Long.parseLong(content.get("confirmedById").toString());
            }
        }
        return new Effective(
                record.id(),
                record.residentId(),
                record.deathAt(),
                place,
                discoveredBy,
                confirmedById,
                cause,
                record.evidenceFileId(),
                corrected);
    }

    private static String text(Map<?, ?> content, String key, String fallback) {
        Object value = content.get(key);
        return value == null ? fallback : value.toString();
    }
}
