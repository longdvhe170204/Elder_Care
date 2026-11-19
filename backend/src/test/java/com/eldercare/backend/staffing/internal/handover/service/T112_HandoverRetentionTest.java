package com.eldercare.backend.staffing.internal.handover.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 000 T112 (FR-051a) cho 008: bàn giao có mục hoặc đánh giá của người cao tuổi trả qua {@code referencing}, để hồ sơ
 * không bị xóa trước bàn giao còn giữ.
 */
class T112_HandoverRetentionTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    @SuppressWarnings("unchecked")
    private final HandoverRetention retention = new HandoverRetention(
            jdbc,
            mock(RetentionDatabase.class),
            mock(ObjectProvider.class),
            mock(ParameterApi.class),
            mock(BusinessTime.class));

    @Test
    void T112_handoversMentioningTheResidentAreReturnedByReferencing() {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq(55L), eq(55L))).thenReturn(List.of(9L, 12L));

        assertThat(retention.referencing(new DataRef(SharedRowRetention.RESIDENT, 55L)))
                .containsExactlyInAnyOrder(new DataRef("M09.HANDOVER", 9L), new DataRef("M09.HANDOVER", 12L));
        assertThat(retention.referencing(new DataRef("M09.SHIFT", 55L))).isEmpty();
    }
}
