package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementSourceType;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
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
    void FR_008c_onlyApprovedRequestOfSameDepartmentAndTypeIsUsable() throws Exception {
        long id = assetRequest(1_800_000);
        assertThat(port.checkUsable(id, kitchen, ProcurementType.ASSET)).isEmpty();
        assertThat(port.checkUsable(id, technical, ProcurementType.ASSET))
                .extracting(UnmetCondition::code)
                .containsExactly("PROCUREMENT_REQUEST_NOT_USABLE");
        assertThat(port.checkUsable(id, kitchen, ProcurementType.SERVICE)).isNotEmpty();
        assertThat(port.checkUsable(-1, kitchen, ProcurementType.ASSET)).isNotEmpty();
        as(kitchenLead);
        long draft = created(body(kitchen, "ASSET", "Nháp"));
        assertThat(port.checkUsable(draft, kitchen, ProcurementType.ASSET)).isNotEmpty();
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

    @Test
    void Q294_assetRequestMatchesOnlyWhenLinkedAcquisitionCostEqualsActual() throws Exception {
        long id = assetRequest(1_800_000);
        port.linkSource(id, ProcurementSourceType.ASSET, TestUsers.nextId(), 1_000_000);
        as(kitchenLead);
        command(id, "record-purchased", purchase(1_900_000, kitchenLead)).andExpect(status().isOk());
        assertThatThrownBy(() -> port.linkSource(id, ProcurementSourceType.ASSET, TestUsers.nextId(), 900_000))
                .as("chỉ gắn khi Đã duyệt")
                .hasFieldOrPropertyWithValue("errorCode", "PROCUREMENT_REQUEST_NOT_USABLE");
        as(accountant);
        command(id, "confirm-match")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RECEIPT_MISMATCH"));
        command(id, "return-for-reconciliation", "{\"reason\":\"Thiếu tài sản\"}")
                .andExpect(status().isOk());
        port.linkSource(id, ProcurementSourceType.ASSET, TestUsers.nextId(), 900_000);
        as(kitchenLead);
        command(id, "record-purchased", purchase(1_900_000, kitchenLead)).andExpect(status().isOk());
        as(accountant);
        command(id, "confirm-match").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("RECONCILED");
    }
}
