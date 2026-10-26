package com.cambae.backend.m09.shift.web;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.api.StaffStatisticsApi;
import com.cambae.backend.m09.shift.internal.service.PostPublicationService;
import com.cambae.backend.m09.shift.web.request.ShiftRequests;
import com.cambae.backend.m09.shift.web.response.ShiftResponses;
import com.cambae.backend.m09.web.response.M09ProblemBody;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** T041: bốn lệnh đúng hợp đồng; khóa chống lặp được xử lý ở bộ lọc dùng chung. */
@RestController
@RequestMapping("/api/v1")
class PostPublicationController {
    private final PostPublicationService commands;
    private final StaffStatisticsApi statistics;
    private final ObjectProvider<CurrentUser> users;
    private final BusinessTime time;

    PostPublicationController(
            PostPublicationService commands,
            StaffStatisticsApi statistics,
            ObjectProvider<CurrentUser> users,
            BusinessTime time) {
        this.commands = commands;
        this.statistics = statistics;
        this.users = users;
        this.time = time;
    }

    @PostMapping("/shifts/{id}/add-staff")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    ShiftResponses.Result add(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ShiftRequests.AddStaff body) {
        return new ShiftResponses.Result(
                null,
                ShiftResponses.warnings(commands.addStaff(
                        id,
                        body.staffId(),
                        Boolean.TRUE.equals(body.confirmWarnings()),
                        body.reason(),
                        commands.target(id))));
    }

    @PostMapping("/shifts/{id}/change-lead")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    void lead(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ShiftRequests.ChangeLead body) {
        commands.changeLead(id, body.staffId(), body.reason(), commands.target(id));
    }

    @PostMapping("/shifts/{id}/record-absence")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
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
    void absence(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody ShiftRequests.RecordAbsence body) {
        commands.recordAbsence(id, body.staffId(), body.replacementLeadStaffId(), body.reason(), commands.target(id));
    }

    @PostMapping("/shifts/{id}/cancel")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    void cancel(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody ShiftRequests.ReasonBody body) {
        commands.cancel(id, body.reason(), commands.target(id));
    }

    @GetMapping("/duty-basis-audit-logs")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    List<ShiftResponses.DutyBasis> history(
            @RequestParam(required = false) Long floorAreaId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        if (!users.getObject().roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Chỉ Quản lý viện tra cứu căn cứ nhiệm vụ");
        var scope = floorAreaId == null
                ? ScopeFilter.FACILITY
                : new ScopeFilter(false, Set.of(floorAreaId), Set.of(), Set.of());
        var start = from == null ? time.startOfDay(time.today()) : from;
        var end = to == null ? time.startOfDay(time.today().plusDays(1)) : to;
        if (!start.isBefore(end))
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Thời điểm kết thúc phải sau thời điểm bắt đầu");
        return statistics
                .nonRoleBases(scope, start, end)
                .map(r -> new ShiftResponses.DutyBasis(
                        r.auditId(), r.targetType(), r.targetId(), r.staffId(), r.basis(), r.at()))
                .toList();
    }
}
