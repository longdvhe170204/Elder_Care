package com.eldercare.backend.admission.internal.change.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Chỉ mục thuộc tính của yêu cầu thay đổi (FR-046; loại {@code M02.CHANGE_ATTRIBUTE} của {@code simple_record}). Lệch so với data-model
 * 0.1 (ghi {@code khoa_ky_thuat}): {@code technical_lock} chỉ có khóa, không giữ được yêu cầu, hợp đồng và không nhả được; ghi lý do ở
 * plan. {@code unique_key} chỉ có giá trị khi chờ hiệu lực nên {@code UNIQUE} chặn hai yêu cầu chờ hiệu lực cùng thuộc tính.
 */
public record ChangeAttribute(long approvalRequestId, long contractId, ChangeType attribute) {

    public static final SharedKind<ChangeAttribute> KIND = SharedKind.of("M02.CHANGE_ATTRIBUTE", ChangeAttribute.class);
}
