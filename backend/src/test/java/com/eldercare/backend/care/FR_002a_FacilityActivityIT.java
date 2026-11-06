package com.eldercare.backend.care;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.dto.AreaView;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.roombedallocation.api.dto.RoomView;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T004 (FR-002a, FR-002b; Q-175; chủ dự án chọn 07/11/2026): hoạt động toàn viện. Địa điểm mặc định chọn từ phòng
 * hoặc khu vực của 003 (tồn tại, đang dùng). Người tạo khai báo, sửa trong phạm vi tầng của chính mình; Trưởng tầng của
 * tầng nơi đặt phòng mặc định cũng quản lý; địa điểm là khu vực thì chỉ người tạo.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class})
class FR_002a_FacilityActivityIT extends MySqlIntegrationTestBase {
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

    @MockitoSpyBean
    LocationStructureApi structure;

    long creatorFloor, roomFloor, room, area, typeId;
    TestUsers.TestUser creator;

    @BeforeEach
    void fixture() {
        creatorFloor = TestUsers.nextId();
        roomFloor = TestUsers.nextId();
        room = TestUsers.nextId();
        area = TestUsers.nextId();
        doReturn(Optional.of(new RoomView(room, roomFloor, 1, "R1", "Phòng sinh hoạt", true)))
                .when(structure)
                .room(room);
        doReturn(Optional.of(new AreaView(area, "K1", "Khu A", true)))
                .when(structure)
                .area(area);
        doReturn(Optional.empty()).when(structure).room(org.mockito.ArgumentMatchers.longThat(id -> id != room));
        doReturn(true).when(structure).active(any(LocationRef.class));
        creator = floorManager(creatorFloor);
        typeId = catalogs.create(
                        ActivityKinds.ACTIVITY_TYPES,
                        new NewCatalogItem<>(
                                null,
                                "Văn nghệ " + TestUsers.nextId(),
                                null,
                                0,
                                new ActivityTypeAttributes(true, false, List.of())))
                .id();
        user.set(creator.user());
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

    String body(String location) {
        return "{\"name\":\"Văn nghệ toàn viện\",\"typeId\":" + typeId
                + ",\"scope\":\"FACILITY\",\"maxParticipants\":40" + ",\"defaultLocation\":" + location + "}";
    }

    long declare(String location) throws Exception {
        var header = mvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "facility-" + TestUsers.nextId())
                        .content(body(location)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return Long.parseLong(header.substring(header.lastIndexOf('/') + 1));
    }

    org.springframework.test.web.servlet.ResultActions rename(long id, int version, String location) throws Exception {
        return mvc.perform(put("/api/v1/activities/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "facility-" + TestUsers.nextId())
                .header("If-Match", "\"" + version + "\"")
                .content(body(location).replace("Văn nghệ toàn viện", "Văn nghệ cuối tuần")));
    }

    @Test
    void FR_002b_creatorDeclaresAndEditsAFacilityActivityLocatedOnAnotherFloor() throws Exception {
        long id = declare("{\"roomId\":" + room + "}");
        rename(id, 0, "{\"roomId\":" + room + "}").andExpect(status().isOk());
    }

    @Test
    void FR_002b_floorManagerOfTheDefaultRoomFloorManagesItOthersDoNot() throws Exception {
        long id = declare("{\"roomId\":" + room + "}");
        user.set(floorManager(roomFloor).user());
        rename(id, 0, "{\"roomId\":" + room + "}").andExpect(status().isOk());
        user.set(floorManager(TestUsers.nextId()).user());
        rename(id, 1, "{\"roomId\":" + room + "}").andExpect(status().isNotFound());
    }

    @Test
    void FR_002b_areaLocatedFacilityActivityIsManagedOnlyByItsCreator() throws Exception {
        long id = declare("{\"areaId\":" + area + "}");
        user.set(floorManager(roomFloor).user());
        rename(id, 0, "{\"areaId\":" + area + "}").andExpect(status().isNotFound());
        user.set(creator.user());
        rename(id, 0, "{\"areaId\":" + area + "}").andExpect(status().isOk());
    }

    @Test
    void FR_002a_defaultLocationMustBeAnExistingActiveRoomOrArea() throws Exception {
        for (String invalid :
                List.of("{\"roomId\":" + TestUsers.nextId() + "}", "{\"roomId\":" + room + ",\"areaId\":" + area + "}"))
            mvc.perform(post("/api/v1/activities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "facility-" + TestUsers.nextId())
                            .content(body(invalid)))
                    .andExpect(status().isUnprocessableContent());
        doReturn(false).when(structure).active(new LocationRef("AREA", area));
        mvc.perform(post("/api/v1/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "facility-" + TestUsers.nextId())
                        .content(body("{\"areaId\":" + area + "}")))
                .andExpect(status().isUnprocessableContent());
    }
}
