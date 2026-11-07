package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.file.FileService;
import com.eldercare.backend.resident.api.ConsentApi;
import com.eldercare.backend.resident.api.dto.ConsenterType;
import com.eldercare.backend.resident.api.events.ConsentChanged;
import com.eldercare.backend.resident.internal.consent.domain.Consent;
import com.eldercare.backend.resident.internal.consent.domain.ConsentStatus;
import com.eldercare.backend.resident.internal.consent.service.ConsentService.RecordConsent;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T053 (US5 #1, #2, #5, #6; FR-010, FR-013, FR-016, DBR-03): Hành chính ghi nhận bản đồng ý có bản ký scan; bản mới
 * thay bản Hiệu lực cũ trong cùng lệnh, luôn tối đa một bản Hiệu lực; ghi trên hồ sơ trạng thái cuối bị chặn; chỉ Hành
 * chính ghi; nội dung đã ghi không đổi được ở cơ sở dữ liệu.
 */
class ConsentIT extends ConsentTestBase {

    @Autowired
    ConsentApi api;

    @Autowired
    ApplicationEvents events;

    @Autowired
    FileService fileService;

    @Test
    void US5_residentConsent_isActive_attachesEvidence_andPublishesChange() {
        long residentId = newResident();
        long x = relatedFamilyMember(residentId);
        long evidence = evidence();

        Consent saved = record(
                residentId,
                new RecordConsent(ConsenterType.RESIDENT, null, null, Set.of(x), true, false, Instant.now(), evidence));

        assertThat(saved.status()).isEqualTo(ConsentStatus.ACTIVE);
        assertThat(saved.familyMemberIdsAllowedHealthView()).containsExactly(x);
        assertThat(saved.photoConsent()).isTrue();
        assertThat(saved.notificationConsent()).isFalse();
        assertThat(saved.evidenceFileId()).isEqualTo(evidence);
        assertThat(jdbc.queryForObject(
                        "SELECT CONCAT(owner_type, ':', owner_id) FROM file WHERE id = ?", String.class, evidence))
                .isEqualTo("M01.CONSENT:" + saved.id());
        assertThat(events.stream(ConsentChanged.class).filter(e -> e.residentId() == residentId))
                .singleElement()
                .satisfies(e -> assertThat(e.activeConsentId()).isEqualTo(saved.id()));
        assertThat(auditActions(saved.id())).containsExactly("CONSENT_RECORDED");
    }
}
