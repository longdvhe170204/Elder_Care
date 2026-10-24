package com.cambae.backend.m01;

import com.cambae.backend.m01.api.dto.CourtDecisionRef;
import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.OpinionSourceType;
import com.cambae.backend.m01.api.dto.RecordEndOfLifeWish;
import com.cambae.backend.m01.api.dto.RepresentativeSelection;
import com.cambae.backend.m01.api.dto.SelfDeterminationAssessment;
import com.cambae.backend.m01.api.dto.WishChoice;
import com.cambae.backend.m01.api.dto.WishOpinion;
import com.cambae.backend.m01.api.dto.WishSignerType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dựng lệnh ghi phiếu nguyện vọng cuối đời cho test (001 T067): mặc định là phiếu hợp lệ do chính người cao tuổi trả lời
 * và ký, Bác sĩ nhận định tự quyết được; mỗi test chỉ đổi phần cần thử.
 */
final class WishCommandBuilder {

    private final long residentId;
    private WishChoice choice = WishChoice.STAY_PALLIATIVE;
    private String firstContact = "Con trai: 0901 234 567";
    private String note = "Muốn có người thân bên cạnh";
    private WishSignerType respondentType = WishSignerType.RESIDENT;
    private Long respondentFamilyMemberId;
    private String relationship;
    private Long assessingDoctorId;
    private SelfDeterminationAssessment assessment;
    private WishSignerType signerType = WishSignerType.RESIDENT;
    private Long signerFamilyMemberId;
    private RepresentativeSelection representativeSelection;
    private CourtDecisionRef courtDecision;
    private final List<WishOpinion> opinions = new ArrayList<>();
    private final Instant signedAt = Instant.now();
    private Long signedCopyFileId;

    WishCommandBuilder(long residentId, long assessingDoctorId, long signedCopyFileId) {
        this.residentId = residentId;
        this.assessingDoctorId = assessingDoctorId;
        this.signedCopyFileId = signedCopyFileId;
        this.assessment = new SelfDeterminationAssessment(
                true, "Giải thích bằng lời, nhắc lại hai lần", LocalDate.now().plusDays(30));
    }

    WishCommandBuilder choice(WishChoice value) {
        this.choice = value;
        return this;
    }

    WishCommandBuilder assessment(boolean canSelfDetermine, String supportMethod, LocalDate reviewDate) {
        this.assessment = new SelfDeterminationAssessment(canSelfDetermine, supportMethod, reviewDate);
        return this;
    }

    WishCommandBuilder noAssessment() {
        this.assessment = null;
        return this;
    }

    WishCommandBuilder assessingDoctor(Long accountId) {
        this.assessingDoctorId = accountId;
        return this;
    }

    WishCommandBuilder respondent(WishSignerType type, Long familyMemberId, String relation) {
        this.respondentType = type;
        this.respondentFamilyMemberId = familyMemberId;
        this.relationship = relation;
        return this;
    }

    /** Người đại diện của người bệnh trả lời và ký, kèm cách được chọn; Bác sĩ nhận định không tự quyết được. */
    WishCommandBuilder representativeSigns(long familyMemberId, RepresentativeSelection selection) {
        this.assessment = new SelfDeterminationAssessment(
                false, "Giải thích cho người đại diện", LocalDate.now().plusDays(30));
        this.respondentType = WishSignerType.PATIENT_REPRESENTATIVE;
        this.respondentFamilyMemberId = familyMemberId;
        this.relationship = "Con gái";
        this.signerType = WishSignerType.PATIENT_REPRESENTATIVE;
        this.signerFamilyMemberId = familyMemberId;
        this.representativeSelection = selection;
        return this;
    }

    /** Người đại diện theo pháp luật trả lời và ký, kèm quyết định của Tòa án. */
    WishCommandBuilder legalRepresentativeSigns(long familyMemberId, CourtDecisionRef decision) {
        representativeSigns(familyMemberId, null);
        this.respondentType = WishSignerType.LEGAL_REPRESENTATIVE;
        this.signerType = WishSignerType.LEGAL_REPRESENTATIVE;
        this.courtDecision = decision;
        return this;
    }

    WishCommandBuilder signer(WishSignerType type, Long familyMemberId) {
        this.signerType = type;
        this.signerFamilyMemberId = familyMemberId;
        return this;
    }

    WishCommandBuilder selection(RepresentativeSelection value) {
        this.representativeSelection = value;
        return this;
    }

    WishCommandBuilder courtDecision(CourtDecisionRef value) {
        this.courtDecision = value;
        return this;
    }

    WishCommandBuilder opinion(OpinionSourceType type, Long familyMemberId, String content) {
        opinions.add(new WishOpinion(new OpinionSource(type, familyMemberId), content));
        return this;
    }

    WishCommandBuilder signedCopy(Long fileId) {
        this.signedCopyFileId = fileId;
        return this;
    }

    RecordEndOfLifeWish build() {
        return new RecordEndOfLifeWish(
                residentId,
                choice,
                firstContact,
                note,
                respondentType,
                respondentFamilyMemberId,
                relationship,
                assessingDoctorId,
                assessment,
                signerType,
                signerFamilyMemberId,
                representativeSelection,
                courtDecision,
                List.copyOf(opinions),
                signedAt,
                signedCopyFileId);
    }
}
