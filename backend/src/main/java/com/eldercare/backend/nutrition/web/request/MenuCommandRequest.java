package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh trên thực đơn (api-011 {@code POST /menus/{id}/{command}}; FR-029): lý do của trả lại, hủy. */
@Schema(name = "MenuCommand")
public record MenuCommandRequest(
        @Schema(implementation = String.class, minLength = 1, maxLength = 2000)
        Reason reason) {}
