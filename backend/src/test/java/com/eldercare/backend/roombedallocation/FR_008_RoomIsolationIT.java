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
@org.springframework.test.context.event.RecordApplicationEvents
class FR_008_RoomIsolationIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

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
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE status_history SET reason='Sửa' WHERE entity_type='M03.ROOM' AND attribute='ISOLATION' AND entity_id=?",
                        room))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "DELETE FROM status_history WHERE entity_type='M03.ROOM' AND attribute='ISOLATION' AND entity_id=?",
                        room))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Autowired
    com.eldercare.backend.roombedallocation.api.InfectionControlCommandApi infectionCommands;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus infection;

    @Autowired
    org.springframework.test.context.event.ApplicationEvents events;
}
