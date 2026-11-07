package com.eldercare.backend.resident.internal.status.service;

import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.OpenAbsence;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.TemporaryAbsenceOverdue;
import com.eldercare.backend.resident.api.spi.AbsenceApi;
import com.eldercare.backend.resident.internal.status.persistence.ResidentStatusStore;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cảnh báo Tạm vắng quá hạn trở lại (001 T051; BR-M01-01, FR-050): quá mốc dự kiến cộng CFG-M01-01 mà chưa Ghi nhận
 * trở về thì phát {@link TemporaryAbsenceOverdue} đúng một lần cho mỗi mốc (dấu {@code M01.RETURN_OVERDUE_ALERT}); gia hạn đổi
 * mốc nên cảnh báo lại. Lượt vắng đang mở lấy từ {@link AbsenceApi} của 004; chưa có 004 thì không cảnh báo.
 */
@Service
public class TemporaryAbsenceOverdueAlert {

    public static final String JOB = "TEMPORARY_ABSENCE_OVERDUE";

    private final ResidentStatusStore statuses;
    private final ObjectProvider<AbsenceApi> absences;
    private final ObjectProvider<BedAllocationApi> beds;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    TemporaryAbsenceOverdueAlert(
            ResidentStatusStore statuses,
            ObjectProvider<AbsenceApi> absences,
            ObjectProvider<BedAllocationApi> beds,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.statuses = statuses;
        this.absences = absences;
        this.beds = beds;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Số cảnh báo mới phát trong lần chạy này. */
    @Transactional
    public int run() {
        AbsenceApi api = absences.getIfAvailable();
        if (api == null) {
            return 0;
        }
        Duration grace = parameters.read(ParameterCode.CFG_M01_01).asDuration();
        Instant now = time.now();
        int alerted = 0;
        for (long residentId : statuses.idsByStatus(ResidentStatus.TEMPORARILY_ABSENT)) {
            OpenAbsence open = api.open(residentId).orElse(null);
            if (open == null || !now.isAfter(open.expectedReturnAt().plus(grace))) {
                continue;
            }
            if (statuses.recordOverdueAlert(open.absenceId(), open.expectedReturnAt(), residentId)) {
                events.publishEvent(new TemporaryAbsenceOverdue(
                        residentId, open.absenceId(), open.expectedReturnAt(), floorOf(residentId, now), now));
                alerted++;
            }
        }
        return alerted;
    }

    /** Tầng của người cao tuổi theo phân bổ giường (003); chưa có 003 hoặc chưa phân bổ thì báo không theo tầng. */
    private Long floorOf(long residentId, Instant at) {
        BedAllocationApi api = beds.getIfAvailable();
        return api == null
                ? null
                : api.openAt(residentId, at).map(BedAllocationView::floorAreaId).orElse(null);
    }
}
