package com.eldercare.backend.resident.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Một điều kiện chưa thỏa của lệnh trạng thái.
 *
 * @param basis mã BR hoặc FR, ví dụ {@code BR-M12-03}
 * @param waivable có thể được miễn qua yêu cầu phê duyệt (chỉ 004)
 * @param module feature sở hữu điều kiện, ví dụ {@code 013} (api-001 {@code Condition.module}); 001 gắn từ
 *     {@link com.eldercare.backend.resident.api.spi.StatusTransitionParticipant#module()} khi bên tham gia để rỗng
 */
@Schema(name = "StatusCondition")
public record Condition(String code, String description, String basis, boolean waivable, String module) {
    public Condition(String code, String description, String basis, boolean waivable) {
        this(code, description, basis, waivable, null);
    }

    public Condition withModule(String owner) {
        return new Condition(code, description, basis, waivable, owner);
    }
}
