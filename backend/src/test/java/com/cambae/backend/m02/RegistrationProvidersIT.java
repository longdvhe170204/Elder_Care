package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.file.FilePermissionProvider;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider.Candidate;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider.DataRef;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.GeneratedKeyHolder;
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

    private long registration(long residentId, String status, Long replaces) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO residence_registration (resident_id, type, due_at, status,"
                                    + " replaced_registration_id) VALUES (?, 'STAY_NOTIFICATION', NOW(6), ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, residentId);
                    ps.setString(2, status);
                    ps.setObject(3, replaces);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    private long count(String table, long residentId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE resident_id = ?", Long.class, residentId);
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
        jdbc.update(
                "INSERT INTO registration_submission (registration_id, submitted_date, channel, receiving_authority,"
                        + " recorded_by, recorded_at) VALUES (?, CURDATE(), 'IN_PERSON', 'Công an phường', 7, NOW(6))",
                old);
        jdbc.update(
                "INSERT INTO registration_obligation (resident_id, trigger_type, trigger_at, due_at, source_id)"
                        + " VALUES (?, 'ADMISSION_COMPLETED', NOW(6), NOW(6), 1)",
                residentId);
        jdbc.update(
                "INSERT INTO registration_reminder (type, resident_id, `date`) VALUES ('CREATE', ?, CURDATE())",
                residentId);
        jdbc.update("INSERT INTO registration_resident (resident_id) VALUES (?)", residentId);

        assertThat(retention().referencesTo(old)).containsExactly(new DataRef(ENTITY, renewal));
        assertThat(retention().referencesTo(renewal)).isEmpty();

        retention().delete(List.of(renewal));
        assertThat(count("residence_registration", residentId)).isOne();
        assertThat(count("registration_obligation", residentId))
                .as("còn khai báo")
                .isOne();

        retention().delete(List.of(old));
        assertThat(count("residence_registration", residentId)).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM registration_submission WHERE registration_id = ?", Long.class, old))
                .isZero();
        assertThat(count("registration_obligation", residentId)).isZero();
        assertThat(count("registration_reminder", residentId)).isZero();
        assertThat(count("registration_resident", residentId)).isZero();
    }
}
