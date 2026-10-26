package com.cambae.backend.m02.internal.change.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m02.internal.change.domain.ResidencyChangeContent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 004 T075 (US5 #6, #9; FR-043, Q-338; research T-06): hàm thuần xác định "thay đổi nhỏ" từ nội dung yêu cầu, ảnh hưởng
 * chi phí và nội dung hợp đồng hiện hành: chỉ loại đổi dịch vụ, mọi dịch vụ thêm hoặc bớt thuộc gói, ảnh hưởng chi phí 0.
 */
class IsMinorChangeTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Hợp đồng hiện hành: dịch vụ 11 (chăm sóc, thuộc gói), 12 (suất ăn, thuộc gói), 13 (đưa đi khám, ngoài gói). */
    private static final JsonNode CURRENT = JSON.readTree("""
            {"services": [
              {"serviceId": 11, "inPackage": true, "quantity": 1},
              {"serviceId": 12, "inPackage": true, "quantity": 1},
              {"serviceId": 13, "inPackage": false, "quantity": 1}]}""");

    private static final JsonNode NO_COST = JSON.readTree("[]");

    private static ResidencyChangeContent content(String changeTypes, String newValue) {
        return ResidencyChangeContent.parse(JSON.readTree("""
                {"contractId": 1, "changeTypes": %s, "newValue": %s}""".formatted(changeTypes, newValue)));
    }

    private static JsonNode cost(String items) {
        return JSON.readTree(items);
    }

    @Test
    void addingAnInPackageService_withNoCostImpact_isMinor() {
        var change = content("[\"SERVICE\"]", """
                {"services": [{"serviceId": 20, "action": "ADD", "inPackage": true}]}""");

        assertThat(IsMinorChange.test(change, NO_COST, CURRENT)).isTrue();
    }

    @Test
    void removingAnInPackageService_isMinor() {
        var change = content("[\"SERVICE\"]", """
                {"services": [{"serviceId": 12, "action": "REMOVE"}]}""");

        assertThat(IsMinorChange.test(change, NO_COST, CURRENT)).isTrue();
    }

    @Test
    void addingAServiceOutsideThePackage_isNotMinor() {
        var change = content("[\"SERVICE\"]", """
                {"services": [{"serviceId": 20, "action": "ADD", "inPackage": false}]}""");

        assertThat(IsMinorChange.test(change, NO_COST, CURRENT)).isFalse();
    }

    @Test
    void removingAServiceThatIsOutsideThePackageInTheCurrentContract_isNotMinor() {
        var change = content("[\"SERVICE\"]", """
                {"services": [{"serviceId": 13, "action": "REMOVE"}]}""");

        assertThat(IsMinorChange.test(change, NO_COST, CURRENT)).isFalse();
    }

    @Test
    void removingAServiceTheCurrentContractDoesNotHave_isNotMinor() {
        var change = content("[\"SERVICE\"]", """
                {"services": [{"serviceId": 99, "action": "REMOVE"}]}""");

        assertThat(IsMinorChange.test(change, NO_COST, CURRENT)).isFalse();
    }

    @Test
    void aCostImpactThatChangesAPrice_isNotMinor() {
        var change = content("[\"SERVICE\"]", """
                {"services": [{"serviceId": 20, "action": "ADD", "inPackage": true}]}""");
        JsonNode cost = cost("""
                [{"item": "SERVICE:20", "previousUnitPrice": 0, "newUnitPrice": 150000}]""");

        assertThat(IsMinorChange.test(change, cost, CURRENT)).isFalse();
    }

    @Test
    void aCostImpactWithEqualPrices_isStillMinor() {
        var change = content("[\"SERVICE\"]", """
                {"services": [{"serviceId": 20, "action": "ADD", "inPackage": true}]}""");
        JsonNode cost = cost("""
                [{"item": "SERVICE:20", "previousUnitPrice": 0, "newUnitPrice": 0}]""");

        assertThat(IsMinorChange.test(change, cost, CURRENT)).isTrue();
    }

    @Test
    void anyOtherChangeType_isNotMinor() {
        var withDuration = content("[\"SERVICE\", \"DURATION\"]", """
                {"services": [{"serviceId": 20, "action": "ADD", "inPackage": true}], "endDate": "2031-01-01"}""");
        var careLevel = content("[\"CARE_LEVEL\"]", """
                {"careLevel": "REGULAR"}""");

        assertThat(IsMinorChange.test(withDuration, NO_COST, CURRENT)).isFalse();
        assertThat(IsMinorChange.test(careLevel, NO_COST, CURRENT)).isFalse();
    }

    @Test
    void aChangeThatAlsoChangesTheDeposit_isNotMinor() {
        var change = content("[\"SERVICE\"]", """
                {"services": [{"serviceId": 20, "action": "ADD", "inPackage": true}], "depositAmount": 1000000}""");

        assertThat(IsMinorChange.test(change, NO_COST, CURRENT)).isFalse();
    }
}
