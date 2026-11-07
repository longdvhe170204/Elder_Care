package com.eldercare.backend.belongings;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Nền test tích hợp của M12 qua REST với phiên thật (lớp quyền của 002). Dữ liệu schema test giữ qua các lần chạy: tên
 * danh mục, người cao tuổi lấy từ {@link TestUsers#nextId()}.
 */
abstract class BelongingsTestBase extends MySqlIntegrationTestBase {

    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    MockMvc mvc;
    TestUsers users;
    String manager;
    String office;
    String nurse;

    @BeforeEach
    void setUpBelongings() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        users = new TestUsers(jdbc, time);
        manager = TestUsers.bearer(
                users.session(users.staff(Role.NURSING_HOME_MANAGER).accountId()));
        office = TestUsers.bearer(users.session(users.staff(Role.OFFICE_STAFF).accountId()));
        nurse = TestUsers.bearer(users.session(users.staff(Role.NURSING_STAFF).accountId()));
    }

    static String unique(String name) {
        return name + " " + TestUsers.nextId();
    }

    ResultActions postJson(String token, String path, String body) throws Exception {
        return mvc.perform(post(path)
                .header("Authorization", token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    ResultActions putJson(String token, String path, String body) throws Exception {
        return mvc.perform(put(path)
                .header("Authorization", token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    ResultActions getJson(String token, String path) throws Exception {
        return mvc.perform(get(path).header("Authorization", token));
    }

    long catalogId(String kind, String code) {
        return jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type = ? AND code = ?", Long.class, kind, code);
    }

    /** Một đồ Đang giữ dựng bằng SQL (lệnh Tiếp nhận làm ở US1). */
    long heldBelonging(long residentId, Long storageLocationId) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO belonging (resident_id, type_id, description, quantity, status, holder_type,"
                                    + " holder_id, storage_location_id, condition_level_id, created_by, created_at)"
                                    + " VALUES (?, ?, 'Đồ thử', 1, 'HELD', 'STAFF', 1, ?, ?, 1, NOW(6))",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, residentId);
                    ps.setLong(2, catalogId("M12.BELONGING_TYPE", "CLOTHING"));
                    ps.setObject(3, storageLocationId);
                    ps.setLong(4, catalogId("M12.CONDITION_LEVEL", "GOOD"));
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }
}
