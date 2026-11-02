package com.eldercare.backend.admission.internal.contract.service;

import com.eldercare.backend.admission.internal.contract.domain.ContractContent.ArrivalLine;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.DayCareCapacityApi;
import com.eldercare.backend.roombedallocation.api.dto.CapacityViolation;
import com.eldercare.backend.roombedallocation.api.dto.DayCareConfiguration;
import com.eldercare.backend.roombedallocation.api.dto.RegistrationCount;
import com.eldercare.backend.roombedallocation.api.dto.SessionDate;
import com.eldercare.backend.roombedallocation.api.spi.DayCareArrivalCounter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiểm sức chứa khu nghỉ bán trú khi lịch đến có hiệu lực ràng buộc (004 research T-10; 003 FR-039, FR-040, research G-11): khóa khu nghỉ,
 * lấy các buổi lịch chiếm, đếm số đăng ký trước và sau, để 003 kết luận. Lịch theo thứ lặp vô hạn nên kiểm trong {@value #HORIZON_DAYS}
 * ngày từ ngày bắt đầu, đủ mọi thứ trong tuần. Chạy trong transaction của lệnh. Chưa có cấu hình khu nghỉ thì 003 báo lỗi của nó.
 */
@Component
public class DayCareCapacityCheck {

    static final int HORIZON_DAYS = 28;

    private final ObjectProvider<DayCareCapacityApi> capacity;
    private final DayCareArrivalCounter counter;

    DayCareCapacityCheck(ObjectProvider<DayCareCapacityApi> capacity, DayCareArrivalCounter counter) {
        this.capacity = capacity;
        this.counter = counter;
    }

    /** Các vi phạm nếu thêm lịch {@code schedule} từ {@code from} (tới {@code to} nếu có); rỗng là đủ chỗ. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<UnmetCondition> violations(List<ArrivalLine> schedule, LocalDate from, LocalDate to) {
        DayCareCapacityApi api = capacity.getIfAvailable();
        if (api == null) {
            return List.of(new UnmetCondition(
                    "DAY_CARE_CAPACITY_CHECK_UNAVAILABLE",
                    "Chưa kiểm được sức chứa khu nghỉ bán trú (feature 003); chưa ghi nhận được (FR-041)"));
        }
        DayCareConfiguration configuration = api.lock();
        Map<SessionDate, Integer> before = counter.countFrom(from, configuration.sessions());
        Set<SessionDate> added = new HashSet<>();
        LocalDate last = from.plusDays(HORIZON_DAYS - 1L);
        for (ArrivalLine line : schedule) {
            if (line.date() != null && line.date().isAfter(last)) {
                last = line.date();
            }
        }
        for (LocalDate day = from; !day.isAfter(last) && (to == null || !day.isAfter(to)); day = day.plusDays(1)) {
            for (ArrivalLine line : schedule) {
                if (matches(line, day)) {
                    added.addAll(api.occupiedSessions(day, line.arrivalTime(), line.departureTime()));
                }
            }
        }
        Map<SessionDate, RegistrationCount> counts = new HashMap<>();
        for (SessionDate session : added) {
            int current = before.getOrDefault(session, 0);
            counts.put(session, new RegistrationCount(current, current + 1));
        }
        List<UnmetCondition> unmet = new ArrayList<>();
        for (CapacityViolation violation : api.checkCapacity(counts)) {
            unmet.add(new UnmetCondition(
                    violation.code(),
                    "Buổi " + violation.session().sessionId() + " ngày "
                            + violation.session().date() + " vượt sức chứa " + violation.capacity() + ", còn "
                            + violation.remaining() + " chỗ (003 FR-039)"));
        }
        return unmet;
    }

    private static boolean matches(ArrivalLine line, LocalDate day) {
        return line.date() != null
                ? line.date().equals(day)
                : line.dayOfWeek() == day.getDayOfWeek().getValue();
    }
}
