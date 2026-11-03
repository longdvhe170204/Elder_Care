package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.care.internal.governance.domain.CareRetentionType;
import com.eldercare.backend.care.internal.governance.persistence.CareGovernanceStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** T035/R-15: hai định danh logic cùng một dòng phải được duyệt cả chuỗi lưu giữ. */
class FR_036_CareResultGovernanceIT extends CareResultTestBase {
    @Test
    void FR_050_unlinkedResultRetentionUsesBusinessTimezone() throws Exception {
        var planned = jdbc.queryForObject(
                        "SELECT planned_at FROM care_task WHERE id=?", java.time.LocalDateTime.class, task)
                .atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
                .toInstant();
        jdbc.update(
                "UPDATE care_task SET resident_id=NULL,location_type='FLOOR',location_id=? WHERE id=?", floor, task);
        record("{\"numericValue\":200}").andExpect(status().isOk());
        assertThat(governance.rows(CareRetentionType.LOCATION_CARE_RESULT))
                .filteredOn(r -> r.id() == task)
                .singleElement()
                .satisfies(r -> assertThat(r.plannedAt()).isEqualTo(planned));
    }

    @Autowired
    CareGovernanceStore governance;

    @Test
    void FR_036_resultScopeReadsTheMergedTaskRow() throws Exception {
        record("{\"numericValue\":200}").andExpect(status().isOk());
        var target = governance.scope("M04.CARE_RESULT", task);
        assertThat(target.residentId()).isEqualTo(resident);
        assertThat(target.floorAreaId()).isEqualTo(floor);
    }

    @Test
    void FR_036_retentionGraphIncludesBothLogicalIdentities() throws Exception {
        record("{\"numericValue\":200}").andExpect(status().isOk());
        assertThat(governance.references(CareRetentionType.TASK, task)).contains(new DataRef("M04.CARE_RESULT", task));
        assertThat(governance.references(CareRetentionType.CARE_RESULT, task)).contains(new DataRef("M04.TASK", task));
        assertThat(governance.rows(CareRetentionType.CARE_RESULT)).anyMatch(r -> r.id() == task);
    }
}
