package com.cambae.backend.m02.internal.absence.service;

import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.ArrivalScheduleView;
import com.cambae.backend.m02.api.dto.Attendance;
import com.cambae.backend.m02.api.dto.ContractContentView;
import com.cambae.backend.m02.api.dto.DayCareSessionView;
import com.cambae.backend.m02.api.spi.DayCareAttendancePort;
import com.cambae.backend.m02.internal.absence.persistence.DayCareStore;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private final DayCareStore store;
    private final ParameterApi parameters;
    private final ObjectProvider<DayCareAttendancePort> attendance;

    DayCareSessions(
            ContractReadApi contracts,
            DayCareStore store,
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
        JsonNode table = null;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            List<ArrivalScheduleView> lines = contracts.arrivalScheduleAtDate(residentId, day).stream()
                    .sorted(Comparator.comparing(ArrivalScheduleView::arrivalTime))
                    .toList();
            if (lines.isEmpty()) {
                continue;
            }
            boolean closure = store.activeClosureExists(day);
            Optional<ContractContentView> content = contracts.contentAtDate(residentId, day);
            for (int i = 0; i < lines.size(); i++) {
                long sessionId = i + 1L;
                Attendance status = closure || port == null
                        ? null
                        : port.status(residentId, day, sessionId).orElse(null);
                Integer coefficient = null;
                if (status != null && content.isPresent()) {
                    if (table == null) {
                        table = parameters.read(ParameterCode.CFG_M02_05).raw();
                    }
                    BigDecimal value = AbsencePolicyResolver.dayCare(
                                    status,
                                    AbsenceDayCalculator.policyLines(
                                            content.get().content()),
                                    table)
                            .coefficient();
                    coefficient = value.intValue();
                }
                sessions.add(new DayCareSessionView(day, sessionId, status, coefficient, closure));
            }
        }
        return sessions;
    }
}
