package com.eldercare.backend.administration.account.internal.scope.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Lỗi dữ liệu nguồn của lần kiểm quyền ở {@code simple_record} loại {@code M15.PERMISSION_DATA_ERROR} (002 T098;
 * FR-041b, CHK059): trạng thái {@code OPEN}, {@code FIXED}; lỗi đang mở giữ {@link #openKey} ở {@code unique_key} nên
 * mỗi (nguồn, bản ghi, loại lỗi) tối đa một lỗi đang mở; khắc phục thì nhả khóa.
 */
public record PermissionDataErrorContent(
        String source,
        Long recordId,
        String errorKind,
        Instant firstAt,
        Instant lastAt,
        int occurrences,
        Instant fixedAt) {

    public static final SharedKind<PermissionDataErrorContent> KIND =
            SharedKind.of("M15.PERMISSION_DATA_ERROR", PermissionDataErrorContent.class);

    public static final String OPEN = "OPEN";
    public static final String FIXED = "FIXED";

    public static String openKey(String source, Long recordId, String errorKind) {
        return "M15.PDE:" + source + "|" + (recordId == null ? "" : recordId) + "|" + errorKind;
    }

    public PermissionDataErrorContent occurredAgain(Instant at) {
        return new PermissionDataErrorContent(source, recordId, errorKind, firstAt, at, occurrences + 1, null);
    }

    public PermissionDataErrorContent fixed(Instant at) {
        return new PermissionDataErrorContent(source, recordId, errorKind, firstAt, lastAt, occurrences, at);
    }
}
