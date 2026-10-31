package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.care.internal.careplan.domain.*;
import com.eldercare.backend.care.internal.careplan.persistence.*;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.resident.api.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** FR-002a: nhận định theo quyết định, kiểm nguồn ý kiến qua API duy nhất của 001. */
@Service
public class CareRiskService {
    private final CarePlanStore plans;
    private final CareRiskStore risks;
    private final CareLockStore locks;
    private final AccountApi accounts;
    private final OpinionSourceApi sources;
    private final ObjectProvider<FamilyRelationshipApi> families;
    private final ResidentApi residents;
    private final AuditLogApi audit;

    public CareRiskService(
            CarePlanStore plans,
            CareRiskStore risks,
            CareLockStore locks,
            AccountApi accounts,
            OpinionSourceApi sources,
            ObjectProvider<FamilyRelationshipApi> families,
            ResidentApi residents,
            AuditLogApi audit) {
        this.plans = plans;
        this.risks = risks;
        this.locks = locks;
        this.accounts = accounts;
        this.sources = sources;
        this.families = families;
        this.residents = residents;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void replace(Target target, long planId, String etag, List<CareRiskAgreement> requested) {
        var before = plans.require(planId);
        locks.lock(List.of(before.residentId()));
        var plan = plans.locked(planId);
        if (plan.status() != CarePlanStatus.DRAFT)
            throw new BusinessException(HttpStatus.CONFLICT, "REVISION_LOCKED", "Chỉ sửa thỏa thuận ở bản Nháp");
        if (residents.isFinalStatus(plan.residentId()))
            throw new BusinessException(HttpStatus.CONFLICT, "RESIDENT_FINAL_STATUS", "Hồ sơ đã ở trạng thái cuối");
        VersionEtag.requireMatch(etag, plan.version(), Map.of("id", planId, "version", plan.version()));
        var old = new HashMap<Long, CareRiskAgreement>();
        risks.list(planId).forEach(row -> old.put(row.id(), row));
        var ids = new HashSet<Long>();
        var rows = new ArrayList<CareRiskAgreement>();
        for (var row : requested) {
            if (row.id() != 0 && (!old.containsKey(row.id()) || !ids.add(row.id())))
                throw invalid("Thỏa thuận không thuộc bản Nháp hoặc bị lặp");
            var previous = old.get(row.id());
            if (row.sourceAgreementId() != null
                    && (previous == null || !Objects.equals(row.sourceAgreementId(), previous.sourceAgreementId())))
                throw invalid("Không được tự chọn gốc thỏa thuận");
            validate(plan.residentId(), row);
            rows.add(new CareRiskAgreement(
                    row.id(),
                    planId,
                    previous == null ? null : previous.sourceAgreementId(),
                    row.choice(),
                    row.discussedRisks(),
                    row.residentParticipates(),
                    row.participants(),
                    row.assessingDoctorId(),
                    row.selfDeterminationAssessment(),
                    row.assessedAt(),
                    row.decisionMaker(),
                    row.opinions(),
                    row.reviewDate(),
                    row.recordedBy(),
                    row.recordedAt()));
        }
        risks.save(planId, rows);
        plans.touch(planId);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_PLAN_RISKS_REPLACED", false))
                .entity("M04.CARE_PLAN", planId)
                .before(Map.of("version", plan.version(), "riskAgreements", old.values()))
                .after(Map.of("version", plan.version() + 1, "riskAgreements", rows))
                .build());
    }

    private void validate(long residentId, CareRiskAgreement row) {
        if (row.choice() == null
                || row.choice().length() > 2000
                || row.discussedRisks() == null
                || row.discussedRisks().length() > 2000
                || row.selfDeterminationAssessment() == null
                || row.decisionMaker() == null
                || row.reviewDate() == null) throw invalid("Thỏa thuận thiếu trường hoặc vượt độ dài");
        var assessment = row.selfDeterminationAssessment();
        var maker = row.decisionMaker();
        if (assessment.supportMethod() == null
                || assessment.supportMethod().isBlank()
                || assessment.supportMethod().length() > 2000
                || assessment.reviewDate() == null
                || assessment.canSelfDetermine() && !row.residentParticipates())
            throw invalid("Nhận định thiếu cách hỗ trợ, ngày xem lại hoặc người tham gia");
        doctor(row.assessingDoctorId());
        if (maker.type() == null
                || !Set.of("RESIDENT", "PATIENT_REPRESENTATIVE", "DOCTOR").contains(maker.type())
                || assessment.canSelfDetermine() != maker.type().equals("RESIDENT"))
            throw invalid("Người quyết định không khớp nhận định");
        switch (maker.type()) {
            case "RESIDENT" -> {
                if (maker.familyMemberId() != null || maker.doctorId() != null)
                    throw invalid("Người cao tuổi tự quyết không dùng id người quyết định thay");
            }
            case "PATIENT_REPRESENTATIVE" -> {
                if (maker.familyMemberId() == null
                        || maker.doctorId() != null
                        || !belongs(maker.familyMemberId(), residentId))
                    throw invalid("Người đại diện không có quan hệ Hiệu lực");
            }
            case "DOCTOR" -> {
                if (maker.doctorId() == null
                        || maker.familyMemberId() != null
                        || maker.reason() == null
                        || maker.reason().isBlank()
                        || maker.reason().length() > 1000) throw invalid("Bác sĩ quyết định cần lý do và id hợp lệ");
                doctor(maker.doctorId());
            }
            default -> throw invalid("Người quyết định không hợp lệ");
        }
        var participants = new HashSet<String>();
        for (var participant : row.participants()) {
            if (participant.type() == null
                    || !Set.of("FAMILY_MEMBER", "STAFF").contains(participant.type())
                    || participant.id() <= 0
                    || !participants.add(participant.type() + ":" + participant.id()))
                throw invalid("Người tham gia không hợp lệ hoặc bị lặp");
            if (participant.type().equals("FAMILY_MEMBER") && !belongs(participant.id(), residentId))
                throw invalid("Người tham gia không có quan hệ Hiệu lực");
        }
        for (var opinion : row.opinions()) {
            if (opinion.opinion().content() == null
                    || opinion.opinion().content().length() > 2000
                    || opinion.opinion().source() == null) throw invalid("Ý kiến thiếu nội dung hoặc nguồn");
            sources.validate(residentId, opinion.opinion().source());
        }
    }

    private boolean belongs(long familyId, long residentId) {
        var source = families.getIfAvailable();
        return source != null && source.belongsToResident(familyId, residentId);
    }

    private void doctor(long staffId) {
        if (accounts.byStaff(staffId)
                .filter(account -> accounts.hasRole(account.accountId(), Role.DOCTOR))
                .isEmpty()) throw invalid("Bác sĩ nhận định/quyết định phải có vai trò AC-03");
    }

    private InvalidValueException invalid(String message) {
        return new InvalidValueException(message);
    }
}
