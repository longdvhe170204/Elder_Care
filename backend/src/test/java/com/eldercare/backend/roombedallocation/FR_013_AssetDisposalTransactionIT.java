package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 019 T026: kiểm transaction duyệt → áp dụng thật của 000, không gọi handler trực tiếp. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FR_013_AssetDisposalTransactionIT extends AssetTestBase {
    @Test
    void FR_013_approvalCommitAppliesOrdinaryAssetDisposalInANewTransaction() throws Exception {
        long asset = created(donated("Tặng"));
        approveDisposal(asset);
        assertThat(stateOf(asset)).isEqualTo("DISPOSED");
    }

    @Test
    void FR_015_approvalCommitDisposesAssetAndRetiresBedAtomically() throws Exception {
        long bed = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,?)", bed, room, "G-" + bed);
        long asset = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO asset(id,code,type_id,name,department_id,source,bed_id,status,location_type,"
                        + "location_id,floor_area_id,created_at) VALUES(?,?,?,?,?,'BED',?,'READY','ROOM',?,?,?)",
                asset,
                "BED-" + bed,
                bedType,
                "Giường " + bed,
                care,
                bed,
                room,
                floor,
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
        as(careLead);
        approveDisposal(asset);
        assertThat(stateOf(asset)).isEqualTo("DISPOSED");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("RETIRED");
    }

    private void approveDisposal(long asset) throws Exception {
        command(asset, "request-disposal", "{\"reason\":\"Hỏng không sửa được\"}")
                .andExpect(status().isOk());
        assertThat(stateOf(asset)).isEqualTo("READY");
        long request = jdbc.queryForObject("SELECT disposal_request_id FROM asset WHERE id=?", Long.class, asset);
        as(manager);
        String etag = mvc.perform(get("/api/v1/approval-requests/" + request))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(post("/api/v1/approval-requests/" + request + "/approve")
                        .header("If-Match", etag)
                        .header("Idempotency-Key", "disposal-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM approval_request WHERE id=?", String.class, request))
                .isEqualTo("APPLIED");
        assertThat(jdbc.queryForMap("SELECT result_type, result_id FROM approval_request WHERE id=?", request))
                .containsEntry("result_type", "M03.ASSET")
                .containsEntry("result_id", Long.toString(asset));
        assertThat(jdbc.queryForObject("SELECT disposed_at IS NOT NULL FROM asset WHERE id=?", Boolean.class, asset))
                .isTrue();
    }
}
