package com.eldercare.backend.administration.account.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân {@code PUT /me/contact} (api-002, bổ sung 2026-11-09): null hoặc chuỗi rỗng là xóa giá trị. */
public record MyContactBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        String currentPassword,

        @Schema(
                types = {"string", "null"},
                description = "Số di động 0 và 9 chữ số; cho phép dấu cách, chấm, gạch ngang")
        String phoneNumber,

        @Schema(types = {"string", "null"}) String email) {}
