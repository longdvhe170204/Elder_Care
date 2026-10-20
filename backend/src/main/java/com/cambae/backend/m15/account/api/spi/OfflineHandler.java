package com.cambae.backend.m15.account.api.spi;

import com.cambae.backend.common.offline.SyncOutcome;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import java.time.Instant;

/**
 * SPI: module ghi nhận cài để nhận bản ghi ngoại tuyến qua {@code /sync}; 002 điều phối kiểm quyền theo thời điểm thiết
 * bị và chờ xem lại (002 research A-11; contracts/module-interfaces.md mục 1.7).
 *
 * @param <T> nội dung bản ghi
 */
public interface OfflineHandler<T> {

    /** Loại bản ghi, ví dụ {@code M04.CARE_TASK_RESULT}. */
    String recordType();

    PermissionKey permissionKey();

    Target target(T content);

    void validate(T content);

    /** Ghi bản ghi thật; trả tham chiếu tới bản ghi đã ghi. */
    String apply(T content, SyncOutcome outcome, Instant effectiveAt);

    /** {@code true}: thao tác bắt buộc trực tuyến (kích hoạt khẩn cấp, liều có kiểm soát đặc biệt; Q-01). */
    default boolean requiresOnline() {
        return false;
    }
}
