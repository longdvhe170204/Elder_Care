package com.eldercare.backend.resident.internal.assessment.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.events.CareLevelChangeDeclinedLeave;
import com.eldercare.backend.resident.api.events.CareLevelKeepReviewDue;
import com.eldercare.backend.resident.api.spi.CareLevelChangeRequestApi;
import com.eldercare.backend.resident.api.spi.CareRiskAgreementPort;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.CareLevelChangeDecision;
import com.eldercare.backend.resident.internal.assessment.domain.CareLevelChangeDecision.Decision;
import com.eldercare.backend.resident.internal.assessment.persistence.AssessmentRepository;
import com.eldercare.backend.resident.internal.assessment.persistence.CareLevelChangeDecisionStore;
import com.eldercare.backend.resident.internal.assessment.persistence.CareLevelHistoryStore;
import com.eldercare.backend.resident.internal.status.service.ProfileOpenGuard;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quyết định khi gia đình không đồng ý đổi mức chăm sóc theo lần đánh giá lại (001 FR-036b, Q-423; plan-bf08 Bước 6):
 * Hành chính ghi giữ mức cũ (kèm thỏa thuận lựa chọn có rủi ro đang hiệu lực, ngày xem lại) hoặc rời viện. Có quyết định
 * thì nhắc lập yêu cầu đổi mức (CFG-M03-02) của lần đánh giá đó dừng ({@link CareLevelChangeRequestReminderService}); rời
 * viện thì Hành chính được báo lập hồ sơ kết thúc lưu trú, hệ thống không tự lập (Q-306). Quyết định gắn lần đánh giá nên
 * lần đánh giá lại mới làm quyết định cũ hết tác dụng.
 */
@Service
public class CareLevelChangeDecisionService {

    public record Input(
            Long assessmentId, Decision decision, Long riskAgreementId, LocalDate reviewDate, String note) {}

    static final int NOTE_MAX = 1000;

    private final CareLevelChangeDecisionStore decisions;
    private final AssessmentRepository assessments;
    private final AssessmentContents contents;
    private final CareLevelHistoryStore history;
    private final ObjectProvider<CareLevelChangeRequestApi> requests;
    private final ObjectProvider<CareRiskAgreementPort> risks;
    private final ObjectProvider<CurrentUser> currentUser;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ProfileOpenGuard guard;

    CareLevelChangeDecisionService(
            CareLevelChangeDecisionStore decisions,
            AssessmentRepository assessments,
            AssessmentContents contents,
            CareLevelHistoryStore history,
            ObjectProvider<CareLevelChangeRequestApi> requests,
            ObjectProvider<CareRiskAgreementPort> risks,
            ObjectProvider<CurrentUser> currentUser,
            ApplicationEventPublisher events,
            BusinessTime time,
            ProfileOpenGuard guard) {
        this.decisions = decisions;
        this.assessments = assessments;
        this.contents = contents;
        this.history = history;
        this.requests = requests;
        this.risks = risks;
        this.currentUser = currentUser;
        this.events = events;
        this.time = time;
        this.guard = guard;
    }

    /**
     * Ghi quyết định (Hành chính, Quản lý viện). Lần đánh giá phải là lần gần nhất đã xác nhận (409 {@code
     * NOT_LATEST_ASSESSMENT}) và ra mức khác mức hiện hành (409 {@code CARE_LEVEL_CHANGE_NOT_NEEDED}); đang có yêu cầu đổi
     * mức chưa kết thúc thì 409 {@code CHANGE_REQUEST_EXISTS} (hủy yêu cầu trước); đã có quyết định thì 409 {@code
     * DECISION_EXISTS}; giữ mức cũ cần thỏa thuận đang hiệu lực (409 {@code RISK_AGREEMENT_REQUIRED}) và ngày xem lại sau hôm
     * nay (422).
     */
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.RESIDENCY_CHANGE_REQUEST, operation = Operation.CREATE)
    @Transactional
    public CareLevelChangeDecision record(Target target, long residentId, Input input) {
        CurrentUser user = currentUser.getObject();
        if (user.type() == AccountType.FAMILY_MEMBER
                || !(user.roles().contains(Role.OFFICE_STAFF) || user.roles().contains(Role.NURSING_HOME_MANAGER))) {
            throw new PermissionDeniedException("Chỉ Hành chính ghi quyết định khi gia đình không đồng ý đổi mức");
        }
        guard.requireOpen(residentId);
        if (input == null || input.assessmentId() == null || input.decision() == null) {
            throw new InvalidValueException("Cần lần đánh giá và quyết định");
        }
        String note = input.note() == null || input.note().isBlank()
                ? null
                : input.note().strip();
        if (note != null && note.length() > NOTE_MAX) {
            throw new InvalidValueException("Ghi chú tối đa " + NOTE_MAX + " ký tự");
        }
        var latest = assessments.findFirstByResidentIdAndStatusOrderByConfirmedAtDescIdDesc(
                residentId, AssessmentStatus.CONFIRMED);
        if (latest.isEmpty() || latest.get().getId() != input.assessmentId()) {
            throw conflict(
                    "NOT_LATEST_ASSESSMENT",
                    "Quyết định chỉ ghi cho lần đánh giá gần nhất đã xác nhận của người cao tuổi");
        }
        CareLevel assessed = contents.of(latest.get()).careLevel();
        if (history.current(residentId).map(current -> current == assessed).orElse(false)) {
            throw conflict(
                    "CARE_LEVEL_CHANGE_NOT_NEEDED",
                    "Lần đánh giá này không ra mức chăm sóc khác mức hiện hành; không cần quyết định");
        }
        CareLevelChangeRequestApi api = requests.getIfAvailable();
        if (api != null && api.hasOpenRequest(residentId)) {
            throw conflict(
                    "CHANGE_REQUEST_EXISTS",
                    "Đang có yêu cầu đổi mức chăm sóc chưa kết thúc; hủy yêu cầu đó trước khi ghi quyết định");
        }
        if (decisions.existsFor(input.assessmentId())) {
            throw conflict("DECISION_EXISTS", "Lần đánh giá này đã có quyết định");
        }
        LocalDate today = time.today();
        if (input.decision() == Decision.KEEP_CURRENT_LEVEL) {
            CareRiskAgreementPort port = risks.getIfAvailable();
            if (input.riskAgreementId() == null
                    || port == null
                    || !port.isActive(residentId, input.riskAgreementId(), today)) {
                throw conflict(
                        "RISK_AGREEMENT_REQUIRED",
                        "Giữ mức cũ cần thỏa thuận lựa chọn có rủi ro đang hiệu lực trong kế hoạch chăm sóc (Điều dưỡng"
                                + " lập, được duyệt cùng kế hoạch)");
            }
            if (input.reviewDate() == null || !input.reviewDate().isAfter(today)) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_CONTENT, "REVIEW_DATE_INVALID", "Ngày xem lại phải sau hôm nay");
            }
        }
        boolean keep = input.decision() == Decision.KEEP_CURRENT_LEVEL;
        long id = decisions.insert(
                residentId,
                input.assessmentId(),
                input.decision(),
                keep ? input.riskAgreementId() : null,
                keep ? input.reviewDate() : null,
                note,
                user.accountId(),
                time.now());
        if (!keep) {
            events.publishEvent(new CareLevelChangeDeclinedLeave(residentId, id, time.now()));
        }
        return decisions.ofResident(residentId).stream()
                .filter(r -> r.id() == id)
                .findFirst()
                .orElseThrow();
    }

    @RequiresPermission(function = Function.RESIDENCY_CHANGE_REQUEST, operation = Operation.CREATE)
    @Transactional(readOnly = true)
    public List<CareLevelChangeDecision> list(Target target, long residentId) {
        return decisions.ofResident(residentId);
    }

    /** Tới ngày xem lại quyết định giữ mức (còn là lần đánh giá gần nhất): nhắc Bác sĩ, Hành chính một lần. */
    @Transactional
    public int remindReviews() {
        int sent = 0;
        for (CareLevelChangeDecision row : decisions.reviewDue(time.today())) {
            var latest = assessments.findFirstByResidentIdAndStatusOrderByConfirmedAtDescIdDesc(
                    row.residentId(), AssessmentStatus.CONFIRMED);
            boolean stillCurrent = latest.isPresent() && latest.get().getId() == row.assessmentId();
            if (decisions.markReviewNotified(row.id(), time.now()) && stillCurrent) {
                events.publishEvent(new CareLevelKeepReviewDue(row.residentId(), row.id(), time.now()));
                sent++;
            }
        }
        return sent;
    }

    /** Lần đánh giá đã có quyết định thì dừng nhắc lập yêu cầu đổi mức của lần đó (Q-423 (b)). */
    boolean decided(long assessmentId) {
        return decisions.existsFor(assessmentId);
    }

    private static BusinessException conflict(String code, String text) {
        return new BusinessException(HttpStatus.CONFLICT, code, text);
    }
}
