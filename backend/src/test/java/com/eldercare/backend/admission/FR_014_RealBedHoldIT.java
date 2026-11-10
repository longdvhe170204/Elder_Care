package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T025 (US2; FR-012, FR-014; BR-M02-01): ghi nhận đã liên hệ giữ chỗ giường thật của 003 qua {@code
 * BedAllocationCommandApi} (bản cài thật, không mock), từ chối giường trả giường về cho 003.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_014_RealBedHoldIT extends WaitingListTestBase {

    private long bed;

    private void emptyBed() {
        long area = TestUsers.nextId();
        long floor = TestUsers.nextId();
        long type = TestUsers.nextId();
        long room = TestUsers.nextId();
        bed = TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES(?,?,?)", area, "A" + area, "Khu " + area);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)", floor, area, "F" + floor, "Tầng " + floor);
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(?,?,2)", type, "Loại " + type);
        jdbc.update("INSERT INTO room_type_care_level VALUES(?,'BASIC')", type);
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                room,
                floor,
                type,
                "R" + room,
                "Phòng " + room);
        jdbc.update("UPDATE room SET allowed_care_levels=JSON_ARRAY('BASIC') WHERE id=?", room);
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G1')", bed, room);
        jdbc.update(
                "INSERT INTO bed_state_history(bed_id,to_state,event,occurred_at,basis_type,basis_id,floor_area_id)"
                        + " VALUES(?,'EMPTY','SETUP',?,'TEST',1,?)",
                bed,
                Timestamp.from(time.now().minusSeconds(3600)),
                area);
    }

    private void command(long entryId, String path, String body) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/waiting-list-entries/" + entryId + "/" + path)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private String bedState() {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id = ?", String.class, bed);
    }

    @Test
    void FR_014_recordContacted_holdsTheRealBed_andDecliningReturnsIt() throws Exception {
        emptyBed();
        long entryId = newEntry(Gender.FEMALE, LocalDate.of(2000, 1, 1).minusDays(System.nanoTime() % 3000), "BASIC")
                .get("id")
                .asLong();

        command(entryId, "record-contacted", "{\"bedId\":" + bed + ",\"outOfOrderReason\":\"Chọn thử\"}");
        assertThat(bedState()).as("giữ chỗ cho hồ sơ chờ").isEqualTo("HELD");
        assertThat(jdbc.queryForObject("SELECT hold_waiting_record_id FROM bed WHERE id = ?", Long.class, bed))
                .isEqualTo(entryId);

        command(entryId, "decline-bed", "{\"reason\":\"Gia đình chưa sẵn sàng\"}");
        assertThat(bedState()).as("trả giường về cho 003").isNotEqualTo("HELD");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/waiting-list-entries/" + entryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }
}
