package com.cambae.backend.m02.web.response;

import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.OpinionSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Nguồn của ý kiến, như api-001 (contracts/api-004.yaml, schema {@code OpinionSource}); chỉ {@code type} bắt buộc. */
@Schema(name = "AdmissionOpinionSource")
public record OpinionSourceBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OpinionSourceType type,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        Long familyMemberId) {

    public static OpinionSourceBody of(OpinionSource source) {
        return new OpinionSourceBody(source.type(), source.familyMemberId());
    }

    public OpinionSource toSource() {
        return new OpinionSource(type, familyMemberId);
    }
}
