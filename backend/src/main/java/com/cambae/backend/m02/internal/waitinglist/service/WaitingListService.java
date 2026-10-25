package com.cambae.backend.m02.internal.waitinglist.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.ResidentStatusApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.domain.ApplicationStatus;
import com.cambae.backend.m02.internal.application.persistence.AdmissionApplicationStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m02.internal.waitinglist.domain.Circumstance;
import com.cambae.backend.m02.internal.waitinglist.domain.CircumstanceStatus;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreBreakdown;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreCause;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreHistoryRow;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListStatus;
import com.cambae.backend.m02.internal.waitinglist.persistence.WaitingListStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestApi;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.CreateRequest;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hồ sơ chờ (004 US2; FR-007 → FR-011, FR-017, FR-018, BR-M02-03, BR-M02-10): Hành chính đưa lượt đăng ký Đang xử lý vào
 * danh sách, sửa tình huống đặc biệt, đề nghị điều chỉnh điểm, xác nhận còn nhu cầu, ghi nhận Từ chối và Hủy chờ; Quản lý
 * viện chỉ xem và duyệt. Mọi lệnh khóa {@code resident_residency} trước, rồi dòng hồ sơ chờ (research T-03). Từ chối và Hủy
 * chờ không tự đổi trạng thái: đi qua lệnh Hủy tiếp nhận của 001, 001 khóa người cao tuổi rồi gọi bean của 004
 * ({@link WaitingListTransition}). Ghi nhận đã liên hệ, từ chối giường, danh sách theo giường chờ 003 công bố giao diện.
 */
@Service
public class WaitingListService {

    public static final String ENTITY_TYPE = "M02.WAITING_LIST_ENTRY";

    /** Tình huống "từng lưu trú tại viện": đánh dấu sẵn ở Chờ xác nhận khi có hồ sơ trước Kết thúc lưu trú (FR-007). */
    public static final String PREVIOUSLY_RESIDED = "PREVIOUSLY_RESIDED";

    static final AuditAction CREATED = new AuditAction("M02_WAITING_ENTRY_CREATED", false);
    static final AuditAction CIRCUMSTANCES_UPDATED = new AuditAction("M02_WAITING_ENTRY_CIRCUMSTANCES_UPDATED", true);
    static final AuditAction CONFIRMED = new AuditAction("M02_WAITING_ENTRY_CONFIRMED", false);

    /** Hồ sơ chờ kèm tình huống, họ tên và giới tính của người cao tuổi (nguồn 001). */
    public record View(WaitingListEntry entry, List<Circumstance> circumstances, String fullName, Gender gender) {}

    /** Dòng lịch sử điểm kèm người đề nghị, người duyệt, lý do khi là điều chỉnh (FR-011). */
    public record HistoryView(ScoreHistoryRow row, Long proposedById, Long approverId, String reason) {}

    private final WaitingListStore store;
    private final AdmissionApplicationStore applications;
    private final ResidencyLock residencyLock;
    private final ResidentApi residents;
    private final CareLevelApi careLevels;
    private final ResidentStatusApi residentStatus;
    private final ScoreRecalculation scores;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<ApprovalRequestApi> approvals;

    WaitingListService(
            WaitingListStore store,
            AdmissionApplicationStore applications,
            ResidencyLock residencyLock,
            ResidentApi residents,
            CareLevelApi careLevels,
            ResidentStatusApi residentStatus,
            ScoreRecalculation scores,
            AuditLogApi auditLog,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            ObjectProvider<ApprovalRequestApi> approvals) {
        this.store = store;
        this.applications = applications;
        this.residencyLock = residencyLock;
        this.residents = residents;
        this.careLevels = careLevels;
        this.residentStatus = residentStatus;
        this.scores = scores;
        this.auditLog = auditLog;
        this.time = time;
        this.currentUser = currentUser;
        this.approvals = approvals;
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
    public View create(Target target, long applicationId, List<String> circumstanceCodes) {
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
        Set<String> codes = new LinkedHashSet<>(circumstanceCodes == null ? List.of() : circumstanceCodes);
        requireKnown(codes, Set.of());

        Optional<CareLevel> assessed = careLevels.current(residentId);
        CurrentUser user = currentUser.getObject();
        long id;
        try {
            id = store.insert(
                    applicationId,
                    residentId,
                    assessed.orElse(application.expectedCareLevel()),
                    assessed.isEmpty(),
                    new ScoreBreakdown(0, 0, 0, 0),
                    user.accountId(),
                    time.now());
        } catch (DuplicateKeyException e) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "WAITING_ENTRY_ALREADY_EXISTS",
                    "Người cao tuổi đã có hồ sơ chờ chưa ở trạng thái cuối (FR-007)");
        }
        codes.forEach(code -> store.insertCircumstance(id, code, CircumstanceStatus.CONFIRMED));
        if (!codes.contains(PREVIOUSLY_RESIDED) && hasEndedPreviousResidency(residentId)) {
            store.insertCircumstance(id, PREVIOUSLY_RESIDED, CircumstanceStatus.PENDING_CONFIRMATION);
        }
        applications.markWaiting(applicationId);
        WaitingListEntry created = scores.first(store.find(id).orElseThrow(), time.today());
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY_TYPE, id)
                .resident(residentId)
                .after(snapshot(created, store.circumstances(id)))
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

    /** Lịch sử điểm theo thứ tự thời gian, điều chỉnh kèm người đề nghị, người duyệt, lý do (FR-010, FR-011). */
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<HistoryView> history(Target target, long entryId) {
        store.find(entryId).orElseThrow(NotFoundException::new);
        List<HistoryView> views = new ArrayList<>();
        for (ScoreHistoryRow row : store.history(entryId)) {
            Optional<ApprovalRequestView> request = row.approvalRequestId() == null
                    ? Optional.empty()
                    : approvals.getObject().find(row.approvalRequestId());
            views.add(new HistoryView(
                    row,
                    request.map(ApprovalRequestView::requesterId).orElse(null),
                    request.map(ApprovalRequestView::decidedBy).orElse(null),
                    request.map(ApprovalRequestView::reason).orElse(null)));
        }
        return views;
    }

    /**
     * Sửa tình huống đặc biệt (FR-007): {@code codes} là toàn bộ tình huống sau khi sửa. Tình huống đang chờ xác nhận mà có
     * trong danh sách thì thành đã xác nhận; tình huống vắng mặt thì bỏ. Tính lại điểm ngay (FR-010).
     */
    @BusinessCommand
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.UPDATE)
    @Transactional
    public View updateCircumstances(Target target, long entryId, List<String> codes, Reason reason) {
        WaitingListEntry found = store.find(entryId).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        WaitingListEntry entry = lockOpen(entryId);
        List<Circumstance> before = store.circumstances(entryId);
        Map<String, CircumstanceStatus> existing = before.stream()
                .collect(Collectors.toMap(Circumstance::code, Circumstance::status, (a, b) -> a, LinkedHashMap::new));
        Set<String> wanted = new LinkedHashSet<>(codes == null ? List.of() : codes);
        requireKnown(wanted, existing.keySet());

        existing.forEach((code, status) -> {
            if (!wanted.contains(code)) {
                store.deleteCircumstance(entryId, code);
            } else if (status == CircumstanceStatus.PENDING_CONFIRMATION) {
                store.confirmCircumstance(entryId, code);
            }
        });
        wanted.stream()
                .filter(code -> !existing.containsKey(code))
                .forEach(code -> store.insertCircumstance(entryId, code, CircumstanceStatus.CONFIRMED));
        store.touch(entryId, time.now());
        WaitingListEntry updated =
                scores.rescore(store.find(entryId).orElseThrow(), time.today(), ScoreCause.CIRCUMSTANCES_CHANGED);
        auditLog.write(AuditLogEntry.builder()
                .action(CIRCUMSTANCES_UPDATED)
                .entity(ENTITY_TYPE, entryId)
                .resident(entry.residentId())
                .before(snapshot(entry, before))
                .after(snapshot(updated, store.circumstances(entryId)))
                .reason(reason)
                .build());
        return view(updated);
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

    /**
     * Lập đề nghị điều chỉnh điểm (FR-011): yêu cầu {@code M02.SCORE_ADJUSTMENT} gửi duyệt ngay, Quản lý viện duyệt; điểm chỉ
     * đổi khi yêu cầu được áp dụng ({@link ScoreAdjustmentRequestHandler}).
     */
    @BusinessCommand
    @RequiresPermission(function = Function.WAITING_LIST, operation = Operation.UPDATE)
    @Transactional
    public ApprovalRequestView proposeAdjustment(Target target, long entryId, int points, Reason reason) {
        WaitingListEntry entry = store.find(entryId).orElseThrow(NotFoundException::new);
        if (!entry.status().isOpen()) {
            throw closed();
        }
        if (points == 0) {
            throw new InvalidValueException("points phải khác 0");
        }
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("entryId", entryId);
        content.put("points", points);
        return approvals
                .getObject()
                .create(new CreateRequest(ScoreAdjustmentRequestHandler.TYPE, content, time.today(), reason, true));
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

    private boolean hasEndedPreviousResidency(long residentId) {
        return residents
                .previousProfileId(residentId)
                .filter(previous -> residents.status(previous) == ResidentStatus.RESIDENCY_ENDED)
                .isPresent();
    }

    private WaitingListEntry lockOpen(long entryId) {
        WaitingListEntry entry = store.lock(entryId).orElseThrow(NotFoundException::new);
        if (!entry.status().isOpen()) {
            throw closed();
        }
        return entry;
    }

    /** Mã tình huống phải có trong bảng CFG-M02-08, hoặc đã có sẵn trên hồ sơ (FR-007). */
    private void requireKnown(Set<String> codes, Set<String> alreadyOnEntry) {
        Set<String> catalog = scores.table().circumstanceCodes();
        List<String> unknown = codes.stream()
                .filter(code -> !catalog.contains(code) && !alreadyOnEntry.contains(code))
                .toList();
        if (!unknown.isEmpty()) {
            throw new InvalidValueException("Tình huống không có trong bảng CFG-M02-08: " + unknown);
        }
    }

    private static BusinessException closed() {
        return new BusinessException(
                HttpStatus.CONFLICT, "WAITING_ENTRY_CLOSED", "Hồ sơ chờ đã ở trạng thái cuối (FR-007)");
    }

    private View view(WaitingListEntry entry) {
        return new View(
                entry,
                store.circumstances(entry.id()),
                residents.identity(entry.residentId()).fullName(),
                residents.gender(entry.residentId()).orElse(null));
    }

    static Map<String, Object> snapshot(WaitingListEntry entry, List<Circumstance> circumstances) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", entry.status().name());
        map.put("careLevel", entry.careLevel().name());
        map.put("score", entry.score());
        map.put(
                "circumstances",
                circumstances.stream()
                        .map(c -> c.code() + ":" + c.status().name())
                        .toList());
        return map;
    }
}
