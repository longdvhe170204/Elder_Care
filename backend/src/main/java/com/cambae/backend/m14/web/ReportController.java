package com.cambae.backend.m14.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m14.internal.detail.service.MetricDetailService;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters.CostMode;
import com.cambae.backend.m14.internal.report.domain.ReportGroup;
import com.cambae.backend.m14.internal.report.service.ReportService;
import com.cambae.backend.m14.internal.scope.service.ReportPeriods;
import com.cambae.backend.m14.web.response.MetricDetailBody;
import com.cambae.backend.m14.web.response.ReportBody;
import com.cambae.backend.m15.account.api.CurrentUser;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Báo cáo một nhóm và danh sách chi tiết của một con số theo contracts/api-016.yaml (thẻ {@code reports}; 016 T014, T022
 * phần khung). {@code date} là báo cáo tại một ngày (từ ngày đó tới ngày đó).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "reports")
public class ReportController {

    private final ReportService reports;
    private final ReportPeriods periods;
    private final MetricDetailService details;
    private final CurrentUser user;

    public ReportController(
            ReportService reports, ReportPeriods periods, MetricDetailService details, CurrentUser user) {
        this.reports = reports;
        this.periods = periods;
        this.details = details;
        this.user = user;
    }

    @GetMapping("/reports/{group}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ReportBody report(
            @PathVariable ReportGroup group,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) List<Long> floorAreaId,
            @RequestParam(required = false) String residenceType,
            @RequestParam(required = false) Long careLevelId,
            @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) Long shiftId,
            @RequestParam(required = false) CostMode costMode) {
        ReportFilters filters = new ReportFilters(
                floorAreaId == null ? null : Set.copyOf(floorAreaId),
                residenceType,
                careLevelId,
                residentId,
                staffId,
                shiftId,
                costMode);
        LocalDate start = date != null ? date : from;
        LocalDate end = date != null ? date : to;
        return ReportBody.of(reports.period(user, group, periods.check(start, end), filters));
    }

    @GetMapping("/metric-details/{metricCode}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    MetricDetailBody details(
            @PathVariable String metricCode,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String breakdownKey,
            @RequestParam(required = false) OffsetDateTime asOf,
            @RequestParam(required = false) Integer page) {
        return MetricDetailBody.of(details.details(user, metricCode, from, to, breakdownKey, page == null ? 1 : page));
    }
}
