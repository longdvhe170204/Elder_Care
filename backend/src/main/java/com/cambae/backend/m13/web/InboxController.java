package com.cambae.backend.m13.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.internal.inbox.domain.InboxFilter;
import com.cambae.backend.m13.internal.inbox.service.InboxService;
import com.cambae.backend.m13.web.request.PushDeviceBody;
import com.cambae.backend.m13.web.response.InboxBody;
import com.cambae.backend.m13.web.response.NotificationBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Hộp thông báo, luồng SSE, thiết bị đẩy theo contracts/api-009.yaml (thẻ {@code inbox}; 009 T033, T034). Mọi tài khoản dùng
 * được, chỉ với thông báo của chính mình (bảng FR-039).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "inbox")
public class InboxController {

    private final InboxService inbox;

    public InboxController(InboxService inbox) {
        this.inbox = inbox;
    }

    @GetMapping("/me/notifications")
    InboxBody list(
            @RequestParam(required = false) NotificationLevel level,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) String cursor) {
        return InboxBody.of(inbox.list(new InboxFilter(level, unread, eventType, residentId), cursor));
    }

    @GetMapping("/me/notifications/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    NotificationBody open(@PathVariable long id) {
        return NotificationBody.of(inbox.open(id));
    }

    @PostMapping("/me/notifications/{id}/hide")
    @ApiResponse(responseCode = "200", description = "OK")
    ResponseEntity<Void> hide(
            @PathVariable long id, @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        inbox.hide(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiResponse(
            responseCode = "200",
            description = "text/event-stream",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE, schema = @Schema(type = "string")))
    SseEmitter stream() {
        return inbox.stream();
    }

    @PostMapping("/push-devices")
    @ApiResponse(responseCode = "200", description = "OK")
    ResponseEntity<Void> registerPushDevice(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody PushDeviceBody body) {
        inbox.registerPushDevice(body.app().name(), body.fcmToken());
        return ResponseEntity.ok().build();
    }
}
