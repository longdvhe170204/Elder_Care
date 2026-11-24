package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.family.api.events.PickupExceptionNotice;
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

    /**
     * Test BF-12 C-2, C-5 (2026-11-24): danh sách ngoại lệ đón cho nhân viên (thay nhập mã) và cho người đại diện trên
     * cổng (FR-050); người thân không phải người đại diện không thấy; lọc trạng thái.
     */
    @Test
    void FR_050_listExceptions_staffAndRepresentativeOnly() throws Exception {
        long id = id(createException(office, "Ngô Hàng Xóm", OUTSIDER_DOC).andExpect(status().isCreated()));
        String path = "/api/v1/pickup-exceptions?residentId=" + a;

        JsonNode staff = getJsonAs(office, path);
        assertThat(staff).singleElement().satisfies(e -> {
            assertThat(e.get("id").asLong()).isEqualTo(id);
            assertThat(e.get("residentId").asLong()).isEqualTo(a);
            assertThat(e.get("fullName").asString()).isEqualTo("Ngô Hàng Xóm");
            assertThat(e.get("idDocumentNumber").asString()).isEqualTo(OUTSIDER_DOC);
            assertThat(e.get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
            assertThat(e.get("createdAt").isNull()).isFalse();
        });
        assertThat(getJsonAs(family(mMember), "/api/v1/pickup-exceptions")).isEmpty();

        exceptionCommand(family(rMember), id, "confirm", "{}").andExpect(status().isOk());
        JsonNode effective = getJsonAs(family(rMember), "/api/v1/pickup-exceptions?status=EFFECTIVE");
        assertThat(effective).singleElement().satisfies(e -> {
            assertThat(e.get("method").asString()).isEqualTo("PORTAL");
            assertThat(e.get("expiresAt").isNull()).isFalse();
        });
        assertThat(getJsonAs(office, path + "&status=PENDING_CONFIRMATION")).isEmpty();
    }
}
