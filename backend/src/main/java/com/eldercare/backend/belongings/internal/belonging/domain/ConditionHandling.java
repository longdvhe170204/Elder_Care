package com.eldercare.backend.belongings.internal.belonging.domain;

/** Lựa chọn khi mức tình trạng xấu hơn bản ghi gần nhất (FR-015): ghi nhận, hoặc thực hiện Ghi hư hỏng. */
public enum ConditionHandling {
    ACKNOWLEDGE,
    RECORD_DAMAGE
}
