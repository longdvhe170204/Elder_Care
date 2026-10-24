package com.cambae.backend.m01.internal.status.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.CommandContext;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m01.api.spi.StatusTransitionParticipant;
import com.cambae.backend.m01.internal.assessment.domain.Assessment;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentStatus;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.persistence.AssessmentRepository;
import com.cambae.backend.m01.internal.assessment.service.ReassessmentNoticeService;
import com.cambae.backend.m01.internal.consent.persistence.ConsentStore;
import com.cambae.backend.m01.internal.consent.service.MissingConsentReminderService;
import com.cambae.backend.m01.internal.wish.service.EndOfLifeWishService;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService;
import com.cambae.backend.m01.internal.wish.service.WishReminderService;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điều kiện và tác động riêng của M01 trong lệnh trạng thái (research H-02 mục 4): Hoàn tất tiếp nhận cần đánh giá đầu
 * vào Đã xác nhận và lần đánh giá Đã xác nhận gần nhất chưa quá CFG-M01-02 (FR-034a). Thiếu bản đồng ý Hiệu lực thì không
 * chặn nhưng cảnh báo và bắt đầu nhắc Hành chính (FR-012); thiếu phiếu nguyện vọng cuối đời thì cảnh báo và bắt đầu
 * nhắc Bác sĩ, Điều dưỡng (FR-023d); thiếu phiếu nguyện vọng khi điều trị thì chỉ cảnh báo (FR-023l). Ghi nhận trở về từ Điều trị tại bệnh viện tính hạn đánh giá
 * lại từ lúc trở về và thêm lần báo căn cứ trở về (FR-041; T048). Chạy trước các module khác.
 */
@Component
@Order(0)
class M01StatusParticipant implements StatusTransitionParticipant {

    static final String NOT_CONFIRMED = "INITIAL_ASSESSMENT_NOT_CONFIRMED";
    static final String EXPIRED = "INITIAL_ASSESSMENT_EXPIRED";
    static final String MISSING_CONSENT = "MISSING_CONSENT";
    static final String MISSING_END_OF_LIFE_WISH = "MISSING_END_OF_LIFE_WISH";
    static final String MISSING_TREATMENT_WISH = "MISSING_TREATMENT_WISH";

    private final AssessmentRepository assessments;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final ConsentStore consents;
    private final MissingConsentReminderService reminders;
    private final ReassessmentNoticeService reassessmentNotices;
    private final EndOfLifeWishService endOfLifeWishes;
    private final TreatmentWishService treatmentWishes;
    private final WishReminderService wishReminders;

    M01StatusParticipant(
            AssessmentRepository assessments,
            ParameterApi parameters,
            BusinessTime time,
            ConsentStore consents,
            MissingConsentReminderService reminders,
            ReassessmentNoticeService reassessmentNotices,
            EndOfLifeWishService endOfLifeWishes,
            TreatmentWishService treatmentWishes,
            WishReminderService wishReminders) {
        this.assessments = assessments;
        this.parameters = parameters;
        this.time = time;
        this.consents = consents;
        this.reminders = reminders;
        this.reassessmentNotices = reassessmentNotices;
        this.endOfLifeWishes = endOfLifeWishes;
        this.treatmentWishes = treatmentWishes;
        this.wishReminders = wishReminders;
    }

    @Override
    public Set<StatusCommand> interestedCommands() {
        return Set.of(StatusCommand.COMPLETE_ADMISSION, StatusCommand.RECORD_RETURN);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Condition> checkConditions(CommandContext context) {
        if (context.command() != StatusCommand.COMPLETE_ADMISSION) {
            return List.of();
        }
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
    @Transactional(readOnly = true)
    public List<Warning> warnings(CommandContext context) {
        if (context.command() != StatusCommand.COMPLETE_ADMISSION) {
            return List.of();
        }
        long residentId = context.residentId();
        List<Warning> warnings = new ArrayList<>();
        if (consents.findActive(residentId).isEmpty()) {
            warnings.add(new Warning(
                    MISSING_CONSENT,
                    "Chưa có bản đồng ý chia sẻ dữ liệu Hiệu lực; Hành chính sẽ được nhắc ghi nhận (FR-012)"));
        }
        if (!endOfLifeWishes.hasActive(residentId)) {
            warnings.add(new Warning(
                    MISSING_END_OF_LIFE_WISH,
                    "Chưa có nguyện vọng cuối đời; Bác sĩ, Điều dưỡng phụ trách sẽ được nhắc ghi phiếu (FR-023d)"));
        }
        if (!treatmentWishes.hasActive(residentId)) {
            warnings.add(new Warning(
                    MISSING_TREATMENT_WISH, "Chưa có nguyện vọng khi điều trị; không nhắc định kỳ (FR-023l)"));
        }
        return warnings;
    }

    @Override
    public void applyEffects(TransitionResult result) {
        if (result.command() == StatusCommand.RECORD_RETURN && result.from() == ResidentStatus.HOSPITALIZED) {
            reassessmentNotices.onReturnFromHospital(result.residentId(), result.historyId(), result.at());
        }
        if (result.command() == StatusCommand.COMPLETE_ADMISSION) {
            if (consents.findActive(result.residentId()).isEmpty()) {
                reminders.startReminding(result.residentId());
            }
            if (!endOfLifeWishes.hasActive(result.residentId())) {
                wishReminders.startReminding(result.residentId());
            }
        }
    }
}
