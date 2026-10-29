package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedReadinessService;
import com.eldercare.backend.roombedallocation.web.request.BedReadinessWrite;
import com.eldercare.backend.roombedallocation.web.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** T060: HTTP sẵn sàng, không cấp thêm quyền cấu hình/phân bổ cho người xác nhận. */
@RestController
@RequestMapping("/api/v1/beds/{id}")
public class BedReadinessController {
    private final BedReadinessService service;
    private final ResidentApi residents;

    public BedReadinessController(BedReadinessService service, ResidentApi residents) {
        this.service = service;
        this.residents = residents;
    }

    @GetMapping("/readiness-conditions")
    public BedReadinessResponse conditions(@PathVariable long id) {
        var value = service.conditions(service.target(id));
        var resident = value.departedResidentId();
        return new BedReadinessResponse(
                value.allowed(),
                value.disinfectionRequired(),
                resident == null
                        ? null
                        : new ResidentSummarySchema(
                                resident, residents.identity(resident).fullName(), null, null),
                value.unmetConditions());
    }

    @PostMapping("/confirm-readiness")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Đã xác nhận",
                content =
                        @Content(mediaType = "application/json", schema = @Schema(implementation = BedResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Chưa thỏa điều kiện",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Thiếu dấu xác nhận",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public BedResponse confirm(@PathVariable long id, @Valid @RequestBody BedReadinessWrite body) {
        return BedResponse.of(service.confirm(service.target(id), body.disinfected(), body.ppeUsed(), body.note()));
    }
}
