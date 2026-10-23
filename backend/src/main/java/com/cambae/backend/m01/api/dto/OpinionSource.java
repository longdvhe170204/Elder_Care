package com.cambae.backend.m01.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Nguồn của ý kiến dùng chung (FR-023g, BR-M01-14); {@code familyMemberId} chỉ có khi {@code FAMILY_MEMBER}. */
public record OpinionSource(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OpinionSourceType type, Long familyMemberId) {}
