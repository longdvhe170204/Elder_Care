package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.BusinessTime;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T043 (US5; FR-041, FR-042, FR-045; 003 FR-029): yêu cầu đổi giường qua {@code BedAllocationCommandApi} thật của 003.
 * Duyệt thì đặt trước phân bổ Tương lai ở giường mới; tới ngày hiệu lực, điều kiện áp dụng kiểm phân bổ đặt trước đó ({@code
 * checkScheduledTransfer}, không kiểm lại như lúc đặt trước) rồi chuyển với mốc 00:00 ngày hiệu lực. Giường mới không còn đạt
 * FR-018 thì yêu cầu Áp dụng không thành, phụ lục Không áp dụng, phân bổ theo lịch bị hủy, người ở lại giường cũ.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_045_BedChangeApplyIT extends ChangeTestBase {

    private record Beds(long residentId, long contractId, long oldAllocation, long newBed, long newRoom) {}

    private Beds setup() throws Exception {
        Setup s = activeSetup();
        long oldAllocation = activeAllocation(s.residentId(), realBed(realRoom(true), "OCCUPIED"));
        long newRoom = realRoom(true);
        return new Beds(s.residentId(), s.contractId(), oldAllocation, realBed(newRoom, "EMPTY"), newRoom);
    }

    private long approveBedChange(Beds b, LocalDate effective) throws Exception {
        return createAndApprove(
                request(b.contractId(), "[\"BED\"]", "{\"newBedId\": %d}".formatted(b.newBed()), effective));
    }

    private String allocationStatus(long residentId, long bedId) {
        return jdbc.queryForObject(
                "SELECT status FROM bed_allocation WHERE resident_id=? AND bed_id=? ORDER BY id DESC LIMIT 1",
                String.class,
                residentId,
                bedId);
    }

    /** Dịch vụ phí lưu trú của loại phòng, đơn giá mở từ 2020-01-01. */
    private void residencyFee(long roomTypeId, int price) {
        String name = "Phí lưu trú " + roomTypeId + " " + System.nanoTime();
        jdbc.update(
                M02Sql.INSERT_SERVICE,
                name,
                M02Sql.attributes("RESIDENCY_FEE", "tháng", "MONTH", null, roomTypeId, null));
        long id = jdbc.queryForObject(M02Sql.SERVICE_ID, Long.class, name);
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from) VALUES (?, ?, '2020-01-01')",
                id,
                price);
    }

    private long roomTypeOf(long roomId) {
        return jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, roomId);
    }

    /** FR-038: đổi sang giường ở loại phòng khác thì ảnh hưởng chi phí có dòng phí lưu trú theo loại phòng cũ, mới. */
    @Test
    void FR_038_bedOfAnotherRoomType_showsResidencyFeeImpact() throws Exception {
        long oldRoom = realRoom(true);
        long newRoom = realRoom(true);
        residencyFee(roomTypeOf(oldRoom), 3_000_000);
        residencyFee(roomTypeOf(newRoom), 5_000_000);
        long residentId = residentWithRepresentative();
        var body = body(residentId);
        body.put("roomTypeId", roomTypeOf(oldRoom));
        long contractId = activeContract(residentId, body);
        activeAllocation(residentId, realBed(oldRoom, "OCCUPIED"));
        long newBed = realBed(newRoom, "EMPTY");

        var created = createChange(
                request(contractId, "[\"BED\"]", "{\"newBedId\": %d}".formatted(newBed), today().plusDays(2)));

        var fee = java.util.stream.StreamSupport.stream(
                        created.get("costImpact").spliterator(), false)
                .filter(item -> "RESIDENCY_FEE".equals(item.get("item").asString()))
                .findFirst();
        assertThat(fee).as("costImpact %s", created.get("costImpact")).isPresent();
        assertThat(fee.get().get("previousUnitPrice").asLong()).isEqualTo(3_000_000);
        assertThat(fee.get().get("newUnitPrice").asLong()).isEqualTo(5_000_000);
    }

    @Test
    void FR_041_approveReserves_effectiveDateTransfersAtMidnight() throws Exception {
        Beds b = setup();
        LocalDate effective = today().plusDays(2);
        long requestId = approveBedChange(b, effective);
        assertThat(allocationStatus(b.residentId(), b.newBed())).isEqualTo("FUTURE");

        applyDueOn(effective);

        assertThat(requestStatus(requestId)).isEqualTo("APPLIED");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE id=?", String.class, b.oldAllocation()))
                .isEqualTo("CLOSED");
        assertThat(allocationStatus(b.residentId(), b.newBed())).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                                "SELECT planned_at FROM bed_allocation WHERE resident_id=? AND bed_id=?",
                                Timestamp.class,
                                b.residentId(),
                                b.newBed())
                        .toInstant())
                .isEqualTo(effective.atStartOfDay(BusinessTime.ZONE).toInstant());
        // 003 FR-016a (quickstart 003 Q21, B2): job chạy 12:00, trễ hơn CFG-M03-08 (30 phút) so với mốc 00:00
        assertThat(jdbc.queryForObject(
                        "SELECT executed_late FROM bed_allocation WHERE resident_id=? AND bed_id=?",
                        Boolean.class,
                        b.residentId(),
                        b.newBed()))
                .isTrue();
    }
}
