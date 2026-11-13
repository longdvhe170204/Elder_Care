package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

/**
 * 003 FR-030 (Q-306), hợp đồng 003 mục 1.4; api-004 {@code reference} loại {@code BED_TRANSFER_ORDER} (chủ dự án duyệt nối
 * nhánh 2026-11-12): Hành chính lập yêu cầu đổi giường sau lần chuyển gấp khác giá, tham chiếu phân bổ của lần chuyển đó. Lập
 * thì 003 gỡ dấu "chờ lập yêu cầu"; từ chối thì phân bổ mang dấu "chờ chuyển về phòng cùng giá"; duyệt và áp dụng thì không
 * đặt trước, không chuyển giường lần nữa (người đã ở giường mới), phụ lục đổi loại phòng theo giường mới để đơn giá mới áp
 * dụng từ ngày hiệu lực. Giường, phân bổ là dữ liệu thật của 003.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_030_UrgentTransferRequestIT extends ChangeTestBase {

    private record Urgent(long contractId, long allocationId, long bedId) {}

    /** Người đã được chuyển gấp sang giường khác giá: phân bổ Hiệu lực mang dấu chờ lập yêu cầu. */
    private Urgent urgentlyTransferred() throws Exception {
        Setup s = activeSetup();
        long bed = realBed(realRoom(true), "OCCUPIED");
        long allocation = activeAllocation(s.residentId(), bed);
        jdbc.update("UPDATE bed_allocation SET awaiting_request=TRUE WHERE id=?", allocation);
        return new Urgent(s.contractId(), allocation, bed);
    }

    private long createUrgentRequest(Urgent u, LocalDate effective) throws Exception {
        ObjectNode body = request(u.contractId(), "[\"BED\"]", "{\"newBedId\": %d}".formatted(u.bedId()), effective);
        body.putObject("reference").put("type", "BED_TRANSFER_ORDER").put("id", u.allocationId());
        long id = createChange(body).get("approvalRequestId").asLong();
        assertThat(flags(u.allocationId()))
                .as("lập yêu cầu gỡ dấu chờ lập yêu cầu")
                .containsEntry("awaiting_request", false);
        submit(id);
        return id;
    }

    private Map<String, Object> flags(long allocationId) {
        return jdbc.queryForMap(
                "SELECT status, awaiting_request, awaiting_same_price FROM bed_allocation WHERE id=?", allocationId);
    }

    @Test
    void FR_030_rejected_allocationAwaitsSamePriceRoom() throws Exception {
        Urgent u = urgentlyTransferred();
        long id = createUrgentRequest(u, today());

        user.set(manager.user());
        generic(id, "reject", "{\"comment\":\"Không đồng ý đơn giá mới\"}").andExpect(status().isOk());

        // Quickstart 003 Q15 (B1): đặt dấu trong chính lệnh từ chối (người duyệt là người thực hiện), không chờ
        // listener sau commit
        assertThat(flags(u.allocationId())).containsEntry("awaiting_same_price", true);
        assertThat(flags(u.allocationId())).containsEntry("status", "ACTIVE");
    }

    private int reminderCycles(long allocationId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM reminder_marker WHERE kind='M03.ALLOCATION_REMINDER.AWAITING_REQUEST'"
                        + " AND entity_type='BED_ALLOCATION' AND entity_id=? AND marker LIKE 'START:%'",
                Integer.class, allocationId);
    }

    /**
     * Chủ dự án chốt 2026-11-12: yêu cầu sau chuyển gấp bị hủy hoặc Áp dụng không thành thì 003 đặt lại dấu "chờ lập yêu
     * cầu" và mở lại mốc nhắc CFG-M03-02.
     */
    @Test
    void FR_030_cancelledOrApplyFailed_flagAndReminderRestored() throws Exception {
        Urgent u = urgentlyTransferred();
        long cancelled = createUrgentRequest(u, today());
        generic(cancelled, "cancel", "{\"reason\":\"Lập nhầm ngày hiệu lực\"}").andExpect(status().isOk());
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(flags(u.allocationId())).containsEntry("awaiting_request", true));
        assertThat(reminderCycles(u.allocationId())).isEqualTo(1);

        long failed = createUrgentRequest(u, today().plusDays(1));
        managerApproves(failed);
        jdbc.update("UPDATE contract SET status='CANCELLED' WHERE id=?", u.contractId());
        applyDueOn(today().plusDays(1));

        assertThat(requestStatus(failed)).isEqualTo("APPLY_FAILED");
        assertThat(flags(u.allocationId())).containsEntry("awaiting_request", true);
        assertThat(reminderCycles(u.allocationId())).isEqualTo(2);
    }

    @Test
    void FR_030_approvedAndApplied_noSecondTransfer() throws Exception {
        Urgent u = urgentlyTransferred();
        long id = createUrgentRequest(u, today());

        managerApproves(id);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=(SELECT resident_id FROM bed_allocation WHERE id=?)"
                                + " AND status='FUTURE'",
                        Integer.class,
                        u.allocationId()))
                .as("không đặt trước phân bổ")
                .isZero();
        applyDueOn(today());

        assertThat(requestStatus(id)).isEqualTo("APPLIED");
        assertThat(flags(u.allocationId()))
                .containsEntry("status", "ACTIVE")
                .containsEntry("awaiting_same_price", false);
        assertThat(latestContent(u.contractId()).get("bedId").asLong()).isEqualTo(u.bedId());
        assertThat(latestContent(u.contractId()).get("roomTypeId").asLong())
                .as("đơn giá theo loại phòng của giường mới")
                .isEqualTo(jdbc.queryForObject(
                        "SELECT r.room_type_id FROM bed b JOIN room r ON r.id=b.room_id WHERE b.id=?",
                        Long.class,
                        u.bedId()));
    }
}
