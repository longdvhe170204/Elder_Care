package com.eldercare.backend.roombedallocation.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** 019 T018: thân {@code AssetTypeWrite} của api-019; {@code id} có thì sửa, {@code status} ACTIVE hoặc INACTIVE. */
public record AssetTypeWrite(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        String status) {}
