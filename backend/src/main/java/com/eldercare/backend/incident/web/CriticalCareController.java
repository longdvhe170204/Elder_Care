package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.incident.internal.critical.domain.ContactChannel;
import com.eldercare.backend.incident.internal.critical.domain.ReconfirmationResult;
import com.eldercare.backend.incident.internal.critical.domain.WishReconfirmationCommand;
import com.eldercare.backend.incident.internal.critical.service.CriticalCareService;
import com.eldercare.backend.incident.web.request.ExecuteChoiceRequest;
import com.eldercare.backend.incident.web.request.IncidentReasonRequest;
import com.eldercare.backend.incident.web.request.RecordCriticalFlagRequest;
import com.eldercare.backend.incident.web.request.TransferOutBody;
import com.eldercare.backend.incident.web.request.WishReconfirmationRequest;
import com.eldercare.backend.incident.web.response.CriticalFlagRecordedResponse;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nguy kịch (tag {@code critical-condition} của api-007; US11, T046): ghi, xác nhận, gỡ dấu nguy kịch; kết quả xác nhận
 * lại nguyện vọng và lựa chọn thực hiện trên cảnh báo nguy kịch.
 */
@RestController
@RequestMapping("/api/v1")
public class CriticalCareController {
    private static final String PROBLEM = "application/problem+json";

    private final CriticalCareService critical;
    private final CurrentUser currentUser;

    public CriticalCareController(CriticalCareService critical, CurrentUser currentUser) {
        this.critical = critical;
        this.currentUser = currentUser;
    }

    @PostMapping("/residents/{residentId}/critical-flags")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Dấu và cảnh báo"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<CriticalFlagRecordedResponse> record(
            @PathVariable long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody RecordCriticalFlagRequest request) {
        var recorded = critical.record(residentId, request.type(), request.assessment(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CriticalFlagRecordedResponse(recorded.flagId(), recorded.alertId()));
    }

    @PostMapping("/critical-flags/{id}/confirm")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> confirm(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        critical.confirm(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/critical-flags/{id}/remove")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<Void> remove(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody IncidentReasonRequest request) {
        critical.remove(id, request.reason(), currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/alerts/{id}/wish-reconfirmation")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> reconfirm(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody WishReconfirmationRequest request) {
        var person = request.contactedPerson();
        var newWish = request.newWish();
        critical.reconfirm(
                id,
                new WishReconfirmationCommand(
                        person == null ? null : person.familyMemberId(),
                        person == null ? null : person.fullName(),
                        parse(ContactChannel.class, request.channel()),
                        request.contactedAt(),
                        parse(ReconfirmationResult.class, request.result()),
                        newWish == null ? null : newWish::command),
                currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/alerts/{id}/execute-choice")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> executeChoice(
            @PathVariable long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ExecuteChoiceRequest request) {
        critical.executeChoice(
                id, request.choice(), request.reason(), TransferOutBody.toCommand(request.transferOut()), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Giá trị ngoài enum để dịch vụ trả 422 thay vì lỗi đọc thân lệnh. */
    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Giá trị không hợp lệ: " + value);
        }
    }
}
