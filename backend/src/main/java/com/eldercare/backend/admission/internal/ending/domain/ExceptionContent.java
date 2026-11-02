package com.eldercare.backend.admission.internal.ending.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Ngoại lệ điều kiện kết thúc đã duyệt (nhóm 3, loại {@code M02.ENDING_CONDITION_EXCEPTION} của {@code record_event}; FR-064). {@code
 * endingId} rỗng khi căn cứ là danh sách việc sau qua đời, chỉ cho {@code END_H_SETTLEMENT} (FR-071a, Q-387; kiểm ở handler). Mỗi yêu
 * cầu phê duyệt một dòng nhờ {@code unique_key}.
 */
public record ExceptionContent(Long endingId, EndingConditionCode conditionCode, long approvalRequestId) {

    public static final SharedKind<ExceptionContent> KIND =
            SharedKind.of("M02.ENDING_CONDITION_EXCEPTION", ExceptionContent.class);
}
