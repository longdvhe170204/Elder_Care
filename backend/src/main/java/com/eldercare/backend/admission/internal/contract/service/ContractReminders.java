package com.eldercare.backend.admission.internal.contract.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.api.events.M02Reminder;
import com.eldercare.backend.admission.internal.contract.domain.ExpiringContract;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.admission.internal.transition.service.ReminderLog;
import com.eldercare.backend.admission.internal.transition.service.ReminderRule;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Nhắc Hành chính về hợp đồng (FR-026, FR-029, BR-M02-05; mức Nhẹ, không có bản ghi nghiệp vụ nào được tạo): hợp đồng dài
 * hạn và bán trú được nhắc mỗi ngày từ CFG-M02-04 ngày trước tới ngày kết thúc ({@code CONTRACT_EXPIRING}); hợp đồng ngắn
 * ngày được nhắc đúng vào ngày kết thúc để tạo Kết thúc lưu trú hoặc Thay đổi lưu trú ({@code SHORT_TERM_CONTRACT}); hợp
 * đồng quá hạn được nhắc mỗi ngày ({@code CONTRACT_OVERDUE}). Phần "chưa có hồ sơ kết thúc hay yêu cầu đổi thời gian đang
 * mở" của hợp đồng ngắn ngày bổ sung khi có bảng của hai chức năng đó.
 *
 * <p>Demo 2026-11-25 (góp ý nhóm, E làm, chờ B, A duyệt): nội dung nêu người cao tuổi, ngày hết hạn và việc cần làm;
 * người đại diện được báo một lần ở ngày đầu của khoảng nhắc (hợp đồng dài hạn, bán trú), ngày kết thúc (ngắn ngày) và
 * ngày đầu quá hạn, không lặp mỗi ngày.
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
    private final ResidentApi residents;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    ContractReminders(
            ContractStore store, ParameterApi parameters, ReminderLog log, BusinessTime time, ResidentApi residents) {
        this.store = store;
        this.parameters = parameters;
        this.log = log;
        this.time = time;
        this.residents = residents;
    }

    @Override
    public int remind(LocalDate day) {
        long windowDays = parameters.read(ParameterCode.CFG_M02_04).asDuration().toDays();
        Instant now = time.now();
        int sent = 0;
        for (ExpiringContract contract : store.activeWithEndDate()) {
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
                log.send(reminder(type, contract, day, windowDays, now));
                sent++;
            }
        }
        return sent;
    }

    private M02Reminder reminder(String type, ExpiringContract contract, LocalDate day, long windowDays, Instant now) {
        String name = residents.identity(contract.residentId()).fullName();
        LocalDate end = contract.currentEndDate();
        String endText = end.format(DATE);
        String detail;
        String family = null;
        switch (type) {
            case OVERDUE -> {
                long days = ChronoUnit.DAYS.between(end, day);
                detail = "Hợp đồng của " + name + " đã quá hạn " + days + " ngày (hết hạn ngày " + endText
                        + "). Lập hồ sơ kết thúc lưu trú hoặc điều chỉnh hợp đồng.";
                if (day.equals(contract.overdueSince()))
                    family = "Hợp đồng lưu trú của " + name + " đã hết hạn ngày " + endText
                            + ". Vui lòng liên hệ viện để gia hạn hoặc làm thủ tục kết thúc lưu trú.";
            }
            case SHORT_TERM -> {
                detail = "Hợp đồng ngắn ngày của " + name + " hết hạn hôm nay (" + endText
                        + "). Lập hồ sơ kết thúc lưu trú hoặc điều chỉnh hợp đồng.";
                family = "Hợp đồng lưu trú ngắn ngày của " + name + " hết hạn hôm nay (" + endText
                        + "). Vui lòng liên hệ viện để gia hạn hoặc làm thủ tục kết thúc lưu trú.";
            }
            default -> {
                long left = ChronoUnit.DAYS.between(day, end);
                detail = "Hợp đồng của " + name + " hết hạn ngày " + endText + " (còn " + left
                        + " ngày). Lập hồ sơ kết thúc lưu trú hoặc điều chỉnh hợp đồng.";
                if (day.equals(end.minusDays(windowDays)))
                    family = "Hợp đồng lưu trú của " + name + " sẽ hết hạn ngày " + endText + " (còn " + left
                            + " ngày). Vui lòng liên hệ viện để gia hạn hoặc làm thủ tục kết thúc lưu trú.";
            }
        }
        return new M02Reminder(type, contract.id(), day, now, contract.residentId(), detail, family);
    }
}
