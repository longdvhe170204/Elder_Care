package com.eldercare.backend.resident.internal.wish.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.internal.wish.domain.RecordedOpinion;
import com.eldercare.backend.resident.internal.wish.domain.WishOpinionContent;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Ý kiến khác kèm phiếu nguyện vọng (FR-023g, BR-M01-14) ở {@code record_event} loại {@code M01.WISH_OPINION}: ghi đã xác
 * nhận ngay khi thêm nên chỉ thêm, không sửa, không xóa. Đối tượng cha là loại bản ghi của phiếu ({@code
 * M01.END_OF_LIFE_WISH} hoặc {@code M01.TREATMENT_WISH}) để chuỗi lưu giữ xóa ý kiến cùng phiếu.
 */
@Component
public class WishOpinionStore {

    static final SharedKind<WishOpinionContent> KIND = SharedKind.of("M01.WISH_OPINION", WishOpinionContent.class);

    private final RecordEventApi events;

    WishOpinionStore(RecordEventApi events) {
        this.events = events;
    }

    /** Ghi các ý kiến của phiên bản phiếu {@code wishId}, trong transaction của lệnh ghi phiếu. */
    void add(String wishType, long wishId, List<WishOpinion> opinions, long recordedBy, Instant recordedAt) {
        EntityRef parent = new EntityRef(wishType, wishId);
        for (WishOpinion opinion : opinions) {
            OpinionSource source = opinion.source();
            events.add(
                    KIND,
                    parent,
                    new NewRecordEvent<>(
                            new WishOpinionContent(source.type().name(), source.familyMemberId(), opinion.content()),
                            null,
                            recordedBy,
                            false,
                            recordedAt,
                            null,
                            recordedAt,
                            null,
                            null));
        }
    }

    /** Ý kiến của phiếu theo thứ tự đã ghi. */
    List<RecordedOpinion> list(String wishType, long wishId) {
        return events.list(KIND, new EntityRef(wishType, wishId)).stream()
                .map(e -> new RecordedOpinion(
                        new OpinionSource(
                                OpinionSourceType.valueOf(e.content().sourceType()),
                                e.content().sourceFamilyMemberId()),
                        e.content().content(),
                        e.actorId(),
                        e.occurredAt()))
                .toList();
    }
}
