package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.api.dto.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.yaml.snakeyaml.Yaml;

/** Kiểu dữ liệu liên module lấy trường đã mô tả, không tự thêm trường HTTP. */
class CostViewSchemaTest {
    @ParameterizedTest
    @MethodSource("views")
    @SuppressWarnings("unchecked")
    void FR_037_publicViewFieldsMatchTheApprovedSchema(Class<?> view, String schemaName) throws Exception {
        var spec = (Map<String, Object>) new Yaml()
                .load(Files.readString(Path.of("..", "specs", "010-incurred-costs", "contracts", "api-010.yaml")));
        var components = (Map<String, Object>) spec.get("components");
        var schemas = (Map<String, Map<String, Object>>) components.get("schemas");
        var properties = (Map<String, Object>) schemas.get(schemaName).get("properties");
        assertThat(Arrays.stream(view.getRecordComponents()).map(c -> c.getName()))
                .containsExactlyInAnyOrderElementsOf(properties.keySet());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> views() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(CostItemView.class, "CostItem"),
                org.junit.jupiter.params.provider.Arguments.of(CostStatementView.class, "CostStatement"),
                org.junit.jupiter.params.provider.Arguments.of(BillingSourceView.class, "BillingSource"),
                org.junit.jupiter.params.provider.Arguments.of(
                        ProvisionalCostSummaryView.class, "ProvisionalCostSummary"));
    }
}
