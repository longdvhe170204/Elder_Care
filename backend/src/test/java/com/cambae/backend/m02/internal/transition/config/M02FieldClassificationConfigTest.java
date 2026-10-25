package com.cambae.backend.m02.internal.transition.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 004 T011 (data-model mục 13; FR-028, FR-069, BR-M02-21): phân loại trường nhật ký của M02. Giá, khoản cọc, dòng ghi đè
 * chính sách vắng là tài chính; nguyên nhân qua đời là sức khỏe; trường khác không khai báo (công khai nội bộ).
 */
class M02FieldClassificationConfigTest {

    private static List<FieldClassificationProvider> classifications() {
        M02FieldClassificationConfig config = new M02FieldClassificationConfig();
        return List.of(
                config.contractClassification(),
                config.addendumClassification(),
                config.residencyChangeClassification(),
                config.deathRecordClassification());
    }

    private final Map<String, FieldClassificationProvider> byType = classifications().stream()
            .collect(Collectors.toMap(FieldClassificationProvider::entityType, Function.identity()));

    @Test
    void FR_028_contractPriceDepositAndPolicyOverrides_areFinance() {
        Map<String, InfoGroup> groups = byType.get("M02.CONTRACT").fieldGroups();

        assertThat(groups)
                .containsEntry("depositAmount", InfoGroup.FINANCE)
                .containsEntry("noDepositRequired", InfoGroup.FINANCE)
                .containsEntry("absencePolicyOverrides", InfoGroup.FINANCE)
                .containsEntry("services", InfoGroup.FINANCE)
                .containsEntry("recordedUnitPrices", InfoGroup.FINANCE);
        assertThat(groups).doesNotContainKey("contractNumber");
    }

    @Test
    void FR_028_addendumAndChangeRequestValues_areFinance() {
        assertThat(byType.get("M02.ADDENDUM").fieldGroups())
                .containsEntry("contentBefore", InfoGroup.FINANCE)
                .containsEntry("contentAfter", InfoGroup.FINANCE);
        assertThat(byType.get("M02.RESIDENCY_CHANGE").fieldGroups())
                .containsEntry("previousValue", InfoGroup.FINANCE)
                .containsEntry("newValue", InfoGroup.FINANCE)
                .containsEntry("costImpact", InfoGroup.FINANCE);
    }

    @Test
    void BR_M02_21_causeOfDeath_isHealth() {
        assertThat(byType.get("M02.DEATH_RECORD").fieldGroups()).containsOnly(Map.entry("cause", InfoGroup.HEALTH));
    }

    @Test
    void everyProvider_belongsToM02() {
        List<String> types = byType.keySet().stream().sorted().toList();

        assertThat(types).allMatch(type -> type.startsWith("M02."));
        assertThat(types).containsExactly("M02.ADDENDUM", "M02.CONTRACT", "M02.DEATH_RECORD", "M02.RESIDENCY_CHANGE");
    }
}
