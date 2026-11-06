package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.spi.PickupRecordPort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * 012 T018 (FR-027, FR-024; CFG-M10-09, CFG-M10-08; Q19): bản ghi đón chưa dùng quá CFG-M10-09 kể từ lúc ghi không còn
 * là căn cứ và việc theo phút ghi Không dùng; ngoại lệ Hiệu lực quá CFG-M10-08 chuyển Hết hạn. Việc xử lý mọi bản ghi đã
 * đến hạn, nên test chỉ xét bản ghi của chính mình.
 */
class FR_027_PickupRecordExpiryIT extends PickupTestBase {

    @Autowired
    PickupRecordPort absencePickups;

    @Autowired
    List<M10TimedTask> tasks;

    @Test
    void FR_027_unusedRecord_staysValidWithinTheWindow_thenTurnsNotUsed() throws Exception {
        JsonNode record =
                json(pickup(office, "TEMPORARY_ABSENCE", rMember, R_DOC).andExpect(status().isCreated()));
        Instant recordedAt = Instant.parse(record.get("at").asString());

        assertThat(absencePickups.validPickupRecord(a, rMember, recordedAt.plus(Duration.ofMinutes(119))))
                .isPresent();
        assertThat(absencePickups.validPickupRecord(a, rMember, recordedAt.plus(Duration.ofHours(2))))
                .isEmpty();

        runMinuteTasks(recordedAt.plus(Duration.ofMinutes(119)));
        assertThat(recordStatus(record)).isEqualTo("UNUSED");
        runMinuteTasks(recordedAt.plus(Duration.ofHours(2)).plusSeconds(30));
        assertThat(recordStatus(record)).isEqualTo("NOT_USED");
        runMinuteTasks(recordedAt.plus(Duration.ofHours(2)).plusSeconds(90));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M10.PICKUP_RECORD_EVENT' AND entity_id = ?",
                        Integer.class,
                        record.get("id").asLong()))
                .isEqualTo(1);
    }

    @Test
    void FR_024_effectiveException_expiresAfterCfgM10_08() throws Exception {
        long exception =
                id(createException(office, "Ngô Hàng Xóm", "079100000077").andExpect(status().isCreated()));
        exceptionCommand(staffSession(Role.NURSING_HOME_MANAGER), exception, "approve", reason("Người đại diện bận"))
                .andExpect(status().isOk());
        Instant effective = time.now();

        runMinuteTasks(effective.plus(Duration.ofMinutes(239)));
        assertThat(exceptionStatus(exception)).isEqualTo("EFFECTIVE");
        runMinuteTasks(effective.plus(Duration.ofHours(4)).plusSeconds(30));
        assertThat(exceptionStatus(exception)).isEqualTo("EXPIRED");
    }

    private void runMinuteTasks(Instant at) {
        tasks.stream()
                .filter(t -> t.cadence() == M10TimedTask.Cadence.MINUTE)
                .filter(t -> t.name().startsWith("PICKUP_"))
                .forEach(t -> t.run(at, at));
    }

    private String recordStatus(JsonNode record) throws Exception {
        for (JsonNode p : getJson("/api/v1/residents/" + a + "/pickup-records")) {
            if (p.get("id").asLong() == record.get("id").asLong()) {
                return p.get("status").asString();
            }
        }
        throw new AssertionError("Không thấy bản ghi đón");
    }
}
