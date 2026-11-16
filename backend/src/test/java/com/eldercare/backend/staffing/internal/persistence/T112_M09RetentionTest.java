package com.eldercare.backend.staffing.internal.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 000 T112 (FR-051a) cho 008: phân công chăm sóc trỏ tới người cao tuổi ({@code target_type = 'RESIDENT'}) trả qua
 * {@code referencing}; các loại M09 khác không gắn hồ sơ người cao tuổi.
 */
class T112_M09RetentionTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    private M09Retention retention(M09Retention.Kind kind) {
        return new M09Retention(
                kind,
                jdbc,
                mock(RetentionDatabase.class),
                mock(BusinessTime.class),
                mock(AccountApi.class),
                mock(ProfessionalActivityApi.class),
                mock(CatalogApi.class),
                mock(StatusHistoryApi.class),
                mock(RecordEventApi.class),
                mock(SimpleRecordApi.class));
    }

    @Test
    void T112_careAssignmentsOfTheResidentAreReturnedByReferencing() {
        when(jdbc.queryForList(anyString(), eq(Long.class), eq(88L))).thenReturn(List.of(4L, 6L));
        var resident = new DataRef(SharedRowRetention.RESIDENT, 88L);

        assertThat(retention(M09Retention.Kind.ASSIGNMENT).referencing(resident))
                .containsExactlyInAnyOrder(new DataRef("M09.ASSIGNMENT", 4L), new DataRef("M09.ASSIGNMENT", 6L));
        assertThat(retention(M09Retention.Kind.SHIFT).referencing(resident)).isEmpty();
        assertThat(retention(M09Retention.Kind.ASSIGNMENT).referencing(new DataRef("M09.STAFF", 88L)))
                .isEmpty();
    }
}
