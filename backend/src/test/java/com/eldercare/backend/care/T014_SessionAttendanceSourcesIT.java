package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.care.api.events.ActivityAttendanceReminder;
import com.eldercare.backend.care.api.spi.TaskSourcePort;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T014: nhắc Hoàn tất điểm danh một lần sau kết thúc + CFG-M04-06 (FR-030); nguồn công việc {@code ACTIVITY_SESSION}
 * cho 005 (module-interfaces 1.1 {@code NguonCongViecPort}); ghi ngoại tuyến {@code M04.SESSION_ATTENDANCE} (Q-01).
 */
@Transactional
@RecordApplicationEvents
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class T014_SessionAttendanceSourcesIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeResidents residents;

    @Autowired
    List<CareMinuteWork> minuteWork;

    @Autowired
    List<TaskSourcePort> sources;

    @Autowired
    List<OfflineHandler<?>> offline;

    @Autowired
    ApplicationEvents events;

    SessionAttendanceFixture data;

    @BeforeEach
    void fixture() {
        data = new SessionAttendanceFixture(jdbc, time, assignments, user, residents);
    }

    @Test
    void FR_030_unfinishedSessionIsRemindedOnceAfterEndPlusGrace() {
        long overdue = data.session(data.activity(false), Duration.ofHours(-4));
        long recent = data.session(data.activity(false), Duration.ofMinutes(-30));
        for (var w : minuteWork) w.run(time.now());
        assertThat(events.stream(ActivityAttendanceReminder.class).map(ActivityAttendanceReminder::sessionId))
                .contains(overdue)
                .doesNotContain(recent);
        long first = events.stream(ActivityAttendanceReminder.class)
                .filter(e -> e.sessionId() == overdue)
                .count();
        for (var w : minuteWork) w.run(time.now());
        assertThat(events.stream(ActivityAttendanceReminder.class)
                        .filter(e -> e.sessionId() == overdue)
                        .count())
                .isEqualTo(first)
                .isEqualTo(1);
    }

    @Test
    void FR_017_activeRegistrationIsAValidActivityTaskSource() {
        long session = data.session(data.activity(false), Duration.ofHours(2));
        long resident = data.resident();
        long registration = data.registration(session, resident);
        var source = sources.stream()
                .filter(s -> s.managedTypes().contains(TaskSourcePort.SourceType.ACTIVITY_SESSION))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Thiếu nguồn công việc ACTIVITY_SESSION của 014"));
        var at = time.now().plus(Duration.ofHours(2)).plus(Duration.ofMinutes(10));
        var ok = source.validate(TaskSourcePort.SourceType.ACTIVITY_SESSION, registration, resident, at);
        assertThat(ok.valid()).isTrue();
        assertThat(ok.residentMatches()).isTrue();
        assertThat(ok.timeMatches()).isTrue();
        assertThat(source.validate(TaskSourcePort.SourceType.ACTIVITY_SESSION, registration, resident + 1, at)
                        .residentMatches())
                .isFalse();
        assertThat(source.validate(
                                TaskSourcePort.SourceType.ACTIVITY_SESSION,
                                registration,
                                resident,
                                at.plus(Duration.ofHours(5)))
                        .timeMatches())
                .isFalse();
        assertThat(source.sources(resident, time.now(), time.now().plus(Duration.ofHours(4))))
                .extracting(TaskSourcePort.SourceView::id)
                .containsExactly(registration);
    }

    @Test
    void Q_01_sessionAttendanceHasAnOfflineHandler() {
        assertThat(offline).extracting(OfflineHandler::recordType).contains("M04.SESSION_ATTENDANCE");
    }
}
