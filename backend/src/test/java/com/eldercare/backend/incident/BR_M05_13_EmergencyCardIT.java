package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.resident.api.dto.WishChoice;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * BR-M05-13, FR-047 (b, c), FR-048 (US5 #1, #4): sự cố Khẩn cấp dựng thẻ thông tin khẩn cấp từ 001, 003, 006, 012 và
 * lưu bản chụp cùng sự cố; nguồn lỗi thì thẻ ghi "không lấy được" và vẫn lưu; người không được xem sức khỏe chỉ thấy
 * nhận dạng.
 */
class BR_M05_13_EmergencyCardIT extends EmergencyTestBase {
    private static final String CARD = "/api/v1/incidents/{id}/emergency-card";

    @Test
    void BR_M05_13_criticalIncidentShowsCardAndStoresSnapshot() throws Exception {
        activeWish(WishChoice.STAY_PALLIATIVE);
        String body = "{\"residentId\":" + resident + ",\"incidentTypeId\":" + typeId("FALL") + ",\"discoveredAt\":\""
                + time.now() + "\",\"description\":\"Khó thở\",\"level\":\"CRITICAL\"}";

        postNew(nurse, INCIDENTS, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emergencyCard.identification.fullName").value("Nguyễn Thị An"))
                .andExpect(jsonPath("$.emergencyCard.identification.roomBed").value("P1 / G1"))
                .andExpect(jsonPath("$.emergencyCard.endOfLifeWish.choice").value("STAY_PALLIATIVE"))
                .andExpect(jsonPath("$.emergencyCard.allergies[0].name").value("Penicillin"))
                .andExpect(jsonPath("$.emergencyCard.chronicConditions[0].name").value("Tăng huyết áp"))
                .andExpect(
                        jsonPath("$.emergencyCard.currentMedications.length()").value(2))
                .andExpect(jsonPath("$.emergencyCard.careNotes").value("Nguy cơ ngã cao, hỗ trợ khi di chuyển"))
                .andExpect(jsonPath("$.emergencyCard.primaryContact.fullName").value("Lê Văn Liên Hệ"))
                .andExpect(jsonPath("$.emergencyCard.criticalFlag").value(false))
                .andExpect(jsonPath("$.emergencyCard.unavailable.length()").value(0));

        long incident = jdbc.queryForObject("SELECT MAX(id) FROM incident WHERE resident_id = ?", Long.class, resident);
        assertThat(count("M05.EMERGENCY_CARD", incident)).isEqualTo(1);
        mvc.perform(as(nurse, MockMvcRequestBuilders.get(CARD, incident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies[0].name").value("Penicillin"))
                .andExpect(jsonPath("$.snapshotAt").isNotEmpty());
    }

    @Test
    void FR_048_noWishCardSaysNotRecorded_andFailedSourceIsListed() throws Exception {
        doThrow(new IllegalStateException("006 lỗi")).when(prescriptions).activeMedications(resident);

        long incident = criticalIncident();

        mvc.perform(as(nurse, MockMvcRequestBuilders.get(CARD, incident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endOfLifeWish.notRecorded").value(true))
                .andExpect(jsonPath("$.unavailable[0]").value("currentMedications"))
                .andExpect(jsonPath("$.allergies[0].name").value("Penicillin"));
    }

    @Test
    void FR_048_viewerWithoutHealthAccessSeesIdentificationOnly() throws Exception {
        long incident = criticalIncident();
        doReturn(Visibility.NONE).when(visibility).canView(any(), any(), any());

        mvc.perform(as(nurse, MockMvcRequestBuilders.get(CARD, incident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identification.fullName").value("Nguyễn Thị An"))
                .andExpect(jsonPath("$.allergies").doesNotExist())
                .andExpect(jsonPath("$.endOfLifeWish").doesNotExist());
    }
}
