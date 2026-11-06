package com.eldercare.backend.family.web;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.family.internal.feedback.domain.Feedback;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Status;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Type;
import com.eldercare.backend.family.internal.feedback.service.FeedbackService;
import com.eldercare.backend.family.web.request.FeedbackCommandRequest;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest;
import com.eldercare.backend.family.web.request.SubmitFeedbackRequest.SenderType;
import com.eldercare.backend.family.web.response.AssignableStaffResponse;
import com.eldercare.backend.family.web.response.CreatedResponse;
import com.eldercare.backend.family.web.response.FeedbackDetailResponse;
import com.eldercare.backend.family.web.response.FeedbackResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
 * Phản hồi, khiếu nại (012 T030; contracts/api-012.yaml, tag {@code feedback}; US6, FR-058 → FR-066). Gửi trả {@code {id}}
 * dù hợp đồng không khai nội dung (như lệnh US1 → US4); lệnh trả chi tiết. Người gửi là người cao tuổi, loại Mong muốn,
 * lệnh {@code change-type}, {@code link-opinion} thuộc US9.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "feedback")
class FeedbackController {

    private static final String PROBLEM = "application/problem+json";

    private final FeedbackService feedback;

    FeedbackController(FeedbackService feedback) {
        this.feedback = feedback;
    }

    @GetMapping("/feedback")
    List<FeedbackResponse> list(
            @RequestParam(required = false) String status, @RequestParam(required = false) Boolean overdue) {
        Status filter;
        try {
            filter = status == null || status.isBlank() ? null : Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Trạng thái không hợp lệ: " + status);
        }
        return feedback.list(filter, overdue).stream().map(FeedbackResponse::of).toList();
    }

    @PostMapping("/feedback")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "OK", content = @Content)})
    ResponseEntity<CreatedResponse> submit(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody SubmitFeedbackRequest body) {
        long id = feedback.submit(new FeedbackService.Submission(
                body.residentId(),
                body.senderType() == SenderType.RESIDENT
                        ? Feedback.SenderType.RESIDENT
                        : Feedback.SenderType.FAMILY_MEMBER,
                body.familyMemberId(),
                Type.valueOf(body.type().name()),
                body.group(),
                body.wishTopic() == null
                        ? null
                        : Feedback.WishTopic.valueOf(body.wishTopic().name()),
                body.content(),
                body.fileIds()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedResponse(id));
    }

    @GetMapping("/feedback/{id}")
    FeedbackDetailResponse get(@PathVariable long id) {
        return FeedbackDetailResponse.of(feedback.get(id));
    }

    @GetMapping("/feedback/{id}/assignable-staff")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    List<AssignableStaffResponse> assignableStaff(@PathVariable long id) {
        return feedback.assignableStaff(id).stream()
                .map(AssignableStaffResponse::of)
                .toList();
    }

    @PostMapping("/feedback/{id}/{command}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "STATE_CHANGED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    FeedbackDetailResponse command(
            @PathVariable long id,
            @PathVariable
                    @Schema(
                            allowableValues = {
                                "start-handling",
                                "reply",
                                "add-note",
                                "reassign",
                                "change-priority",
                                "agree-to-close",
                                "reopen",
                                "change-type",
                                "link-opinion"
                            })
                    String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody(required = false) FeedbackCommandRequest body) {
        FeedbackCommandRequest b = body == null ? FeedbackCommandRequest.EMPTY : body;
        FeedbackService.CommandBody c = new FeedbackService.CommandBody(
                b.reason(),
                b.handlingDirection(),
                b.content(),
                b.assigneeId(),
                b.priority(),
                b.newType() == null ? null : Type.valueOf(b.newType().name()),
                b.opinion() == null ? null : b.opinion().itemId(),
                b.fileIds());
        var view = switch (command) {
            case "start-handling" -> feedback.startHandling(id);
            case "reply" -> feedback.reply(id, c);
            case "add-note" -> feedback.addNote(id, c);
            case "reassign" -> feedback.reassign(id, c);
            case "change-priority" -> feedback.changePriority(id, c);
            case "agree-to-close" -> feedback.agreeToClose(id);
            case "reopen" -> feedback.reopen(id, c);
            // Chỉ cho phản hồi loại Mong muốn của người cao tuổi (FR-058b, BR-M10-12)
            case "change-type" -> feedback.changeType(id, c);
            case "link-opinion" -> feedback.linkOpinion(id, c);
            default -> throw new NotFoundException();
        };
        return FeedbackDetailResponse.of(view);
    }
}
