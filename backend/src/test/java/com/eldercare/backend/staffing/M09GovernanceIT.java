package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FilePermissionProvider;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

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
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position) VALUES (?,'[Nhân viên provider]','NURSING_STAFF')",
                id);
        return id;
    }

    private long credential(long staff) {
        return AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) SELECT src.staff_id,(SELECT c.id FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE' AND COALESCE(c.m09_legacy_id,c.id)=src.credential_type_id),src.issue_date,src.expiry_date FROM (SELECT ? AS staff_id,1 AS credential_type_id,'2026-01-01' AS issue_date,'2026-12-31' AS expiry_date) src",
                staff);
    }

    private RetentionProvider retention(String type) {
        return retention.stream()
                .filter(p -> p.dataType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void FR_057_credentialFixtureReturnsItsOwnKeyWhenPoolChangesConnection() throws Exception {
        long owner = staff();
        var originalJdbc = jdbc;
        try (var insertConnection = originalJdbc.getDataSource().getConnection();
                var otherConnection = originalJdbc.getDataSource().getConnection()) {
            var insertSource = new SingleConnectionDataSource(insertConnection, true);
            var otherSource = new SingleConnectionDataSource(otherConnection, true);
            new JdbcTemplate(otherSource).queryForObject("SELECT LAST_INSERT_ID(0)", Long.class);
            var switchingSource = mock(DataSource.class);
            when(switchingSource.getConnection()).thenReturn(insertSource.getConnection(), otherSource.getConnection());
            jdbc = new JdbcTemplate(switchingSource);

            long id = credential(owner);

            assertThat(id)
                    .isEqualTo(originalJdbc.queryForObject(
                            "SELECT id FROM credential WHERE staff_id=?", Long.class, owner));
        } finally {
            jdbc = originalJdbc;
        }
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
        long id = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,code,name,status,attributes) SELECT 'M09.PRACTICE_SCOPE','M09',src.code,src.name,src.status,JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'legacyCode',src.code) FROM (SELECT 'TEST-RETENTION-SCOPE' AS code,'[Phạm vi giữ lại]' AS name,'INACTIVE' AS status) src");
        jdbc.update(
                "INSERT INTO catalog_item(catalog_type,module,code,name,status,attributes) VALUES"
                        + " ('M15.PROFESSIONAL_ACTIVITY','M15','TEST-RETENTION-ACTIVITY','[Nghiệp vụ giữ lại]','ACTIVE',"
                        + " JSON_OBJECT('functionCode','PRESCRIPTION','requiredLicenseType','DOCTOR_LICENSE',"
                        + " 'requiresFacilityScope',FALSE,'isDefault',FALSE,'practiceScopeIds',JSON_ARRAY(?)))",
                id);
        assertThat(activities.usesCredentialType("DOCTOR_LICENSE")).isTrue();
        assertThat(activities.usesPracticeScope(id)).isTrue();
        assertThat(retention("M09.PRACTICE_SCOPE").expiredCandidates(Instant.parse("2100-01-01T00:00:00Z"), 100))
                .noneMatch(c -> c.id() == id);
    }

    @Test
    void FR_051_expiredCredentialAndItsScopeLinksAreDeletedTogetherByRetentionAccount() {
        long owner = staff();
        long scope = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,code,name,status,attributes) VALUES ('M09.PRACTICE_SCOPE','M09','TEST-RETENTION-DELETE','[Phạm vi kiểm thử xóa]','ACTIVE',JSON_OBJECT())");
        long id = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date,created_at,practice_scope_ids) SELECT ?,c.id,'2000-01-01','2001-01-01','2000-01-01',JSON_ARRAY(?) FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE' AND COALESCE(c.m09_legacy_id,c.id)=1",
                owner,
                scope);
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_LENGTH(practice_scope_ids) FROM credential WHERE id=?", Integer.class, id))
                .isEqualTo(1);
        var provider = retention("M09.CREDENTIAL");
        assertThat(provider.expiredCandidates(Instant.parse("2020-01-01T00:00:00Z"), 100))
                .anyMatch(c -> c.id() == id);
        retentionDatabase.transaction().executeWithoutResult(status -> provider.delete(List.of(id)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM credential WHERE id=?", Integer.class, id))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM credential WHERE JSON_CONTAINS(practice_scope_ids,CAST(? AS JSON))",
                        Integer.class,
                        scope))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_profile WHERE id=?", Integer.class, owner))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M09.PRACTICE_SCOPE' AND id=?",
                        Integer.class,
                        scope))
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
