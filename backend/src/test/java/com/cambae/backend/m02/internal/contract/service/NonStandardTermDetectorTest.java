package com.cambae.backend.m02.internal.contract.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.BillingPeriod;
import com.cambae.backend.m02.internal.contract.domain.ContractContent;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ServiceLine;
import com.cambae.backend.m02.internal.contract.domain.NonStandardTerm;
import com.cambae.backend.m02.internal.contract.domain.NonStandardTerm.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 004 T030 (FR-025a; research T-16, T-04): hàm thuần xác định điều khoản khác chuẩn của hợp đồng Nháp: giá khác phiên bản
 * đơn giá tại ngày bắt đầu; dòng ghi đè chính sách phí khi vắng hoặc giữ giường khác bảng CFG-M02-05.
 */
class NonStandardTermDetectorTest {

    /** Giá trị mặc định của CFG-M02-05 (V000_04). */
    private static final JsonNode TABLE = parse("""
            {"rows":[
             {"residencyType":"LONG_TERM","absenceType":"HOME_OR_OUTING","fromDay":1,"toDay":null,"feeRatePercent":100,"holdBed":"YES"},
             {"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":1,"toDay":7,"feeRatePercent":100,"holdBed":"YES"},
             {"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":8,"toDay":30,"feeRatePercent":70,"holdBed":"YES"},
             {"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":31,"toDay":null,"feeRatePercent":"MANAGER_DECIDES","holdBed":"MANAGER_DECIDES"},
             {"residencyType":"SHORT_TERM","absenceType":"ANY","fromDay":1,"toDay":null,"feeRatePercent":"BY_CONTRACT","holdBed":"BY_CONTRACT"},
             {"residencyType":"DAY_CARE","absenceType":"NOTIFIED_IN_ADVANCE","fromDay":null,"toDay":null,"feeRatePercent":0,"holdBed":null},
             {"residencyType":"DAY_CARE","absenceType":"UNANNOUNCED","fromDay":null,"toDay":null,"feeRatePercent":50,"holdBed":null}]}""");

    private static final LongFunction<Optional<BigDecimal>> PRICES =
            id -> id == 1 ? Optional.of(new BigDecimal("5000000")) : Optional.empty();

    private static JsonNode parse(String json) {
        return JsonMapper.builder().build().readTree(json);
    }

    private static ContractContent content(List<ServiceLine> services, List<AbsencePolicyLine> overrides) {
        return new ContractContent(
                ResidenceType.LONG_TERM_RESIDENTIAL,
                LocalDate.of(2026, 11, 1),
                null,
                CareLevel.BASIC,
                101L,
                null,
                BillingPeriod.MONTH,
                services,
                true,
                null,
                null,
                overrides,
                List.of(),
                null);
    }

    private static List<NonStandardTerm> detect(List<ServiceLine> services, List<AbsencePolicyLine> overrides) {
        return NonStandardTermDetector.detect(content(services, overrides), PRICES, TABLE);
    }

    private static ServiceLine service(long id, String customPrice) {
        return new ServiceLine(id, true, BigDecimal.ONE, customPrice == null ? null : new BigDecimal(customPrice));
    }

    private static AbsencePolicyLine policy(
            ResidenceType type, String absence, Integer from, Integer to, String coefficient, String holdBed) {
        return new AbsencePolicyLine(type, absence, from, to, coefficient, holdBed);
    }

    @Test
    void FR_025a_noCustomPriceNoOverride_isStandard() {
        assertThat(detect(List.of(service(1, null)), List.of())).isEmpty();
    }

    @Test
    void FR_025a_customPriceEqualToTheStandard_isNotATerm() {
        assertThat(detect(List.of(service(1, "5000000")), List.of())).isEmpty();
    }

    @Test
    void FR_025a_customPriceDifferentFromTheStandard_isAPriceTerm() {
        List<NonStandardTerm> terms = detect(List.of(service(1, "4500000")), List.of());

        assertThat(terms).singleElement().satisfies(term -> {
            assertThat(term.type()).isEqualTo(Type.PRICE);
            assertThat(term.standardValue()).isEqualTo("5000000");
            assertThat(term.proposedValue()).isEqualTo("4500000");
            assertThat(term.description()).contains("1");
        });
    }

    @Test
    void FR_025a_customPriceForAServiceWithoutAStandardPrice_isAPriceTermWithNoStandard() {
        List<NonStandardTerm> terms = detect(List.of(service(2, "100000")), List.of());

        assertThat(terms).singleElement().satisfies(term -> {
            assertThat(term.type()).isEqualTo(Type.PRICE);
            assertThat(term.standardValue()).isNull();
            assertThat(term.proposedValue()).isEqualTo("100000");
        });
    }

    @Test
    void FR_025a_overridesEqualToTheTable_areStandard() {
        List<AbsencePolicyLine> lines = List.of(
                policy(ResidenceType.LONG_TERM_RESIDENTIAL, "HOME_VISIT", 1, null, "100", "YES"),
                policy(ResidenceType.LONG_TERM_RESIDENTIAL, "FAMILY_OUTING", 1, null, "100", "YES"),
                policy(ResidenceType.LONG_TERM_RESIDENTIAL, "HOSPITAL", 8, 30, "70", "YES"),
                policy(ResidenceType.LONG_TERM_RESIDENTIAL, "HOSPITAL", 31, null, "MANAGER_DECIDES", "MANAGER_DECIDES"),
                policy(ResidenceType.SHORT_TERM_RESIDENTIAL, "HOME_VISIT", 1, null, "PER_CONTRACT", "PER_CONTRACT"),
                policy(ResidenceType.DAY_CARE, "UNANNOUNCED", null, null, "50", null));

        assertThat(detect(List.of(), lines)).isEmpty();
    }

    @Test
    void FR_025a_aDifferentCoefficient_isAnAbsencePolicyTerm() {
        List<NonStandardTerm> terms =
                detect(List.of(), List.of(policy(ResidenceType.LONG_TERM_RESIDENTIAL, "HOSPITAL", 8, 30, "90", "YES")));

        assertThat(terms).singleElement().satisfies(term -> {
            assertThat(term.type()).isEqualTo(Type.ABSENCE_POLICY);
            assertThat(term.standardValue()).isEqualTo("70");
            assertThat(term.proposedValue()).isEqualTo("90");
        });
    }

    @Test
    void FR_025a_aDifferentBedHold_isABedHoldPolicyTerm() {
        List<NonStandardTerm> terms = detect(
                List.of(), List.of(policy(ResidenceType.LONG_TERM_RESIDENTIAL, "HOME_VISIT", 1, null, "100", "NO")));

        assertThat(terms).singleElement().satisfies(term -> {
            assertThat(term.type()).isEqualTo(Type.BED_HOLD_POLICY);
            assertThat(term.standardValue()).isEqualTo("YES");
            assertThat(term.proposedValue()).isEqualTo("NO");
        });
    }

    @Test
    void FR_025a_aLineDifferingInBoth_givesTwoTerms() {
        List<NonStandardTerm> terms = detect(
                List.of(), List.of(policy(ResidenceType.LONG_TERM_RESIDENTIAL, "HOME_VISIT", 1, null, "80", "NO")));

        assertThat(terms).extracting(NonStandardTerm::type).containsExactly(Type.ABSENCE_POLICY, Type.BED_HOLD_POLICY);
    }

    @Test
    void FR_025a_aLineWithNoMatchingTableRow_isAnAbsencePolicyTerm() {
        List<NonStandardTerm> terms = detect(
                List.of(), List.of(policy(ResidenceType.LONG_TERM_RESIDENTIAL, "HOSPITAL", 5, 12, "100", "YES")));

        assertThat(terms).singleElement().satisfies(term -> {
            assertThat(term.type()).isEqualTo(Type.ABSENCE_POLICY);
            assertThat(term.standardValue()).isNull();
        });
    }

    @Test
    void FR_025a_priceAndPolicyTermsAreReportedTogether_pricesFirst() {
        List<NonStandardTerm> terms = detect(
                List.of(service(1, "4000000")),
                List.of(policy(ResidenceType.DAY_CARE, "UNANNOUNCED", null, null, "70", null)));

        assertThat(terms).extracting(NonStandardTerm::type).containsExactly(Type.PRICE, Type.ABSENCE_POLICY);
    }
}
