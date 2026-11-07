package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.resident.api.spi.ContractApi;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T031, T035 (US3; FR-024, FR-028, BR-M02-09, FR-029): nội dung hợp đồng tại ngày D là phiên bản có {@code
 * effective_from} lớn nhất không muộn hơn D; phụ lục (US5) chỉ thêm phiên bản, nên test thêm phiên bản bằng SQL. Cùng
 * truy vấn phục vụ {@code ContractReadApi} (003, 005, 010, 012, 017, 018) và {@code ContractApi} (001).
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_09_ContentAtDateIT extends ContractTestBase {

    @Autowired
    ContractReadApi readApi;

    @Autowired
    ContractApi residenceTypes;

    /** Phiên bản {@code sequence} từ {@code from}: sao nội dung phiên bản 0 rồi đổi mức và loại lưu trú. */
    private void addVersion(long contractId, int sequence, LocalDate from, String careLevel, String residenceType)
            throws Exception {
        String zero = jdbc.queryForObject(
                "SELECT content FROM contract_version WHERE kind = 'CONTENT' AND contract_id = ? AND sequence_no = 0",
                String.class,
                contractId);
        ObjectNode content = (ObjectNode) JSON.readTree(zero);
        content.put("careLevel", careLevel);
        content.put("residenceType", residenceType);
        jdbc.update(
                "INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content,"
                        + " residence_type, care_level, created_at, locked_at) VALUES ('CONTENT', ?, ?, ?, ?, ?, ?, NOW(6),"
                        + " NOW(6))",
                contractId,
                sequence,
                from,
                content.toString(),
                residenceType,
                careLevel);
    }

    private JsonNode contentAt(long id, LocalDate date) throws Exception {
        user.set(office.user());
        return JSON.readTree(
                mvc.perform(get("/api/v1/contracts/" + id + "/content").param("date", date.toString()))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
    }

    /** Hợp đồng Hiệu lực bắt đầu cách hôm nay 100 ngày, kèm hai phiên bản phụ lục giả lập sau 30 và 60 ngày. */
    private record Setup(long residentId, long id, LocalDate start) {}

    private Setup withVersions() throws Exception {
        long residentId = residentWithRepresentative();
        LocalDate start = today().minusDays(100);
        ObjectNode body = body(residentId);
        body.put("startDate", start.toString());
        long id = activeContract(residentId, body);
        addVersion(id, 1, start.plusDays(30), "REGULAR", "LONG_TERM_RESIDENTIAL");
        addVersion(id, 2, start.plusDays(60), "SPECIAL", "SHORT_TERM_RESIDENTIAL");
        return new Setup(residentId, id, start);
    }

    @Test
    void BR_M02_09_contentAtDate_isTheLatestVersionNotLaterThanTheDate() throws Exception {
        Setup s = withVersions();

        assertThat(contentAt(s.id(), s.start()).get("version").asInt()).isZero();
        JsonNode beforeFirstAddendum = contentAt(s.id(), s.start().plusDays(29));
        assertThat(beforeFirstAddendum.get("version").asInt()).isZero();
        assertThat(beforeFirstAddendum.get("content").get("careLevel").asString())
                .isEqualTo("BASIC");

        JsonNode onFirstAddendum = contentAt(s.id(), s.start().plusDays(30));
        assertThat(onFirstAddendum.get("version").asInt()).isEqualTo(1);
        assertThat(onFirstAddendum.get("effectiveFrom").asString())
                .isEqualTo(s.start().plusDays(30).toString());
        assertThat(onFirstAddendum.get("content").get("careLevel").asString()).isEqualTo("REGULAR");

        assertThat(contentAt(s.id(), s.start().plusDays(59)).get("version").asInt())
                .isEqualTo(1);
        JsonNode last = contentAt(s.id(), s.start().plusDays(60));
        assertThat(last.get("version").asInt()).isEqualTo(2);
        assertThat(last.get("content").get("residenceType").asString()).isEqualTo("SHORT_TERM_RESIDENTIAL");
    }
}
