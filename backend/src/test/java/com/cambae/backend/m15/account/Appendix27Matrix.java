package com.cambae.backend.m15.account;

import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.Operation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Đọc Phụ lục 27 của {@code docs/business/nghiep-vu.md} và quy đổi từng ô thành dòng quyền tối đa theo luật đã chốt ở
 * plan 002 "Ghi chú triển khai" T007 (research A-04, spec FR-020). Dùng trong {@code PermissionMatrixMatchesSourceIT}
 * để đối chiếu với {@code permission_matrix} đã nạp (T017).
 */
final class Appendix27Matrix {

    static final Path SOURCE = Path.of("..", "docs", "business", "nghiep-vu.md");

    /** Cột vai trò của Phụ lục 27, theo thứ tự: QL, TT, BS, ĐD, CS, Bếp, HC, NT, KT, NCT. */
    static final List<String> ROLE_COLUMNS =
            List.of("AC-01", "AC-02", "AC-03", "AC-04", "AC-05", "AC-07", "AC-09", "AC-10", "AC-14", "AC-16");

    /** Vai trò có phạm vi T, D theo phân công (19.3; Phụ lục 27 "Phạm vi của T và D theo vai trò"). */
    static final Set<String> ASSIGNED_ROLES = Set.of("AC-02", "AC-04", "AC-05");

    static final String FAMILY_MEMBER = "AC-10";

    /** Dòng có ghi nhận nhóm 3: ô T thêm CONFIRM (spec FR-020 bảng loại thao tác "Xác nhận"). */
    static final Set<Function> RECORDING_FUNCTIONS = EnumSet.of(
            Function.TASK_CHECKLIST,
            Function.DAY_CARE_ATTENDANCE,
            Function.ACTIVITY_OUTING,
            Function.QUALITY_CHECK,
            Function.VITAL_SIGN,
            Function.INCIDENT_EMERGENCY,
            Function.MEDICATION_ADMINISTRATION,
            Function.MEDICATION_RECONCILIATION,
            Function.MEDICATION_RECEIPT,
            Function.MEAL_PREPARATION,
            Function.MEAL_RECEIPT,
            Function.SPECIAL_MEAL_SERVICE,
            Function.FAMILY_FOOD_CHECK,
            Function.SHIFT_HANDOVER,
            Function.BELONGINGS,
            Function.BALANCE_LEDGER);

    /** Dòng chuyên môn: ô T thêm PROFESSIONAL (spec FR-020 "T³, T⁴ và T trên các dòng chuyên môn"; data-model mục 4). */
    static final Set<Function> PROFESSIONAL_FUNCTIONS =
            EnumSet.of(Function.PRESCRIPTION, Function.MEDICATION_ADMINISTRATION);

    /** Chú thích ghi "toàn viện" cho ô P: ¹¹ (Bác sĩ, Hành chính), ²⁹ (Kế toán) ở dòng "Dashboard, báo cáo". */
    static final Set<String> FACILITY_WIDE_FOOTNOTES = Set.of("11", "29");

    private static final Pattern TOKEN = Pattern.compile("^([XPTDC])((?:[⁰¹²³⁴⁵⁶⁷⁸⁹]+\\s?)*)$");
    private static final String SUPERSCRIPTS = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    /** Một dòng quyền: khóa (role, function, operation), phạm vi, mã chú thích tăng dần. */
    record Row(String role, Function function, Operation operation, String scope, List<String> footnotes) {

        String key() {
            return role + "|" + function + "|" + operation;
        }
    }

    private Appendix27Matrix() {}

    /** Các dòng quyền của Phụ lục 27 cho những vai trò trong {@code roles}. */
    static List<Row> rows(Set<String> roles) throws IOException {
        return rows(Files.readString(SOURCE, StandardCharsets.UTF_8), roles);
    }

    static List<Row> rows(String document, Set<String> roles) {
        Map<String, Function> byLabel =
                Arrays.stream(Function.values()).collect(Collectors.toMap(Function::label, f -> f));
        Map<String, Row> result = new LinkedHashMap<>();
        for (List<String> cells : tableRows(document)) {
            String label =
                    cells.get(0).replaceAll("[" + SUPERSCRIPTS + "]+$", "").trim();
            Function function = byLabel.get(label);
            if (function == null) {
                throw new IllegalStateException("Dòng Phụ lục 27 không có trong Function: " + label);
            }
            for (int i = 0; i < ROLE_COLUMNS.size(); i++) {
                String role = ROLE_COLUMNS.get(i);
                String cell = cells.get(i + 2);
                if (!roles.contains(role) || cell.startsWith("—")) {
                    continue;
                }
                for (String token : cell.split(",")) {
                    addToken(result, role, function, token.trim());
                }
            }
        }
        return new ArrayList<>(result.values());
    }

    /** Tên các dòng của Phụ lục 27, không kèm chú thích. */
    static List<String> labels(String document) {
        return tableRows(document).stream()
                .map(c -> c.get(0).replaceAll("[" + SUPERSCRIPTS + "]+$", "").trim())
                .toList();
    }

    private static List<List<String>> tableRows(String document) {
        String section = document.split("## 27\\. Phụ lục", 2)[1].split("\n## 28\\.", 2)[0];
        List<List<String>> rows = new ArrayList<>();
        for (String line : section.split("\n")) {
            String l = line.strip();
            if (!l.startsWith("| ") || l.startsWith("| Chức năng") || l.startsWith("| ---")) {
                continue;
            }
            String[] parts = l.substring(1, l.length() - 1).split("\\|", -1);
            List<String> cells = Arrays.stream(parts).map(String::strip).toList();
            if (cells.size() != ROLE_COLUMNS.size() + 2) {
                throw new IllegalStateException("Dòng Phụ lục 27 sai số cột: " + l);
            }
            rows.add(cells);
        }
        return rows;
    }

    private static void addToken(Map<String, Row> result, String role, Function function, String token) {
        Matcher m = TOKEN.matcher(token);
        if (!m.matches()) {
            throw new IllegalStateException("Ký hiệu không đọc được ở " + function + "/" + role + ": " + token);
        }
        List<String> footnotes = footnotes(m.group(2));
        char symbol = m.group(1).charAt(0);
        String executionScope =
                FAMILY_MEMBER.equals(role) ? "RELATIONSHIP" : ASSIGNED_ROLES.contains(role) ? "ASSIGNED" : "FACILITY";
        switch (symbol) {
            case 'X' -> put(result, role, function, Operation.VIEW, viewScope(role, "FACILITY"), footnotes);
            case 'P' -> {
                boolean facilityWide = footnotes.stream().anyMatch(FACILITY_WIDE_FOOTNOTES::contains);
                put(
                        result,
                        role,
                        function,
                        Operation.VIEW,
                        viewScope(role, facilityWide ? "FACILITY" : "ASSIGNED"),
                        footnotes);
            }
            case 'C' -> {
                for (Operation op : List.of(Operation.VIEW, Operation.CREATE, Operation.UPDATE)) {
                    put(result, role, function, op, executionScope, footnotes);
                }
            }
            case 'T' -> {
                for (Operation op : List.of(Operation.VIEW, Operation.CREATE, Operation.UPDATE)) {
                    put(result, role, function, op, executionScope, footnotes);
                }
                if (RECORDING_FUNCTIONS.contains(function)) {
                    put(result, role, function, Operation.CONFIRM, executionScope, footnotes);
                }
                if (PROFESSIONAL_FUNCTIONS.contains(function)) {
                    put(result, role, function, Operation.PROFESSIONAL, executionScope, footnotes);
                }
            }
            case 'D' -> {
                put(result, role, function, Operation.VIEW, executionScope, footnotes);
                put(result, role, function, Operation.APPROVE, executionScope, footnotes);
                if (function == Function.PERIOD_CLOSING) {
                    put(result, role, function, Operation.FINALIZE, executionScope, footnotes);
                }
            }
            default -> throw new IllegalStateException("Ký hiệu lạ: " + symbol);
        }
    }

    /** Người thân luôn theo quan hệ (FR-045). */
    private static String viewScope(String role, String scope) {
        return FAMILY_MEMBER.equals(role) ? "RELATIONSHIP" : scope;
    }

    /** Hai ký hiệu của cùng ô cho cùng thao tác: phạm vi rộng hơn thắng (FR-017), chú thích hợp lại. */
    private static void put(
            Map<String, Row> result,
            String role,
            Function function,
            Operation op,
            String scope,
            List<String> footnotes) {
        Row row = new Row(role, function, op, scope, footnotes);
        result.merge(row.key(), row, (a, b) -> {
            Set<String> merged = new TreeSet<>(Appendix27Matrix::compareCodes);
            merged.addAll(a.footnotes());
            merged.addAll(b.footnotes());
            return new Row(role, function, op, wider(a.scope(), b.scope()), List.copyOf(merged));
        });
    }

    private static String wider(String a, String b) {
        return a.equals("FACILITY") || b.equals("FACILITY") ? "FACILITY" : a;
    }

    private static List<String> footnotes(String group) {
        Set<String> codes = new TreeSet<>(Appendix27Matrix::compareCodes);
        for (String g : group.trim().split("\\s+")) {
            if (g.isEmpty()) {
                continue;
            }
            StringBuilder code = new StringBuilder();
            for (char c : g.toCharArray()) {
                code.append(SUPERSCRIPTS.indexOf(c));
            }
            codes.add(code.toString());
        }
        return List.copyOf(codes);
    }

    static int compareCodes(String a, String b) {
        return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
    }
}
