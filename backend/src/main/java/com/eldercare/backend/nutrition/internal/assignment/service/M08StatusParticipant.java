package com.eldercare.backend.nutrition.internal.assignment.service;

import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Bean {@code ThamGiaChuyenTrangThai} của 011 (T014; module-interfaces mục 1.1; FR-014, FR-015): không có điều kiện chặn.
 * Trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận) Ngừng bản Hiệu lực, Hủy bản đang chờ; Hoàn tất tiếp nhận khi
 * chưa có bản gán thì báo Bác sĩ. Thứ tự sau 006, trước 010 (001 H-02; 012 là 70). Phát sinh "−1" cho bữa đã chốt chưa
 * tới giờ thêm ở phase chốt suất.
 */
@Component
@Order(50)
class M08StatusParticipant implements StatusTransitionParticipant {

    private static final Set<ResidentStatus> FINAL =
            EnumSet.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final DietAssignmentService assignments;

    M08StatusParticipant(DietAssignmentService assignments) {
        this.assignments = assignments;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(
                StatusCommand.COMPLETE_ADMISSION,
                StatusCommand.CANCEL_ADMISSION,
                StatusCommand.TRANSFER_OUT,
                StatusCommand.END_RESIDENCY,
                StatusCommand.RECORD_DEATH);
    }

    @Override
    public List<Condition> checkConditions(CommandContext context) {
        return List.of();
    }

    @Override
    public void applyEffects(TransitionResult result) {
        if (FINAL.contains(result.to())) {
            assignments.onFinalStatus(result.residentId(), result.to(), result.at());
        } else if (result.command() == StatusCommand.COMPLETE_ADMISSION) {
            assignments.onAdmissionCompleted(result.residentId(), result.at());
        }
    }
}
