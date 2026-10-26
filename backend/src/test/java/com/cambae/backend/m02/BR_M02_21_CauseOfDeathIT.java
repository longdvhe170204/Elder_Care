package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.support.TestUsers.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T062 (US8; FR-069, BR-M02-21): nguyên nhân tử vong là thông tin sức khỏe nên chỉ vai trò xem được nhóm sức khỏe thấy;
 * Hành chính, Kế toán không thấy kể cả người đã ghi nhận lệnh. Bổ sung nguyên nhân và sửa sai đi đường đính chính {@code
 * M02.DEATH_RECORD}.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_21_CauseOfDeathIT extends DeathTestBase {

    private JsonNode record(TestUser who, long residentId) throws Exception {
        return json(
                getAs(who, "/api/v1/residents/" + residentId + "/death-record").andExpect(status().isOk()));
    }

    private static boolean hidden(JsonNode record, String field) {
        for (JsonNode name : record.get("hiddenFields")) {
            if (field.equals(name.asString())) {
                return true;
            }
        }
        return false;
    }

    private long dead(String cause) throws Exception {
        long residentId = inResidence();
        doctorRecordsDeath(residentId, cause);
        return residentId;
    }

    @Test
    void BR_M02_21_doctorSeesTheCause_officeAndAccountantDoNot() throws Exception {
        long residentId = dead("Suy tim cấp");

        JsonNode asDoctor = record(doctor, residentId);
        assertThat(asDoctor.get("cause").asString()).isEqualTo("Suy tim cấp");
        assertThat(hidden(asDoctor, "cause")).isFalse();

        for (TestUser who : new TestUser[] {office, users.staff(Role.ACCOUNTANT)}) {
            JsonNode seen = record(who, residentId);
            assertThat(seen.has("cause")).as("nguyên nhân bị bỏ").isFalse();
            assertThat(hidden(seen, "cause")).isTrue();
            assertThat(seen.get("place").asString()).isEqualTo("Phòng P201");
            assertThat(seen.get("discoveredBy").asString()).isEqualTo("Nhân viên chăm sóc trực");
            assertThat(seen.has("deathAt")).isTrue();
            assertThat(seen.has("confirmedBy")).isTrue();
        }
    }

    @Test
    void BR_M02_21_evenTheAdministrativeStaffWhoRecordedTheCommandCannotReadTheCause() throws Exception {
        long residentId = inResidence();
        transferOut(residentId);
        recordDeath(office, residentId, deathDetails(time.now(), "Nhiễm khuẩn huyết", evidenceFile()));

        JsonNode seen = record(office, residentId);

        assertThat(seen.has("cause")).isFalse();
        assertThat(hidden(seen, "cause")).isTrue();
        assertThat(seen.get("evidenceFileId").asLong())
                .as("bằng chứng do mình tải lên")
                .isPositive();
    }

    @Test
    void FR_069_addingTheCause_goesThroughACorrection_andTheOriginalRowStaysUntouched() throws Exception {
        long residentId = dead(null);
        long recordId =
                jdbc.queryForObject("SELECT id FROM death_record WHERE resident_id = ?", Long.class, residentId);
        assertThat(record(doctor, residentId).has("cause")).isFalse();

        postAs(
                        doctor,
                        "/api/v1/death-records/" + recordId + "/correct",
                        "{\"type\":\"EDIT_CONTENT\",\"correctedContent\":{\"cause\":\"Nhồi máu cơ tim\"},"
                                + "\"reason\":\"Có kết luận của bệnh viện\"}")
                .andExpect(status().isCreated());

        JsonNode corrected = record(doctor, residentId);
        assertThat(corrected.get("cause").asString()).isEqualTo("Nhồi máu cơ tim");
        assertThat(corrected.get("corrected").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject("SELECT cause_of_death FROM death_record WHERE id = ?", String.class, recordId))
                .as("bản gốc nhóm 3 không bị sửa")
                .isNull();
        assertThat(record(office, residentId).has("cause")).isFalse();
    }

    @Test
    void FR_069_aCorrectionCannotTouchTheMomentOfDeathOrTheEvidence() throws Exception {
        long residentId = dead("Suy tim");
        long recordId =
                jdbc.queryForObject("SELECT id FROM death_record WHERE resident_id = ?", Long.class, residentId);

        for (String field : new String[] {"deathAt", "evidenceFileId", "unknown"}) {
            postAs(
                            doctor,
                            "/api/v1/death-records/" + recordId + "/correct",
                            "{\"type\":\"EDIT_CONTENT\",\"correctedContent\":{\"" + field + "\":\"x\"},"
                                    + "\"reason\":\"Sửa thử\"}")
                    .andExpect(status().isUnprocessableContent());
        }
    }

    @Test
    void FR_069_aDeathRecordCannotBeVoided() throws Exception {
        long residentId = dead("Suy tim");
        long recordId =
                jdbc.queryForObject("SELECT id FROM death_record WHERE resident_id = ?", Long.class, residentId);

        postAs(
                        doctor,
                        "/api/v1/death-records/" + recordId + "/correct",
                        "{\"type\":\"CANCEL_RECORD\",\"reason\":\"Ghi nhầm\"}")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_069_aResidentWithoutADeathRecord_returnsNotFound() throws Exception {
        long residentId = inResidence();

        getAs(doctor, "/api/v1/residents/" + residentId + "/death-record").andExpect(status().isNotFound());
    }
}
