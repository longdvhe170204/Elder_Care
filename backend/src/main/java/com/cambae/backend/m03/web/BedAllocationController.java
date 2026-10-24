package com.cambae.backend.m03.web;

import com.cambae.backend.m03.internal.allocation.domain.AllocationStatus;
import com.cambae.backend.m03.internal.allocation.service.*;
import com.cambae.backend.m03.web.request.*;
import com.cambae.backend.m03.web.response.*;
import com.cambae.backend.m15.account.api.AccountApi;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** T027: endpoint Phase 4 đã dịch và duyệt; không serialize entity hoặc cột ngoài hợp đồng. */
@RestController
@RequestMapping("/api/v1/bed-allocations")
public class BedAllocationController {
    private final AllocationCommands commands;
    private final AllocationReading reading;
    private final AccountApi accounts;

    public BedAllocationController(AllocationCommands commands, AllocationReading reading, AccountApi accounts) {
        this.commands = commands;
        this.reading = reading;
        this.accounts = accounts;
    }

    @GetMapping
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                array = @ArraySchema(schema = @Schema(implementation = BedAllocationResponse.class)))),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public List<BedAllocationResponse> list(
            @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) Long bedId,
            @RequestParam(required = false) AllocationStatus status) {
        return reading.list(residentId, bedId, status).stream()
                .map(row -> BedAllocationResponse.of(row, accounts))
                .toList();
    }

    @GetMapping("/{id}")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = BedAllocationResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public BedAllocationResponse detail(@PathVariable long id) {
        return BedAllocationResponse.of(reading.detail(id), accounts);
    }

    @PostMapping
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Đã phân bổ",
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
                description = "Điều kiện chưa thỏa",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Thông tin không hợp lệ",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResponseEntity<BedAllocationResponse> create(@Valid @RequestBody BedAllocationWrite body) {
        long id = commands.create(body.residentId(), body.bedId(), body.startAt(), body.reason());
        return ResponseEntity.status(201).body(detail(id));
    }

    @PostMapping("/{id}/cancel")
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
                description = "Đã hủy",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = BedAllocationResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Điều kiện chưa thỏa",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public BedAllocationResponse cancel(@PathVariable long id, @Valid @RequestBody ReasonBody body) {
        commands.cancel(id, body.reason());
        return detail(id);
    }

    @GetMapping("/suitable-beds")
    public List<SuitableBedResponse> suitable(
            @RequestParam long residentId,
            @RequestParam(required = false) Instant startAt,
            @RequestParam(required = false) Long fromAllocationId,
            @RequestParam(defaultValue = "false") boolean infectionControl) {
        return reading.suitable(residentId, startAt, fromAllocationId, infectionControl).stream()
                .map(bed -> new SuitableBedResponse(
                        LocationResponse.of(bed.location()), null, bed.readyDeadline(), null, bed.state()))
                .toList();
    }
}
