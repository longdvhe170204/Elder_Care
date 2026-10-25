package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore.Expiring;
import com.cambae.backend.m02.internal.transition.service.ReminderLog;
import com.cambae.backend.m02.internal.transition.service.ReminderRule;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Nhắc Hành chính về hợp đồng (FR-026, FR-029, BR-M02-05; mức Nhẹ, không có bản ghi nghiệp vụ nào được tạo): hợp đồng dài
 * hạn và bán trú được nhắc mỗi ngày từ CFG-M02-04 ngày trước tới ngày kết thúc ({@code CONTRACT_EXPIRING}); hợp đồng ngắn
 * ngày được nhắc đúng vào ngày kết thúc để tạo Kết thúc lưu trú hoặc Thay đổi lưu trú ({@code SHORT_TERM_CONTRACT}); hợp
 * đồng quá hạn được nhắc mỗi ngày ({@code CONTRACT_OVERDUE}). Phần "chưa có hồ sơ kết thúc hay yêu cầu đổi thời gian đang
 * mở" của hợp đồng ngắn ngày bổ sung khi có bảng của hai chức năng đó.
 */
@Component
class ContractReminders implements ReminderRule {

    static final String EXPIRING = "CONTRACT_EXPIRING";
    static final String SHORT_TERM = "SHORT_TERM_CONTRACT";
    static final String OVERDUE = "CONTRACT_OVERDUE";

    private final ContractStore store;
    private final ParameterApi parameters;
    private final ReminderLog log;
    private final BusinessTime time;

    ContractReminders(ContractStore store, ParameterApi parameters, ReminderLog log, BusinessTime time) {
        this.store = store;
        this.parameters = parameters;
        this.log = log;
        this.time = time;
    }

    @Override
    public int remind(LocalDate day) {
        long windowDays = parameters.read(ParameterCode.CFG_M02_04).asDuration().toDays();
        Instant now = time.now();
        int sent = 0;
        for (Expiring contract : store.activeWithEndDate()) {
            LocalDate end = contract.currentEndDate();
            String type = null;
            if (contract.overdueSince() != null) {
                type = OVERDUE;
            } else if (contract.residenceType() == ResidenceType.SHORT_TERM_RESIDENTIAL) {
                type = day.equals(end) ? SHORT_TERM : null;
            } else if (!day.isAfter(end) && !day.isBefore(end.minusDays(windowDays))) {
                type = EXPIRING;
            }
            if (type != null) {
                log.send(type, contract.id(), day, now);
                sent++;
            }
        }
        return sent;
    }
}
