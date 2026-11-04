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
    /** Xác nhận "đã đối chiếu nguyện vọng" (FR-048a); lệnh xác nhận ghi ở US5 (T041). */
    @SuppressWarnings("rawtypes")
    static final SharedKind<Map> WISH_CHECK = SharedKind.of("M05.WISH_CHECK", Map.class);

    /** Người tham gia xử lý (data-model {@code nguoi_xu_ly_su_co}, vai trò {@code THAM_GIA}; FR-048b). */
    @SuppressWarnings("rawtypes")
    static final SharedKind<Map> HANDLER = SharedKind.of("M05.INCIDENT_HANDLER", Map.class);

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

    public boolean isHandler(long incidentId, long staffAccountId) {
        return records.findByUniqueKey(HANDLER, handlerKey(incidentId, staffAccountId))
                .isPresent();
    }

    private static String handlerKey(long incidentId, long staffAccountId) {
        return "INCIDENT_HANDLER:" + incidentId + ":" + staffAccountId + ":JOINED";
    }

    public boolean wishChecked(long incidentId) {
        return !records.list(WISH_CHECK, incident(incidentId)).isEmpty();
    }

    private static EntityRef incident(long incidentId) {
        return new EntityRef("M05.INCIDENT", incidentId);
    }
}
