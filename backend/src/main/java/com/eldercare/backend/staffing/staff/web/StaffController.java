package com.eldercare.backend.staffing.staff.web;

import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.staffing.staff.internal.service.StaffService;
import com.eldercare.backend.staffing.staff.web.request.StaffProfileInput;
import com.eldercare.backend.staffing.staff.web.response.StaffProfileBody;
import com.eldercare.backend.staffing.web.response.M09ProblemBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T006: các endpoint mô tả hồ sơ đúng api-008; lệnh trạng thái làm ở phase sau. */
@RestController
@RequestMapping("/api/v1/staff")
class StaffController {
    private final StaffService staff;

    StaffController(StaffService staff) {
        this.staff = staff;
    }

    @GetMapping
    List<StaffProfileBody> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @Schema(allowableValues = {"WORKING", "RESIGNED"}) String status,
            @RequestParam(required = false) Long departmentId) {
        return staff.list(q, status, departmentId).stream()
                .map(StaffProfileBody::of)
                .toList();
    }

    @GetMapping("/{id}")
    ResponseEntity<StaffProfileBody> get(@PathVariable long id) {
        var s = staff.get(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(s.version())).body(StaffProfileBody.of(s));
    }

    @PostMapping
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = StaffProfileBody.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> create(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey, @Valid @RequestBody StaffProfileInput input) {
        var s = staff.create(input.input());
        return ResponseEntity.created(URI.create("/api/v1/staff/" + s.id())).build();
    }

    @PutMapping("/{id}")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = StaffProfileBody.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> update(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody StaffProfileInput input) {
        var s = staff.update(id, ifMatch, input.input());
        return ResponseEntity.ok().eTag(VersionEtag.of(s.version())).build();
    }

    @PostMapping("/{id}/terminate-employment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "OK",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "OK",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "OK",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> terminateEmployment(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody com.eldercare.backend.staffing.staff.web.request.StaffCommands.Terminate input) {
        staff.terminateEmployment(id, input.effectiveAt(), input.replacementStaffId(), input.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/rehire")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "422",
                description = "OK",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> rehire(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody com.eldercare.backend.staffing.staff.web.request.StaffCommands.ReasonBody input) {
        staff.rehire(id, input.reason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/change-department")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "OK",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> changeDepartment(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody com.eldercare.backend.staffing.staff.web.request.StaffCommands.ChangeDepartment input) {
        staff.changeDepartment(id, input.departmentId(), input.replacementStaffId(), input.reason());
        return ResponseEntity.ok().build();
    }
}
