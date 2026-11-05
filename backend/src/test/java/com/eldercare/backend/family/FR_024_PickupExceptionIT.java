package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.family.api.events.PickupExceptionNotice;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * 012 T018 (US2 kịch bản 3 → 6; FR-024, FR-025; BR-M10-03; Q17, Q18): ngoại lệ đón cho người ngoài danh sách chỉ Hiệu
 * lực khi người đại diện xác nhận qua cổng hoặc Quản lý viện duyệt kèm lý do; dùng cho đúng một lượt đón trong CFG-M10-08;
 * không thêm người vào danh sách; từ chối, hết hạn thì lượt đón vẫn bị chặn; hủy ngoại lệ Hiệu lực cần lý do.
 */
class FR_024_PickupExceptionIT extends PickupTestBase {

    static final String OUTSIDER_DOC = "079100000099";

    @Test
    void US2_3_exceptionRequest_waitsForRepresentativeAndManager() throws Exception {
        JsonNode created =
                json(createException(office, "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isCreated()));

        assertThat(exceptionStatus(created.get("id").asLong())).isEqualTo("PENDING_CONFIRMATION");
        assertThat(events.stream(PickupExceptionNotice.class)).singleElement().satisfies(n -> {
            assertThat(n.kind()).isEqualTo(PickupExceptionNotice.Kind.NEEDS_CONFIRMATION);
            assertThat(n.representativeIds()).containsExactly(rMember);
        });
    }

    @Test
    void US2_4_representativeConfirms_oneUse_pickerIsNotAddedToTheList() throws Exception {
        long exception =
                id(createException(office, "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isCreated()));
        exceptionCommand(family(rMember), exception, "confirm", "{}").andExpect(status().isOk());
        assertThat(exceptionStatus(exception)).isEqualTo("EFFECTIVE");

        JsonNode record = json(pickupWithException(office, "TEMPORARY_ABSENCE", exception, OUTSIDER_DOC)
                .andExpect(status().isCreated()));

        assertThat(record.get("basis").asString()).isEqualTo("REPRESENTATIVE_EXCEPTION");
        assertThat(exceptionStatus(exception)).isEqualTo("USED");
        assertThat(getJson("/api/v1/residents/" + a + "/pickup-list"))
                .noneSatisfy(p -> assertThat(p.get("fullName").asString()).isEqualTo("Ngô Hàng Xóm"));
        pickupWithException(office, "TEMPORARY_ABSENCE", exception, OUTSIDER_DOC)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PICKUP_BLOCKED"));
    }

    @Test
    void US2_4_exceptionDocumentMustMatch() throws Exception {
        long exception =
                id(createException(office, "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isCreated()));
        exceptionCommand(family(rMember), exception, "confirm", "{}").andExpect(status().isOk());

        pickupWithException(office, "TEMPORARY_ABSENCE", exception, "079100000098")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.blockReason").value("ID_DOCUMENT_MISMATCH"));
        assertThat(exceptionStatus(exception)).isEqualTo("EFFECTIVE");
    }

    @Test
    void US2_5_managerApprovesWithReason_basisIsManagerException() throws Exception {
        long exception =
                id(createException(office, "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isCreated()));
        String manager = staffSession(Role.NURSING_HOME_MANAGER);

        exceptionCommand(manager, exception, "approve", "{}").andExpect(status().isUnprocessableContent());
        exceptionCommand(manager, exception, "approve", reason("Đã gọi người đại diện không được"))
                .andExpect(status().isOk());

        JsonNode record = json(pickupWithException(office, "FAMILY_OUTING", exception, OUTSIDER_DOC)
                .andExpect(status().isCreated()));
        assertThat(record.get("basis").asString()).isEqualTo("MANAGER_EXCEPTION");
    }

    @Test
    void US2_5_rejected_pickupStaysBlocked() throws Exception {
        long exception =
                id(createException(office, "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isCreated()));

        exceptionCommand(family(rMember), exception, "reject", reason("Không quen người này"))
                .andExpect(status().isOk());

        assertThat(exceptionStatus(exception)).isEqualTo("REJECTED");
        pickupWithException(office, "TEMPORARY_ABSENCE", exception, OUTSIDER_DOC)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PICKUP_BLOCKED"));
    }

    @Test
    void US2_6_expiredException_blocksAndIsMarkedExpired() throws Exception {
        long exception =
                id(createException(office, "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isCreated()));
        exceptionCommand(family(rMember), exception, "confirm", "{}").andExpect(status().isOk());
        Instant effective = time.now();
        int blocksBefore = blocks();

        at(effective.plus(Duration.ofHours(4)).plusSeconds(60));
        String laterOffice = staffSession(Role.OFFICE_STAFF);
        pickupWithException(laterOffice, "TEMPORARY_ABSENCE", exception, OUTSIDER_DOC)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PICKUP_EXCEPTION_EXPIRED"));

        assertThat(blocks()).isEqualTo(blocksBefore + 1);
        assertThat(lastBlockReason()).isEqualTo("EXCEPTION_EXPIRED");
        assertThat(exceptionStatus(exception)).isEqualTo("EXPIRED");
    }

    @Test
    void FR_024_onlyARepresentativeConfirms_cancelNeedsAReason() throws Exception {
        long exception =
                id(createException(office, "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isCreated()));

        exceptionCommand(family(mMember), exception, "confirm", "{}").andExpect(status().isForbidden());
        exceptionCommand(family(rMember), exception, "confirm", "{}").andExpect(status().isOk());
        exceptionCommand(family(rMember), exception, "cancel", "{}").andExpect(status().isUnprocessableContent());
        exceptionCommand(family(rMember), exception, "cancel", reason("Gia đình tự đón"))
                .andExpect(status().isOk());

        assertThat(exceptionStatus(exception)).isEqualTo("CANCELLED");
    }

    @Test
    void FR_025_emptyPickupList_everyPickupGoesThroughAnException() throws Exception {
        requestRight(family(rMember), a, m, "PICKUP_ALLOWED", false).andExpect(status().isCreated());
        requestRight(family(rMember), a, r, "PICKUP_ALLOWED", false).andExpect(status().isCreated());

        pickup(office, "TEMPORARY_ABSENCE", rMember, R_DOC)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.blockReason").value("NOT_ON_LIST"));
        long exception = id(createException(office, "Trần R", R_DOC).andExpect(status().isCreated()));
        exceptionCommand(staffSession(Role.NURSING_HOME_MANAGER), exception, "approve", reason("Danh sách rỗng"))
                .andExpect(status().isOk());
        pickupWithException(office, "TEMPORARY_ABSENCE", exception, R_DOC).andExpect(status().isCreated());
    }

    @Test
    void FR_022_onlySourceCommandHoldersCreateExceptions() throws Exception {
        createException(staffSession(Role.DOCTOR), "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isForbidden());
        createException(family(rMember), "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isForbidden());
    }
}
