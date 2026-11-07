package com.eldercare.backend.incident.internal.emergency.service;

import static com.eldercare.backend.incident.internal.emergency.domain.EmergencyActions.SUMMARY_PRINTED;
import static com.eldercare.backend.incident.internal.emergency.domain.EmergencyActions.TRANSFERRED_OUT;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.document.PdfDocument;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileService;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.emergency.domain.TransferOutCommand;
import com.eldercare.backend.incident.internal.emergency.domain.TransferOutInfo;
import com.eldercare.backend.incident.internal.emergency.domain.TransferRefusedException;
import com.eldercare.backend.incident.internal.emergency.domain.TransferSummaryContent;
import com.eldercare.backend.incident.internal.emergency.persistence.EmergencyStore;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentProgressionService;
import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TreatmentWishView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Chuyển viện từ sự cố và bản tóm tắt chuyển viện (FR-049, FR-050, FR-050h, BR-M05-14, BR-M05-32; research S-12,
 * S-25). Một transaction: lệnh {@code TRANSFER_OUT} của 001 với căn cứ là sự cố, bản tóm tắt, phiếu PDF lưu ở
 * {@code common.file}. 001 từ chối thì ném {@link TransferRefusedException}; {@link TransferOutCoordinator} ghi lần thử
 * sau khi lệnh đã rollback.
 */
@Service
public class TransferOutService {
    private static final String ENTITY = "M05.INCIDENT";
    private static final Set<String> TRANSFERABLE = Set.of("NEW", "IN_PROGRESS");
    private static final Set<Role> ROLES = Set.of(Role.NURSING_STAFF, Role.DOCTOR);
    private static final Set<Role> VIEWERS =
            Set.of(Role.DOCTOR, Role.NURSING_STAFF, Role.FLOOR_MANAGER, Role.NURSING_HOME_MANAGER);
    private static final String REFUSED = "TRANSFER_REFUSED";

    private final IncidentStore incidents;
    private final IncidentProgressionStore progressions;
    private final IncidentProgressionService progressionTypes;
    private final EmergencyStore store;
    private final EmergencyCardService cards;
    private final PermissionApi permissions;
    private final ResidentStatusApi statuses;
    private final EndOfLifeWishApi wishes;
    private final FileApi files;
    private final FileService fileContents;
    private final ParameterApi parameters;
    private final AccountApi accounts;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final Optional<VitalSignApi> vitalSigns;

    public TransferOutService(
            IncidentStore incidents,
            IncidentProgressionStore progressions,
            IncidentProgressionService progressionTypes,
            EmergencyStore store,
            EmergencyCardService cards,
            PermissionApi permissions,
            ResidentStatusApi statuses,
            EndOfLifeWishApi wishes,
            FileApi files,
            FileService fileContents,
            ParameterApi parameters,
            AccountApi accounts,
            AuditLogApi audit,
            BusinessTime time,
            @Autowired(required = false) VitalSignApi vitalSigns) {
        this.incidents = incidents;
        this.progressions = progressions;
        this.progressionTypes = progressionTypes;
        this.store = store;
        this.cards = cards;
        this.permissions = permissions;
        this.statuses = statuses;
        this.wishes = wishes;
        this.files = files;
        this.fileContents = fileContents;
        this.parameters = parameters;
        this.accounts = accounts;
        this.audit = audit;
        this.time = time;
        this.vitalSigns = Optional.ofNullable(vitalSigns);
    }

    /** {@code GET /incidents/{id}/transfer-out/info} (FR-049): chỉ hiển thị, không gợi ý cơ sở (Q-306). */
    @Transactional(readOnly = true)
    public TransferOutInfo info(long incidentId, CurrentUser user) {
        var incident = viewable(incidentId, user);
        long residentId = requireResident(incident);
        var treatment = wishes.currentTreatmentWish(residentId);
        var card = cards.build(residentId, time.now());
        return new TransferOutInfo(
                treatment.map(TransferOutService::treatmentWish).orElse(null),
                EmergencyCardService.wish(wishes.current(residentId)),
                card.primaryContact(),
                treatment.map(TreatmentWishView::askFamilyFirst).orElse(false),
                incident.level() == IncidentLevel.CRITICAL);
    }

    /** Lệnh Chuyển viện; trả mã bản tóm tắt. */
    @Transactional
    public long transfer(long incidentId, TransferOutCommand command, CurrentUser user) {
        if (ROLES.stream().noneMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Chỉ Điều dưỡng, Bác sĩ chuyển viện từ sự cố");
        }
        var locked = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        var incident = incidents.view(incidentId).orElseThrow(NotFoundException::new);
        var permission = permissions.hasPermission(
                user, new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE), target(incident));
        if (!permission.allowed()) {
            if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xử lý sự cố");
        }
        long residentId = requireResident(incident);
        if (!TRANSFERABLE.contains(locked.status())) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("FR-049", "Chỉ chuyển viện từ sự cố Mới hoặc Đang xử lý")));
        }
        if (blank(command.transferUnit())) throw new InvalidValueException("Thiếu cơ sở tiếp nhận");
        if (command.transferAt() == null) throw new InvalidValueException("Thiếu thời điểm chuyển");
        if (blank(command.companion())) throw new InvalidValueException("Thiếu người đi kèm");
        var treatment = wishes.currentTreatmentWish(residentId);
        boolean mustAskFirst = treatment.map(TreatmentWishView::askFamilyFirst).orElse(false);
        String notAskedReason = null;
        if (mustAskFirst) {
            if (command.requiredContactReached() == null) {
                throw new InvalidValueException(
                        "Nguyện vọng khi điều trị cần hỏi gia đình trước: ghi đã liên lạc chưa");
            }
            // Plan điểm báo lại 11: sự cố Khẩn cấp không bắt lý do.
            if (!command.requiredContactReached()) {
                if (!blank(command.notAskedFirstReason())) {
                    notAskedReason = new Reason(command.notAskedFirstReason()).value();
                } else if (locked.level() != IncidentLevel.CRITICAL) {
                    throw new ReasonRequiredException();
                }
            }
        }

        var now = time.now();
        var result = execute(residentId, incidentId, command, user);
        var card = cards.build(residentId, now);
        String preparedBy = accounts.fullName(user.accountId()).orElse(Long.toString(user.accountId()));
        var draft = new TransferSummaryContent(
                residentId,
                card.identification() == null ? null : card.identification().fullName(),
                command.transferUnit(),
                command.transferAt(),
                command.expectedReturnAt(),
                command.companion(),
                command.requiredContactReached(),
                notAskedReason,
                card.allergies(),
                card.currentMedications(),
                latestVitalSigns(residentId),
                progressions.list(incidentId),
                card.endOfLifeWish(),
                treatment.map(TransferOutService::treatmentWish).orElse(null),
                treatment.map(TreatmentWishView::id).orElse(null),
                card.primaryContact(),
                0L,
                user.accountId(),
                preparedBy,
                now,
                result.historyId());
        byte[] pdf = TransferSummaryDocument.render(draft, facility());
        long fileId = files.upload(
                "phieu-tom-tat-chuyen-vien-" + incidentId + ".pdf", "application/pdf", pdf, user.accountId());
        long summaryId = store.addSummary(incidentId, withDocument(draft, fileId), incident.floorAreaId());
        files.attach(fileId, EmergencyStore.SUMMARY_TYPE, summaryId);
        audit.write(AuditLogEntry.builder()
                .action(TRANSFERRED_OUT)
                .entity(ENTITY, incidentId)
                .resident(residentId)
                .after(Map.of("summaryId", summaryId, "receivingFacility", command.transferUnit()))
                .build());
        // FR-050h: lần tải đầu trong lệnh cũng là một lần in.
        printed(summaryId, residentId, false);
        return summaryId;
    }

    /** Ghi lần thử bị 001 từ chối thành diễn biến {@code TRANSFER_REFUSED} (FR-049), sau khi lệnh đã rollback. */
    @Transactional
    public void recordRefusal(long incidentId, BusinessException refusal, CurrentUser user) {
        var incident = incidents.view(incidentId).orElseThrow(NotFoundException::new);
        String detail = refusal instanceof ConditionNotMetException unmet
                ? unmet.unmetConditions().stream()
                        .map(UnmetCondition::description)
                        .collect(Collectors.joining("; "))
                : refusal.getMessage();
        var now = time.now();
        progressions.add(
                incidentId,
                progressionTypes.typeIdByCode(REFUSED),
                REFUSED,
                "Chuyển viện bị từ chối: " + detail,
                now,
                user.accountId(),
                false,
                incident.floorAreaId(),
                now);
    }

    @Transactional(readOnly = true)
    public RecordEvent<TransferSummaryContent> summary(long summaryId, CurrentUser user) {
        var summary = store.summary(summaryId);
        viewableSummary(summary.parent().entityId(), user);
        return summary;
    }

    /** Phiếu PDF đã lưu, đúng byte đã dựng trong lệnh (FR-050h). */
    @Transactional(readOnly = true)
    public byte[] document(long summaryId, CurrentUser user) {
        var summary = summary(summaryId, user);
        return fileContents
                .download(summary.content().documentFileId(), user.accountId())
                .content();
    }

    /** In lại: file đã lưu đóng thêm dấu "Bản in lại"; ghi lần in vào nhật ký (FR-050h, 19.4). */
    @Transactional
    public byte[] reprint(long summaryId, CurrentUser user) {
        var summary = summary(summaryId, user);
        byte[] stored = fileContents
                .download(summary.content().documentFileId(), user.accountId())
                .content();
        printed(summaryId, summary.content().residentId(), true);
        return PdfDocument.stamp(stored, TransferSummaryDocument.REPRINT);
    }

    private com.eldercare.backend.resident.api.dto.TransitionResult execute(
            long residentId, long incidentId, TransferOutCommand command, CurrentUser user) {
        var factory = JsonNodeFactory.instance;
        ObjectNode details = factory.objectNode();
        details.put("transferUnit", command.transferUnit());
        if (command.expectedReturnAt() != null) {
            details.put("expectedReturnAt", command.expectedReturnAt().toString());
        }
        if (command.handoverStaffId() != null) details.put("handoverStaffId", command.handoverStaffId());
        details.putObject("basis").put("incidentId", incidentId);
        if (!command.attachmentIds().isEmpty()) {
            var attachments = details.putArray("attachmentFileIds");
            command.attachmentIds().forEach(attachments::add);
        }
        try {
            return statuses.execute(
                    residentId,
                    StatusCommand.TRANSFER_OUT,
                    user,
                    Optional.of(new Reason("Chuyển viện theo sự cố " + incidentId)),
                    details,
                    new Basis(null, null, incidentId, null));
        } catch (BusinessException refusal) {
            throw new TransferRefusedException(refusal);
        }
    }

    private void printed(long summaryId, long residentId, boolean reprint) {
        audit.write(AuditLogEntry.builder()
                .action(SUMMARY_PRINTED)
                .entity(EmergencyStore.SUMMARY_TYPE, summaryId)
                .resident(residentId)
                .after(Map.of("reprint", reprint))
                .build());
    }

    private List<Map<String, Object>> latestVitalSigns(long residentId) {
        return vitalSigns
                .map(api -> api.latestPerType(residentId).values().stream()
                        .map(v -> {
                            var item = new LinkedHashMap<String, Object>();
                            item.put("indicatorName", v.indicatorName());
                            item.put("values", v.values());
                            item.put("unit", v.unit());
                            item.put("measuredAt", v.measuredAt());
                            return (Map<String, Object>) item;
                        })
                        .toList())
                .orElse(List.of());
    }

    private static Map<String, Object> treatmentWish(TreatmentWishView w) {
        var item = new LinkedHashMap<String, Object>();
        item.put("id", w.id());
        item.put(
                "preferredHospitals",
                w.preferredHospitals().stream()
                        .map(h -> Map.of("name", h.name(), "address", h.address() == null ? "" : h.address()))
                        .toList());
        item.put("askFamilyFirst", w.askFamilyFirst());
        item.put(
                "notifyContacts",
                w.notifyContacts().stream()
                        .map(c -> Map.of(
                                "familyMemberId", c.familyMemberId(),
                                "fullName", c.fullName(),
                                "phoneNumber", c.phoneNumber() == null ? "" : c.phoneNumber()))
                        .toList());
        item.put(
                "accompaniedBy",
                w.accompaniedBy() == null ? null : w.accompaniedBy().name());
        item.put("note", w.note());
        item.put("at", w.at());
        return item;
    }

    private static TransferSummaryContent withDocument(TransferSummaryContent s, long fileId) {
        return new TransferSummaryContent(
                s.residentId(),
                s.residentName(),
                s.receivingFacility(),
                s.transferAt(),
                s.expectedReturnAt(),
                s.companion(),
                s.requiredContactReached(),
                s.notAskedFirstReason(),
                s.allergies(),
                s.currentMedications(),
                s.latestVitalSigns(),
                s.progressions(),
                s.endOfLifeWish(),
                s.treatmentWish(),
                s.treatmentWishVersionId(),
                s.primaryContact(),
                fileId,
                s.preparedById(),
                s.preparedBy(),
                s.preparedAt(),
                s.statusHistoryId());
    }

    private FacilityInfo facility() {
        var value = parameters.read(ParameterCode.CFG_M11_11);
        return value.isSet() ? value.asTable(FacilityInfo.class) : null;
    }

    private IncidentView viewable(long incidentId, CurrentUser user) {
        var incident = incidents.view(incidentId).orElseThrow(NotFoundException::new);
        var permission = permissions.hasPermission(
                user, new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.VIEW), target(incident));
        if (!permission.allowed()) {
            if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xem sự cố");
        }
        return incident;
    }

    /** Bản tóm tắt: Bác sĩ, Điều dưỡng, Trưởng tầng trong phạm vi, Quản lý viện (api-007). */
    private void viewableSummary(long incidentId, CurrentUser user) {
        if (VIEWERS.stream().noneMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Không có quyền xem bản tóm tắt chuyển viện");
        }
        viewable(incidentId, user);
    }

    private static long requireResident(IncidentView incident) {
        if (incident.residentId() == null) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-049", "Sự cố không gắn người cao tuổi")));
        }
        return incident.residentId();
    }

    private static Target target(IncidentView incident) {
        return new Target(ENTITY, Long.toString(incident.id()), incident.residentId(), incident.floorAreaId());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
