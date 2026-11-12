package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.retention.scheduling.ExpiredDataPurgeJob;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ConsenterType;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.consent.service.ConsentService;
import com.eldercare.backend.resident.internal.consent.service.ConsentService.RecordConsent;
import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecordContent;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import com.eldercare.backend.resident.internal.health.service.InitialHealthRecordService;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 001 T011, T079 (data-model mục 11; FR-050, FR-051, FR-051a; 000 R-11): M01 đăng ký {@code RetentionProvider} cho hồ sơ
 * người cao tuổi và các bản ghi con. Mọi loại tính hạn theo CFG-M15-03 từ lúc hồ sơ vào trạng thái cuối; hồ sơ còn
 * hoạt động, hồ sơ trạng thái cuối chưa quá hạn, hồ sơ có hồ sơ sau còn hoạt động trỏ tới không bao giờ bị xóa. Khi xóa:
 * cả chuỗi (lịch sử, đồng ý, sức khỏe, đánh giá, phiếu nguyện vọng, nhắc, đính chính, nhật ký) cùng một transaction.
 * Đồng hồ giả lập; dữ liệu tạo bằng lệnh thật rồi đẩy đồng hồ tới sau hạn.
 */
@Import({FakeFileConfig.class, CostParticipantOff.class})
class ResidentRetentionIT extends StatusTestBase {

    private static final Reason REASON = new Reason("Kiểm thử lưu giữ");

    @Autowired
    FileApi files;

    @Autowired
    HealthItemService items;

    @Autowired
    InitialHealthRecordService records;

    @Autowired
    ConsentService consents;

    @Autowired
    EndOfLifeWishService wishes;

    @Autowired
    TreatmentWishService treatments;

    @Autowired
    CorrectionApi corrections;

    @Autowired
    FakeFamilyRelationships families;

    @Autowired
    ExpiredDataPurgeJob job;

    /** Provider hồ sơ M01 thật; ca rollback giả lập lỗi khi xóa hồ sơ. */
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean(name = "residentRetention")
    com.eldercare.backend.administration.retention.api.spi.RetentionProvider residentRetention;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private long pdf() {
        return files.upload(
                "ban-ky.pdf", FileTypes.PDF, "%PDF-1.4 thử".getBytes(StandardCharsets.UTF_8), office.accountId());
    }

    /** Gắn tệp như lệnh của module (FileApi.attach chỉ chạy trong transaction của lệnh). */
    private long attach(long fileId, String ownerType, long ownerId) {
        jdbc.update(
                "UPDATE file SET owner_type = ?, owner_id = ?, attached_at = uploaded_at WHERE id = ?",
                ownerType,
                ownerId,
                fileId);
        return fileId;
    }

    /** Người cao tuổi có đủ loại bản ghi của M01, đã Kết thúc lưu trú vào lúc đồng hồ hiện tại. */
    private long endedResidentWithEverything() {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        confirmed(id, AssessmentType.INITIAL, CareLevel.REGULAR, "Nhận xét", "Lưu ý");
        setStatus(id, "IN_RESIDENCE");
        long assessmentId =
                jdbc.queryForObject("SELECT id FROM assessment WHERE resident_id = ? LIMIT 1", Long.class, id);

        user.set(doctor.user());
        items.record(
                target(id),
                new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false));
        assignNurse(nurse, id);
        user.set(nurse.user());
        var draft = records.saveDraft(
                target(id),
                null,
                new InitialHealthRecordContent(
                        "O+", "Đi lại khó", "Tự vệ sinh", "Tỉnh", "Ghi", "Ít muối", "Thuốc A", "Tập"));
        user.set(doctor.user());
        records.confirm(target(id), "\"" + draft.version() + "\"");

        user.set(office.user());
        long relative = TestUsers.nextId();
        families.relate(relative, id);
        consents.record(
                target(id),
                new RecordConsent(
                        ConsenterType.RESIDENT, null, null, Set.of(relative), true, true, Instant.now(), pdf()));

        user.set(nurse.user());
        wishes.record(
                target(id), new WishCommandBuilder(id, doctor.accountId(), pdf()).build(), WishRecordSource.ADMISSION);
        treatments.record(
                target(id), new TreatmentCommandBuilder().notify(relative).build());

        user.set(doctor.user());
        corrections.create(
                "M01.ASSESSMENT", assessmentId, CorrectionType.AMEND_CONTENT, Map.of("careNote", "Lưu ý đúng"), REASON);

        jdbc.update(
                "INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at) VALUES"
                        + " ('M01.PERIODIC_REMINDER.MISSING_CONSENT', 'M01.RESIDENT', ?, 'START:0', ?)",
                id,
                java.sql.Timestamp.from(time.now()));
        com.eldercare.backend.support.ResidentHistoryRows.insertNotice(
                jdbc, id, "PERIODIC", "ASSESSMENT", assessmentId, time.now());

        user.set(office.user());
        statusService.grantTemporaryAbsence(target(id), REASON, noDetails());
        // đính chính lý do của dòng lịch sử (cách A, người ghi gốc): job xóa cùng chuỗi hồ sơ
        corrections.create(
                "M01.RESIDENT_STATUS_HISTORY",
                lastStatusHistoryId(id),
                CorrectionType.AMEND_CONTENT,
                Map.of("reason", "Lý do đúng"),
                REASON);
        statusService.recordReturn(target(id), REASON, noDetails());
        statusService.endResidency(target(id), REASON, noDetails());
        return id;
    }

    private int count(String table, long residentId) {
        if (table.startsWith("simple_record:")) {
            // lần báo đánh giá lại ở simple_record từ 001 T084
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM simple_record WHERE kind = ? AND resident_id = ?",
                    Integer.class,
                    table.substring("simple_record:".length()),
                    residentId);
        }
        if (table.startsWith("status_history:")) {
            // lịch sử trạng thái, mức chăm sóc ở status_history từ 001 T084
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM status_history WHERE entity_type = 'M01.RESIDENT' AND entity_id = ?"
                            + " AND attribute = ?",
                    Integer.class,
                    residentId,
                    table.substring("status_history:".length()));
        }
        String column = table.equals("resident") ? "id" : table.equals("reminder_marker") ? "entity_id" : "resident_id";
        String scope = table.equals("reminder_marker") ? " AND entity_type = 'M01.RESIDENT'" : "";
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?" + scope, Integer.class, residentId);
    }

    private long lastStatusHistoryId(long residentId) {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM status_history WHERE entity_type = 'M01.RESIDENT' AND attribute = 'STATUS'"
                        + " AND entity_id = ?",
                Long.class,
                residentId);
    }

    private static final List<String> TABLES = List.of(
            "resident",
            "status_history:STATUS",
            "status_history:CARE_LEVEL",
            "simple_record:M01.INITIAL_HEALTH_RECORD",
            "health_item",
            "assessment",
            "consent",
            "wish",
            "simple_record:M01.REASSESSMENT_NOTICE",
            "reminder_marker");

    private void runJobAfter(Duration sinceNow) {
        clock.setTo(Instant.now().plus(sinceNow));
        user.set(FakePermissions.user(0, Role.SCHEDULER));
        job.run();
    }

    @Test
    void FR_051_endedResidentPastCfgM1503_isDeletedWithTheWholeChain() {
        long id = endedResidentWithEverything();
        long assessmentId =
                jdbc.queryForObject("SELECT id FROM assessment WHERE resident_id = ? LIMIT 1", Long.class, id);
        String historyCorrections =
                "SELECT COUNT(*) FROM correction c JOIN status_history h ON h.id = c.original_record_id"
                        + " WHERE c.record_type = 'M01.RESIDENT_STATUS_HISTORY' AND h.entity_type = 'M01.RESIDENT'"
                        + " AND h.entity_id = ?";
        assertThat(jdbc.queryForObject(historyCorrections, Integer.class, id)).isEqualTo(1);
        java.util.List<Long> historyIds = jdbc.queryForList(
                "SELECT id FROM status_history WHERE entity_type = 'M01.RESIDENT' AND entity_id = ?", Long.class, id);
        for (String table : TABLES) {
            assertThat(count(table, id)).as("trước khi xóa: %s", table).isPositive();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE resident_id = ?", Integer.class, id))
                .isPositive();
        // tệp có loại chủ sở hữu khác loại bản ghi (FilePermissionProvider.recordType): ảnh gắn hồ sơ, giấy tờ bệnh
        // viện
        // gắn dòng lịch sử trạng thái; cùng tệp bản ký của bản đồng ý (loại chủ sở hữu trùng loại bản ghi)
        long photo = attach(pdf(), "M01.RESIDENT_PHOTO", id);
        long hospitalDocument = attach(pdf(), "M01.HOSPITAL_DOCUMENT", lastStatusHistoryId(id));
        String residentFiles = "SELECT COUNT(*) FROM file WHERE id IN (" + photo + ", " + hospitalDocument
                + ") OR (owner_type = 'M01.CONSENT' AND owner_id IN (SELECT id FROM consent WHERE resident_id = " + id
                + "))";
        assertThat(jdbc.queryForObject(residentFiles, Integer.class)).isEqualTo(3);

        runJobAfter(Duration.ofDays(11 * 366));

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM file WHERE id IN (" + photo + ", " + hospitalDocument + ")",
                        Integer.class))
                .as("tệp gắn hồ sơ, dòng lịch sử (FR-051a)")
                .isZero();

        for (String table : TABLES) {
            assertThat(count(table, id)).as("sau khi xóa: %s", table).isZero();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction WHERE record_type = 'M01.ASSESSMENT' AND original_record_id = ?",
                        Integer.class,
                        assessmentId))
                .as("đính chính của hồ sơ")
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction WHERE record_type = 'M01.RESIDENT_STATUS_HISTORY'"
                                + " AND original_record_id IN ("
                                + historyIds.stream()
                                        .map(String::valueOf)
                                        .collect(java.util.stream.Collectors.joining(","))
                                + ")",
                        Integer.class))
                .as("đính chính của dòng lịch sử trạng thái")
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment WHERE resident_id = ?", Integer.class, id))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M15.PURGE_RUN'", Integer.class))
                .as("bản tóm tắt đợt xóa")
                .isPositive();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE resident_id = ? AND entity_type LIKE 'M01.%'",
                        Integer.class, id))
                .as("nhật ký gắn hồ sơ hết hạn cùng chuỗi (AuditLogRetention, FR-050)")
                .isZero();
    }

    /**
     * 010 T033 (FR-051a, data-model 010 mục 10; 000 T112 {@code referencing}): hồ sơ thật của 001 cùng bảng chi phí, khoản và
     * khoản điều chỉnh của 010; job chưa xóa khi hồ sơ chưa hết hạn, hết hạn thì xóa cả chuỗi (điều chỉnh trước khoản gốc,
     * khoản trước bảng, bảng trước hồ sơ, khóa ngoại không chặn).
     */
    @Test
    void T033_FR_051a_costStatementsAndItemsFollowTheRealResidentChain() {
        long id = endedResidentWithEverything();
        long period = TestUsers.nextId(), statement = TestUsers.nextId();
        long item = TestUsers.nextId(), adjustment = TestUsers.nextId();
        long seed = Math.floorMod(period, 72000);
        var month = java.time.YearMonth.of(2000 + (int) seed / 12, 1 + (int) seed % 12);
        var now = java.sql.Timestamp.from(time.now());
        jdbc.update(
                "INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,unique_key,created_at)"
                        + " VALUES (?,'PERIOD',?,?,?,?,?)",
                period,
                month.toString(),
                month.atDay(1),
                month.atEndOfMonth(),
                "PERIOD:" + month,
                now);
        jdbc.update(
                "INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)"
                        + " VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)",
                statement,
                period,
                id,
                "STATEMENT:" + period + ":" + id + ":REGULAR",
                now);
        String insertItem = "INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,description,"
                + "incurred_on,quantity,unit,amount,reason,original_item_id,original_period_id,created_at,created_by)"
                + " VALUES (?,?,?,?,'OTHER',?,'[Khoản kiểm chuỗi lưu giữ]',?,1,'UNIT',?,'Kiểm chuỗi',?,?,?,1)";
        jdbc.update(insertItem, item, id, statement, period, "MANUAL", month.atDay(1), 300, null, null, now);
        jdbc.update(
                insertItem, adjustment, id, statement, period, "ADJUSTMENT", month.atDay(1), -100, item, period, now);
        String costRows = "SELECT (SELECT COUNT(*) FROM cost_statement WHERE id=?) + (SELECT COUNT(*) FROM cost_item"
                + " WHERE resident_id=?)";

        runJobAfter(Duration.ofDays(366));
        assertThat(count("resident", id)).as("hồ sơ chưa hết hạn").isEqualTo(1);
        assertThat(jdbc.queryForObject(costRows, Integer.class, statement, id))
                .as("bảng, khoản chi phí giữ cùng hồ sơ")
                .isEqualTo(3);

        runJobAfter(Duration.ofDays(11 * 366));
        assertThat(count("resident", id)).isZero();
        assertThat(jdbc.queryForObject(costRows, Integer.class, statement, id))
                .as("bảng, khoản, khoản điều chỉnh xóa cùng chuỗi hồ sơ")
                .isZero();
    }

    /** Mã bảng của người và giao dịch nộp tiền dựng cho hồ sơ. */
    private record CostAndBalance(long statement, long cashPayment) {}

    /**
     * Bảng chi phí kỳ tháng riêng, bảng của người, khoản gốc 300 và khoản điều chỉnh -100 (010); sổ số dư và ba giao dịch
     * Đã xác nhận (017): nộp tiền mặt, thanh toán bảng (trỏ bảng của 010), điều chỉnh trỏ giao dịch nộp tiền.
     */
    private CostAndBalance costAndBalanceRows(long id) {
        long period = TestUsers.nextId(), statement = TestUsers.nextId();
        long item = TestUsers.nextId(), adjustment = TestUsers.nextId();
        long seed = Math.floorMod(period, 72000);
        var month = java.time.YearMonth.of(2000 + (int) seed / 12, 1 + (int) seed % 12);
        var now = java.sql.Timestamp.from(time.now());
        jdbc.update(
                "INSERT INTO cost_statement(id,kind,period_month,period_starts_on,period_ends_on,unique_key,created_at)"
                        + " VALUES (?,'PERIOD',?,?,?,?,?)",
                period,
                month.toString(),
                month.atDay(1),
                month.atEndOfMonth(),
                "PERIOD:" + month,
                now);
        jdbc.update(
                "INSERT INTO cost_statement(id,kind,period_id,resident_id,statement_type,unique_key,created_at)"
                        + " VALUES (?,'STATEMENT',?,?,'REGULAR',?,?)",
                statement,
                period,
                id,
                "STATEMENT:" + period + ":" + id + ":REGULAR",
                now);
        String insertItem = "INSERT INTO cost_item(id,resident_id,statement_id,period_id,cost_type,origin,description,"
                + "incurred_on,quantity,unit,amount,reason,original_item_id,original_period_id,created_at,created_by)"
                + " VALUES (?,?,?,?,'OTHER',?,'[Khoản kiểm chuỗi lưu giữ]',?,1,'UNIT',?,'Kiểm chuỗi',?,?,?,1)";
        jdbc.update(insertItem, item, id, statement, period, "MANUAL", month.atDay(1), 300, null, null, now);
        jdbc.update(
                insertItem, adjustment, id, statement, period, "ADJUSTMENT", month.atDay(1), -100, item, period, now);
        jdbc.update(
                "INSERT INTO resident_balance(resident_id,opened_at,opening_contract_id,payment_code,balance)"
                        + " VALUES (?,?,?,?,300)",
                id,
                now,
                TestUsers.nextId(),
                "K" + Math.floorMod(TestUsers.nextId(), 1_000_000_000_000L));
        String insertTransaction = "INSERT INTO balance_transaction(resident_id,ledger,type,amount,occurred_at,method,"
                + "cost_statement_id,original_transaction_id,reason,status,locked_at,confirmed_at,created_at,created_by)"
                + " VALUES (?,'BALANCE',?,?,?,?,?,?,?,'CONFIRMED',?,?,?,1)";
        jdbc.update(insertTransaction, id, "CASH_PAYMENT", 500, now, "CASH", null, null, null, now, now, now);
        long cash = jdbc.queryForObject(
                "SELECT MAX(id) FROM balance_transaction WHERE resident_id=? AND type='CASH_PAYMENT'", Long.class, id);
        jdbc.update(
                insertTransaction,
                id,
                "STATEMENT_PAYMENT",
                -200,
                now,
                "INTERNAL",
                statement,
                null,
                null,
                now,
                now,
                now);
        jdbc.update(
                insertTransaction, id, "ADJUSTMENT", -50, now, "OTHER", null, cash, "Ghi nhầm số tiền", now, now, now);
        // Cặp cấn trừ cọc ↔ số dư (017) trỏ nhau hai chiều qua linked_transaction_id, cả hai đã khóa: chỉ nối được vòng
        // khi dòng đầu chưa khóa, rồi khóa.
        jdbc.update(
                "INSERT INTO balance_transaction(resident_id,ledger,type,amount,occurred_at,method,status,created_at,"
                        + "created_by) VALUES (?,'DEPOSIT','DEPOSIT_OFFSET',-100,?,'INTERNAL','PENDING_APPROVAL',?,1)",
                id,
                now,
                now);
        long offset = jdbc.queryForObject(
                "SELECT MAX(id) FROM balance_transaction WHERE resident_id=? AND type='DEPOSIT_OFFSET'",
                Long.class,
                id);
        jdbc.update(
                "INSERT INTO balance_transaction(resident_id,ledger,type,amount,occurred_at,method,linked_transaction_id,"
                        + "status,locked_at,confirmed_at,created_at,created_by)"
                        + " VALUES (?,'BALANCE','DEPOSIT_OFFSET',100,?,'INTERNAL',?,'CONFIRMED',?,?,?,1)",
                id,
                now,
                offset,
                now,
                now,
                now);
        long credit = jdbc.queryForObject(
                "SELECT MAX(id) FROM balance_transaction WHERE resident_id=? AND ledger='BALANCE'"
                        + " AND type='DEPOSIT_OFFSET'",
                Long.class,
                id);
        jdbc.update(
                "UPDATE balance_transaction SET status='CONFIRMED', linked_transaction_id=?, locked_at=?, confirmed_at=?"
                        + " WHERE id=?",
                credit,
                now,
                now,
                offset);
        return new CostAndBalance(statement, cash);
    }

    /** Số dòng chi phí (bảng của người, khoản) và số dư (sổ, giao dịch) còn của hồ sơ. */
    private int costAndBalanceCount(long id, CostAndBalance rows) {
        return jdbc.queryForObject(
                "SELECT (SELECT COUNT(*) FROM cost_statement WHERE id=?)"
                        + " + (SELECT COUNT(*) FROM cost_item WHERE resident_id=?)"
                        + " + (SELECT COUNT(*) FROM resident_balance WHERE resident_id=?)"
                        + " + (SELECT COUNT(*) FROM balance_transaction WHERE resident_id=?)",
                Integer.class,
                rows.statement(),
                id,
                id,
                id);
    }

    /**
     * 010 T033 (FR-051a, data-model 010 mục 10, 017 mục 8; 000 T112 {@code referencing}): sổ và giao dịch số dư của 017
     * ({@code BalanceRetention}) cùng bảng, khoản của 010 trong chuỗi hồ sơ thật của 001: chưa hết hạn thì giữ cả chuỗi;
     * hết hạn thì xóa cả chuỗi (giao dịch điều chỉnh trước giao dịch được trỏ, giao dịch trước sổ, khóa ngoại không chặn).
     */
    @Test
    void T033_FR_051a_balanceLedgerAndTransactionsFollowTheRealResidentChainWithCosts() {
        long id = endedResidentWithEverything();
        var rows = costAndBalanceRows(id);
        assertThat(costAndBalanceCount(id, rows)).isEqualTo(9);

        runJobAfter(Duration.ofDays(366));
        assertThat(count("resident", id)).as("hồ sơ chưa hết hạn").isEqualTo(1);
        assertThat(costAndBalanceCount(id, rows))
                .as("chi phí, sổ, giao dịch giữ cùng hồ sơ")
                .isEqualTo(9);

        runJobAfter(Duration.ofDays(11 * 366));
        assertThat(count("resident", id)).isZero();
        assertThat(costAndBalanceCount(id, rows))
                .as("bảng, khoản, sổ, giao dịch xóa cùng chuỗi hồ sơ")
                .isZero();
    }

    /**
     * 010 T033 (FR-051, data-model 010 mục 10: rollback nguyên tử): lỗi khi xóa hồ sơ, bước cuối của chuỗi, sau khi đã xóa
     * khoản, bảng, giao dịch, sổ trong cùng transaction → không dòng nào mất, đợt ghi dừng giữa chừng; lần chạy sau (hết
     * lỗi) xóa cả chuỗi.
     */
    @Test
    void T033_FR_051_failureMidChainWithCostRows_rollsBackTheWholeChain() {
        long id = endedResidentWithEverything();
        var rows = costAndBalanceRows(id);
        org.mockito.Mockito.doThrow(
                        new org.springframework.dao.DataIntegrityViolationException("[lỗi giả lập khi xóa hồ sơ]"))
                .when(residentRetention)
                .delete(org.mockito.ArgumentMatchers.argThat(ids -> ids != null && ids.contains(id)));
        try {
            runJobAfter(Duration.ofDays(11 * 366));
            assertThat(count("resident", id)).as("hồ sơ").isEqualTo(1);
            for (String table : TABLES) {
                assertThat(count(table, id)).as("rollback: %s", table).isPositive();
            }
            assertThat(costAndBalanceCount(id, rows))
                    .as("rollback: chi phí, sổ, giao dịch")
                    .isEqualTo(9);
            assertThat(jdbc.queryForObject(
                            "SELECT JSON_EXTRACT(details, '$.stopped') FROM audit_log"
                                    + " WHERE entity_type = 'M15.PURGE_RUN' ORDER BY id DESC LIMIT 1",
                            String.class))
                    .as("đợt dừng giữa chừng")
                    .isEqualTo("true");
        } finally {
            org.mockito.Mockito.reset(residentRetention);
        }

        runJobAfter(Duration.ofDays(11 * 366));
        assertThat(count("resident", id)).isZero();
        assertThat(costAndBalanceCount(id, rows)).isZero();
    }

    private static String ResidentProfileTestBase_citizenId() {
        return String.valueOf(300_000_000_000L + (System.nanoTime() % 99_999_999_999L));
    }

    private static com.eldercare.backend.resident.internal.profile.domain.PersonalInfo personal(
            String name, String cccd) {
        return new com.eldercare.backend.resident.internal.profile.domain.PersonalInfo(
                name + " " + cccd,
                java.time.LocalDate.of(1940, 1, 1),
                com.eldercare.backend.resident.api.dto.Gender.MALE,
                com.eldercare.backend.resident.internal.profile.domain.IdDocumentType.CITIZEN_ID,
                cccd,
                null,
                null,
                null,
                null,
                null);
    }
}
