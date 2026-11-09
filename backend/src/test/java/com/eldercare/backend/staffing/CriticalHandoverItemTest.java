package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.staffing.internal.handover.domain.CriticalHandoverItem;
import com.eldercare.backend.support.TableRows;
import org.junit.jupiter.api.Test;

class CriticalHandoverItemTest {
    @Test
    void FR_041_onlySpecifiedCurrentSourceStatesRequireNotes() {
        // nhóm, trạng thái, mức, cần ghi chú
        TableRows.csv(
                r -> assertThat(CriticalHandoverItem.requiresNote(r[0], r[1], r[2]))
                        .isEqualTo(Boolean.parseBoolean(r[3])),
                "ALERT,OPEN,EMERGENCY,true",
                "INCIDENT,OPEN,MEDIUM,true",
                "ALERT,OPEN,LOW,false",
                "DOSE,MISSED,,true",
                "DOSE,REFUSED,,true",
                "DOSE,LATE,,false",
                "TASK,OVERDUE,MANDATORY,true",
                "TASK,OVERDUE,NORMAL,false",
                "TASK,OPEN,MANDATORY,false",
                "VITAL_SIGN,ABNORMAL,,false",
                "FOLLOW_UP_ISSUE,OPEN,,false");
    }
}
