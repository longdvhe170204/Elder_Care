package com.eldercare.backend.resident;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.AssessmentView;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BUG-03 (FR-008, FR-009; contracts/api-001.yaml, schema {@code Profile}): {@code GET /residents/{id}} nạp mục sức khỏe
 * đang hiệu lực, nhu cầu và lưu ý chăm sóc của lần Đã xác nhận gần nhất để khối An toàn hiện được dị ứng.
 */
@AutoConfigureMockMvc
@WithMockUser
class ProfileSafetyFieldsIT extends AssessmentTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    HealthItemService items;

    private long residentWithSafetyData() {
        long id = newResident();
        setStatus(id, ResidentStatus.ADMISSION_IN_PROGRESS.name());
        long need = careNeedId();
        assignNurse(nurse, id);
        user.set(nurse.user());
        AssessmentView draft = assessments.create(target(id), AssessmentType.INITIAL, "Căn cứ thử");
        user.set(doctor.user());
        AssessmentView saved = assessments.saveDraft(
                target(id), draft.id(), etag(draft), full(CareLevel.REGULAR, "Nhận xét", "Dễ ngã", Set.of(need)));
        assessments.confirm(target(id), saved.id(), etag(saved));
        items.record(
                target(id),
                new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false));
        return id;
    }

    @Test
    void BUG03_doctor_seesActiveAllergyCareNoteAndCareNeeds() throws Exception {
        long id = residentWithSafetyData();
        user.set(doctor.user());

        mvc.perform(get("/api/v1/residents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeHealthItems", hasSize(1)))
                .andExpect(jsonPath("$.activeHealthItems[0].type").value("ALLERGY"))
                .andExpect(jsonPath("$.activeHealthItems[0].allergen.name").isNotEmpty())
                .andExpect(jsonPath("$.careNote").value("Dễ ngã"))
                .andExpect(jsonPath("$.careNeeds", hasSize(1)));
    }

    @Test
    void BUG03_office_seesCareNoteOnly_healthAndNeedsAreHidden() throws Exception {
        long id = residentWithSafetyData();
        user.set(office.user());

        mvc.perform(get("/api/v1/residents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careNote").value("Dễ ngã"))
                .andExpect(jsonPath("$.activeHealthItems").doesNotExist())
                .andExpect(jsonPath("$.careNeeds").doesNotExist())
                .andExpect(jsonPath("$.hiddenFields", hasItems("activeHealthItems", "careNeeds")));
    }
}
