package com.eldercare.backend.billing.internal.export.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.billing.internal.export.domain.CostExportFile;
import com.eldercare.backend.billing.internal.export.persistence.CostExportStore;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.provision.service.FinalizedCostViews;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.document.ExcelDocument;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-039/041: file và dấu lần đầu cùng transaction; nhật ký chung giữ lịch sử xuất. */
@Service
public class CostAccountingExport {
    private static final AuditAction EXPORT = new AuditAction("COST_ACCOUNTING_EXPORT", false);
    private final CostAccess access;
    private final CostStatementStore statements;
    private final CostExportStore store;
    private final FinalizedCostViews views;
    private final ResidentApi residents;
    private final AuditLogApi audit;
    private final BusinessTime time;

    public CostAccountingExport(
            CostAccess access,
            CostStatementStore statements,
            CostExportStore store,
            FinalizedCostViews views,
            ResidentApi residents,
            AuditLogApi audit,
            BusinessTime time) {
        this.access = access;
        this.statements = statements;
        this.store = store;
        this.views = views;
        this.residents = residents;
        this.audit = audit;
        this.time = time;
    }

    @Transactional
    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.PERIOD_CLOSING, operation = Operation.CREATE)
    public CostExportFile export(long period, String scope, String format) {
        access.require(Role.ACCOUNTANT, Function.PERIOD_CLOSING, Operation.CREATE, "M11.COST_PERIOD", period, null);
        if (!Set.of("ALL", "NEVER_EXPORTED").contains(scope)
                || !Set.of("XLSX", "CSV").contains(format))
            throw new InvalidValueException("Phạm vi hoặc định dạng xuất không hợp lệ");
        statements.lockPeriod(period);
        var all = statements.list(period, null, null);
        var selected = all.stream()
                .filter(s -> s.status().equals("FINALIZED"))
                .filter(s -> scope.equals("ALL") || store.firstExport(s.id()) == null)
                .sorted(Comparator.comparingLong(s -> s.id()))
                .toList();
        var ids = selected.stream().map(s -> s.id()).toList();
        var first = new LinkedHashSet<Long>();
        selected.stream().filter(s -> store.firstExport(s.id()) == null).forEach(s -> first.add(s.id()));
        // FR-041: giữ nhãn của lần ALL gần nhất nếu tập bảng không đổi.
        var previous = scope.equals("ALL")
                ? previousAll(period)
                        .filter(a -> numbers(a.details().get("statementIds")).equals(ids))
                : Optional.<AuditLogRecord>empty();
        if (previous.isPresent()) {
            previous.ifPresent(a -> {
                first.clear();
                first.addAll(numbers(a.details().get("firstStatementIds")));
            });
        }
        List<String> headers = List.of(
                "Mã người cao tuổi",
                "Họ tên",
                "Kỳ",
                "Loại chi phí",
                "Mô tả",
                "Ngày phát sinh",
                "Số lượng",
                "Đơn vị",
                "Đơn giá",
                "Thành tiền",
                "Thuộc gói",
                "Tham chiếu nguồn",
                "Khoản gốc",
                "Kỳ gốc",
                "Loại điều chỉnh",
                "Mã bảng",
                "Lần xuất");
        List<List<?>> rows = new ArrayList<>();
        long total = 0;
        for (var s : selected) {
            var identity = residents.identity(s.residentId());
            for (var i : views.statement(s).items()) {
                total = Math.addExact(total, i.amount());
                var source = i.billingSource();
                rows.add(Arrays.asList(
                        identity.profileCode(),
                        identity.fullName(),
                        s.month().toString(),
                        typeLabel(i.costType()),
                        i.description(),
                        i.incurredOn(),
                        i.quantity(),
                        i.unit(),
                        i.unitPrice(),
                        i.amount(),
                        i.flags().contains("INCLUDED_IN_PACKAGE") ? "Có" : "Không",
                        source == null
                                ? ""
                                : source.sourceType() + ":" + source.sourceId() + ":" + source.billingUnit(),
                        i.originalItemId(),
                        i.originalPeriod(),
                        store.adjustmentType(i.id()),
                        s.id(),
                        first.contains(s.id()) ? "Xuất lần đầu" : "Đã xuất trước đó"));
            }
        }
        if (previous.isPresent() && previous.get().details().get("rows") instanceof List<?> saved) {
            rows.clear();
            saved.forEach(row -> {
                var values = new ArrayList<Object>((List<?>) row);
                if (values.get(5) instanceof String date) values.set(5, java.time.LocalDate.parse(date));
                rows.add(values);
            });
        }
        int count = rows.size();
        String control = "Tổng kiểm soát: " + ids.size() + " bảng; " + count + " khoản; " + total + " đồng";
        var pending = all.stream()
                .filter(s -> !s.status().equals("FINALIZED"))
                .map(s -> s.residentId())
                .distinct()
                .sorted()
                .map(id -> residents.identity(id))
                .map(i -> i.profileCode() + " — " + i.fullName())
                .toList();
        byte[] bytes;
        if (format.equals("CSV")) {
            var csv = new StringBuilder("\uFEFF");
            csv.append(csvRow(headers));
            rows.forEach(r -> csv.append(csvRow(r)));
            csv.append(csvRow(List.of(control)));
            csv.append(csvRow(List.of("Người cao tuổi có bảng chưa chốt")));
            pending.forEach(p -> csv.append(csvRow(List.of(p))));
            bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            var document = new ExcelDocument();
            var sheet = document.sheet("Chi phí đã chốt");
            var columns = new ArrayList<ExcelDocument.Column>();
            for (int c = 0; c < headers.size(); c++) {
                var cellFormat = c == 5
                        ? ExcelDocument.Format.DATE
                        : c == 6 || c == 12 || c == 15
                                ? ExcelDocument.Format.NUMBER
                                : c == 8 || c == 9 ? ExcelDocument.Format.MONEY : ExcelDocument.Format.TEXT;
                columns.add(new ExcelDocument.Column(headers.get(c), cellFormat));
            }
            sheet.table(columns, rows).line(control);
            var pendingSheet = document.sheet("Bảng chưa chốt");
            pendingSheet.line("Người cao tuổi có bảng chưa chốt");
            pending.forEach(pendingSheet::line);
            bytes = document.toBytes();
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("scope", scope);
        details.put("format", format);
        details.put("statementIds", ids);
        details.put("firstStatementIds", List.copyOf(first));
        details.put("itemCount", count);
        details.put("total", total);
        if (scope.equals("ALL")) details.put("rows", rows);
        audit.write(AuditLogEntry.builder()
                .action(EXPORT)
                .entity("M11.COST_PERIOD", period)
                .details(details)
                .build());
        long exportId = audit.recorded(
                        EXPORT,
                        "M11.COST_PERIOD",
                        Long.toString(period),
                        access.user().accountId())
                .getFirst()
                .id();
        selected.forEach(s -> {
            store.mark(s.id(), exportId);
            audit.write(AuditLogEntry.builder()
                    .action(new AuditAction("COST_STATEMENT_EXPORTED", false))
                    .entity("M11.COST_STATEMENT", s.id())
                    .resident(s.residentId())
                    .details(Map.of("exportId", exportId, "periodId", period, "scope", scope, "format", format))
                    .build());
        });
        return new CostExportFile(
                bytes,
                format.equals("CSV")
                        ? "text/csv;charset=UTF-8"
                        : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "costs-" + period + "." + format.toLowerCase(Locale.ROOT));
    }

    private static List<Long> numbers(Object raw) {
        return raw instanceof List<?> list
                ? list.stream().map(v -> ((Number) v).longValue()).toList()
                : List.of();
    }

    private Optional<AuditLogRecord> previousAll(long period) {
        AuditLogRecord last = null;
        long cursor = 0;
        while (true) {
            var page = audit.recordedBetween(
                    "M11.COST_PERIOD",
                    java.time.Instant.parse("1000-01-01T00:00:00Z"),
                    time.now().plusSeconds(1),
                    cursor,
                    AuditLogApi.RECORDED_LIMIT);
            for (var record : page) {
                if (record.details() != null
                        && record.entityId().equals(Long.toString(period))
                        && "ALL".equals(record.details().get("scope"))
                        && record.details().containsKey("rows")) last = record;
            }
            if (page.size() < AuditLogApi.RECORDED_LIMIT) return Optional.ofNullable(last);
            cursor = page.getLast().id();
        }
    }

    static String csvRow(List<?> values) {
        return values.stream()
                        .map(v -> {
                            String value = v instanceof Number
                                    ? new java.math.BigDecimal(v.toString())
                                            .stripTrailingZeros()
                                            .toPlainString()
                                    : Objects.toString(v, "");
                            String trimmed = value.stripLeading();
                            if (v instanceof String && !trimmed.isEmpty() && "=+@-".indexOf(trimmed.charAt(0)) >= 0)
                                value = "'" + value;
                            return "\"" + value.replace("\"", "\"\"") + "\"";
                        })
                        .collect(java.util.stream.Collectors.joining(","))
                + "\r\n";
    }

    private static String typeLabel(com.eldercare.backend.billing.api.dto.CostType type) {
        return switch (type) {
            case RESIDENCY_FEE -> "Phí lưu trú";
            case DAY_CARE_SESSION_FEE -> "Phí buổi bán trú";
            case SERVICE -> "Dịch vụ";
            case CONSUMABLE -> "Vật phẩm tiêu hao";
            case ACTIVITY -> "Hoạt động";
            case MEDICATION -> "Thuốc";
            case FAMILY_OVERNIGHT_STAY -> "Người thân ở lại";
            case FAMILY_MEAL -> "Suất ăn người thân";
            case PURCHASE_ASSISTANCE -> "Mua hộ";
            case OTHER -> "Khoản khác";
        };
    }
}
