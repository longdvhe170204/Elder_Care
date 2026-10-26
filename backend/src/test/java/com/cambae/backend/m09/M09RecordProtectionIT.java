package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T042: bảng JDBC nhóm 3 của 008 không nằm trong danh sách entity JPA của test chung 000. */
class M09RecordProtectionIT extends MySqlIntegrationTestBase {
    private static final Set<String> RECORD_TABLES = Set.of(
            "staff_department",
            "department_lead_history",
            "shift_lead_history",
            "shift_publication",
            "staff_import_batch",
            "staffing_ratio_calculation",
            "shift_handover",
            "handover_item",
            "handover_assessment",
            "handover_comment",
            "handover_reply");

    @Test
    void DBR_23_allJdbcRecordTablesHaveBeforeUpdateAndDeleteGuards() throws SQLException {
        try (var connection = adminConnection();
                var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT EVENT_OBJECT_TABLE,EVENT_MANIPULATION,ACTION_TIMING"
                        + " FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE()")) {
            var guarded = new HashMap<String, Set<String>>();
            while (rows.next()) {
                if ("BEFORE".equals(rows.getString(3)))
                    guarded.computeIfAbsent(rows.getString(1), ignored -> new HashSet<>())
                            .add(rows.getString(2));
            }
            for (String table : RECORD_TABLES)
                assertThat(guarded.get(table)).as(table).contains("UPDATE", "DELETE");
        }
    }

    @Test
    void DBR_23_originalRecordsRemainImmutableEvenForAdmin_andFixturesRollBack() throws SQLException {
        try (var connection = adminConnection()) {
            connection.setAutoCommit(false);
            try {
                var records = records(connection);
                assertThat(records.keySet()).containsExactlyInAnyOrderElementsOf(RECORD_TABLES);
                try (var statement = connection.createStatement()) {
                    for (var row : records.entrySet()) {
                        String table = row.getKey();
                        long id = row.getValue();
                        assertThatThrownBy(
                                        () -> statement.executeUpdate("UPDATE " + table + " SET id=id WHERE id=" + id))
                                .as("UPDATE %s", table)
                                .isInstanceOf(SQLException.class)
                                .hasFieldOrPropertyWithValue("SQLState", "45000");
                        assertThatThrownBy(() -> statement.executeUpdate("DELETE FROM " + table + " WHERE id=" + id))
                                .as("DELETE %s", table)
                                .isInstanceOf(SQLException.class)
                                .hasFieldOrPropertyWithValue("SQLState", "45000");
                    }
                }
            } finally {
                connection.rollback();
            }
        }
    }

    private Map<String, Long> records(Connection c) throws SQLException {
        var records = new LinkedHashMap<String, Long>();
        long staff = insert(
                c, "INSERT INTO staff_profile(full_name,position) VALUES ('[Kiểm bất biến JDBC]','NURSING_STAFF')");
        long template = insert(
                c,
                "INSERT INTO shift_template(name,start_time,end_time,scope_type) VALUES ('[Kiểm bất biến]','07:00','15:00','FLOOR')");
        long schedule = insert(
                c,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2026-10','FLOOR',"
                        + TestUsers.nextId() + ",'PUBLISHED')");
        long shift = insert(
                c,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) VALUES ("
                        + schedule + "," + template + ",'2026-10-19','2026-10-19 08:00','FLOOR',1,TRUE,'PUBLISHED')");
        long handover = insert(
                c,
                "INSERT INTO shift_handover(shift_id,handover_staff_id,created_at) VALUES (" + shift + "," + staff
                        + ",'2026-10-19 07:30')");
        records.put(
                "handover_item",
                insert(
                        c,
                        "INSERT INTO handover_item(handover_id,item_group,resident_id,description,created_at) VALUES ("
                                + handover + ",'FOLLOW_UP_ISSUE',1,'[Nội dung kiểm bất biến]','2026-10-19 07:31')"));
        records.put(
                "handover_assessment",
                insert(
                        c,
                        "INSERT INTO handover_assessment(handover_id,assessment_kind,content) VALUES (" + handover
                                + ",'GENERAL','[Nhận định kiểm bất biến]')"));
        long comment = insert(
                c,
                "INSERT INTO handover_comment(handover_id,content,sender,created_at) VALUES (" + handover
                        + ",'[Ý kiến kiểm bất biến]','[Người kiểm thử]','2026-10-19 07:32')");
        records.put("handover_comment", comment);
        records.put(
                "handover_reply",
                insert(
                        c,
                        "INSERT INTO handover_reply(comment_id,content,sender,created_at) VALUES (" + comment
                                + ",'[Phản hồi kiểm bất biến]','[Người kiểm thử]','2026-10-19 07:33')"));
        try (var statement = c.createStatement()) {
            statement.executeUpdate(
                    "UPDATE shift_handover SET status='CONFIRMED',finalized_at='2026-10-19 07:40',confirmed_at='2026-10-19 07:45' WHERE id="
                            + handover);
        }
        records.put("shift_handover", handover);
        records.put(
                "shift_publication",
                insert(
                        c,
                        "INSERT INTO shift_publication(schedule_id,published_at,content) VALUES (" + schedule
                                + ",'2026-10-18','{}')"));
        records.put(
                "staff_import_batch",
                insert(
                        c,
                        "INSERT INTO staff_import_batch(file_name,row_count,created_by,created_at) VALUES ('kiem-thu.xlsx',1,1,'2026-10-18')"));
        records.put(
                "staffing_ratio_calculation",
                insert(
                        c,
                        "INSERT INTO staffing_ratio_calculation(shift_id,calculated_at,event,total_weight,staff_count,ratio,threshold,meets_threshold) VALUES ("
                                + shift + ",'2026-10-19','PUBLISHED',0,1,0,8,TRUE)"));
        records.put(
                "shift_lead_history",
                insert(
                        c,
                        "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,ends_at,basis) VALUES (" + shift
                                + "," + staff + ",'2026-10-19','2026-10-19 08:00','SCHEDULED')"));
        long department =
                insert(c, "INSERT INTO department(name) VALUES ('[Bộ phận kiểm bất biến " + TestUsers.nextId() + "]')");
        records.put(
                "staff_department",
                insert(
                        c,
                        "INSERT INTO staff_department(staff_id,department_id,starts_at,ends_at,actor_id,reason) VALUES ("
                                + staff + "," + department
                                + ",'2026-10-19','2026-10-19 08:00',1,'[Lịch sử kiểm bất biến]')"));
        records.put(
                "department_lead_history",
                insert(
                        c,
                        "INSERT INTO department_lead_history(department_id,staff_id,starts_at,ends_at,actor_id,reason) VALUES ("
                                + department + "," + staff
                                + ",'2026-10-19','2026-10-19 08:00',1,'[Lý do kiểm bất biến]')"));
        return records;
    }

    private long insert(Connection c, String sql) throws SQLException {
        try (var statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }
}
