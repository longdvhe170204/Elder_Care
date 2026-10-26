package com.cambae.backend.m02.internal.death.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.api.PostDeathTaskApi;
import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import com.cambae.backend.m02.api.dto.PostDeathItemStatusView;
import com.cambae.backend.m02.api.dto.PostDeathTaskView;
import com.cambae.backend.m02.api.spi.PostDeathItemPort;
import com.cambae.backend.m02.internal.death.domain.DeathRecord;
import com.cambae.backend.m02.internal.death.domain.PostDeathTask;
import com.cambae.backend.m02.internal.death.persistence.DeathStore;
import com.cambae.backend.m02.internal.ending.service.ResidencyEndingService;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Danh sách việc bắt buộc sau qua đời (004 US8; FR-071, FR-072, FR-071a; research T-13), tên logic {@code
 * DanhSachViecSauQuaDoiApi}. Trạng thái từng mục lấy từ {@link PostDeathItemPort} của module sở hữu và tính lại khi mở danh
 * sách, khi module sở hữu gọi {@link #refresh}, khi job nhắc chạy; kết quả khác bản đã ghi thì ghi lại. Mọi mục Hoàn thành
 * hoặc Không áp dụng thì đặt dấu Đã đóng của hồ sơ lưu trú; dấu không gỡ được và danh sách đóng băng từ đó (FR-072, 013
 * FR-026a). Module chưa cài port cho một mục thì mục đó Chưa hoàn thành: không đóng hồ sơ khi chưa biết.
 */
@Service
public class PostDeathTaskService implements PostDeathTaskApi {

    public static final String ENTITY_TYPE = "M02.DEATH_RECORD";

    static final AuditAction CLOSED = new AuditAction("M02_POST_DEATH_CLOSED", false);
    static final AuditAction ITEM_CHANGED = new AuditAction("M02_POST_DEATH_ITEM_CHANGED", false);

    private static final String NO_PORT = "Module sở hữu mục này chưa công bố trạng thái";

    private final DeathStore store;
    private final ObjectProvider<PostDeathItemPort> ports;
    private final ObjectProvider<ApprovalRequestApi> approvals;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;
    private final BusinessTime time;

    PostDeathTaskService(
            DeathStore store,
            ObjectProvider<PostDeathItemPort> ports,
            ObjectProvider<ApprovalRequestApi> approvals,
            ResidencyLock residencyLock,
            AuditLogApi auditLog,
            BusinessTime time) {
        this.store = store;
        this.ports = ports;
        this.approvals = approvals;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
        this.time = time;
    }

    @Override
    @Transactional
    public void refresh(long residentId) {
        store.ofResident(residentId).ifPresent(record -> recalculate(record));
    }

    @Override
    @Transactional
    public PostDeathTaskView view(long residentId) {
        return recalculate(store.ofResident(residentId).orElseThrow(NotFoundException::new));
    }

    /** Danh sách cho màn web (Phụ lục 27 dòng "Kết thúc lưu trú, qua đời"). */
    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.VIEW)
    @Transactional
    public PostDeathTaskView list(Target target, long residentId) {
        return view(residentId);
    }

    /**
     * Tính lại và ghi các mục đổi; đặt dấu Đã đóng khi mọi mục xong. Dấu đã có thì chỉ đọc lại bản đã ghi (FR-072). Chạy dưới
     * khóa {@code resident_residency} của người cao tuổi.
     */
    @Transactional
    public PostDeathTaskView recalculate(DeathRecord record) {
        long residentId = record.residentId();
        residencyLock.lock(residentId);
        Map<PostDeathItemCode, PostDeathTask> stored = new EnumMap<>(PostDeathItemCode.class);
        store.tasks(record.id()).forEach(task -> stored.put(task.code(), task));
        Instant closedAt = store.closedAt(residentId).orElse(null);
        if (closedAt != null) {
            return viewOf(stored, closedAt);
        }
        // DATETIME(6): cắt về micro giây để giá trị trả ngay bằng giá trị đọc lại từ cơ sở dữ liệu
        Instant now = time.now().truncatedTo(ChronoUnit.MICROS);
        Map<PostDeathItemCode, PostDeathItemStatusView> current = current(residentId);
        for (PostDeathItemCode code : PostDeathItemCode.values()) {
            PostDeathItemStatusView fresh = current.get(code);
            PostDeathTask old = stored.get(code);
            if (old != null && old.status() == fresh.status() && java.util.Objects.equals(old.basis(), fresh.basis())) {
                continue;
            }
            store.upsertTask(record.id(), code, fresh.status(), fresh.basis(), now);
            stored.put(code, new PostDeathTask(record.id(), code, fresh.status(), fresh.basis(), now));
            if (old != null) {
                auditLog.write(AuditLogEntry.builder()
                        .action(ITEM_CHANGED)
                        .entity(ENTITY_TYPE, record.id())
                        .resident(residentId)
                        .before(Map.of(
                                "item", code.name(), "status", old.status().name()))
                        .after(Map.of(
                                "item", code.name(), "status", fresh.status().name()))
                        .build());
            }
        }
        boolean allDone = stored.values().stream().allMatch(PostDeathTask::done);
        if (allDone && store.close(residentId, now)) {
            closedAt = now;
            auditLog.write(AuditLogEntry.builder()
                    .action(CLOSED)
                    .entity(ENTITY_TYPE, record.id())
                    .resident(residentId)
                    .after(Map.of("closedAt", now.toString()))
                    .build());
        }
        return viewOf(stored, closedAt);
    }

    /**
     * Ngoại lệ cho mục quyết toán số dư và tiền cọc khi gia đình còn nợ (FR-071a, Q-387): Hành chính hoặc Kế toán lập yêu cầu
     * {@code M02.ENDING_EXCEPTION} căn cứ danh sách việc; Quản lý viện duyệt. Chỉ khi danh sách còn mở và mục quyết toán chưa
     * xong; mục khác không có ngoại lệ.
     */
    @BusinessCommand
    @RequiresPermission(function = Function.RESIDENCY_END_DEATH, operation = Operation.CREATE)
    @Transactional
    public ApprovalRequestView requestSettlementException(Target target, long residentId, Reason reason) {
        DeathRecord record = store.ofResident(residentId).orElseThrow(NotFoundException::new);
        PostDeathTaskView view = recalculate(record);
        if (view.closedAt() != null) {
            throw conflict(
                    "POST_DEATH_LIST_CLOSED", "Danh sách việc sau qua đời đã đóng, không còn mục để miễn (FR-072)");
        }
        boolean settlementOpen = view.items().stream()
                .anyMatch(i ->
                        i.code() == PostDeathItemCode.SETTLEMENT && i.status() == PostDeathItemStatus.NOT_COMPLETED);
        if (!settlementOpen) {
            throw conflict(
                    "POST_DEATH_SETTLEMENT_NOT_OPEN", "Mục quyết toán số dư và tiền cọc không còn Chưa hoàn thành");
        }
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("residentId", residentId);
        content.put("basis", ResidencyEndingService.POST_DEATH_BASIS);
        content.put("conditionCode", "END_H_SETTLEMENT");
        return approvals
                .getObject()
                .create(new CreateRequest(ResidencyEndingService.EXCEPTION_TYPE, content, time.today(), reason, true));
    }

    /** Mã bản ghi qua đời của người cao tuổi; không có thì 404. */
    @Transactional(readOnly = true)
    public long recordIdOf(long residentId) {
        return store.ofResident(residentId).orElseThrow(NotFoundException::new).id();
    }

    /** Danh sách còn mở và mục quyết toán chưa xong: điều kiện để ngoại lệ được duyệt, áp dụng (FR-071a). */
    @Transactional
    public boolean settlementExceptionAllowed(long residentId) {
        return store.ofResident(residentId)
                .map(this::recalculate)
                .filter(view -> view.closedAt() == null)
                .map(view -> view.items().stream()
                        .anyMatch(i -> i.code() == PostDeathItemCode.SETTLEMENT
                                && i.status() == PostDeathItemStatus.NOT_COMPLETED))
                .orElse(false);
    }

    private Map<PostDeathItemCode, PostDeathItemStatusView> current(long residentId) {
        Map<PostDeathItemCode, PostDeathItemStatusView> byCode = ports.orderedStream()
                .collect(Collectors.toMap(
                        PostDeathItemPort::code, port -> port.status(residentId), (first, second) -> first));
        Map<PostDeathItemCode, PostDeathItemStatusView> all = new EnumMap<>(PostDeathItemCode.class);
        for (PostDeathItemCode code : PostDeathItemCode.values()) {
            all.put(
                    code,
                    byCode.getOrDefault(code, new PostDeathItemStatusView(PostDeathItemStatus.NOT_COMPLETED, NO_PORT)));
        }
        return all;
    }

    private static PostDeathTaskView viewOf(Map<PostDeathItemCode, PostDeathTask> tasks, Instant closedAt) {
        List<PostDeathTaskView.Item> items = tasks.values().stream()
                .map(t -> new PostDeathTaskView.Item(t.code(), t.status(), t.basis()))
                .toList();
        return new PostDeathTaskView(items, closedAt);
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
