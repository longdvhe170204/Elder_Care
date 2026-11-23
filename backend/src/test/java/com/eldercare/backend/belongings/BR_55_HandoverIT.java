package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * 013 T012 (US2; BR-M12-01, FR-011 → FR-015a, SC-001; quickstart Q5 → Q8, Q10): lệnh bàn giao tạo đúng một bản ghi mỗi
 * đồ và cập nhật cột suy ra cùng lần; tách đồ, đếm thiếu; người giao là người giữ; bản ghi không sửa được.
 */
class BR_55_HandoverIT extends HandoverTestBase {

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

    /**
     * FR-011, FR-020 (test luồng BF-14 2026-11-23): lịch sử bàn giao nêu tên người giao, người nhận cả khi họ là người cao
     * tuổi, nhân viên (bản ghi chỉ lưu mã).
     */
    @Test
    void FR_011_historyNamesResidentGiverAndStaffRecipient() throws Exception {
        shifts.staff(new com.eldercare.backend.staffing.api.dto.StaffPublicView(
                officeA.staffId(), "Hành chính thử A", "Hành chính", null, null));
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 2", location());

        getJson(tokenA, "/api/v1/belongings/" + clothes)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handovers[0].giverType").value("RESIDENT"))
                .andExpect(jsonPath("$.handovers[0].giverName").value("Người cao tuổi thử"))
                .andExpect(jsonPath("$.handovers[0].recipientType").value("STAFF"))
                .andExpect(jsonPath("$.handovers[0].recipientName").value("Hành chính thử A"));
    }

    /** Mở đồ từ thông báo có nguồn là bản ghi bàn giao (test luồng BF-14 2026-11-23): đọc đồ, người cao tuổi của bản ghi. */
    @Test
    void FR_032_handoverRef_givesBelongingAndResident_unknownIs404() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        long handover = latestHandover(clothes);

        getJson(tokenA, "/api/v1/belongings/handovers/" + handover)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(handover))
                .andExpect(jsonPath("$.belongingId").value(clothes))
                .andExpect(jsonPath("$.residentId").value(resident))
                .andExpect(jsonPath("$.kind").value("INTAKE"));
        getJson(tokenA, "/api/v1/belongings/handovers/" + Long.MAX_VALUE).andExpect(status().isNotFound());
    }
}
