package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.staffing.internal.ratio.domain.StaffingRatioCalculator;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class StaffingRatioCalculatorTest {
    private final Map<CareLevel, BigDecimal> weights = Map.of(
            CareLevel.BASIC, new BigDecimal("1"),
            CareLevel.REGULAR, new BigDecimal("1.5"),
            CareLevel.SPECIAL, new BigDecimal("2.5"));

    @Test
    void BR_M09_02_weightedResidentsAboveMaximum_warns() {
        var residents = new ArrayList<StaffingRatioCalculator.Resident>();
        for (int i = 1; i <= 20; i++) residents.add(new StaffingRatioCalculator.Resident(i, CareLevel.BASIC));
        for (int i = 21; i <= 26; i++) residents.add(new StaffingRatioCalculator.Resident(i, CareLevel.SPECIAL));
        var result = StaffingRatioCalculator.calculate(
                residents,
                List.of(
                        member(1, Role.NURSING_STAFF), member(2, Role.CAREGIVER),
                        member(3, Role.CAREGIVER), member(4, Role.NURSING_STAFF)),
                weights,
                new BigDecimal("8"));
        assertThat(result.totalWeight()).isEqualByComparingTo("35");
        assertThat(result.staffCount()).isEqualTo(4);
        assertThat(result.ratio()).isEqualByComparingTo("8.75");
        assertThat(result.meetsThreshold()).isFalse();
    }

    @Test
    void BR_M09_14_membersCountWithoutAssignments_oncePerPerson_excludesAbsenceLeaveAndOtherRoles() {
        var members = List.of(
                new StaffingRatioCalculator.Member(1, Set.of(Role.NURSING_STAFF, Role.CAREGIVER), false, false),
                member(1, Role.CAREGIVER),
                member(2, Role.FLOOR_MANAGER),
                member(3, Role.DOCTOR),
                new StaffingRatioCalculator.Member(4, Set.of(Role.CAREGIVER), true, false),
                new StaffingRatioCalculator.Member(5, Set.of(Role.NURSING_STAFF), false, true));
        var result = StaffingRatioCalculator.calculate(
                List.of(
                        new StaffingRatioCalculator.Resident(1, CareLevel.SPECIAL),
                        new StaffingRatioCalculator.Resident(1, CareLevel.SPECIAL)),
                members,
                weights,
                new BigDecimal("2.5"));
        assertThat(result.staffCount()).isEqualTo(1);
        assertThat(result.totalWeight()).isEqualByComparingTo("2.5");
        assertThat(result.meetsThreshold()).isTrue();
    }

    private StaffingRatioCalculator.Member member(long id, Role role) {
        return new StaffingRatioCalculator.Member(id, Set.of(role), false, false);
    }
}
