package com.cambae.backend.m01;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.support.TestUsers.TestUser;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 T077 (FR-023m, BR-M01-16; US7 #10; contracts {@code /treatment-wishes}): nội dung phiếu thuộc nhóm sức khỏe, quyền
 * theo dòng "Dị ứng, bệnh nền" của Phụ lục 27. Bác sĩ xem toàn viện, Điều dưỡng trong phạm vi, người thân khi bản đồng ý
 * Hiệu lực bao gồm họ; Hành chính bị chặn nội dung nhưng hồ sơ cho cờ {@code hasTreatmentWish}; Kế toán không có cờ.
 */
@AutoConfigureMockMvc
@WithMockUser
class TreatmentWishVisibilityIT extends WishTestBase {

    @Autowired
    MockMvc mvc;

    private String url(long residentId) {
        return "/api/v1/residents/" + residentId + "/treatment-wishes";
    }

    private void wishExists(long residentId) {
        assignNurse(nurse, residentId);
        user.set(nurse.user());
        treatments.record(target(residentId), new TreatmentCommandBuilder().build());
    }

    private String body() {
        return """
                {"preferredHospitals":[{"name":"Bệnh viện X","address":"12 Lê Lợi"}],"askFamilyFirst":false,
                 "accompaniedBy":"FAMILY","respondent":{"type":"RESIDENT"}}""";
    }

    @Test
    void FR_023m_doctorSeesActiveAndHistory() throws Exception {
        long residentId = newResident();
        wishExists(residentId);
        wishExists(residentId);
        user.set(doctor.user());

        mvc.perform(get(url(residentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active.status").value("ACTIVE"))
                .andExpect(jsonPath("$.active.preferredHospitals[0].name").value("Bệnh viện X"))
                .andExpect(jsonPath("$.history", hasSize(2)));
    }

    @Test
    void US7_10_officeIsBlockedFromTheContent_butSeesTheFlagOnTheProfile() throws Exception {
        long residentId = newResident();
        user.set(office.user());
        mvc.perform(get("/api/v1/residents/" + residentId))
                .andExpect(jsonPath("$.hasTreatmentWish").value(false));
        wishExists(residentId);

        user.set(office.user());
        mvc.perform(get(url(residentId))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/residents/" + residentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasTreatmentWish").value(true));
    }

    @Test
    void FR_023m_accountantHasNoFlagAndNoContent() throws Exception {
        long residentId = newResident();
        wishExists(residentId);
        TestUser accountant = users.staff(Role.ACCOUNTANT);
        user.set(accountant.user());

        mvc.perform(get(url(residentId))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/residents/" + residentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasTreatmentWish").doesNotExist())
                .andExpect(jsonPath("$.hiddenFields", org.hamcrest.Matchers.hasItem("hasTreatmentWish")));
    }

    @Test
    void FR_023m_flagIsNotListedAsHidden_forWhoeverSeesIt() throws Exception {
        long residentId = newResident();
        wishExists(residentId);

        for (var viewer : new TestUser[] {office, doctor, nurse}) {
            user.set(viewer.user());
            assignNurse(nurse, residentId);
            mvc.perform(get("/api/v1/residents/" + residentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasTreatmentWish").value(true))
                    .andExpect(jsonPath(
                            "$.hiddenFields",
                            org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("hasTreatmentWish"))));
        }
    }

    @Test
    void FR_023m_nurseSeesOnlyAssignedResidents() throws Exception {
        long residentId = newResident();
        wishExists(residentId);
        long unassigned = newResident();
        user.set(nurse.user());

        mvc.perform(get(url(residentId))).andExpect(status().isOk());
        mvc.perform(get(url(unassigned))).andExpect(status().isNotFound());
    }

    @Test
    void FR_023m_familyMemberSeesTheWishOnlyWhileAnActiveConsentIncludesThem() throws Exception {
        long residentId = newResident();
        wishExists(residentId);
        long covered = relatedFamilyMember(residentId);
        long notCovered = relatedFamilyMember(residentId);
        record(residentId, byResident(Set.of(covered)));

        user.set(users.family(covered).user());
        mvc.perform(get(url(residentId))).andExpect(status().isOk());
        user.set(users.family(notCovered).user());
        mvc.perform(get(url(residentId))).andExpect(status().isForbidden());
    }

    @Test
    void FR_023j_onlyDoctorAndNurseRecord_officeIsForbidden() throws Exception {
        long residentId = newResident();
        user.set(office.user());
        mvc.perform(post(url(residentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isForbidden());

        assignNurse(nurse, residentId);
        user.set(nurse.user());
        mvc.perform(post(url(residentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wish.status").value("ACTIVE"))
                .andExpect(jsonPath("$.wish.preferredHospitals[0].address").value("12 Lê Lợi"))
                .andExpect(jsonPath("$.warnings", hasSize(0)));
    }

    @Test
    void FR_023j_httpMapsTheErrorsTo409And422() throws Exception {
        long residentId = newResident();
        long stranger = newFamilyMember();
        assignNurse(nurse, residentId);
        user.set(nurse.user());

        mvc.perform(post(url(residentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().replace(
                                        "\"askFamilyFirst\":false",
                                        "\"askFamilyFirst\":true,\"notifyFamilyMemberIds\":[" + stranger + "]")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_FAMILY_MEMBER"));

        mvc.perform(post(url(residentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                body().replaceAll("\"preferredHospitals\":\\[[^]]*\\],", "\"preferredHospitals\":[],")))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(post(url(residentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().replace("\"askFamilyFirst\":false", "\"askFamilyFirst\":true")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings[0].code").value("NO_FAMILY_MEMBER_TO_NOTIFY"));
    }
}
