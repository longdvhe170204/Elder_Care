package com.eldercare.backend.resident.internal.status.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.OpenAbsence;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.TemporaryAbsenceOverdue;
import com.eldercare.backend.resident.api.spi.AbsenceApi;
import com.eldercare.backend.resident.internal.status.persistence.ResidentStatusStore;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cảnh báo Tạm vắng quá hạn trở lại (001 T051; BR-M01-01, FR-050): quá mốc dự kiến cộng CFG-M01-01 mà chưa Ghi nhận
 * trở về thì phát {@link TemporaryAbsenceOverdue} đúng một lần cho mỗi mốc ({@code return_overdue_alert}); gia hạn đổi
 * mốc nên cảnh báo lại. Lượt vắng đang mở lấy từ {@link AbsenceApi} của 004; chưa có 004 thì không cảnh báo.
 */
@Service
public class TemporaryAbsenceOverdueAlert {

    public static final String JOB = "TEMPORARY_ABSENCE_OVERDUE";

    private final ResidentStatusStore statuses;
    private final ObjectProvider<AbsenceApi> absences;
    private final ParameterApi parameters;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    TemporaryAbsenceOverdueAlert(
            ResidentStatusStore statuses,
            ObjectProvider<AbsenceApi> absences,
            ParameterApi parameters,
            JdbcTemplate jdbc,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.statuses = statuses;
        this.absences = absences;
        this.parameters = parameters;
        this.jdbc = jdbc;
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
            int inserted = jdbc.update(
                    "INSERT IGNORE INTO return_overdue_alert (absence_id, expected_return_at, resident_id, alerted_at)"
                            + " VALUES (?, ?, ?, ?)",
                    open.absenceId(),
                    Timestamp.from(open.expectedReturnAt()),
                    residentId,
                    Timestamp.from(now));
            if (inserted == 1) {
                events.publishEvent(new TemporaryAbsenceOverdue(residentId, open.absenceId(), open.expectedReturnAt()));
                alerted++;
            }
        }
        return alerted;
    }
}
