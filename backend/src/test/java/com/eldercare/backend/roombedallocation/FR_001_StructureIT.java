package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.roombedallocation.api.RoomPolicyApi;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import com.eldercare.backend.roombedallocation.internal.structure.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** T011: dựng cơ cấu, sao chính sách loại phòng và quyền thật Phụ lục 27. */
@AutoConfigureMockMvc
@WithMockUser
class FR_001_StructureIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @Autowired
    LocationStructureApi locations;

    @Autowired
    RoomPolicyApi policies;

    /** T017: controller cơ cấu ghi qua HTTP (ETag), {@code LocationStructureApi}, {@code RoomPolicyApi} đọc cùng dữ liệu. */
    @Test
    void FR_001_structureHttpCommandsAndPublicReadApisShareTheSameData() throws Exception {
        String code = "H" + area;
        var created = mvc.perform(post("/api/v1/areas")
                        .header("Idempotency-Key", "area-" + area)
                        .contentType("application/json")
                        .content("{\"code\":\"" + code + "\",\"name\":\"Khu HTTP\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long id = tools.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();
        String etag = mvc.perform(get("/api/v1/areas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(put("/api/v1/areas/" + id)
                        .header("If-Match", etag)
                        .header("Idempotency-Key", "area-put-" + area)
                        .contentType("application/json")
                        .content("{\"code\":\"" + code + "\",\"name\":\"Khu đã đổi tên\"}"))
                .andExpect(status().isOk());
        assertThat(locations.area(id))
                .hasValueSatisfying(a -> assertThat(a.name()).isEqualTo("Khu đã đổi tên"));
        assertThat(locations.room(room))
                .hasValueSatisfying(r -> assertThat(r.floorId()).isEqualTo(floor));
        assertThat(policies.allocationProperties(bed).careLevels()).containsExactly(CareLevelCode.BASIC);
    }
}
