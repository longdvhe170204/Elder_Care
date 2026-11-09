package com.eldercare.backend.belongings;

import com.eldercare.backend.belongings.api.events.BelongingNotice;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.family.FakeFamilyRights;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Nền US1 của 013: người cao tuổi, vị trí, tệp ảnh, người thân giả (012 {@code FakeFamilyRights}). */
@Import({FakeFamilyRights.Config.class, FakeFileConfig.class})
@RecordApplicationEvents
abstract class IntakeTestBase extends BelongingsTestBase {

    static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    FakeFamilyRights family;

    @Autowired
    FileApi files;

    @Autowired
    ApplicationEvents events;

    @AfterEach
    void resetFamily() {
        family.reset();
    }

    long resident(String status) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                                    + " created_at, created_by) VALUES (?, 'Người cao tuổi thử', '1940-05-01', 'FEMALE',"
                                    + " ?, NOW(6), NOW(6), 1)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, UUID.randomUUID().toString().substring(0, 20));
                    ps.setString(2, status);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    long location() {
        jdbc.update(
                "INSERT INTO catalog_item (catalog_type, module, name, sort_order, status, attributes)"
                        + " VALUES ('M12.STORAGE_LOCATION', 'M12', ?, 0, 'ACTIVE', JSON_OBJECT())",
                "Tủ thử " + TestUsers.nextId());
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM catalog_item WHERE catalog_type = 'M12.STORAGE_LOCATION'", Long.class);
    }

    long photo() {
        // nội dung khác nhau mỗi lần để không dùng chung một tệp giữa các test (quyền tải theo chủ tệp)
        long unique = TestUsers.nextId();
        return files.upload(
                "anh.jpg",
                "image/jpeg",
                new byte[] {
                    (byte) 0xFF,
                    (byte) 0xD8,
                    (byte) 0xFF,
                    0x00,
                    (byte) unique,
                    (byte) (unique >> 8),
                    (byte) (unique >> 16)
                },
                1L);
    }

    long typeId(String code) {
        return catalogId("M12.BELONGING_TYPE", code);
    }

    long levelId(String code) {
        return catalogId("M12.CONDITION_LEVEL", code);
    }

    String familySession(long familyMemberId) {
        return TestUsers.bearer(users.session(users.family(familyMemberId).accountId()));
    }

    static long id(String json) {
        return JSON.readTree(json).get("id").asLong();
    }

    static JsonNode tree(String json) {
        return JSON.readTree(json);
    }

    List<NotificationRequest> notices() {
        return events.stream(BelongingNotice.class)
                .flatMap(n -> n.notificationRequests().stream())
                .toList();
    }
}
