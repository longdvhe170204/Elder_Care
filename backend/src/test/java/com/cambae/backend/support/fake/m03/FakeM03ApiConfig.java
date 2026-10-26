package com.cambae.backend.support.fake.m03;

import static org.mockito.Mockito.mock;

import com.cambae.backend.m03.api.BedAllocationCommandApi;
import com.cambae.backend.m03.api.BedHandoverAlertApi;
import com.cambae.backend.m03.api.BedStatisticsApi;
import com.cambae.backend.m03.api.BedStatusApi;
import com.cambae.backend.m03.api.DayCareCapacityApi;
import com.cambae.backend.m03.api.LocationHistoryApi;
import com.cambae.backend.m03.api.LocationStructureApi;
import com.cambae.backend.m03.api.ResidentLocationApi;
import com.cambae.backend.m03.api.RoomPolicyApi;
import com.cambae.backend.m03.internal.bed.domain.BedAssetCommandPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả các API T003 và cổng T004. Test nhập cấu hình này, đặt kết quả bằng Mockito.doReturn/doAnswer.
 * Lời gọi chưa đặt kịch bản báo lỗi, tránh cho lệnh giả âm thầm thành công khi chưa có bản thật.
 */
@TestConfiguration
public class FakeM03ApiConfig {
    private static <T> T unconfigured(Class<T> type) {
        return mock(type, invocation -> {
            throw new IllegalStateException("Test phải đặt kịch bản cho " + type.getSimpleName() + "."
                    + invocation.getMethod().getName());
        });
    }

    @Bean
    @Primary
    ResidentLocationApi residentLocationApi() {
        return unconfigured(ResidentLocationApi.class);
    }

    @Bean
    @Primary
    LocationHistoryApi locationHistoryApi() {
        return unconfigured(LocationHistoryApi.class);
    }

    @Bean
    @Primary
    BedAllocationCommandApi bedAllocationCommandApi() {
        return unconfigured(BedAllocationCommandApi.class);
    }

    @Bean
    @Primary
    DayCareCapacityApi dayCareCapacityApi() {
        return unconfigured(DayCareCapacityApi.class);
    }

    @Bean
    @Primary
    LocationStructureApi locationStructureApi() {
        return unconfigured(LocationStructureApi.class);
    }

    @Bean
    @Primary
    RoomPolicyApi roomPolicyApi() {
        return unconfigured(RoomPolicyApi.class);
    }

    @Bean
    @Primary
    BedStatusApi bedStatusApi() {
        return unconfigured(BedStatusApi.class);
    }

    @Bean
    @Primary
    BedStatisticsApi bedStatisticsApi() {
        return unconfigured(BedStatisticsApi.class);
    }

    @Bean
    @Primary
    BedHandoverAlertApi bedHandoverAlertApi() {
        return unconfigured(BedHandoverAlertApi.class);
    }

    @Bean
    @Primary
    BedAssetCommandPort bedAssetCommandPort() {
        return unconfigured(BedAssetCommandPort.class);
    }
}
