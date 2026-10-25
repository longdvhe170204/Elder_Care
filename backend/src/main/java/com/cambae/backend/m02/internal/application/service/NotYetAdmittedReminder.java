package com.cambae.backend.m02.internal.application.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.api.dto.ContractView;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.persistence.AdmissionApplicationStore;
import com.cambae.backend.m02.internal.transition.service.ReminderLog;
import com.cambae.backend.m02.internal.transition.service.ReminderRule;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Nhắc hằng ngày "phí đang tính cho ngày chưa vào ở" (FR-076, FR-074): người cao tuổi nội trú có lượt đăng ký còn mở
 * (chưa Hoàn tất tiếp nhận, chưa Hủy tiếp nhận) mà hợp đồng Hiệu lực đã bắt đầu trước hôm nay. Dừng khi lượt đóng hoặc
 * phụ lục đưa ngày bắt đầu ra khỏi quá khứ, vì điều kiện được xét lại mỗi ngày. Mức Nhẹ, chỉ Hành chính; không có mốc
 * báo Quản lý viện riêng (CHK012).
 */
@Component
class NotYetAdmittedReminder implements ReminderRule {

    static final String TYPE = "NOT_YET_ADMITTED";

    private final AdmissionApplicationStore store;
    private final AdmissionContractConditions contracts;
    private final ReminderLog log;
    private final BusinessTime time;

    NotYetAdmittedReminder(
            AdmissionApplicationStore store,
            AdmissionContractConditions contracts,
            ReminderLog log,
            BusinessTime time) {
        this.store = store;
        this.contracts = contracts;
        this.log = log;
        this.time = time;
    }

    @Override
    public int remind(LocalDate day) {
        int sent = 0;
        for (AdmissionApplication application : store.listOpen()) {
            Optional<ContractView> contract = contracts.activeContract(application.residentId());
            if (contract.isPresent() && AdmissionConditionService.isFeeChargedBeforeAdmission(contract.get(), day)) {
                log.send(TYPE, application.id(), day, time.now());
                sent++;
            }
        }
        return sent;
    }
}
