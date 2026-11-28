package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 FR-041, FR-045; 003 module-interfaces mục 1.4a (chủ dự án chốt 2026-11-16, duyệt khai báo 2026-11-17): đổi loại lưu
 * trú từ bán trú sang nội trú kèm giường mới. Lúc duyệt 003 kiểm FR-018 và giữ giường bằng phân bổ Tương lai đầu tiên; ngày
 * hiệu lực phân bổ bắt đầu cùng phụ lục, nội dung hợp đồng có giường và loại phòng của giường mới.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_041_DayCareToResidentialIT extends ChangeTestBase {

    private long dayCareContract(long residentId) throws Exception {
        ObjectNode body = body(residentId);
        body.put("residenceType", "DAY_CARE");
        body.remove("roomTypeId");
        body.put("residencyFeeBillingPeriod", "SESSION");
        ArrayNode schedule = body.putArray("arrivalSchedule");
        ObjectNode line = schedule.addObject();
        line.put("dayOfWeek", 1);
        line.put("arrivalTime", "08:00");
        line.put("departureTime", "16:00");
        return activeContract(residentId, body);
    }

    private ObjectNode toResidential(long contractId, long bedId, LocalDate effective) {
        return request(
                contractId,
                "[\"RESIDENCE_TYPE\"]",
                "{\"residenceType\": \"LONG_TERM_RESIDENTIAL\", \"newBedId\": %d, \"residencyFeeBillingPeriod\": \"MONTH\"}"
                        .formatted(bedId),
                effective);
    }

    private String allocationStatus(long residentId, long bedId) {
        return jdbc.queryForObject(
                "SELECT status FROM bed_allocation WHERE resident_id=? AND bed_id=? ORDER BY id DESC LIMIT 1",
                String.class,
                residentId,
                bedId);
    }

    @Test
    void FR_041_approvalHoldsTheBed_effectiveDateStartsTheFirstAllocation() throws Exception {
        long residentId = residentWithRepresentative();
        long contractId = dayCareContract(residentId);
        long room = realRoom(true);
        long roomType = jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, room);
        LocalDate effective = today().plusDays(2);

        // Giường đang có người: 003 chặn duyệt, không giữ giường, không tạo phụ lục
        long occupied = realBed(room, "OCCUPIED");
        long blockedId = createChange(toResidential(contractId, occupied, effective))
                .get("approvalRequestId")
                .asLong();
        submit(blockedId);
        user.set(manager.user());
        MvcResult blocked =
                approveRaw(blockedId).andExpect(status().isConflict()).andReturn();
        assertThat(unmetCodes(blocked)).contains("ALLOCATION_BED_STATE").doesNotContain("ALLOCATION_NOT_ACTIVE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_allocation WHERE resident_id=?", Integer.class, residentId))
                .isZero();
        assertThat(addenda(contractId)).isEmpty();

        long bed = realBed(room, "EMPTY");
        long requestId = createAndApprove(toResidential(contractId, bed, effective));
        assertThat(allocationStatus(residentId, bed)).isEqualTo("FUTURE");

        applyDueOn(effective);

        assertThat(requestStatus(requestId)).isEqualTo("APPLIED");
        assertThat(allocationStatus(residentId, bed)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("OCCUPIED");
        JsonNode content = latestContent(contractId);
        assertThat(content.get("residenceType").asString()).isEqualTo("LONG_TERM_RESIDENTIAL");
        assertThat(content.get("bedId").asLong()).isEqualTo(bed);
        assertThat(content.get("roomTypeId").asLong()).isEqualTo(roomType);
    }

    @Test
    void FR_041_unchangedResidenceType_rejectedForStaffAndFamily() throws Exception {
        long residentId = residentWithRepresentative();
        long contractId = dayCareContract(residentId);
        ObjectNode sameToDayCare = request(
                contractId,
                "[\"RESIDENCE_TYPE\", \"ARRIVAL_SCHEDULE\"]",
                "{\"residenceType\": \"DAY_CARE\", \"arrivalSchedule\": [{\"dayOfWeek\": 1, \"arrivalTime\": \"08:00\", \"departureTime\": \"16:00\"}]}",
                today().plusDays(1));

        // Nhân viên lập yêu cầu trùng loại lưu trú hiện hành -> 422 RESIDENCE_TYPE_UNCHANGED
        user.set(office.user());
        MvcResult staffResult = createRaw("/api/v1/residency-change-requests", sameToDayCare)
                .andExpect(status().is(422))
                .andReturn();
        assertThat(staffResult.getResponse().getContentAsString()).contains("RESIDENCE_TYPE_UNCHANGED");

        // Người đại diện gửi qua cổng người thân trùng loại lưu trú hiện hành -> 422 RESIDENCE_TYPE_UNCHANGED
        long memberId = representativeOf(residentId);
        families.representative(families.relate(memberId, residentId));
        ObjectNode familyBody = sameToDayCare.deepCopy();
        familyBody.remove("representativeConfirmation");
        user.set(users.family(memberId).user());
        MvcResult familyResult = createRaw("/api/v1/residency-change-requests", familyBody)
                .andExpect(status().is(422))
                .andReturn();
        assertThat(familyResult.getResponse().getContentAsString()).contains("RESIDENCE_TYPE_UNCHANGED");
    }
}
