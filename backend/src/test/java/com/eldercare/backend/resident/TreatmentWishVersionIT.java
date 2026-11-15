package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWish;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWishStatus;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService.Result;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T077 (FR-023j, FR-023k, FR-023g, DBR-39, BR-M01-16; US7 #9, #10; SC-014): phiếu nguyện vọng khi điều trị có phiên
 * bản như phiếu cuối đời nhưng bản cũ chuyển Hết hiệu lực. Một hoặc hai bệnh viện ưu tiên; người cần báo là người thân
 * có quan hệ Hiệu lực (0..n); hỏi gia đình trước mà không có người cần báo thì chỉ cảnh báo; ý kiến gia đình khác lưu
 * từng dòng có nguồn; {@code currentTreatmentWish} trả đủ trường cho 007.
 */
@Tag("BR-06")
class TreatmentWishVersionIT extends WishTestBase {

    @Autowired
    EndOfLifeWishApi api;

    private Result record(long residentId, TreatmentCommandBuilder builder) {
        assignNurse(nurse, residentId);
        user.set(nurse.user());
        return treatments.record(target(residentId), builder.build());
    }

    @Test
    void US7_9_firstVersion_isActive_withWriterAndAudit() {
        long residentId = newResident();
        long child = relatedFamilyMember(residentId);

        TreatmentWish wish =
                record(residentId, new TreatmentCommandBuilder().notify(child)).wish();

        assertThat(wish.status()).isEqualTo(TreatmentWishStatus.ACTIVE);
        assertThat(wish.preferredHospitals())
                .extracting(PreferredHospital::name)
                .containsExactly("Bệnh viện X");
        assertThat(wish.askFamilyFirst()).isTrue();
        assertThat(wish.notifyFamilyMemberIds()).containsExactly(child);
        assertThat(wish.accompaniedBy()).isEqualTo(TreatmentEscort.FAMILY);
        assertThat(wish.recordedBy()).isEqualTo(nurse.accountId());
        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type = 'M01.TREATMENT_WISH' AND entity_id = ?",
                        String.class,
                        wish.id()))
                .contains("TREATMENT_WISH_RECORDED");
    }
}
