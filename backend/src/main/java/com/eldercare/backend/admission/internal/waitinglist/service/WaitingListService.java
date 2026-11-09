package com.eldercare.backend.admission.internal.waitinglist.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.admission.internal.application.domain.AdmissionApplication;
import com.eldercare.backend.admission.internal.application.domain.ApplicationStatus;
import com.eldercare.backend.admission.internal.application.persistence.AdmissionApplicationStore;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.admission.internal.waitinglist.domain.BedContact;
import com.eldercare.backend.admission.internal.waitinglist.domain.BedContactResult;
import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListEntry;
import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListStatus;
import com.eldercare.backend.admission.internal.waitinglist.persistence.BedContactStore;
import com.eldercare.backend.admission.internal.waitinglist.persistence.WaitingListStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.CareLevelApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.HoldCancellationReason;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hồ sơ chờ (004 US2; FR-007 → FR-018, BR-M02-01, 02, 03, 10; Q-394): Hành chính đưa lượt đăng ký Đang xử lý vào danh sách, đánh
 * dấu Ưu tiên, ghi nhận đã liên hệ, từ chối giường, xác nhận còn nhu cầu, ghi nhận Từ chối và Hủy chờ. Quản lý viện chỉ xem. Mọi
 * lệnh khóa theo người trước, rồi dòng hồ sơ chờ (research T-03). Từ chối và Hủy chờ không tự đổi trạng thái: đi qua lệnh Hủy
 * tiếp nhận của 001, 001 khóa người cao tuổi rồi gọi {@link WaitingListTransition}. Giữ chỗ giường qua {@link
 * BedAllocationCommandApi} của 003.
 */
@Service
public class WaitingListService {

    public static final String ENTITY_TYPE = "M02.WAITING_LIST_ENTRY";

    static final AuditAction CREATED = new AuditAction("M02_WAITING_ENTRY_CREATED", false);
    static final AuditAction PRIORITY_MARKED = new AuditAction("M02_WAITING_ENTRY_PRIORITY_MARKED", true);
    static final AuditAction CONFIRMED = new AuditAction("M02_WAITING_ENTRY_CONFIRMED", false);
    static final AuditAction CONTACTED = new AuditAction("M02_WAITING_ENTRY_CONTACTED", false);
    static final AuditAction BED_DECLINED = new AuditAction("M02_WAITING_ENTRY_BED_DECLINED", true);

    /** Hồ sơ chờ kèm họ tên, giới tính (nguồn 001) và dấu "từng lưu trú tại viện" chỉ để hiển thị (FR-007, Q-307). */
    public record View(WaitingListEntry entry, String fullName, Gender gender, boolean previouslyResided) {}

    private final WaitingListStore store;
    private final BedContactStore contacts;
    private final AdmissionApplicationStore applications;
    private final MatchingEntryService matching;
    private final ResidencyLock residencyLock;
    private final ResidentApi residents;
    private final CareLevelApi careLevels;
    private final ResidentStatusApi residentStatus;
    private final ObjectProvider<BedAllocationCommandApi> beds;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    WaitingListService(
            WaitingListStore store,
            BedContactStore contacts,
            AdmissionApplicationStore applications,
            MatchingEntryService matching,
            ResidencyLock residencyLock,
            ResidentApi residents,
            CareLevelApi careLevels,
            ResidentStatusApi residentStatus,
            ObjectProvider<BedAllocationCommandApi> beds,
            AuditLogApi auditLog,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.contacts = contacts;
        this.applications = applications;
        this.matching = matching;
        this.residencyLock = residencyLock;
        this.residents = residents;
        this.careLevels = careLevels;
        this.residentStatus = residentStatus;
        this.beds = beds;
        this.auditLog = auditLog;
        this.time = time;
        this.currentUser = currentUser;
    }

    public static Target target(long entryId, long residentId) {
        return new Target(ENTITY_TYPE, String.valueOf(entryId), residentId, null);
    }

    /** Đối tượng kiểm quyền của một hồ sơ chờ; không có thì 404. */
    @Transactional(readOnly = true)
    public Target targetOf(long entryId) {
        WaitingListEntry entry = store.find(entryId).orElseThrow(NotFoundException::new);
        return target(entry.id(), entry.residentId());
    }

    /** Đối tượng kiểm quyền của lệnh đưa vào danh sách chờ: người cao tuổi của lượt, chưa có hồ sơ. */
    @Transactional(readOnly = true)
    public Target targetOfApplication(long applicationId) {
        AdmissionApplication application = applications.find(applicationId).orElseThrow(NotFoundException::new);
        return new Target(ENTITY_TYPE, null, application.residentId(), null);
    }

    /** Mã hồ sơ chờ mới nhất của một lượt đăng ký, để lượt hiện {@code waitingListEntryId}. */
    @Transactional(readOnly = true)
    public Optional<Long> entryIdOfApplication(long applicationId) {
        return store.findIdByApplication(applicationId);
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.CREATE)
    @Transactional
    public View create(Target target, long applicationId) {
        AdmissionApplication found = applications.find(applicationId).orElseThrow(NotFoundException::new);
        long residentId = found.residentId();
        residencyLock.lock(residentId);
        AdmissionApplication application = applications.lock(applicationId).orElseThrow(NotFoundException::new);
        if (application.status() != ApplicationStatus.IN_PROGRESS) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "APPLICATION_NOT_IN_PROGRESS",
                    "Chỉ đưa vào danh sách chờ lượt đăng ký Đang xử lý (FR-007)");
        }
        if (residents.status(residentId) != ResidentStatus.ADMISSION_IN_PROGRESS) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESIDENT_NOT_ADMISSION_IN_PROGRESS",
                    "Chỉ đưa người cao tuổi Đang tiếp nhận vào danh sách chờ (FR-007)");
        }
        Optional<CareLevel> assessed = careLevels.current(residentId);
        long id;
        try {
            id = store.insert(
                    applicationId,
                    residentId,
                    assessed.orElse(application.expectedCareLevel()),
                    assessed.isEmpty(),
                    currentUser.getObject().accountId(),
                    time.now());
        } catch (DuplicateKeyException e) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "WAITING_ENTRY_ALREADY_EXISTS",
                    "Người cao tuổi đã có hồ sơ chờ chưa ở trạng thái cuối (FR-007)");
        }
        applications.markWaiting(applicationId);
        WaitingListEntry created = store.find(id).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY_TYPE, id)
                .resident(residentId)
                .after(snapshot(created))
                .build());
        return view(created);
    }

    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<View> list(Target target, Set<WaitingListStatus> statuses) {
        return store.list(statuses).stream().map(this::view).toList();
    }

    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public View get(Target target, long entryId) {
        return view(store.find(entryId).orElseThrow(NotFoundException::new));
    }

    /** Danh sách phù hợp của một giường (FR-012, FR-013): không chọn sẵn, không giới hạn số hồ sơ. */
    public record Matching(long bedId, Instant vacancyStart, List<View> entries) {}

    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Matching matchingOf(Target target, long bedId) {
        MatchingEntryService.Result result = matching.matching(bedId);
        return new Matching(
                bedId,
                result.vacancyStart(),
                result.entries().stream().map(this::view).toList());
    }

    /** Đánh dấu hoặc gỡ dấu Ưu tiên, bắt buộc lý do, có nhật ký trước và sau (FR-009, BR-M02-10, Q-394). */
    @BusinessCommand
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.UPDATE)
    @Transactional
    public View markPriority(Target target, long entryId, boolean priority, Reason reason) {
        WaitingListEntry found = store.find(entryId).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        WaitingListEntry entry = lockOpen(entryId);
        store.updatePriority(entryId, priority, reason.value());
        WaitingListEntry updated = store.find(entryId).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(PRIORITY_MARKED)
                .entity(ENTITY_TYPE, entryId)
                .resident(entry.residentId())
                .before(snapshot(entry))
                .after(snapshot(updated))
                .reason(reason)
                .build());
        return view(updated);
    }

    /**
     * Ghi nhận đã liên hệ với một giường (FR-014, BR-M02-02): hồ sơ Đang chờ thỏa FR-012 với giường đó, giường chuyển Đang giữ chỗ
     * hạn CFG-M02-02 (003). Chọn hồ sơ không đứng đầu danh sách còn lại thì bắt buộc lý do, lưu ở lần liên hệ.
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.UPDATE)
    @Transactional
    public View recordContacted(Target target, long entryId, long bedId, Reason outOfOrderReason) {
        WaitingListEntry found = store.find(entryId).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        WaitingListEntry entry = store.lock(entryId).orElseThrow(NotFoundException::new);
        if (entry.status() != WaitingListStatus.WAITING) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "WAITING_ENTRY_NOT_WAITING", "Chỉ liên hệ hồ sơ Đang chờ (FR-014)");
        }
        BedAllocationCommandApi api = beds.getObject();
        MatchingEntryService.Result result = matching.matching(bedId);
        if (result.vacancyStart() == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "BED_NOT_EMPTY", "Giường không ở trạng thái Trống cần chọn hồ sơ (FR-012)");
        }
        List<WaitingListEntry> candidates = result.entries();
        if (candidates.stream().noneMatch(candidate -> candidate.id() == entryId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "ENTRY_NOT_MATCHING_BED",
                    "Hồ sơ không thỏa điều kiện của giường này (FR-012)");
        }
        boolean outOfOrder = candidates.getFirst().id() != entryId;
        if (outOfOrder && outOfOrderReason == null) {
            throw new InvalidValueException(
                    "outOfOrderReason bắt buộc khi chọn hồ sơ không đứng đầu danh sách (FR-014)");
        }
        Instant now = time.now();
        api.holdForWaitingRecord(entryId, entry.residentId(), bedId, now);
        try {
            store.updateStatus(entryId, WaitingListStatus.CONTACTED, bedId);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "BED_ALREADY_HELD", "Giường đang được giữ cho hồ sơ khác (FR-014)");
        }
        contacts.add(
                entryId,
                new BedContact(bedId, BedContactResult.CONTACTED, null, outOfOrder ? outOfOrderReason.value() : null),
                currentUser.getObject().accountId(),
                now);
        WaitingListEntry updated = store.find(entryId).orElseThrow();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("bedId", bedId);
        details.put("outOfOrder", outOfOrder);
        auditLog.write(AuditLogEntry.builder()
                .action(CONTACTED)
                .entity(ENTITY_TYPE, entryId)
                .resident(entry.residentId())
                .before(snapshot(entry))
                .after(snapshot(updated))
                .details(details)
                .build());
        return view(updated);
    }

    /** Gia đình từ chối giường này nhưng vẫn chờ: hồ sơ về Đang chờ, giữ ngày đăng ký và dấu Ưu tiên; giường về Trống (FR-012). */
    @BusinessCommand
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.UPDATE)
    @Transactional
    public void declineBed(Target target, long entryId, Reason reason) {
        WaitingListEntry found = store.find(entryId).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        WaitingListEntry entry = store.lock(entryId).orElseThrow(NotFoundException::new);
        if (entry.status() != WaitingListStatus.CONTACTED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "WAITING_ENTRY_NOT_CONTACTED", "Chỉ từ chối giường của hồ sơ Đã liên hệ");
        }
        long bedId = entry.heldBedId();
        beds.getObject().cancelHold(entryId, new HoldCancellationReason(WaitingListTransition.BED_DECLINED_REASON));
        store.updateStatus(entryId, WaitingListStatus.WAITING, null);
        contacts.add(
                entryId,
                new BedContact(bedId, BedContactResult.BED_DECLINED, reason.value(), null),
                currentUser.getObject().accountId(),
                time.now());
        auditLog.write(AuditLogEntry.builder()
                .action(BED_DECLINED)
                .entity(ENTITY_TYPE, entryId)
                .resident(entry.residentId())
                .before(snapshot(entry))
                .after(snapshot(store.find(entryId).orElseThrow()))
                .reason(reason)
                .build());
    }

    /** Xác nhận gia đình còn nhu cầu: đặt lại mốc CFG-M02-03 (FR-017). Chỉ hồ sơ Đang chờ. */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.UPDATE)
    @Transactional
    public void confirmStillNeeded(Target target, long entryId) {
        WaitingListEntry found = store.find(entryId).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        WaitingListEntry entry = store.lock(entryId).orElseThrow(NotFoundException::new);
        if (entry.status() != WaitingListStatus.WAITING) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "WAITING_ENTRY_NOT_WAITING",
                    "Chỉ xác nhận còn nhu cầu cho hồ sơ Đang chờ (FR-017)");
        }
        store.touch(entryId, time.now());
        auditLog.write(AuditLogEntry.builder()
                .action(CONFIRMED)
                .entity(ENTITY_TYPE, entryId)
                .resident(entry.residentId())
                .build());
    }

    /** Ghi nhận gia đình không tiếp tục chờ: Hủy tiếp nhận của 001 kèm lệnh Từ chối của hồ sơ (FR-002). */
    @BusinessCommand
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.UPDATE)
    @Transactional
    public void decline(Target target, long entryId, Reason reason) {
        cancelAdmission(entryId, reason, WaitingListTransition.DECLINE);
    }

    /** Hủy chờ: như Từ chối, hồ sơ chuyển Hủy chờ (FR-002). */
    @BusinessCommand
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.UPDATE)
    @Transactional
    public void cancelWaiting(Target target, long entryId, Reason reason) {
        cancelAdmission(entryId, reason, WaitingListTransition.CANCEL_WAITING);
    }

    private void cancelAdmission(long entryId, Reason reason, String waitingListCommand) {
        WaitingListEntry entry = store.find(entryId).orElseThrow(NotFoundException::new);
        if (!entry.status().isOpen()) {
            throw closed();
        }
        ObjectNode details = JsonNodeFactory.instance.objectNode();
        details.put("waitingListEntryId", entryId);
        details.put("waitingListCommand", waitingListCommand);
        residentStatus.execute(
                entry.residentId(),
                StatusCommand.CANCEL_ADMISSION,
                currentUser.getObject(),
                Optional.of(reason),
                details,
                null);
    }

    private WaitingListEntry lockOpen(long entryId) {
        WaitingListEntry entry = store.lock(entryId).orElseThrow(NotFoundException::new);
        if (!entry.status().isOpen()) {
            throw closed();
        }
        return entry;
    }

    private static BusinessException closed() {
        return new BusinessException(
                HttpStatus.CONFLICT, "WAITING_ENTRY_CLOSED", "Hồ sơ chờ đã ở trạng thái cuối (FR-007)");
    }

    private View view(WaitingListEntry entry) {
        return new View(
                entry,
                residents.identity(entry.residentId()).fullName(),
                residents.gender(entry.residentId()).orElse(null),
                hasEndedPreviousResidency(entry.residentId()));
    }

    /** FR-007: hiện "từng lưu trú tại viện" để Hành chính cân nhắc; hệ thống không tự đánh dấu (Q-307). */
    private boolean hasEndedPreviousResidency(long residentId) {
        return residents
                .previousProfileId(residentId)
                .filter(previous -> residents.status(previous) == ResidentStatus.RESIDENCY_ENDED)
                .isPresent();
    }

    static Map<String, Object> snapshot(WaitingListEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", entry.status().name());
        map.put("careLevel", entry.careLevel().name());
        map.put("priority", entry.priority());
        map.put("priorityReason", entry.priorityReason());
        map.put("heldBedId", entry.heldBedId());
        return map;
    }
}
