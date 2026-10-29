package com.eldercare.backend.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Một điều kiện chưa thỏa: mã quy tắc (BR-…) và mô tả (FR-006). Điều kiện của lệnh trạng thái (001, 004 FR-004) kèm
 * căn cứ, feature sở hữu và có miễn được qua yêu cầu phê duyệt không; trường không có thì không ghi ra thân lỗi.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UnmetCondition(String code, String description, String basis, String module, Boolean waivable) {

    public UnmetCondition(String code, String description) {
        this(code, description, null, null, null);
    }
}
