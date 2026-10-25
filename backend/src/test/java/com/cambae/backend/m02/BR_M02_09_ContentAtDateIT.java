package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidenceTypePeriod;
import com.cambae.backend.m01.api.spi.ContractApi;
import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ArrivalScheduleView;
import com.cambae.backend.m02.api.dto.ContractContentView;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.api.dto.DepositAmountView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T031, T035 (US3; FR-024, FR-028, BR-M02-09, FR-029): nội dung hợp đồng tại ngày D là phiên bản có {@code
 * effective_from} lớn nhất không muộn hơn D; phụ lục (US5) chỉ thêm phiên bản, nên test thêm phiên bản bằng SQL. Cùng
 * truy vấn phục vụ {@code ContractReadApi} (003, 005, 010, 012, 017, 018) và {@code ContractApi} (001).
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_09_ContentAtDateIT extends ContractTestBase {

    @Autowired
    ContractReadApi readApi;

    @Autowired
    ContractApi residenceTypes;

    /** Phiên bản {@code sequence} từ {@code from}: sao nội dung phiên bản 0 rồi đổi mức và loại lưu trú. */
    private void addVersion(long contractId, int sequence, LocalDate from, String careLevel, String residenceType)
            throws Exception {
        String zero = jdbc.queryForObject(
                "SELECT content FROM contract_content_version WHERE contract_id = ? AND sequence_no = 0",
                String.class,
                contractId);
        ObjectNode content = (ObjectNode) JSON.readTree(zero);
        content.put("careLevel", careLevel);
        content.put("residenceType", residenceType);
        jdbc.update(
                "INSERT INTO contract_content_version (contract_id, sequence_no, effective_from, content,"
                        + " residence_type, care_level) VALUES (?, ?, ?, ?, ?, ?)",
                contractId,
                sequence,
                from,
                content.toString(),
                residenceType,
                careLevel);
    }

    private JsonNode contentAt(long id, LocalDate date) throws Exception {
        user.set(office.user());
        return JSON.readTree(
                mvc.perform(get("/api/v1/contracts/" + id + "/content").param("date", date.toString()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    /** Hợp đồng Hiệu lực bắt đầu cách hôm nay 100 ngày, kèm hai phiên bản phụ lục giả lập sau 30 và 60 ngày. */
    private record Setup(long residentId, long id, LocalDate start) {}

    private Setup withVersions() throws Exception {
        long residentId = residentWithRepresentative();
        LocalDate start = today().minusDays(100);
        ObjectNode body = body(residentId);
        body.put("startDate", start.toString());
        long id = activeContract(residentId, body);
        addVersion(id, 1, start.plusDays(30), "REGULAR", "LONG_TERM_RESIDENTIAL");
        addVersion(id, 2, start.plusDays(60), "SPECIAL", "SHORT_TERM_RESIDENTIAL");
        return new Setup(residentId, id, start);
    }

    @Test
    void BR_M02_09_contentAtDate_isTheLatestVersionNotLaterThanTheDate() throws Exception {
        Setup s = withVersions();

        assertThat(contentAt(s.id(), s.start()).get("version").asInt()).isZero();
        JsonNode beforeFirstAddendum = contentAt(s.id(), s.start().plusDays(29));
        assertThat(beforeFirstAddendum.get("version").asInt()).isZero();
        assertThat(beforeFirstAddendum.get("content").get("careLevel").asString())
                .isEqualTo("BASIC");

        JsonNode onFirstAddendum = contentAt(s.id(), s.start().plusDays(30));
        assertThat(onFirstAddendum.get("version").asInt()).isEqualTo(1);
        assertThat(onFirstAddendum.get("effectiveFrom").asString())
                .isEqualTo(s.start().plusDays(30).toString());
        assertThat(onFirstAddendum.get("content").get("careLevel").asString()).isEqualTo("REGULAR");

        assertThat(contentAt(s.id(), s.start().plusDays(59)).get("version").asInt())
                .isEqualTo(1);
        JsonNode last = contentAt(s.id(), s.start().plusDays(60));
        assertThat(last.get("version").asInt()).isEqualTo(2);
        assertThat(last.get("content").get("residenceType").asString()).isEqualTo("SHORT_TERM_RESIDENTIAL");
    }

    @Test
    void BR_M02_09_contentCarriesTheRecordedUnitPrices() throws Exception {
        Setup s = withVersions();

        JsonNode content = contentAt(s.id(), s.start());

        assertThat(content.get("recordedUnitPrices")).hasSize(1);
        assertThat(content.get("recordedUnitPrices").get(0).get("serviceId").asLong())
                .isEqualTo(basicCare);
        assertThat(content.get("recordedUnitPrices").get(0).get("unitPrice").asLong())
                .isEqualTo(5_000_000L);
        assertThat(content.get("content").has("recordedUnitPrice")).isFalse();
    }

    @Test
    void BR_M02_09_beforeTheStartDate_orOnADraft_isNotFound() throws Exception {
        Setup s = withVersions();
        long draft = createDraft(residentWithRepresentative());

        user.set(office.user());
        mvc.perform(get("/api/v1/contracts/" + s.id() + "/content")
                        .param("date", s.start().minusDays(1).toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/contracts/" + draft + "/content").param("date", today().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/contracts/" + s.id() + "/content")).andExpect(status().isBadRequest());
    }

    @Test
    void FR_029_afterTheEndDate_theLastTermsStillApply() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        body.put("startDate", today().minusDays(60).toString());
        body.put("endDate", today().minusDays(30).toString());
        long id = activeContract(residentId, body);

        JsonNode content = contentAt(id, today());

        assertThat(content.get("version").asInt()).isZero();
        assertThat(readApi.contentAtDate(residentId, today())).isPresent();
    }

    @Test
    void FR_024_readApi_contentAtDate_matchesTheEndpoint() throws Exception {
        Setup s = withVersions();

        Optional<ContractContentView> at45 =
                readApi.contentAtDate(s.residentId(), s.start().plusDays(45));
        assertThat(at45).isPresent();
        assertThat(at45.get().contractId()).isEqualTo(s.id());
        assertThat(at45.get().version()).isEqualTo(1);
        assertThat(at45.get().effectiveFrom()).isEqualTo(s.start().plusDays(30));
        assertThat(at45.get().addendumId()).isNull();
        assertThat(at45.get().recordedUnitPrices()).containsEntry(basicCare, new BigDecimal("5000000"));

        assertThat(readApi.contentAtDate(s.residentId(), s.start().minusDays(1)))
                .isEmpty();
        assertThat(readApi.contentAtDate(residentWithRepresentative(), today())).isEmpty();
    }

    @Test
    void FR_024_readApi_contract_ofResident_depositAmount() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        body.remove("noDepositRequired");
        body.put("depositAmount", 15_000_000);
        long id = activeContract(residentId, body);

        Optional<ContractView> view = readApi.contract(id);
        assertThat(view).isPresent();
        assertThat(view.get().residentId()).isEqualTo(residentId);
        assertThat(view.get().status().name()).isEqualTo("ACTIVE");
        assertThat(view.get().careLevel()).isEqualTo(CareLevel.BASIC);
        assertThat(view.get().contractNumber()).startsWith("HD-");
        assertThat(readApi.ofResident(residentId)).extracting(ContractView::id).containsExactly(id);
        assertThat(readApi.depositAmount(id)).contains(new DepositAmountView(id, false, new BigDecimal("15000000")));
        assertThat(readApi.contract(987_654_321L)).isEmpty();
    }

    @Test
    void FR_030_depositAmount_notRequired_isFlagged() throws Exception {
        long residentId = residentWithRepresentative();
        long id = activeContract(residentId, body(residentId));

        assertThat(readApi.depositAmount(id)).contains(new DepositAmountView(id, true, BigDecimal.ZERO));
    }

    @Test
    void FR_019_arrivalScheduleAtDate_listsTheWeeklyAndTheDatedLines_ofAnActiveContract() throws Exception {
        long residentId = residentWithRepresentative();
        LocalDate monday = today().plusDays((8 - today().getDayOfWeek().getValue()) % 7 + 7);
        ObjectNode body = body(residentId);
        body.put("residenceType", "DAY_CARE");
        body.put("residencyFeeBillingPeriod", "SESSION");
        body.remove("roomTypeId");
        body.put("startDate", today().toString());
        var schedule = body.putArray("arrivalSchedule");
        schedule.addObject().put("dayOfWeek", 1).put("arrivalTime", "07:30").put("departureTime", "16:30");
        schedule.addObject()
                .put("date", monday.plusDays(2).toString())
                .put("arrivalTime", "13:00")
                .put("departureTime", "17:00");
        long id = activeContract(residentId, body);

        List<ArrivalScheduleView> mondayLines = readApi.arrivalScheduleAtDate(residentId, monday);
        assertThat(mondayLines).containsExactly(new ArrivalScheduleView(id, LocalTime.of(7, 30), LocalTime.of(16, 30)));
        assertThat(readApi.arrivalScheduleAtDate(residentId, monday.plusDays(2)))
                .containsExactly(new ArrivalScheduleView(id, LocalTime.of(13, 0), LocalTime.of(17, 0)));
        assertThat(readApi.arrivalScheduleAtDate(residentId, monday.plusDays(1)))
                .isEmpty();
        assertThat(readApi.arrivalScheduleAtDate(residentId, today().minusDays(1)))
                .isEmpty();
    }

    @Test
    void FR_009_residenceTypeFor001_followsTheContentVersions() throws Exception {
        Setup s = withVersions();

        assertThat(residenceTypes.currentResidenceType(s.residentId(), s.start().plusDays(10)))
                .contains(ResidenceType.LONG_TERM_RESIDENTIAL);
        assertThat(residenceTypes.currentResidenceType(s.residentId(), s.start().plusDays(70)))
                .contains(ResidenceType.SHORT_TERM_RESIDENTIAL);
        assertThat(residenceTypes.currentResidenceType(s.residentId(), s.start().minusDays(1)))
                .isEmpty();

        List<ResidenceTypePeriod> history = residenceTypes.residenceTypeHistory(s.residentId());
        assertThat(history)
                .extracting(ResidenceTypePeriod::type, ResidenceTypePeriod::from)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(ResidenceType.LONG_TERM_RESIDENTIAL, s.start()),
                        org.assertj.core.groups.Tuple.tuple(
                                ResidenceType.LONG_TERM_RESIDENTIAL, s.start().plusDays(30)),
                        org.assertj.core.groups.Tuple.tuple(
                                ResidenceType.SHORT_TERM_RESIDENTIAL, s.start().plusDays(60)));
        assertThat(history.get(0).to()).contains(s.start().plusDays(29));
        assertThat(history.get(2).to()).isEmpty();
    }

    @Test
    void FR_028_onlyTheOfficeManagerAndAccountantMayView_otherRolesAre403() throws Exception {
        long residentId = residentWithRepresentative();
        long id = activeContract(residentId, body(residentId));
        assignNurse(nurse, residentId);

        for (var viewer : List.of(office, manager, users.staff(com.cambae.backend.common.Role.ACCOUNTANT))) {
            user.set(viewer.user());
            mvc.perform(get("/api/v1/contracts/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.depositAmount").doesNotExist())
                    .andExpect(jsonPath("$.noDepositRequired").value(true))
                    .andExpect(jsonPath("$.hiddenFields.length()").value(0));
        }
        for (var viewer : List.of(nurse, doctor)) {
            user.set(viewer.user());
            mvc.perform(get("/api/v1/contracts/" + id)).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/contracts/" + id + "/content").param("date", today().toString()))
                    .andExpect(status().isForbidden());
        }
    }
}
