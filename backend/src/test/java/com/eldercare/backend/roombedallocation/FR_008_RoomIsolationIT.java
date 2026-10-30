package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T053–T054: lệnh cách ly và ràng buộc nhật sử thật, không sửa trạng thái bằng endpoint danh mục. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class FR_008_RoomIsolationIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @Test
    void FR_008_doctorListsIsolationRoomsWithoutConfigurationPermission() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        mvc.perform(get("/api/v1/room-isolation/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + room + ")]").isNotEmpty());
        mvc.perform(get("/api/v1/rooms")).andExpect(status().isForbidden());
    }

    @Test
    void FR_008_managerIsolatesAndDoctorLiftsWithAppendOnlyHistory() throws Exception {
        mvc.perform(post("/api/v1/rooms/" + room + "/isolate")
                        .contentType("application/json")
                        .content("{\"reason\":\"Phòng cần cách ly\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isolated").value(true));
        user.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        mvc.perform(post("/api/v1/rooms/" + room + "/lift-isolation")
                        .contentType("application/json")
                        .content("{\"reason\":\"Đủ điều kiện gỡ\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/rooms/" + room + "/isolation-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("ISOLATE"))
                .andExpect(jsonPath("$[1].action").value("LIFT"));
        assertThat(jdbc.queryForObject("SELECT isolated FROM room WHERE id=?", Boolean.class, room))
                .isFalse();
        assertThatThrownBy(() -> jdbc.update("UPDATE room_isolation_history SET reason='Sửa' WHERE room_id=?", room))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM room_isolation_history WHERE room_id=?", room))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void FR_008_floorManagerCanReadButCannotIsolate() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER).user());
        mvc.perform(get("/api/v1/rooms/" + room + "/isolation-history")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/rooms/" + room + "/isolate")
                        .contentType("application/json")
                        .content("{\"reason\":\"Cách ly\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_008_duplicateCommandAndMissingReasonAreRejected() throws Exception {
        mvc.perform(post("/api/v1/rooms/" + room + "/isolate")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/rooms/" + room + "/isolate")
                        .contentType("application/json")
                        .content("{\"reason\":\"Cách ly\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/rooms/" + room + "/isolate")
                        .contentType("application/json")
                        .content("{\"reason\":\"Lặp\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM room_isolation_history WHERE room_id=?", Integer.class, room))
                .isEqualTo(1);
    }
}
