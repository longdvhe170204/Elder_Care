package com.eldercare.backend.administration.account.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.internal.offline.service.SyncService;
import com.eldercare.backend.administration.account.web.request.SyncBody;
import com.eldercare.backend.administration.account.web.response.SyncRecordResultBody;
import com.eldercare.backend.administration.account.web.response.SyncResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
