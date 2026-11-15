package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.session.service.LateReturnConsentService;
import com.eldercare.backend.care.web.request.ActivityReasonWrite;
import com.eldercare.backend.care.web.request.LateReturnConsentWrite;
import com.eldercare.backend.care.web.request.LateReturnConsentWrite.Portal;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.common.error.InvalidValueException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** FR-016a: lệnh ghi và rút đồng ý về muộn theo hai kênh của api-014. */
@RestController
@RequestMapping("/api/v1")
public class LateReturnConsentController {
    private final LateReturnConsentService consents;

    public LateReturnConsentController(LateReturnConsentService consents) {
        this.consents = consents;
    }

    @PostMapping("/late-return-consents")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void recordSigned(@RequestHeader("Idempotency-Key") String key, @RequestBody LateReturnConsentWrite body) {
        requireIds(body.residentId(), body.sessionId());
        consents.create(
                consents.target(body.sessionId(), body.residentId()),
                body.sessionId(),
                body.residentId(),
                body.representativeId(),
                body.signedFileId(),
                false);
    }

    @PostMapping("/late-return-consents/{id}/withdraw")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void withdrawSigned(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ActivityReasonWrite body) {
        consents.withdraw(consents.target(id), id, body.reason(), false);
    }

    @PostMapping("/family-portal/late-return-consents")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void recordFromPortal(@RequestHeader("Idempotency-Key") String key, @RequestBody Portal body) {
        requireIds(body.residentId(), body.sessionId());
        consents.create(
                consents.target(body.sessionId(), body.residentId()),
                body.sessionId(),
                body.residentId(),
                null,
                null,
                true);
    }

    @PostMapping("/family-portal/late-return-consents/{id}/withdraw")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public void withdrawFromPortal(@PathVariable long id, @RequestHeader("Idempotency-Key") String key) {
        consents.withdraw(consents.target(id), id, null, true);
    }

    private static void requireIds(Long residentId, Long sessionId) {
        if (residentId == null || residentId <= 0 || sessionId == null || sessionId <= 0)
            throw new InvalidValueException("Cần mã người cao tuổi và mã buổi");
    }
}
