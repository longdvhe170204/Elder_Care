package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.incident.service.IncidentCorrectionProviders;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Import(BR_M05_21_IncidentCorrectionIT.Users.class)
/** T036: đính chính Hủy ghi nhận, đổi loại sự cố; BR-M05-21; trigger chỉ mở đúng hai đường đó. */
@Transactional
class BR_M05_21_IncidentCorrectionIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    List<CorrectableRecordProvider> providers;

    @Autowired
    IncidentCatalogService catalog;

    private CorrectableRecordProvider incidentProvider() {
        return providers.stream()
                .filter(p -> IncidentCorrectionProviders.INCIDENT.equals(p.recordType()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void T036_voidClosedIncidentMovesToCancelled() {
        long id = insert("MEDIUM", false, "CLOSED");
        var provider = incidentProvider();

        provider.afterCorrection(correction(id, CorrectionType.VOID, null));

        assertThat(jdbc.queryForObject("SELECT status FROM incident WHERE id = ?", String.class, id))
                .isEqualTo("CANCELLED");
        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET status = 'CLOSED' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void T036_amendChangesCurrentTypeButKeepsOriginalType() {
        long id = insert("LOW", false, "CLOSED");
        var target = catalog.incidentTypes().stream()
                .filter(type -> type.active() && !"SUSPECTED_INFECTION".equals(type.code()))
                .findFirst()
                .orElseThrow();
        var provider = incidentProvider();
        provider.validateContent(Map.of("incidentTypeId", target.id()));

        provider.afterCorrection(correction(id, CorrectionType.AMEND_CONTENT, Map.of("incidentTypeId", target.id())));

        var row =
                jdbc.queryForMap("SELECT current_type_id, incident_type_id, type_code FROM incident WHERE id = ?", id);
        assertThat(((Number) row.get("current_type_id")).longValue()).isEqualTo(target.id());
        assertThat(((Number) row.get("incident_type_id")).longValue()).isNotEqualTo(target.id());
        assertThat(row.get("type_code")).isEqualTo(target.code());
    }

    @Test
    void T036_correctionContentMustNameAnActiveType() {
        var provider = incidentProvider();
        assertThatThrownBy(() -> provider.validateContent(Map.of("description", "x")))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> provider.validateContent(Map.of("incidentTypeId", -1L)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void T036_closedIncidentOtherHandlingColumnsStayLocked() {
        long id = insert("MEDIUM", false, "CLOSED");
        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET resolution = 'Sửa' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE incident SET description = 'Sửa' WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void BR_M05_21_everEmergencyIncidentOnlyNurseOrDoctorMayCorrect() {
        long id = insert("MEDIUM", true, "CLOSED");
        var provider = incidentProvider();

        assertThat(provider.allows(user(Role.FLOOR_MANAGER), CreationMethod.C, id))
                .isFalse();
        assertThat(provider.allows(user(Role.NURSING_STAFF), CreationMethod.C, id))
                .isTrue();
        assertThat(provider.allows(user(Role.DOCTOR), CreationMethod.C, id)).isTrue();
    }

    @Test
    void BR_M05_21_nonEmergencyIncidentMayBeCorrectedByFloorManager() {
        long id = insert("LOW", false, "CLOSED");
        assertThat(incidentProvider().allows(user(Role.FLOOR_MANAGER), CreationMethod.C, id))
                .isTrue();
    }

    @TestConfiguration
    static class Users {
        @Bean
        SwitchableUser current() {
            return new SwitchableUser();
        }
    }

    private static CurrentUser user(Role role) {
        var user = mock(CurrentUser.class);
        when(user.roles()).thenReturn(Set.of(role));
        return user;
    }

    private static CorrectionView correction(long id, CorrectionType type, Object content) {
        return new CorrectionView(
                1L,
                IncidentCorrectionProviders.INCIDENT,
                id,
                1,
                type,
                content,
                "Ghi nhầm",
                1L,
                null,
                null,
                Instant.now());
    }

    private long insert(String level, boolean wasEmergency, String status) {
        long id = TestUsers.nextId();
        var at = Timestamp.from(Instant.parse("2026-11-03T00:00:00Z"));
        jdbc.update(
                """
                INSERT INTO incident (id, resident_id, incident_type_id, current_type_id,
                  type_code, discovered_at, description, initial_level, level, was_emergency,
                  recorded_by, created_at, confirmed_at, status)
                VALUES (?, ?, ?, ?, 'SUSPECTED_INFECTION', ?, 'Ghi nhận ban đầu', ?, ?, ?, ?, ?, ?, 'NEW')
                """,
                id,
                TestUsers.nextId(),
                TestUsers.nextId(),
                TestUsers.nextId(),
                at,
                level,
                level,
                wasEmergency,
                TestUsers.nextId(),
                at,
                at);
        if (!"NEW".equals(status)) {
            jdbc.update("UPDATE incident SET status = ?, resolution = 'Xong' WHERE id = ?", status, id);
        }
        return id;
    }
}
