package com.eldercare.backend.resident;

import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.resident.api.dto.ConsenterType;
import com.eldercare.backend.resident.internal.consent.domain.Consent;
import com.eldercare.backend.resident.internal.consent.service.ConsentService;
import com.eldercare.backend.resident.internal.consent.service.ConsentService.RecordConsent;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Nền chung cho test bản đồng ý của 001 US5 (T053, T054): tệp bằng chứng lưu ở bản giả S3, quan hệ người thân đặt ở bản
 * giả {@link FakeFamilyRelationships} (012 cài). Người ghi nhận là Hành chính (AC-09, Phụ lục 27 dòng "Bản đồng ý chia
 * sẻ dữ liệu").
 */
@Import(FakeFileConfig.class)
public abstract class ConsentTestBase extends HealthTestBase {

    private static final byte[] PDF = "%PDF-1.4 bản ký thử".getBytes(StandardCharsets.UTF_8);

    @Autowired
    protected FileApi files;

    @Autowired
    protected ConsentService consents;

    @Autowired
    protected FakeFamilyRelationships families;

    /** Tệp PDF vừa tải lên, chưa gắn, do Hành chính tải. */
    protected long evidence() {
        return files.upload("ban-ky.pdf", FileTypes.PDF, PDF, office.accountId());
    }

    protected static long newFamilyMember() {
        return TestUsers.nextId();
    }

    /** Người thân có quan hệ Hiệu lực với người cao tuổi (bản giả 012). */
    protected long relatedFamilyMember(long residentId) {
        long id = newFamilyMember();
        families.relate(id, residentId);
        return id;
    }

    /** Người cao tuổi tự đồng ý: cho {@code allowed} xem sức khỏe, đồng ý hình ảnh và thông báo. */
    protected RecordConsent byResident(Set<Long> allowed) {
        return new RecordConsent(ConsenterType.RESIDENT, null, null, allowed, true, true, Instant.now(), evidence());
    }

    /** Hành chính ghi bản đồng ý của người cao tuổi. */
    protected Consent record(long residentId, RecordConsent command) {
        user.set(office.user());
        return consents.record(target(residentId), command);
    }

    protected int activeCount(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM consent WHERE resident_id = ? AND status = 'ACTIVE'", Integer.class, residentId);
    }

    protected List<String> auditActions(long consentId) {
        return jdbc.queryForList(
                "SELECT action FROM audit_log WHERE entity_type = 'M01.CONSENT' AND entity_id = ? ORDER BY id",
                String.class,
                consentId);
    }
}
