package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.Appendix27Matrix.Row;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SC-001, FR-016, FR-018, FR-020, FR-022 (002 T017; research A-04): ma trận quyền nạp ở V002_04 khớp từng ô Phụ lục 27
 * của tài liệu nguồn theo luật quy đổi ở plan "Ghi chú triển khai" T007; điều chỉnh của 19.3 đúng danh sách; quy tắc
 * xem trường đúng 19.3. Tài liệu nguồn đổi mà migration chưa đổi thì test lỗi.
 */
class PermissionMatrixMatchesSourceIT extends MySqlIntegrationTestBase {

    /** Điều chỉnh của 19.3 so với Phụ lục 27 (data-model mục 3; spec FR-025, Q-177). */
    static final List<Row> SECTION_19_3 = List.of(
            new Row("AC-01", Function.SHIFT_SWAP_REQUEST, Operation.APPROVE, "FACILITY", List.of()),
            new Row("AC-01", Function.SHIFT_SWAP_REQUEST, Operation.CREATE, "FACILITY", List.of()),
            new Row("AC-02", Function.SHIFT_SWAP_REQUEST, Operation.CREATE, "ASSIGNED", List.of()));

    /** Dòng hạn chế của quy tắc xem trường; cặp không có dòng là FULL (19.3; chú thích ⁵, ¹⁴, ¹⁶, ³⁰). */
    static final Set<String> FIELD_VISIBILITY = Set.of(
            "AC-07|HEALTH|NONE",
            "AC-07|MEDICATION|NONE",
            "AC-07|WISHES|NONE",
            "AC-09|HEALTH|NONE",
            "AC-09|MEDICATION|NONE",
            "AC-09|WISHES|PRESENCE_ONLY",
            "AC-14|HEALTH|NONE",
            "AC-14|MEDICATION|NONE",
            "AC-14|WISHES|PRESENCE_ONLY");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_016_functionEnum_matchesAppendix27Rows() throws IOException {
        List<String> source =
                Appendix27Matrix.labels(Files.readString(Appendix27Matrix.SOURCE, StandardCharsets.UTF_8));

        assertThat(Arrays.stream(Function.values()).map(Function::label).toList())
                .isEqualTo(source);
    }

    @Test
    void SC_001_loadedMatrix_matchesEveryCellOfAppendix27() throws IOException {
        Set<String> roles = new HashSet<>(
                jdbc.queryForList("SELECT code FROM catalog_item WHERE catalog_type = 'M15.ROLE'", String.class));

        assertThat(loaded("APPENDIX_27")).containsExactlyInAnyOrderElementsOf(Appendix27Matrix.rows(roles));
    }

    /** 002 T090 (FR-016, FR-068; research A-15): vai trò AC-16 nạp ở V002_08, phạm vi chính mình, cột NCT của Phụ lục 27. */
    @Test
    void FR_068_residentRole_selfScope_matrixColumnLoaded() throws IOException {
        assertThat(jdbc.queryForList(
                        "SELECT CONCAT(attributes->>'$.accountType', '/', attributes->>'$.executionScope') FROM catalog_item"
                                + " WHERE catalog_type = 'M15.ROLE' AND code = 'AC-16'",
                        String.class))
                .containsExactly("RESIDENT/SELF");
        List<Row> expected = Appendix27Matrix.rows(Set.of("AC-16"));

        assertThat(expected).isNotEmpty();
        assertThat(loaded("APPENDIX_27").stream()
                        .filter(r -> r.role().equals("AC-16"))
                        .toList())
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void FR_018_section19_3Adjustments_exactlyAsListed() {
        assertThat(loaded("SECTION_19_3")).containsExactlyInAnyOrderElementsOf(SECTION_19_3);
    }

    @Test
    void FR_022_fieldVisibilityRules_matchSection19_3() {
        List<String> rules = jdbc.query(
                "SELECT SUBSTRING_INDEX(code, ':', 1), SUBSTRING_INDEX(code, ':', -1), attributes->>'$.visibility'"
                        + " FROM catalog_item WHERE catalog_type = 'M15.FIELD_VISIBILITY'",
                (rs, i) -> rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3));

        assertThat(rules).containsExactlyInAnyOrderElementsOf(FIELD_VISIBILITY);
    }

    private List<Row> loaded(String source) {
        return jdbc.query(
                "SELECT role, function_code, operation, scope, footnote_codes FROM permission_matrix WHERE source = ?",
                (rs, i) -> new Row(
                        rs.getString(1),
                        Function.valueOf(rs.getString(2)),
                        Operation.valueOf(rs.getString(3)),
                        rs.getString(4),
                        codes(rs.getString(5))),
                source);
    }

    /** Mảng JSON chuỗi, ví dụ {@code ["10", "43"]}, thành danh sách mã tăng dần. */
    private static List<String> codes(String json) {
        String inner = json.trim().replaceAll("^\\[|]$", "").trim();
        if (inner.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(inner.split(","))
                .map(s -> s.trim().replace("\"", ""))
                .sorted(Appendix27Matrix::compareCodes)
                .toList();
    }
}
