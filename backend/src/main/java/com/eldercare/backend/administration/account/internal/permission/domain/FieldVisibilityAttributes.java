package com.eldercare.backend.administration.account.internal.permission.domain;

import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Quy tắc xem trường ở {@code catalog_item} loại {@code M15.FIELD_VISIBILITY} (002 T098; FR-022): mã mục
 * {@code <vai trò>:<nhóm thông tin>}; nạp bằng migration, không có API sửa.
 */
public record FieldVisibilityAttributes(Visibility visibility) {

    public static final SharedKind<FieldVisibilityAttributes> KIND = SharedKind.of(
                    "M15.FIELD_VISIBILITY", FieldVisibilityAttributes.class)
            .notDeletable();
}
