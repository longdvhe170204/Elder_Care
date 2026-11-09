package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T042 (DBR-07, FR-040, FR-047): phụ lục chỉ do yêu cầu đã duyệt tạo, ngày hiệu lực không sớm hơn ngày bắt đầu hợp đồng
 * gốc, không có thao tác sửa hay xóa phụ lục; cơ sở dữ liệu giữ các quy tắc này khi mã bị bỏ qua.
 */
@AutoConfigureMockMvc
@WithMockUser
class DBR_07_AddendumIT extends ChangeTestBase {

    private static final String INSERT_ADDENDUM =
            "INSERT INTO contract_version (kind, contract_id, approval_request_id,"
                    + " change_types, content_before, content_after, effective_date, addendum_status, created_at)"
                    + " VALUES ('ADDENDUM', ?, ?, 'CARE_LEVEL', '{}', '{}', ?, 'PENDING_EFFECTIVE', NOW(6))";

    @Test
    void DBR_07_theDatabaseRejectsAnAddendumDatedBeforeTheContractStart() throws Exception {
        Setup s = activeSetup();

        assertThatThrownBy(() -> jdbc.update(INSERT_ADDENDUM, s.contractId(), 9_000_001L, today().minusDays(1)))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("DBR-07");
    }
}
