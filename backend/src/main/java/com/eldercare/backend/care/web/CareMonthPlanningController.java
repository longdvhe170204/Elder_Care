package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.task.service.CareMonthPlanning;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.care.web.request.CareMonthRequest;
import com.eldercare.backend.care.web.response.CareMonthCopyResponse;
import com.eldercare.backend.care.web.response.CareMonthDraftResponse;
import com.eldercare.backend.care.web.response.CareMonthPublishResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.YearMonth;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Q-453 (mở lại Q-439, FR-054): Trưởng tầng lập công việc theo tháng, Nháp rồi Công bố. */
@RestController
@RequestMapping("/api/v1/care-tasks/month-drafts")
public class CareMonthPlanningController {
    private final CareMonthPlanning months;
    private final CareTaskService commands;

    public CareMonthPlanningController(CareMonthPlanning months, CareTaskService commands) {
        this.months = months;
        this.commands = commands;
    }

    @PostMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public CareMonthCopyResponse copy(
            @RequestHeader("Idempotency-Key") String key, @RequestBody CareMonthRequest body) {
        var result = months.copyFromPlans(
                CareMonthPlanning.floorTarget(body.floorAreaId()), body.floorAreaId(), YearMonth.parse(body.month()));
        return new CareMonthCopyResponse(result.created(), result.skipped(), result.shifts());
    }

    @GetMapping
    @ApiResponse(responseCode = "200", description = "OK")
    public List<CareMonthDraftResponse> drafts(@RequestParam long floorAreaId, @RequestParam String month) {
        return months.drafts(CareMonthPlanning.floorTarget(floorAreaId), floorAreaId, YearMonth.parse(month)).stream()
                .map(CareMonthDraftResponse::of)
                .toList();
    }

    @PostMapping("/publish")
    @ApiResponse(responseCode = "200", description = "OK")
    public CareMonthPublishResponse publish(
            @RequestHeader("Idempotency-Key") String key, @RequestBody CareMonthRequest body) {
        return new CareMonthPublishResponse(months.publish(
                CareMonthPlanning.floorTarget(body.floorAreaId()), body.floorAreaId(), YearMonth.parse(body.month())));
    }

    @PostMapping("/{id}/discard")
    @ApiResponse(responseCode = "200", description = "OK")
    public void discard(@PathVariable long id, @RequestHeader("Idempotency-Key") String key) {
        months.discard(commands.target(id), id);
    }
}
