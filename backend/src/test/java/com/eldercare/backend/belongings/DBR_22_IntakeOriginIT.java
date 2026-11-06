package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 013 T008 (US1; DBR-22, FR-003 → FR-009, BR-M12-05; quickstart Q1, Q2, Q4): lệnh Tiếp nhận tạo đồ Đang giữ và đúng một
 * bản ghi Tiếp nhận cùng lần; đồ có giá trị bắt buộc ảnh.
 */
class DBR_22_IntakeOriginIT extends IntakeTestBase {

    private String phone(long locationId, String photos) {
        return "{\"typeId\": " + typeId("PHONE") + ", \"description\": \"Điện thoại Nokia\", \"quantity\": 1,"
                + " \"condition\": {\"levelId\": " + levelId("GOOD") + "}, \"giver\": {\"type\": \"RESIDENT\"},"
                + " \"at\": \"2026-11-05T08:00:00+07:00\", \"storageLocationId\": " + locationId
                + ", \"photoFileIds\": " + photos + "}";
    }

    @Test
    void Q1_BR_M12_05_valuableWithoutPhoto_isBlocked_andNothingIsCreated() throws Exception {
        long resident = resident("IN_RESIDENCE");

        postJson(office, "/api/v1/residents/" + resident + "/belongings", phone(location(), "[]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PHOTO_REQUIRED"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM belonging WHERE resident_id = ?", Integer.class, resident))
                .isZero();
    }

    @Test
    void Q2_DBR_22_intakeCreatesHeldBelongingWithExactlyOneIntakeRecord() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long photo = photo();
        family.representative(TestUsers.nextId(), resident);

        long id =
                id(postJson(office, "/api/v1/residents/" + resident + "/belongings", phone(location, "[" + photo + "]"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("HELD"))
                        .andExpect(jsonPath("$.storageLocationId").value(location))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE belonging_id = ? AND kind = 'INTAKE'",
                        Integer.class,
                        id))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT b.intake_handover_id = h.id AND b.current_handover_id = h.id AND h.valuable"
                                + " FROM belonging b JOIN belonging_handover h ON h.belonging_id = b.id WHERE b.id = ?",
                        Boolean.class,
                        id))
                .isTrue();
        assertThat(files.attachedTo(
                        "M12.BELONGING_PHOTO",
                        jdbc.queryForObject("SELECT intake_handover_id FROM belonging WHERE id = ?", Long.class, id)))
                .hasSize(1);
        assertThat(notices()).anySatisfy(n -> {
            assertThat(n.recipientGroups()).containsExactly(new RecipientGroup.Representative(resident));
            assertThat(n.level()).contains(NotificationLevel.LOW);
            assertThat(n.informationSubject()).isEqualTo(Optional.of(InformationSubject.ABOUT_FAMILY_MATTER));
            assertThat(n.shortContent().orElseThrow()).doesNotContain("Nokia");
        });

        getJson(nurse, "/api/v1/belongings/" + id).andExpect(status().is4xxClientError());
        getJson(office, "/api/v1/belongings/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.belonging.status").value("HELD"))
                .andExpect(jsonPath("$.handovers.length()").value(1))
                .andExpect(jsonPath("$.handovers[0].kind").value("INTAKE"));
        getJson(office, "/api/v1/residents/" + resident + "/belongings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void FR_003_cashUsesAmount_notQuantity() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        String body = "{\"typeId\": " + typeId("CASH") + ", \"description\": \"Tiền mặt\", %s,"
                + " \"condition\": {\"levelId\": " + levelId("GOOD") + "}, \"giver\": {\"type\": \"RESIDENT\"},"
                + " \"at\": \"2026-11-05T08:00:00+07:00\", \"storageLocationId\": " + location
                + ", \"photoFileIds\": [%d]}";

        postJson(office, "/api/v1/residents/" + resident + "/belongings", body.formatted("\"quantity\": 1", photo()))
                .andExpect(status().isUnprocessableEntity());
        postJson(
                        office,
                        "/api/v1/residents/" + resident + "/belongings",
                        body.formatted("\"cashAmount\": 2000000", photo()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cashAmount").value(2000000));
    }

    @Test
    void FR_006_finalStatus_blocksIntake() throws Exception {
        long resident = resident("DECEASED");

        postJson(office, "/api/v1/residents/" + resident + "/belongings", phone(location(), "[" + photo() + "]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENT_FINAL_STATE"));
    }

    @Test
    void Q4_FR_007_nurseOutsideScope_andManager_cannotIntake() throws Exception {
        long resident = resident("IN_RESIDENCE");

        postJson(nurse, "/api/v1/residents/" + resident + "/belongings", phone(location(), "[" + photo() + "]"))
                .andExpect(status().is4xxClientError());
        postJson(manager, "/api/v1/residents/" + resident + "/belongings", phone(location(), "[" + photo() + "]"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM belonging WHERE resident_id = ?", Integer.class, resident))
                .isZero();
    }

    @Test
    void FR_011_heldIntake_needsStorageLocation() throws Exception {
        long resident = resident("IN_RESIDENCE");
        String body = "{\"typeId\": " + typeId("CLOTHING") + ", \"description\": \"Áo\", \"quantity\": 5,"
                + " \"condition\": {\"levelId\": " + levelId("GOOD") + "}, \"giver\": {\"type\": \"RESIDENT\"},"
                + " \"at\": \"2026-11-05T08:00:00+07:00\"}";

        postJson(office, "/api/v1/residents/" + resident + "/belongings", body)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("STORAGE_LOCATION_REQUIRED"));
    }
}
