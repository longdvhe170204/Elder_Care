package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.api.dto.BillingSourceView;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/** Phê duyệt 06/11: chọn đơn vị nguồn cụ thể, giữ đơn vị sử dụng và không nhập giá trị tính phí. */
@SuppressWarnings("unchecked")
class CostSourceSelectionSchemaTest {
    private Map<String, Object> schema(String name) throws Exception {
        Map<String, Object> contract = new Yaml()
                .load(Files.readString(Path.of("..", "specs", "010-incurred-costs", "contracts", "api-010.yaml")));
        return ((Map<String, Map<String, Object>>) ((Map<String, Object>) contract.get("components")).get("schemas"))
                .get(name);
    }

    @Test
    void FR_005_selectedUnitRequiresBothTheUnchangedSourceIdAndBillingUnit() throws Exception {
        var properties = (Map<String, Map<String, Object>>)
                schema("CreateCostsFromEvents").get("properties");
        assertThat(properties).containsKey("sourceUnits");
        var units = properties.get("sourceUnits");
        assertThat(units.get("minItems")).isEqualTo(1);
        assertThat(units.get("uniqueItems")).isEqualTo(true);
        var item = (Map<String, Object>) units.get("items");
        assertThat((List<String>) item.get("required")).containsExactlyInAnyOrder("sourceId", "billingUnit");
        assertThat(((Map<String, Object>) item.get("properties")).keySet())
                .containsExactlyInAnyOrder("sourceId", "billingUnit");
    }

    @Test
    void FR_005_theThreeSelectionModesAreMutuallyExclusive() throws Exception {
        var selection =
                (List<Map<String, Object>>) schema("CreateCostsFromEvents").get("oneOf");
        assertThat(selection).isNotNull().hasSize(3);
        assertThat(selection)
                .extracting(option -> option.get("required"))
                .containsExactlyInAnyOrder(List.of("sourceIds"), List.of("sourceUnits"), List.of("dateRange"));
    }

    @Test
    void FR_005_billingIdentityAndPhysicalUnitAreBothAvailableToTheCaller() throws Exception {
        var properties = (Map<String, Object>) schema("BillingSource").get("properties");
        assertThat(properties).containsKeys("unit", "billingUnit");
        assertThat(Arrays.stream(BillingSourceView.class.getRecordComponents()).map(component -> component.getName()))
                .contains("unit", "billingUnit");
    }

    @Test
    void FR_013_sourceSelectionStillDoesNotAcceptQuantityPriceFactorOrAmount() throws Exception {
        var properties = (Map<String, Object>) schema("CreateCostsFromEvents").get("properties");
        assertThat(properties).doesNotContainKeys("quantity", "unit", "unitPrice", "factor", "amount");
    }
}
