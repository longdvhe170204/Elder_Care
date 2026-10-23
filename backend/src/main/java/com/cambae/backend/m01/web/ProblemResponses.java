package com.cambae.backend.m01.web;

import com.cambae.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Phản hồi lỗi RFC 9457 lặp lại ở nhiều endpoint của M01, khai một lần để OpenAPI sinh ra khớp
 * {@code contracts/api-001.yaml} (responses {@code Problem}, {@code CommandOk}, {@code CommandConflict},
 * {@code CommandInvalid}); {@code ApiContractIT} so từng mã phản hồi.
 */
final class ProblemResponses {

    private ProblemResponses() {}

    /** 403: thiếu quyền ở lớp vai trò hoặc phạm vi (002 FR-043). */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "403",
            description = "Thiếu quyền",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @interface Forbidden {}

    /** 404: ngoài phạm vi hoặc không có (FR-007). */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(
            responseCode = "404",
            description = "Không có hoặc ngoài phạm vi",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemBody.class)))
    @interface NotFound {}

    /** Lệnh đổi trạng thái người cao tuổi (contracts: {@code CommandOk}, 403, {@code CommandConflict}, {@code CommandInvalid}). */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã chuyển trạng thái"),
        @ApiResponse(
                responseCode = "403",
                description = "Thiếu quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "UNMET_CONDITIONS (đủ danh sách), INVALID_TRANSITION, PROFILE_CLOSED",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "REASON_REQUIRED, INVALID_DETAILS",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    @interface StatusCommand {}
}
