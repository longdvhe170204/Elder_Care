package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.m02.internal.ending.domain.Handover;
import com.cambae.backend.m02.internal.ending.domain.RecipientType;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CurrentValue;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Các lần bàn giao còn hiệu lực của hồ sơ kết thúc (FR-063 (e); nhóm 3): bỏ lần đã Hủy ghi nhận, áp bản sửa mới nhất của thời
 * điểm, nhân viên bàn giao, người thân, cơ sở tiếp nhận (research T-13). Điều kiện (e) Đạt khi còn ít nhất một lần.
 */
@Component
public class EndingHandovers {

    static final String RECORD_TYPE = "M02.RESIDENT_HANDOVER";

    private final ResidencyEndingStore store;
    private final ObjectProvider<CorrectionApi> corrections;

    EndingHandovers(ResidencyEndingStore store, ObjectProvider<CorrectionApi> corrections) {
        this.store = store;
        this.corrections = corrections;
    }

    /** Lần bàn giao với giá trị hiện hành. */
    public record Effective(
            long id,
            RecipientType recipientType,
            Long familyMemberId,
            String facilityName,
            Instant handoverAt,
            long handoverStaffId) {}

    public List<Effective> effective(long endingId) {
        List<Handover> raw = store.handovers(endingId);
        if (raw.isEmpty()) {
            return List.of();
        }
        Map<Long, CurrentValue> current = corrections
                .getObject()
                .current(RECORD_TYPE, raw.stream().map(Handover::id).toList());
        List<Effective> result = new ArrayList<>();
        for (Handover handover : raw) {
            CurrentValue value = current.get(handover.id());
            if (value != null && value.voided()) {
                continue;
            }
            Effective effective = new Effective(
                    handover.id(),
                    handover.recipientType(),
                    handover.familyMemberId(),
                    handover.facilityName(),
                    handover.handoverAt(),
                    handover.handoverStaffId());
            if (value != null && value.corrected() && value.content() instanceof Map<?, ?> content) {
                effective = amended(effective, content);
            }
            result.add(effective);
        }
        return result;
    }

    /** Lần bàn giao hiện hành gần nhất, nếu có. */
    public Optional<Effective> latest(long endingId) {
        List<Effective> all = effective(endingId);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.getLast());
    }

    private static Effective amended(Effective base, Map<?, ?> content) {
        Instant at = content.get("handoverAt") == null ? base.handoverAt() : parse(content.get("handoverAt"), base);
        long staff = content.get("handoverStaffId") == null
                ? base.handoverStaffId()
                : Long.parseLong(content.get("handoverStaffId").toString());
        Long family = base.familyMemberId();
        String facility = base.facilityName();
        if (base.recipientType() == RecipientType.FAMILY_MEMBER && content.get("familyMemberId") != null) {
            family = Long.parseLong(content.get("familyMemberId").toString());
        }
        if (base.recipientType() == RecipientType.RECEIVING_FACILITY && content.get("facilityName") != null) {
            facility = content.get("facilityName").toString();
        }
        return new Effective(base.id(), base.recipientType(), family, facility, at, staff);
    }

    private static Instant parse(Object value, Effective base) {
        try {
            return Instant.parse(value.toString());
        } catch (DateTimeParseException e) {
            return base.handoverAt();
        }
    }
}
