package com.eldercare.backend.roombedallocation.internal.vehicle.config;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleSchedule;
import com.eldercare.backend.roombedallocation.internal.vehicle.persistence.VehicleScheduleStore;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 019 T004 phần {@code M03.VEHICLE_SCHEDULE} (W-08, W-09): lịch xe toàn viện (không gắn tầng); lưu giữ không gắn hồ sơ
 * theo CFG-M15-04, tính từ lúc lịch khóa (Đã hoàn thành, Đã hủy). Lịch không có trường tài chính.
 */
@Configuration
public class VehicleScheduleGovernance {
    static final String TYPE = VehicleSchedule.KIND.code();

    @Bean
    ScopeAssignmentProvider vehicleScheduleScope(VehicleScheduleStore store) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return TYPE;
            }

            @Override
            public Target assignScope(long id) {
                if (!store.exists(id)) throw new InvalidPermissionDataException(TYPE, "MISSING_RECORD", id);
                return new Target(TYPE, Long.toString(id), null, null);
            }
        };
    }

    @Bean
    RetentionProvider vehicleScheduleRetention(VehicleScheduleStore store, RetentionDatabase database) {
        return new RetentionProvider() {
            @Override
            public String dataType() {
                return TYPE;
            }

            @Override
            public RetentionKind retentionKind() {
                return new RetentionKind.NotResidentLinked();
            }

            @Override
            public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
                return store.lockedBefore(cutoff, batch).stream();
            }

            /** Chuyến đi (014) giữ id chuyến trong lịch, không ngược lại; lịch không bị bản ghi khác trỏ tới. */
            @Override
            public Set<DataRef> referencesTo(long id) {
                return Set.of();
            }

            @Override
            public void delete(Collection<Long> ids) {
                store.delete(database.jdbc(), ids);
            }
        };
    }
}
