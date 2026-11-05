package com.eldercare.backend.family.internal.portal.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogRecord;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.admission.api.dto.ContractView;
import com.eldercare.backend.care.api.PublicCareResultApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.internal.portal.domain.HealthAccess;
import com.eldercare.backend.family.internal.portal.domain.HealthSection;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.visit.service.VisitQueries;
import com.eldercare.backend.incident.api.FamilyPortalHealthApi;
import com.eldercare.backend.medication.api.FamilyPortalMedicationApi;
import com.eldercare.backend.resident.api.AllergenApi;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.AllergenView;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.HealthItemView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cổng người thân (012 T027; US5, FR-046 → FR-048, BR-M10-01; research F-10, F-11). Chỉ tài khoản người thân, chỉ người
 * cao tuổi có quan hệ Hiệu lực; trả giá trị hiện hành (FR-047). Phần sức khỏe đi qua 012: kiểm quyền xem sức khỏe có tác
 * dụng (quyền bật và bản đồng ý bao gồm người thân), đọc API nguồn, ghi nhật ký lượt xem trong cùng yêu cầu (R-15: hành
 * động nhật ký {@code HEALTH_SECTION_VIEWED} trên đối tượng {@link #VIEW_ENTITY}). Nguồn chưa cài (007 chỉ số, cân nặng,
 * sự cố; 011 chế độ ăn, thực đơn; 004 lịch khám; công thức ăn uống của 005) trả "chưa có dữ liệu", không số giả, không
 * ghi nhật ký.
 */
@Service
public class PortalService {

    public static final String VIEW_ENTITY = "M10.HEALTH_VIEW";
    private static final AuditAction VIEWED = new AuditAction("HEALTH_SECTION_VIEWED", false);
    // Khoảng mặc định khi người xem không chọn (hiển thị, không phải mốc nghiệp vụ)
    private static final int DEFAULT_DAYS = 30;

    /** Người cao tuổi của người thân kèm quyền có tác dụng tại lúc xem. */
    public record PortalResident(
            long residentId,
            String fullName,
            String relationship,
            boolean representative,
            boolean primaryContact,
            Map<FamilyRight, Boolean> rights,
            boolean canViewBelongings) {}

    /** Thông tin chung (FR-046 (1)); {@code unavailable} là các mục chưa có nguồn. */
    public record General(
            long residentId,
            String fullName,
            String status,
            String location,
            List<Map<String, Object>> visits,
            List<Map<String, Object>> absences,
            List<Map<String, Object>> dayCareSchedule,
            List<Map<String, Object>> incidents,
            List<String> unavailable) {}

    /** Một phần sức khỏe; {@code available} sai khi nguồn chưa có, kèm {@code note}. */
    public record HealthPart(String section, boolean available, String note, List<Map<String, Object>> items) {}

    public record Contracts(List<ContractView> contracts) {}

    public record HealthViewLog(long familyMemberId, long residentId, String section, String source, Instant at) {}

    private final RelationshipQueries relationships;
    private final FamilyRightsApi rights;
    private final VisitQueries visits;
    private final ResidentApi residents;
    private final ResidentStatusApi statuses;
    private final ResidentLocationApi locations;
    private final HealthItemApi healthItems;
    private final AllergenApi allergens;
    private final ObjectProvider<FamilyPortalHealthApi> incidentHealth;
    private final ObjectProvider<FamilyPortalMedicationApi> medications;
    private final ObjectProvider<PublicCareResultApi> careResults;
    private final ObjectProvider<ContractReadApi> contracts;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;
    private final TransactionTemplate tx;

    PortalService(
            RelationshipQueries relationships,
            FamilyRightsApi rights,
            VisitQueries visits,
            ResidentApi residents,
            ResidentStatusApi statuses,
            ResidentLocationApi locations,
            HealthItemApi healthItems,
            AllergenApi allergens,
            ObjectProvider<FamilyPortalHealthApi> incidentHealth,
            ObjectProvider<FamilyPortalMedicationApi> medications,
            ObjectProvider<PublicCareResultApi> careResults,
            ObjectProvider<ContractReadApi> contracts,
            AuditLogApi auditLog,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser,
            PlatformTransactionManager transactions) {
        this.relationships = relationships;
        this.rights = rights;
        this.visits = visits;
        this.residents = residents;
        this.statuses = statuses;
        this.locations = locations;
        this.healthItems = healthItems;
        this.allergens = allergens;
        this.incidentHealth = incidentHealth;
        this.medications = medications;
        this.careResults = careResults;
        this.contracts = contracts;
        this.auditLog = auditLog;
        this.time = time;
        this.currentUser = currentUser;
        this.tx = new TransactionTemplate(transactions);
    }

    @Transactional(readOnly = true)
    public List<PortalResident> residents() {
        long self = familyMember();
        Instant now = time.now();
        return relationships.activeOfFamilyMember(self).stream()
                .map(r -> {
                    Map<FamilyRight, Boolean> effective = new EnumMap<>(FamilyRight.class);
                    for (FamilyRight right : FamilyRight.values()) {
                        effective.put(right, rights.rightEffective(self, r.residentId(), right, now));
                    }
                    return new PortalResident(
                            r.residentId(),
                            residents.identity(r.residentId()).fullName(),
                            r.relationship().name(),
                            r.representative(),
                            r.primaryContact(),
                            effective,
                            rights.canViewBelongings(self, r.residentId(), now));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public General general(long residentId) {
        long self = related(residentId);
        Instant now = time.now();
        LocalDate today = time.today();
        List<String> unavailable = new ArrayList<>(List.of("menu"));
        String location = locations
                .current(residentId)
                .map(l -> l.roomName() == null ? l.bedCode() : l.roomName() + ", giường " + l.bedCode())
                .orElse(null);
        List<Map<String, Object>> ownVisits = visits.ofFamilyMember(residentId, self, today).stream()
                .map(v -> row(
                        "id", v.id(),
                        "date", v.content().date(),
                        "startTime", v.content().startTime().toString(),
                        "endTime", v.content().endTime().toString(),
                        "status", v.status()))
                .toList();
        List<Map<String, Object>> absences = statuses
                .statusPeriods(
                        residentId,
                        Set.of(ResidentStatus.TEMPORARILY_ABSENT),
                        now.minus(Duration.ofDays(DEFAULT_DAYS)),
                        now)
                .stream()
                .map(p -> row("from", p.from(), "to", p.to().orElse(null)))
                .toList();
        List<Map<String, Object>> schedule = new ArrayList<>();
        ContractReadApi reads = contracts.getIfAvailable();
        if (reads != null) {
            for (int i = 0; i < 7; i++) {
                for (AttendanceScheduleView s : reads.attendanceOn(residentId, today.plusDays(i))) {
                    schedule.add(row(
                            "date", s.date(),
                            "arrivalTime", s.arrivalTime().toString(),
                            "departureTime", s.departureTime().toString()));
                }
            }
        }
        List<Map<String, Object>> incidents = new ArrayList<>();
        FamilyPortalHealthApi health = incidentHealth.getIfAvailable();
        if (health == null) {
            unavailable.add("incidents");
        } else {
            health.incidents(residentId, self, now.minus(Duration.ofDays(DEFAULT_DAYS)), now)
                    .forEach(i -> incidents.add(row(
                            "incidentId", i.incidentId(),
                            "type", i.general().typeName(),
                            "occurredAt", i.general().occurredAt(),
                            "level", i.general().level(),
                            "processingStatus", i.general().processingStatus())));
        }
        return new General(
                residentId,
                residents.identity(residentId).fullName(),
                residents.status(residentId).name(),
                location,
                ownVisits,
                absences,
                schedule,
                incidents,
                unavailable);
    }

    /**
     * Phần sức khỏe (FR-046 (2)); mở được thì ghi nhật ký lượt xem (FR-048). Không bọc transaction: nguồn báo lỗi (005
     * chưa có công thức ăn uống) rollback transaction của chính nó; nhật ký ghi trong transaction riêng.
     */
    public HealthPart health(long residentId, String path, LocalDate from, LocalDate to) {
        long self = related(residentId);
        HealthSection section = HealthSection.fromPath(path).orElseThrow(NotFoundException::new);
        Instant now = time.now();
        HealthAccess.Decision decision = HealthAccess.decide(
                relationships.rightEnabledAt(self, residentId, FamilyRight.VIEW_HEALTH, now),
                rights.rightEffective(self, residentId, FamilyRight.VIEW_HEALTH, now));
        if (decision != HealthAccess.Decision.ALLOWED) {
            throw new PermissionDeniedException(
                    decision.name(),
                    decision == HealthAccess.Decision.NO_HEALTH_VIEW_RIGHT
                            ? "Người thân không có quyền xem sức khỏe"
                            : "Bản đồng ý chia sẻ thông tin chưa bao gồm người thân này",
                    Map.of());
        }
        LocalDate end = to == null ? time.today() : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_DAYS) : from;
        HealthPart part =
                read(section, residentId, self, time.startOfDay(start), time.startOfDay(end.plusDays(1)), start, end);
        if (part.available()) {
            tx.executeWithoutResult(s -> auditLog.write(AuditLogEntry.builder()
                    .action(VIEWED)
                    .entity(VIEW_ENTITY, residentId)
                    .resident(residentId)
                    .details(Map.of("familyMemberId", self, "section", section.name(), "source", "PORTAL"))
                    .build()));
        }
        return part;
    }

    /** Hợp đồng (FR-046 (4)): chỉ người đại diện, chỉ xem. */
    @Transactional(readOnly = true)
    public Contracts contract(long residentId) {
        long self = related(residentId);
        if (!rights.isRepresentative(self, residentId, time.now())) {
            throw denied();
        }
        ContractReadApi reads = contracts.getIfAvailable();
        return new Contracts(reads == null ? List.of() : reads.ofResident(residentId));
    }

    /** Tra cứu nhật ký lượt xem sức khỏe (FR-048): chỉ Quản lý viện. */
    @Transactional(readOnly = true)
    public List<HealthViewLog> viewLogs(Long residentId, Long familyMemberId, Instant from, Instant to) {
        CurrentUser user = currentUser.getObject();
        if (user.type() != AccountType.STAFF || !user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw denied();
        }
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? time.now().plusSeconds(1) : to;
        List<HealthViewLog> result = new ArrayList<>();
        long after = 0;
        while (true) {
            List<AuditLogRecord> page =
                    auditLog.recordedBetween(VIEW_ENTITY, start, end, after, AuditLogApi.RECORDED_LIMIT);
            for (AuditLogRecord r : page) {
                long resident = Long.parseLong(r.entityId());
                long member = ((Number) r.details().get("familyMemberId")).longValue();
                if ((residentId == null || residentId == resident)
                        && (familyMemberId == null || familyMemberId == member)) {
                    result.add(new HealthViewLog(
                            member,
                            resident,
                            String.valueOf(r.details().get("section")),
                            String.valueOf(r.details().get("source")),
                            r.occurredAt()));
                }
            }
            if (page.size() < AuditLogApi.RECORDED_LIMIT) {
                return result;
            }
            after = page.getLast().id();
        }
    }

    private HealthPart read(
            HealthSection section,
            long residentId,
            long self,
            Instant from,
            Instant to,
            LocalDate fromDay,
            LocalDate toDay) {
        String code = section.path();
        switch (section) {
            case ALLERGIES_CONDITIONS -> {
                List<Map<String, Object>> items = new ArrayList<>();
                for (HealthItemView i :
                        healthItems.active(residentId, HealthItemType.ALLERGY, HealthItemType.CHRONIC_CONDITION)) {
                    String text = i.allergenId()
                            .flatMap(allergens::allergen)
                            .map(AllergenView::name)
                            .or(i::freeText)
                            .orElse("");
                    items.add(row("type", i.type().name(), "text", text));
                }
                return new HealthPart(code, true, null, items);
            }
            case MEDICATIONS -> {
                FamilyPortalMedicationApi api = medications.getIfAvailable();
                if (api == null) {
                    return missing(code, "Thuốc");
                }
                List<Map<String, Object>> items = api.prescriptions(residentId, self).orElse(List.of()).stream()
                        .map(p -> row(
                                "medicationName", p.medicationName(),
                                "dosageInstruction", p.dosageInstruction(),
                                "status", p.status(),
                                "startDate", p.startDate(),
                                "endDate", p.endDate()))
                        .toList();
                return new HealthPart(code, true, null, items);
            }
            case CARE_RESULTS -> {
                PublicCareResultApi api = careResults.getIfAvailable();
                if (api == null) {
                    return missing(code, "Kết quả chăm sóc");
                }
                List<Map<String, Object>> items = api.results(residentId, from, to).stream()
                        .map(r -> row(
                                "resultType",
                                String.valueOf(r.resultType()),
                                "performedAt",
                                r.performedAt(),
                                "values",
                                r.values()))
                        .toList();
                return new HealthPart(code, true, null, items);
            }
            case EATING -> {
                PublicCareResultApi api = careResults.getIfAvailable();
                if (api == null) {
                    return missing(code, "Ăn uống");
                }
                try {
                    var summary = api.summary(residentId, fromDay, toDay);
                    Map<String, Object> item = new LinkedHashMap<>();
                    summary.averageEatingRatio().ifPresent(v -> item.put("averageEatingRatio", v));
                    summary.averageWaterMl().ifPresent(v -> item.put("averageWaterMl", v));
                    return new HealthPart(code, true, null, item.isEmpty() ? List.of() : List.of(item));
                } catch (BusinessException e) {
                    // 005 chưa chốt công thức tổng hợp (team-plan mục 8): không trả số giả
                    return new HealthPart(code, false, e.getMessage(), List.of());
                }
            }
            case VITAL_SIGNS, INCIDENTS -> {
                FamilyPortalHealthApi api = incidentHealth.getIfAvailable();
                if (api == null) {
                    return missing(code, section == HealthSection.INCIDENTS ? "Sự cố" : "Chỉ số");
                }
                List<Map<String, Object>> items = section == HealthSection.INCIDENTS
                        ? api.incidents(residentId, self, from, to).stream()
                                .map(i -> row(
                                        "incidentId", i.incidentId(),
                                        "type", i.general().typeName(),
                                        "occurredAt", i.general().occurredAt(),
                                        "level", i.general().level(),
                                        "health", i.health().orElse(null)))
                                .toList()
                        : api.vitalSigns(residentId, self, from, to).stream()
                                .map(v -> row(
                                        "indicatorId",
                                        v.indicatorId(),
                                        "values",
                                        v.values(),
                                        "level",
                                        String.valueOf(v.level())))
                                .toList();
                return new HealthPart(code, true, null, items);
            }
            case WEIGHT -> {
                return missing(code, "Cân nặng");
            }
            case DIET -> {
                return missing(code, "Chế độ ăn riêng");
            }
            case MEDICAL_APPOINTMENTS -> {
                return missing(code, "Lịch khám bên ngoài");
            }
            default -> throw new NotFoundException();
        }
    }

    private static HealthPart missing(String code, String name) {
        return new HealthPart(code, false, name + ": chưa có dữ liệu, tính năng nguồn chưa hoạt động", List.of());
    }

    private long familyMember() {
        CurrentUser user = currentUser.getObject();
        if (user.type() != AccountType.FAMILY_MEMBER) {
            throw denied();
        }
        return user.familyMemberId().orElseThrow();
    }

    /** Người thân đang đăng nhập có quan hệ Hiệu lực với người cao tuổi (US5 kịch bản 5, 7). */
    private long related(long residentId) {
        long self = familyMember();
        Optional<FamilyRelationship> active = relationships.active(self, residentId);
        if (active.isEmpty()) {
            throw denied();
        }
        return self;
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            row.put((String) pairs[i], pairs[i + 1]);
        }
        return row;
    }

    private static PermissionDeniedException denied() {
        return new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", "SCOPE"));
    }
}
