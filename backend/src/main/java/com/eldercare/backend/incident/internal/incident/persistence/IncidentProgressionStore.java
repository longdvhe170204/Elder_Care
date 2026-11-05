package com.eldercare.backend.incident.internal.incident.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.incident.internal.incident.domain.ProgressionContent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Diễn biến sự cố và xác nhận đối chiếu nguyện vọng trong bảng sự kiện chung (R-15: {@code dien_bien_su_co},
 * {@code xac_nhan_doi_chieu} → {@code su_kien_chung}); chỉ thêm, không sửa (nhóm 3).
 */
@Repository
public class IncidentProgressionStore {
    static final SharedKind<ProgressionContent> PROGRESSION =
            SharedKind.of("M05.INCIDENT_PROGRESSION", ProgressionContent.class);
    /** Xác nhận "đã đối chiếu nguyện vọng" (FR-048a, DBR-19): khóa duy nhất {@code WISH_CHECK:<incidentId>}. */
    @SuppressWarnings("rawtypes")
    static final SharedKind<Map> WISH_CHECK = SharedKind.of("M05.WISH_CHECK", Map.class);

    /** Người tham gia xử lý (data-model {@code nguoi_xu_ly_su_co}, vai trò {@code THAM_GIA}; FR-048b). */
    @SuppressWarnings("rawtypes")
    static final SharedKind<Map> HANDLER = SharedKind.of("M05.INCIDENT_HANDLER", Map.class);

    @SuppressWarnings("rawtypes")
    static final SharedKind<Map> LEVEL_CHANGE = SharedKind.of("M05.INCIDENT_LEVEL_CHANGE", Map.class);

    @SuppressWarnings("rawtypes")
    static final SharedKind<Map> ASSIGNMENT = SharedKind.of("M05.INCIDENT_ASSIGNMENT", Map.class);

    private final RecordEventApi records;

    public IncidentProgressionStore(RecordEventApi records) {
        this.records = records;
    }

    public long add(
            long incidentId,
            long typeId,
            String typeCode,
            String content,
            Instant occurredAt,
            long actorId,
            boolean outOfScope,
            Long floorAreaId,
            Instant at) {
        return records.add(
                        PROGRESSION,
                        incident(incidentId),
                        new NewRecordEvent<>(
                                new ProgressionContent(typeId, typeCode, content, outOfScope),
                                null,
                                actorId,
                                false,
                                occurredAt,
                                at,
                                at,
                                floorAreaId,
                                null))
                .id();
    }

    /** Loại của mọi diễn biến đã ghi, để kiểm điều kiện chuyển theo dõi (bảng FR-045). */
    public List<Long> typeIds(long incidentId) {
        return records.list(PROGRESSION, incident(incidentId)).stream()
                .map(event -> event.content().progressionTypeId())
                .toList();
    }

    /** Tham gia xử lý sự cố Khẩn cấp (FR-041): một dòng mỗi người, gọi lại không ghi thêm. */
    public void join(long incidentId, long staffAccountId, Instant at, Long floorAreaId) {
        String key = handlerKey(incidentId, staffAccountId);
        if (records.findByUniqueKey(HANDLER, key).isPresent()) return;
        records.add(
                HANDLER,
                incident(incidentId),
                new NewRecordEvent<>(
                        Map.of("role", "JOINED"), null, staffAccountId, false, at, at, at, floorAreaId, key));
    }

    /** Lịch sử phân công (data-model {@code phan_cong_su_co}, nhóm 3, R-15 → {@code su_kien_chung}; FR-045a). */
    public void recordAssignment(
            long incidentId,
            long assignedBy,
            long assignee,
            String note,
            String statusAtAssignment,
            Instant at,
            Long floorAreaId) {
        records.add(
                ASSIGNMENT,
                incident(incidentId),
                new NewRecordEvent<>(
                        Map.of(
                                "assignedBy",
                                assignedBy,
                                "assignee",
                                assignee,
                                "note",
                                note,
                                "statusAtAssignment",
                                statusAtAssignment),
                        null,
                        assignedBy,
                        false,
                        at,
                        at,
                        at,
                        floorAreaId,
                        null));
    }

    public boolean isHandler(long incidentId, long staffAccountId) {
        return records.findByUniqueKey(HANDLER, handlerKey(incidentId, staffAccountId))
                .isPresent();
    }

    private static String handlerKey(long incidentId, long staffAccountId) {
        return "INCIDENT_HANDLER:" + incidentId + ":" + staffAccountId + ":JOINED";
    }

    public boolean wishChecked(long incidentId) {
        return records.findByUniqueKey(WISH_CHECK, wishCheckKey(incidentId)).isPresent();
    }

    /** Lệnh xác nhận đối chiếu nguyện vọng (US5, T041); mỗi sự cố một dòng. */
    public void addWishCheck(long incidentId, long wishVersionId, long actorId, Instant at, Long floorAreaId) {
        records.add(
                WISH_CHECK,
                incident(incidentId),
                new NewRecordEvent<>(
                        Map.of("wishVersionId", wishVersionId),
                        null,
                        actorId,
                        false,
                        at,
                        null,
                        at,
                        floorAreaId,
                        wishCheckKey(incidentId)));
    }

    private static String wishCheckKey(long incidentId) {
        return "WISH_CHECK:" + incidentId;
    }

    private static EntityRef incident(long incidentId) {
        return new EntityRef("M05.INCIDENT", incidentId);
    }

    /** Diễn biến theo thứ tự ghi (api-007 {@code Incident.progressions}). */
    public List<Map<String, Object>> list(long incidentId) {
        return records.list(PROGRESSION, incident(incidentId)).stream()
                .map(event -> {
                    var item = new java.util.LinkedHashMap<String, Object>();
                    item.put("id", event.id());
                    item.put("progressionTypeId", event.content().progressionTypeId());
                    item.put("typeCode", event.content().typeCode());
                    item.put("content", event.content().content());
                    item.put("occurredAt", event.occurredAt());
                    item.put("recordedBy", event.actorId());
                    item.put("outOfScope", event.content().outOfScope());
                    return (Map<String, Object>) item;
                })
                .toList();
    }

    /** Mã tài khoản người tham gia xử lý (lệnh tham gia). */
    public List<Long> handlerAccountIds(long incidentId) {
        return records.list(HANDLER, incident(incidentId)).stream()
                .map(event -> event.actorId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    /** Một diễn biến theo mã (cho đính chính); không có thì ném lỗi không tìm thấy của kho chung. */
    public com.eldercare.backend.administration.shared.api.dto.RecordEvent<ProgressionContent> get(long id) {
        return records.require(PROGRESSION, id);
    }

    /** Đổi mức sự cố (data-model {@code doi_muc_su_co}, R-15 → {@code su_kien_chung}; FR-044). */
    public void recordLevelChange(
            long incidentId, String from, String to, String reason, long actorId, Instant at, Long floorAreaId) {
        var content = new java.util.LinkedHashMap<String, Object>();
        content.put("from", from);
        content.put("to", to);
        content.put("reason", reason);
        records.add(
                LEVEL_CHANGE,
                incident(incidentId),
                new NewRecordEvent<>(content, null, actorId, false, at, at, at, floorAreaId, null));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> levelChanges(long incidentId) {
        return records.list(LEVEL_CHANGE, incident(incidentId)).stream()
                .map(event -> {
                    var item = new java.util.LinkedHashMap<String, Object>((Map<String, Object>) event.content());
                    item.put("changedBy", event.actorId());
                    item.put("changedAt", event.occurredAt());
                    return (Map<String, Object>) item;
                })
                .toList();
    }
}
