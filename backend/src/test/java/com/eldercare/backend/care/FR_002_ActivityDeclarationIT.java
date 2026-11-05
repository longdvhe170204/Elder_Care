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
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** 014 T004 (FR-002, FR-002b, FR-004, FR-009, FR-010; research L-04): hoạt động do Trưởng tầng khai báo. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class})
class FR_002_ActivityDeclarationIT extends MySqlIntegrationTestBase {
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
    JsonMapper json;

    long floor;
    long typeId;
    TestUsers.TestUser manager;

    @BeforeEach
    void fixture() {
        floor = TestUsers.nextId();
        manager = floorManager(floor);
        typeId = catalogs.create(
                        ActivityKinds.ACTIVITY_TYPES,
                        new NewCatalogItem<>(
                                null,
                                "Vận động " + TestUsers.nextId(),
                                null,
                                0,
                                new ActivityTypeAttributes(true, false, List.of())))
                .id();
        user.set(manager.user());
    }

    TestUsers.TestUser floorManager(long floorAreaId) {
        var staff = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        assignments.assign(
                staff.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floorAreaId,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(86400),
                        null));
        return staff;
    }

    String body(String extra) {
        return "{\"name\":\"Thể dục buổi sáng\",\"typeId\":" + typeId + ",\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                + ",\"maxParticipants\":20,\"durationMinutes\":45" + extra + "}";
    }

    MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "activity-" + TestUsers.nextId())
                .content(body);
    }

    long declare() throws Exception {
        var location = mvc.perform(write(post("/api/v1/activities"), body("")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    ResultActions command(long id, String command, String body) throws Exception {
        return mvc.perform(write(post("/api/v1/activities/" + id + "/" + command), body));
    }

    @Test
    void FR_002_floorManagerDeclaresActivityOfOwnFloorWithTypeDefaults() throws Exception {
        long id = declare();
        var list = json.readTree(mvc.perform(get("/api/v1/activities").param("floorAreaId", Long.toString(floor)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("id").asLong()).isEqualTo(id);
        assertThat(list.get(0).get("groupActivity").asBoolean()).isTrue();
        assertThat(list.get(0).get("status").asString()).isEqualTo("ACTIVE");
    }

    @Test
    /** Ngoài phạm vi phân công: lớp phạm vi 002 trả 404 (không lộ đối tượng). */
    void FR_002_floorManagerCannotDeclareForAnotherFloor() throws Exception {
        mvc.perform(write(
                        post("/api/v1/activities"),
                        body("").replace("\"floorAreaId\":" + floor, "\"floorAreaId\":" + TestUsers.nextId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_002_onlyFloorManagerDeclaresActivities() throws Exception {
        var caregiver = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        assignments.assign(
                caregiver.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(3600)));
        user.set(caregiver.user());
        mvc.perform(write(post("/api/v1/activities"), body(""))).andExpect(status().isForbidden());
    }

    @Test
    void FR_002_invalidDeclarationsAreRejected() throws Exception {
        mvc.perform(write(post("/api/v1/activities"), body(",\"charged\":true")))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(write(post("/api/v1/activities"), body(",\"groupActivity\":false")))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(write(post("/api/v1/activities"), body("").replace(",\"floorAreaId\":" + floor, "")))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(write(
                        post("/api/v1/activities"),
                        body(",\"groupActivity\":false,\"groupActivityChangeReason\":\"Hoạt động cá nhân\"")))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_002b_onlyTheFloorManagerOfTheFloorEditsTheActivity() throws Exception {
        long id = declare();
        mvc.perform(write(put("/api/v1/activities/" + id), body("").replace("Thể dục buổi sáng", "Thể dục nhẹ"))
                        .header("If-Match", "\"0\""))
                .andExpect(status().isOk());
        user.set(floorManager(TestUsers.nextId()).user());
        mvc.perform(write(put("/api/v1/activities/" + id), body("")).header("If-Match", "\"1\""))
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_004_deactivatedActivityIsInactive() throws Exception {
        long id = declare();
        command(id, "deactivate", "{\"reason\":\"Ngừng tổ chức\",\"effectiveDate\":\"" + time.today() + "\"}")
                .andExpect(status().isOk());
        var list = json.readTree(mvc.perform(get("/api/v1/activities").param("floorAreaId", Long.toString(floor)))
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(list.get(0).get("status").asString()).isEqualTo("INACTIVE");
    }

    @Test
    void FR_009_templateChangeNeedsReasonAndDateFromTodayAndKeepsVersions() throws Exception {
        long id = declare();
        String template =
                "\"template\":{\"daysOfWeek\":[1,2,3,4,5],\"startTime\":\"08:00\",\"endTime\":\"08:45\",\"startDate\":\""
                        + time.today() + "\"}";
        command(
                        id,
                        "change-template",
                        "{" + template + ",\"effectiveDate\":\"" + time.today().minusDays(1)
                                + "\",\"reason\":\"Lịch mới\"}")
                .andExpect(status().isUnprocessableContent());
        command(id, "change-template", "{" + template + ",\"effectiveDate\":\"" + time.today() + "\"}")
                .andExpect(status().isUnprocessableContent());
        command(
                        id,
                        "change-template",
                        "{" + template + ",\"effectiveDate\":\"" + time.today() + "\",\"reason\":\"Lịch mới\"}")
                .andExpect(status().isOk());
        command(
                        id,
                        "change-template",
                        "{" + template.replace("08:00", "09:00").replace("08:45", "09:45") + ",\"effectiveDate\":\""
                                + time.today().plusDays(7) + "\",\"reason\":\"Đổi giờ\"}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind='M04.TEMPLATE_VERSION' AND entity_id=?",
                        Long.class,
                        id))
                .isEqualTo(2L);
    }

    @Test
    void FR_010_regularParticipantIsUniqueWhileActiveAndCanBeRemoved() throws Exception {
        long id = declare();
        long resident = TestUsers.nextId();
        command(id, "add-regular-participant", "{\"residentId\":" + resident + "}")
                .andExpect(status().isOk());
        command(id, "add-regular-participant", "{\"residentId\":" + resident + "}")
                .andExpect(status().isConflict());
        command(id, "remove-regular-participant", "{\"residentId\":" + resident + ",\"reason\":\"Không tham gia nữa\"}")
                .andExpect(status().isOk());
        command(id, "add-regular-participant", "{\"residentId\":" + resident + "}")
                .andExpect(status().isOk());
    }

    @Test
    void FR_006_createSessionsFromTemplateWaitsForUserStory1() throws Exception {
        long id = declare();
        command(
                        id,
                        "create-sessions-from-template",
                        "{\"fromDate\":\"" + time.today() + "\",\"toDate\":\""
                                + time.today().plusDays(6) + "\"}")
                .andExpect(status().isServiceUnavailable());
    }
}
