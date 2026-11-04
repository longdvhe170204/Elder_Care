package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.admission.internal.registration.domain.ObligationTrigger;
import com.eldercare.backend.admission.internal.registration.domain.ReminderType;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionChannel;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationObligationStore;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationReminderStore;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationResidentStore;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationSubmissionStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.file.FilePermissionProvider;
import com.eldercare.backend.resident.internal.status.service.ResidentStatusService;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 018 T005 (contracts/module-interfaces.md mục 2; FR-009, FR-050, FR-051, FR-051a, NFR-07, NFR-08): bốn provider của khai
 * báo: phân loại trường nhật ký, lưu giữ theo hồ sơ (CFG-M15-03), quyền tải tệp bằng chứng (Hành chính, Quản lý viện), gắn
 * phạm vi theo người cao tuổi.
 */
class RegistrationProvidersIT extends M02TestBase {

    private static final String ENTITY = "M02.RESIDENCE_REGISTRATION";

    @Autowired
    List<FieldClassificationProvider> classifications;

    @Autowired
    List<RetentionProvider> retentions;

    @Autowired
    List<FilePermissionProvider> filePermissions;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    RegistrationSubmissionStore submissionStore;

    @Autowired
    RegistrationObligationStore obligationStore;

    @Autowired
    RegistrationReminderStore reminderStore;

    @Autowired
    RegistrationResidentStore residentStore;

    private <T> T only(List<T> providers, java.util.function.Predicate<T> match) {
        return providers.stream()
                .filter(match)
                .reduce((a, b) -> {
                    throw new IllegalStateException("Hai provider cùng loại");
                })
                .orElseThrow();
    }

    private RetentionProvider retention() {
        return only(retentions, p -> ENTITY.equals(p.dataType()));
    }

    private FilePermissionProvider evidence() {
        return only(filePermissions, p -> "M02.REGISTRATION_EVIDENCE".equals(p.ownerType()));
    }

    private ScopeAssignmentProvider scope() {
        return only(scopes, p -> ENTITY.equals(p.entityType()));
    }

    /** Khai báo dựng bằng SQL ở {@code simple_record} (không khóa duy nhất, để dựng đủ trạng thái cho một người). */
    private long registration(long residentId, String status, Long replaces) {
        String content = "{\"type\":\"STAY_NOTIFICATION\",\"renewal\":false,\"dueAt\":\"%s\",".formatted(time.now())
                + "\"localPermanentResident\":false"
                + (replaces == null ? "" : ",\"replacedRegistrationId\":" + replaces)
                + "}";
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO simple_record (kind, module, resident_id, status, content, created_at,"
                                    + " created_by, version) VALUES (?, 'M02', ?, ?, ?, UTC_TIMESTAMP(6), 1, 0)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, RegistrationSql.REGISTRATION);
                    ps.setLong(2, residentId);
                    ps.setString(3, status);
                    ps.setString(4, content);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    private void cancelAdmission(long residentId) {
        statusService.cancelAdmission(
                ResidentStatusService.target(residentId),
                new Reason("Gia đình rút hồ sơ"),
                JsonNodeFactory.instance.objectNode());
    }

    @Test
    void FR_049_fieldClassification_isRegistered_withNoRestrictedField() {
        assertThat(only(classifications, p -> ENTITY.equals(p.entityType())).fieldGroups())
                .isEmpty();
    }

    @Test
    void FR_034_scope_carriesTheResidentOfTheRegistration() {
        long residentId = newResident();
        long id = registration(residentId, "TO_SUBMIT", null);

        assertThat(scope().assignScope(id)).isEqualTo(new Target(ENTITY, String.valueOf(id), residentId, null));
        assertThatThrownBy(() -> scope().assignScope(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void NFR_08_evidence_isDownloadableByOfficeStaffAndManager_only() {
        long id = registration(newResident(), "TO_SUBMIT", null);

        user.set(office.user());
        assertThat(evidence().canDownload(id)).as("Hành chính").isTrue();
        user.set(users.staff(Role.NURSING_HOME_MANAGER).user());
        assertThat(evidence().canDownload(id)).as("Quản lý viện").isTrue();
        user.set(nurse.user());
        assertThat(evidence().canDownload(id)).as("Điều dưỡng").isFalse();
        user.set(users.staff(Role.ACCOUNTANT).user());
        assertThat(evidence().canDownload(id)).as("Kế toán").isFalse();
        user.set(office.user());
        assertThat(evidence().canDownload(Long.MAX_VALUE))
                .as("khai báo không có")
                .isFalse();
    }

    @Test
    void FR_051_registrationsAreKeptPerResidentRecord_fromTheFinalStatus() {
        long ended = admissionResident();
        long old = registration(ended, "CONFIRMED", null);
        long renewal = registration(ended, "TO_SUBMIT", old);
        long active = registration(newResident(), "TO_SUBMIT", null);
        cancelAdmission(ended);

        assertThat(retention().retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
        List<Long> afterCutoff = retention()
                .expiredCandidates(time.now().plus(Duration.ofDays(1)), 100)
                .map(Candidate::id)
                .toList();
        assertThat(afterCutoff).contains(old, renewal).doesNotContain(active);
        assertThat(retention()
                        .expiredCandidates(time.now().minus(Duration.ofDays(1)), 100)
                        .map(Candidate::id)
                        .toList())
                .doesNotContain(old, renewal);
    }

    @Test
    void FR_051a_newerRegistrationIsDeletedFirst_withSubmissions_thenTechnicalRows() {
        long residentId = admissionResident();
        long old = registration(residentId, "CONFIRMED", null);
        long renewal = registration(residentId, "TO_SUBMIT", old);
        transaction.executeWithoutResult(s -> {
            submissionStore.add(
                    residentId, old, time.today(), SubmissionChannel.IN_PERSON, "Công an phường", null, 7, time.now());
            obligationStore.record(residentId, ObligationTrigger.ADMISSION_COMPLETED, time.now(), time.now(), 1);
            reminderStore.claim(ReminderType.CREATE, residentId, time.today());
            residentStore.confirmLocalPermanentResident(residentId, "Căn cứ", 7, time.now());
        });

        assertThat(retention().referencesTo(old)).containsExactly(new DataRef(ENTITY, renewal));
        assertThat(retention().referencesTo(renewal)).isEmpty();

        retention().delete(List.of(renewal));
        assertThat(RegistrationSql.registrationCount(jdbc, residentId)).isOne();
        assertThat(RegistrationSql.obligationCount(jdbc, residentId))
                .as("còn khai báo")
                .isOne();

        retention().delete(List.of(old));
        assertThat(RegistrationSql.registrationCount(jdbc, residentId)).isZero();
        assertThat(RegistrationSql.submissionCount(jdbc, old)).isZero();
        assertThat(RegistrationSql.obligationCount(jdbc, residentId)).isZero();
        assertThat(RegistrationSql.reminderCount(jdbc, "CREATE", residentId)).isZero();
        assertThat(RegistrationSql.flagRows(jdbc, residentId)).isZero();
    }
}
