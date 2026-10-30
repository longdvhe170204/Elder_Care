package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommand;
import com.eldercare.backend.roombedallocation.internal.bed.service.BedService;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

class FR_013_BedAssetStateIT extends StructureTestSupport {
    @Autowired
    ApplicationContext context;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    BedService beds;

    private void apply(BedAssetCommand command, boolean damage, boolean repeated) {
        ReflectionTestUtils.invokeMethod(
                context.getBean("bedAssetStateService"),
                "apply",
                bed,
                command,
                new Basis("FR-013", null, null, null),
                damage,
                repeated);
    }

    private void command(BedAssetCommand command) {
        tx.executeWithoutResult(s -> apply(command, false, false));
    }

    private String state() {
        return jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed);
    }

    private long future() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE','2099-01-01','BASIC','CURRENT')",
                resident,
                bed);
        return resident;
    }

    @Test
    void BR_M03_05_occupiedBedRejectsMaintenanceAndRetirement() {
        jdbc.update("UPDATE bed SET state='OCCUPIED' WHERE id=?", bed);
        for (var command : java.util.List.of(BedAssetCommand.MAINTENANCE, BedAssetCommand.RETIREMENT))
            assertThatThrownBy(() -> command(command))
                    .isInstanceOf(com.eldercare.backend.common.error.ConditionNotMetException.class);
        assertThat(state()).isEqualTo("OCCUPIED");
    }

    @Test
    void FR_013_ordinaryMaintenanceRejectsFutureAllocation() {
        future();
        assertThatThrownBy(() -> command(BedAssetCommand.MAINTENANCE))
                .isInstanceOf(com.eldercare.backend.common.error.ConditionNotMetException.class);
        assertThat(state()).isEqualTo("EMPTY");
    }

    @Test
    void BR_M03_06_maintenanceRestoresEmptyAndRetirementIsTerminal() {
        command(BedAssetCommand.MAINTENANCE);
        assertThat(state()).isEqualTo("UNDER_MAINTENANCE");
        command(BedAssetCommand.READY);
        assertThat(state()).isEqualTo("EMPTY");
        command(BedAssetCommand.RETIREMENT);
        assertThat(state()).isEqualTo("RETIRED");
        assertThatThrownBy(() -> command(BedAssetCommand.READY))
                .isInstanceOf(com.eldercare.backend.common.error.ConditionNotMetException.class);
    }

    @Test
    void Q_246_maintenanceRestoresSuspensionAndItsReason() {
        beds.suspend(bed, Reason.of("Đóng tạm phòng"));
        command(BedAssetCommand.MAINTENANCE);
        command(BedAssetCommand.READY);
        assertThat(state()).isEqualTo("SUSPENDED");
        assertThat(jdbc.queryForObject("SELECT suspension_reason FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("Đóng tạm phòng");
    }

    @Test
    void FR_013a_damageDuringPreparationCancelsInitialFutureAtomically() {
        long resident = future();
        tx.executeWithoutResult(s -> apply(BedAssetCommand.MAINTENANCE, true, false));
        assertThat(state()).isEqualTo("UNDER_MAINTENANCE");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT cancellation_reason FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("Giường hỏng");
    }

    @Test
    void FR_013a_sourceCommandFailureRollsBackCancellationAndState() {
        long resident = future();
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
                    apply(BedAssetCommand.RETIREMENT, false, false);
                    throw new IllegalStateException("Lệnh tài sản lỗi");
                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(state()).isEqualTo("EMPTY");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("FUTURE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, bed))
                .isZero();
    }

    @Test
    void FR_013_commandsNeedTheSourceTransaction() {
        assertThatThrownBy(() -> apply(BedAssetCommand.MAINTENANCE, false, false))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    void FR_013a_scheduledFutureIsNotCancelledWithoutTheRequestApplicationFlow() {
        long resident = future();
        jdbc.update("UPDATE bed_allocation SET basis_type='APPROVAL_REQUEST',basis_id=7 WHERE resident_id=?", resident);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> apply(BedAssetCommand.RETIREMENT, false, false)))
                .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class)
                .hasMessageContaining("theo lịch");
        assertThat(jdbc.queryForObject("SELECT status FROM bed_allocation WHERE resident_id=?", String.class, resident))
                .isEqualTo("FUTURE");
        assertThat(state()).isEqualTo("EMPTY");
    }

    @Test
    void FR_013_readyRejectsAnInactiveRoom() {
        command(BedAssetCommand.MAINTENANCE);
        jdbc.update("UPDATE room SET status='INACTIVE' WHERE id=?", room);
        assertThatThrownBy(() -> command(BedAssetCommand.READY))
                .isInstanceOf(com.eldercare.backend.common.error.ConditionNotMetException.class);
        assertThat(state()).isEqualTo("UNDER_MAINTENANCE");
    }
}
