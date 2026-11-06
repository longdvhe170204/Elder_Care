package com.eldercare.backend.family.internal.visit.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.family.internal.visit.domain.VisitSlot;
import com.eldercare.backend.incident.api.IsolationApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.spi.ContractApi;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Dữ liệu nguồn của FR-030 (c), (d) khi đăng ký và khi ghi vào (FR-033).
 *
 * <ul>
 *   <li>(c) khoanh vùng theo người cao tuổi qua {@link IsolationApi#activeByResident} của 007 (phòng nội trú, khu nghỉ bán
 *       trú). 007 chưa có bản cài thì không có vùng nào (plan "Ghi chú triển khai" Phase 6); khu tiếp khách chưa có mã khu
 *       cấu hình ở 003 nên chưa kiểm.
 *   <li>(d) trạng thái hiện tại không phải Tạm vắng, Điều trị tại bệnh viện, chưa vào ở hay trạng thái cuối (BR-M10-02;
 *       Hoạt động bên ngoài vẫn nhận); bán trú có lịch đến ngày thăm (004), khung nằm trọn trong giờ có mặt dự kiến, không
 *       báo vắng hay ngày nghỉ (005). Lượt vắng dự kiến chưa có API ở 004 nên chưa kiểm.
 * </ul>
 */
@Component
class VisitPresence {

    private static final Set<ResidentStatus> PRESENT =
            Set.of(ResidentStatus.IN_RESIDENCE, ResidentStatus.OFF_SITE_ACTIVITY);
    private static final Set<AttendanceStatus> NOT_COMING =
            Set.of(AttendanceStatus.REPORTED_ABSENT, AttendanceStatus.HOLIDAY_CANCELLED);

    private final ResidentApi residents;
    private final ObjectProvider<IsolationApi> isolation;
    private final ObjectProvider<ContractApi> contracts;
    private final ObjectProvider<ContractReadApi> contractReads;
    private final ObjectProvider<DayCareAttendanceApi> attendance;

    VisitPresence(
            ResidentApi residents,
            ObjectProvider<IsolationApi> isolation,
            ObjectProvider<ContractApi> contracts,
            ObjectProvider<ContractReadApi> contractReads,
            ObjectProvider<DayCareAttendanceApi> attendance) {
        this.residents = residents;
        this.isolation = isolation;
        this.contracts = contracts;
        this.contractReads = contractReads;
        this.attendance = attendance;
    }

    boolean isolated(long residentId, Instant at) {
        IsolationApi zones = isolation.getIfAvailable();
        return zones != null && !zones.activeByResident(residentId, at).isEmpty();
    }

    /** Người cao tuổi đang chịu vùng {@code zoneId} (FR-037). */
    boolean inZone(long residentId, long zoneId, Instant at) {
        IsolationApi zones = isolation.getIfAvailable();
        return zones != null && zones.activeByResident(residentId, at).stream().anyMatch(z -> z.id() == zoneId);
    }

    boolean present(long residentId, LocalDate date, VisitSlot slot) {
        if (!PRESENT.contains(residents.status(residentId))) {
            return false;
        }
        if (!dayCare(residentId, date)) {
            return true;
        }
        ContractReadApi reads = contractReads.getIfAvailable();
        List<AttendanceScheduleView> schedule = reads == null ? List.of() : reads.attendanceOn(residentId, date);
        boolean withinHours = schedule.stream()
                .anyMatch(s ->
                        !slot.start().isBefore(s.arrivalTime()) && !slot.end().isAfter(s.departureTime()));
        if (!withinHours) {
            return false;
        }
        DayCareAttendanceApi days = attendance.getIfAvailable();
        return days == null
                || days.onDate(residentId, date)
                        .map(a -> !NOT_COMING.contains(a.status()))
                        .orElse(true);
    }

    private boolean dayCare(long residentId, LocalDate date) {
        ContractApi api = contracts.getIfAvailable();
        if (api == null) {
            return false;
        }
        return api.residenceTypeHistory(residentId).stream()
                .filter(p -> !p.from().isAfter(date)
                        && p.to().map(to -> !to.isBefore(date)).orElse(true))
                .anyMatch(p -> p.type() == ResidenceType.DAY_CARE);
    }
}
