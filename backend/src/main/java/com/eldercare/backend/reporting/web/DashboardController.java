package com.eldercare.backend.reporting.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.reporting.internal.dashboard.service.DashboardService;
import com.eldercare.backend.reporting.web.response.DashboardBody;
import com.eldercare.backend.reporting.web.response.MetricBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dashboard và danh mục chỉ tiêu theo contracts/api-016.yaml (thẻ {@code dashboard}, {@code reports}; 016 T009). ETag là
 * băm của khối và thông điệp (không gồm "tính đến"): web gọi lại mỗi 30 giây với {@code If-None-Match}, không đổi thì 304
 * (research O-06).
 */
@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboards;
    private final CurrentUser user;
    private final JsonMapper json;

    public DashboardController(DashboardService dashboards, CurrentUser user, JsonMapper json) {
        this.dashboards = dashboards;
        this.user = user;
        this.json = json;
    }

    @GetMapping("/dashboard")
    @Tag(name = "dashboard")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "304", description = "Không đổi", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<DashboardBody> dashboard(
            @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        DashboardBody body = DashboardBody.of(dashboards.dashboard(user));
        String etag = etag(body);
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(body);
    }

    @GetMapping("/metrics")
    @Tag(name = "reports")
    @ApiResponse(responseCode = "200", description = "OK")
    List<MetricBody> metrics() {
        return dashboards.metrics(user).stream().map(MetricBody::of).toList();
    }

    private String etag(DashboardBody body) {
        byte[] content = json.writeValueAsString(new DashboardBody(null, body.blocks(), body.message()))
                .getBytes(StandardCharsets.UTF_8);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);
            return "\"" + HexFormat.of().formatHex(hash, 0, 16) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
