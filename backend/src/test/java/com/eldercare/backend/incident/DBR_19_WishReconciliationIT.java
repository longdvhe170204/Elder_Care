package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.resident.api.dto.WishChoice;
import org.junit.jupiter.api.Test;

/**
 * DBR-19, FR-048a (US5 #3, #4): có nguyện vọng thì ghi biện pháp hồi sức bị chặn tới khi có xác nhận đối chiếu của
 * người được xem đủ thẻ; mỗi sự cố một xác nhận; không có nguyện vọng thì không cần và không có gì để xác nhận.
 */
class DBR_19_WishReconciliationIT extends EmergencyTestBase {
    private static final String CHECK = "/api/v1/incidents/{id}/confirm-wish-check";
    private static final String PROGRESSIONS = "/api/v1/incidents/{id}/progressions";

    /** BR-M05-13: có nguyện vọng cuối đời thì phải xác nhận đã đối chiếu trước khi ghi biện pháp hồi sức. */
    @Test
    void DBR_19_BR_M05_13_resuscitationBlockedUntilWishChecked() throws Exception {
        activeWish(WishChoice.STAY_PALLIATIVE);
        long incident = criticalIncident();
        String resuscitation = "{\"progressionTypeId\":" + resuscitationType() + ",\"content\":\"Ép tim\"}";

        post(nurse, PROGRESSIONS, incident, resuscitation)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WISH_NOT_CHECKED"));

        doReturn(Visibility.NONE).when(visibility).canView(any(), any(), any());
        post(nurse, CHECK, incident, "{}").andExpect(status().isForbidden());
        doReturn(Visibility.FULL).when(visibility).canView(any(), any(), any());

        post(nurse, CHECK, incident, "{}").andExpect(status().isCreated());
        assertThat(count("M05.WISH_CHECK", incident)).isEqualTo(1);
        post(nurse, CHECK, incident, "{}").andExpect(status().isConflict());

        post(nurse, PROGRESSIONS, incident, resuscitation).andExpect(status().isCreated());
    }

    private long resuscitationType() {
        return catalog.progressionTypes().stream()
                .filter(type -> type.resuscitation())
                .findFirst()
                .orElseThrow()
                .id();
    }
}
