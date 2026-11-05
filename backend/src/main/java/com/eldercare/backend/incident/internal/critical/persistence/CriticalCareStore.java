package com.eldercare.backend.incident.internal.critical.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.incident.internal.critical.domain.ChoiceExecutionContent;
import com.eldercare.backend.incident.internal.critical.domain.CriticalFlagContent;
import com.eldercare.backend.incident.internal.critical.domain.CriticalFlagStatus;
import com.eldercare.backend.incident.internal.critical.domain.WishReconfirmationContent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Dấu nguy kịch trên hồ sơ chung, kết quả xác nhận lại và lựa chọn thực hiện trên sự kiện chung gắn cảnh báo (R-15,
 * data-model mục 0.1, 9). DBR-34 giữ bằng khóa duy nhất {@code CRITICAL_FLAG:<residentId>} khi dấu còn mở; gỡ dấu nhả
 * khóa và khóa dòng (Đã gỡ là trạng thái cuối).
 */
@Repository
public class CriticalCareStore {
    public static final String FLAG_TYPE = "M05.CRITICAL_FLAG";
    public static final String RECONFIRMATION_TYPE = "M05.WISH_RECONFIRMATION";
    public static final String CHOICE_TYPE = "M05.CHOICE_EXECUTION";
    private static final SharedKind<CriticalFlagContent> FLAG = SharedKind.of(FLAG_TYPE, CriticalFlagContent.class);
    private static final SharedKind<WishReconfirmationContent> RECONFIRMATION =
            SharedKind.of(RECONFIRMATION_TYPE, WishReconfirmationContent.class);
    private static final SharedKind<ChoiceExecutionContent> CHOICE =
            SharedKind.of(CHOICE_TYPE, ChoiceExecutionContent.class);

    private final SimpleRecordApi records;
    private final RecordEventApi events;

    public CriticalCareStore(SimpleRecordApi records, RecordEventApi events) {
        this.records = records;
        this.events = events;
    }

    public Optional<SimpleRecord<CriticalFlagContent>> openFlag(long residentId) {
        return records.findByUniqueKey(FLAG, key(residentId));
    }

    public SimpleRecord<CriticalFlagContent> create(
            long residentId, CriticalFlagStatus status, CriticalFlagContent content, long actor) {
        return records.create(
                FLAG,
                new NewSimpleRecord<>(
                        residentId, null, status.name(), content, null, null, key(residentId), actor, false));
    }

    public SimpleRecord<CriticalFlagContent> lock(long id) {
        return records.lockForUpdate(FLAG, id);
    }

    /** Đổi trạng thái hoặc nội dung khi dấu còn mở; sang Đã gỡ thì nhả khóa duy nhất và khóa dòng. */
    public SimpleRecord<CriticalFlagContent> change(
            SimpleRecord<CriticalFlagContent> flag, CriticalFlagStatus status, CriticalFlagContent content) {
        boolean removed = status == CriticalFlagStatus.REMOVED;
        return records.change(
                        FLAG,
                        flag.id(),
                        flag.version(),
                        new RecordChange<>(
                                status.name(), content, null, removed ? null : key(flag.residentId()), removed))
                .after();
    }

    public RecordEvent<WishReconfirmationContent> addReconfirmation(
            long alertId, WishReconfirmationContent content, long actor, Instant at, Long floorAreaId) {
        return events.add(
                RECONFIRMATION,
                alert(alertId),
                new NewRecordEvent<>(content, null, actor, false, content.contactedAt(), null, at, floorAreaId, null));
    }

    public List<RecordEvent<WishReconfirmationContent>> reconfirmations(long alertId) {
        return events.list(RECONFIRMATION, alert(alertId));
    }

    public RecordEvent<WishReconfirmationContent> reconfirmation(long id) {
        return events.require(RECONFIRMATION, id);
    }

    public RecordEvent<ChoiceExecutionContent> addChoice(
            long alertId, ChoiceExecutionContent content, long actor, Instant at, Long floorAreaId) {
        return events.add(
                CHOICE,
                alert(alertId),
                new NewRecordEvent<>(content, null, actor, false, at, null, at, floorAreaId, null));
    }

    public List<RecordEvent<ChoiceExecutionContent>> choices(long alertId) {
        return events.list(CHOICE, alert(alertId));
    }

    public RecordEvent<ChoiceExecutionContent> choice(long id) {
        return events.require(CHOICE, id);
    }

    private static EntityRef alert(long alertId) {
        return new EntityRef("M05.ALERT", alertId);
    }

    private static String key(long residentId) {
        return "CRITICAL_FLAG:" + residentId;
    }
}
