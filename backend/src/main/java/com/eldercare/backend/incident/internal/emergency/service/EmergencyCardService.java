package com.eldercare.backend.incident.internal.emergency.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.incident.api.events.EmergencyIncidentActivated;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import com.eldercare.backend.incident.internal.critical.persistence.CriticalCareStore;
import com.eldercare.backend.incident.internal.emergency.domain.EmergencyCard;
import com.eldercare.backend.incident.internal.emergency.persistence.EmergencyStore;
import com.eldercare.backend.incident.internal.incident.domain.EmergencyActivationRequested;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentNotices;
import com.eldercare.backend.medication.api.PrescriptionApi;
import com.eldercare.backend.resident.api.AllergenApi;
import com.eldercare.backend.resident.api.AssessmentApi;
import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.EndOfLifeWishView;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.HealthItemView;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thẻ thông tin khẩn cấp (FR-047 b, c, FR-048, BR-M05-13; research S-12). Kích hoạt chạy trong transaction của lệnh
 * làm sự cố thành Khẩn cấp: dựng thẻ từ 001, 003, 006, 012, lưu bản chụp, phát {@link EmergencyIncidentActivated}. Mỗi
 * nguồn đọc riêng; nguồn lỗi thì mục ghi vào {@code unavailable} và thẻ vẫn lưu. Đọc tuần tự trong luồng của lệnh (không
 * dùng luồng ảo như research S-12 ghi), vì các nguồn đọc trong transaction đang mở; ghi lệch ở plan.
 */
@Service
public class EmergencyCardService {
    private static final String ENTITY = "M05.INCIDENT";

    private final EmergencyStore store;
    private final IncidentStore incidents;
    private final CriticalCareStore critical;
    private final PermissionApi permissions;
    private final FieldVisibilityApi visibility;
    private final ApplicationEventPublisher events;
    private final EndOfLifeWishApi wishes;
    private final HealthItemApi healthItems;
    private final AllergenApi allergens;
    private final AssessmentApi assessments;
    private final PrescriptionApi prescriptions;
    private final Optional<ResidentApi> residents;
    private final Optional<ResidentLocationApi> locations;
    private final Optional<EmergencyFamilyPort> family;
    private final Optional<AlertStaffPort> staff;

    public EmergencyCardService(
            EmergencyStore store,
            IncidentStore incidents,
            CriticalCareStore critical,
            PermissionApi permissions,
            FieldVisibilityApi visibility,
            ApplicationEventPublisher events,
            EndOfLifeWishApi wishes,
            HealthItemApi healthItems,
            AllergenApi allergens,
            AssessmentApi assessments,
            PrescriptionApi prescriptions,
            @Autowired(required = false) ResidentApi residents,
            @Autowired(required = false) ResidentLocationApi locations,
            @Autowired(required = false) EmergencyFamilyPort family,
            @Autowired(required = false) AlertStaffPort staff) {
        this.store = store;
        this.incidents = incidents;
        this.critical = critical;
        this.permissions = permissions;
        this.visibility = visibility;
        this.events = events;
        this.wishes = wishes;
        this.healthItems = healthItems;
        this.allergens = allergens;
        this.assessments = assessments;
        this.prescriptions = prescriptions;
        this.residents = Optional.ofNullable(residents);
        this.locations = Optional.ofNullable(locations);
        this.family = Optional.ofNullable(family);
        this.staff = Optional.ofNullable(staff);
    }

    /**
     * Kích hoạt khẩn cấp (FR-047). Lệnh nâng mức chưa tính Bác sĩ trực: tính ở đây, gắn dấu FR-047b và gửi thông báo
     * khẩn cấp; lệnh ghi nhận đã gửi nhóm người nhận khẩn cấp cùng thông báo ghi nhận nên sự kiện không mang yêu cầu.
     */
    @EventListener
    public void activate(EmergencyActivationRequested request) {
        long id = request.incidentId();
        Instant at = request.at();
        boolean noOnDutyDoctor;
        List<NotificationRequest> notices = List.of();
        if (request.noOnDutyDoctor() != null) {
            noOnDutyDoctor = request.noOnDutyDoctor();
        } else {
            noOnDutyDoctor = staff.map(port -> port.onDutyDoctors(at).isEmpty()).orElse(true);
            if (noOnDutyDoctor) incidents.markNoOnDutyDoctor(id);
            notices = IncidentNotices.emergencyActivated(
                    id, request.residentId(), request.floorAreaId(), noOnDutyDoctor, request.actorId(), at);
        }
        store.saveCard(id, build(request.residentId(), at), request.actorId(), request.floorAreaId());
        events.publishEvent(new EmergencyIncidentActivated(
                id,
                request.residentId(),
                noOnDutyDoctor,
                ENTITY + ":" + id + ":EMERGENCY_ACTIVATED:" + at.toEpochMilli(),
                at,
                notices));
    }

    /** {@code GET /incidents/{id}/emergency-card}: bản chụp lần kích hoạt gần nhất, lọc theo quyền xem sức khỏe. */
    @Transactional(readOnly = true)
    public EmergencyCard card(long incidentId, CurrentUser user) {
        var incident = incidents.view(incidentId).orElseThrow(NotFoundException::new);
        var permission = permissions.hasPermission(
                user, new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.VIEW), target(incident));
        if (!permission.allowed()) {
            if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xem sự cố");
        }
        return forViewer(incidentId, user)
                .orElseThrow(() -> new ConditionNotMetException(
                        List.of(new UnmetCondition("FR-047", "Sự cố chưa kích hoạt quy trình khẩn cấp"))));
    }

    /**
     * Bản chụp gần nhất cho người xem (kết quả lệnh ghi nhận, kích hoạt): đủ nội dung khi được xem đủ nhóm sức khỏe của
     * người cao tuổi, ngược lại chỉ nhận dạng (FR-048). Không kiểm quyền xem sự cố.
     */
    public Optional<EmergencyCard> forViewer(long incidentId, CurrentUser user) {
        var incident = incidents.view(incidentId).orElseThrow(NotFoundException::new);
        return store.latestCard(incidentId)
                .map(card -> fullHealthAccess(user, incident) ? card : card.identificationOnly());
    }

    /** Người được xem đủ thẻ (FR-048, FR-048a): quyền xem nhóm sức khỏe của người cao tuổi ở mức đầy đủ. */
    public boolean fullHealthAccess(CurrentUser user, IncidentView incident) {
        return visibility.canView(user, InfoGroup.HEALTH, target(incident)) == Visibility.FULL;
    }

    /** Dựng thẻ tại {@code at}; mỗi nguồn lỗi chỉ làm mất mục của nó. */
    public EmergencyCard build(long residentId, Instant at) {
        var unavailable = new ArrayList<String>();
        var identification = read("identification", unavailable, () -> identification(residentId));
        var wish = read("endOfLifeWish", unavailable, () -> wish(wishes.current(residentId)));
        boolean criticalFlag = Boolean.TRUE.equals(read(
                "criticalFlag", unavailable, () -> critical.openFlag(residentId).isPresent()));
        var items = read(
                "healthItems",
                null,
                () -> healthItems.active(residentId, HealthItemType.ALLERGY, HealthItemType.CHRONIC_CONDITION));
        List<Map<String, Object>> allergies = null;
        List<Map<String, Object>> conditions = null;
        if (items == null) {
            unavailable.add("allergies");
            unavailable.add("chronicConditions");
        } else {
            allergies = read(
                    "allergies",
                    unavailable,
                    () -> items.stream()
                            .filter(item -> item.type() == HealthItemType.ALLERGY)
                            .map(this::allergy)
                            .toList());
            conditions = items.stream()
                    .filter(item -> item.type() == HealthItemType.CHRONIC_CONDITION)
                    .map(item -> named(item.freeText().orElse(null)))
                    .toList();
        }
        var medications = read(
                "currentMedications",
                unavailable,
                () -> prescriptions.activeMedications(residentId).stream()
                        .map(m -> {
                            var item = new LinkedHashMap<String, Object>();
                            item.put("name", m.medication().name());
                            item.put("strength", m.medication().strength());
                            item.put("dosage", m.dosage());
                            item.put("route", m.routeOfAdministration());
                            return (Map<String, Object>) item;
                        })
                        .toList());
        var careNotes = read(
                "careNotes",
                unavailable,
                () -> assessments
                        .currentCareNote(List.of(residentId))
                        .getOrDefault(residentId, Optional.empty())
                        .orElse(null));
        var contact = read(
                "primaryContact",
                unavailable,
                () -> family.flatMap(port -> port.primaryContact(residentId))
                        .map(c -> new EmergencyCard.Contact(
                                c.fullName(), c.phoneNumber().orElse(null)))
                        .orElse(null));
        return new EmergencyCard(
                identification,
                wish,
                criticalFlag,
                allergies,
                medications,
                conditions,
                careNotes,
                contact,
                List.copyOf(unavailable),
                at);
    }

    /** Nguyện vọng cuối đời Hiệu lực cho thẻ, bản tóm tắt; chưa có thì {@code notRecorded = true} (FR-048). */
    public static Map<String, Object> wish(Optional<EndOfLifeWishView> current) {
        var result = new LinkedHashMap<String, Object>();
        current.ifPresentOrElse(
                w -> {
                    result.put("id", w.id());
                    result.put("choice", w.choice().name());
                    result.put("firstContact", w.firstContact());
                    result.put("note", w.note());
                    result.put("signerType", w.signerType().name());
                    result.put("signedAt", w.signedAt());
                },
                () -> result.put("notRecorded", true));
        return result;
    }

    private EmergencyCard.Identification identification(long residentId) {
        var identity = residents.map(api -> api.identity(residentId)).orElse(null);
        String roomBed = locations
                .flatMap(api -> api.current(residentId))
                .map(l -> l.roomName() + " / " + l.bedCode())
                .orElse(null);
        return new EmergencyCard.Identification(
                identity == null ? null : identity.fullName(),
                identity == null
                        ? null
                        : identity.photoFileId().map(String::valueOf).orElse(null),
                null,
                roomBed);
    }

    private Map<String, Object> allergy(HealthItemView item) {
        String name = item.allergenId()
                .flatMap(allergens::allergen)
                .map(a -> a.name())
                .orElse(item.freeText().orElse(null));
        return named(name);
    }

    private static Map<String, Object> named(String name) {
        var item = new LinkedHashMap<String, Object>();
        item.put("name", name);
        return item;
    }

    private static <T> T read(String section, List<String> unavailable, Supplier<T> source) {
        try {
            return source.get();
        } catch (RuntimeException e) {
            if (unavailable != null) unavailable.add(section);
            return null;
        }
    }

    private static Target target(IncidentView incident) {
        return new Target(ENTITY, Long.toString(incident.id()), incident.residentId(), incident.floorAreaId());
    }
}
