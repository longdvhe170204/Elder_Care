package com.eldercare.backend.nutrition.internal.serving.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.IncidentCommandApi;
import com.eldercare.backend.incident.api.IncidentTypeApi;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.incident.api.events.AlertRequested;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.MealSlip;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SpecialPortion;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionFlag;
import com.eldercare.backend.nutrition.internal.closing.persistence.ClosingRecordStore;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictType;
import com.eldercare.backend.nutrition.internal.serving.domain.ServingConfirmation;
import com.eldercare.backend.nutrition.internal.serving.domain.ServingConfirmation.Result;
import com.eldercare.backend.nutrition.internal.serving.persistence.ServingRecordStore;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xác nhận phục vụ suất đặc biệt (mục G, FR-049 → FR-051, BR-M08-14; research M-10): đúng người, và không xung đột với
 * dị ứng, hạn chế hiện hành tại lúc phục vụ của người được phục vụ. Xung đột dị ứng: cảnh báo 007 mức Trung bình
 * (transaction riêng, còn dù lệnh bị chặn), chặn đến khi khai đã ăn (kèm sự cố 007 trong cùng lệnh) hoặc chưa ăn. Quyền
 * "Xác nhận phục vụ suất đặc biệt" CREATE, phạm vi tầng/khu theo ca.
 */
@Service
public class ServingService {

    public static final String ENTITY = "M08.SPECIAL_PORTION";
    static final String INCIDENT_TYPE = "WRONG_MEAL_SERVED";
    static final String INCIDENT_SOURCE = "EATING";

    private static final AuditAction CONFIRMED = new AuditAction("SERVING_CONFIRMED", false);

    private final ClosingRecordStore records;
    private final ServingRecordStore confirmations;
    private final DietAssignmentStore assignments;
    private final NutritionCatalogStore catalog;
    private final HealthItemApi healthItems;
    private final ServingAlerts alerts;
    private final ObjectProvider<IncidentCommandApi> incidents;
    private final ObjectProvider<IncidentTypeApi> incidentTypes;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;

    ServingService(
            ClosingRecordStore records,
            ServingRecordStore confirmations,
            DietAssignmentStore assignments,
            NutritionCatalogStore catalog,
            HealthItemApi healthItems,
            ServingAlerts alerts,
            ObjectProvider<IncidentCommandApi> incidents,
            ObjectProvider<IncidentTypeApi> incidentTypes,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time) {
        this.records = records;
        this.confirmations = confirmations;
        this.assignments = assignments;
        this.catalog = catalog;
        this.healthItems = healthItems;
        this.alerts = alerts;
        this.incidents = incidents;
        this.incidentTypes = incidentTypes;
        this.audit = audit;
        this.user = user;
        this.time = time;
    }

    /** Dữ liệu sự cố khi khai đã ăn (api-011 {@code ServingConfirmation.incident}). */
    public record IncidentData(String severity, String symptoms, String treatment) {}

    /** Lệnh xác nhận (api-011 {@code ServingConfirmation}): {@code residentId} là người đang được phục vụ. */
    public record Command(long residentId, Boolean hasEaten, IncidentData incident, Instant deviceTime) {}

    /** Đối tượng kiểm quyền: suất, tầng/khu của phiếu chứa suất. */
    @Transactional(readOnly = true)
    public Target targetOf(long specialId) {
        SimpleRecord<SpecialPortion> row = records.special(specialId);
        long floor = records.requireSlip(row.parent().entityId()).content().floorAreaId();
        return new Target(ENTITY, Long.toString(specialId), row.content().residentId(), floor);
    }

    @RequiresPermission(function = Function.SPECIAL_MEAL_SERVICE, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void confirm(Target target, long specialId, Command command) {
        doConfirm(specialId, command, command.deviceTime());
    }

    /** Ghi từ đồng bộ ngoại tuyến: quyền đã được 002 kiểm theo thời điểm thiết bị; kiểm lại xung đột tại lúc đồng bộ. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long applySynced(long specialId, Command command, Instant deviceAt) {
        return doConfirm(specialId, command, deviceAt);
    }

    /**
     * FR-050 cho 005: người có suất thường, không thuộc danh sách cần đối chiếu, không cần xác nhận; người khác cần
     * xác nhận thành công ({@code CORRECT_PORTION}) của bữa.
     */
    @Transactional(readOnly = true)
    public boolean servingConfirmed(long dailyMealId, long residentId) {
        for (SimpleRecord<MealSlip> slip : records.slips(dailyMealId)) {
            for (SimpleRecord<SpecialPortion> row : records.specials(slip.id())) {
                SpecialPortion s = row.content();
                if (s.residentId() != residentId
                        || !ClosingRecordStore.ACTIVE.equals(row.status())
                        || !(s.special() || s.flags().contains(PortionFlag.NEEDS_CROSS_CHECK))) {
                    continue;
                }
                return confirmations.of(row.id()).stream()
                        .anyMatch(e -> e.content().result() == Result.CORRECT_PORTION);
            }
        }
        return true;
    }

    private long doConfirm(long specialId, Command c, Instant deviceAt) {
        SimpleRecord<SpecialPortion> row = records.special(specialId);
        if (!ClosingRecordStore.ACTIVE.equals(row.status())) {
            throw blocked("STATE_CHANGED", "Suất đã đổi trạng thái");
        }
        SpecialPortion s = row.content();
        long floor = records.requireSlip(row.parent().entityId()).content().floorAreaId();
        Instant now = time.now();
        Set<ConflictType> conflicts = conflictsOf(c.residentId(), s);
        if (conflicts.contains(ConflictType.ALLERGY)) {
            return onAllergyConflict(row, s, floor, c, deviceAt, now);
        }
        if (!conflicts.isEmpty()) {
            throw blocked("PORTION_VIOLATES_RESTRICTION", "Suất trái hạn chế ăn uống của người được phục vụ");
        }
        if (c.residentId() != s.residentId()) {
            throw blocked("PORTION_WRONG_RESIDENT", "Suất không đúng người được phục vụ");
        }
        return save(row, s, floor, c.residentId(), Result.CORRECT_PORTION, null, null, deviceAt, now);
    }

    private long onAllergyConflict(
            SimpleRecord<SpecialPortion> row, SpecialPortion s, long floor, Command c, Instant deviceAt, Instant now) {
        String source = "M08.ALLERGEN_PORTION_SERVED:" + row.id() + ":" + c.residentId();
        if (c.hasEaten() == null) {
            alerts.request(new AlertRequested(
                    c.residentId(),
                    "ALLERGEN_PORTION_SERVED",
                    "MEDIUM",
                    "SPECIAL_PORTION",
                    row.id(),
                    Optional.empty(),
                    now,
                    source));
            throw blocked(
                    "PORTION_HAS_ALLERGEN",
                    "Suất có thành phần gây dị ứng với người được phục vụ: khai đã ăn hoặc chưa ăn");
        }
        if (!c.hasEaten()) {
            return save(row, s, floor, c.residentId(), Result.BLOCKED_CONFLICT_NOT_EATEN, null, source, deviceAt, now);
        }
        IncidentData data = c.incident();
        if (data == null
                || data.severity() == null
                || data.severity().isBlank()
                || data.symptoms() == null
                || data.symptoms().isBlank()) {
            throw new InvalidValueException("Khai đã ăn phải kèm mức độ và triệu chứng của sự cố");
        }
        long incident = incidents
                .getObject()
                .record(
                        new RecordIncidentCommand(
                                c.residentId(),
                                incidentTypes.getObject().idOf(INCIDENT_TYPE),
                                INCIDENT_SOURCE,
                                deviceAt != null ? deviceAt : now,
                                now,
                                floor,
                                null,
                                null,
                                data.symptoms().strip(),
                                data.severity().strip(),
                                null,
                                data.treatment() == null || data.treatment().isBlank()
                                        ? null
                                        : data.treatment().strip(),
                                false),
                        user);
        return save(row, s, floor, c.residentId(), Result.BLOCKED_CONFLICT_EATEN, incident, source, deviceAt, now);
    }

    private long save(
            SimpleRecord<SpecialPortion> row,
            SpecialPortion s,
            long floor,
            long served,
            Result result,
            Long incident,
            String alertSource,
            Instant deviceAt,
            Instant now) {
        RecordEvent<ServingConfirmation> saved = confirmations.add(
                new ServingConfirmation(row.id(), s.dailyMealId(), served, floor, result, incident, alertSource),
                user.accountId(),
                now,
                deviceAt);
        audit.write(AuditLogEntry.builder()
                .action(CONFIRMED)
                .entity(ServingConfirmation.KIND.code(), saved.id())
                .resident(served)
                .after(Map.of("result", result.name(), "specialPortionId", row.id()))
                .build());
        return saved.id();
    }

    /** Xung đột của các món thực sự phục vụ với dị ứng, hạn chế hiện hành của người được phục vụ (FR-049). */
    private Set<ConflictType> conflictsOf(long served, SpecialPortion s) {
        Optional<DietAssignment> active = assignments.active(served);
        long dietId = active.map(DietAssignment::dietId).orElse(s.dietId());
        Set<Long> personal = active.map(a -> (Set<Long>) new HashSet<>(a.personalRestrictions()))
                .orElse(Set.of());
        Set<Long> dietRestrictions =
                new HashSet<>(catalog.diet(dietId).attributes().restrictions());
        List<AllergyItem> allergies = healthItems.active(served, HealthItemType.ALLERGY).stream()
                .map(i -> new AllergyItem(i.allergenId().orElse(null), i.notAutoChecked()))
                .toList();
        Set<Long> ingredients = new HashSet<>();
        for (long dish : servedDishes(s)) {
            ingredients.addAll(catalog.effectiveIngredients(dish));
        }
        return ConflictCheck.conflicts(ingredients, allergies, personal, dietRestrictions);
    }

    private static List<Long> servedDishes(SpecialPortion s) {
        if (s.assignedDishId() != null) {
            return List.of(s.assignedDishId());
        }
        return s.dishes().stream()
                .map(d -> d.substituteDishId() != null ? d.substituteDishId() : d.dishId())
                .toList();
    }

    private static BusinessException blocked(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
