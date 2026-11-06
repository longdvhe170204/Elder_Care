package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T042/T050: giữ đủ 11 loại bản gốc; JSON nằm trong bàn giao, các dòng chung phân biệt theo loại/id. */
class M09RecordProtectionIT extends MySqlIntegrationTestBase {
    private static final Set<String> RECORD_TABLES =
            Set.of("staff_department", "status_history", "record_event", "simple_record", "shift_handover");

    private record ProtectedRow(String kind, String table, long id, String field) {}

    @Test
    void DBR_23_allJdbcRecordTablesHaveBeforeUpdateAndDeleteGuards() throws SQLException {
        try (var c = adminConnection();
                var st = c.createStatement();
                var rows = st.executeQuery(
                        "SELECT EVENT_OBJECT_TABLE,EVENT_MANIPULATION,ACTION_TIMING FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE()")) {
            var guarded = new HashMap<String, Set<String>>();
            while (rows.next())
                if ("BEFORE".equals(rows.getString(3)))
                    guarded.computeIfAbsent(rows.getString(1), ignored -> new HashSet<>())
                            .add(rows.getString(2));
            for (String table : RECORD_TABLES)
                assertThat(guarded.get(table)).as(table).contains("UPDATE", "DELETE");
        }
    }

    @Test
    void DBR_23_originalRecordsRemainImmutableEvenForAdmin_andFixturesRollBack() throws SQLException {
        try (var c = adminConnection()) {
            c.setAutoCommit(false);
            try {
                var records = records(c);
                assertThat(records).hasSize(11);
                assertThat(records.stream().map(ProtectedRow::table).collect(java.util.stream.Collectors.toSet()))
                        .isEqualTo(RECORD_TABLES);
                try (var st = c.createStatement()) {
                    for (var row : records) {
                        assertThatThrownBy(() -> st.executeUpdate("UPDATE " + row.table() + " SET " + row.field() + "="
                                        + row.field() + " WHERE id=" + row.id()))
                                .as("UPDATE %s", row.kind())
                                .isInstanceOf(SQLException.class)
                                .hasFieldOrPropertyWithValue("SQLState", "45000");
                        assertThatThrownBy(
                                        () -> st.executeUpdate("DELETE FROM " + row.table() + " WHERE id=" + row.id()))
                                .as("DELETE %s", row.kind())
                                .isInstanceOf(SQLException.class)
                                .hasFieldOrPropertyWithValue("SQLState", "45000");
                    }
                }
            } finally {
                c.rollback();
            }
        }
    }

    private List<ProtectedRow> records(Connection c) throws SQLException {
        var rows = new ArrayList<ProtectedRow>();
        long staff = insert(c, "INSERT INTO staff_profile(full_name,position) VALUES ('[Kiểm bất biến]','Điều dưỡng')");
        long department = insert(
                c,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES ('M09.DEPARTMENT','M09','[Bộ phận bảo vệ "
                        + TestUsers.nextId() + "]','ACTIVE',JSON_OBJECT())");
        long depPeriod = insert(
                c,
                "INSERT INTO staff_department(staff_id,department_id,starts_at,ends_at,actor_id,reason) VALUES ("
                        + staff + "," + department + ",'2026-10-18','2026-10-19',1,'Lịch sử đã đóng')");
        rows.add(new ProtectedRow("STAFF_DEPARTMENT", "staff_department", depPeriod, "id"));
        long depLead = insert(
                c,
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,ends_at,command,actor_id,by_system,reason) VALUES ('M09.DEPARTMENT',"
                        + department + ",'LEAD_STAFF','M09','PERIOD'," + staff
                        + ",'2026-10-18','2026-10-19','LEAD_CHANGED',1,FALSE,'Lịch sử đã đóng')");
        rows.add(new ProtectedRow("DEPARTMENT_LEAD_HISTORY", "status_history", depLead, "id"));
        long template = insert(
                c,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES ('M09.SHIFT_TEMPLATE','M09','[Mẫu bảo vệ "
                        + TestUsers.nextId()
                        + "]','ACTIVE',JSON_OBJECT('startTime','07:00:00','endTime','15:00:00','scopeType','FLOOR','requiresHandover',CAST('true' AS JSON)))");
        long schedule = insert(
                c, "INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES ('2026-10','FLOOR'," + template + ")");
        long shift = insert(
                c,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES ("
                        + schedule + "," + template + ",'2026-10-18','2026-10-18 08:00','FLOOR'," + template
                        + ",TRUE,'CLOSED')");
        long lead = insert(
                c,
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,ends_at,command,by_system) VALUES ('M09.SHIFT',"
                        + shift + ",'LEAD_STAFF','M09','PERIOD'," + staff
                        + ",'2026-10-18','2026-10-18 08:00','SCHEDULED',FALSE)");
        rows.add(new ProtectedRow("SHIFT_LEAD_HISTORY", "status_history", lead, "id"));
        long handover = insert(
                c,
                "INSERT INTO shift_handover(shift_id,handover_staff_id,created_at,finalized_at,confirmed_at,status,items,assessments,next_item_id) VALUES ("
                        + shift + "," + staff
                        + ",'2026-10-18 07:30','2026-10-18 07:50',NULL,'PREPARED',JSON_ARRAY(JSON_OBJECT('id',1,'itemGroup','TASK','residentId',1,'description','Bản gốc','section','FINALIZED','createdAt','2026-10-18T07:31:00Z')),JSON_ARRAY(JSON_OBJECT('assessmentKind','GENERAL','residentId',NULL,'residentKey',0,'content','Nhận định gốc')),2)");
        rows.add(new ProtectedRow("HANDOVER", "shift_handover", handover, "id"));
        rows.add(new ProtectedRow("HANDOVER_ITEMS", "shift_handover", handover, "items"));
        rows.add(new ProtectedRow("HANDOVER_ASSESSMENTS", "shift_handover", handover, "assessments"));
        long comment = event(
                c,
                "M09.HANDOVER",
                handover,
                "M09.HANDOVER_COMMENT",
                "JSON_OBJECT('content','Ý kiến gốc','sender','[Người nhận]')",
                null);
        rows.add(new ProtectedRow("HANDOVER_COMMENT", "record_event", comment, "content"));
        long reply = event(
                c,
                "M09.HANDOVER",
                handover,
                "M09.HANDOVER_REPLY",
                "JSON_OBJECT('content','Phản hồi gốc','sender','[Người giao]')",
                comment);
        rows.add(new ProtectedRow("HANDOVER_REPLY", "record_event", reply, "content"));
        long publication = event(
                c, "M09.SCHEDULE", schedule, "M09.SHIFT_PUBLICATION", "JSON_OBJECT('content',JSON_OBJECT())", null);
        rows.add(new ProtectedRow("SHIFT_PUBLICATION", "record_event", publication, "content"));
        long ratio = event(
                c,
                "M09.SHIFT",
                shift,
                "M09.STAFFING_RATIO_CALCULATION",
                "JSON_OBJECT('calculatedAt','2026-10-18T00:00:00Z','event','PUBLISHED','totalWeight',1,'staffCount',1,'ratio',1,'threshold',1,'meetsThreshold',CAST('true' AS JSON))",
                null);
        rows.add(new ProtectedRow("STAFFING_RATIO_CALCULATION", "record_event", ratio, "content"));
        long batch = insert(
                c,
                "INSERT INTO simple_record(kind,module,status,content,locked_at,created_at,created_by) VALUES ('M09.STAFF_IMPORT_BATCH','M09','RECORDED',JSON_OBJECT('fileName','kiem-thu.xlsx','rowCount',1),'2026-10-18','2026-10-18',1)");
        rows.add(new ProtectedRow("STAFF_IMPORT_BATCH", "simple_record", batch, "content"));
        try (var statement = c.createStatement()) {
            statement.executeUpdate(
                    "UPDATE shift_handover SET status='CONFIRMED',confirmed_at='2026-10-18 08:00' WHERE id="
                            + handover);
        }
        return rows;
    }

    private long event(Connection c, String type, long parent, String kind, String content, Long reply)
            throws SQLException {
        return insert(
                c,
                "INSERT INTO record_event(entity_type,entity_id,module,kind,content,reply_to_id,by_system,occurred_at,confirmed_at) VALUES ('"
                        + type + "'," + parent + ",'M09','" + kind + "'," + content + "," + reply
                        + ",FALSE,'2026-10-18','2026-10-18')");
    }

    private long insert(Connection c, String sql) throws SQLException {
        try (var st = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.executeUpdate();
            try (var keys = st.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }
}
