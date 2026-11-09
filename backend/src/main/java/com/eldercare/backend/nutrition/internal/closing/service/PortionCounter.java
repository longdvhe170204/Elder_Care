package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.care.api.dto.AttendanceView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.dto.StayMealView;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.api.dto.TripOverlapView;
import com.eldercare.backend.nutrition.api.spi.StayMealPort;
import com.eldercare.backend.nutrition.api.spi.TripMealPort;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedKind;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedResidents;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedResidents.Counted;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedResidents.MealSlot;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedResidents.ResidentFacts;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionContent;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionContent.Portion;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionFlag;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.persistence.MenuStore;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.OpenAbsence;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.spi.AbsenceApi;
import com.eldercare.backend.resident.api.spi.ContractApi;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongFunction;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Dựng dữ liệu cho {@link CountedResidents} và {@link PortionContent} với dữ liệu tại thời điểm gọi (FR-034 → FR-038;
 * research M-08): trạng thái (001), loại lưu trú (004 qua {@code ContractApi}), lượt vắng (004), lịch đến (004), có mặt bán
 * trú (005), vị trí và khu bán trú (003), lượt ở lại (012), chuyến đi (014), bản gán (011), dị ứng (001), thực đơn đã công bố.
 * Thiếu bean của 012, 014 thì ghi {@code missingData} {@code STAY}, {@code TRIP} (module-interfaces mục 1.6).
 */
@Component
class PortionCounter {

    private static final Set<ResidentStatus> MAY_EAT = EnumSet.of(
            ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.OFF_SITE_ACTIVITY);

    private final ResidentApi residents;
    private final ContractApi residenceTypes;
    private final AbsenceApi absences;
    private final ContractReadApi contracts;
    private final DayCareAttendanceApi attendance;
    private final ResidentLocationApi locations;
    private final LocationStructureApi structure;
    private final ObjectProvider<StayMealPort> stays;
    private final ObjectProvider<TripMealPort> trips;
    private final DietAssignmentStore assignments;
    private final NutritionCatalogStore catalog;
    private final HealthItemApi healthItems;
    private final MenuStore menus;

    PortionCounter(
            ResidentApi residents,
            ContractApi residenceTypes,
            AbsenceApi absences,
            ContractReadApi contracts,
            DayCareAttendanceApi attendance,
            ResidentLocationApi locations,
            LocationStructureApi structure,
            ObjectProvider<StayMealPort> stays,
            ObjectProvider<TripMealPort> trips,
            DietAssignmentStore assignments,
            NutritionCatalogStore catalog,
            HealthItemApi healthItems,
            MenuStore menus) {
        this.residents = residents;
        this.residenceTypes = residenceTypes;
        this.absences = absences;
        this.contracts = contracts;
        this.attendance = attendance;
        this.locations = locations;
        this.structure = structure;
        this.stays = stays;
        this.trips = trips;
        this.assignments = assignments;
        this.catalog = catalog;
        this.healthItems = healthItems;
        this.menus = menus;
    }

    /** Kết quả tính: người được tính kèm nội dung suất, thực đơn đã công bố, nguồn thiếu, người cần món an toàn bị xung đột. */
    record Count(
            List<CountedPerson> persons, Optional<Menu> menu, Set<String> missingData, List<Long> safeDishConflicts) {}

    /**
     * Tính cho bữa ngày; {@code only} rỗng là mọi người, có giá trị thì chỉ các người cao tuổi đó (phát sinh, FR-039).
     * {@code assignedDishes}: món Bác sĩ chỉ định riêng theo người (FR-037); {@code applySafeDish}: áp món an toàn (FR-037a).
     */
    Count count(
            DailyMeal meal, Optional<Collection<Long>> only, Map<Long, Long> assignedDishes, boolean applySafeDish) {
        Instant mealTime = meal.mealTime().orElseThrow();
        LocalTime local = mealTime.atZone(BusinessTime.ZONE).toLocalTime();
        Set<String> missing = new LinkedHashSet<>();
        TripMealPort tripPort = trips.getIfAvailable();
        StayMealPort stayPort = stays.getIfAvailable();
        if (tripPort == null) {
            missing.add("TRIP");
        }
        if (stayPort == null) {
            missing.add("STAY");
        }
        List<TripOverlapView> overlapping = tripPort == null ? List.of() : tripPort.overlappingMealTime(mealTime);
        Collection<Long> ids = only.orElseGet(() -> residents.byStatus(MAY_EAT));
        Optional<Long> dayCareFloor = dayCareFloor();
        Map<Long, Long> floors = new HashMap<>();
        List<ResidentFacts> facts = new ArrayList<>();
        for (long id : ids) {
            ResidentStatus status = residents.status(id);
            if (!MAY_EAT.contains(status)) {
                continue;
            }
            boolean dayCare = residenceTypes
                    .currentResidenceType(id, meal.date())
                    .filter(t -> t == ResidenceType.DAY_CARE)
                    .isPresent();
            Long floor = dayCare ? dayCareFloor.orElse(null) : residentialFloor(id, mealTime);
            if (floor != null && !dayCare) {
                floors.put(id, floor);
            }
            Optional<OpenAbsence> absence = absences.open(id);
            List<AttendanceView> sessions = dayCare ? attendance.inRange(id, meal.date(), meal.date()) : List.of();
            AttendanceStatus scheduled = sessions.stream()
                    .filter(s -> !s.adHocSession())
                    .map(AttendanceView::status)
                    .findFirst()
                    .orElse(null);
            boolean adHocPresent =
                    sessions.stream().anyMatch(s -> s.adHocSession() && s.status() == AttendanceStatus.PRESENT);
            facts.add(new ResidentFacts(
                    id,
                    status,
                    dayCare,
                    absence.isPresent(),
                    absence.map(OpenAbsence::expectedReturnAt).orElse(null),
                    overlapping,
                    dayCare ? contracts.attendanceOn(id, meal.date()) : List.of(),
                    scheduled,
                    adHocPresent,
                    floor));
        }
        List<StayMealView> stayViews = stayPort == null
                ? List.of()
                : stayPort.staysWithMealRegistration(mealTime).stream()
                        .filter(s -> only.map(o -> o.contains(s.residentId())).orElse(true))
                        .toList();
        for (StayMealView stay : stayViews) {
            floors.computeIfAbsent(stay.residentId(), r -> residentialFloor(r, mealTime));
        }
        floors.values().removeIf(java.util.Objects::isNull);
        List<Counted> counted = CountedResidents.count(
                new MealSlot(mealTime, local, meal.servesResidential(), meal.servesDayCare()),
                facts,
                stayViews,
                floors);
        Optional<Menu> menu = menus.publishedOn(meal.date());
        List<MenuDish> rows = menu.map(m -> m.content().dishes().stream()
                        .filter(d -> d.date().equals(meal.date()) && d.mealId() == meal.mealId())
                        .toList())
                .orElseGet(List::of);
        Diet defaultDiet = catalog.diets().stream()
                .filter(d -> d.active() && d.attributes().isDefault())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Viện chưa có chế độ ăn mặc định (FR-001)"));
        Map<Long, Set<Long>> ingredients = new HashMap<>();
        LongFunction<Set<Long>> ingredientsOf = id -> ingredients.computeIfAbsent(id, catalog::effectiveIngredients);
        List<CountedPerson> persons = new ArrayList<>();
        List<Long> safeConflicts = new ArrayList<>();
        for (Counted c : counted) {
            if (c.kind() == CountedKind.FAMILY) {
                persons.add(new CountedPerson(
                        CountedKind.FAMILY,
                        c.residentId(),
                        c.familyMemberId(),
                        c.stayId(),
                        c.floorAreaId(),
                        defaultDiet.id(),
                        null,
                        Texture.REGULAR,
                        PortionContent.family(ofDiet(rows, defaultDiet.id())),
                        Set.of(),
                        false));
                continue;
            }
            Optional<DietAssignment> assignment = assignments.active(c.residentId());
            Diet diet = assignment.map(a -> catalog.diet(a.dietId())).orElse(defaultDiet);
            Texture texture = assignment.map(DietAssignment::texture).orElse(Texture.REGULAR);
            Set<Long> personal = assignment
                    .map(a -> (Set<Long>) new HashSet<>(a.personalRestrictions()))
                    .orElse(Set.of());
            Set<Long> dietRestrictions = new HashSet<>(diet.attributes().restrictions());
            List<AllergyItem> allergies = healthItems.active(c.residentId(), HealthItemType.ALLERGY).stream()
                    .map(i -> new AllergyItem(i.allergenId().orElse(null), i.notAutoChecked()))
                    .toList();
            boolean crossCheck = allergies.stream().anyMatch(AllergyItem::notAutoChecked);
            Portion portion = PortionContent.of(new PortionContent.Input(
                    ofDiet(rows, diet.id()),
                    menu.isPresent(),
                    texture,
                    assignedDishes.get(c.residentId()),
                    dish -> ConflictCheck.hasConflict(ingredientsOf.apply(dish), allergies, personal, dietRestrictions),
                    diet.attributes().safeDishId(),
                    applySafeDish,
                    crossCheck));
            if (portion.safeDishConflicts()) {
                safeConflicts.add(c.residentId());
            }
            Set<PortionFlag> flags = EnumSet.noneOf(PortionFlag.class);
            flags.addAll(portion.flags());
            if (c.expectedReturn()) {
                flags.add(PortionFlag.EXPECTED_RETURN);
            }
            if (assignment.isEmpty()) {
                flags.add(PortionFlag.NO_DIET_ASSIGNED);
            }
            persons.add(new CountedPerson(
                    c.kind(),
                    c.residentId(),
                    null,
                    null,
                    c.floorAreaId(),
                    diet.id(),
                    assignment.map(DietAssignment::id).orElse(null),
                    texture,
                    portion.dishes(),
                    flags,
                    portion.special()));
        }
        return new Count(persons, menu, missing, safeConflicts);
    }

    private static List<MenuDish> ofDiet(List<MenuDish> rows, long dietId) {
        return rows.stream().filter(r -> r.dietId() == dietId).toList();
    }

    private Long residentialFloor(long residentId, Instant at) {
        Optional<LocationView> location = locations.at(residentId, at).or(() -> locations.current(residentId));
        return location.map(l -> l.floorId() != null ? l.floorId() : l.areaId()).orElse(null);
    }

    /** Khu bán trú (003) như 005 ({@code AttendanceSources.floor}); chưa cấu hình thì người bán trú không được chia tầng. */
    private Optional<Long> dayCareFloor() {
        Optional<LocationRef> ref = structure.dayCareRestAreaLocation();
        if (ref.isEmpty()) {
            return Optional.empty();
        }
        return switch (ref.get().type()) {
            case "FLOOR", "AREA" -> Optional.of(ref.get().id());
            case "ROOM" -> structure.room(ref.get().id()).map(r -> r.floorId());
            default -> Optional.empty();
        };
    }
}
