package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.node.ObjectNode;

/**
 * FR-043l (Q-445, R22): loại thay đổi "Loại phòng" (mã {@code BED}) chỉ dùng khi giường mới thuộc loại phòng khác loại phòng
 * của hợp đồng hiện hành. Giường cùng loại phòng thì bị chặn 422 {@code BED_CHANGE_SAME_ROOM_TYPE}, hướng dẫn dùng Chuyển
 * giường trực tiếp (7.4), không lập yêu cầu, không ký phụ lục. Không chặn khi đổi giường đi kèm bán trú sang nội trú (giường
 * đầu tiên) hoặc yêu cầu lập sau chuyển giường gấp; yêu cầu lập trước khi có quy tắc bị chặn lại khi gửi duyệt.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043l_RoomTypeChangeIT extends ChangeTestBase {

    private long roomTypeOf(long roomId) {
        return jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, roomId);
    }

    /** Hợp đồng ghi loại phòng của {@code currentRoom}; người cao tuổi đang ở một giường của phòng đó. */
    private long contractInRoom(long residentId, long currentRoom) throws Exception {
        ObjectNode body = body(residentId);
        body.put("roomTypeId", roomTypeOf(currentRoom));
        long contractId = activeContract(residentId, body);
        activeAllocation(residentId, realBed(currentRoom, "OCCUPIED"));
        return contractId;
    }

    private ObjectNode bedChange(long contractId, long newBed) {
        return request(contractId, "[\"BED\"]", "{\"newBedId\": %d}".formatted(newBed), today().plusDays(3));
    }

    @Test
    void R22_bedOfSameRoomType_rejectedWithDirectTransferHint() throws Exception {
        long room = realRoom(true);
        long residentId = residentWithRepresentative();
        long contractId = contractInRoom(residentId, room);
        long sameTypeBed = realBed(room, "EMPTY");

        user.set(office.user());
        MvcResult result = createRaw("/api/v1/residency-change-requests", bedChange(contractId, sameTypeBed))
                .andExpect(status().is(422))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("BED_CHANGE_SAME_ROOM_TYPE");
        assertThat(body).contains("Chuyển giường trực tiếp");
    }

    @Test
    void R22_bedOfAnotherRoomType_created() throws Exception {
        long room = realRoom(true);
        long otherRoom = realRoom(true);
        long residentId = residentWithRepresentative();
        long contractId = contractInRoom(residentId, room);
        long otherTypeBed = realBed(otherRoom, "EMPTY");

        assertThat(createChange(bedChange(contractId, otherTypeBed)).get("approvalRequestId"))
                .isNotNull();
    }

    @Test
    void R22_requestCreatedBeforeRule_blockedOnSubmit() throws Exception {
        long room = realRoom(true);
        long otherRoom = realRoom(true);
        long residentId = residentWithRepresentative();
        long contractId = contractInRoom(residentId, room);
        long otherTypeBed = realBed(otherRoom, "EMPTY");
        long id = createChange(bedChange(contractId, otherTypeBed))
                .get("approvalRequestId")
                .asLong();
        // Phòng của giường mới đổi sang cùng loại phòng với hợp đồng sau khi lập (yêu cầu cũ lập trước khi có quy tắc)
        jdbc.update("UPDATE room SET room_type_id=? WHERE id=?", roomTypeOf(room), otherRoom);

        user.set(office.user());
        MvcResult blocked =
                generic(id, "submit", "{}").andExpect(status().isConflict()).andReturn();
        assertThat(blocked.getResponse().getContentAsString()).contains("BED_CHANGE_SAME_ROOM_TYPE");
    }
}
