package com.eldercare.backend.billing.balance.internal.export.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.billing.balance.internal.document.service.FamilyCostsRight;
import com.eldercare.backend.billing.balance.internal.export.domain.BalanceExportRecords;
import com.eldercare.backend.billing.balance.internal.export.domain.BalanceExportRecords.BalanceExportRecord;
import com.eldercare.backend.billing.balance.internal.export.domain.BalanceExportRecords.Type;
import com.eldercare.backend.billing.balance.internal.export.domain.BalanceStatement;
import com.eldercare.backend.billing.balance.internal.export.domain.ExportFormat;
import com.eldercare.backend.billing.balance.internal.export.domain.IncomeExpenseReport;
import com.eldercare.backend.billing.balance.internal.export.persistence.ConfirmedEntries;
import com.eldercare.backend.billing.balance.internal.export.service.BalanceExportDocument.ResidentStatement;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementService;
import com.eldercare.backend.billing.balance.internal.transaction.domain.BalanceEntry;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileContent;
import com.eldercare.backend.common.file.FileService;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xuất sao kê và báo cáo thu chi (017 T038, US8; FR-025 → FR-027; UC-88; research E-11; Q-381, Q-386). Lệnh của Kế toán
 * dựng file một lần từ giao dịch Đã xác nhận, lưu qua {@code FileApi}, ghi lần xuất ở {@code record_event} và nhật ký;
 * tải lại trả đúng file đã dựng. Người thân có quyền xem chi phí xuất sao kê của người cao tuổi đó qua cổng (ghi nhật ký,
 * không lưu file). Khoảng không dài hơn CFG-M14-01, nếu vượt thì 422 {@code EXPORT_RANGE_EXCEEDED}.
 */
@Service
public class BalanceExportService {

    static final AuditAction EXPORTED = new AuditAction("BALANCE_EXPORTED", false);

    /** Một lần xuất cho màn hình (schema {@code BalanceExport}). */
    public record Export(
            long id, Type type, ExportFormat format, LocalDate from, LocalDate to, int rowCount, Instant at) {}

    private final ConfirmedEntries entries;
    private final BalanceStatistics statistics;
    private final RecordEventApi records;
    private final FileApi files;
    private final FileService fileContents;
    private final FamilyCostsRight familyRight;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;

    BalanceExportService(
            ConfirmedEntries entries,
            BalanceStatistics statistics,
            RecordEventApi records,
            FileApi files,
            FileService fileContents,
            FamilyCostsRight familyRight,
            ResidentApi residents,
            ParameterApi parameters,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time) {
        this.entries = entries;
        this.statistics = statistics;
        this.records = records;
        this.files = files;
        this.fileContents = fileContents;
        this.familyRight = familyRight;
        this.residents = residents;
        this.parameters = parameters;
        this.audit = audit;
        this.user = user;
        this.time = time;
    }

    /** FR-025 (a): sao kê của một hoặc nhiều hồ sơ. */
    @RequiresPermission(function = Function.STATEMENT_REPORT, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Export statement(Target target, List<Long> residentIds, LocalDate from, LocalDate to, ExportFormat format) {
        checkRange(from, to);
        List<Long> ids = List.copyOf(new LinkedHashSet<>(residentIds));
        List<ResidentStatement> statements = statements(ids, from, to);
        byte[] content = statementFile(statements, from, to, format);
        int rows =
                statements.stream().mapToInt(s -> s.statement().lines().size()).sum();
        return store(Type.BALANCE_STATEMENT, format, from, to, ids, rows, content);
    }

    /** FR-025 (b), Q-386: báo cáo thu chi thu gọn theo khoảng. */
    @RequiresPermission(function = Function.STATEMENT_REPORT, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Export incomeExpense(Target target, LocalDate from, LocalDate to, ExportFormat format) {
        checkRange(from, to);
        IncomeExpenseReport report = statistics.report(from, to);
        Map<Long, String> names = new HashMap<>();
        report.debtors()
                .forEach(d -> names.put(
                        d.residentId(), residents.identity(d.residentId()).fullName()));
        FacilityInfo facility = facility();
        byte[] content = format == ExportFormat.PDF
                ? BalanceExportDocument.incomeExpensePdf(facility, null, report, names, from, to)
                : BalanceExportDocument.incomeExpenseExcel(facility, report, names, from, to);
        return store(
                Type.INCOME_EXPENSE,
                format,
                from,
                to,
                List.of(),
                3 + report.debtors().size(),
                content);
    }

    /** FR-027: tải đúng file đã dựng; mỗi lần tải ghi nhật ký. Quyền tệp kiểm lại ở {@code FilePermissionProvider}. */
    @RequiresPermission(function = Function.STATEMENT_REPORT, operation = Operation.VIEW)
    @Transactional
    public Download download(Target target, long exportId) {
        RecordEvent<BalanceExportRecord> row = records.require(BalanceExportRecords.KIND, exportId);
        BalanceExportRecord c = row.content();
        FileContent file = fileContents.download(c.fileId(), user.accountId());
        audit.write(AuditLogEntry.builder()
                .action(SettlementService.DOWNLOADED)
                .entity(BalanceExportRecords.KIND.code(), exportId)
                .after(Map.of("format", c.format().name(), "type", c.type().name()))
                .build());
        return new Download(c.format(), file);
    }

    /** FR-026: người thân có quyền xem chi phí tải sao kê của người cao tuổi đó; ghi nhật ký, không lưu file. */
    @RequiresPermission(function = Function.STATEMENT_REPORT, operation = Operation.VIEW)
    @Transactional
    public Download portalStatement(Target target, long residentId, LocalDate from, LocalDate to, ExportFormat format) {
        familyRight.require(residentId);
        checkRange(from, to);
        List<ResidentStatement> statements = statements(List.of(residentId), from, to);
        byte[] content = statementFile(statements, from, to, format);
        audit.write(AuditLogEntry.builder()
                .action(EXPORTED)
                .entity(BalanceExportRecords.KIND.code(), residentId)
                .resident(residentId)
                .after(exportDetails(
                        Type.BALANCE_STATEMENT,
                        format,
                        from,
                        to,
                        List.of(residentId),
                        statements.getFirst().statement().lines().size()))
                .build());
        return new Download(
                format, new FileContent("sao-ke-" + residentId + "." + format.extension(), format.mimeType(), content));
    }

    /** Tệp và định dạng của một lần tải. */
    public record Download(ExportFormat format, FileContent file) {}

    private List<ResidentStatement> statements(List<Long> ids, LocalDate from, LocalDate to) {
        Instant end = time.startOfDay(to.plusDays(1));
        Map<Long, List<BalanceEntry>> byResident =
                entries.upTo(ids, end).stream().collect(Collectors.groupingBy(BalanceEntry::residentId));
        List<ResidentStatement> statements = new ArrayList<>();
        for (long id : ids) {
            statements.add(new ResidentStatement(
                    residents.identity(id).fullName(),
                    BalanceStatement.of(byResident.getOrDefault(id, List.of()), from, to)));
        }
        return statements;
    }

    private byte[] statementFile(
            List<ResidentStatement> statements, LocalDate from, LocalDate to, ExportFormat format) {
        FacilityInfo facility = facility();
        return format == ExportFormat.PDF
                ? BalanceExportDocument.statementPdf(facility, null, statements, from, to)
                : BalanceExportDocument.statementExcel(facility, statements, from, to);
    }

    private Export store(
            Type type, ExportFormat format, LocalDate from, LocalDate to, List<Long> ids, int rows, byte[] content) {
        Instant now = time.now();
        long by = user.accountId();
        String role = user.roles().stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        long fileId = files.upload(
                (type == Type.BALANCE_STATEMENT ? "sao-ke-" : "bao-cao-thu-chi-") + from + "-" + to + "."
                        + format.extension(),
                format.mimeType(),
                content,
                by);
        RecordEvent<BalanceExportRecord> row = records.add(
                BalanceExportRecords.KIND,
                new EntityRef(BalanceExportRecords.PARENT_TYPE, by),
                new NewRecordEvent<>(
                        new BalanceExportRecord(type, format, from, to, ids, rows, role, fileId),
                        null,
                        by,
                        false,
                        now,
                        null,
                        now,
                        null,
                        null));
        files.attach(fileId, BalanceExportRecords.KIND.code(), row.id());
        audit.write(AuditLogEntry.builder()
                .action(EXPORTED)
                .entity(BalanceExportRecords.KIND.code(), row.id())
                .after(exportDetails(type, format, from, to, ids, rows))
                .build());
        return new Export(row.id(), type, format, from, to, rows, now);
    }

    private static Map<String, Object> exportDetails(
            Type type, ExportFormat format, LocalDate from, LocalDate to, List<Long> ids, int rows) {
        var details = new LinkedHashMap<String, Object>();
        details.put("type", type.name());
        details.put("format", format.name());
        details.put("from", from.toString());
        details.put("to", to.toString());
        details.put("residentIds", ids);
        details.put("rowCount", rows);
        return details;
    }

    private void checkRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", "Khoảng ngày không hợp lệ");
        }
        Period maximum = parameters.read(ParameterCode.CFG_M14_01).asPeriod();
        if (!to.isBefore(from.plus(maximum))) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "EXPORT_RANGE_EXCEEDED",
                    "Khoảng xuất dài hơn giới hạn " + maximum + " (CFG-M14-01)",
                    Map.of("maximum", maximum.toString()));
        }
    }

    private FacilityInfo facility() {
        var value = parameters.read(ParameterCode.CFG_M11_11);
        return value.isSet() ? value.asTable(FacilityInfo.class) : null;
    }
}
