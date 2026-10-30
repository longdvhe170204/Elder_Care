package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m09.handover.internal.domain.CriticalHandoverItem;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CriticalHandoverItemTest {
    @ParameterizedTest
    @CsvSource({
        "ALERT,OPEN,EMERGENCY,true", "INCIDENT,OPEN,MEDIUM,true", "ALERT,OPEN,LOW,false",
        "DOSE,MISSED,,true", "DOSE,REFUSED,,true", "DOSE,LATE,,false",
        "TASK,OVERDUE,MANDATORY,true", "TASK,OVERDUE,NORMAL,false", "TASK,OPEN,MANDATORY,false",
        "VITAL_SIGN,ABNORMAL,,false", "FOLLOW_UP_ISSUE,OPEN,,false"
    })
    void FR_041_onlySpecifiedCurrentSourceStatesRequireNotes(
            String group, String status, String level, boolean expected) {
        assertThat(CriticalHandoverItem.requiresNote(group, status, level)).isEqualTo(expected);
    }
}
