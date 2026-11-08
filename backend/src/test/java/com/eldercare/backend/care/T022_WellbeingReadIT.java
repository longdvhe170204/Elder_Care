package com.eldercare.backend.care;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.internal.session.domain.SessionKinds;
import com.eldercare.backend.care.internal.wellbeing.domain.Preference;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class T022_WellbeingReadIT extends CareResultTestBase {
    @Autowired
    SimpleRecordApi records;

    @Autowired
    RecordEventApi eventsApi;

    @Test
    void FR_051_053_readsPreferencesAndWellbeingFromTheirRecordedSources() throws Exception {
        com.eldercare.backend.support.TableRows.each(java.util.List.of("PREFERENCES", "PROFILE"), scenario -> {
            fixture();
            if ("PREFERENCES".equals(scenario))
                FR_051_readsCurrentAndHistoricalPreferencesWithTheirSourcesAndRecorders();
            else FR_053_readsWeeklyParticipationAndSessionCommunicationAndLeavesUnknownAlertsUnavailable();
        });
    }

    private void FR_051_readsCurrentAndHistoricalPreferencesWithTheirSourcesAndRecorders() throws Exception {
        var source = new OpinionSource(OpinionSourceType.RESIDENT, null);
        long group = TestUsers.nextId();
        long current = preference(group, source, Preference.CURRENT);
        long history = preference(group, source, Preference.REPLACED);
        mvc.perform(get("/api/v1/residents/" + resident + "/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current[0].id").value(current))
                .andExpect(jsonPath("$.current[0].source.type").value("RESIDENT"))
                .andExpect(jsonPath("$.current[0].recordedBy").value(baseManager.accountId()))
                .andExpect(jsonPath("$.history[0].id").value(history))
                .andExpect(jsonPath("$.history[0].endedBy").value(baseManager.accountId()));
    }

    private void FR_053_readsWeeklyParticipationAndSessionCommunicationAndLeavesUnknownAlertsUnavailable()
            throws Exception {
        long activity = TestUsers.nextId(), session = TestUsers.nextId(), registration = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,status,content,created_by,created_at)"
                        + " VALUES(?,'M04.ACTIVITY','M04','ACTIVE',?, ?,NOW(6))",
                activity,
                "{\"name\":\"Đọc sách\",\"typeId\":1,\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                        + ",\"groupActivity\":true,\"maxParticipants\":3,\"createdByStaffId\":" + performer + "}",
                baseManager.accountId());
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,floor_area_id,status,created_at,created_by)"
                        + " VALUES(?,?,?,?,'SINGLE',?,'ATTENDANCE_TAKEN',NOW(6),?)",
                session,
                activity,
                local(time.now().minusSeconds(1800)),
                local(time.now()),
                floor,
                baseManager.accountId());
        jdbc.update(
                "INSERT INTO activity_registration(id,session_id,resident_id,source,registered_at,status)"
                        + " VALUES(?,?,?,'MANUAL',NOW(6),'ATTENDED')",
                registration,
                session,
                resident);
        eventsApi.add(
                SessionKinds.ATTENDANCE,
                new EntityRef("M01.RESIDENT", resident),
                new NewRecordEvent<>(
                        new SessionKinds.AttendanceResult(
                                registration,
                                session,
                                resident,
                                "PRESENT",
                                null,
                                "LEFT_EARLY",
                                "ACTIVE",
                                null,
                                null,
                                false,
                                null),
                        null,
                        baseManager.accountId(),
                        false,
                        time.now(),
                        null,
                        time.now(),
                        floor,
                        SessionKinds.key(registration)));
        mvc.perform(get("/api/v1/residents/" + resident + "/wellbeing-profile")
                        .param("from", time.today().toString())
                        .param("to", time.today().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyParticipation[0].presentCount").value(1))
                .andExpect(jsonPath("$.sessions[0].sessionId").value(session))
                .andExpect(jsonPath("$.sessions[0].participationLevel").value("LEFT_EARLY"))
                .andExpect(jsonPath("$.sessions[0].communicationLevel").value("ACTIVE"))
                .andExpect(jsonPath("$.alerts").isEmpty());
    }

    private long preference(long group, OpinionSource source, String status) {
        boolean ended = !Preference.CURRENT.equals(status);
        return records.create(
                        Preference.KIND,
                        new NewSimpleRecord<>(
                                resident,
                                null,
                                status,
                                new Preference(
                                        group,
                                        "Thích đọc sách",
                                        Preference.LIKE,
                                        source,
                                        time.now(),
                                        ended ? time.now() : null,
                                        ended ? baseManager.accountId() : null,
                                        null),
                                null,
                                null,
                                null,
                                baseManager.accountId(),
                                ended))
                .id();
    }
}
