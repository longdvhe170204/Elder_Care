package com.eldercare.backend.incident.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Mô tả lỗi của {@code /sos} ({@code SosProblem}); lỗi thật do ProblemDetailsHandler trả, kèm {@code contacts}. */
@Schema(name = "SosProblem")
public record SosProblemBody(String errorCode, String detail, SosStatusResponse.FallbackContacts contacts) {}
