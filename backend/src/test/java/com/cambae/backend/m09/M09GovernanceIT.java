package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.file.FilePermissionProvider;
import com.cambae.backend.m15.account.api.ProfessionalActivityApi;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import com.cambae.backend.support.FakePermissions;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/** T009: provider thật, chỉ thay danh tính hiện tại để kiểm quyền tải tệp; không giả quyền vai trò. */
@Import(M09GovernanceIT.Users.class)
class M09GovernanceIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    List<FieldClassificationProvider> fields;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    List<FilePermissionProvider> files;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    ProfessionalActivityApi activities;

    @Autowired
    SwitchableUser user;

    @Autowired
    RetentionDatabase retentionDatabase;

    private long staff() {
        jdbc.update("INSERT INTO staff_profile(full_name,position) VALUES ('[Nhân viên provider]','NURSING_STAFF')");
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long credential(long staff) {
        jdbc.update(
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) VALUES (?,1,'2026-01-01','2026-12-31')",
                staff);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private RetentionProvider retention(String type) {
        return retention.stream()
                .filter(p -> p.dataType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void FR_052_auditClassificationSeparatesContactPrivateAndHealthData() {
        assertThat(fields.stream()
                        .filter(p -> p.entityType().equals("M09.STAFF"))
                        .findFirst()
                        .orElseThrow()
                        .fieldGroups())
                .containsEntry("phoneNumber", InfoGroup.STAFF_CONTACT)
                .containsEntry("terminationReason", InfoGroup.STAFF_RESTRICTED);
        assertThat(fields.stream()
                        .filter(p -> p.entityType().equals("M09.CREDENTIAL"))
                        .findFirst()
                        .orElseThrow()
                        .fieldGroups())
                .containsEntry("credentialNumber", InfoGroup.STAFF_RESTRICTED);
        assertThat(fields.stream()
                        .filter(p -> p.entityType().equals("M09.STAFF_ABSENCE"))
                        .findFirst()
                        .orElseThrow()
                        .fieldGroups())
                .containsEntry("reason", InfoGroup.STAFF_REASON);
        assertThat(fields.stream()
                        .filter(p -> p.entityType().equals("M09.HANDOVER"))
                        .findFirst()
                        .orElseThrow()
                        .fieldGroups())
                .containsEntry("notes", InfoGroup.HEALTH);
    }

    @Test
    void FR_052_scanDownloadIsOnlyForManagerOrCredentialOwner() {
        long owner = staff();
        long id = credential(owner);
        var provider = files.stream()
                .filter(p -> p.ownerType().equals("M09.CREDENTIAL_SCAN"))
                .findFirst()
                .orElseThrow();
        user.set(FakePermissions.user(owner, Role.NURSING_STAFF));
        assertThat(provider.canDownload(id)).isTrue();
        user.set(FakePermissions.user(owner + 100000, Role.NURSING_STAFF));
        assertThat(provider.canDownload(id)).isFalse();
        user.set(FakePermissions.user(owner + 100000, Role.NURSING_HOME_MANAGER));
        assertThat(provider.canDownload(id)).isTrue();
        assertThat(provider.canDownload(Long.MAX_VALUE)).isFalse();
    }

    @Test
    void FR_034_staffAndCredentialHaveScopeProviders() {
        long owner = staff();
        long id = credential(owner);
        assertThat(scopes.stream()
                        .filter(p -> p.entityType().equals("M09.STAFF"))
                        .findFirst()
                        .orElseThrow()
                        .assignScope(owner)
                        .id())
                .isEqualTo(Long.toString(owner));
        assertThat(scopes.stream()
                        .filter(p -> p.entityType().equals("M09.CREDENTIAL"))
                        .findFirst()
                        .orElseThrow()
                        .assignScope(id)
                        .type())
                .isEqualTo("M09.CREDENTIAL");
    }

    @Test
    void FR_051_staffRetentionKeepsCredentialReferences() {
        long owner = staff();
        long id = credential(owner);
        assertThat(retention("M09.STAFF").referencesTo(owner))
                .contains(new RetentionProvider.DataRef("M09.CREDENTIAL", id));
        assertThat(retention("M09.STAFF").expiredCandidates(Instant.parse("2100-01-01T00:00:00Z"), 100))
                .noneMatch(c -> c.id() == owner);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void FR_051_professionalActivityReferencesPreventCatalogPurge() {
        jdbc.update(
                "INSERT INTO practice_scope(code,name,status) VALUES ('TEST-RETENTION-SCOPE','[Phạm vi giữ lại]','INACTIVE')");
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO professional_activity(code,name,function_code,required_license_type,requires_facility_scope) VALUES ('TEST-RETENTION-ACTIVITY','[Nghiệp vụ giữ lại]','PRESCRIPTION','DOCTOR_LICENSE',FALSE)");
        long activity = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO professional_activity_practice_scope(activity_id,practice_scope_id) VALUES (?,?)",
                activity,
                id);
        assertThat(activities.usesCredentialType("DOCTOR_LICENSE")).isTrue();
        assertThat(activities.usesPracticeScope(id)).isTrue();
        assertThat(retention("M09.PRACTICE_SCOPE").expiredCandidates(Instant.parse("2100-01-01T00:00:00Z"), 100))
                .noneMatch(c -> c.id() == id);
    }

    @Test
    void FR_051_expiredCredentialAndItsScopeLinksAreDeletedTogetherByRetentionAccount() {
        long owner = staff();
        jdbc.update(
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date,created_at) VALUES (?,1,'2000-01-01','2001-01-01','2000-01-01')",
                owner);
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO practice_scope(code,name) VALUES ('TEST-RETENTION-DELETE','[Phạm vi kiểm thử xoá]')");
        long scope = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO credential_practice_scope(credential_id,practice_scope_id) VALUES (?,?)", id, scope);
        var provider = retention("M09.CREDENTIAL");
        assertThat(provider.expiredCandidates(Instant.parse("2020-01-01T00:00:00Z"), 100))
                .anyMatch(c -> c.id() == id);
        retentionDatabase.transaction().executeWithoutResult(status -> provider.delete(List.of(id)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credential WHERE id=?", Integer.class, id))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM credential_practice_scope WHERE credential_id=?", Integer.class, id))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_profile WHERE id=?", Integer.class, owner))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM practice_scope WHERE id=?", Integer.class, scope))
                .isEqualTo(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser m09User() {
            return new SwitchableUser();
        }
    }
}
