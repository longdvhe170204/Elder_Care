package com.eldercare.backend.belongings.internal.belonging.service;

import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.BELONGING_HANDOVER;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.api.dto.HolderType;
import com.eldercare.backend.belongings.api.events.BelongingStatusChanged;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingSplit;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCheckResult;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommand;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommandCheck;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import com.eldercare.backend.belongings.internal.belonging.domain.LockedBelonging;
import com.eldercare.backend.belongings.internal.belonging.domain.NewBelonging;
import com.eldercare.backend.belongings.internal.belonging.domain.NewHandover;
import com.eldercare.backend.belongings.internal.belonging.domain.ReturnFacts;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.belongings.internal.consent.service.SelfCustodyConsentService;
import com.eldercare.backend.belongings.internal.incident.service.BelongingIncidents;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.staffing.api.ShiftApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh bàn giao đồ gửi ({@code BanGiaoService}; 013 T013; FR-011 → FR-015a, BR-M12-01, research K-03, K-04): Giao sử
 * dụng, Thu lại, Chuyển giữ, Báo thất lạc, Ghi hư hỏng, Tìm thấy theo bảng chuyển data-model mục 3. Mỗi đồ trong lệnh
 * tạo đúng một bản ghi (đếm thiếu thêm bản ghi Báo thất lạc cho phần thiếu); cột suy ra, tách đồ, đồng ý Hết hiệu lực,
 * nhật ký, thông báo cùng transaction. Báo thất lạc, Ghi hư hỏng ghi hoặc liên kết sự cố theo lựa chọn của người ghi
 * (FR-021, {@link BelongingIncidents}); Tìm thấy ghi diễn biến vào sự cố của lần Báo thất lạc (FR-025).
 */
@Service
public class BelongingHandoverService {

    static final String RECORD_TYPE = BelongingRecalculation.RECORD_TYPE;

    private static final Set<HandoverType> COMMANDS = EnumSet.of(
            HandoverType.GIVE_FOR_USE,
            HandoverType.TAKE_BACK,
            HandoverType.TRANSFER_CUSTODY,
            HandoverType.REPORT_LOST,
            HandoverType.RECORD_DAMAGE,
            HandoverType.FOUND);

    private static final Set<ResidentStatus> FINAL_STATES =
            EnumSet.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    /** FR-015a: đồng ý cho tự giữ hết hiệu lực khi đồ sang các trạng thái này. */
    private static final Set<BelongingStatus> CONSENT_ENDS = EnumSet.of(
            BelongingStatus.RETURNED, BelongingStatus.LOST, BelongingStatus.DAMAGED, BelongingStatus.DISPOSED);

    private static final Set<String> INVALID_CODES = Set.of("REASON_REQUIRED", "STORAGE_LOCATION_REQUIRED");

    /** Bản ghi đã tạo. */
    public record Created(long handoverId, long belongingId, HandoverType kind, BelongingStatus targetStatus) {}

    /** Kết quả lệnh: bản ghi đã tạo, các đồ sau lệnh (phiên bản mới cho lệnh kế tiếp). */
    public record Result(List<Created> handovers, List<BelongingView> belongings) {}

    private final BelongingLocks locks;
    private final BelongingStore belongings;
    private final BelongingHandoverStore handovers;
    private final BelongingRecalculation recalculation;
    private final ValuableTypes valuableTypes;
    private final CatalogApi catalogs;
    private final ResidentApi residents;
    private final ShiftApi shifts;
    private final AccountApi accounts;
    private final FileApi files;
    private final SelfCustodyConsentService consents;
    private final BelongingIncidents incidents;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    BelongingHandoverService(
            BelongingLocks locks,
            BelongingStore belongings,
            BelongingHandoverStore handovers,
            BelongingRecalculation recalculation,
            ValuableTypes valuableTypes,
            CatalogApi catalogs,
            ResidentApi residents,
            ShiftApi shifts,
            AccountApi accounts,
            FileApi files,
            SelfCustodyConsentService consents,
            BelongingIncidents incidents,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.locks = locks;
        this.belongings = belongings;
        this.handovers = handovers;
        this.recalculation = recalculation;
        this.valuableTypes = valuableTypes;
        this.catalogs = catalogs;
        this.residents = residents;
        this.shifts = shifts;
        this.accounts = accounts;
        this.files = files;
        this.consents = consents;
        this.incidents = incidents;
        this.audit = audit;
        this.events = events;
        this.time = time;
        this.users = users;
    }

    /** Hành chính; Điều dưỡng trong phạm vi: khóa "Đồ gửi" thao tác Sửa theo người cao tuổi chủ đồ (FR-029). */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.UPDATE)
    @Transactional
    public Result handover(Target resident, HandoverCommand command) {
        users.getObject().staffId().orElseThrow(() -> new PermissionDeniedException("Chỉ nhân viên bàn giao đồ gửi"));
        if (command.type() == null || !COMMANDS.contains(command.type())) {
            throw invalid("INVALID_VALUE", "Loại bàn giao không hợp lệ");
        }
        return execute(resident, command, null);
    }

    /** Người nhận của lệnh Trả đã được {@code ReturnService} xác minh (FR-016): loại, mã, họ tên và dữ kiện của lần trả. */
    public record ReturnInfo(GiverType recipientType, Long recipientId, String recipientName, ReturnFacts facts) {}

    /**
     * Lệnh Trả (FR-016 → FR-019): các đồ chuyển Đã trả, người nhận ghi ở bản ghi; gọi từ {@code ReturnService} trong
     * transaction của nó sau khi xác minh người nhận. Khóa, kiểm điều kiện từng đồ, ảnh, đồng ý Hết hiệu lực giống các
     * lệnh bàn giao khác.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Result returnItems(Target resident, HandoverCommand command, ReturnInfo info) {
        if (command.type() != HandoverType.RETURN) {
            throw invalid("INVALID_VALUE", "Loại bàn giao không hợp lệ");
        }
        return execute(resident, command, info);
    }

    private Result execute(Target resident, HandoverCommand command, ReturnInfo returnInfo) {
        CurrentUser user = users.getObject();
        long staffId = user.staffId().orElseThrow(() -> new PermissionDeniedException("Chỉ nhân viên bàn giao đồ gửi"));
        if (command.items().isEmpty()) {
            throw invalid("INVALID_VALUE", "Lệnh bàn giao cần ít nhất một đồ");
        }
        Map<Long, Integer> versions = new LinkedHashMap<>();
        for (HandoverCommand.Item item : command.items()) {
            if (versions.put(item.belongingId(), item.version()) != null) {
                throw invalid("INVALID_VALUE", "Một đồ chỉ xuất hiện một lần trong lệnh");
            }
            if (item.countedAmount() != null && command.type() == HandoverType.REPORT_LOST) {
                throw invalid("INVALID_VALUE", "Báo thất lạc không có số lượng đếm được");
            }
        }
        long residentId = resident.residentId();
        Map<Long, LockedBelonging> locked = new HashMap<>();
        for (LockedBelonging b : locks.lock(residentId, versions)) {
            locked.put(b.id(), b);
        }
        Long location = command.storageLocationId();
        if (location != null
                && catalogs.require(BelongingCatalogs.STORAGE_LOCATION, location)
                                .status()
                        != CatalogStatus.ACTIVE) {
            throw blocked("STORAGE_LOCATION_INACTIVE", "Vị trí lưu giữ đã ngừng hiệu lực");
        }
        if (command.recipientStaffId() != null
                && accounts.byStaff(command.recipientStaffId()).isEmpty()) {
            throw invalid("INVALID_VALUE", "Người nhận không phải nhân viên có tài khoản");
        }
        ResidentStatus residentStatus = residents.status(residentId);
        Context context = new Context(
                user,
                staffId,
                residentId,
                residentStatus,
                user.roles().contains(Role.OFFICE_STAFF),
                time.now(),
                command.at() == null ? time.now() : command.at(),
                returnInfo);

        List<Created> created = new ArrayList<>();
        Set<Long> intakes = new LinkedHashSet<>();
        Set<Long> touched = new LinkedHashSet<>();
        for (HandoverCommand.Item item : command.items()) {
            LockedBelonging b = locked.get(item.belongingId());
            intakes.add(belongings.intakeHandoverOf(b.id()).orElseThrow(NotFoundException::new));
            apply(context, command, item, b, created, touched);
        }
        for (long intake : intakes) {
            recalculation.verify(residentId, intake);
        }
        List<BelongingView> after = touched.stream()
                .map(id -> belongings.find(id).orElseThrow(NotFoundException::new))
                .toList();
        return new Result(created, after);
    }

    private record Context(
            CurrentUser user,
            long staffId,
            long residentId,
            ResidentStatus residentStatus,
            boolean officeStaff,
            Instant now,
            Instant at,
            ReturnInfo returnInfo) {

        boolean deceased() {
            return residentStatus == ResidentStatus.DECEASED;
        }
    }

    private void apply(
            Context ctx,
            HandoverCommand command,
            HandoverCommand.Item item,
            LockedBelonging b,
            List<Created> created,
            Set<Long> touched) {
        var type = catalogs.require(BelongingCatalogs.BELONGING_TYPE, b.typeId());
        boolean valuable = valuableTypes.isValuable(type.name());
        long levelId = item.conditionLevelId() == null ? b.conditionLevelId() : item.conditionLevelId();
        Integer order = null;
        boolean unusable = false;
        if (item.conditionLevelId() != null) {
            var level = catalogs.requireActive(BelongingCatalogs.CONDITION_LEVEL, levelId);
            order = level.sortOrder();
            unusable = level.attributes().unusable();
        }
        int currentOrder = catalogs.require(BelongingCatalogs.CONDITION_LEVEL, b.conditionLevelId())
                .sortOrder();

        GiverType giverType = command.giverType();
        Long giverId = command.giverId();
        if (giverType == null) {
            giverType = b.holderType() == HolderType.RESIDENT ? GiverType.RESIDENT : GiverType.STAFF;
            giverId = b.holderType() == HolderType.RESIDENT ? ctx.residentId() : ctx.staffId();
        } else if (giverType == GiverType.RESIDENT && giverId == null) {
            giverId = ctx.residentId();
        }
        boolean absenceConfirmed = command.holderAbsent()
                && b.holderType() == HolderType.STAFF
                && (!shifts.onShift(b.holderId(), ctx.now())
                        || accounts.byStaff(b.holderId())
                                .map(a -> a.status() != AccountStatus.ACTIVE)
                                .orElse(true));
        HandoverCommandCheck.Current current =
                new HandoverCommandCheck.Current(b.status(), b.holderType(), b.holderId(), b.version(), currentOrder);
        HandoverCheckResult check = HandoverCommandCheck.check(
                current,
                HandoverCommandCheck.Request.builder(command.type(), item.version())
                        .giver(giverType, giverId)
                        .officeStaff(ctx.officeStaff())
                        .holderAbsent(command.holderAbsent(), absenceConfirmed)
                        .storageLocationId(command.storageLocationId())
                        .reason(command.reason())
                        .conditionOrder(order)
                        .unusableLevel(unusable)
                        .conditionHandling(command.conditionHandling())
                        .photoCount(item.photoFileIds().size())
                        .valuable(valuable)
                        .requiresConsent(type.attributes().requiresConsentForUse())
                        .hasEffectiveConsent(consents.hasEffective(b.id()))
                        .residentInFinalState(FINAL_STATES.contains(ctx.residentStatus()))
                        .build());
        if (!check.allowed()) {
            throw violation(b.id(), check.violations());
        }
        HandoverType kind = check.effectiveType();
        BelongingStatus target = check.target();
        boolean cash = b.cashAmount() != null;
        BigDecimal currentAmount = cash ? b.cashAmount() : b.quantity();
        BelongingSplit.Plan plan;
        try {
            plan = BelongingSplit.plan(currentAmount, item.amount(), item.countedAmount());
        } catch (IllegalArgumentException e) {
            throw invalid("INVALID_VALUE", e.getMessage());
        }
        boolean carriesIncident = kind == HandoverType.REPORT_LOST || kind == HandoverType.RECORD_DAMAGE;
        if (wantsIncident(item) && !carriesIncident && plan.missing().signum() == 0) {
            throw invalid("INVALID_VALUE", "Chỉ Báo thất lạc, Ghi hư hỏng mới ghi hoặc liên kết sự cố (FR-021)");
        }
        if (plan.missing().signum() > 0) {
            // FR-013: phần đếm thiếu Báo thất lạc trong cùng lệnh, theo điều kiện của Báo thất lạc
            HandoverCheckResult lost = HandoverCommandCheck.check(
                    current,
                    HandoverCommandCheck.Request.builder(HandoverType.REPORT_LOST, item.version())
                            .reason(command.reason())
                            .build());
            if (!lost.allowed()) {
                throw violation(b.id(), lost.violations());
            }
        }

        GiverType recipientType = null;
        Long recipientId = null;
        Long holderLocation = null;
        switch (kind) {
            case GIVE_FOR_USE -> {
                recipientType = GiverType.RESIDENT;
                recipientId = ctx.residentId();
            }
            case REPORT_LOST -> {}
            case RETURN -> {
                recipientType = ctx.returnInfo().recipientType();
                recipientId = ctx.returnInfo().recipientId();
            }
            case TRANSFER_CUSTODY -> {
                recipientType = GiverType.STAFF;
                recipientId = command.recipientStaffId() != null
                        ? command.recipientStaffId()
                        : b.holderType() == HolderType.STAFF ? b.holderId() : ctx.staffId();
                holderLocation = command.storageLocationId();
                if (b.holderType() == HolderType.STAFF
                        && recipientId.equals(b.holderId())
                        && Objects.equals(holderLocation, b.storageLocationId())) {
                    throw invalid("INVALID_VALUE", "Chuyển giữ cần vị trí mới hoặc người giữ mới (FR-011)");
                }
            }
            default -> {
                recipientType = GiverType.STAFF;
                recipientId = command.recipientStaffId() != null ? command.recipientStaffId() : ctx.staffId();
                holderLocation = command.storageLocationId();
            }
        }
        GiverType recordedGiverType = giverType;
        Long recordedGiverId = giverId;
        if (command.holderAbsent()) {
            recordedGiverType = GiverType.STAFF;
            recordedGiverId = b.holderId();
        } else if (kind == HandoverType.FOUND) {
            recordedGiverType = null;
            recordedGiverId = null;
        }
        String moveNote = null;
        if (kind == HandoverType.RETURN && b.status() == BelongingStatus.IN_USE) {
            // FR-018: Trả từ Đang được người cao tuổi sử dụng ghi người bàn giao là nhân viên thực hiện
            recordedGiverType = GiverType.STAFF;
            recordedGiverId = ctx.staffId();
            moveNote = "Thu từ người cao tuổi";
        }
        Instant occurredAt =
                kind == HandoverType.REPORT_LOST && command.discoveredAt() != null ? command.discoveredAt() : ctx.at();

        BigDecimal originalAmount = plan.remaining().signum() > 0
                ? plan.remaining()
                : plan.moved().signum() > 0 ? plan.moved() : plan.missing();
        if (plan.moved().signum() > 0) {
            long movedId = plan.originalMoves()
                    ? b.id()
                    : split(ctx, b, plan.moved(), cash, target, recipientType, recipientId, holderLocation, levelId);
            long record = record(
                    ctx,
                    b,
                    movedId,
                    kind,
                    target,
                    recordedGiverType,
                    recordedGiverId,
                    recipientType,
                    recipientId,
                    occurredAt,
                    plan.moved(),
                    cash,
                    levelId,
                    item.conditionDescription(),
                    holderLocation,
                    command.reason(),
                    command.holderAbsent(),
                    valuable,
                    item.photoFileIds(),
                    moveNote,
                    created,
                    touched);
            if (carriesIncident) {
                attachIncident(ctx, item, kind, record, occurredAt, command.reason());
            }
            if (kind == HandoverType.FOUND) {
                reportFound(ctx, b, plan.moved(), currentAmount);
            }
            notices(ctx, kind, target, record, type.name());
            if (command.holderAbsent()) {
                events.publishEvent(BelongingNotices.notice(
                        "BELONGING_HANDOVER_HOLDER_ABSENT",
                        RECORD_TYPE,
                        record,
                        NotificationLevel.LOW,
                        ctx.residentId(),
                        List.of(new RecipientGroup.Staff(b.holderId(), "HOLDER")),
                        type.name() + ": Hành chính đã bàn giao thay khi bạn vắng mặt",
                        ctx.user().accountId(),
                        ctx.now()));
            }
        }
        if (plan.missing().signum() > 0) {
            long lostId = plan.remaining().signum() == 0 && plan.moved().signum() == 0
                    ? b.id()
                    : split(ctx, b, plan.missing(), cash, BelongingStatus.LOST, null, null, null, levelId);
            long record = record(
                    ctx,
                    b,
                    lostId,
                    HandoverType.REPORT_LOST,
                    BelongingStatus.LOST,
                    recordedGiverType,
                    recordedGiverId,
                    null,
                    null,
                    ctx.at(),
                    plan.missing(),
                    cash,
                    levelId,
                    item.conditionDescription(),
                    null,
                    command.reason(),
                    command.holderAbsent(),
                    valuable,
                    List.of(),
                    "Đếm thiếu khi bàn giao (FR-013)",
                    created,
                    touched);
            if (!carriesIncident) {
                // FR-024: phần thiếu của từng đồ liên kết sự cố riêng
                attachIncident(ctx, item, HandoverType.REPORT_LOST, record, ctx.at(), command.reason());
            }
            notices(ctx, HandoverType.REPORT_LOST, BelongingStatus.LOST, record, type.name());
        }
        if (originalAmount.compareTo(currentAmount) != 0) {
            belongings.setAmount(b.id(), cash ? null : originalAmount, cash ? originalAmount : null);
        }
        touched.add(b.id());
    }

    private static boolean wantsIncident(HandoverCommand.Item item) {
        return item.incident() != null
                && item.incident().method() != null
                && !"NONE".equals(item.incident().method());
    }

    /** FR-021, FR-024: thực hiện lựa chọn sự cố của người ghi rồi lưu mã sự cố ở bản ghi (đặt một lần). */
    private void attachIncident(
            Context ctx, HandoverCommand.Item item, HandoverType kind, long record, Instant at, String reason) {
        Long incidentId = incidents.choose(item.incident(), kind, ctx.residentId(), at, reason, ctx.user());
        if (incidentId != null) {
            handovers.linkIncident(record, incidentId);
        }
    }

    /** FR-025: Tìm thấy thêm diễn biến "đã tìm thấy" vào sự cố của lần Báo thất lạc, không đóng sự cố. */
    private void reportFound(Context ctx, LockedBelonging b, BigDecimal found, BigDecimal lostAmount) {
        handovers
                .lossIncidentOf(b.id())
                .ifPresent(incident -> incidents.found(
                        incident,
                        "đã tìm thấy " + found.stripTrailingZeros().toPlainString() + "/"
                                + lostAmount.stripTrailingZeros().toPlainString(),
                        ctx.user()));
    }

    /** Đồ mới tách từ {@code b} mang phần bàn giao hoặc phần thiếu (FR-012, FR-013). */
    private long split(
            Context ctx,
            LockedBelonging b,
            BigDecimal amount,
            boolean cash,
            BelongingStatus status,
            GiverType holder,
            Long holderId,
            Long location,
            long levelId) {
        BelongingView source = belongings.find(b.id()).orElseThrow(NotFoundException::new);
        HolderType holderType =
                holder == null ? null : holder == GiverType.RESIDENT ? HolderType.RESIDENT : HolderType.STAFF;
        return belongings.insertSplit(
                new NewBelonging(
                        ctx.residentId(),
                        b.typeId(),
                        source.description(),
                        cash ? null : amount,
                        cash ? amount : null,
                        source.unit(),
                        status,
                        holderType,
                        holderType == null ? null : holderId,
                        location,
                        levelId,
                        ctx.user().accountId(),
                        ctx.now()),
                b.id(),
                belongings.intakeHandoverOf(b.id()).orElseThrow(NotFoundException::new));
    }

    private long record(
            Context ctx,
            LockedBelonging source,
            long belongingId,
            HandoverType kind,
            BelongingStatus target,
            GiverType giverType,
            Long giverId,
            GiverType recipientType,
            Long recipientId,
            Instant occurredAt,
            BigDecimal amount,
            boolean cash,
            long levelId,
            String conditionDescription,
            Long location,
            String reason,
            boolean holderAbsent,
            boolean valuable,
            List<Long> photos,
            String note,
            List<Created> created,
            Set<Long> touched) {
        long record = handovers.insert(new NewHandover(
                belongingId,
                ctx.residentId(),
                kind,
                target,
                giverType,
                giverId,
                null,
                null,
                recipientType,
                recipientId,
                kind == HandoverType.RETURN ? ctx.returnInfo().recipientName() : null,
                occurredAt,
                cash ? null : amount,
                cash ? amount : null,
                levelId,
                conditionDescription,
                location,
                reason,
                holderAbsent,
                valuable,
                photos,
                note,
                ctx.user().accountId(),
                ctx.now(),
                kind == HandoverType.RETURN ? ctx.returnInfo().facts() : null));
        HolderType holderType = recipientType == null
                ? null
                : recipientType == GiverType.RESIDENT ? HolderType.RESIDENT : HolderType.STAFF;
        // Đã trả, Thất lạc, Đã xử lý: không còn người giữ (data-model mục 3)
        boolean noHolder = target == BelongingStatus.LOST
                || target == BelongingStatus.RETURNED
                || target == BelongingStatus.DISPOSED;
        belongings.applyHandover(
                belongingId,
                target,
                noHolder ? null : holderType,
                noHolder ? null : recipientId,
                location,
                levelId,
                record,
                ctx.now());
        for (long fileId : photos) {
            files.attach(fileId, IntakeService.PHOTO_OWNER, record);
        }
        if (CONSENT_ENDS.contains(target)) {
            consents.expire(belongingId);
        }
        events.publishEvent(
                new BelongingStatusChanged(belongingId, ctx.residentId(), source.status(), target, record, ctx.now()));
        Map<String, Object> after = new HashMap<>();
        after.put("kind", kind.name());
        after.put("targetStatus", target.name());
        after.put("amount", amount);
        after.put("storageLocationId", location);
        after.put("recipientId", recipientId);
        after.put("holderAbsent", holderAbsent);
        audit.write(AuditLogEntry.builder()
                .action(BELONGING_HANDOVER)
                .entity(RECORD_TYPE, record)
                .resident(ctx.residentId())
                .before(Map.of(
                        "belongingId", source.id(), "status", source.status().name()))
                .after(after)
                .build());
        created.add(new Created(record, belongingId, kind, target));
        touched.add(belongingId);
        return record;
    }

    /** FR-032: thất lạc, hư hỏng (Trung bình), tìm thấy (Nhẹ); sau qua đời chỉ người đại diện. */
    private void notices(Context ctx, HandoverType kind, BelongingStatus target, long record, String typeName) {
        RecipientGroup family = ctx.deceased()
                ? new RecipientGroup.Representative(ctx.residentId())
                : new RecipientGroup.PrimaryContact(ctx.residentId());
        String event;
        NotificationLevel level;
        List<RecipientGroup> recipients;
        String text;
        if (target == BelongingStatus.LOST || target == BelongingStatus.DAMAGED) {
            boolean lost = target == BelongingStatus.LOST;
            event = lost ? "BELONGING_LOST" : "BELONGING_DAMAGED";
            level = NotificationLevel.MEDIUM;
            recipients = List.of(family, new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
            text = typeName + (lost ? ": đồ gửi thất lạc" : ": đồ gửi hư hỏng");
        } else if (kind == HandoverType.FOUND) {
            event = "BELONGING_FOUND";
            level = NotificationLevel.LOW;
            recipients = List.of(family);
            text = typeName + ": đồ gửi thất lạc đã được tìm thấy";
        } else {
            return;
        }
        events.publishEvent(BelongingNotices.notice(
                event,
                RECORD_TYPE,
                record,
                event + ":" + record,
                level,
                ctx.residentId(),
                recipients,
                text,
                ctx.user().accountId(),
                ctx.now(),
                ctx.deceased()));
    }

    private static BusinessException violation(long belongingId, List<String> violations) {
        String code = violations.getFirst();
        return new BusinessException(
                INVALID_CODES.contains(code) ? HttpStatus.UNPROCESSABLE_CONTENT : HttpStatus.CONFLICT,
                code,
                "Lệnh bàn giao không đạt điều kiện: " + String.join(", ", violations),
                Map.of("belongingId", belongingId, "violations", violations));
    }

    private static BusinessException blocked(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private static BusinessException invalid(String code, String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
}
