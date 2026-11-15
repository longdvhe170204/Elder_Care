package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
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
