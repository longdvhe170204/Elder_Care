package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * 014 T007 (BR-M04-21; FR-004, FR-006 → FR-013; US1 kịch bản 1 → 4): Trưởng tầng tạo buổi từ mẫu bằng lệnh, buổi lẻ,
 * dời, hủy; đổi mẫu, ngừng, người thường xuyên tác động lên buổi chưa diễn ra.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import({SwitchableUserConfig.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class BR_38_TaoBuoiTuMauIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    FakeResidents residents;

    @Autowired
    JsonMapper json;

    @Autowired
    ApplicationEvents events;

    long floor;
    long activity;
    LocalDate today;
    List<Long> regulars;

    @BeforeEach
    void fixture() throws Exception {
        floor = TestUsers.nextId();
        today = time.today();
        var manager = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        assignments.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(86400),
                        null));
        user.set(manager.user());
        long typeId = catalogs.create(
                        ActivityKinds.ACTIVITY_TYPES,
                        new NewCatalogItem<>(
                                null,
                                "Vận động " + TestUsers.nextId(),
                                null,
                                0,
                                new ActivityTypeAttributes(true, false, List.of())))
                .id();
        var location = mvc.perform(write(
                        post("/api/v1/activities"),
                        "{\"name\":\"Thể dục buổi sáng\",\"typeId\":" + typeId + ",\"scope\":\"FLOOR\",\"floorAreaId\":"
                                + floor + ",\"maxParticipants\":20,\"durationMinutes\":45}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        activity = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        template(today, "08:00", "08:45").andExpect(status().isOk());
        regulars = List.of(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId());
        for (long r : regulars) {
            residents.resident(r, "[Họ tên NCT]", Gender.FEMALE, ResidentStatus.IN_RESIDENCE);
            command(activity, "add-regular-participant", "{\"residentId\":" + r + "}")
                    .andExpect(status().isOk());
        }
    }

    MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "session-" + TestUsers.nextId())
                .content(body);
    }

    ResultActions command(long id, String command, String body) throws Exception {
        return mvc.perform(write(post("/api/v1/activities/" + id + "/" + command), body));
    }

    ResultActions sessionCommand(long id, String command, String body) throws Exception {
        return mvc.perform(write(post("/api/v1/activity-sessions/" + id + "/" + command), body));
    }

    ResultActions template(LocalDate from, String start, String end) throws Exception {
        return command(
                activity,
                "change-template",
                "{\"reason\":\"Lịch mới\",\"effectiveDate\":\"" + from
                        + "\",\"template\":{\"daysOfWeek\":[1,2,3,4,5,6,7]," + "\"startTime\":\"" + start
                        + "\",\"endTime\":\"" + end + "\",\"startDate\":\"" + today + "\"}}");
    }

    ResultActions createFromTemplate(LocalDate from, LocalDate to) throws Exception {
        return command(
                activity, "create-sessions-from-template", "{\"fromDate\":\"" + from + "\",\"toDate\":\"" + to + "\"}");
    }

    List<java.util.Map<String, Object>> sessions() {
        return jdbc.queryForList(
                "SELECT id, starts_at, status, cancelled_by FROM activity_session WHERE activity_id=? ORDER BY starts_at",
                activity);
    }

    int registrations(long sessionId, String source, String status) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM activity_registration WHERE session_id=? AND source=? AND status=?",
                Integer.class,
                sessionId,
                source,
                status);
    }

    long firstSession() {
        return ((Number) sessions().getFirst().get("id")).longValue();
    }

    @Test
    void BR_38_createFromTemplateMakesMissingSessionsWithRegularsRegistered() throws Exception {
        // Từ ngày mai: buổi hôm nay có thể đã bắt đầu, khi đó người thường xuyên không được đăng ký sẵn (FR-015 (e)).
        createFromTemplate(today.plusDays(1), today.plusDays(7)).andExpect(status().isOk());
        var rows = sessions();
        assertThat(rows).hasSize(7);
        assertThat(rows).allSatisfy(r -> assertThat(r.get("status")).isEqualTo("SCHEDULED"));
        for (var r : rows)
            assertThat(registrations(((Number) r.get("id")).longValue(), "REGULAR", "REGISTERED"))
                    .isEqualTo(3);
        var list = json.readTree(mvc.perform(get("/api/v1/activity-sessions")
                        .param("from", today.plusDays(1).toString())
                        .param("to", today.plusDays(7).toString())
                        .param("floorAreaId", Long.toString(floor)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(list).hasSize(7);
        assertThat(list.get(0).get("registrationCount").asInt()).isEqualTo(3);
        assertThat(list.get(0).get("isTrip").asBoolean()).isFalse();
    }
}
