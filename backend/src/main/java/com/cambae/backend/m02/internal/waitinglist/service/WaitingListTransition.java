package com.cambae.backend.m02.internal.waitinglist.service;

import static com.cambae.backend.m02.internal.waitinglist.service.WaitingListService.ENTITY_TYPE;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListStatus;
import com.cambae.backend.m02.internal.waitinglist.persistence.WaitingListStore;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Phần của hồ sơ chờ trong lệnh Hủy tiếp nhận của 001 (FR-002; research T-03), do {@code M02StatusParticipant} gọi trong
 * transaction của lệnh (001 đã khóa dòng hồ sơ người cao tuổi; ở đây khóa tiếp {@code resident_residency}, rồi hồ sơ chờ).
 * Lệnh đến từ hồ sơ chờ mang {@code waitingListEntryId} và {@code waitingListCommand}: Từ chối thành Từ chối, Hủy chờ thành
 * Hủy chờ. Lệnh đến từ 001 không mang hai trường đó: hồ sơ chờ chưa ở trạng thái cuối của người đó chuyển Hủy chờ, không gọi
 * lại 001. Giữ chỗ giường của hồ sơ Đã liên hệ trả về cho 003 khi 003 công bố giao diện (chưa có trên develop).
 */
@Component
public class WaitingListTransition {

    public static final String DECLINE = "DECLINE";
    public static final String CANCEL_WAITING = "CANCEL_WAITING";

    // Lý do của lệnh nằm ở lịch sử trạng thái và nhật ký của 001 (cùng historyId), nên dòng này không cần thêm lý do
    static final AuditAction DECLINED = new AuditAction("M02_WAITING_ENTRY_DECLINED", false);
    static final AuditAction CANCELLED = new AuditAction("M02_WAITING_ENTRY_CANCELLED", false);

    private final WaitingListStore store;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;

    WaitingListTransition(WaitingListStore store, ResidencyLock residencyLock, AuditLogApi auditLog) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelled(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        JsonNode details = result.details();
        boolean fromEntry = has(details, "waitingListEntryId");
        Optional<WaitingListEntry> found = fromEntry
                ? store.lock(details.get("waitingListEntryId").asLong())
                : store.lockOpenByResident(residentId);
        if (found.isEmpty() || !found.get().status().isOpen()) {
            return;
        }
        WaitingListEntry entry = found.get();
        if (entry.residentId() != residentId) {
            throw new InvalidValueException("details.waitingListEntryId không thuộc người cao tuổi này");
        }
        boolean declined =
                fromEntry && DECLINE.equals(details.get("waitingListCommand").asString());
        WaitingListStatus next = declined ? WaitingListStatus.DECLINED : WaitingListStatus.WAITING_CANCELLED;
        store.updateStatus(entry.id(), next);
        auditLog.write(AuditLogEntry.builder()
                .action(declined ? DECLINED : CANCELLED)
                .entity(ENTITY_TYPE, entry.id())
                .resident(residentId)
                .before(Map.of("status", entry.status().name()))
                .after(Map.of("status", next.name()))
                .details(Map.of("historyId", result.historyId()))
                .build());
    }

    private static boolean has(JsonNode details, String field) {
        JsonNode node = details == null ? null : details.get(field);
        return node != null && !node.isNull();
    }
}
