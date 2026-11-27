package com.eldercare.backend.admission.internal.ending.service;

import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.internal.absence.service.AbsenceNames;
import com.eldercare.backend.admission.internal.ending.domain.RecipientType;
import com.eldercare.backend.admission.internal.ending.domain.ResidencyEnding;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Nội dung báo người đại diện khi kết thúc lưu trú hoàn tất (demo 2026-11-26, E làm, chờ B duyệt): ngày kết thúc, lý do,
 * bàn giao (người nhận, thời điểm, nhân viên) và nơi xem chi phí, quyết toán.
 */
@Component
class EndingFamilyNotice {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Map<EndCase, String> CASES = Map.of(
            EndCase.CONTRACT_EXPIRED, "hết hạn hợp đồng",
            EndCase.CONTRACT_TERMINATION, "chấm dứt hợp đồng",
            EndCase.DISCHARGE, "xuất viện",
            EndCase.TRANSFER_TO_ANOTHER_FACILITY, "chuyển cơ sở khác",
            EndCase.DEATH, "qua đời");

    private final ResidentApi residents;
    private final EndingHandovers handovers;
    private final AbsenceNames names;

    EndingFamilyNotice(ResidentApi residents, EndingHandovers handovers, AbsenceNames names) {
        this.residents = residents;
        this.handovers = handovers;
        this.names = names;
    }

    String text(ResidencyEnding ending, LocalDate endDate) {
        String name = residents.identity(ending.residentId()).fullName();
        StringBuilder text = new StringBuilder(name)
                .append(" đã kết thúc lưu trú tại viện ngày ")
                .append(endDate.format(DATE))
                .append(" (")
                .append(CASES.getOrDefault(ending.endCase(), "kết thúc lưu trú"))
                .append(").");
        handovers
                .latest(ending.id())
                .ifPresent(h -> text.append(" Bàn giao cho ")
                        .append(
                                h.recipientType() == RecipientType.FAMILY_MEMBER && h.familyMemberId() != null
                                        ? names.familyMember(h.familyMemberId())
                                        : h.facilityName())
                        .append(" lúc ")
                        .append(h.handoverAt().atZone(BusinessTime.ZONE).format(DATE_TIME))
                        .append(", nhân viên bàn giao ")
                        .append(names.staff(h.handoverStaffId()))
                        .append('.'));
        text.append(" Bảng chi phí kỳ cuối, bảng quyết toán xem ở mục Chi phí và Chứng từ trên ứng dụng.");
        return text.toString();
    }
}
