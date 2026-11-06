package com.eldercare.backend.nutrition.internal.slip.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.api.events.MealSlipDelivered;
import com.eldercare.backend.nutrition.api.events.MealSlipDeliveredLate;
import com.eldercare.backend.nutrition.api.events.MealSlipDiscrepancyReported;
import com.eldercare.backend.nutrition.api.events.SlipDiscrepancyNotice;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.Adjustment;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.MealSlip;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SlipDiscrepancy;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SpecialPortion;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SupplementaryPortion;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson.DishChoice;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionFlag;
import com.eldercare.backend.nutrition.internal.closing.persistence.ClosingRecordStore;
import com.eldercare.backend.nutrition.internal.closing.persistence.PortionClosingStore;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.FloorView;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phiếu bữa ăn sau chốt (mục F, FR-044 → FR-048b, BR-M08-09 → 12; research M-09): bếp đánh dấu chuẩn bị, giao, ghi xử lý
 * sai lệch, giao phần bổ sung (quyền "Chuẩn bị, giao phiếu" UPDATE); người nhận tại tầng nhận, báo sai lệch (quyền "Nhận
 * phiếu, báo sai lệch", phạm vi tầng/khu theo ca; tự kiểm để trả {@code OUT_OF_RECEIVING_SCOPE}). Bếp nhận
 * {@link KitchenSlip} chỉ có trường FR-046; nhân viên tại tầng nhận {@link FloorSlip} (SC-008). Lệnh khóa dòng phiếu, đọc
 * lại dưới {@code READ_COMMITTED} như chốt suất.
 */
@Service
public class MealSlipService {

    public static final String ENTITY = "M08.MEAL_SLIP";

    private static final Set<String> RESOLUTIONS = Set.of("ADD_PORTION", "REPLACE_PORTION", "OTHER");

    private static final AuditAction SPECIAL_PREPARED = new AuditAction("SPECIAL_PORTION_PREPARED", false);
    private static final AuditAction PREPARED = new AuditAction("MEAL_SLIP_PREPARED", false);
    private static final AuditAction DELIVERED = new AuditAction("MEAL_SLIP_DELIVERED", false);
    private static final AuditAction RECEIVED = new AuditAction("MEAL_SLIP_RECEIVED", false);
    private static final AuditAction DISCREPANCY = new AuditAction("MEAL_SLIP_DISCREPANCY_REPORTED", false);
    private static final AuditAction RESOLVED = new AuditAction("MEAL_SLIP_DISCREPANCY_RESOLVED", false);
    private static final AuditAction SUPPLEMENT_DELIVERED = new AuditAction("SUPPLEMENTARY_PORTION_DELIVERED", false);
    private static final AuditAction SUPPLEMENT_RECEIVED = new AuditAction("SUPPLEMENTARY_PORTION_RECEIVED", false);

    private final ClosingRecordStore records;
    private final PortionClosingStore closings;
    private final NutritionCatalogStore catalog;
    private final ResidentApi residents;
    private final ResidentLocationApi residentLocations;
    private final LocationStructureApi structure;
    private final HealthItemApi healthItems;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    MealSlipService(
            ClosingRecordStore records,
            PortionClosingStore closings,
            NutritionCatalogStore catalog,
            ResidentApi residents,
            ResidentLocationApi residentLocations,
            LocationStructureApi structure,
            HealthItemApi healthItems,
            PermissionApi permissions,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.records = records;
        this.closings = closings;
        this.catalog = catalog;
        this.residents = residents;
        this.residentLocations = residentLocations;
        this.structure = structure;
        this.healthItems = healthItems;
        this.permissions = permissions;
        this.audit = audit;
        this.user = user;
        this.time = time;
        this.events = events;
    }

    /** Số suất theo chế độ ăn (tên chế độ ăn). */
    public record DietCount(String diet, int portionCount) {}

    /** Nhãn suất đặc biệt (FR-045): chỉ trường FR-046; dấu là danh sách đóng, không có "cần đối chiếu". */
    public record Label(
            long id,
            String fullName,
            String room,
            String diet,
            Texture texture,
            String dishes,
            List<String> flags,
            boolean prepared) {}

    /** Phần bổ sung của phiếu (FR-048b; trường thêm vào hợp đồng 2026-11-07). */
    public record Supplement(long id, Long dietId, int portionCount, Instant deliveredAt, Instant receivedAt) {}

    /** Phiếu cho bếp (api-011 {@code KitchenMealSlip}; FR-046, SC-008). */
    public record KitchenSlip(
            long id,
            String floorArea,
            String meal,
            List<DietCount> portionsByDiet,
            List<Label> labels,
            MealSlipStatus status,
            List<Supplement> supplementaryPortions) {}

    /** Phiếu cho nhân viên tại tầng (api-011 {@code FloorMealSlip}): thêm người cần đối chiếu, dị ứng khi có quyền xem. */
    public record FloorSlip(KitchenSlip slip, List<Long> needsCrossCheck, List<Map<String, Object>> allergies) {}

    public static Target target(long slipId, Long floorAreaId) {
        return new Target(ENTITY, Long.toString(slipId), null, floorAreaId);
    }

    /** Đối tượng kiểm quyền của phiếu: tầng/khu (phạm vi người nhận tại tầng). */
    @Transactional(readOnly = true)
    public Target targetOf(long slipId) {
        return target(slipId, records.requireSlip(slipId).content().floorAreaId());
    }

    @Transactional(readOnly = true)
    public Target targetOfSupplement(long supplementId) {
        SupplementaryPortion p = records.supplement(supplementId).content();
        return target(p.slipId(), p.floorAreaId());
    }

    @Transactional(readOnly = true)
    public Target targetOfSpecial(long specialId) {
        SimpleRecord<SpecialPortion> row = records.special(specialId);
        return target(row.parent().entityId(), null);
    }

    /** Nhân viên bếp luôn nhận {@link KitchenSlip}, kể cả khi có vai trò khác (SC-008). */
    public boolean isKitchen() {
        return user.roles().contains(Role.KITCHEN_STAFF);
    }

    /** Danh sách phiếu cho bếp (FR-046): quyền "Suất ăn đã chốt, phiếu bữa ăn" VIEW. */
    @RequiresPermission(function = Function.MEAL_COUNT_TICKET, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<KitchenSlip> kitchenSlips(Target target, long dailyMealId, Long floorAreaId) {
        return slipsOf(dailyMealId, floorAreaId).stream().map(this::kitchen).toList();
    }

    /**
     * Danh sách phiếu cho nhân viên tại tầng: chỉ phiếu của tầng/khu trong phạm vi quyền "Nhận phiếu, báo sai lệch" VIEW;
     * dị ứng của từng người chỉ khi có quyền xem dị ứng (19.3).
     */
    @Transactional(readOnly = true)
    public List<FloorSlip> floorSlips(long dailyMealId, Long floorAreaId) {
        List<FloorSlip> out = new ArrayList<>();
        boolean any = false;
        for (SimpleRecord<MealSlip> slip : slipsOf(dailyMealId, floorAreaId)) {
            PermissionResult allowed = permissions.hasPermission(
                    user,
                    new PermissionKey(Function.MEAL_RECEIPT, Operation.VIEW),
                    target(slip.id(), slip.content().floorAreaId()));
            if (!allowed.allowed()) {
                continue;
            }
            any = true;
            List<SimpleRecord<SpecialPortion>> specials = activeSpecials(slip.id());
            List<Long> crossCheck = specials.stream()
                    .filter(s -> s.content().flags().contains(PortionFlag.NEEDS_CROSS_CHECK))
                    .map(s -> s.content().residentId())
                    .toList();
            List<Map<String, Object>> allergies = new ArrayList<>();
            for (SimpleRecord<SpecialPortion> s : specials) {
                long resident = s.content().residentId();
                if (!mayViewAllergy(resident)) {
                    continue;
                }
                healthItems.active(resident, HealthItemType.ALLERGY).forEach(i -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("residentId", resident);
                    i.allergenId().ifPresent(a -> row.put("allergenId", a));
                    i.freeText().ifPresent(t -> row.put("text", t));
                    allergies.add(row);
                });
            }
            out.add(new FloorSlip(kitchen(slip), crossCheck, allergies));
        }
        if (!any && floorAreaId != null) {
            throw outOfScope();
        }
        return out;
    }

    /** Bếp đánh dấu một suất đặc biệt đã chuẩn bị (FR-045); phiếu đã giao thì không đổi được. */
    @RequiresPermission(function = Function.MEAL_PREPARATION, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void markSpecialPrepared(Target target, long specialId) {
        SimpleRecord<SpecialPortion> row = records.special(specialId);
        SimpleRecord<MealSlip> slip = records.lockSlip(row.parent().entityId());
        row = records.special(specialId);
        SpecialPortion s = row.content();
        if (!ClosingRecordStore.ACTIVE.equals(row.status())
                || s.prepared()
                || !MealSlipStatus.CLOSED.name().equals(slip.status())) {
            throw stateChanged();
        }
        records.changeSpecial(row, ClosingRecordStore.ACTIVE, prepared(s));
        audit.write(AuditLogEntry.builder()
                .action(SPECIAL_PREPARED)
                .entity(SpecialPortion.KIND.code(), specialId)
                .resident(s.residentId())
                .build());
    }

    /** Lệnh của bếp trên phiếu: {@code mark-prepared}, {@code deliver}, {@code resolve-discrepancy} (bảng phiếu). */
    @RequiresPermission(function = Function.MEAL_PREPARATION, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void kitchenCommand(Target target, long slipId, String command, String resolution, String description) {
        SimpleRecord<MealSlip> slip = records.lockSlip(slipId);
        Instant now = time.now();
        switch (command) {
            case "mark-prepared" -> markPrepared(slip);
            case "deliver" -> deliver(slip, now);
            case "resolve-discrepancy" -> resolve(slip, resolution, description, now);
            default -> throw new NotFoundException();
        }
    }

    /**
     * Lệnh của người nhận tại tầng trên phiếu (FR-048): {@code receive} (một lần), {@code report-discrepancy} (nội dung bắt
     * buộc). Tự kiểm quyền "Nhận phiếu, báo sai lệch" theo tầng/khu của phiếu: ngoài phạm vi trả 403
     * {@code OUT_OF_RECEIVING_SCOPE} (quickstart Q33).
     */
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void floorCommand(long slipId, String command, String content) {
        Operation operation = switch (command) {
            case "receive" -> Operation.CONFIRM;
            case "report-discrepancy" -> Operation.CREATE;
            default -> throw new NotFoundException();
        };
        SimpleRecord<MealSlip> slip = records.lockSlip(slipId);
        requireReceiver(slip, operation);
        Instant now = time.now();
        if (!MealSlipStatus.DELIVERED.name().equals(slip.status())) {
            throw stateChanged();
        }
        if (operation == Operation.CONFIRM) {
            records.changeSlip(
                    slip, MealSlipStatus.RECEIVED.name(), slip.content().received(user.accountId(), now));
            write(RECEIVED, slipId, Map.of());
            return;
        }
        String text = required(content);
        records.addDiscrepancy(
                new SlipDiscrepancy(
                        slipId, null, text, user.accountId(), user.staffId().orElse(null), null, null, null),
                user.accountId(),
                now,
                slip.content().floorAreaId());
        records.changeSlip(slip, MealSlipStatus.HAS_DISCREPANCY.name(), slip.content());
        write(DISCREPANCY, slipId, Map.of("content", text));
        MealSlip m = slip.content();
        events.publishEvent(new MealSlipDiscrepancyReported(slipId, m.dailyMealId(), m.floorAreaId(), now));
        events.publishEvent(new SlipDiscrepancyNotice(
                slipId,
                m.dailyMealId(),
                false,
                false,
                null,
                "M08.SLIP_DISCREPANCY:" + slipId + ":" + now.toEpochMilli(),
                user.accountId(),
                now));
    }

    /** Bếp giao phần bổ sung (FR-048b): ghi người, thời điểm giao một lần. */
    @RequiresPermission(function = Function.MEAL_PREPARATION, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deliverSupplement(Target target, long supplementId) {
        RecordEvent<SupplementaryPortion> row = lockedSupplement(supplementId);
        if (row.content().deliveredAt() != null) {
            throw stateChanged();
        }
        records.deliverSupplement(row, user.accountId(), time.now());
        write(SUPPLEMENT_DELIVERED, supplementId, Map.of("slipId", row.content().slipId()));
    }

    /**
     * Người nhận tại tầng trên phần bổ sung (FR-048b): {@code receive} (một lần, sau khi sai lệch đã được xử lý),
     * {@code report-discrepancy} (lưu như sai lệch của phiếu; phiếu giữ trạng thái).
     */
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void supplementFloorCommand(long supplementId, String command, String content) {
        Operation operation = switch (command) {
            case "receive" -> Operation.CONFIRM;
            case "report-discrepancy" -> Operation.CREATE;
            default -> throw new NotFoundException();
        };
        SupplementaryPortion p = records.supplement(supplementId).content();
        SimpleRecord<MealSlip> slip = records.lockSlip(p.slipId());
        requireReceiver(slip, operation);
        RecordEvent<SupplementaryPortion> row = records.supplement(supplementId);
        if (row.content().deliveredAt() == null || row.confirmedAt() != null) {
            throw stateChanged();
        }
        Instant now = time.now();
        if (operation == Operation.CONFIRM) {
            if (!unresolved(slip.id(), supplementId).isEmpty()) {
                throw stateChanged();
            }
            records.receiveSupplement(row, user.accountId(), now);
            write(SUPPLEMENT_RECEIVED, supplementId, Map.of("slipId", slip.id()));
            return;
        }
        String text = required(content);
        records.addDiscrepancy(
                new SlipDiscrepancy(
                        slip.id(),
                        supplementId,
                        text,
                        user.accountId(),
                        user.staffId().orElse(null),
                        null,
                        null,
                        null),
                user.accountId(),
                now,
                slip.content().floorAreaId());
        write(DISCREPANCY, slip.id(), Map.of("supplementId", supplementId, "content", text));
        events.publishEvent(new MealSlipDiscrepancyReported(
                slip.id(), slip.content().dailyMealId(), slip.content().floorAreaId(), now));
        events.publishEvent(new SlipDiscrepancyNotice(
                slip.id(),
                slip.content().dailyMealId(),
                false,
                true,
                null,
                "M08.SLIP_DISCREPANCY:" + slip.id() + ":" + supplementId + ":" + now.toEpochMilli(),
                user.accountId(),
                now));
    }

    /** Số suất trên phiếu theo chế độ ăn: lần chốt cộng phát sinh cần giao đã được bếp xác nhận (DBR-27, FR-044). */
    public Map<Long, Integer> portionsByDiet(long dailyMealId, long floorAreaId) {
        Map<Long, Integer> out = new TreeMap<>();
        closings.ofDailyMeal(dailyMealId).map(PortionClosing::portions).orElse(List.of()).stream()
                .filter(p -> p.floorAreaId() == floorAreaId)
                .forEach(p -> out.merge(p.dietId(), p.portionCount(), Integer::sum));
        for (RecordEvent<Adjustment> row : records.adjustments(dailyMealId)) {
            Adjustment a = row.content();
            if (a.floorAreaId() == floorAreaId && a.needsDelivery() && row.confirmedAt() != null) {
                out.merge(a.dietId(), a.portionCount(), Integer::sum);
            }
        }
        out.values().removeIf(v -> v == 0);
        return out;
    }

    private void markPrepared(SimpleRecord<MealSlip> slip) {
        if (!MealSlipStatus.CLOSED.name().equals(slip.status())) {
            throw stateChanged();
        }
        List<UnmetCondition> unmet = new ArrayList<>();
        notPrepared(slip.id()).ifPresent(unmet::add);
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        records.changeSlip(slip, MealSlipStatus.PREPARED.name(), slip.content());
        write(PREPARED, slip.id(), Map.of());
    }

    /**
     * Giao (FR-047): mọi suất đặc biệt đã chuẩn bị, không còn suất thiếu món thay thế chưa được Bác sĩ xử lý, mọi phát sinh
     * cần giao của tầng/khu đã được bếp xác nhận, bữa có bản ghi lưu mẫu; nêu đủ các điều kiện chưa đạt rồi mới xét trạng
     * thái Đã chuẩn bị.
     */
    private void deliver(SimpleRecord<MealSlip> slip, Instant now) {
        MealSlip m = slip.content();
        List<UnmetCondition> unmet = new ArrayList<>();
        notPrepared(slip.id()).ifPresent(unmet::add);
        long missing = activeSpecials(slip.id()).stream()
                .filter(s -> s.content().flags().contains(PortionFlag.MISSING_SUBSTITUTE_DISH)
                        && s.content().assignedDishId() == null)
                .count();
        if (missing > 0) {
            unmet.add(new UnmetCondition(
                    "MISSING_SUBSTITUTE_DISH", missing + " suất thiếu món thay thế chưa được Bác sĩ xử lý"));
        }
        long pending = records.adjustments(m.dailyMealId()).stream()
                .filter(r -> r.content().floorAreaId() == m.floorAreaId()
                        && r.content().needsDelivery()
                        && r.confirmedAt() == null)
                .count();
        if (pending > 0) {
            unmet.add(new UnmetCondition(
                    "ADJUSTMENTS_NOT_CONFIRMED", pending + " phát sinh của tầng/khu chưa được bếp xác nhận"));
        }
        if (!records.hasRetainedSample(m.dailyMealId())) {
            unmet.add(new UnmetCondition("SAMPLE_NOT_RETAINED", "Bữa chưa có bản ghi lưu mẫu (FR-060, BR-M08-15)"));
        }
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        if (!MealSlipStatus.PREPARED.name().equals(slip.status())) {
            throw stateChanged();
        }
        records.changeSlip(slip, MealSlipStatus.DELIVERED.name(), m.delivered(user.accountId(), now));
        write(DELIVERED, slip.id(), Map.of());
        events.publishEvent(new MealSlipDelivered(slip.id(), m.dailyMealId(), m.floorAreaId(), now));
        closings.ofDailyMeal(m.dailyMealId()).ifPresent(c -> {
            Instant deadline = c.mealTime().plus(Duration.ofMinutes(c.lateThresholdMinutes()));
            if (now.isAfter(deadline)) {
                events.publishEvent(
                        new MealSlipDeliveredLate(slip.id(), m.dailyMealId(), m.floorAreaId(), deadline, now));
            }
        });
    }

    /** Bếp ghi cách xử lý mọi sai lệch chưa xử lý của phiếu và phần bổ sung; phiếu Có sai lệch về Đã giao (BR-M08-11). */
    private void resolve(SimpleRecord<MealSlip> slip, String resolution, String description, Instant now) {
        if (resolution == null || !RESOLUTIONS.contains(resolution)) {
            throw new InvalidValueException("Cách xử lý phải là ADD_PORTION, REPLACE_PORTION hoặc OTHER");
        }
        List<RecordEvent<SlipDiscrepancy>> open = unresolved(slip.id(), null);
        if (open.isEmpty()) {
            throw stateChanged();
        }
        String text = description == null || description.isBlank() ? null : description.strip();
        for (RecordEvent<SlipDiscrepancy> row : open) {
            records.resolveDiscrepancy(row, resolution, text, user.accountId(), now);
            events.publishEvent(new SlipDiscrepancyNotice(
                    slip.id(),
                    slip.content().dailyMealId(),
                    true,
                    row.content().supplementId() != null,
                    row.content().reporterStaffId(),
                    "M08.SLIP_DISCREPANCY_RESOLVED:" + row.id(),
                    user.accountId(),
                    now));
        }
        if (MealSlipStatus.HAS_DISCREPANCY.name().equals(slip.status())) {
            records.changeSlip(slip, MealSlipStatus.DELIVERED.name(), slip.content());
        }
        write(RESOLVED, slip.id(), Map.of("resolution", resolution));
    }

    /** Sai lệch chưa xử lý của phiếu; {@code supplementId} khác {@code null} thì chỉ của phần bổ sung đó. */
    private List<RecordEvent<SlipDiscrepancy>> unresolved(long slipId, Long supplementId) {
        return records.discrepancies(slipId).stream()
                .filter(r -> r.confirmedAt() == null)
                .filter(r ->
                        supplementId == null || supplementId.equals(r.content().supplementId()))
                .toList();
    }

    private Optional<UnmetCondition> notPrepared(long slipId) {
        long count = activeSpecials(slipId).stream()
                .filter(s -> s.content().special() && !s.content().prepared())
                .count();
        return count == 0
                ? Optional.empty()
                : Optional.of(new UnmetCondition("NOT_PREPARED", count + " suất đặc biệt chưa được đánh dấu chuẩn bị"));
    }

    private void requireReceiver(SimpleRecord<MealSlip> slip, Operation operation) {
        PermissionResult result = permissions.hasPermission(
                user,
                new PermissionKey(Function.MEAL_RECEIPT, operation),
                target(slip.id(), slip.content().floorAreaId()));
        if (result.allowed()) {
            return;
        }
        if (result.failedLayer().orElse(null) == FailedLayer.SCOPE) {
            throw outOfScope();
        }
        throw new PermissionDeniedException(
                "PERMISSION_DENIED",
                "Không có quyền nhận phiếu, báo sai lệch",
                Map.of("failedLayer", result.failedLayer().map(Enum::name).orElse("ROLE")));
    }

    private RecordEvent<SupplementaryPortion> lockedSupplement(long supplementId) {
        records.lockSlip(records.supplement(supplementId).content().slipId());
        return records.supplement(supplementId);
    }

    private List<SimpleRecord<MealSlip>> slipsOf(long dailyMealId, Long floorAreaId) {
        return records.slips(dailyMealId).stream()
                .filter(s -> floorAreaId == null || s.content().floorAreaId() == floorAreaId)
                .toList();
    }

    private List<SimpleRecord<SpecialPortion>> activeSpecials(long slipId) {
        return records.specials(slipId).stream()
                .filter(s -> ClosingRecordStore.ACTIVE.equals(s.status()))
                .toList();
    }

    private KitchenSlip kitchen(SimpleRecord<MealSlip> slip) {
        MealSlip m = slip.content();
        List<DietCount> counts = portionsByDiet(m.dailyMealId(), m.floorAreaId()).entrySet().stream()
                .map(e -> new DietCount(catalog.diet(e.getKey()).name(), e.getValue()))
                .toList();
        List<SimpleRecord<SpecialPortion>> specials = activeSpecials(slip.id());
        Map<Long, LocationView> where = residentLocations.current(
                specials.stream().map(s -> s.content().residentId()).collect(Collectors.toSet()));
        List<Label> labels = specials.stream()
                .map(s -> label(s, Optional.ofNullable(where.get(s.content().residentId()))))
                .toList();
        List<Supplement> supplements = records.supplements(slip.id()).stream()
                .map(r -> new Supplement(
                        r.id(),
                        r.content().dietId(),
                        r.content().portionCount(),
                        r.content().deliveredAt(),
                        r.content().receivedAt()))
                .toList();
        return new KitchenSlip(
                slip.id(),
                structure.floor(m.floorAreaId()).map(FloorView::name).orElse(Long.toString(m.floorAreaId())),
                catalog.meal(DailyMeal.mealIdOf(m.dailyMealId())).name(),
                counts,
                labels,
                MealSlipStatus.valueOf(slip.status()),
                supplements);
    }

    private Label label(SimpleRecord<SpecialPortion> row, Optional<LocationView> where) {
        SpecialPortion s = row.content();
        String dishes = s.assignedDishId() != null
                ? catalog.dish(s.assignedDishId()).name()
                : s.dishes().stream().map(this::dishName).collect(Collectors.joining(", "));
        List<String> flags = s.flags().stream()
                .filter(f -> f != PortionFlag.NEEDS_CROSS_CHECK)
                .map(Enum::name)
                .sorted()
                .toList();
        return new Label(
                row.id(),
                residents.identity(s.residentId()).fullName(),
                where.map(LocationView::roomName).orElse(null),
                catalog.diet(s.dietId()).name(),
                s.texture(),
                dishes,
                flags,
                s.prepared());
    }

    private String dishName(DishChoice choice) {
        return catalog.dish(choice.substituteDishId() != null ? choice.substituteDishId() : choice.dishId())
                .name();
    }

    private boolean mayViewAllergy(long residentId) {
        return permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.ALLERGY_CONDITION, Operation.VIEW),
                        new Target("M01.RESIDENT", Long.toString(residentId), residentId, null))
                .allowed();
    }

    private void write(AuditAction action, long id, Map<String, Object> after) {
        audit.write(AuditLogEntry.builder()
                .action(action)
                .entity(ENTITY, id)
                .after(after)
                .build());
    }

    private static SpecialPortion prepared(SpecialPortion s) {
        return new SpecialPortion(
                s.dailyMealId(),
                s.residentId(),
                s.dietId(),
                s.texture(),
                s.dishes(),
                s.flags(),
                s.special(),
                s.assignedDishId(),
                s.assignNote(),
                s.assignedBy(),
                s.assignedAt(),
                s.heldServedAt(),
                s.heldDiscardedAt(),
                true);
    }

    private static String required(String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidValueException("Thiếu nội dung sai lệch (FR-048)");
        }
        return content.strip();
    }

    private static PermissionDeniedException outOfScope() {
        return new PermissionDeniedException(
                "OUT_OF_RECEIVING_SCOPE",
                "Ngoài phạm vi nhận phiếu của tầng/khu trong ca (FR-048)",
                Map.of("failedLayer", "SCOPE"));
    }

    private static BusinessException stateChanged() {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Phiếu đã đổi trạng thái");
    }
}
