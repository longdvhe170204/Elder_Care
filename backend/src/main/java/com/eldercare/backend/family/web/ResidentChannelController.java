package com.eldercare.backend.family.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.family.internal.feedback.domain.Feedback;
import com.eldercare.backend.family.internal.feedback.domain.Feedback.Type;
import com.eldercare.backend.family.internal.feedback.service.FeedbackService;
import com.eldercare.backend.family.internal.residentchannel.service.ResidentChannelService;
import com.eldercare.backend.family.web.request.ResidentChannelFeedbackRequest;
import com.eldercare.backend.family.web.response.CreatedResponse;
import com.eldercare.backend.family.web.response.FeedbackResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kênh người cao tuổi (012 T068; contracts/api-012.yaml, tag {@code resident-channel}; US9, FR-083 → FR-086): chỉ tài
 * khoản AC-16, không có mã người cao tuổi trong đường dẫn. Hợp đồng không khai nội dung 200 của năm phần; hình dạng ở
 * {@link ResidentChannelService}. Lệnh trên một phản hồi (Đồng ý đóng, Mở lại) đi qua {@code /feedback/{id}/{command}}.
 */
@RestController
@RequestMapping("/api/v1/resident-channel")
@Tag(name = "resident-channel")
class ResidentChannelController {

    private static final String PROBLEM = "application/problem+json";

    private final ResidentChannelService channel;
    private final FeedbackService feedback;

    ResidentChannelController(ResidentChannelService channel, FeedbackService feedback) {
        this.channel = channel;
        this.feedback = feedback;
    }

    @GetMapping("/today")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResidentChannelService.Today today() {
        return channel.today();
    }

    @GetMapping("/menu")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResidentChannelService.Section<Object> menu(@RequestParam(required = false) LocalDate week) {
        return channel.menu();
    }

    @GetMapping("/activities")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResidentChannelService.Section<ResidentChannelService.ChannelActivity> activities() {
        return channel.upcomingActivities();
    }

    @GetMapping("/visits")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResidentChannelService.Section<ResidentChannelService.ChannelVisit> visits() {
        return channel.upcomingVisits();
    }

    @GetMapping("/belongings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResidentChannelService.Section<ResidentChannelService.ChannelBelonging> belongings() {
        return channel.heldBelongings();
    }

    /** Phản hồi của chính người cao tuổi, kể cả được ghi thay (FR-058a, FR-086). */
    @GetMapping("/feedback")
    List<FeedbackResponse> feedback() {
        channel.self();
        return feedback.list(null, null).stream().map(FeedbackResponse::of).toList();
    }

    @PostMapping("/feedback")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "OK", content = @Content)})
    ResponseEntity<CreatedResponse> submit(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ResidentChannelFeedbackRequest body) {
        long residentId = channel.self();
        long id = feedback.submit(new FeedbackService.Submission(
                residentId,
                Feedback.SenderType.RESIDENT,
                null,
                Type.valueOf(body.type().name()),
                body.group(),
                body.wishTopic() == null
                        ? null
                        : Feedback.WishTopic.valueOf(body.wishTopic().name()),
                body.content(),
                List.of()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedResponse(id));
    }
}
