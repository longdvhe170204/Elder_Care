package com.eldercare.backend.health.internal.governance.persistence;

import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;

/** Người cao tuổi đã sang trạng thái cuối trước mốc lưu giữ (CFG-M15-03), kèm thời điểm sang trạng thái cuối. */
final class ExpiredResidents {
    static final int CHUNK = 500;
    private static final Set<ResidentStatus> FINAL_STATUSES =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private ExpiredResidents() {}

    static Map<Long, Instant> before(ObjectProvider<ResidentApi> residents, Instant cutoff) {
        ResidentApi api = residents.getIfAvailable();
        Map<Long, Instant> expired = new HashMap<>();
        if (api == null) return expired;
        for (long residentId : api.byStatus(FINAL_STATUSES)) {
            api.finalStatusAt(residentId)
                    .filter(at -> at.isBefore(cutoff))
                    .ifPresent(at -> expired.put(residentId, at));
        }
        return expired;
    }

    /** Danh sách id dạng {@code 1,2,3} theo từng khúc {@link #CHUNK} cho mệnh đề {@code IN}. */
    static List<String> chunks(Iterable<Long> ids) {
        List<Long> all = new java.util.ArrayList<>();
        ids.forEach(all::add);
        List<String> result = new java.util.ArrayList<>();
        for (int from = 0; from < all.size(); from += CHUNK) {
            result.add(all.subList(from, Math.min(from + CHUNK, all.size())).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }
        return result;
    }
}
