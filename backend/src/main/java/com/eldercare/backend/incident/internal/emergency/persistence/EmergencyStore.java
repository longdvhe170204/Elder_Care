package com.eldercare.backend.incident.internal.emergency.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.incident.internal.emergency.domain.EmergencyCard;
import com.eldercare.backend.incident.internal.emergency.domain.EmergencyCardSnapshot;
import com.eldercare.backend.incident.internal.emergency.domain.TransferSummaryContent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Bản chụp thẻ khẩn cấp và bản tóm tắt chuyển viện trên sự kiện chung gắn sự cố (R-15:
 * {@code ban_chup_the_khan_cap}, {@code ban_tom_tat_chuyen_vien} → {@code su_kien_chung}; nhóm 3, chỉ thêm). Xác nhận
 * đối chiếu nguyện vọng ở {@code IncidentProgressionStore} (loại {@code M05.WISH_CHECK} khai báo một lần).
 */
@Repository
public class EmergencyStore {
    public static final String SUMMARY_TYPE = "M05.TRANSFER_SUMMARY";
    private static final SharedKind<EmergencyCardSnapshot> CARD =
            SharedKind.of("M05.EMERGENCY_CARD", EmergencyCardSnapshot.class);

    private static final SharedKind<TransferSummaryContent> SUMMARY =
            SharedKind.of(SUMMARY_TYPE, TransferSummaryContent.class);

    private final RecordEventApi events;

    public EmergencyStore(RecordEventApi events) {
        this.events = events;
    }

    public void saveCard(long incidentId, EmergencyCard card, long actorId, Long floorAreaId) {
        Instant at = card.snapshotAt();
        events.add(
                CARD,
                incident(incidentId),
                new NewRecordEvent<>(
                        new EmergencyCardSnapshot(card, List.of(actorId)),
                        null,
                        actorId,
                        false,
                        at,
                        null,
                        at,
                        floorAreaId,
                        null));
    }

    /** Bản chụp của lần kích hoạt gần nhất. */
    public Optional<EmergencyCard> latestCard(long incidentId) {
        var cards = events.list(CARD, incident(incidentId));
        return cards.isEmpty()
                ? Optional.empty()
                : Optional.of(cards.getLast().content().card());
    }

    public long addSummary(long incidentId, TransferSummaryContent content, Long floorAreaId) {
        Instant at = content.preparedAt();
        return events.add(
                        SUMMARY,
                        incident(incidentId),
                        new NewRecordEvent<>(
                                content, null, content.preparedById(), false, at, null, at, floorAreaId, null))
                .id();
    }

    public RecordEvent<TransferSummaryContent> summary(long id) {
        return events.require(SUMMARY, id);
    }

    private static EntityRef incident(long incidentId) {
        return new EntityRef("M05.INCIDENT", incidentId);
    }
}
