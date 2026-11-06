package com.eldercare.backend.belongings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;

/**
 * 013 T027 (US6; BR-M12-09, FR-030, Q-151, SC-012; quickstart Q42): cổng người thân chỉ cho người đại diện và người có
 * quyền "được phép đón"; kiểm quyền mỗi lần gọi; không lộ vị trí lưu giữ hay tên nhân viên; ảnh theo cùng quyền.
 */
class BR_M12_09_PortalBelongingsIT extends ReturnTestBase {

    private String path(long resident) {
        return "/api/v1/family-portal/residents/" + resident + "/belongings";
    }

    @Test
    void Q42_representativeAndPickerSeeBelongingsAndHistory_otherRelativeDoesNot() throws Exception {
        long resident = resident("IN_RESIDENCE");
        intake(resident, "CLOTHING", "\"quantity\": 1", location());
        intake(resident, "PHONE", "\"quantity\": 1", location());
        intake(resident, "GLASSES", "\"quantity\": 1", location());
        long representative = TestUsers.nextId();
        long picker = TestUsers.nextId();
        long other = TestUsers.nextId();
        family.representative(representative, resident)
                .relate(picker, resident, FamilyRight.PICKUP_ALLOWED)
                .relate(other, resident);

        getJson(portalSession(representative), path(resident))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].history.length()").value(1))
                .andExpect(jsonPath("$[0].storageLocationId").doesNotExist())
                .andExpect(jsonPath("$[0].history[0].storageLocationId").doesNotExist());
        getJson(portalSession(picker), path(resident)).andExpect(status().isOk());
        getJson(portalSession(other), path(resident)).andExpect(status().isForbidden());
        getJson(tokenA, path(resident)).andExpect(status().isForbidden());
    }

    @Test
    void Q42_rightIsCheckedOnEveryCall() throws Exception {
        long resident = resident("IN_RESIDENCE");
        intake(resident, "CLOTHING", "\"quantity\": 1", location());
        long representative = TestUsers.nextId();
        family.representative(representative, resident);
        String session = portalSession(representative);

        getJson(session, path(resident)).andExpect(status().isOk());
        family.reset();

        getJson(session, path(resident)).andExpect(status().isForbidden());
    }

    @Test
    void Q42_photoFollowsTheSamePermission() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long belonging = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        long photo = jdbc.queryForObject(
                "SELECT CAST(photo_file_ids ->> '$[0]' AS UNSIGNED) FROM belonging_handover WHERE belonging_id = ?",
                Long.class,
                belonging);
        long representative = TestUsers.nextId();
        long other = TestUsers.nextId();
        family.representative(representative, resident).relate(other, resident);

        mvc.perform(get("/api/v1/files/" + photo).header("Authorization", portalSession(representative)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/files/" + photo).header("Authorization", portalSession(other)))
                .andExpect(status().isForbidden());
    }
}
