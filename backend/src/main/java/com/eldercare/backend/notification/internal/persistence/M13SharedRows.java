package com.eldercare.backend.notification.internal.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.error.AlreadyProcessedException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi dòng loại M13 vào bảng chung qua API của 000 (009 T043; research R-15 mục 2: đọc bằng SQL trong
 * {@code persistence}, ghi qua API). Trùng {@code unique_key} báo {@link DuplicateKeyException} như khi M13 còn bảng
 * riêng, để service giữ cách xử lý lượt chạy đồng thời. Job gọi ngoài transaction thì mỗi lần ghi là một transaction, như
 * câu {@code INSERT} tự commit trước đây.
 */
@Component
public class M13SharedRows {

    /** Người lập dòng {@code simple_record} do hệ thống tạo (không có tài khoản). */
    static final long SYSTEM = 0;

    private final RecordEventApi events;
    private final SimpleRecordApi records;

    M13SharedRows(RecordEventApi events, SimpleRecordApi records) {
        this.events = events;
        this.records = records;
    }

    /** Thêm dòng con đã xác nhận (không sửa được); {@code actorId} {@code null} là Hệ thống. Trả id dòng. */
    @Transactional
    public <T> long addEvent(
            SharedKind<T> kind,
            String parentType,
            long parentId,
            T content,
            Long actorId,
            Instant at,
            String uniqueKey) {
        try {
            return events.add(
                            kind,
                            new EntityRef(parentType, parentId),
                            new NewRecordEvent<>(
                                    content, null, actorId, actorId == null, at, null, at, null, uniqueKey))
                    .id();
        } catch (AlreadyProcessedException e) {
            throw new DuplicateKeyException(e.getMessage(), e);
        }
    }

    /** Thêm đối tượng nhỏ do hệ thống lập; {@code parent} có thể {@code null}. Trả id dòng. */
    @Transactional
    public <T> long createRecord(SharedKind<T> kind, EntityRef parent, String status, T content, String uniqueKey) {
        try {
            return records.create(
                            kind,
                            new NewSimpleRecord<>(null, parent, status, content, null, null, uniqueKey, SYSTEM, false))
                    .id();
        } catch (ConditionNotMetException e) {
            throw new DuplicateKeyException(e.getMessage(), e);
        }
    }
}
