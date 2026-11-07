package com.eldercare.backend.admission.internal.registration.persistence;

import com.eldercare.backend.admission.internal.persistence.M02RunStore;
import com.eldercare.backend.admission.internal.registration.domain.ReminderType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dấu nhắc của khai báo ở {@code reminder_marker} qua {@link M02RunStore} (data-model 018 mục 0.1: {@code nhac_khai_bao} →
 * {@code moc_da_nhac}; research Z-06; R-15): việc của job và listener giành dấu trước khi phát nhắc, cùng transaction với
 * lần phát, nên mỗi (loại, người, ngày) nhắc một lần kể cả khi chạy lại hay chạy song song. Chỉ ghi dấu kỹ thuật, không tạo
 * khai báo (Q-306).
 */
@Repository
public class RegistrationReminderStore {

    private static final String PREFIX = "REGISTRATION_";

    private final M02RunStore markers;

    RegistrationReminderStore(M02RunStore markers) {
        this.markers = markers;
    }

    /** {@code true} nếu đây là lần nhắc đầu tiên của (loại, người, ngày). */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(ReminderType type, long residentId, LocalDate day) {
        return markers.markReminded(PREFIX + type.name(), residentId, day);
    }

    /** Ngày nhắc gần nhất của (loại, người), rỗng nếu chưa nhắc (nhắc mỗi CFG-M02-09, FR-008). */
    public Optional<LocalDate> lastOn(ReminderType type, long residentId) {
        return markers.lastReminded(PREFIX + type.name(), residentId);
    }
}
