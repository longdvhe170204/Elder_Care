package com.eldercare.backend.care;

import static org.hamcrest.Matchers.hasItem;
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
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import({SwitchableUserConfig.class, FakeM15Config.class, FakeLocations.Config.class})
class T004_ActivityCommandsIT extends MySqlIntegrationTestBase {
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
    FakeLocations locations;

    @Autowired
    CatalogApi catalogs;

    @Test
    void FR_001_002b_catalogAndFacilitySessionCommandsRespectTheirPermissions() throws Exception {
        com.eldercare.backend.support.TableRows.each(List.of("CATALOG", "SESSION"), scenario -> {
            if ("CATALOG".equals(scenario)) FR_001_catalogUsesTheApprovedViewAndWritePermissions();
            else FR_002b_facilitySessionIsManagedByTheCreatorOrTheManagerOfItsOwnLocation();
        });
    }

    private void FR_001_catalogUsesTheApprovedViewAndWritePermissions() throws Exception {
        var users = new TestUsers(jdbc, time);
        var manager = users.staff(Role.NURSING_HOME_MANAGER);
        var caregiver = users.staff(Role.CAREGIVER);
        String name = "Thể dục " + TestUsers.nextId();
        String write = "{\"name\":\"" + name + "\",\"groupActivity\":true,\"offSite\":false,\"interestGroupIds\":[]}";
        user.set(caregiver.user());
        command("/api/v1/activity-types", write).andExpect(status().isForbidden());
        user.set(manager.user());
        command("/api/v1/activity-types", write).andExpect(status().isOk());
        var item = catalogs.list(ActivityKinds.ACTIVITY_TYPES, null, false).stream()
                .filter(row -> row.name().equals(name))
                .findFirst()
                .orElseThrow();
        user.set(caregiver.user());
        mvc.perform(get("/api/v1/activity-types"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[?(@.name == '" + name + "')].groupActivity").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.name == '" + name + "')].offSite").value(hasItem(false)));
        user.set(manager.user());
        command(
                        "/api/v1/activity-types",
                        "{\"id\":" + item.id() + ",\"name\":\"" + name + "\",\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/activity-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '" + name + "')].status").value(hasItem("INACTIVE")));
    }

    private void FR_002b_facilitySessionIsManagedByTheCreatorOrTheManagerOfItsOwnLocation() throws Exception {
        var users = new TestUsers(jdbc, time);
        long creatorFloor = TestUsers.nextId(), sessionFloor = TestUsers.nextId(), outsideFloor = TestUsers.nextId();
        long defaultRoom = TestUsers.nextId(), sessionRoom = TestUsers.nextId();
        locations
                .floor(creatorFloor, null)
                .floor(sessionFloor, null)
                .floor(outsideFloor, null)
                .room(defaultRoom, creatorFloor)
                .room(sessionRoom, sessionFloor);
        var creator = floorManager(users, creatorFloor);
        var locationManager = floorManager(users, sessionFloor);
        var outsider = floorManager(users, outsideFloor);
        user.set(creator.user());
        long type = catalogs.create(
                        ActivityKinds.ACTIVITY_TYPES,
                        new NewCatalogItem<>(
                                null,
                                "Loại " + defaultRoom,
                                null,
                                0,
                                new ActivityTypeAttributes(false, false, List.of())))
                .id();
        long activity = id(command(
                "/api/v1/activities",
                "{\"name\":\"Đọc sách\",\"typeId\":" + type
                        + ",\"scope\":\"FACILITY\",\"maxParticipants\":6,\"defaultLocation\":{\"roomId\":" + defaultRoom
                        + "}}"));
        var start =
                time.today().plusDays(1).atTime(9, 0).atZone(BusinessTime.ZONE).toInstant();
        long session = id(command(
                "/api/v1/activity-sessions",
                "{\"activityId\":" + activity + ",\"startsAt\":\"" + start + "\",\"endsAt\":\""
                        + start.plusSeconds(1800) + "\",\"locationId\":" + sessionRoom + "}"));
        String path = "/api/v1/activity-sessions/" + session;
        String reschedule = "{\"startsAt\":\"" + start.plusSeconds(3600) + "\",\"endsAt\":\"" + start.plusSeconds(5400)
                + "\",\"reason\":\"Điều chỉnh giờ sinh hoạt\"}";
        user.set(outsider.user());
        command(path + "/reschedule", reschedule).andExpect(status().isNotFound());
        user.set(locationManager.user());
        command(path + "/reschedule", reschedule).andExpect(status().isOk());
        user.set(creator.user());
        command(path + "/cancel", "{\"reason\":\"Đổi kế hoạch\"}").andExpect(status().isOk());
    }

    private TestUsers.TestUser floorManager(TestUsers users, long floor) {
        var manager = users.staff(Role.FLOOR_MANAGER);
        assignments.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(3600),
                        null));
        return manager;
    }

    private ResultActions command(String path, String body) throws Exception {
        return mvc.perform(post(path)
                .header("Idempotency-Key", "activity-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static long id(ResultActions response) throws Exception {
        var path = response.andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
    }
}
