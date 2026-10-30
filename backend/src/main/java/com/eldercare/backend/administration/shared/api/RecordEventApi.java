package com.eldercare.backend.administration.shared.api;

import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Bản ghi con dùng chung {@code record_event} (nhóm 3; bản nháp sửa được tới khi xác nhận, mục 1.4; module-interfaces
 * 000 mục 1.8). Lệnh ghi chạy trong transaction của lệnh gọi.
 */
public interface RecordEventApi {

    /** 409 {@code ALREADY_PROCESSED} khi trùng {@code uniqueKey}. */
    <T> RecordEvent<T> add(SharedKind<T> kind, EntityRef parent, NewRecordEvent<T> event);

    /** 409 khi dòng đã xác nhận. */
    <T> RecordEvent<T> updateDraft(SharedKind<T> kind, long id, T content);

    /** 409 khi dòng đã xác nhận. */
    <T> RecordEvent<T> confirm(SharedKind<T> kind, long id, Instant confirmedAt);

    /** 404 khi không có hoặc thuộc loại khác. */
    <T> RecordEvent<T> require(SharedKind<T> kind, long id);

    <T> Optional<RecordEvent<T>> findByUniqueKey(SharedKind<T> kind, String uniqueKey);

    /** Theo {@code occurred_at}, {@code id}. */
    <T> List<RecordEvent<T>> list(SharedKind<T> kind, EntityRef parent);
}
