package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementSourceType;
import com.eldercare.backend.roombedallocation.api.spi.ProcurementRequestPort;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 019 T005 (FR-008c, DBR-36, Q-290, Q-294; contracts mục 2.1): M03 gắn tài sản, bảo trì thuê ngoài vào yêu cầu Đã duyệt
 * cùng bộ phận qua SPI; một nguồn gắn đúng một yêu cầu; có nguồn gắn thì không hủy được; loại tài sản đối chiếu với tổng
 * nguyên giá.
 */
class DBR_36_SourceLinkIT extends ProcurementTestBase {

    @Autowired
    ProcurementRequestPort port;

    long assetRequest(long approvedAmount) throws Exception {
        as(kitchenLead);
        long id = created(body(kitchen, "ASSET", "Mua tủ lạnh"));
        command(id, "submit").andExpect(status().isOk());
        as(manager);
        command(id, "approve", "{\"approvedAmount\":" + approvedAmount + ",\"reason\":\"Giảm\"}")
                .andExpect(status().isOk());
        return id;
    }

    @Test
    void DBR_36_aSourceLinksToOneRequestAndBlocksCancellation() throws Exception {
        long first = assetRequest(1_800_000);
        long second = assetRequest(1_800_000);
        long asset = TestUsers.nextId();
        port.linkSource(first, ProcurementSourceType.ASSET, asset, 1_000_000);
        assertThatThrownBy(() -> port.linkSource(second, ProcurementSourceType.ASSET, asset, 1_000_000))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "SOURCE_ALREADY_LINKED");
        assertThatThrownBy(() -> port.linkSource(first, ProcurementSourceType.ASSET, asset, 1_000_000))
                .hasFieldOrPropertyWithValue("errorCode", "SOURCE_ALREADY_LINKED");

        as(kitchenLead);
        command(first, "cancel", "{\"reason\":\"Không mua nữa\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("HAS_LINKED_SOURCES"));
        mvc.perform(get(URL).param("departmentId", Long.toString(kitchen)))
                .andExpect(
                        jsonPath("$[?(@.id==" + first + ")].linkedSourceTotal").value(1_000_000));
    }
}
