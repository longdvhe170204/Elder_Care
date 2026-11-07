package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Thân ghi lưu mẫu (api-011 {@code RetainedSampleCommand}; FR-061): mỗi món đã lưu mẫu hay không, lý do bắt buộc khi không
 * lưu (kiểm ở service). Gọi lại cho cùng bữa để thêm món mới phát sinh sau.
 */
@Schema(name = "RetainedSampleCommand")
public record RetainedSampleRequest(
        String storageLocation, @NotNull @Valid List<Dish> dishes) {

    @Schema(name = "RetainedSampleDish")
    public record Dish(@NotNull Long dishId, @NotNull Boolean retained, String notRetainedReason) {}
}
