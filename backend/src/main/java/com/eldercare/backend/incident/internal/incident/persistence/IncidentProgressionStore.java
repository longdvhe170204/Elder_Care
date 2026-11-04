package com.eldercare.backend.incident.internal.incident.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.incident.internal.incident.domain.ProgressionContent;
import java.time.Instant;
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

    public boolean wishChecked(long incidentId) {
        return !records.list(WISH_CHECK, incident(incidentId)).isEmpty();
    }

    private static EntityRef incident(long incidentId) {
        return new EntityRef("M05.INCIDENT", incidentId);
    }
}
