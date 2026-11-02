package com.eldercare.backend.admission.internal.waitinglist.service;

import static com.eldercare.backend.admission.internal.waitinglist.service.WaitingListService.ENTITY_TYPE;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.admission.internal.application.persistence.AdmissionApplicationStore;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.admission.internal.waitinglist.domain.BedContact;
import com.eldercare.backend.admission.internal.waitinglist.domain.BedContactResult;
import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListEntry;
import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListStatus;
import com.eldercare.backend.admission.internal.waitinglist.persistence.BedContactStore;
import com.eldercare.backend.admission.internal.waitinglist.persistence.WaitingListStore;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.HoldCancellationReason;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Phần của hồ sơ chờ trong lệnh Hủy tiếp nhận của 001 (FR-002; research T-03), do {@code M02StatusParticipant} gọi trong
 * transaction của lệnh (001 đã khóa dòng hồ sơ người cao tuổi; ở đây khóa tiếp theo người, rồi hồ sơ chờ).
 * Lệnh đến từ hồ sơ chờ mang {@code waitingListEntryId} và {@code waitingListCommand}: Từ chối thành Từ chối, Hủy chờ thành
 * Hủy chờ. Lệnh đến từ 001 không mang hai trường đó: hồ sơ chờ chưa ở trạng thái cuối của người đó chuyển Hủy chờ, không gọi
 * lại 001. Giữ chỗ giường của hồ sơ Đã liên hệ trả về cho 003 qua {@link BedAllocationCommandApi}; chưa có bản cài thì lệnh trả
 * 503 và rollback.
 */
@Component
public class WaitingListTransition {

    public static final String DECLINE = "DECLINE";
    public static final String CANCEL_WAITING = "CANCEL_WAITING";
    static final String BED_DECLINED_REASON = "BED_DECLINED";
    static final String ADMISSION_CANCELLED_REASON = "ADMISSION_CANCELLED";

    // Lý do của lệnh nằm ở lịch sử trạng thái và nhật ký của 001 (cùng historyId), nên dòng này không cần thêm lý do
    static final AuditAction DECLINED = new AuditAction("M02_WAITING_ENTRY_DECLINED", false);
    static final AuditAction CANCELLED = new AuditAction("M02_WAITING_ENTRY_CANCELLED", false);
    static final AuditAction HOLD_EXPIRED = new AuditAction("M02_WAITING_ENTRY_HOLD_EXPIRED", false);
    static final AuditAction ALLOCATED = new AuditAction("M02_WAITING_ENTRY_ALLOCATED", false);

    private final WaitingListStore store;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;
    private final BedContactStore contacts;
    private final AdmissionApplicationStore applications;
    private final ObjectProvider<BedAllocationCommandApi> beds;
    private final BusinessTime time;

    WaitingListTransition(
            WaitingListStore store,
            ResidencyLock residencyLock,
            AuditLogApi auditLog,
            BedContactStore contacts,
            AdmissionApplicationStore applications,
            ObjectProvider<BedAllocationCommandApi> beds,
            BusinessTime time) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
        this.contacts = contacts;
        this.applications = applications;
        this.beds = beds;
        this.time = time;
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
        if (entry.status() == WaitingListStatus.CONTACTED) {
            BedAllocationCommandApi api = beds.getIfAvailable();
            if (api == null) {
                throw WaitingListService.bedSourceUnavailable();
            }
            api.cancelHold(entry.id(), new HoldCancellationReason(ADMISSION_CANCELLED_REASON));
        }
        store.updateStatus(entry.id(), next, null);
        auditLog.write(AuditLogEntry.builder()
                .action(declined ? DECLINED : CANCELLED)
                .entity(ENTITY_TYPE, entry.id())
                .resident(residentId)
                .before(Map.of("status", entry.status().name()))
                .after(Map.of("status", next.name()))
                .details(Map.of("historyId", result.historyId()))
                .build());
    }

    /**
     * Quá hạn giữ chỗ (FR-015, BR-M02-02): một lần chạy của job 003 trả giường rồi gọi hàm này trong cùng transaction; hồ sơ về Đang
     * chờ, giữ ngày đăng ký và dấu Ưu tiên. Hồ sơ không còn Đã liên hệ thì bỏ qua.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void holdExpired(long entryId) {
        WaitingListEntry found = store.find(entryId).orElse(null);
        if (found == null) {
            return;
        }
        residencyLock.lock(found.residentId());
        WaitingListEntry entry = store.lock(entryId).orElseThrow();
        if (entry.status() != WaitingListStatus.CONTACTED) {
            return;
        }
        long bedId = entry.heldBedId();
        store.updateStatus(entryId, WaitingListStatus.WAITING, null);
        contacts.add(entryId, new BedContact(bedId, BedContactResult.EXPIRED, null, null), null, time.now());
        auditLog.write(AuditLogEntry.builder()
                .action(HOLD_EXPIRED)
                .entity(ENTITY_TYPE, entryId)
                .resident(entry.residentId())
                .before(Map.of("status", entry.status().name(), "heldBedId", bedId))
                .after(Map.of("status", WaitingListStatus.WAITING.name()))
                .build());
    }

    /**
     * Người cao tuổi được phân bổ giường (FR-016, data-model mục 3, 4): hồ sơ chờ chưa ở trạng thái cuối chuyển Đã tiếp nhận, lượt
     * đăng ký từ Đang chờ về Đang xử lý. Giữ chỗ khác (nếu có) do 003 kết thúc. Không có hồ sơ chờ thì bỏ qua.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void allocated(long residentId, long allocationId) {
        residencyLock.lock(residentId);
        Optional<WaitingListEntry> open = store.lockOpenByResident(residentId);
        if (open.isEmpty()) {
            return;
        }
        WaitingListEntry entry = open.get();
        store.updateStatus(entry.id(), WaitingListStatus.ADMITTED, null);
        applications.markBackInProgress(entry.applicationId());
        auditLog.write(AuditLogEntry.builder()
                .action(ALLOCATED)
                .entity(ENTITY_TYPE, entry.id())
                .resident(residentId)
                .before(Map.of("status", entry.status().name()))
                .after(Map.of("status", WaitingListStatus.ADMITTED.name()))
                .details(Map.of("allocationId", allocationId))
                .build());
    }

    private static boolean has(JsonNode details, String field) {
        JsonNode node = details == null ? null : details.get(field);
        return node != null && !node.isNull();
    }
}
