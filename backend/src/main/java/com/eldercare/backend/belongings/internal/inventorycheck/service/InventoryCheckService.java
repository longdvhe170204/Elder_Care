package com.eldercare.backend.belongings.internal.inventorycheck.service;

import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.INVENTORY_CHECK_CANCELLED;
import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.INVENTORY_CHECK_COMPLETED;
import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.INVENTORY_CHECK_CREATED;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommand;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import com.eldercare.backend.belongings.internal.belonging.domain.LockedBelonging;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingHandoverService;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingLocks;
import com.eldercare.backend.belongings.internal.belonging.service.ValuableTypes;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.HeldBelonging;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryCheck;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryCompletion;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryPeriod;
import com.eldercare.backend.belongings.internal.inventorycheck.persistence.InventoryCheckStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu kiểm kê đồ có giá trị ({@code KiemKeService}; 013 T025, FR-028a → FR-028c, BR-M12-07, Q-154, Q-306, Q-307;
 * research K-10): Hành chính chọn vị trí và lập, phiếu gồm mọi đồ có giá trị Đang giữ tại vị trí; hoàn thành ghi kết quả
 * từng đồ và sinh bản ghi bàn giao (Báo thất lạc cho Không tìm thấy, Ghi hư hỏng cho Lệch tình trạng chọn hư hỏng, Tìm
 * thấy cho đồ Thất lạc thấy tại vị trí) trong cùng transaction; kết quả Đúng không sinh bản ghi. Hệ thống không tạo
 * phiếu và không gợi ý vị trí.
 */
@Service
public class InventoryCheckService {

    static final String RECORD_TYPE = "M12.INVENTORY_CHECK";
    private static final String ENTITY = "M12.BELONGING";
    private static final Set<Role> READERS = Set.of(Role.NURSING_HOME_MANAGER, Role.OFFICE_STAFF);
    private static final Set<String> RESULTS =
            Set.of(InventoryCheck.MATCHED, InventoryCheck.CONDITION_MISMATCH, InventoryCheck.NOT_FOUND);

    /** Giá trị CFG-M12-04: chu kỳ và hạn hoàn thành. */
    record Schedule(Span cycle, Span completionDeadline) {}

    record Span(int value, String unit) {}

    /** Một phiếu kèm dấu trễ hạn tính lúc xem (Chờ kiểm kê quá hạn mang dấu trễ hạn, FR-028c). */
    public record CheckView(long id, String status, InventoryCheck content, boolean late, int version) {}

    /** Số đồ có giá trị Đang giữ của một vị trí và phiếu của kỳ nếu có (không xếp ưu tiên, FR-028a). */
    public record LocationSummary(long storageLocationId, int valuableCount, Long checkId) {}

    public record Overview(
            String periodCode, java.time.LocalDate dueDate, List<LocationSummary> locations, List<CheckView> checks) {}

    private final SimpleRecordApi records;
    private final CatalogApi catalogs;
    private final BelongingStore belongings;
    private final InventoryCheckStore store;
    private final BelongingLocks locks;
    private final BelongingHandoverService handovers;
    private final ValuableTypes valuableTypes;
    private final ParameterApi parameters;
    private final FileApi files;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    InventoryCheckService(
            SimpleRecordApi records,
            CatalogApi catalogs,
            BelongingStore belongings,
            InventoryCheckStore store,
            BelongingLocks locks,
            BelongingHandoverService handovers,
            ValuableTypes valuableTypes,
            ParameterApi parameters,
            FileApi files,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.records = records;
        this.catalogs = catalogs;
        this.belongings = belongings;
        this.store = store;
        this.locks = locks;
        this.handovers = handovers;
        this.valuableTypes = valuableTypes;
        this.parameters = parameters;
        this.files = files;
        this.audit = audit;
        this.events = events;
        this.time = time;
        this.users = users;
    }

    public InventoryPeriod currentPeriod() {
        Schedule schedule = parameters.read(ParameterCode.CFG_M12_04).asTable(Schedule.class);
        return InventoryPeriod.of(
                time.today(),
                schedule.cycle().value(),
                schedule.cycle().unit(),
                schedule.completionDeadline().value());
    }

    /**
     * Kỳ hiện tại (hoặc kỳ {@code periodCode}), các vị trí có đồ có giá trị Đang giữ theo mã vị trí và phiếu của kỳ
     * (FR-028a, FR-028c); không có thứ tự ưu tiên hay vị trí "nên lập".
     */
    @Transactional(readOnly = true)
    public Overview overview(String periodCode) {
        requireReader();
        InventoryPeriod current = currentPeriod();
        String code = periodCode == null || periodCode.isBlank() ? current.code() : periodCode;
        List<SimpleRecord<InventoryCheck>> checks = ofPeriod(code);
        boolean isCurrent = code.equals(current.code());
        List<LocationSummary> locations = new ArrayList<>();
        if (isCurrent) {
            Map<Long, Long> checkOf = new HashMap<>();
            checks.forEach(c -> checkOf.put(c.content().storageLocationId(), c.id()));
            store.countByLocation(valuableTypes.typeIds())
                    .forEach((location, count) ->
                            locations.add(new LocationSummary(location, count, checkOf.get(location))));
        }
        return new Overview(
                code,
                isCurrent
                        ? current.dueDate()
                        : checks.isEmpty() ? null : checks.getFirst().content().dueDate(),
                locations,
                checks.stream().map(this::view).toList());
    }

    @Transactional(readOnly = true)
    public CheckView get(long id) {
        requireReader();
        return view(records.require(InventoryCheck.KIND, id));
    }

    /** Lập phiếu cho một vị trí (Hành chính); mỗi (vị trí, kỳ) một phiếu, vị trí không có đồ có giá trị Đang giữ thì chặn. */
    @Transactional
    public CheckView create(long storageLocationId) {
        CurrentUser user = requireOffice();
        catalogs.require(BelongingCatalogs.STORAGE_LOCATION, storageLocationId);
        InventoryPeriod period = currentPeriod();
        String key = storageLocationId + ":" + period.code();
        if (records.findByUniqueKey(InventoryCheck.KIND, key).isPresent()) {
            throw conflict("INVENTORY_CHECK_ALREADY_EXISTS", "Vị trí đã có phiếu kiểm kê của kỳ " + period.code());
        }
        List<HeldBelonging> held = store.heldAt(storageLocationId, valuableTypes.typeIds());
        if (held.isEmpty()) {
            throw conflict("NO_VALUABLE_BELONGINGS", "Vị trí không có đồ có giá trị Đang giữ");
        }
        List<InventoryCheck.Line> lines = held.stream()
                .map(h -> new InventoryCheck.Line(
                        h.id(), h.residentId(), h.amount(), h.conditionLevelId(), null, null, null, null))
                .toList();
        InventoryCheck content = new InventoryCheck(
                storageLocationId,
                period.code(),
                period.dueDate(),
                period.overdueOn(time.today()),
                lines,
                List.of(),
                null,
                null,
                null);
        SimpleRecord<InventoryCheck> created = records.create(
                InventoryCheck.KIND,
                new NewSimpleRecord<>(
                        null, null, InventoryCheck.WAITING, content, null, null, key, user.accountId(), false));
        audit.write(AuditLogEntry.builder()
                .action(INVENTORY_CHECK_CREATED)
                .entity(RECORD_TYPE, created.id())
                .after(Map.of(
                        "storageLocationId", storageLocationId,
                        "periodCode", period.code(),
                        "belongings", lines.size()))
                .build());
        return view(created);
    }

    /** Hủy khi mọi đồ trong phiếu đã rời vị trí trước khi kiểm (FR-028b), có lý do. */
    @Transactional
    public CheckView cancel(long id, String reason) {
        CurrentUser user = requireOffice();
        if (reason == null || reason.isBlank()) {
            throw invalid("Hủy phiếu cần lý do");
        }
        SimpleRecord<InventoryCheck> record = records.lockForUpdate(InventoryCheck.KIND, id);
        requireWaiting(record);
        InventoryCheck check = record.content();
        boolean anyLeft = check.lines().stream().anyMatch(line -> stillThere(line.belongingId(), check));
        if (anyLeft) {
            throw conflict(
                    "INVENTORY_CHECK_HAS_BELONGINGS", "Còn đồ trong phiếu ở vị trí: hoàn thành phiếu thay vì hủy");
        }
        InventoryCheck cancelled = new InventoryCheck(
                check.storageLocationId(),
                check.periodCode(),
                check.dueDate(),
                check.late(),
                check.lines().stream()
                        .map(l -> l.completed(InventoryCheck.DROPPED, null, "Đã rời vị trí trước khi kiểm kê", null))
                        .toList(),
                List.of(),
                user.accountId(),
                time.now(),
                reason);
        SimpleRecord<InventoryCheck> saved = records.change(
                        InventoryCheck.KIND,
                        id,
                        record.version(),
                        new RecordChange<>(InventoryCheck.CANCELLED, cancelled, null, record.uniqueKey(), true))
                .after();
        audit.write(AuditLogEntry.builder()
                .action(INVENTORY_CHECK_CANCELLED)
                .entity(RECORD_TYPE, id)
                .after(Map.of("reason", reason))
                .build());
        return view(saved);
    }

    /**
     * Hoàn thành phiếu: kết quả cho mọi đồ còn Đang giữ tại vị trí; đồ đã đổi trạng thái hoặc vị trí sau khi lập bị loại
     * kèm ghi chú; bản ghi bàn giao sinh ra trong cùng transaction (lỗi thì cả lần hoàn thành rollback).
     */
    @Transactional
    public CheckView complete(long id, InventoryCompletion body) {
        CurrentUser user = requireOffice();
        SimpleRecord<InventoryCheck> record = records.lockForUpdate(InventoryCheck.KIND, id);
        requireWaiting(record);
        InventoryCheck check = record.content();
        Map<Long, InventoryCompletion.LineResult> results = new HashMap<>();
        Set<Long> inCheck = new LinkedHashSet<>();
        check.lines().forEach(l -> inCheck.add(l.belongingId()));
        for (InventoryCompletion.LineResult r : body.lines()) {
            if (!inCheck.contains(r.belongingId()) || results.put(r.belongingId(), r) != null) {
                throw invalid("Kết quả thuộc đồ không có trong phiếu hoặc lặp lại: " + r.belongingId());
            }
            if (r.result() == null || !RESULTS.contains(r.result())) {
                throw invalid("Kết quả phải là MATCHED, CONDITION_MISMATCH hoặc NOT_FOUND");
            }
        }
        TreeMap<Long, Set<Long>> idsByResident = new TreeMap<>();
        check.lines()
                .forEach(l -> idsByResident
                        .computeIfAbsent(l.residentId(), k -> new LinkedHashSet<>())
                        .add(l.belongingId()));
        Map<Long, Long> residentOfFound = new HashMap<>();
        for (long foundId : new LinkedHashSet<>(body.found())) {
            if (inCheck.contains(foundId)) {
                throw invalid("Đồ Tìm thấy không nằm trong phiếu: " + foundId);
            }
            long owner = belongings.residentOf(foundId).orElseThrow(NotFoundException::new);
            residentOfFound.put(foundId, owner);
            idsByResident.computeIfAbsent(owner, k -> new LinkedHashSet<>()).add(foundId);
        }
        Map<Long, LockedBelonging> locked = new HashMap<>();
        idsByResident.forEach(
                (resident, ids) -> locks.lockCurrent(resident, ids).forEach(b -> locked.put(b.id(), b)));

        String reason = "Phát hiện khi kiểm kê kỳ " + check.periodCode();
        Map<Long, Long> handoverOf = new HashMap<>();
        Map<Long, InventoryCheck.Line> done = new HashMap<>();
        for (InventoryCheck.Line line : check.lines()) {
            LockedBelonging b = locked.get(line.belongingId());
            if (b == null
                    || b.status() != BelongingStatus.HELD
                    || !Objects.equals(b.storageLocationId(), check.storageLocationId())) {
                done.put(
                        line.belongingId(),
                        line.completed(
                                InventoryCheck.DROPPED,
                                null,
                                "Đã đổi trạng thái hoặc vị trí sau khi lập phiếu"
                                        + (b == null ? "" : " (" + b.status() + ")"),
                                null));
                continue;
            }
            InventoryCompletion.LineResult r = results.get(line.belongingId());
            if (r == null) {
                throw invalid("Thiếu kết quả cho đồ " + line.belongingId() + " (FR-028b)");
            }
            if (InventoryCheck.CONDITION_MISMATCH.equals(r.result())) {
                if (r.newConditionLevelId() == null) {
                    throw invalid("Lệch tình trạng cần mức tình trạng mới");
                }
                catalogs.requireActive(BelongingCatalogs.CONDITION_LEVEL, r.newConditionLevelId());
            } else if (r.recordDamage()) {
                throw invalid("Chỉ Lệch tình trạng mới chuyển thành Ghi hư hỏng");
            }
        }
        for (Map.Entry<Long, Set<Long>> entry : idsByResident.entrySet()) {
            long resident = entry.getKey();
            Target target = new Target(ENTITY, null, resident, null);
            List<HandoverCommand.Item> lost = new ArrayList<>();
            List<HandoverCommand.Item> foundItems = new ArrayList<>();
            for (long itemId : entry.getValue()) {
                LockedBelonging b = locked.get(itemId);
                if (residentOfFound.containsKey(itemId)) {
                    BigDecimal amount = b.cashAmount() != null ? b.cashAmount() : b.quantity();
                    foundItems.add(
                            new HandoverCommand.Item(itemId, b.version(), amount, null, null, null, List.of(), null));
                    continue;
                }
                if (done.containsKey(itemId)) {
                    continue;
                }
                InventoryCompletion.LineResult r = results.get(itemId);
                switch (r.result()) {
                    case InventoryCheck.NOT_FOUND ->
                        lost.add(new HandoverCommand.Item(
                                itemId, b.version(), null, null, null, null, List.of(), r.incident()));
                    case InventoryCheck.CONDITION_MISMATCH -> {
                        if (r.recordDamage()) {
                            HandoverCommand command = new HandoverCommand(
                                    HandoverType.RECORD_DAMAGE,
                                    List.of(new HandoverCommand.Item(
                                            itemId,
                                            b.version(),
                                            null,
                                            null,
                                            r.newConditionLevelId(),
                                            r.newConditionDescription(),
                                            r.photoFileIds(),
                                            r.incident())),
                                    GiverType.STAFF,
                                    b.holderId(),
                                    null,
                                    check.storageLocationId(),
                                    null,
                                    reason,
                                    false,
                                    null,
                                    null);
                            handovers
                                    .inventoryHandover(target, command, id)
                                    .handovers()
                                    .forEach(c -> handoverOf.put(c.belongingId(), c.handoverId()));
                        }
                    }
                    default -> {}
                }
            }
            if (!lost.isEmpty()) {
                HandoverCommand command = new HandoverCommand(
                        HandoverType.REPORT_LOST, lost, null, null, null, null, null, reason, false, null, null);
                handovers
                        .inventoryHandover(target, command, id)
                        .handovers()
                        .forEach(c -> handoverOf.put(c.belongingId(), c.handoverId()));
            }
            if (!foundItems.isEmpty()) {
                HandoverCommand command = new HandoverCommand(
                        HandoverType.FOUND,
                        foundItems,
                        null,
                        null,
                        null,
                        check.storageLocationId(),
                        null,
                        null,
                        false,
                        null,
                        null);
                handovers.inventoryHandover(target, command, id);
            }
        }
        List<InventoryCheck.Line> lines = new ArrayList<>();
        for (InventoryCheck.Line line : check.lines()) {
            InventoryCheck.Line finished = done.get(line.belongingId());
            if (finished == null) {
                InventoryCompletion.LineResult r = results.get(line.belongingId());
                boolean mismatch = InventoryCheck.CONDITION_MISMATCH.equals(r.result());
                finished = line.completed(
                        r.result(),
                        mismatch ? r.newConditionLevelId() : null,
                        mismatch ? r.newConditionDescription() : null,
                        handoverOf.get(line.belongingId()));
            }
            lines.add(finished);
        }
        List<InventoryCheck.UnknownOwnerItem> unknown = new ArrayList<>();
        for (InventoryCheck.UnknownOwnerItem item : body.unknownOwnerItems()) {
            if (item.description() == null
                    || item.description().isBlank()
                    || item.photoFileIds().isEmpty()) {
                throw invalid("Đồ không rõ chủ cần mô tả và ảnh (FR-028b)");
            }
            item.photoFileIds().forEach(fileId -> files.attach(fileId, InventoryPhotoPermission.OWNER_TYPE, id));
            unknown.add(item);
        }
        Instant now = time.now();
        InventoryCheck completed = new InventoryCheck(
                check.storageLocationId(),
                check.periodCode(),
                check.dueDate(),
                check.late() || time.today().isAfter(check.dueDate()),
                lines,
                unknown,
                user.accountId(),
                now,
                null);
        SimpleRecord<InventoryCheck> saved = records.change(
                        InventoryCheck.KIND,
                        id,
                        record.version(),
                        new RecordChange<>(InventoryCheck.COMPLETED, completed, null, record.uniqueKey(), true))
                .after();
        if (!unknown.isEmpty()) {
            events.publishEvent(BelongingNotices.notice(
                    "INVENTORY_UNKNOWN_OWNER_ITEMS",
                    RECORD_TYPE,
                    id,
                    "INVENTORY_UNKNOWN_OWNER_ITEMS:" + id,
                    NotificationLevel.LOW,
                    null,
                    List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                    "Kiểm kê phát hiện " + unknown.size() + " đồ không rõ chủ: cần xem xét",
                    user.accountId(),
                    now,
                    false));
        }
        audit.write(AuditLogEntry.builder()
                .action(INVENTORY_CHECK_COMPLETED)
                .entity(RECORD_TYPE, id)
                .after(Map.of(
                        "periodCode", completed.periodCode(),
                        "late", completed.late(),
                        "records", handoverOf.size(),
                        "unknownOwnerItems", unknown.size()))
                .build());
        return view(saved);
    }

    List<SimpleRecord<InventoryCheck>> ofPeriod(String code) {
        return records
                .listByStatus(
                        InventoryCheck.KIND,
                        Set.of(InventoryCheck.WAITING, InventoryCheck.COMPLETED, InventoryCheck.CANCELLED))
                .stream()
                .filter(r -> code.equals(r.content().periodCode()))
                .toList();
    }

    private boolean stillThere(long belongingId, InventoryCheck check) {
        return belongings
                .find(belongingId)
                .filter(b -> b.status() == BelongingStatus.HELD
                        && Objects.equals(b.storageLocationId(), check.storageLocationId()))
                .isPresent();
    }

    private CheckView view(SimpleRecord<InventoryCheck> r) {
        boolean late = r.content().late()
                || (InventoryCheck.WAITING.equals(r.status())
                        && time.today().isAfter(r.content().dueDate()));
        return new CheckView(r.id(), r.status(), r.content(), late, r.version());
    }

    private void requireWaiting(SimpleRecord<InventoryCheck> record) {
        if (!InventoryCheck.WAITING.equals(record.status())) {
            throw conflict("INVENTORY_CHECK_NOT_PENDING", "Phiếu kiểm kê không còn Chờ kiểm kê");
        }
    }

    private CurrentUser requireOffice() {
        CurrentUser user = users.getObject();
        if (!user.roles().contains(Role.OFFICE_STAFF)) {
            throw new PermissionDeniedException("Chỉ Hành chính lập và hoàn thành phiếu kiểm kê (FR-028a, FR-029)");
        }
        return user;
    }

    private void requireReader() {
        if (users.getObject().roles().stream().noneMatch(READERS::contains)) {
            throw new PermissionDeniedException("Không có quyền xem phiếu kiểm kê");
        }
    }

    private static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", message);
    }
}
