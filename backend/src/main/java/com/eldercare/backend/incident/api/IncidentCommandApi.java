package com.eldercare.backend.incident.api;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;

/** Lệnh của actor từ module khác; bản thật bắt buộc tham gia transaction của lệnh gọi (mục 1.6). */
public interface IncidentCommandApi {
    long record(RecordIncidentCommand command, CurrentUser user);

    void addProgression(long incidentId, String progressionTypeCode, String content, CurrentUser user);

    void acknowledge(long incidentId, CurrentUser user);
}
