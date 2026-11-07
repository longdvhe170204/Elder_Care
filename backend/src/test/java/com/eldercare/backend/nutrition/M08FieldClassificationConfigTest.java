package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.nutrition.internal.classification.config.M08FieldClassificationConfig;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** 011 T007, research M-15 (FR-064): trường dị ứng, hạn chế, chế độ ăn trong nhật ký mang nhóm sức khỏe. */
class M08FieldClassificationConfigTest {

    private final Map<String, FieldClassificationProvider> byType = providers().stream()
            .collect(Collectors.toMap(FieldClassificationProvider::entityType, Function.identity()));

    private static List<FieldClassificationProvider> providers() {
        M08FieldClassificationConfig config = new M08FieldClassificationConfig();
        return List.of(
                config.dietClassification(),
                config.dietAssignmentClassification(),
                config.familyFoodClassification(),
                config.servingConfirmationClassification());
    }

    @Test
    void FR_064_dietRestrictions_areHealth() {
        assertThat(byType.get("M08.DIET").fieldGroups()).containsEntry("restrictions", InfoGroup.HEALTH);
    }

    @Test
    void FR_064_dietAssignmentDietRestrictionsAndInstruction_areHealth() {
        Map<String, InfoGroup> groups = byType.get("M08.DIET_ASSIGNMENT").fieldGroups();

        assertThat(groups)
                .containsEntry("dietId", InfoGroup.HEALTH)
                .containsEntry("texture", InfoGroup.HEALTH)
                .containsEntry("personalRestrictions", InfoGroup.HEALTH)
                .containsEntry("personalInstruction", InfoGroup.HEALTH)
                .containsEntry("professionalReason", InfoGroup.HEALTH);
        assertThat(groups).doesNotContainKey("effectiveFrom");
    }

    @Test
    void FR_064_familyFoodIngredientsAndSystemReasons_areHealth() {
        assertThat(byType.get("M08.FAMILY_FOOD").fieldGroups())
                .containsEntry("ingredients", InfoGroup.HEALTH)
                .containsEntry("systemReasons", InfoGroup.HEALTH)
                .doesNotContainKey("description");
    }

    @Test
    void FR_064_servingConfirmationIncident_isHealth() {
        assertThat(byType.get("M08.SERVING_CONFIRMATION").fieldGroups())
                .containsEntry("hasEaten", InfoGroup.HEALTH)
                .containsEntry("incident", InfoGroup.HEALTH);
    }
}
