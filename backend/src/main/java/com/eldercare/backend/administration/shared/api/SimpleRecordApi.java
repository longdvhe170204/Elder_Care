package com.eldercare.backend.administration.shared.api;

import com.eldercare.backend.administration.shared.api.dto.Change;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Đối tượng nhỏ dùng chung {@code simple_record} (nhóm 2, khóa theo dòng, constitution XII; máy trạng thái ở domain của
 * module; module-interfaces 000 mục 1.8). Lệnh ghi chạy trong transaction của lệnh gọi.
 */
public interface SimpleRecordApi {

    /** 409 khi trùng {@code uniqueKey}. */
    <T> SimpleRecord<T> create(SharedKind<T> kind, NewSimpleRecord<T> record);

    /** 404 khi không có hoặc thuộc loại khác. */
    <T> SimpleRecord<T> require(SharedKind<T> kind, long id);

    /** Như {@link #require}, khóa dòng tới hết transaction ({@code SELECT … FOR UPDATE}). */
    <T> SimpleRecord<T> lockForUpdate(SharedKind<T> kind, long id);

    /** 412 khi {@code version} khác phiên bản hiện hành; 409 {@code RECORD_LOCKED} khi dòng đã khóa. */
    <T> Change<SimpleRecord<T>> change(SharedKind<T> kind, long id, int version, RecordChange<T> change);

    <T> Optional<SimpleRecord<T>> findByUniqueKey(SharedKind<T> kind, String uniqueKey);

    /** {@code statuses} rỗng là mọi trạng thái; theo {@code id}. */
    <T> List<SimpleRecord<T>> listByResident(SharedKind<T> kind, long residentId, Set<String> statuses);

    /** Theo {@code id}. */
    <T> List<SimpleRecord<T>> listByParent(SharedKind<T> kind, EntityRef parent);

    /**
     * Mọi dòng của loại có trạng thái thuộc {@code statuses} (không rỗng), theo {@code id}; cho danh sách không gắn người
     * cao tuổi hay đối tượng cha (đổi api 2026-10-30, 002 T098).
     */
    <T> List<SimpleRecord<T>> listByStatus(SharedKind<T> kind, Set<String> statuses);
}
