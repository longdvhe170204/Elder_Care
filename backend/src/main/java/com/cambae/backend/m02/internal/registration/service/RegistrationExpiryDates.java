package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionView;
import com.cambae.backend.m15.correction.api.CurrentValue;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Ngày hết hạn hiện hành của khai báo (FR-003, FR-006): cột {@code expiry_date} đặt một lần (trigger V018_01), sửa sai qua
 * đính chính {@code M02.RESIDENCE_REGISTRATION} nên ngày đúng nằm ở lần đính chính mới nhất có {@code expiryDate}. Lần
 * đính chính sau sửa trường khác không làm mất ngày đã sửa trước đó.
 */
@Component
class RegistrationExpiryDates {

    // Lười: CorrectionService cần RegistrationCorrectionProvider, provider cần service khai báo, service cần lớp này
    private final ObjectProvider<CorrectionApi> corrections;

    RegistrationExpiryDates(ObjectProvider<CorrectionApi> corrections) {
        this.corrections = corrections;
    }

    /** Khai báo → ngày hết hạn hiện hành; khai báo không có ngày hết hạn thì không có trong kết quả. */
    Map<Long, LocalDate> of(List<ResidenceRegistration> registrations) {
        Map<Long, LocalDate> result = new HashMap<>();
        registrations.forEach(r -> {
            if (r.expiryDate() != null) {
                result.put(r.id(), r.expiryDate());
            }
        });
        Map<Long, CurrentValue> current = corrections
                .getObject()
                .current(
                        RegistrationNames.ENTITY_TYPE,
                        registrations.stream().map(ResidenceRegistration::id).toList());
        current.forEach((id, value) -> {
            if (!value.corrected()) {
                return;
            }
            List<CorrectionView> chain = corrections
                    .getObject()
                    .chain(RegistrationNames.ENTITY_TYPE, id)
                    .corrections();
            for (int i = chain.size() - 1; i >= 0; i--) {
                if (chain.get(i).correctedContent() instanceof Map<?, ?> content && content.get("expiryDate") != null) {
                    result.put(id, LocalDate.parse(String.valueOf(content.get("expiryDate"))));
                    return;
                }
            }
        });
        return result;
    }
}
