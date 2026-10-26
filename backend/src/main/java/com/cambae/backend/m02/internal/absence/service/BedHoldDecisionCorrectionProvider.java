package com.cambae.backend.m02.internal.absence.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m02.internal.absence.domain.BedHoldDecision;
import com.cambae.backend.m02.internal.absence.persistence.AbsenceStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.correction.api.CorrectionView;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính quyết định giữ giường (FR-061; data-model mục 13): chỉ hệ số và ngày xem xét lại. Quyết định thuộc Quản lý
 * viện nên chỉ Quản lý viện ghi đính chính. Sau khi lưu, các ngày vắng từ ngày quyết định áp dụng được tính lại.
 */
@Component
class BedHoldDecisionCorrectionProvider implements CorrectableRecordProvider {

    private static final Set<String> FIELDS = Set.of("coefficient", "reviewDate");

    private final AbsenceStore store;
    private final AbsenceService service;

    BedHoldDecisionCorrectionProvider(AbsenceStore store, AbsenceService service) {
        this.store = store;
        this.service = service;
    }

    @Override
    public String recordType() {
        return AbsenceService.DECISION_RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return store.decisionById(id).isPresent();
    }

    @Override
    public long originalRecorder(long id) {
        return find(id).decidedBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.NURSING_HOME_MANAGER);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException("Nội dung đúng cần coefficient hoặc reviewDate của quyết định");
        }
        if (map.get("coefficient") != null) {
            try {
                BigDecimal value = new BigDecimal(map.get("coefficient").toString());
                if (value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) {
                    throw new InvalidValueException("coefficient phải từ 0 đến 100");
                }
            } catch (NumberFormatException e) {
                throw new InvalidValueException("coefficient phải là số");
            }
        }
        if (map.get("reviewDate") != null) {
            try {
                LocalDate.parse(map.get("reviewDate").toString());
            } catch (DateTimeParseException e) {
                throw new InvalidValueException("reviewDate phải theo ISO-8601");
            }
        }
    }

    @Override
    public Object originalContent(long id) {
        BedHoldDecision decision = find(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("coefficient", decision.coefficient().toPlainString());
        content.put(
                "reviewDate",
                decision.reviewDate() == null ? null : decision.reviewDate().toString());
        return content;
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        service.recalculateAfterDecisionCorrection(correction.originalRecordId());
    }

    private BedHoldDecision find(long id) {
        return store.decisionById(id).orElseThrow(NotFoundException::new);
    }
}
