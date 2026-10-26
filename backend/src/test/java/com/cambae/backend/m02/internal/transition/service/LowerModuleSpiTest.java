package com.cambae.backend.m02.internal.transition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cambae.backend.m01.api.ResidentStatusApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusPeriod;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 004 T010 (contracts mục 1.1): các SPI mà M02 cài cho module thấp hơn. Thời điểm trạng thái cuối lấy từ lịch sử trạng
 * thái của 001; SPI lượt vắng còn là khung trả rỗng tới khi có bảng của user story tương ứng. SPI hợp đồng ({@code ContractApi}) đã có
 * bản thật ở Phase 5, kiểm ở {@code BR_M02_09_ContentAtDateIT}; SPI yêu cầu đổi mức đã có bản thật ở Phase 7, kiểm ở {@code
 * CareLevelChangeRequestIT}.
 */
class LowerModuleSpiTest {

    private static final Set<ResidentStatus> FINAL =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    @Test
    void FR_049_finalStatusAt_isWhenTheResidentEnteredAFinalStatus() {
        ResidentStatusApi statuses = mock(ResidentStatusApi.class);
        Instant deceasedAt = Instant.parse("2030-03-04T15:00:00Z");
        when(statuses.statusPeriods(eq(7L), eq(FINAL), any(), any()))
                .thenReturn(List.of(new StatusPeriod(ResidentStatus.DECEASED, deceasedAt, Optional.empty())));

        assertThat(new ResidencyEndLookup(statuses).finalStatusAt(7L)).contains(deceasedAt);
    }

    @Test
    void FR_049_finalStatusAt_isEmptyWhileTheResidentIsNotInAFinalStatus() {
        ResidentStatusApi statuses = mock(ResidentStatusApi.class);
        when(statuses.statusPeriods(eq(8L), eq(FINAL), any(), any())).thenReturn(List.of());

        assertThat(new ResidencyEndLookup(statuses).finalStatusAt(8L)).isEmpty();
    }

    @Test
    void FR_048_skeletonAbsenceSpi_returnsEmptyUntilItsStoryAddsTables() {
        assertThat(new AbsenceLookup().open(1L)).isEmpty();
    }
}
