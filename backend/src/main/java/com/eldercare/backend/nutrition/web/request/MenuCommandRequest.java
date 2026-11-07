package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Thân lệnh trên thực đơn (api-011 {@code POST /menus/{id}/{command}}; FR-025, FR-025a, FR-029). */
@Schema(name = "MenuCommand")
public record MenuCommandRequest(
        @Schema(implementation = String.class, minLength = 1, maxLength = 2000)
        Reason reason,

        Boolean processingCheckConfirmed,
        String processingNote,
        List<String> confirmedWarnings) {}
