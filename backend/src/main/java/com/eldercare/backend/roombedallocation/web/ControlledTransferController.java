package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.roombedallocation.internal.isolation.service.ControlledTransferService;
import com.eldercare.backend.roombedallocation.web.request.*;
import com.eldercare.backend.roombedallocation.web.response.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Hai operation kiểm soát lây nhiễm đã duyệt. */
@RestController
@RequestMapping("/api/v1/bed-allocations/{id}")
public class ControlledTransferController {
    private final ControlledTransferService service;
    private final AccountApi accounts;

    public ControlledTransferController(ControlledTransferService service, AccountApi accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    @PostMapping("/infection-control-transfer")
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
                description = "Đã chuyển",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = BedTransferResult.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Bị chặn",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Bác sĩ hoặc lý do",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public BedTransferResult transfer(@PathVariable long id, @Valid @RequestBody InfectionControlTransferWrite body) {
        var value = service.transfer(id, body.newBedId(), body.prescribingDoctorId(), body.reason());
        return new BedTransferResult(
                BedAllocationResponse.of(value.previousAllocation(), accounts),
                BedAllocationResponse.of(value.newAllocation(), accounts),
                List.of());
    }

    @PostMapping("/confirm-isolated-stay")
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
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = BedAllocationResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Không có dấu chờ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public BedAllocationResponse confirm(@PathVariable long id, @RequestBody ReasonBody body) {
        return BedAllocationResponse.of(service.confirm(id, body.reason()), accounts);
    }
}
