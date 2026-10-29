package com.eldercare.backend.resident.internal.assessment.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.CareLevelAtDate;
import com.eldercare.backend.resident.internal.assessment.persistence.CareLevelHistoryStore;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tra mức chăm sóc cho endpoint (FR-052, FR-053): cùng quyền xem hồ sơ người cao tuổi. Module khác dùng {@code
 * CareLevelApi}, không qua lớp này.
 */
@Service
public class CareLevelQueryService {

    private final CareLevelHistoryStore history;
    private final BusinessTime time;

    CareLevelQueryService(CareLevelHistoryStore history, BusinessTime time) {
        this.history = history;
        this.time = time;
    }

    /** Mức có hiệu lực tại cuối ngày {@code date} (mặc định hôm nay), kèm căn cứ; rỗng khi chưa có mức. */
    @RequiresPermission(function = Function.RESIDENT_PROFILE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Optional<CareLevelAtDate> at(Target target, LocalDate date) {
        LocalDate day = date == null ? time.today() : date;
        return history.at(target.residentId(), time.startOfDay(day.plusDays(1)).minusNanos(1_000));
    }

    @RequiresPermission(function = Function.RESIDENT_PROFILE, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CareLevelAtDate> history(Target target) {
        return history.history(target.residentId());
    }
}
