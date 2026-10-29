package com.eldercare.backend.resident;

import com.eldercare.backend.resident.api.dto.HealthInsurance;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService.RecordCommand;
import java.util.ArrayList;
import java.util.List;

/**
 * Dựng lệnh ghi phiếu nguyện vọng khi điều trị cho test (001 T077): mặc định là phiếu hợp lệ một bệnh viện, hỏi gia
 * đình trước, không người cần báo, gia đình đi kèm, do người cao tuổi trả lời.
 */
final class TreatmentCommandBuilder {

    private final List<PreferredHospital> hospitals =
            new ArrayList<>(List.of(new PreferredHospital("Bệnh viện X", "12 Lê Lợi, Quận 1")));
    private HealthInsurance insurance;
    private boolean askFamilyFirst = true;
    private final List<Long> notify = new ArrayList<>();
    private TreatmentEscort escort = TreatmentEscort.FAMILY;
    private String note = "Dị ứng penicillin, báo bác sĩ trực";
    private OpinionSource respondent = new OpinionSource(OpinionSourceType.RESIDENT, null);
    private final List<WishOpinion> opinions = new ArrayList<>();
    private Long signedCopyFileId;

    TreatmentCommandBuilder hospitals(PreferredHospital... values) {
        hospitals.clear();
        hospitals.addAll(List.of(values));
        return this;
    }

    TreatmentCommandBuilder insurance(HealthInsurance value) {
        this.insurance = value;
        return this;
    }

    TreatmentCommandBuilder askFamilyFirst(boolean value) {
        this.askFamilyFirst = value;
        return this;
    }

    TreatmentCommandBuilder notify(Long... familyMemberIds) {
        notify.addAll(List.of(familyMemberIds));
        return this;
    }

    TreatmentCommandBuilder escort(TreatmentEscort value) {
        this.escort = value;
        return this;
    }

    TreatmentCommandBuilder respondent(OpinionSourceType type, Long familyMemberId) {
        this.respondent = new OpinionSource(type, familyMemberId);
        return this;
    }

    TreatmentCommandBuilder opinion(OpinionSourceType type, Long familyMemberId, String content) {
        opinions.add(new WishOpinion(new OpinionSource(type, familyMemberId), content));
        return this;
    }

    TreatmentCommandBuilder signedCopy(Long fileId) {
        this.signedCopyFileId = fileId;
        return this;
    }

    RecordCommand build() {
        return new RecordCommand(
                List.copyOf(hospitals),
                insurance,
                askFamilyFirst,
                List.copyOf(notify),
                escort,
                note,
                respondent,
                List.copyOf(opinions),
                signedCopyFileId);
    }
}
