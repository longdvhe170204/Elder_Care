package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.events.AddendumApplied;
import com.cambae.backend.m02.api.events.DepositAmountChanged;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T042 (US5 #1, #2; FR-042, FR-044, FR-045, BR-M02-04; quickstart Q11): yêu cầu được duyệt tạo phụ lục Chờ hiệu lực; tới
 * ngày hiệu lực, trong một lần áp dụng, phụ lục thành Đã áp dụng, thêm phiên bản nội dung mới (đơn giá ghi theo ngày hiệu
 * lực), đổi mức chăm sóc hiện hành kèm dịch vụ chăm sóc, đổi ngày kết thúc, phát sự kiện cho 010, 018, 005, 011. Ngày trước
 * ngày hiệu lực vẫn theo nội dung cũ.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_04_ApplyAddendumIT extends ChangeTestBase {

    @Autowired
    CareLevelApi careLevels;

    @Autowired
    com.cambae.backend.m02.api.ContractReadApi reads;

    private JsonNode contentAt(long contractId, LocalDate date) throws Exception {
        user.set(office.user());
        return JSON.readTree(mvc.perform(get("/api/v1/contracts/" + contractId + "/content?date=" + date))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    @Test
    void US5_1_approval_createsAPendingAddendum_andNothingChangesYet() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);

        long requestId = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));

        JsonNode addenda = addenda(s.contractId());
        assertThat(addenda).hasSize(1);
        assertThat(addenda.get(0).get("status").asString()).isEqualTo("PENDING_EFFECTIVE");
        assertThat(addenda.get(0).get("approvalRequestId").asLong()).isEqualTo(requestId);
        assertThat(addenda.get(0).get("effectiveDate").asString()).isEqualTo(effective.toString());
        assertThat(addenda.get(0).get("changeTypes").get(0).asString()).isEqualTo("CARE_LEVEL");
        assertThat(addenda.get(0).get("contentBefore").get("careLevel").asString())
                .isEqualTo("BASIC");
        assertThat(addenda.get(0).get("contentAfter").get("careLevel").asString())
                .isEqualTo("REGULAR");
        assertThat(requestStatus(requestId)).isEqualTo("APPROVED");
        assertThat(versionCount(s.contractId())).isEqualTo(1);
        assertThat(careLevels.current(s.residentId()))
                .as("mức chưa đổi")
                .isNotEqualTo(java.util.Optional.of(CareLevel.REGULAR));
    }

    @Test
    void US5_2_onTheEffectiveDate_careLevelChangeAppliesInOneStep() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);
        long requestId = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));

        applyDueOn(effective);

        assertThat(requestStatus(requestId)).isEqualTo("APPLIED");
        JsonNode addendum = addenda(s.contractId()).get(0);
        assertThat(addendum.get("status").asString()).isEqualTo("APPLIED");
        assertThat(versionCount(s.contractId())).isEqualTo(2);
        JsonNode latest = latestContent(s.contractId());
        assertThat(latest.get("careLevel").asString()).isEqualTo("REGULAR");
        long careServices = 0;
        for (JsonNode service : latest.get("services")) {
            if (service.get("serviceId").asLong() == regularCare) {
                careServices++;
                assertThat(service.get("recordedUnitPrice").asLong()).isEqualTo(8_000_000L);
                assertThat(service.get("inPackage").asBoolean()).isTrue();
            }
            assertThat(service.get("serviceId").asLong())
                    .as("dịch vụ chăm sóc mức cũ bị bỏ")
                    .isNotEqualTo(basicCare);
        }
        assertThat(careServices)
                .as("đúng một dịch vụ chăm sóc của mức mới (Q-297)")
                .isEqualTo(1);
        assertThat(careLevels.current(s.residentId())).contains(CareLevel.REGULAR);
        assertThat(jdbc.queryForObject(
                        "SELECT content_version_id FROM contract_addendum WHERE approval_request_id = ?",
                        Long.class,
                        requestId))
                .isNotNull();
        assertThat(events.stream(AddendumApplied.class))
                .anyMatch(e -> e.contractId() == s.contractId()
                        && e.changeTypes().contains("CARE_LEVEL")
                        && e.effectiveDate().equals(effective));
    }

    @Test
    void FR_044_daysBeforeTheEffectiveDate_keepTheOldContent() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(3);
        service("CARE", "Chăm sóc đặc biệt", "SPECIAL", 12_000_000);
        createAndApprove(request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"SPECIAL\"}", effective));

        applyDueOn(effective);

        assertThat(contentAt(s.contractId(), effective.minusDays(1))
                        .get("content")
                        .get("careLevel")
                        .asString())
                .isEqualTo("BASIC");
        assertThat(contentAt(s.contractId(), effective)
                        .get("content")
                        .get("careLevel")
                        .asString())
                .isEqualTo("SPECIAL");
    }

    @Test
    void US5_serviceChange_addsTheServiceWithThePriceOfTheEffectiveDate() throws Exception {
        Setup s = activeSetup();
        long injection = service("INJECTION", "Tiêm", null, 150_000);
        LocalDate effective = today().plusDays(1);

        createAndApprove(request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + injection + ", \"action\": \"ADD\", \"quantity\": 2}]}",
                effective));
        applyDueOn(effective);

        JsonNode latest = latestContent(s.contractId());
        boolean found = false;
        for (JsonNode service : latest.get("services")) {
            if (service.get("serviceId").asLong() == injection) {
                found = true;
                assertThat(service.get("recordedUnitPrice").asLong()).isEqualTo(150_000L);
                assertThat(service.get("inPackage").asBoolean()).isFalse();
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void US5_durationChange_movesTheCurrentEndDate_andClearsTheOverdueMark() throws Exception {
        long residentId = residentWithRepresentative();
        var body = body(residentId);
        body.put("residenceType", "SHORT_TERM_RESIDENTIAL");
        body.put("endDate", today().plusDays(10).toString());
        long contractId = activeContract(residentId, body);
        jdbc.update("UPDATE contract SET overdue_since = ? WHERE id = ?", today().minusDays(2), contractId);
        LocalDate effective = today().plusDays(1);
        LocalDate newEnd = today().plusDays(40);

        createAndApprove(request(contractId, "[\"DURATION\"]", "{\"endDate\": \"" + newEnd + "\"}", effective));
        applyDueOn(effective);

        assertThat(jdbc.queryForObject(
                        "SELECT current_end_date FROM contract WHERE id = ?", LocalDate.class, contractId))
                .isEqualTo(newEnd);
        assertThat(jdbc.queryForObject("SELECT overdue_since FROM contract WHERE id = ?", LocalDate.class, contractId))
                .isNull();
        assertThat(latestContent(contractId).get("endDate").asString()).isEqualTo(newEnd.toString());
    }

    @Test
    void FR_030_depositChange_publishesDepositAmountChanged() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);

        createAndApprove(request(
                s.contractId(),
                "[\"RESIDENCE_TYPE\"]",
                "{\"residenceType\": \"SHORT_TERM_RESIDENTIAL\", \"endDate\": \"" + today().plusDays(60)
                        + "\", \"depositAmount\": 3000000}",
                effective));
        applyDueOn(effective);

        assertThat(events.stream(DepositAmountChanged.class))
                .anyMatch(e ->
                        e.contractId() == s.contractId() && e.newAmount().compareTo(new BigDecimal("3000000")) == 0);
        assertThat(latestContent(s.contractId()).get("residenceType").asString())
                .isEqualTo("SHORT_TERM_RESIDENTIAL");
    }

    @Test
    void FR_024_readApis_followTheLatestContentVersion() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);
        createAndApprove(request(
                s.contractId(),
                "[\"CARE_LEVEL\", \"RESIDENCE_TYPE\"]",
                "{\"careLevel\": \"REGULAR\", \"residenceType\": \"SHORT_TERM_RESIDENTIAL\", \"endDate\": \""
                        + today().plusDays(60) + "\", \"depositAmount\": 2500000}",
                effective));
        applyDueOn(effective);

        var view = reads.contract(s.contractId()).orElseThrow();
        assertThat(view.careLevel()).isEqualTo(CareLevel.REGULAR);
        assertThat(view.residenceType()).isEqualTo(com.cambae.backend.m01.api.dto.ResidenceType.SHORT_TERM_RESIDENTIAL);
        var deposit = reads.depositAmount(s.contractId()).orElseThrow();
        assertThat(deposit.notRequired()).isFalse();
        assertThat(deposit.amount()).isEqualByComparingTo("2500000");
    }

    @Test
    void FR_030_noDepositChange_publishesNoDepositEvent() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);

        createAndApprove(request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));
        long before = events.stream(DepositAmountChanged.class).count();
        applyDueOn(effective);

        assertThat(events.stream(DepositAmountChanged.class).count()).isEqualTo(before);
    }

    @Test
    void FR_042_effectiveToday_appliesRightAfterApproval() throws Exception {
        Setup s = activeSetup();

        long requestId =
                createAndApprove(request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today()));

        assertThat(requestStatus(requestId)).isEqualTo("APPLIED");
        assertThat(addenda(s.contractId()).get(0).get("status").asString()).isEqualTo("APPLIED");
    }

    @Test
    void NFR_04_runningTheJobAgain_doesNotApplyTheSameAddendumTwice() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);
        createAndApprove(request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));

        applyDueOn(effective);
        applyJob.run();

        assertThat(versionCount(s.contractId())).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM care_level_history WHERE resident_id = ? AND basis_type ="
                                + " 'RESIDENCY_CHANGE_REQUEST'",
                        Integer.class,
                        s.residentId()))
                .isEqualTo(1);
    }

    @Test
    void FR_037a_aMissedDay_isCaughtUp_withTheOriginalEffectiveDate() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(2);
        long requestId = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));

        applyDueOn(effective.plusDays(3));

        assertThat(requestStatus(requestId)).isEqualTo("APPLIED");
        assertThat(jdbc.queryForObject(
                        "SELECT effective_from FROM contract_content_version WHERE contract_id = ? AND sequence_no = 1",
                        LocalDate.class,
                        s.contractId()))
                .as("ngày hiệu lực thực tế, không phải ngày chạy bù")
                .isEqualTo(effective);
        assertThat(contentAt(s.contractId(), effective.plusDays(1))
                        .get("content")
                        .get("careLevel")
                        .asString())
                .isEqualTo("REGULAR");
        assertThat(contentAt(s.contractId(), effective.minusDays(1))
                        .get("content")
                        .get("careLevel")
                        .asString())
                .isEqualTo("BASIC");
    }

    @Test
    void FR_044_twoAddendaOnTheSameDate_bothApply_andTheLaterBuildsOnTheEarlier() throws Exception {
        Setup s = activeSetup();
        long injection = service("INJECTION", "Tiêm", null, 150_000);
        LocalDate effective = today().plusDays(1);
        createAndApprove(request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));
        createAndApprove(request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + injection + ", \"action\": \"ADD\"}]}",
                effective));

        applyDueOn(effective);

        assertThat(versionCount(s.contractId())).isEqualTo(3);
        JsonNode latest = latestContent(s.contractId());
        assertThat(latest.get("careLevel").asString()).isEqualTo("REGULAR");
        assertThat(latest.get("services")).anyMatch(n -> n.get("serviceId").asLong() == injection);
        assertThat(contentAt(s.contractId(), effective).get("version").asInt()).isEqualTo(2);
    }
}
