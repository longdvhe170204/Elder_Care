package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * 013 T012 (US2; BR-M12-01, FR-011 → FR-015a, SC-001; quickstart Q5 → Q8, Q10): lệnh bàn giao tạo đúng một bản ghi mỗi
 * đồ và cập nhật cột suy ra cùng lần; tách đồ, đếm thiếu; người giao là người giữ; bản ghi không sửa được.
 */
class BR_M12_01_HandoverIT extends HandoverTestBase {

    @Test
    void Q5_FR_012_partialTransfer_splitsIntoNewBelonging_totalUnchanged() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long floor1 = location();
        long floor2 = location();
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 10", floor1);

        handover(
                        tokenA,
                        command(
                                "TRANSFER_CUSTODY",
                                item(clothes, ", \"quantity\": 4"),
                                ", \"storageLocationId\": " + floor2 + ", \"reason\": \"Chuyển lên tủ tầng 2\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handovers.length()").value(1))
                .andExpect(jsonPath("$.handovers[0].kind").value("TRANSFER_CUSTODY"));

        var all = sameIntake(clothes);
        assertThat(all).hasSize(2);
        long split = all.get(1);
        assertThat(quantityOf(clothes)).isEqualByComparingTo("6");
        assertThat(quantityOf(split)).isEqualByComparingTo("4");
        assertThat(intakeTotal(clothes)).isEqualByComparingTo("10");
        assertThat(jdbc.queryForMap(
                        "SELECT status, storage_location_id, original_belonging_id, current_handover_id FROM belonging"
                                + " WHERE id = ?",
                        split))
                .containsEntry("status", "HELD")
                .containsEntry("storage_location_id", floor2)
                .containsEntry("original_belonging_id", clothes)
                .containsEntry("current_handover_id", latestHandover(split));
        assertThat(jdbc.queryForObject("SELECT storage_location_id FROM belonging WHERE id = ?", Long.class, clothes))
                .isEqualTo(floor1);
        assertThat(jdbc.queryForList(
                        "SELECT kind FROM belonging_handover WHERE belonging_id = ? ORDER BY id", String.class, split))
                .containsExactly("TRANSFER_CUSTODY");
    }

    @Test
    void Q7_FR_014_giverMustBeHolder_unlessHolderIsReallyAbsent() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location);
        onShift(null, officeA.staffId());
        String transfer = ", \"storageLocationId\": " + location() + ", \"reason\": \"Đổi tủ\"";

        handover(tokenB, command("TRANSFER_CUSTODY", item(phone, ""), transfer))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GIVER_NOT_HOLDER"));
        handover(tokenB, command("TRANSFER_CUSTODY", item(phone, ""), transfer + ", \"holderAbsent\": true"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("HOLDER_NOT_ABSENT"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ?", Integer.class, phone))
                .isEqualTo(1);
    }
}
