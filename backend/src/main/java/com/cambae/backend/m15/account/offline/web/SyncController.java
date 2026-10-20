package com.cambae.backend.m15.account.offline.web;

import com.cambae.backend.common.offline.SyncResult;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.offline.internal.SyncService;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code POST /sync} (002 T058; contracts/api-002.yaml; FR-041a, FR-041c, BR-M15-20): đồng bộ lô bản ghi ngoại tuyến
 * của app nhân viên, trả kết quả từng bản ghi. Thiết bị là thiết bị của phiên ({@code X-Device-Id} khi đăng nhập).
 */
@RestController
@RequestMapping("/api/v1")
class SyncController {

    private final SyncService sync;
    private final ObjectProvider<CurrentUser> currentUser;
    private final JsonMapper json;

    SyncController(SyncService sync, ObjectProvider<CurrentUser> currentUser, JsonMapper json) {
        this.sync = sync;
        this.currentUser = currentUser;
        this.json = json;
    }

    record SyncRecordBody(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            UUID idempotencyKey,

            @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "M04.CARE_TASK_RESULT")
            String recordType,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Instant deviceTime,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Map<String, Object> content) {}

    record SyncBody(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Instant lastConnectedAt,

            @NotNull @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            List<SyncRecordBody> records) {}

    record SyncRecordResultBody(UUID idempotencyKey, SyncResult result, String reason, String reference) {}

    record SyncResponse(Boolean sessionEnded, List<Long> residentsInScope, List<SyncRecordResultBody> results) {}

    @PostMapping("/sync")
    @Tag(name = "offline")
    SyncResponse sync(@RequestHeader("X-Device-Id") UUID deviceId, @Valid @RequestBody SyncBody body) {
        SyncService.Report report = sync.sync(
                currentUser.getObject(),
                body.lastConnectedAt(),
                body.records().stream()
                        .map(r -> new SyncService.SyncRecord(
                                r.idempotencyKey().toString(),
                                r.recordType(),
                                r.deviceTime(),
                                json.writeValueAsString(r.content())))
                        .toList());
        return new SyncResponse(
                report.sessionEnded(),
                report.residentsInScope(),
                report.results().stream()
                        .map(r -> new SyncRecordResultBody(
                                UUID.fromString(r.idempotencyKey()), r.result(), r.reason(), r.reference()))
                        .toList());
    }
}
