package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.family.api.dto.ResidentChannelBelongingView;
import com.eldercare.backend.family.api.spi.ResidentChannelBelongingPort;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 013 T033 (US6; FR-030a, Q-366, research K-16; quickstart G1): kênh người cao tuổi thấy đồ Đang giữ, Đang được sử dụng
 * của chính mình (tên, trạng thái, ảnh), không thấy đồ đã trả; mở được ảnh đồ của mình, ảnh đồ của người khác 403.
 */
class FR_030a_ResidentChannelBelongingsIT extends HandoverTestBase {

    @Autowired
    ResidentChannelBelongingPort port;

    private long photoOf(long belongingId) {
        return jdbc.queryForObject(
                "SELECT CAST(photo_file_ids ->> '$[0]' AS UNSIGNED) FROM belonging_handover WHERE belonging_id = ?",
                Long.class,
                belongingId);
    }

    @Test
    void G1_residentSeesOwnHeldAndInUseItemsWithPhoto_notReturnedOnes() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long held = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        long inUse = intake(resident, "GLASSES", "\"quantity\": 1", location());
        long returned = intake(resident, "PERSONAL_ITEMS", "\"quantity\": 1", location());
        jdbc.update("UPDATE belonging SET status = 'IN_USE' WHERE id = ?", inUse);
        jdbc.update(
                "UPDATE belonging SET status = 'RETURNED', holder_type = NULL, holder_id = NULL WHERE id = ?",
                returned);

        List<ResidentChannelBelongingView> items = port.held(resident);

        assertThat(items).extracting(ResidentChannelBelongingView::belongingId).containsExactly(held, inUse);
        assertThat(items).extracting(ResidentChannelBelongingView::status).containsExactly("HELD", "IN_USE");
        assertThat(items.getFirst().photoFileId()).contains(photoOf(held));
        assertThat(items.getFirst().name()).isNotBlank();
    }

    @Test
    void G1_residentOpensOwnPhoto_butNotAnotherResidentsPhoto() throws Exception {
        long own = resident("IN_RESIDENCE");
        long another = resident("IN_RESIDENCE");
        long ownItem = intake(own, "CLOTHING", "\"quantity\": 1", location());
        long otherItem = intake(another, "CLOTHING", "\"quantity\": 1", location());
        String session = TestUsers.bearer(users.session(users.resident(own)));

        mvc.perform(get("/api/v1/files/" + photoOf(ownItem)).header("Authorization", session))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/files/" + photoOf(otherItem)).header("Authorization", session))
                .andExpect(status().isForbidden());
    }
}
