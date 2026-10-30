package com.eldercare.backend.resident;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 T067 (FR-023e, FR-022, Phụ lục 27 dòng "Nguyện vọng cuối đời"; US7 #6; contracts tag
 * {@code end-of-life-wishes}): nội dung phiếu là thông tin sức khỏe. Bác sĩ xem toàn viện, Điều dưỡng trong phạm vi
 * phân công, người thân khi bản đồng ý Hiệu lực bao gồm họ; Hành chính và Kế toán không xem được nội dung, hồ sơ chỉ
 * cho họ cờ {@code hasEndOfLifeWish}. Chỉ Bác sĩ và Điều dưỡng ghi phiếu.
 */
@AutoConfigureMockMvc
@WithMockUser
class EndOfLifeWishVisibilityIT extends WishTestBase {

    @Autowired
    MockMvc mvc;

    private String url(long residentId) {
        return "/api/v1/residents/" + residentId + "/end-of-life-wishes";
    }

    private void wishExists(long residentId) {
        record(command(residentId).build());
    }

    @Test
    void FR_023e_doctorSeesCurrentAndHistory() throws Exception {
        long residentId = newResident();
        wishExists(residentId);
        recordAs(
                com.eldercare.backend.resident.api.dto.WishRecordSource.FAMILY_REQUEST,
                command(residentId).build());
        user.set(doctor.user());

        mvc.perform(get(url(residentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasEndOfLifeWish").value(true))
                .andExpect(jsonPath("$.current.status").value("ACTIVE"))
                .andExpect(jsonPath("$.current.choice").value("STAY_PALLIATIVE"))
                .andExpect(jsonPath("$.history", hasSize(2)));
    }

    @Test
    void US7_6_officeAndAccountantCannotReadTheWish_butTheProfileCarriesTheFlag() throws Exception {
        long residentId = newResident();
        TestUser accountant = users.staff(Role.ACCOUNTANT);
        user.set(office.user());
        mvc.perform(get("/api/v1/residents/" + residentId))
                .andExpect(jsonPath("$.hasEndOfLifeWish").value(false));
        wishExists(residentId);

        user.set(office.user());
        mvc.perform(get(url(residentId))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/residents/" + residentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasEndOfLifeWish").value(true));
        user.set(accountant.user());
        mvc.perform(get(url(residentId))).andExpect(status().isForbidden());
    }

    @Test
    void FR_023e_nurseSeesOnlyAssignedResidents() throws Exception {
        long residentId = newResident();
        wishExists(residentId);
        long unassigned = newResident();
        user.set(nurse.user());

        mvc.perform(get(url(residentId))).andExpect(status().isOk());
        mvc.perform(get(url(unassigned))).andExpect(status().isNotFound());
    }

    @Test
    void FR_023e_familyMemberSeesTheWishOnlyWhileAnActiveConsentIncludesThem() throws Exception {
        long residentId = newResident();
        wishExists(residentId);
        long covered = relatedFamilyMember(residentId);
        long notCovered = relatedFamilyMember(residentId);
        record(residentId, byResident(Set.of(covered)));

        user.set(users.family(covered).user());
        mvc.perform(get(url(residentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.choice").exists());
        user.set(users.family(notCovered).user());
        mvc.perform(get(url(residentId))).andExpect(status().isForbidden());
    }

    @Test
    void FR_023a_onlyDoctorAndNurseRecord_officeIsForbidden() throws Exception {
        long residentId = newResident();
        long doctorId = doctor.accountId();
        String body = """
                {"choice":"RETURN_HOME","firstContact":"Con trai 0901","respondentType":"RESIDENT",
                 "assessingDoctorId":%d,"selfDeterminationAssessment":{"canSelfDetermine":true,
                 "supportMethod":"Giải thích bằng lời","reviewDate":"2099-01-01"},"signerType":"RESIDENT",
                 "signedAt":"2026-10-20T08:00:00Z","signedCopyFileId":%d,"source":"ADMISSION"}""".formatted(doctorId, evidence());

        user.set(office.user());
        mvc.perform(post(url(residentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        assignNurse(nurse, residentId);
        user.set(nurse.user());
        mvc.perform(post(url(residentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.choice").value("RETURN_HOME"))
                .andExpect(jsonPath("$.signerType").value("RESIDENT"))
                .andExpect(jsonPath("$.selfDeterminationAssessment.canSelfDetermine")
                        .value(true))
                .andExpect(jsonPath("$.selfDeterminationAssessment.reviewDate").value("2099-01-01"))
                .andExpect(jsonPath("$.source").value("ADMISSION"));
    }

    @Test
    void FR_023c_httpMapsSignerErrorsTo409And422() throws Exception {
        long residentId = newResident();
        long stranger = newFamilyMember();
        assignNurse(nurse, residentId);
        user.set(nurse.user());
        String body = """
                {"choice":"RETURN_HOME","firstContact":"Con trai 0901","respondentType":"PATIENT_REPRESENTATIVE",
                 "respondentFamilyMemberId":%d,"relationship":"Con","assessingDoctorId":%d,
                 "selfDeterminationAssessment":{"canSelfDetermine":false,"supportMethod":"Giải thích bằng lời",
                 "reviewDate":"2099-01-01"},"signerType":"PATIENT_REPRESENTATIVE","signerFamilyMemberId":%d,
                 "representativeSelection":"CHOSEN_BY_FAMILY","signedAt":"2026-10-20T08:00:00Z",
                 "signedCopyFileId":%d,"source":"ADMISSION"}""".formatted(stranger, doctor.accountId(), stranger, evidence());

        mvc.perform(post(url(residentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_SIGNER"));

        String noAssessment = body.replaceAll("\"selfDeterminationAssessment\":\\{[^}]*\\},", "");
        mvc.perform(post(url(residentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noAssessment))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_023c_representativeChosenByResident_isReadableBack() throws Exception {
        long residentId = newResident();
        long family = relatedFamilyMember(residentId);
        record(command(residentId)
                .representativeSigns(family, RepresentativeSelection.CHOSEN_BY_RESIDENT)
                .build());
        user.set(doctor.user());

        mvc.perform(get(url(residentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.signerType").value("PATIENT_REPRESENTATIVE"))
                .andExpect(jsonPath("$.current.representativeSelection").value("CHOSEN_BY_RESIDENT"));
    }
}
