package com.eldercare.backend.administration.account.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

public record DeactivateBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {}
