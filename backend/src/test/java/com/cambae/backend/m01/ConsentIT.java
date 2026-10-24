package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.file.FileService;
import com.cambae.backend.m01.api.ConsentApi;
import com.cambae.backend.m01.api.dto.ConsenterType;
import com.cambae.backend.m01.api.events.ConsentChanged;
import com.cambae.backend.m01.internal.consent.domain.Consent;
import com.cambae.backend.m01.internal.consent.domain.ConsentStatus;
import com.cambae.backend.m01.internal.consent.service.ConsentService.RecordConsent;
import java.time.Instant;
import java.util.List;
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

    @Test
    void US5_withoutEvidence_is422() {
        long residentId = newResident();

        assertThatThrownBy(() -> record(
                        residentId,
                        new RecordConsent(
                                ConsenterType.RESIDENT, null, null, Set.of(), true, true, Instant.now(), null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("EVIDENCE_REQUIRED");
                    assertThat(e.status().value()).isEqualTo(422);
                });

        assertThat(activeCount(residentId)).isZero();
    }

    @Test
    void US5_newConsent_supersedesTheActiveOne_inTheSameCommand() {
        long residentId = newResident();
        long x = relatedFamilyMember(residentId);
        long y = relatedFamilyMember(residentId);
        Consent first = record(residentId, byResident(Set.of(x)));

        Consent second = record(residentId, byResident(Set.of(y)));

        assertThat(activeCount(residentId)).isEqualTo(1);
        Consent old = consents.list(target(residentId)).stream()
                .filter(c -> c.id() == first.id())
                .findFirst()
                .orElseThrow();
        assertThat(old.status()).isEqualTo(ConsentStatus.WITHDRAWN);
        assertThat(old.withdrawal().supersededById()).isEqualTo(second.id());
        assertThat(old.withdrawal().evidenceFileId()).isNull();
        assertThat(api.current(residentId)).get().satisfies(v -> {
            assertThat(v.id()).isEqualTo(second.id());
            assertThat(v.familyMemberIdsAllowedHealthView()).containsExactly(y);
        });
        assertThat(api.includes(residentId, y)).isTrue();
        assertThat(api.includes(residentId, x)).isFalse();
        assertThat(auditActions(first.id())).containsExactly("CONSENT_RECORDED", "CONSENT_SUPERSEDED");
        assertThat(events.stream(ConsentChanged.class)
                        .filter(e -> e.residentId() == residentId)
                        .map(ConsentChanged::activeConsentId)
                        .toList())
                .containsExactly(first.id(), second.id());
    }

    @Test
    void US5_list_returnsHistoryNewestFirst() {
        long residentId = newResident();
        Consent first = record(residentId, byResident(Set.of()));
        Consent second = record(residentId, byResident(Set.of()));

        List<Long> ids =
                consents.list(target(residentId)).stream().map(Consent::id).toList();

        assertThat(ids).containsExactly(second.id(), first.id());
    }

    @Test
    void BR_M01_05_recordOnClosedProfile_is409() {
        long residentId = newResident();
        setStatus(residentId, "DECEASED");

        assertThatThrownBy(() -> record(residentId, byResident(Set.of())))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("PROFILE_CLOSED");
                    assertThat(e.status().value()).isEqualTo(409);
                });
    }

    @Test
    void AC09_onlyOfficeStaffRecords_doctorAndNurseAreDenied() {
        long residentId = newResident();
        assignNurse(nurse, residentId);
        RecordConsent command = byResident(Set.of());

        user.set(doctor.user());
        assertThatThrownBy(() -> consents.record(target(residentId), command))
                .isInstanceOf(PermissionDeniedException.class);
        user.set(nurse.user());
        assertThatThrownBy(() -> consents.record(target(residentId), command))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void NFR_08_evidenceDownload_followsTheViewPermissionOfConsents() {
        long residentId = newResident();
        Consent saved = record(residentId, byResident(Set.of()));

        user.set(office.user());
        assertThat(fileService
                        .download(saved.evidenceFileId(), office.accountId())
                        .content())
                .isNotEmpty();
        user.set(doctor.user());
        assertThatThrownBy(() -> fileService.download(saved.evidenceFileId(), doctor.accountId()))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_016_contentCannotBeChangedOrDeleted_inTheDatabase() throws Exception {
        long residentId = newResident();
        Consent saved = record(residentId, byResident(Set.of(relatedFamilyMember(residentId))));

        try (var admin = adminConnection();
                var update = admin.prepareStatement("UPDATE consent SET photo_consent = 0 WHERE id = ?");
                var children = admin.prepareStatement(
                        "UPDATE consent_family_member SET family_member_id = 1 WHERE consent_id = ?")) {
            update.setLong(1, saved.id());
            assertThatThrownBy(update::executeUpdate).hasMessageContaining("consent");
            children.setLong(1, saved.id());
            assertThatThrownBy(children::executeUpdate).hasMessageContaining("append-only");
        }
        assertThatThrownBy(() -> jdbc.update("DELETE FROM consent WHERE id = ?", saved.id()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void FR_013_twoActiveConsentsAreRejectedByTheDatabase() throws Exception {
        long residentId = newResident();
        record(residentId, byResident(Set.of()));

        try (var admin = adminConnection();
                var insert = admin.prepareStatement(
                        "INSERT INTO consent (resident_id, consenter_type, photo_consent, notification_consent,"
                                + " consented_at, evidence_file_id, status, created_at, created_by)"
                                + " SELECT resident_id, consenter_type, photo_consent, notification_consent,"
                                + " consented_at, evidence_file_id, status, created_at, created_by"
                                + " FROM consent WHERE resident_id = ? AND status = 'ACTIVE'")) {
            insert.setLong(1, residentId);
            assertThatThrownBy(insert::executeUpdate).hasMessageContaining("Duplicate");
        }
    }
}
