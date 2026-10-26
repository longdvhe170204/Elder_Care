package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.ContractStatus;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * 004 T029 (US3; DBR-06, FR-020, FR-023, FR-025a, FR-036): cơ sở dữ liệu tự chặn sửa nội dung hợp đồng và bảng con khi
 * hợp đồng ngoài Nháp hoặc đang chờ duyệt điều khoản; phiên bản nội dung chỉ thêm; các ràng buộc CHECK của bảng hợp đồng.
 */
class ContractContentLockDbIT extends M02TestBase {

    private long draft() {
        return contracts.contract(
                admissionResident(),
                ContractStatus.DRAFT,
                ResidenceType.LONG_TERM_RESIDENTIAL,
                CareLevel.BASIC,
                today());
    }

    private long ofStatus(ContractStatus status) {
        return contracts.contract(
                admissionResident(), status, ResidenceType.LONG_TERM_RESIDENTIAL, CareLevel.BASIC, today());
    }

    private long service() {
        String name = "Dịch vụ khóa " + System.nanoTime();
        jdbc.update("INSERT INTO service (type, name, unit, billing_period) VALUES ('MEAL', ?, 'suất', 'UNIT')", name);
        return jdbc.queryForObject("SELECT id FROM service WHERE name = ?", Long.class, name);
    }

    private int editContent(long id) {
        return jdbc.update("UPDATE contract SET bed_hold_policy = 'Giữ giường 30 ngày' WHERE id = ?", id);
    }

    private int addService(long contractId, long serviceId) {
        return jdbc.update(
                "INSERT INTO contract_service (contract_id, service_id, in_package, quantity) VALUES (?, ?, TRUE, 1)",
                contractId,
                serviceId);
    }

    @Test
    void DBR_06_draft_contentAndChildrenAreEditable() {
        long id = draft();
        long service = service();

        assertThat(editContent(id)).isEqualTo(1);
        assertThat(addService(id, service)).isEqualTo(1);
        assertThat(jdbc.update("UPDATE contract_service SET quantity = 2 WHERE contract_id = ?", id))
                .isEqualTo(1);
        assertThat(jdbc.update("DELETE FROM contract_service WHERE contract_id = ?", id))
                .isEqualTo(1);
    }

    @Test
    void FR_020_outsideDraft_contentIsLocked_forEveryStatus() {
        for (ContractStatus status : ContractStatus.values()) {
            if (status == ContractStatus.DRAFT) {
                continue;
            }
            long id = ofStatus(status);
            assertThatThrownBy(() -> editContent(id)).as(status.name()).isInstanceOf(DataAccessException.class);
            assertThatThrownBy(
                            () -> jdbc.update("UPDATE contract SET end_date = ? WHERE id = ?", today().plusDays(9), id))
                    .as(status.name())
                    .isInstanceOf(DataAccessException.class);
        }
    }

    @Test
    void FR_025a_awaitingTermsApproval_locksADraft() {
        long id = draft();
        jdbc.update("UPDATE contract SET awaiting_terms_approval = TRUE WHERE id = ?", id);

        assertThatThrownBy(() -> editContent(id)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> addService(id, service())).isInstanceOf(DataAccessException.class);

        jdbc.update("UPDATE contract SET awaiting_terms_approval = FALSE WHERE id = ?", id);
        assertThat(editContent(id)).isEqualTo(1);
    }

    @Test
    void FR_023_aLockedContract_stillAcceptsTheNonContentColumns() {
        long id = ofStatus(ContractStatus.ACTIVE);

        assertThat(jdbc.update(
                        "UPDATE contract SET current_end_date = ?, overdue_since = ?, manager_notified_overdue_at = NOW(6)"
                                + " WHERE id = ?",
                        today().plusDays(5),
                        today().plusDays(6),
                        id))
                .isEqualTo(1);
        assertThat(jdbc.update("UPDATE contract SET status = 'ENDED' WHERE id = ?", id))
                .isEqualTo(1);
    }

    @Test
    void FR_023_childrenOfALockedContract_cannotBeInsertedUpdatedOrDeleted() {
        long id = draft();
        long service = service();
        addService(id, service);
        jdbc.update(
                "INSERT INTO contract_absence_policy (contract_id, residence_type, absence_type, from_day, coefficient,"
                        + " hold_bed) VALUES (?, 'LONG_TERM_RESIDENTIAL', 'HOME_VISIT', 1, '100', 'YES')",
                id);
        jdbc.update("UPDATE contract SET status = 'PENDING_SIGNATURE' WHERE id = ?", id);

        assertThatThrownBy(() -> addService(id, service())).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE contract_service SET quantity = 3 WHERE contract_id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM contract_service WHERE contract_id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE contract_absence_policy SET coefficient = '50' WHERE contract_id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM contract_absence_policy WHERE contract_id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO contract_arrival_schedule (contract_id, day_of_week, arrival_time, departure_time)"
                                + " VALUES (?, 1, '07:30', '16:30')",
                        id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_036_recordedUnitPrice_canBeSetOnceEvenWhenTheContractIsLocked() {
        long id = draft();
        addService(id, service());
        jdbc.update("UPDATE contract SET status = 'PENDING_SIGNATURE' WHERE id = ?", id);

        assertThat(jdbc.update("UPDATE contract_service SET recorded_unit_price = 600000 WHERE contract_id = ?", id))
                .isEqualTo(1);
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE contract_service SET recorded_unit_price = 1 WHERE contract_id = ?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_024_contentVersions_areAppendOnly() {
        long id = ofStatus(ContractStatus.ACTIVE);
        String insert = "INSERT INTO contract_content_version (contract_id, sequence_no, effective_from, content,"
                + " residence_type, care_level) VALUES (?, ?, ?, '{}', 'LONG_TERM_RESIDENTIAL', 'BASIC')";
        assertThat(jdbc.update(insert, id, 0, today())).isEqualTo(1);

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE contract_content_version SET content = '{\"a\":1}' WHERE contract_id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM contract_content_version WHERE contract_id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(insert, id, 0, today().plusDays(1)))
                .as("UNIQUE (hợp đồng, số thứ tự)")
                .isInstanceOf(DataAccessException.class);
        // V004_06 bỏ UNIQUE (hợp đồng, hieu_luc_tu): hai phụ lục khác thuộc tính được cùng ngày hiệu lực
        assertThat(jdbc.update(insert, id, 1, today())).isEqualTo(1);
        assertThat(jdbc.update(insert, id, 2, today().plusDays(30))).isEqualTo(1);
    }

    @Test
    void FR_019_checkConstraints_ofTheContractTables() {
        long id = draft();

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE contract SET residence_type = 'SHORT_TERM_RESIDENTIAL', end_date = NULL WHERE id = ?",
                        id))
                .as("nội trú ngắn ngày cần ngày kết thúc")
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE contract SET end_date = ? WHERE id = ?", today().minusDays(1), id))
                .as("ngày kết thúc không sớm hơn ngày bắt đầu")
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE contract SET status = 'TERMINATED' WHERE id = ?", id))
                .as("chấm dứt cần lý do (FR-027)")
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE contract SET status = 'ACTIVE' WHERE id = ?", id))
                .as("hiệu lực cần ngày ký, người đại diện, bản scan (DBR-02)")
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE contract SET no_deposit_required = TRUE, deposit_amount = 1000 WHERE id = ?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO contract_arrival_schedule (contract_id, day_of_week, arrival_time, departure_time)"
                                + " VALUES (?, 1, '16:30', '07:30')",
                        id))
                .as("giờ về sau giờ đến (Q-286)")
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO contract_arrival_schedule (contract_id, arrival_time, departure_time) VALUES (?,"
                                + " '07:30', '16:30')",
                        id))
                .as("thứ trong tuần hoặc ngày cụ thể")
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_019_contractNumber_isUnique() {
        long first = draft();
        long second = draft();
        String number = jdbc.queryForObject("SELECT contract_number FROM contract WHERE id = ?", String.class, first);

        assertThatThrownBy(() -> jdbc.update("UPDATE contract SET contract_number = ? WHERE id = ?", number, second))
                .isInstanceOf(DataAccessException.class);
    }
}
