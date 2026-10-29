package com.eldercare.backend.roombedallocation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** T017: endpoint hợp đồng đã được duyệt, quyền thật, ETag và cảnh báo 201. */
@AutoConfigureMockMvc
@WithMockUser
class FR_001_StructureHttpIT extends StructureTestSupport {
    @Test
    void FR_044_commandWithoutReasonUsesTheSharedReasonError() throws Exception {
        mvc.perform(post("/api/v1/rooms/" + room + "/deactivate")
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
    }

    @Test
    void FR_043_optionalAreaReasonIsPreservedInAudit() throws Exception {
        mvc.perform(post("/api/v1/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"WHY" + area + "\",\"name\":\"Khu có lý do\",\"reason\":\"Mở thêm khu\"}"))
                .andExpect(status().isCreated());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                        "SELECT reason FROM audit_log WHERE entity_type='M03.AREA' AND JSON_UNQUOTE(JSON_EXTRACT(data_after,'$.code'))=? ORDER BY id DESC LIMIT 1",
                        String.class,
                        "WHY" + area))
                .isEqualTo("Mở thêm khu");
    }

    @Test
    void FR_003_bedCodeCanBeEditedWithoutChangingItsState() throws Exception {
        mvc.perform(put("/api/v1/beds/" + bed)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":" + room + ",\"code\":\"G-EDIT\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/beds/" + bed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("G-EDIT"))
                .andExpect(jsonPath("$.state").value("EMPTY"));
    }

    @Autowired
    MockMvc mvc;

    @Test
    void FR_001_areaCommandsUseEnglishFieldsAndVersionEtag() throws Exception {
        mvc.perform(post("/api/v1/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "area-" + area)
                        .content("{\"code\":\"HTTP" + area + "\",\"name\":\"Khu HTTP\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.code").value("HTTP" + area));
        mvc.perform(get("/api/v1/areas/" + area))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""));
        mvc.perform(put("/api/v1/areas/" + area)
                        .header("If-Match", "\"9\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"A" + area + "\",\"name\":\"Tên mới\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.currentValue.version").value(0));
    }

    @Test
    void FR_002_floorManagerCannotWriteButCanReadAllFloors() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER).user());
        mvc.perform(get("/api/v1/floors").param("areaId", Long.toString(area)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].areaId").value(area));
        mvc.perform(post("/api/v1/areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"FORBID\",\"name\":\"Không được tạo\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void BR_M03_22_bedPostReturns201AndWarningWithFullLocation() throws Exception {
        jdbc.update("UPDATE room_type SET standard_beds=1 WHERE id=?", type);
        mvc.perform(post("/api/v1/beds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\":" + room + ",\"code\":\"HTTP-G2\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bed.state").value("EMPTY"))
                .andExpect(jsonPath("$.bed.location.roomId").value(room))
                .andExpect(jsonPath("$.warnings[0].code").value("STANDARD_BED_COUNT_EXCEEDED"));
    }

    @Test
    void Q_260_roomDeactivationListsTheBlockingBed() throws Exception {
        mvc.perform(post("/api/v1/rooms/" + room + "/deactivate")
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Ngừng phòng\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("FR-005"));
    }
}
