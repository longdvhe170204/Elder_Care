package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.api.IncidentCommandApi;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản thật lệnh sự cố cho module khác (module-interfaces 007 mục 1.6): cùng kiểm quyền, trạng thái và tác động như
 * REST; bắt buộc chạy trong transaction của lệnh gọi. {@code acknowledge} là "Tiếp nhận xử lý" của thao tác "Xác nhận
 * và tiếp nhận" (009 FR-026a, Q-102).
 */
@Service
public class DefaultIncidentCommandApi implements IncidentCommandApi {
    private final IncidentRecordService records;
    private final IncidentProgressionService progressions;
    private final IncidentService incidents;

    public DefaultIncidentCommandApi(
            IncidentRecordService records, IncidentProgressionService progressions, IncidentService incidents) {
        this.records = records;
        this.progressions = progressions;
        this.incidents = incidents;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long record(RecordIncidentCommand command, CurrentUser user) {
        return records.record(command, user).incidentId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addProgression(long incidentId, String progressionTypeCode, String content, CurrentUser user) {
        progressions.add(incidentId, progressions.typeIdByCode(progressionTypeCode), content, null, user);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void acknowledge(long incidentId, CurrentUser user) {
        incidents.receive(incidentId, user);
    }
}
