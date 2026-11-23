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
import com.eldercare.backend.admission.api.PostDeathTaskApi;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
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
import com.eldercare.backend.belongings.internal.ending.persistence.BelongingEndingStore;
import com.eldercare.backend.belongings.internal.ending.service.BelongingPostDeathItem;
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
import java.util.Optional;
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
    private final PostDeathTaskApi postDeathTasks;
    private final BelongingPostDeathItem postDeathItem;
    private final BelongingEndingStore endingStore;
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
            PostDeathTaskApi postDeathTasks,
            BelongingPostDeathItem postDeathItem,
            BelongingEndingStore endingStore,
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
        this.postDeathTasks = postDeathTasks;
        this.postDeathItem = postDeathItem;
        this.endingStore = endingStore;
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
        return execute(resident, command, null, null);
    }

    /**
     * Bản ghi sinh ra khi hoàn thành phiếu kiểm kê (FR-028b): Báo thất lạc cho đồ Không tìm thấy, Ghi hư hỏng cho đồ
     * Lệch tình trạng, Tìm thấy cho đồ Thất lạc thấy ở vị trí; cùng điều kiện và hiệu lực như lệnh bàn giao thường, bản ghi
     * mang mã phiếu. Chạy trong transaction của lệnh hoàn thành phiếu.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Result inventoryHandover(Target resident, HandoverCommand command, long inventoryCheckId) {
        if (command.type() != HandoverType.REPORT_LOST
                && command.type() != HandoverType.RECORD_DAMAGE
                && command.type() != HandoverType.FOUND) {
            throw invalid("INVALID_VALUE", "Phiếu kiểm kê chỉ sinh Báo thất lạc, Ghi hư hỏng, Tìm thấy");
        }
        return execute(resident, command, null, inventoryCheckId);
    }

    /** Đề nghị xử lý đã duyệt áp dụng cho một đồ (FR-027a): hình thức, đơn vị tiếp nhận, lý do của đề nghị. */
    public record Disposal(long approvalRequestId, String method, String receivingUnit, String reason) {}

    /**
     * Áp dụng đề nghị "Xử lý đồ không người nhận" đã duyệt cho một đồ: bản ghi loại Xử lý, đồ chuyển Đã xử lý, đồng ý tự
     * giữ Hết hiệu lực, báo người đại diện. Đồ không còn Đang giữ hoặc Hư hỏng thì bị loại khỏi phần áp dụng (trả trống,
     * FR-027a, 000 FR-038). Chạy trong transaction áp dụng của yêu cầu phê duyệt.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Created> dispose(long residentId, long belongingId, Disposal disposal) {
        CurrentUser user = users.getObject();
        List<LockedBelonging> found = locks.lockCurrent(residentId, List.of(belongingId));
        if (found.isEmpty()) {
            throw new NotFoundException();
        }
        LockedBelonging b = found.getFirst();
        if (b.status() != BelongingStatus.HELD && b.status() != BelongingStatus.DAMAGED) {
            return Optional.empty();
        }
        Instant now = time.now();
        Context ctx = new Context(
                user,
                user.staffId().orElse(0L),
                residentId,
                residents.status(residentId),
                false,
                now,
                now,
                null,
                null,
                disposal.approvalRequestId(),
                disposal.receivingUnit());
        boolean cash = b.cashAmount() != null;
        BigDecimal amount = cash ? b.cashAmount() : b.quantity();
        String typeName =
                catalogs.require(BelongingCatalogs.BELONGING_TYPE, b.typeId()).name();
        GiverType holder = b.holderType() == HolderType.RESIDENT ? GiverType.RESIDENT : GiverType.STAFF;
        List<Created> created = new ArrayList<>();
        Set<Long> touched = new LinkedHashSet<>();
        long record = record(
                ctx,
                b,
                b.id(),
                HandoverType.DISPOSAL,
                BelongingStatus.DISPOSED,
                holder,
                b.holderId(),
                null,
                null,
                now,
                amount,
                cash,
                b.conditionLevelId(),
                null,
                null,
                disposal.reason(),
                false,
                valuableTypes.isValuable(typeName),
                List.of(),
                "Xử lý đồ không người nhận: " + disposal.method(),
                created,
                touched);
        events.publishEvent(BelongingNotices.notice(
                "BELONGING_DISPOSED",
                RECORD_TYPE,
                record,
                NotificationLevel.LOW,
                residentId,
                List.of(new RecipientGroup.Representative(residentId)),
                typeName + ": đồ gửi đã được xử lý theo đề nghị đã duyệt",
                user.accountId(),
                now));
        recalculation.verify(residentId, belongings.intakeHandoverOf(b.id()).orElseThrow(NotFoundException::new));
        if (ctx.deceased()) {
            postDeathTasks.refresh(residentId);
        }
        return Optional.of(created.getFirst());
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
        return execute(resident, command, info, null);
    }

    private Result execute(Target resident, HandoverCommand command, ReturnInfo returnInfo, Long inventoryCheckId) {
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
                returnInfo,
                inventoryCheckId,
                null,
                null);
        // FR-026a: Tìm thấy sau khi mục sau qua đời đã Hoàn thành thì mục không mở lại, đồ theo dõi riêng từ lúc Tìm
        // thấy
        boolean foundAfterCompletion = command.type() == HandoverType.FOUND
                && context.deceased()
                && postDeathItem.status(residentId).status() == PostDeathItemStatus.COMPLETED;

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
        if (foundAfterCompletion) {
            created.stream()
                    .filter(c -> c.kind() == HandoverType.FOUND)
                    .forEach(c -> endingStore.startDisposalClock(c.belongingId(), context.now()));
        }
        if (context.deceased()) {
            postDeathTasks.refresh(residentId);
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
            ReturnInfo returnInfo,
            Long inventoryCheckId,
            Long approvalRequestId,
            String receivingUnit) {

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
        ConditionChoice condition = condition(item);
        HandoverCommandCheck.Current current = current(b);
        Party giver = giver(ctx, command, b);
        HandoverCheckResult check = checkItem(
                ctx,
                command,
                item,
                b,
                current,
                giver,
                condition,
                valuable,
                type.attributes().requiresConsentForUse());
        HandoverType kind = check.effectiveType();
        boolean cash = b.cashAmount() != null;
        BigDecimal currentAmount = cash ? b.cashAmount() : b.quantity();
        BelongingSplit.Plan plan = splitPlan(currentAmount, item);
        checkIncidentAndShortage(command, item, b, current, kind, plan);

        ItemPlan p = new ItemPlan(
                item,
                b,
                kind,
                check.target(),
                recordedGiver(ctx, command, b, kind, giver),
                recipient(ctx, command, b, kind),
                kind == HandoverType.RETURN && b.status() == BelongingStatus.IN_USE ? "Thu từ người cao tuổi" : null,
                cash,
                levelId,
                valuable,
                type.name());
        BigDecimal originalAmount = plan.remaining().signum() > 0
                ? plan.remaining()
                : plan.moved().signum() > 0 ? plan.moved() : plan.missing();
        if (plan.moved().signum() > 0) {
            recordMoved(ctx, command, p, plan, currentAmount, created, touched);
        }
        if (plan.missing().signum() > 0) {
            recordMissing(ctx, command, p, plan, created, touched);
        }
        if (originalAmount.compareTo(currentAmount) != 0) {
            belongings.setAmount(b.id(), cash ? null : originalAmount, cash ? originalAmount : null);
        }
        touched.add(b.id());
    }

    /** Mức tình trạng mới chọn ở dòng lệnh: thứ tự và dấu "không dùng được"; không chọn thì rỗng. */
    private record ConditionChoice(Integer order, boolean unusable) {}

    /** Người bàn giao hoặc người nhận: loại và mã. */
    private record Party(GiverType type, Long id) {}

    /** Người nhận và vị trí lưu giữ mới sau bàn giao. */
    private record Recipient(GiverType type, Long id, Long location) {}

    /** Một đồ đã qua kiểm tra, kèm mọi giá trị cần để ghi bản ghi bàn giao. */
    private record ItemPlan(
            HandoverCommand.Item item,
            LockedBelonging b,
            HandoverType kind,
            BelongingStatus target,
            Party giver,
            Recipient recipient,
            String moveNote,
            boolean cash,
            long levelId,
            boolean valuable,
            String typeName) {}

    private ConditionChoice condition(HandoverCommand.Item item) {
        if (item.conditionLevelId() == null) {
            return new ConditionChoice(null, false);
        }
        var level = catalogs.requireActive(BelongingCatalogs.CONDITION_LEVEL, item.conditionLevelId());
        return new ConditionChoice(level.sortOrder(), level.attributes().unusable());
    }

    private HandoverCommandCheck.Current current(LockedBelonging b) {
        int currentOrder = catalogs.require(BelongingCatalogs.CONDITION_LEVEL, b.conditionLevelId())
                .sortOrder();
        return new HandoverCommandCheck.Current(b.status(), b.holderType(), b.holderId(), b.version(), currentOrder);
    }

    /** Người bàn giao theo lệnh; không ghi thì là người đang giữ (người cao tuổi hoặc nhân viên thực hiện). */
    private static Party giver(Context ctx, HandoverCommand command, LockedBelonging b) {
        GiverType giverType = command.giverType();
        if (giverType == null) {
            boolean resident = b.holderType() == HolderType.RESIDENT;
            return new Party(
                    resident ? GiverType.RESIDENT : GiverType.STAFF, resident ? ctx.residentId() : ctx.staffId());
        }
        if (giverType == GiverType.RESIDENT && command.giverId() == null) {
            return new Party(giverType, ctx.residentId());
        }
        return new Party(giverType, command.giverId());
    }

    /** Kiểm điều kiện của lệnh cho một đồ ({@link HandoverCommandCheck}); không đạt thì báo vi phạm. */
    private HandoverCheckResult checkItem(
            Context ctx,
            HandoverCommand command,
            HandoverCommand.Item item,
            LockedBelonging b,
            HandoverCommandCheck.Current current,
            Party giver,
            ConditionChoice condition,
            boolean valuable,
            boolean requiresConsent) {
        boolean absenceConfirmed = command.holderAbsent()
                && b.holderType() == HolderType.STAFF
                && (!shifts.onShift(b.holderId(), ctx.now())
                        || accounts.byStaff(b.holderId())
                                .map(a -> a.status() != AccountStatus.ACTIVE)
                                .orElse(true));
        HandoverCheckResult check = HandoverCommandCheck.check(
                current,
                HandoverCommandCheck.Request.builder(command.type(), item.version())
                        .giver(giver.type(), giver.id())
                        .officeStaff(ctx.officeStaff())
                        .holderAbsent(command.holderAbsent(), absenceConfirmed)
                        .storageLocationId(command.storageLocationId())
                        .reason(command.reason())
                        .conditionOrder(condition.order())
                        .unusableLevel(condition.unusable())
                        .conditionHandling(command.conditionHandling())
                        .photoCount(item.photoFileIds().size())
                        .valuable(valuable)
                        .requiresConsent(requiresConsent)
                        .hasEffectiveConsent(consents.hasEffective(b.id()))
                        .residentInFinalState(FINAL_STATES.contains(ctx.residentStatus()))
                        .build());
        if (!check.allowed()) {
            throw violation(b.id(), check.violations());
        }
        return check;
    }

    private static BelongingSplit.Plan splitPlan(BigDecimal currentAmount, HandoverCommand.Item item) {
        try {
            return BelongingSplit.plan(currentAmount, item.amount(), item.countedAmount());
        } catch (IllegalArgumentException e) {
            throw invalid("INVALID_VALUE", e.getMessage());
        }
    }

    private static boolean carriesIncident(HandoverType kind) {
        return kind == HandoverType.REPORT_LOST || kind == HandoverType.RECORD_DAMAGE;
    }

    /**
     * Sự cố chỉ đi kèm Báo thất lạc, Ghi hư hỏng hoặc phần đếm thiếu (FR-021); phần đếm thiếu phải thỏa điều kiện của Báo
     * thất lạc (FR-013).
     */
    private static void checkIncidentAndShortage(
            HandoverCommand command,
            HandoverCommand.Item item,
            LockedBelonging b,
            HandoverCommandCheck.Current current,
            HandoverType kind,
            BelongingSplit.Plan plan) {
        if (wantsIncident(item) && !carriesIncident(kind) && plan.missing().signum() == 0) {
            throw invalid("INVALID_VALUE", "Chỉ Báo thất lạc, Ghi hư hỏng mới ghi hoặc liên kết sự cố (FR-021)");
        }
        if (plan.missing().signum() > 0) {
            HandoverCheckResult lost = HandoverCommandCheck.check(
                    current,
                    HandoverCommandCheck.Request.builder(HandoverType.REPORT_LOST, item.version())
                            .reason(command.reason())
                            .build());
            if (!lost.allowed()) {
                throw violation(b.id(), lost.violations());
            }
        }
    }

    /** Người nhận và vị trí mới theo loại bàn giao; Chuyển giữ phải đổi người giữ hoặc vị trí (FR-011). */
    private static Recipient recipient(Context ctx, HandoverCommand command, LockedBelonging b, HandoverType kind) {
        return switch (kind) {
            case GIVE_FOR_USE -> new Recipient(GiverType.RESIDENT, ctx.residentId(), null);
            case REPORT_LOST -> new Recipient(null, null, null);
            case RETURN ->
                new Recipient(ctx.returnInfo().recipientType(), ctx.returnInfo().recipientId(), null);
            case TRANSFER_CUSTODY -> {
                Long recipientId = command.recipientStaffId() != null
                        ? command.recipientStaffId()
                        : b.holderType() == HolderType.STAFF ? b.holderId() : ctx.staffId();
                Long location = command.storageLocationId();
                if (b.holderType() == HolderType.STAFF
                        && recipientId.equals(b.holderId())
                        && Objects.equals(location, b.storageLocationId())) {
                    throw invalid("INVALID_VALUE", "Chuyển giữ cần vị trí mới hoặc người giữ mới (FR-011)");
                }
                yield new Recipient(GiverType.STAFF, recipientId, location);
            }
            default ->
                new Recipient(
                        GiverType.STAFF,
                        command.recipientStaffId() != null ? command.recipientStaffId() : ctx.staffId(),
                        command.storageLocationId());
        };
    }

    /**
     * Người bàn giao ghi vào bản ghi: người giữ vắng mặt thì là người giữ; Tìm thấy thì không có; Trả từ Đang được người cao
     * tuổi sử dụng thì là nhân viên thực hiện (FR-018).
     */
    private static Party recordedGiver(
            Context ctx, HandoverCommand command, LockedBelonging b, HandoverType kind, Party giver) {
        if (kind == HandoverType.RETURN && b.status() == BelongingStatus.IN_USE) {
            return new Party(GiverType.STAFF, ctx.staffId());
        }
        if (command.holderAbsent()) {
            return new Party(GiverType.STAFF, b.holderId());
        }
        if (kind == HandoverType.FOUND) {
            return new Party(null, null);
        }
        return giver;
    }

    /** Phần được bàn giao: tách đồ nếu cần, ghi bản ghi, sự cố, diễn biến Tìm thấy và thông báo (FR-012, FR-021, FR-025). */
    private void recordMoved(
            Context ctx,
            HandoverCommand command,
            ItemPlan p,
            BelongingSplit.Plan plan,
            BigDecimal currentAmount,
            List<Created> created,
            Set<Long> touched) {
        LockedBelonging b = p.b();
        Recipient to = p.recipient();
        Instant occurredAt = p.kind() == HandoverType.REPORT_LOST && command.discoveredAt() != null
                ? command.discoveredAt()
                : ctx.at();
        long movedId = plan.originalMoves()
                ? b.id()
                : split(ctx, b, plan.moved(), p.cash(), p.target(), to.type(), to.id(), to.location(), p.levelId());
        long record = record(
                ctx,
                b,
                movedId,
                p.kind(),
                p.target(),
                p.giver().type(),
                p.giver().id(),
                to.type(),
                to.id(),
                occurredAt,
                plan.moved(),
                p.cash(),
                p.levelId(),
                p.item().conditionDescription(),
                to.location(),
                command.reason(),
                command.holderAbsent(),
                p.valuable(),
                p.item().photoFileIds(),
                p.moveNote(),
                created,
                touched);
        if (carriesIncident(p.kind())) {
            attachIncident(ctx, p.item(), p.kind(), record, occurredAt, command.reason());
        }
        if (p.kind() == HandoverType.FOUND) {
            reportFound(ctx, b, plan.moved(), currentAmount);
        }
        notices(ctx, p.kind(), p.target(), record, p.typeName());
        if (command.holderAbsent()) {
            events.publishEvent(BelongingNotices.notice(
                    "BELONGING_HANDOVER_HOLDER_ABSENT",
                    RECORD_TYPE,
                    record,
                    NotificationLevel.LOW,
                    ctx.residentId(),
                    List.of(new RecipientGroup.Staff(b.holderId(), "HOLDER")),
                    p.typeName() + ": Hành chính đã bàn giao thay khi bạn vắng mặt",
                    ctx.user().accountId(),
                    ctx.now()));
        }
    }

    /** Phần đếm thiếu: Báo thất lạc trong cùng lệnh, liên kết sự cố riêng nếu lệnh chính không mang sự cố (FR-013, FR-024). */
    private void recordMissing(
            Context ctx,
            HandoverCommand command,
            ItemPlan p,
            BelongingSplit.Plan plan,
            List<Created> created,
            Set<Long> touched) {
        LockedBelonging b = p.b();
        long lostId = plan.remaining().signum() == 0 && plan.moved().signum() == 0
                ? b.id()
                : split(ctx, b, plan.missing(), p.cash(), BelongingStatus.LOST, null, null, null, p.levelId());
        long record = record(
                ctx,
                b,
                lostId,
                HandoverType.REPORT_LOST,
                BelongingStatus.LOST,
                p.giver().type(),
                p.giver().id(),
                null,
                null,
                ctx.at(),
                plan.missing(),
                p.cash(),
                p.levelId(),
                p.item().conditionDescription(),
                null,
                command.reason(),
                command.holderAbsent(),
                p.valuable(),
                List.of(),
                "Đếm thiếu khi bàn giao (FR-013)",
                created,
                touched);
        if (!carriesIncident(p.kind())) {
            attachIncident(ctx, p.item(), HandoverType.REPORT_LOST, record, ctx.at(), command.reason());
        }
        notices(ctx, HandoverType.REPORT_LOST, BelongingStatus.LOST, record, p.typeName());
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
        long part = belongings.insertSplit(
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
        // 013-3: phần tách kế thừa đồng ý cho tự giữ của đồ bị tách; phần sang trạng thái kết thúc không cần đồng ý
        if (!CONSENT_ENDS.contains(status)) {
            consents.inherit(b.id(), part);
        }
        return part;
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
                kind == HandoverType.RETURN ? ctx.returnInfo().facts() : null,
                ctx.inventoryCheckId(),
                ctx.approvalRequestId(),
                ctx.receivingUnit()));
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
                HandoverCommandCheck.describe(violations),
                Map.of("belongingId", belongingId, "violations", violations));
    }

    private static BusinessException blocked(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private static BusinessException invalid(String code, String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
}
