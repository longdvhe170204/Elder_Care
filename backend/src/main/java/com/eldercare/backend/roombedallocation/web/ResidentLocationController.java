package com.eldercare.backend.roombedallocation.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.roombedallocation.web.response.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import org.springframework.web.bind.annotation.*;

/** T027: vue hiện tại và tương lai theo quyền đọc phân bổ. */
@RestController
public class ResidentLocationController {
    private final com.eldercare.backend.roombedallocation.internal.allocation.service.ResidentLocationQueries reading;
    private final AccountApi accounts;

    public ResidentLocationController(
            com.eldercare.backend.roombedallocation.internal.allocation.service.ResidentLocationQueries reading,
            AccountApi accounts) {
        this.reading = reading;
        this.accounts = accounts;
    }

    @GetMapping("/api/v1/residents/{residentId}/location")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Thành công",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ResidentLocationResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = StructureProblem.class)))
    })
    public ResidentLocationResponse location(@PathVariable long residentId) {
        var result = reading.location(residentId);
        var active = result.currentAllocation();
        var future = result.futureAllocation();
        return new ResidentLocationResponse(
                result.location() == null ? null : LocationResponse.of(result.location()),
                active == null ? null : BedAllocationResponse.of(active, accounts),
                future == null ? null : BedAllocationResponse.of(future, accounts),
                result.withoutBedSince());
    }
}
