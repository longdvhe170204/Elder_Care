package com.eldercare.backend.staffing.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.staffing.internal.assignment.service.*;
import com.eldercare.backend.staffing.web.request.AssignmentRequests;
import com.eldercare.backend.staffing.web.response.AssignmentResponses;
import com.eldercare.backend.staffing.web.response.M09ProblemBody;
import com.eldercare.backend.staffing.web.response.ShiftResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T022/T054: giữ thao tác gốc, bổ sung sửa Nháp theo hợp đồng được duyệt29/10. */
@RestController
@RequestMapping("/api/v1")
class AssignmentController {
    private final AssignmentService assignments;
    private final FloorManagerService managers;

    AssignmentController(AssignmentService assignments, FloorManagerService managers) {
        this.assignments = assignments;
        this.managers = managers;
    }

    @GetMapping("/assignments/{id}")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Phân công hiện hành",
                content = @Content(schema = @Schema(implementation = AssignmentResponses.Detail.class))),
        @ApiResponse(
                responseCode = "401",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<AssignmentResponses.Detail> detail(@PathVariable long id) {
        var a = assignments.detail(id);
        return ResponseEntity.ok().eTag(VersionEtag.of(a.version())).body(AssignmentResponses.Detail.of(a));
    }

    @PostMapping("/assignments/{id}/draft-update-preview")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Cảnh báo trước sửa",
                content = @Content(schema = @Schema(implementation = AssignmentResponses.DraftPreview.class))),
        @ApiResponse(
                responseCode = "400",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "401",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
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
    ResponseEntity<AssignmentResponses.DraftPreview> previewDraft(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody AssignmentRequests.New body) {
        var result = assignments.previewDraftUpdate(id, ifMatch, body.input(), assignments.target(id));
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(result.assignment().version()))
                .body(new AssignmentResponses.DraftPreview(ShiftResponses.warnings(result.warnings())));
    }

    @PostMapping("/assignments/{id}/update-draft")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Phân công Nháp sau sửa",
                content = @Content(schema = @Schema(implementation = AssignmentResponses.DraftUpdate.class))),
        @ApiResponse(
                responseCode = "400",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "401",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
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
    ResponseEntity<AssignmentResponses.DraftUpdate> updateDraft(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody AssignmentRequests.New body) {
        var result = assignments.updateDraft(id, ifMatch, body.input(), assignments.target(id));
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(result.assignment().version()))
                .body(new AssignmentResponses.DraftUpdate(
                        AssignmentResponses.Detail.of(result.assignment()),
                        ShiftResponses.warnings(result.warnings())));
    }

    @GetMapping("/floor-manager-assignments")
    List<AssignmentResponses.Floor> floors(@RequestParam(required = false) Long floorAreaId) {
        return managers.list(floorAreaId).stream()
                .map(AssignmentResponses.Floor::of)
                .toList();
    }

    @PostMapping("/floor-manager-assignments")
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = AssignmentResponses.Floor.class))))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã giao", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> assignFloor(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody AssignmentRequests.Floor body) {
        long id = managers.assign(
                body.staffId(),
                body.floorAreaId(),
                body.from(),
                body.to(),
                body.optionalReason(),
                new Target("M09.FLOOR_MANAGER_ASSIGNMENT", null, null, body.floorAreaId()));
        return ResponseEntity.created(URI.create("/api/v1/floor-manager-assignments/" + id))
                .build();
    }

    @PostMapping("/floor-manager-assignments/{id}/end")
    @ApiResponse(responseCode = "200", description = "Đã cập nhật", content = @Content)
    ResponseEntity<Void> endFloor(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody AssignmentRequests.EndFloor body) {
        managers.end(id, body.to(), body.reason(), managers.target(id));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/shifts/{id}/assignments")
    List<AssignmentResponses.Assignment> list(@PathVariable long id) {
        return assignments.list(id).stream()
                .map(AssignmentResponses.Assignment::of)
                .toList();
    }

    @PostMapping("/shifts/{id}/assignments")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Đã lập",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ShiftResponses.Result.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<ShiftResponses.Result> create(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody AssignmentRequests.New body) {
        var result = assignments.create(id, body.input(), assignments.targetOfShift(id));
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + result.id()))
                .body(new ShiftResponses.Result(result.id(), ShiftResponses.warnings(result.warnings())));
    }

    @PostMapping("/shifts/{id}/assignments/preview")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Đã kiểm tra",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ShiftResponses.Result.class))),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ShiftResponses.Result preview(@PathVariable long id, @Valid @RequestBody AssignmentRequests.New body) {
        var result = assignments.preview(id, body.input(), assignments.targetOfShift(id));
        return new ShiftResponses.Result(null, ShiftResponses.warnings(result.warnings()));
    }

    @DeleteMapping("/assignments/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Đã xóa", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ResponseEntity<Void> delete(@PathVariable long id) {
        assignments.delete(id, assignments.target(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assignments/{id}/cancel")
    @ApiResponse(responseCode = "200", description = "Đã hủy", content = @Content)
    ResponseEntity<Void> cancel(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody AssignmentRequests.ReasonBody body) {
        assignments.cancel(id, body.reason(), assignments.target(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/assignments/{id}/end")
    @ApiResponse(responseCode = "200", description = "Đã kết thúc", content = @Content)
    ResponseEntity<Void> end(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody AssignmentRequests.ReasonBody body) {
        assignments.end(id, body.reason(), assignments.target(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/shifts/{id}/copy-assignments")
    AssignmentResponses.Copy copy(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody AssignmentRequests.Copy body) {
        var result = assignments.copy(body.sourceShiftId(), id, assignments.targetOfShift(id));
        return new AssignmentResponses.Copy(
                result.copiedIds(),
                result.notCopied().stream()
                        .map(s -> new AssignmentResponses.Skipped(s.sourceAssignmentId(), s.reason()))
                        .toList());
    }
}
