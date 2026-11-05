package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverRow;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import com.eldercare.backend.belongings.internal.belonging.domain.IntakeGroupMember;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingResidentLock;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.NotificationLookupApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Đính chính bản ghi bàn giao đồ gửi ({@code DinhChinhDuocProvider} {@code M12.BELONGING_HANDOVER}; 013 T013; FR-011a,
 * Q-160, research K-05): không gắn tầng (cách A người ghi gốc, cách C yêu cầu phê duyệt); chỉ sửa trường nội dung, không
 * đổi loại hay trạng thái đích; Hủy ghi nhận chỉ bản ghi gần nhất của đồ. Giới hạn theo bản gốc kiểm ở
 * {@link #afterCorrection} (trong transaction của đính chính) vì {@code allows} không biết kiểu đính chính; sau đó tính
 * lại đồ ({@link BelongingRecalculation}) và báo người đã nhận thông báo về bản ghi bị hủy.
 */
@Component
class BelongingHandoverCorrection implements CorrectableRecordProvider {

    private static final Set<String> FIELDS = Set.of(
            "occurredAt",
            "quantity",
            "conditionLevelId",
            "conditionDescription",
            "storageLocationId",
            "giverName",
            "giverIdDocument",
            "recipientName",
            "recipientStaffId",
            "addedPhotoFileIds");

    private final BelongingHandoverStore handovers;
    private final BelongingStore belongings;
    private final BelongingResidentLock residentLock;
    private final BelongingRecalculation recalculation;
    private final CatalogApi catalogs;
    private final FileApi files;
    private final NotificationLookupApi lookup;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    BelongingHandoverCorrection(
            BelongingHandoverStore handovers,
            BelongingStore belongings,
            BelongingResidentLock residentLock,
            BelongingRecalculation recalculation,
            CatalogApi catalogs,
            FileApi files,
            NotificationLookupApi lookup,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.handovers = handovers;
        this.belongings = belongings;
        this.residentLock = residentLock;
        this.recalculation = recalculation;
        this.catalogs = catalogs;
        this.files = files;
        this.lookup = lookup;
        this.events = events;
        this.time = time;
    }

    @Override
    public String recordType() {
        return BelongingRecalculation.RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return handovers.find(id).isPresent();
    }

    @Override
    public long originalRecorder(long id) {
        return find(id).recordedBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> residentId(long id) {
        return handovers.find(id).map(HandoverRow::residentId);
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.OFFICE_STAFF) || user.roles().contains(Role.NURSING_STAFF);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException("Đính chính bàn giao chỉ sửa trường nội dung " + FIELDS
                    + "; không đổi loại bàn giao, trạng thái đích (FR-011a)");
        }
        if (map.containsKey("occurredAt")) {
            try {
                Instant.parse(String.valueOf(map.get("occurredAt")));
            } catch (DateTimeParseException e) {
                throw new InvalidValueException("occurredAt phải theo ISO-8601");
            }
        }
        if (map.containsKey("quantity")
                && !(map.get("quantity") instanceof Number n && new BigDecimal(n.toString()).signum() > 0)) {
            throw new InvalidValueException("quantity phải lớn hơn 0");
        }
        for (String key : List.of("conditionLevelId", "storageLocationId", "recipientStaffId")) {
            if (map.containsKey(key) && !(map.get(key) instanceof Number n && n.longValue() > 0)) {
                throw new InvalidValueException(key + " phải là mã số nguyên dương");
            }
        }
        if (map.containsKey("addedPhotoFileIds")
                && !(map.get("addedPhotoFileIds") instanceof List<?> list
                        && list.stream().allMatch(v -> v instanceof Number))) {
            throw new InvalidValueException("addedPhotoFileIds phải là danh sách mã tệp");
        }
    }

    @Override
    public Object originalContent(long id) {
        HandoverRow row = find(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("occurredAt", row.occurredAt().toString());
        content.put("quantity", row.amount());
        content.put("conditionLevelId", row.conditionLevelId());
        content.put("storageLocationId", row.storageLocationId());
        if (row.recipientType() == GiverType.STAFF) {
            content.put("recipientStaffId", row.recipientId());
        }
        return content;
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        HandoverRow original = find(correction.originalRecordId());
        long residentId = original.residentId();
        long belongingId = original.belongingId();
        residentLock.lock(residentId);
        long intake = belongings.intakeHandoverOf(belongingId).orElseThrow(NotFoundException::new);
        List<IntakeGroupMember> group = belongings.lockIntakeGroup(residentId, intake);
        boolean firstRecord =
                handovers.ofBelongings(List.of(belongingId)).getFirst().id() == original.id();
        if (correction.type() == CorrectionType.VOID) {
            boolean later =
                    recalculation.effective(List.of(belongingId)).stream().anyMatch(r -> r.id() > original.id());
            boolean splitChildren = firstRecord
                    && group.stream()
                            .anyMatch(m -> !m.excluded()
                                    && m.originalBelongingId() != null
                                    && m.originalBelongingId() == belongingId);
            if (later || splitChildren) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "NOT_LATEST_HANDOVER",
                        "Hủy ghi nhận chỉ áp cho bản ghi bàn giao gần nhất của đồ gửi (FR-011a)");
            }
        } else if (correction.correctedContent() instanceof Map<?, ?> content) {
            checkAmendment(original, firstRecord, content);
        }
        recalculation.recalculate(residentId, intake);
        if (correction.type() == CorrectionType.VOID) {
            notifyVoided(original);
        }
    }

    private void checkAmendment(HandoverRow original, boolean firstRecord, Map<?, ?> content) {
        if (content.containsKey("quantity") && !firstRecord) {
            // Số lượng của đồ lấy từ bản ghi đầu tiên của nó (BelongingAmounts); sửa bản ghi sau làm lệch bất biến
            // SC-001
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "QUANTITY_CORRECTION_NOT_ALLOWED",
                    "Chỉ đính chính số lượng ở bản ghi Tiếp nhận hoặc bản ghi tách ra đồ (FR-012, SC-001)");
        }
        if (content.containsKey("recipientStaffId") && original.recipientType() != GiverType.STAFF) {
            throw new InvalidValueException("Bản ghi này không có người nhận là nhân viên");
        }
        if (content.containsKey("storageLocationId")) {
            if (original.targetStatus() != BelongingStatus.HELD && original.targetStatus() != BelongingStatus.DAMAGED) {
                throw new InvalidValueException("Bản ghi này không có vị trí lưu giữ");
            }
            catalogs.require(
                    BelongingCatalogs.STORAGE_LOCATION,
                    Long.parseLong(content.get("storageLocationId").toString()));
        }
        if (content.containsKey("conditionLevelId")) {
            var level = catalogs.require(
                    BelongingCatalogs.CONDITION_LEVEL,
                    Long.parseLong(content.get("conditionLevelId").toString()));
            if (level.attributes().unusable()
                    && original.kind() != HandoverType.INTAKE
                    && original.kind() != HandoverType.RECORD_DAMAGE) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "UNUSABLE_LEVEL_ONLY_VIA_RECORD_DAMAGE",
                        "Mức Không dùng được chỉ ở Tiếp nhận hoặc Ghi hư hỏng (FR-015)");
            }
        }
        if (content.get("addedPhotoFileIds") instanceof List<?> photos) {
            for (Object fileId : photos) {
                files.attach(Long.parseLong(fileId.toString()), IntakeService.PHOTO_OWNER, original.id());
            }
        }
    }

    /** FR-011a, FR-032: người đã nhận thông báo về sự kiện của bản ghi bị hủy nhận đính chính (Nhẹ). */
    private void notifyVoided(HandoverRow original) {
        Set<RecipientGroup> recipients = new LinkedHashSet<>();
        for (var person : lookup.bySource(BelongingRecalculation.RECORD_TYPE, Long.toString(original.id()))) {
            recipients.add(
                    switch (person.recipientType()) {
                        case STAFF -> new RecipientGroup.Staff(person.recipientId(), "CORRECTION");
                        case FAMILY_MEMBER -> new RecipientGroup.FamilyMember(person.recipientId(), "CORRECTION");
                        case RESIDENT -> new RecipientGroup.Resident(person.recipientId());
                    });
        }
        if (recipients.isEmpty()) {
            return;
        }
        String typeName = belongings
                .find(original.belongingId())
                .map(b -> catalogs.require(BelongingCatalogs.BELONGING_TYPE, b.typeId())
                        .name())
                .orElse("Đồ gửi");
        events.publishEvent(BelongingNotices.notice(
                "BELONGING_HANDOVER_VOIDED",
                BelongingRecalculation.RECORD_TYPE,
                original.id(),
                NotificationLevel.LOW,
                original.residentId(),
                List.copyOf(recipients),
                typeName + ": bản ghi bàn giao đã được hủy ghi nhận (ghi nhầm)",
                null,
                time.now()));
    }

    private HandoverRow find(long id) {
        return handovers.find(id).orElseThrow(NotFoundException::new);
    }
}
