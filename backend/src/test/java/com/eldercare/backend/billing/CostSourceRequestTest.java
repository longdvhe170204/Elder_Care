package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.web.request.CreateCostsFromEventsRequest;
import com.eldercare.backend.billing.web.request.CreateCostsFromEventsRequest.DateRange;
import com.eldercare.backend.billing.web.request.CreateCostsFromEventsRequest.SourceUnit;
import com.eldercare.backend.support.TableRows;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** T012: kiểm thân lệnh, không suy chọn tất cả hoặc nhận ô nhập tiền. */
class CostSourceRequestTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();
    private static final LocalDate DAY = LocalDate.of(2026, 11, 4);

    @AfterAll
    static void closeValidator() {
        FACTORY.close();
    }

    static Stream<CreateCostsFromEventsRequest> validSelections() {
        return Stream.of(
                new CreateCostsFromEventsRequest(42L, "DOSE", List.of("17"), null, null),
                new CreateCostsFromEventsRequest(
                        42L, "OVERNIGHT_STAY", null, List.of(new SourceUnit("stay-17", "2026-11-04")), null),
                new CreateCostsFromEventsRequest(
                        42L, "RESIDENCY_DAY", null, null, new DateRange(DAY, DAY.plusDays(1))));
    }

    @Test
    void FR_005_eachApprovedSelectionModeIsAccepted() {
        TableRows.each(
                validSelections(),
                request -> assertThat(VALIDATOR.validate(request)).isEmpty());
    }

    static Stream<CreateCostsFromEventsRequest> invalidSelections() {
        return Stream.of(
                new CreateCostsFromEventsRequest(null, "DOSE", List.of("17"), null, null),
                new CreateCostsFromEventsRequest(42L, null, List.of("17"), null, null),
                new CreateCostsFromEventsRequest(42L, "UNKNOWN", List.of("17"), null, null),
                new CreateCostsFromEventsRequest(42L, "DOSE", null, null, null),
                new CreateCostsFromEventsRequest(
                        42L, "DOSE", List.of("17"), List.of(new SourceUnit("17", "drug")), null),
                new CreateCostsFromEventsRequest(42L, "DOSE", List.of("17"), null, new DateRange(DAY, DAY)),
                new CreateCostsFromEventsRequest(42L, "DOSE", List.of("17"), null, new DateRange(null, null)),
                new CreateCostsFromEventsRequest(42L, "DOSE", null, null, new DateRange(DAY, DAY)),
                new CreateCostsFromEventsRequest(42L, "DOSE", List.of(), null, null),
                new CreateCostsFromEventsRequest(42L, "DOSE", List.of("17", "17"), null, null),
                new CreateCostsFromEventsRequest(42L, "DOSE", Arrays.asList((String) null), null, null),
                new CreateCostsFromEventsRequest(42L, "DOSE", null, List.of(), null),
                new CreateCostsFromEventsRequest(
                        42L, "DOSE", null, List.of(new SourceUnit("17", "drug"), new SourceUnit("17", "drug")), null),
                new CreateCostsFromEventsRequest(42L, "DOSE", null, List.of(new SourceUnit(null, "drug")), null),
                new CreateCostsFromEventsRequest(42L, "DOSE", null, List.of(new SourceUnit("17", null)), null),
                new CreateCostsFromEventsRequest(42L, "RESIDENCY_DAY", null, null, new DateRange(null, DAY)),
                new CreateCostsFromEventsRequest(42L, "RESIDENCY_DAY", null, null, new DateRange(DAY, null)),
                new CreateCostsFromEventsRequest(
                        42L, "RESIDENCY_DAY", null, null, new DateRange(DAY, DAY.minusDays(1))));
    }

    @Test
    void FR_005_missingMixedDuplicateOrInvalidSelectionsAreRejected() {
        TableRows.each(
                invalidSelections(),
                request -> assertThat(VALIDATOR.validate(request)).isNotEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void FR_013_validationHelpersDoNotAddFieldsToTheApprovedHttpBody() {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var request = validSelections().findFirst().orElseThrow();
        Map<String, Object> body = mapper.readValue(mapper.writeValueAsString(request), Map.class);
        assertThat(body.keySet())
                .containsExactlyInAnyOrder("residentId", "sourceType", "sourceIds", "sourceUnits", "dateRange");
    }
}
