package com.eldercare.backend.administration.parameter;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.parameter.api.dto.ParameterKind;
import com.eldercare.backend.administration.parameter.internal.domain.ParameterValidator;
import com.eldercare.backend.administration.parameter.internal.domain.ParameterValidator.Definition;
import com.eldercare.backend.common.error.InvalidValueException;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * FR-015, Q-315 (000 T074): giá trị mới kiểm theo kiểu, ràng buộc kỹ thuật và ràng buộc có nguồn; CFG-M15-06 phải lớn
 * hơn CFG-M15-05, đổi mã nào cũng kiểm mã kia; sai thì 422 {@code INVALID_VALUE}.
 */
class ParameterValidatorTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final ParameterValidator validator = new ParameterValidator();

    private static JsonNode json(String value) {
        return JSON.readTree(value);
    }

    private static Definition def(String code, ParameterKind kind, String unit, String constraints, String current) {
        return new Definition(code, kind, unit, constraints == null ? null : json(constraints), json(current));
    }

    private static final Definition REMIND =
            def("CFG-M15-05", ParameterKind.DURATION, "HOURS", "{\"min\":1}", "{\"value\":48,\"unit\":\"HOURS\"}");
    private static final Definition ESCALATE = def(
            "CFG-M15-06",
            ParameterKind.DURATION,
            "HOURS",
            "{\"min\":1,\"greaterThan\":\"CFG-M15-05\"}",
            "{\"value\":96,\"unit\":\"HOURS\"}");

    private void valid(Definition target, String value, Definition... others) {
        assertThatCode(() -> validator.validate(target, json(value), List.of(others)))
                .doesNotThrowAnyException();
    }

    private void invalid(Definition target, String value, Definition... others) {
        assertThatThrownBy(() -> validator.validate(target, json(value), List.of(others)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_015_duration_mustUseParameterUnit_andRespectMinimum() {
        Definition alert = def(
                "CFG-M05-01", ParameterKind.DURATION, "MINUTES", "{\"min\":1}", "{\"value\":15,\"unit\":\"MINUTES\"}");

        valid(alert, "{\"value\":20,\"unit\":\"MINUTES\"}");
        invalid(alert, "{\"value\":0,\"unit\":\"MINUTES\"}");
        invalid(alert, "{\"value\":20,\"unit\":\"HOURS\"}");
        invalid(alert, "{\"value\":1.5,\"unit\":\"MINUTES\"}");
        invalid(alert, "20");
        invalid(alert, "null");
    }

    @Test
    void FR_015_integerPercentageMoney_respectTechnicalRanges() {
        Definition count = def("CFG-M01-01", ParameterKind.INTEGER, null, "{\"min\":0}", "3");
        Definition rate = def("CFG-M04-11", ParameterKind.PERCENTAGE, "PERCENT", "{\"min\":0,\"max\":100}", "5");
        Definition money = def("CFG-M11-02", ParameterKind.MONEY, "VND", "{\"min\":0}", "500000");

        valid(count, "0");
        invalid(count, "-1");
        invalid(count, "2.5");
        valid(rate, "12.5");
        invalid(rate, "101");
        invalid(rate, "\"10\"");
        valid(money, "750000");
        invalid(money, "-1");
    }

    @Test
    void FR_015_timeOfDay_andTable_checkShape() {
        Definition meal = def("CFG-M08-04", ParameterKind.TIME_OF_DAY, null, null, "\"07:00\"");
        Definition table = def("CFG-M02-05", ParameterKind.TABLE, null, null, "{\"rows\":[]}");

        valid(meal, "\"07:30\"");
        invalid(meal, "\"25:00\"");
        valid(table, "{\"rows\":[{\"fromDay\":1}]}");
        invalid(table, "\"bảng\"");
        invalid(table, "null");
    }

    @Test
    void FR_015_escalation_mustStayGreaterThanReminder_checkedFromBothSides() {
        valid(ESCALATE, "{\"value\":72,\"unit\":\"HOURS\"}", REMIND);
        invalid(ESCALATE, "{\"value\":20,\"unit\":\"HOURS\"}", REMIND);
        invalid(ESCALATE, "{\"value\":48,\"unit\":\"HOURS\"}", REMIND);

        valid(REMIND, "{\"value\":24,\"unit\":\"HOURS\"}", ESCALATE);
        invalid(REMIND, "{\"value\":100,\"unit\":\"HOURS\"}", ESCALATE);
    }
}
