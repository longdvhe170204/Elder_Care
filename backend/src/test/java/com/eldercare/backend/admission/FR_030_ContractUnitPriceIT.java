package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 003 T032 nhánh dấu giá (003 FR-030, research G-07; bảng duyệt 16 điểm (3), 2026-11-09): 004 so phí lưu trú của loại phòng
 * với loại phòng của hợp đồng Hiệu lực ({@code AllocationResidencySource.differentContractUnitPrice}); cùng giá thì không
 * khác, khác giá hoặc không có hợp đồng Hiệu lực thì khác.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_030_ContractUnitPriceIT extends ChangeTestBase {

    @Autowired
    AllocationResidencySource source;

    private long roomTypeWithFee(int amount) {
        long type = TestUsers.nextId();
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(?,?,2)", type, "Loại " + type);
        String name = "Phí " + type;
        jdbc.update(
                M02Sql.INSERT_SERVICE, name, M02Sql.attributes("RESIDENCY_FEE", "tháng", "MONTH", null, type, null));
        long service = jdbc.queryForObject(M02Sql.SERVICE_ID, Long.class, name);
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from) VALUES (?, ?, '2020-01-01')",
                service,
                amount);
        return type;
    }

    @Test
    void FR_030_comparesWithTheRoomTypeOfTheActiveContract() throws Exception {
        long contractType = roomTypeWithFee(3_000_000);
        long samePrice = roomTypeWithFee(3_000_000);
        long otherPrice = roomTypeWithFee(4_000_000);
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        body.put("roomTypeId", contractType);
        ((ArrayNode) body.get("services")).add(line(meal));
        activeContract(residentId, body);

        assertThat(source.differentContractUnitPrice(residentId, samePrice, today()))
                .isFalse();
        assertThat(source.differentContractUnitPrice(residentId, otherPrice, today()))
                .isTrue();
        assertThat(source.differentContractUnitPrice(TestUsers.nextId(), samePrice, today()))
                .as("không có hợp đồng Hiệu lực")
                .isTrue();
    }
}
