package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.spi.PickupRecordPort;
import com.eldercare.backend.care.api.spi.DayCarePickupPort;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * 012 T018 (US2 kịch bản 1 → 3, 7, 9 → 11; FR-021 → FR-023, FR-026, FR-017; BR-M10-03; Q13 → Q16): ghi đón kiểm người
 * đón thuộc danh sách tại thời điểm và giấy tờ khớp; không đạt thì ghi lần chặn (còn lại sau khi lệnh bị từ chối) và báo
 * Trưởng tầng; người thực hiện phải có quyền lệnh nguồn; bản ghi đón dùng một lần qua hai port của 004, 005; bỏ người đón
 * thì bản ghi chưa dùng chuyển Không dùng ngay, kể cả khi hai lệnh chạy cùng lúc.
 */
class BR_M10_03_PickupProcessIT extends PickupTestBase {

    @Autowired
    PickupRecordPort absencePickups;

    @Autowired
    DayCarePickupPort dayCarePickups;

    @Test
    void US2_1_listedPickerWithMatchingDocument_createsUnusedRecordForTheSourceCommand() throws Exception {
        Instant before = time.now();

        JsonNode record =
                json(pickup(office, "TEMPORARY_ABSENCE", rMember, R_DOC).andExpect(status().isCreated()));

        assertThat(record.get("type").asString()).isEqualTo("TEMPORARY_ABSENCE");
        assertThat(record.get("familyMemberId").asLong()).isEqualTo(rMember);
        assertThat(record.get("basis").asString()).isEqualTo("PICKUP_LIST");
        assertThat(record.get("status").asString()).isEqualTo("UNUSED");
        Instant at = Instant.parse(record.get("at").asString());
        assertThat(at).isAfterOrEqualTo(before.minusMillis(1));
        assertThat(Instant.parse(record.get("expiresAt").asString())).isEqualTo(at.plus(Duration.ofHours(2)));
        assertThat(absencePickups.validPickupRecord(a, rMember, time.now()))
                .hasValueSatisfying(
                        v -> assertThat(v.id()).isEqualTo(record.get("id").asLong()));
        JsonNode list = getJson("/api/v1/residents/" + a + "/pickup-records");
        assertThat(list)
                .singleElement()
                .satisfies(p -> assertThat(p.get("id").asLong())
                        .isEqualTo(record.get("id").asLong()));
    }
}
