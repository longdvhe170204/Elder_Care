package com.eldercare.backend.resident;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BUG-03, phía người thân (FR-009, FR-022; schema {@code FamilyProfile}): có bản đồng ý thì thấy mục sức khỏe; không bao
 * giờ có lưu ý và nhu cầu chăm sóc, vì đó là kết quả đánh giá.
 */
@AutoConfigureMockMvc
@WithMockUser
class ProfileFamilySafetyFieldsIT extends ConsentTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    HealthItemService items;

    @Test
    void BUG03_familyWithConsent_seesHealthItems_butNeverCareNoteOrNeeds() throws Exception {
        long id = newResident();
        long member = relatedFamilyMember(id);
        TestUser family = users.family(member);
        record(id, byResident(Set.of(member)));
        user.set(doctor.user());
        items.record(
                target(id),
                new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false));
        user.set(family.user());

        mvc.perform(get("/api/v1/residents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeHealthItems", hasSize(1)))
                .andExpect(jsonPath("$.careNote").doesNotExist())
                .andExpect(jsonPath("$.careNeeds").doesNotExist())
                .andExpect(jsonPath("$.hiddenFields", hasItems("careNote", "careNeeds")));
    }
}
