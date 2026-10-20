package com.cambae.backend.m15.account.legal.web;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.legal.internal.ProfessionalActivity;
import com.cambae.backend.m15.account.legal.internal.ProfessionalActivityService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh mục nghiệp vụ chuyên môn (002 T065; contracts/api-002.yaml; FR-036). ETag là {@code version} của mục (hợp đồng
 * bổ sung 2026-10-21, chủ dự án duyệt).
 */
@RestController
@RequestMapping("/api/v1/professional-activities")
class ProfessionalActivityController {

    private final ProfessionalActivityService activities;

    ProfessionalActivityController(ProfessionalActivityService activities) {
        this.activities = activities;
    }

    @Schema(name = "ProfessionalActivity")
    record ProfessionalActivityBody(
            Long id,
            String code,
            String name,
            String function,
            String requiredLicenseType,
            List<Long> requiredPracticeScopes,
            Boolean requiresFacilityScope,
            Boolean isDefault,
            Status status,
            Integer version) {

        static ProfessionalActivityBody of(ProfessionalActivity a) {
            return new ProfessionalActivityBody(
                    a.id(),
                    a.code(),
                    a.name(),
                    a.function().name(),
                    a.requiredLicenseType(),
                    a.practiceScopes().stream().sorted().toList(),
                    a.requiresFacilityScope(),
                    a.isDefault(),
                    a.active() ? Status.ACTIVE : Status.INACTIVE,
                    (int) a.version());
        }
    }

    enum Status {
        ACTIVE,
        INACTIVE
    }

    @Schema(name = "ProfessionalActivityInput")
    record ProfessionalActivityInput(
            @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String name,

            @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String function,

            @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String requiredLicenseType,

            List<Long> requiredPracticeScopes,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Boolean requiresFacilityScope,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {

        ProfessionalActivityService.Input input() {
            return new ProfessionalActivityService.Input(
                    name,
                    function,
                    requiredLicenseType,
                    requiredPracticeScopes == null ? Set.of() : Set.copyOf(requiredPracticeScopes),
                    requiresFacilityScope);
        }
    }

    record DeactivateBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    @GetMapping
    @Tag(name = "professional-activities")
    List<ProfessionalActivityBody> list() {
        return activities.list().stream().map(ProfessionalActivityBody::of).toList();
    }

    @PostMapping
    @Tag(name = "professional-activities")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Đã tạo"))
    ResponseEntity<ProfessionalActivityBody> create(@Valid @RequestBody ProfessionalActivityInput body) {
        ProfessionalActivity created = activities.create(body.input(), body.reason());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(created.version()))
                .body(ProfessionalActivityBody.of(created));
    }

    @PutMapping("/{id}")
    @Tag(name = "professional-activities")
    ResponseEntity<ProfessionalActivityBody> update(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody ProfessionalActivityInput body) {
        return ok(activities.update(id, ifMatch, body.input(), body.reason()));
    }

    @PostMapping("/{id}/deactivate")
    @Tag(name = "professional-activities")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ProfessionalActivityBody> deactivate(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch, @RequestBody DeactivateBody body) {
        return ok(activities.deactivate(id, ifMatch, body.reason()));
    }

    private static ResponseEntity<ProfessionalActivityBody> ok(ProfessionalActivity a) {
        return ResponseEntity.ok().eTag(VersionEtag.of(a.version())).body(ProfessionalActivityBody.of(a));
    }
}
