package com.eldercare.backend.care;

import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.common.offline.SyncOutcome;
import com.eldercare.backend.common.offline.SyncResult;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * 005 T034: gọi handler ngoại tuyến như {@code SyncService} của 002 (đọc nội dung JSON theo payload chủ dự án duyệt
 * 2026-11-03, transaction riêng mỗi bản ghi, thời điểm thiết bị là {@code effectiveAt}); quyền tại thời điểm thiết bị đã
 * được 002 kiểm trước khi gọi.
 */
abstract class CareOfflineTestBase extends CareResultTestBase {
    static final String RESULT = "M04.CARE_TASK_RESULT";
    static final String AD_HOC = "M04.AD_HOC_RECORDING";

    @Autowired
    List<OfflineHandler<?>> offlineHandlers;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JsonMapper json;

    @SuppressWarnings("unchecked")
    OfflineHandler<Object> handler(String type) {
        return (OfflineHandler<Object>) offlineHandlers.stream()
                .filter(h -> h.recordType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    Object content(String type, String body) {
        var h = handler(type);
        var contentType = org.springframework.core.ResolvableType.forClass(h.getClass())
                .as(OfflineHandler.class)
                .getGeneric(0)
                .resolve(Object.class);
        return json.readValue(body, contentType);
    }

    String sync(String type, String body, Instant deviceAt, boolean permissionLost) {
        var h = handler(type);
        var value = content(type, body);
        h.validate(value);
        h.target(value);
        return tx.execute(s -> h.apply(
                value,
                permissionLost
                        ? new SyncOutcome(SyncResult.ACCEPTED_WITH_FLAG, "PERMISSION_LOST_BEFORE_SYNC")
                        : new SyncOutcome(SyncResult.ACCEPTED, null),
                deviceAt));
    }

    String resultBody(String action, String recording, Long reasonTypeId) {
        return "{\"taskId\":" + task + ",\"action\":\"" + action + "\""
                + (recording == null ? "" : ",\"recording\":" + recording)
                + (reasonTypeId == null ? "" : ",\"reasonTypeId\":" + reasonTypeId) + "}";
    }

    /** Dời khung công việc về quá khứ để thời điểm thực hiện, thiết bị nằm trước hiện tại. */
    void window(Instant from, Instant to) {
        jdbc.update(
                "UPDATE care_task SET planned_at=?,window_from=?,window_to=? WHERE id=?",
                local(from),
                local(from),
                local(to),
                task);
    }

    long activeReason() {
        return jdbc.queryForObject(
                "SELECT COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(attributes,'$.legacy.id')) AS UNSIGNED),id) FROM"
                        + " catalog_item WHERE catalog_type='M04.NON_PERFORMANCE_REASON_TYPE' AND status='ACTIVE'"
                        + " AND JSON_EXTRACT(attributes,'$.fixed')=false ORDER BY id LIMIT 1",
                Long.class);
    }

    /** Quản lý viện lập loại lý do mới; mục mới không có id cũ nên id mục cũng là {@code reasonTypeId}. */
    long newReason() throws Exception {
        String name = "Lý do ngoại tuyến " + TestUsers.nextId();
        asManager(() -> mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/v1/non-performance-reason-types")
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType("application/json")
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isCreated()));
        return jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M04.NON_PERFORMANCE_REASON_TYPE' AND name=?",
                Long.class,
                name);
    }

    void deactivateReason(long id) throws Exception {
        asManager(() -> mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/v1/non-performance-reason-types/" + id + "/deactivate")
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType("application/json")
                        .content("{\"reason\":\"Không dùng nữa\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isOk()));
    }

    interface Step {
        void run() throws Exception;
    }

    void asManager(Step step) throws Exception {
        user.set(new TestUsers(jdbc, time)
                .staff(com.eldercare.backend.common.Role.NURSING_HOME_MANAGER)
                .user());
        try {
            step.run();
        } finally {
            user.set(baseManager.user());
        }
    }
}
