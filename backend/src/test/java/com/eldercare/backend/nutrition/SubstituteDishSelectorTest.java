package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.nutrition.internal.conflict.domain.SubstituteDishSelector;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 011 T005, research M-06, FR-036: chọn món thay thế đầu tiên không xung đột. */
class SubstituteDishSelectorTest {

    @Test
    void FR_036_firstNonConflictingSubstituteIsChosen() {
        Set<Long> conflicting = Set.of(10L, 11L);

        assertThat(SubstituteDishSelector.choose(List.of(10L, 11L, 12L, 13L), conflicting::contains))
                .contains(12L);
    }

    @Test
    void FR_036_priorityOrderIsKept() {
        assertThat(SubstituteDishSelector.choose(List.of(20L, 21L), id -> false))
                .contains(20L);
    }

    @Test
    void FR_036_allSubstitutesConflict_returnsEmptyForMissingSubstituteFlag() {
        assertThat(SubstituteDishSelector.choose(List.of(10L, 11L), id -> true)).isEmpty();
    }

    @Test
    void FR_036_noSubstituteList_returnsEmpty() {
        assertThat(SubstituteDishSelector.choose(List.of(), id -> false)).isEmpty();
    }
}
