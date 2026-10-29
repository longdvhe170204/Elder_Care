package com.eldercare.backend.resident;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.CourtDecisionRef;
import com.eldercare.backend.resident.api.dto.RecordEndOfLifeWish;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService;
import com.eldercare.backend.resident.internal.wish.service.WishReminderService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Nền chung cho test phiếu nguyện vọng của 001 US7 (T067, T077): bản giả tệp S3 và quan hệ người thân lấy từ
 * {@link ConsentTestBase}. Bác sĩ nhận định là {@code doctor} (AC-03); Điều dưỡng ghi phiếu là {@code nurse} phải được
 * phân công ({@link #assignNurse}); Hành chính {@code office} không có quyền với nội dung phiếu (FR-023e).
 */
public abstract class WishTestBase extends ConsentTestBase {

    protected static final String ENTITY = "M01.END_OF_LIFE_WISH";

    @Autowired
    protected EndOfLifeWishService wishes;

    @Autowired
    protected TreatmentWishService treatments;

    @Autowired
    protected WishReminderService wishReminders;

    protected WishCommandBuilder command(long residentId) {
        return new WishCommandBuilder(residentId, doctor.accountId(), evidence());
    }

    /** Điều dưỡng được phân công ghi phiếu. */
    protected EndOfLifeWish recordAs(WishRecordSource source, RecordEndOfLifeWish command) {
        assignNurse(nurse, command.residentId());
        user.set(nurse.user());
        return wishes.record(target(command.residentId()), command, source);
    }

    protected EndOfLifeWish record(RecordEndOfLifeWish command) {
        return recordAs(WishRecordSource.ADMISSION, command);
    }

    protected static BusinessException failure(Runnable command) {
        try {
            command.run();
        } catch (BusinessException e) {
            return e;
        }
        throw new AssertionError("Lệnh phải bị từ chối");
    }

    protected CourtDecisionRef courtDecision() {
        return new CourtDecisionRef("QĐ-15/2026/TA", LocalDate.of(2026, 3, 2), "TAND quận 1", evidence());
    }

    protected int activeWishCount(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM end_of_life_wish WHERE resident_id = ? AND status = 'ACTIVE'",
                Integer.class,
                residentId);
    }

    protected List<String> wishAuditActions(long wishId) {
        return jdbc.queryForList(
                "SELECT action FROM audit_log WHERE entity_type = '" + ENTITY + "' AND entity_id = ? ORDER BY id",
                String.class,
                wishId);
    }
}
