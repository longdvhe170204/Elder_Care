package com.eldercare.backend.admission.internal.absence.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.Attendance;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.admission.api.dto.DayCareSessionView;
import com.eldercare.backend.admission.api.spi.DayCareAttendancePort;
import com.eldercare.backend.admission.internal.absence.persistence.AdmissionDayCareStore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Các buổi bán trú có lịch trong khoảng và hệ số từng buổi (FR-060, FR-060a; contracts mục 1.4). Buổi của một ngày được đánh
 * số từ 1 theo giờ đến; hợp đồng không giữ mã buổi riêng nên {@code sessionId} là số thứ tự đó, cũng là giá trị 004 truyền cho
 * {@link DayCareAttendancePort}. Trạng thái có mặt do 005 cung cấp: chưa có bản cài hoặc buổi chưa xác định thì không có hệ số.
 * Ngày khu nghỉ không có hệ số (không tính phí buổi).
 */
@Component
public class DayCareSessions {

    private final ContractReadApi contracts;
    private final AdmissionDayCareStore store;
    private final ParameterApi parameters;
    private final ObjectProvider<DayCareAttendancePort> attendance;

    DayCareSessions(
            ContractReadApi contracts,
            AdmissionDayCareStore store,
            ParameterApi parameters,
            ObjectProvider<DayCareAttendancePort> attendance) {
        this.contracts = contracts;
        this.store = store;
        this.parameters = parameters;
        this.attendance = attendance;
    }

    @Transactional(readOnly = true)
    public List<DayCareSessionView> between(long residentId, LocalDate from, LocalDate to) {
        List<DayCareSessionView> sessions = new ArrayList<>();
        DayCareAttendancePort port = attendance.getIfAvailable();
        JsonNode policy = null;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            List<AttendanceScheduleView> lines = contracts.attendanceOn(residentId, day).stream()
                    .sorted(Comparator.comparing(AttendanceScheduleView::arrivalTime))
                    .toList();
            if (lines.isEmpty()) {
                continue;
            }
            boolean closure = store.activeClosureExists(day);
            for (int i = 0; i < lines.size(); i++) {
                long sessionId = i + 1L;
                Attendance status = closure || port == null
                        ? null
                        : port.status(residentId, day, sessionId).orElse(null);
                Integer coefficient = null;
                if (status != null) {
                    if (policy == null) {
                        policy = parameters.read(ParameterCode.CFG_M02_05).raw();
                    }
                    coefficient = switch (status) {
                        case PRESENT -> 100;
                        case ABSENT_NOTIFIED ->
                            policy.get("dayCareNoticedPercent").asInt();
                        case ABSENT_UNNOTIFIED ->
                            policy.get("dayCareUnnoticedPercent").asInt();
                    };
                }
                sessions.add(new DayCareSessionView(day, sessionId, status, coefficient, closure));
            }
        }
        return sessions;
    }
}
