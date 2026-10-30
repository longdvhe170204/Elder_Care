package com.eldercare.backend.notification.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.notification.internal.channel.service.ChannelService;
import com.eldercare.backend.notification.web.request.ChannelChangeBody;
import com.eldercare.backend.notification.web.request.ChannelChangeSchema;
import com.eldercare.backend.notification.web.response.NotificationChannelBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cấu hình kênh thông báo theo contracts/api-009.yaml (thẻ {@code configuration}; 009 T028): Quản lý viện; ETag của
 * danh sách kênh dùng cho {@code If-Match} khi đổi (412 khi đã có người đổi trước).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "configuration")
public class ChannelController {

    private final ChannelService channels;

    public ChannelController(ChannelService channels) {
        this.channels = channels;
    }

    @GetMapping("/notification-channels")
    @ApiResponse(responseCode = "200", description = "OK")
    ResponseEntity<List<NotificationChannelBody>> list() {
        ChannelService.Channels c = channels.list();
        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, c.etag())
                .body(c.channels().stream().map(NotificationChannelBody::of).toList());
    }

    @PutMapping("/notification-channels/{code}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "412",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> change(
            @PathVariable String code,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            content = @Content(schema = @Schema(implementation = ChannelChangeSchema.class)))
                    @RequestBody
                    ChannelChangeBody body) {
        ChannelService.Channels c =
                channels.change(code, ifMatch, body.enabled(), Optional.ofNullable(body.levels()), body.reason());
        return ResponseEntity.ok().header(HttpHeaders.ETAG, c.etag()).build();
    }
}
