package com.cambae.backend.m02.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m02.internal.absence.service.DayCareService;
import com.cambae.backend.m02.web.request.DayCareAbsenceNoticeBody;
import com.cambae.backend.m02.web.request.DayCareClosureDayBody;
import com.cambae.backend.m02.web.request.ReasonBody;
import com.cambae.backend.m02.web.response.DayCareAbsenceNoticeResult;
import com.cambae.backend.m02.web.response.DayCareClosureDayView;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Báo vắng và ngày nghỉ của bán trú (004 US6; contracts/api-004.yaml, tag {@code absences}; FR-060, FR-060a). Mọi logic và
 * kiểm quyền ở service. Khai báo và hủy ngày nghỉ không trả nội dung (đúng hợp đồng); đọc lại qua {@code GET}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "absences")
class DayCareController {

    private static final String PROBLEM = "application/problem+json";

    private final DayCareService dayCare;

    DayCareController(DayCareService dayCare) {
        this.dayCare = dayCare;
    }

    @PostMapping("/day-care-absence-notices")
    @ApiResponse(responseCode = "201", description = "Đã ghi")
    ResponseEntity<DayCareAbsenceNoticeResult> recordNotice(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody DayCareAbsenceNoticeBody body) {
        boolean within = dayCare.recordAbsenceNotice(
                DayCareService.targetForResident(body.residentId()),
                body.residentId(),
                body.date(),
                body.sessionId(),
                body.familyNotifiedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(new DayCareAbsenceNoticeResult(within));
    }

    @GetMapping("/day-care-closure-days")
    List<DayCareClosureDayView> closures(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dayCare.closures(DayCareService.closureTarget(null), from, to).stream()
                .map(DayCareClosureDayView::of)
                .toList();
    }

    @PostMapping("/day-care-closure-days")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã khai báo"),
        @ApiResponse(
                responseCode = "409",
                description = "DAY_ALREADY_DECLARED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> declare(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody DayCareClosureDayBody body) {
        dayCare.declareClosure(DayCareService.closureTarget(null), body.date(), body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/day-care-closure-days/{id}/cancel")
    ResponseEntity<Void> cancel(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        dayCare.cancelClosure(DayCareService.closureTarget(id), id, body.reason());
        return ResponseEntity.ok().build();
    }
}
