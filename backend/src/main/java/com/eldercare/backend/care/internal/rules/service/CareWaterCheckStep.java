package com.eldercare.backend.care.internal.rules.service;

import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** Chạy bù trong ngày; mỗi người một transaction, không tạo lịch hay kết quả. */
@Service
public class CareWaterCheckStep implements CareMinuteWork {
    private final ResidentApi residents;
    private final CareResultRules rules;

    public CareWaterCheckStep(ResidentApi residents, CareResultRules rules) {
        this.residents = residents;
        this.rules = rules;
    }

    @Override
    public int run(Instant now) {
        RuntimeException failure = null;
        int count = 0;
        // Lọc hiện diện tại mốc trong water(), kể cả người đã kết thúc lưu trú sau mốc.
        for (long resident : residents.byStatus(java.util.EnumSet.allOf(ResidentStatus.class))) {
            try {
                rules.water(resident, now.atZone(BusinessTime.ZONE).toLocalDate(), now);
                count++;
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        if (failure != null) throw failure;
        return count;
    }
}
