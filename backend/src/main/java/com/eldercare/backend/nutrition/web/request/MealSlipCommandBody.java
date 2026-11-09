package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Thân lệnh trên phiếu, phần bổ sung (api-011 {@code POST /meal-slips/{id}/{command}}): {@code content} nội dung sai lệch;
 * {@code resolution} ({@code ADD_PORTION}, {@code REPLACE_PORTION}, {@code OTHER}), {@code description} cách xử lý của bếp.
 */
public record MealSlipCommandBody(
        String content,

        @Schema(allowableValues = {"ADD_PORTION", "REPLACE_PORTION", "OTHER"})
        String resolution,

        String description) {}
