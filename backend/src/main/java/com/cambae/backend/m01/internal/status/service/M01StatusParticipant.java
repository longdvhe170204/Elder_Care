package com.cambae.backend.m01.internal.status.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import com.cambae.backend.m01.internal.assessment.domain.Assessment;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentStatus;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.persistence.AssessmentRepository;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điều kiện và tác động riêng của M01 trong lệnh trạng thái (research H-02 mục 4): Hoàn tất tiếp nhận cần đánh giá đầu
 * vào Đã xác nhận và lần đánh giá Đã xác nhận gần nhất chưa quá CFG-M01-02 (FR-034a). Chạy trước các module khác.
 */
@Component
@Order(0)
class M01StatusParticipant implements StatusTransitionParticipant {

    static final String NOT_CONFIRMED = "INITIAL_ASSESSMENT_NOT_CONFIRMED";
    static final String EXPIRED = "INITIAL_ASSESSMENT_EXPIRED";

    private final AssessmentRepository assessments;
    private final ParameterApi parameters;
    private final BusinessTime time;

    M01StatusParticipant(AssessmentRepository assessments, ParameterApi parameters, BusinessTime time) {
        this.assessments = assessments;
        this.parameters = parameters;
        this.time = time;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.COMPLETE_ADMISSION);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Condition> checkConditions(CommandContext context) {
        long residentId = context.residentId();
        if (!assessments.existsByResidentIdAndTypeAndStatus(
                residentId, AssessmentType.INITIAL, AssessmentStatus.CONFIRMED)) {
            return List.of(new Condition(
                    NOT_CONFIRMED, "Chưa có đánh giá đầu vào ở trạng thái Đã xác nhận", "FR-034a", false));
        }
        Optional<Assessment> latest = assessments.findFirstByResidentIdAndStatusOrderByConfirmedAtDescIdDesc(
                residentId, AssessmentStatus.CONFIRMED);
        Duration validity = parameters.read(ParameterCode.CFG_M01_02).asDuration();
        boolean expired = latest.map(a -> time.now().isAfter(a.getConfirmedAt().plus(validity)))
                .orElse(true);
        if (expired) {
            return List.of(new Condition(
                    EXPIRED,
                    "Lần đánh giá Đã xác nhận gần nhất đã quá hạn hiệu lực (CFG-M01-02), cần đánh giá lại",
                    "FR-034a",
                    false));
        }
        return List.of();
    }

    @Override
    public void applyEffects(TransitionResult result) {
        // Tác động riêng của M01 khi Ghi nhận trở về từ bệnh viện cần bảng lan_bao_danh_gia_lai (T062, Phase 8).
    }
}
