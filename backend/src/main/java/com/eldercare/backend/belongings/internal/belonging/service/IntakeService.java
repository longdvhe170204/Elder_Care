package com.eldercare.backend.belongings.internal.belonging.service;

import static com.eldercare.backend.belongings.internal.belonging.domain.BelongingActions.BELONGING_INTAKE;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.api.dto.HolderType;
import com.eldercare.backend.belongings.api.events.BelongingStatusChanged;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import com.eldercare.backend.belongings.internal.belonging.domain.IntakeCommand;
import com.eldercare.backend.belongings.internal.belonging.domain.NewBelonging;
import com.eldercare.backend.belongings.internal.belonging.domain.NewHandover;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingHandoverStore;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingResidentLock;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.belongings.internal.catalog.domain.ValuableCategories;
import com.eldercare.backend.belongings.internal.consent.service.SelfCustodyConsentService;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh Tiếp nhận đồ gửi và Tiếp nhận và giao ngay ({@code TiepNhanService}; 013 T010; FR-003 → FR-009, DBR-22,
 * BR-M12-05, BR-M12-06): tạo đồ Đang giữ và đúng một bản ghi Tiếp nhận cùng transaction; giao ngay thêm bản ghi Giao sử
 * dụng; đồ có giá trị theo CFG-M12-01 lúc lệnh bắt buộc ảnh.
 */
@Service
public class IntakeService {

    public static final String PHOTO_OWNER = "M12.BELONGING_PHOTO";

    /** FR-006: trạng thái người cao tuổi cho phép Tiếp nhận. */
    private static final Set<ResidentStatus> INTAKE_STATUSES = Set.of(
            ResidentStatus.ADMISSION_IN_PROGRESS,
            ResidentStatus.IN_RESIDENCE,
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.OFF_SITE_ACTIVITY,
            ResidentStatus.HOSPITALIZED);

    private final BelongingResidentLock residentLock;
    private final BelongingStore belongings;
    private final BelongingHandoverStore handovers;
    private final CatalogApi catalogs;
    private final ParameterApi parameters;
    private final ResidentApi residents;
    private final FamilyRightsApi family;
    private final FileApi files;
    private final SelfCustodyConsentService consents;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    IntakeService(
            BelongingResidentLock residentLock,
            BelongingStore belongings,
            BelongingHandoverStore handovers,
            CatalogApi catalogs,
            ParameterApi parameters,
            ResidentApi residents,
            FamilyRightsApi family,
            FileApi files,
            SelfCustodyConsentService consents,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.residentLock = residentLock;
        this.belongings = belongings;
        this.handovers = handovers;
        this.catalogs = catalogs;
        this.parameters = parameters;
        this.residents = residents;
        this.family = family;
        this.files = files;
        this.consents = consents;
        this.audit = audit;
        this.events = events;
        this.time = time;
        this.users = users;
    }

    /** Hành chính; Điều dưỡng trong phạm vi (FR-007): khóa "Đồ gửi" thao tác Tạo theo người cao tuổi. */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.CREATE)
    @Transactional
    public BelongingView intake(Target resident, IntakeCommand command) {
        CurrentUser user = users.getObject();
        long staffId =
                user.staffId().orElseThrow(() -> new PermissionDeniedException("Chỉ nhân viên tiếp nhận đồ gửi"));
        long residentId = command.residentId();
        residentLock.lock(residentId);
        if (!INTAKE_STATUSES.contains(residents.status(residentId))) {
            throw blocked("RESIDENT_FINAL_STATE", "Người cao tuổi không ở trạng thái cho phép tiếp nhận đồ (FR-006)");
        }
        var type = catalogs.requireActive(BelongingCatalogs.BELONGING_TYPE, command.typeId());
        BigDecimal quantity = command.quantity();
        BigDecimal cash = command.cashAmount();
        boolean cashType = type.attributes().hasCashAmount();
        if (cashType
                ? cash == null || quantity != null || cash.signum() <= 0
                : quantity == null || cash != null || quantity.signum() <= 0) {
            throw invalid("INVALID_VALUE", cashType ? "Tiền mặt ghi số tiền (đồng)" : "Ghi số lượng lớn hơn 0");
        }
        catalogs.requireActive(BelongingCatalogs.CONDITION_LEVEL, command.conditionLevelId());
        requireGiver(command, residentId);
        Long location = command.storageLocationId();
        if (location == null && !command.giveForUseNow()) {
            throw invalid("STORAGE_LOCATION_REQUIRED", "Ghi vị trí lưu giữ khi đồ vào Đang giữ (FR-003)");
        }
        if (location != null
                && catalogs.require(BelongingCatalogs.STORAGE_LOCATION, location)
                                .status()
                        != CatalogStatus.ACTIVE) {
            throw blocked("STORAGE_LOCATION_INACTIVE", "Vị trí lưu giữ đã ngừng hiệu lực");
        }
        boolean valuable = valuable(type.name());
        if (valuable && command.photoFileIds().isEmpty()) {
            throw blocked("PHOTO_REQUIRED", "Đồ có giá trị bắt buộc có ảnh (BR-M12-05)");
        }
        boolean consentType = type.attributes().requiresConsentForUse();
        if (command.giveForUseNow() && consentType) {
            if (command.giverType() != GiverType.FAMILY_MEMBER
                    || !family.isRepresentative(command.giverId(), residentId, time.now())) {
                throw blocked(
                        "GIVER_NOT_REPRESENTATIVE",
                        "Giao ngay đồ cần đồng ý chỉ khi người giao là người đại diện ký đồng ý (FR-005)");
            }
            if (command.consentSignedFormFileId() == null) {
                throw blocked("SELF_CUSTODY_CONSENT_REQUIRED", "Thiếu bản ký đồng ý cho tự giữ (FR-005, BR-M12-06)");
            }
        }

        Instant now = time.now();
        long belongingId = belongings.insert(new NewBelonging(
                residentId,
                type.id(),
                command.description().strip(),
                quantity,
                cash,
                command.unit(),
                BelongingStatus.HELD,
                HolderType.STAFF,
                staffId,
                location,
                command.conditionLevelId(),
                user.accountId(),
                now));
        long intakeId = handovers.insert(new NewHandover(
                belongingId,
                residentId,
                HandoverType.INTAKE,
                BelongingStatus.HELD,
                command.giverType(),
                command.giverId(),
                command.giverName(),
                command.giverIdDocument(),
                GiverType.STAFF,
                staffId,
                null,
                command.at(),
                quantity,
                cash,
                command.conditionLevelId(),
                command.conditionDescription(),
                location,
                null,
                false,
                valuable,
                command.photoFileIds(),
                command.note(),
                user.accountId(),
                now));
        belongings.setIntakeHandover(belongingId, intakeId);
        belongings.applyHandover(
                belongingId,
                BelongingStatus.HELD,
                HolderType.STAFF,
                staffId,
                location,
                command.conditionLevelId(),
                intakeId,
                now);
        for (long fileId : command.photoFileIds()) {
            files.attach(fileId, PHOTO_OWNER, intakeId);
        }
        events.publishEvent(
                new BelongingStatusChanged(belongingId, residentId, null, BelongingStatus.HELD, intakeId, now));

        if (command.giveForUseNow()) {
            if (consentType) {
                consents.recordInIntake(residentId, belongingId, command.giverId(), command.consentSignedFormFileId());
            }
            long giveId = handovers.insert(new NewHandover(
                    belongingId,
                    residentId,
                    HandoverType.GIVE_FOR_USE,
                    BelongingStatus.IN_USE,
                    GiverType.STAFF,
                    staffId,
                    null,
                    null,
                    GiverType.RESIDENT,
                    residentId,
                    null,
                    command.at(),
                    quantity,
                    cash,
                    command.conditionLevelId(),
                    command.conditionDescription(),
                    null,
                    null,
                    false,
                    valuable,
                    List.of(),
                    null,
                    user.accountId(),
                    now));
            belongings.applyHandover(
                    belongingId,
                    BelongingStatus.IN_USE,
                    HolderType.RESIDENT,
                    residentId,
                    null,
                    command.conditionLevelId(),
                    giveId,
                    now);
            events.publishEvent(new BelongingStatusChanged(
                    belongingId, residentId, BelongingStatus.HELD, BelongingStatus.IN_USE, giveId, now));
        }

        Map<String, Object> after = new HashMap<>();
        after.put("typeId", type.id());
        after.put("quantity", quantity);
        after.put("cashAmount", cash);
        after.put("storageLocationId", location);
        after.put("giveForUseNow", command.giveForUseNow());
        after.put("valuable", valuable);
        audit.write(AuditLogEntry.builder()
                .action(BELONGING_INTAKE)
                .entity("M12.BELONGING", belongingId)
                .resident(residentId)
                .after(after)
                .build());
        if (valuable) {
            events.publishEvent(BelongingNotices.notice(
                    "BELONGING_VALUABLE_INTAKE",
                    "M12.BELONGING",
                    belongingId,
                    NotificationLevel.LOW,
                    residentId,
                    List.of(new RecipientGroup.Representative(residentId)),
                    type.name() + ": viện đã tiếp nhận đồ gửi",
                    user.accountId(),
                    now));
        }
        return belongings.find(belongingId).orElseThrow(NotFoundException::new);
    }

    private void requireGiver(IntakeCommand command, long residentId) {
        GiverType giver = command.giverType();
        if (giver == null || giver == GiverType.STAFF) {
            throw invalid("INVALID_VALUE", "Người giao là người thân, người khác hoặc chính người cao tuổi (FR-003)");
        }
        if (giver == GiverType.FAMILY_MEMBER
                && (command.giverId() == null || !family.hasActiveRelationship(command.giverId(), residentId))) {
            throw invalid("INVALID_VALUE", "Người thân giao đồ phải có quan hệ Hiệu lực (FR-003)");
        }
        if (giver == GiverType.OTHER_PERSON && (isBlank(command.giverName()) || isBlank(command.giverIdDocument()))) {
            throw invalid("INVALID_VALUE", "Người khác giao đồ ghi họ tên và giấy tờ (FR-003)");
        }
    }

    private boolean valuable(String typeName) {
        var value = parameters.read(ParameterCode.CFG_M12_01);
        return value.isSet() && value.asTable(ValuableCategories.class).contains(typeName);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException blocked(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    private static BusinessException invalid(String code, String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, code, message);
    }
}
