package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.m02.api.events.RepresentativeRequestNeedsCompletion;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.FootnoteCondition;
import com.cambae.backend.support.TestUsers.TestUser;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T043 (US5 #8; FR-039, Q-306): người đại diện tạo yêu cầu ở Nháp qua cổng người thân và Hành chính được báo để hoàn
 * thiện; người thân không phải người đại diện không lập được; Bác sĩ chỉ lập yêu cầu đổi mức chăm sóc; hệ thống không tự lập
 * yêu cầu.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(RepresentativeRequestIT.Footnote2.class)
class RepresentativeRequestIT extends ChangeTestBase {

    /**
     * Chú thích ² của Phụ lục 27 (người đại diện) do 012 cài, chưa có (team-plan mục 8, dòng E); không có thì 002 từ chối
     * người thân. Bản thử này chỉ cho qua người thân có quan hệ trong phạm vi, để việc kiểm người đại diện hiệu lực là của
     * chính loại yêu cầu của 004.
     */
    @TestConfiguration
    static class Footnote2 {

        @Bean
        FootnoteCondition representativeFootnote() {
            return new FootnoteCondition() {
                @Override
                public String footnoteCode() {
                    return "2";
                }

                @Override
                public boolean isSatisfied(CurrentUser user, Target target, Instant at) {
                    return user.familyMemberId().isPresent();
                }
            };
        }
    }

    private static final String CARE_LEVEL = "[\"CARE_LEVEL\"]";
    private static final String REGULAR = "{\"careLevel\": \"REGULAR\"}";

    private TestUser relative(long residentId, boolean representative) {
        long familyMemberId = representativeOf(residentId);
        if (!representative) {
            familyMemberId += 1;
        }
        families.relate(familyMemberId, residentId);
        return users.family(familyMemberId);
    }

    @Test
    void US5_8_theRepresentative_createsADraft_andOfficeIsNotified() throws Exception {
        Setup s = activeSetup();
        TestUser representative = relative(s.residentId(), true);
        user.set(representative.user());

        var result = createRaw(
                        "/api/v1/residency-change-requests",
                        request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(10)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
        long requestId = body.get("approvalRequestId").asLong();
        assertThat(requestStatus(requestId)).isEqualTo("DRAFT");
        assertThat(body.get("status").asString()).isEqualTo("DRAFT");
        assertThat(events.stream(RepresentativeRequestNeedsCompletion.class))
                .anyMatch(e -> e.requestId() == requestId && e.residentId() == s.residentId());
    }

    @Test
    void US5_8_aRelativeWhoIsNotTheRepresentative_cannotCreate() throws Exception {
        Setup s = activeSetup();
        user.set(relative(s.residentId(), false).user());

        createRaw(
                        "/api/v1/residency-change-requests",
                        request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(10)))
                .andExpect(status().isForbidden());
    }

    @Test
    void US5_8_aFamilyMemberOfAnotherResident_cannotCreate() throws Exception {
        Setup s = activeSetup();
        Setup other = activeSetup();
        user.set(relative(other.residentId(), true).user());

        // Hợp đồng ngoài phạm vi quan hệ: không lộ sự tồn tại (404, 002 FR-042)
        createRaw(
                        "/api/v1/residency-change-requests",
                        request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(10)))
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_039_aDoctor_createsOnlyCareLevelChanges() throws Exception {
        Setup s = activeSetup();
        long injection = service("INJECTION", "Tiêm", null, 150_000);
        user.set(users.staff(Role.DOCTOR).user());

        createRaw(
                        "/api/v1/residency-change-requests",
                        request(
                                s.contractId(),
                                "[\"SERVICE\"]",
                                "{\"services\": [{\"serviceId\": " + injection + ", \"action\": \"ADD\"}]}",
                                today().plusDays(3)))
                .andExpect(status().isForbidden());
        createRaw(
                        "/api/v1/residency-change-requests",
                        request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(3)))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_028_aDoctor_doesNotSeeTheFinancialCostImpact() throws Exception {
        Setup s = activeSetup();
        user.set(users.staff(Role.DOCTOR).user());

        var result = createRaw(
                        "/api/v1/residency-change-requests",
                        request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(3)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = JSON.readTree(result.getResponse().getContentAsString());
        assertThat(body.has("costImpact")).isFalse();
        assertThat(body.get("hiddenFields")).anyMatch(name -> name.asString().equals("costImpact"));
        assertThat(body.get("newValue").get("careLevel").asString()).isEqualTo("REGULAR");
    }

    @Test
    void FR_039_officeCreatesAnyType_andTheSystemFillsThePreviousValueAndCostImpact() throws Exception {
        Setup s = activeSetup();

        JsonNode created = createChange(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(3)));

        assertThat(created.get("previousValue").get("careLevel").asString()).isEqualTo("BASIC");
        assertThat(created.get("costImpact").get(0).get("previousUnitPrice").asLong())
                .isEqualTo(5_000_000L);
        assertThat(created.get("costImpact").get(0).get("newUnitPrice").asLong())
                .isEqualTo(8_000_000L);
        assertThat(created.get("minorChange").asBoolean()).isFalse();
        assertThat(created.get("contractId").asLong()).isEqualTo(s.contractId());
    }

    @Test
    void FR_039_theSystemNeverCreatesARequestOnItsOwn() throws Exception {
        int before = jdbc.queryForObject(
                "SELECT COUNT(*) FROM approval_request WHERE type = 'M02.RESIDENCY_CHANGE'", Integer.class);

        Setup s = activeSetup();
        noonOf(today().plusDays(30));
        dailyRunner.catchUp();

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM approval_request WHERE type = 'M02.RESIDENCY_CHANGE'", Integer.class))
                .isEqualTo(before);
        assertThat(addenda(s.contractId())).isEmpty();
    }

    @Test
    void FR_039_aReferenceWithoutAnId_is422_andAValidOneIsKept() throws Exception {
        Setup s = activeSetup();
        user.set(office.user());
        ObjectNode invalid = request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(3));
        invalid.putObject("reference").put("type", "ASSESSMENT");

        createRaw("/api/v1/residency-change-requests", invalid)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));

        ObjectNode valid = request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(3));
        valid.putObject("reference").put("type", "ASSESSMENT").put("id", 77);
        JsonNode created = createChange(valid);
        assertThat(created.get("reference").get("type").asString()).isEqualTo("ASSESSMENT");
        assertThat(created.get("reference").get("id").asLong()).isEqualTo(77L);
    }

    @Test
    void FR_040_missingChangeTypeValue_is422() throws Exception {
        Setup s = activeSetup();
        user.set(office.user());

        createRaw("/api/v1/residency-change-requests", request(s.contractId(), CARE_LEVEL, "{}", today().plusDays(3)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));
    }
}
