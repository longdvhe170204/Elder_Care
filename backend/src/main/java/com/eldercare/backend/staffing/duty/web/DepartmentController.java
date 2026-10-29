package com.eldercare.backend.staffing.duty.web;

import com.eldercare.backend.staffing.duty.internal.service.DepartmentDutyService;
import com.eldercare.backend.staffing.duty.web.response.DepartmentBody;
import com.eldercare.backend.staffing.staff.web.request.StaffCommands;
import com.eldercare.backend.staffing.web.response.M09ProblemBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T035: bộ phận và lệnh chỉ định phụ trách. */
@RestController
@RequestMapping("/api/v1/departments")
class DepartmentController {
    private final DepartmentDutyService service;

    DepartmentController(DepartmentDutyService service) {
        this.service = service;
    }

    @GetMapping
    List<DepartmentBody> list() {
        return service.list().stream().map(DepartmentBody::of).toList();
    }

    @PostMapping
    @Operation(
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = DepartmentBody.class))))
    @ApiResponse(responseCode = "201", description = "Đã tạo", content = @Content)
    ResponseEntity<Void> create(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody StaffCommands.NewDepartment input) {
        long id = service.create(input.name(), !Boolean.FALSE.equals(input.active()));
        return ResponseEntity.created(URI.create("/api/v1/departments/" + id)).build();
    }

    @PostMapping("/{id}/assign-lead")
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
    ResponseEntity<Void> assign(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody StaffCommands.AssignLead input) {
        service.assignLead(id, input.staffId(), input.reason());
        return ResponseEntity.ok().build();
    }
}
