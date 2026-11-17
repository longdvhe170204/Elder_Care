package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.BedFlagView;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedReportingSnapshot.Flags;
import com.eldercare.backend.roombedallocation.internal.bed.persistence.BedReportingStore;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedReportingService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * T061, module-interfaces 003 mục 1.8: {@code flaggedBeds} (tên logic {@code giuongMangDau}) chỉ trả ba loại dấu của hợp
 * đồng: phòng không còn phù hợp, chờ bác sĩ xác nhận, chưa có giường. Dấu chờ yêu cầu, chờ cùng giá là dấu nhắc của phân
 * bổ, không thuộc hàm này (góp ý review 2026-11-01).
 */
@SuppressWarnings("unchecked")
class T061_FlaggedBedsTest {

    private static final Instant AT = Instant.parse("2026-11-17T03:00:00Z");

    @Test
    void T061_flaggedBedsReturnsOnlyTheContractFlags() {
        var store = mock(BedReportingStore.class);
        var time = mock(BusinessTime.class);
        when(time.now()).thenReturn(AT);
        when(store.flags())
                .thenReturn(List.of(
                        flags(1, 11, Set.of("ROOM_UNSUITABLE", "AWAITING_REQUEST")),
                        flags(2, 12, Set.of("AWAITING_SAME_PRICE")),
                        flags(3, 13, Set.of("AWAITING_DOCTOR_CONFIRMATION"))));
        when(store.withoutBed()).thenReturn(List.of(14L));
        var service = new BedReportingService(
                store, time, mock(ParameterApi.class), mock(ObjectProvider.class), (ObjectProvider<BedAssetPort>)
                        mock(ObjectProvider.class));

        assertThat(service.flaggedBeds(ScopeFilter.FACILITY))
                .containsExactlyInAnyOrder(
                        new BedFlagView(11L, 1001L, Set.of("ROOM_UNSUITABLE")),
                        new BedFlagView(13L, 1003L, Set.of("AWAITING_DOCTOR_CONFIRMATION")),
                        new BedFlagView(14L, null, Set.of("WITHOUT_BED")));
    }

    private static Flags flags(long id, long resident, Set<String> flags) {
        return new Flags(
                id, resident, 1000 + id, 100, 10, AT.minusSeconds(3600), AT.plusSeconds(3600), true, flags, null);
    }
}
