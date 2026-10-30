package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.roombedallocation.internal.isolation.service.RoomIsolationService;
import com.eldercare.backend.roombedallocation.web.request.*;
import com.eldercare.backend.roombedallocation.web.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** T056: ba operation cách ly đã duyệt; quyền kiểm trong service. */
@RestController
@RequestMapping("/api/v1/rooms/{id}")
public class RoomIsolationController {
    private final RoomIsolationService service;
    private final AccountApi accounts;

    public RoomIsolationController(RoomIsolationService service, AccountApi accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    @PostMapping("/isolate")
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
                description = "Đã đặt",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = RoomResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Đã cách ly",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Lý do",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public RoomResponse isolate(@PathVariable long id, @RequestBody RoomIsolationWrite body) {
        return RoomResponse.of(service.isolate(id, body.reason(), body.incidentId()), true);
    }

    @PostMapping("/lift-isolation")
    @Operation(
            parameters =
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            schema = @Schema(type = "string")))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã gỡ", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Lý do",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public void lift(@PathVariable long id, @RequestBody ReasonBody body) {
        service.lift(id, body.reason());
    }

    @GetMapping("/isolation-history")
    public List<RoomIsolationHistoryRow> history(@PathVariable long id) {
        return service.history(id).stream()
                .map(x -> new RoomIsolationHistoryRow(
                        x.action(),
                        x.occurredAt(),
                        accounts.fullName(x.actorId()).orElse(null),
                        x.reason(),
                        x.incidentId()))
                .toList();
    }
}
