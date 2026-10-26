package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m03.api.dto.BedStatus;
import com.cambae.backend.m03.internal.bed.domain.BedEvent;
import com.cambae.backend.m03.internal.bed.domain.BedTransition;
import com.cambae.backend.m03.internal.bed.service.BedStateService;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

/** T008: khóa, đổi trạng thái, lịch sử và nhật ký cùng transaction trên MySQL. */
class FR_016_BedStateIT extends MySqlIntegrationTestBase {
    private static final AtomicLong IDS = new AtomicLong(90380000);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BedStateService service;

    @Autowired
    TransactionTemplate transaction;

    @MockitoBean
    AuditLogApi audit;

    @MockitoSpyBean
    BusinessTime time;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    RetentionDatabase retentionDatabase;

    @BeforeEach
    void roomAndMock() {
        reset(audit);
        jdbc.update("INSERT IGNORE INTO area(id,code,name) VALUES(903800,'M03-STATE-A','Khu kiểm trạng thái')");
        jdbc.update(
                "INSERT IGNORE INTO floor(id,area_id,code,name) VALUES(903800,903800,'M03-STATE-T','Tầng kiểm trạng thái')");
        jdbc.update("INSERT IGNORE INTO room_type(id,name,standard_beds) VALUES(903800,'Loại kiểm trạng thái',2)");
        jdbc.update(
                "INSERT IGNORE INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(903800,903800,903800,'M03-STATE-P','Phòng kiểm trạng thái','UNRESTRICTED')");
    }

    @Test
    void FR_016_stateHistoryAndAuditCommitTogether() {
        long id = bed();
        var result = transaction.execute(status -> service.transition(id, command(BedEvent.ALLOCATION_STARTED, null)));
        assertThat(result.after()).isEqualTo(BedStatus.OCCUPIED);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id))
                .isEqualTo("OCCUPIED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, id))
                .isEqualTo(1);
        ArgumentCaptor<AuditLogEntry> entry = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(audit).write(entry.capture());
        assertThat(entry.getValue().entityType()).isEqualTo("M03.BED");
        assertThat(entry.getValue().after()).containsEntry("state", "OCCUPIED");
    }

    @Test
    void FR_047_auditFailureRollsBackBedAndHistory() {
        long id = bed();
        doThrow(new IllegalStateException("Lỗi nhật ký giả lập")).when(audit).write(any());
        assertThatThrownBy(() -> transaction.execute(
                        status -> service.transition(id, command(BedEvent.ALLOCATION_STARTED, null))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id))
                .isEqualTo("EMPTY");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, id))
                .isZero();
    }

    @Test
    void FR_016a_delayedExecutionStoresBothTimesAndReadsConfiguration() {
        long id = bed();
        Instant planned = Instant.parse("2000-01-01T00:00:00Z");
        transaction.execute(status -> service.transition(id, command(BedEvent.SUSPEND, planned)));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, id))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT executed_late FROM bed_state_history WHERE bed_id=?", Boolean.class, id))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT planned_at IS NOT NULL FROM bed_state_history WHERE bed_id=?", Boolean.class, id))
                .isTrue();
    }

    @Test
    void FR_011_invalidTransitionWritesNothing() {
        long id = bed();
        assertThatThrownBy(() ->
                        transaction.execute(status -> service.transition(id, command(BedEvent.RETURNED, null))))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, id))
                .isZero();
    }

    @Test
    void Q_246_maintenanceRestoresThePreviousSuspensionAndItsReason() {
        long id = bed();
        transaction.execute(status -> service.transition(id, command(BedEvent.SUSPEND, null)));
        transaction.execute(status -> service.transition(id, command(BedEvent.ASSET_MAINTENANCE, null)));
        transaction.execute(status -> service.transition(id, command(BedEvent.ASSET_READY, null)));
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id))
                .isEqualTo("SUSPENDED");
        assertThat(jdbc.queryForObject("SELECT suspension_reason FROM bed WHERE id=?", String.class, id))
                .isEqualTo("Kiểm thử trạng thái");
    }

    @Test
    void Q_246_retentionKeepsHistoryNeededToRestoreAnActiveMaintenanceState() {
        long id = bed();
        doReturn(Instant.parse("2000-01-01T00:00:00Z")).when(time).now();
        try {
            transaction.execute(status -> service.transition(id, command(BedEvent.SUSPEND, null)));
            transaction.execute(status -> service.transition(id, command(BedEvent.ASSET_MAINTENANCE, null)));
            var provider = retention.stream()
                    .filter(p -> p.dataType().equals("M03.BED_STATE_HISTORY"))
                    .findFirst()
                    .orElseThrow();
            var ownIds = jdbc.queryForList("SELECT id FROM bed_state_history WHERE bed_id=?", Long.class, id);
            var expired = provider.expiredCandidates(Instant.parse("2001-01-01T00:00:00Z"), 1000)
                    .map(RetentionProvider.Candidate::id)
                    .filter(ownIds::contains)
                    .toList();
            assertThat(expired).isEmpty();
            retentionDatabase.transaction().executeWithoutResult(status -> provider.delete(expired));
            transaction.execute(status -> service.transition(id, command(BedEvent.ASSET_READY, null)));
            assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, id))
                    .isEqualTo("SUSPENDED");
            assertThat(jdbc.queryForObject("SELECT suspension_reason FROM bed WHERE id=?", String.class, id))
                    .isEqualTo("Kiểm thử trạng thái");
        } finally {
            reset(time);
        }
    }

    private BedTransition command(BedEvent event, Instant planned) {
        return new BedTransition(
                event,
                Reason.of("Kiểm thử trạng thái"),
                new Basis("FR-016", null, null, null),
                "TEST",
                1,
                planned,
                1L,
                null,
                null,
                null,
                null,
                null,
                false,
                false);
    }

    private long bed() {
        long id = IDS.incrementAndGet();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,903800,?)", id, "G" + id);
        return id;
    }
}
