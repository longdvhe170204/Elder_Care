package com.cambae.backend.m14.internal.export.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.document.FacilityInfo;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.m14.internal.export.domain.ExportContent;
import com.cambae.backend.m14.internal.export.domain.ExportWorkbook;
import com.cambae.backend.m14.internal.metric.domain.ReportFilters;
import com.cambae.backend.m14.internal.report.domain.Report;
import com.cambae.backend.m14.internal.report.domain.ReportGroup;
import com.cambae.backend.m14.internal.report.service.ReportService;
import com.cambae.backend.m14.internal.scope.service.ReportPeriods;
import com.cambae.backend.m14.internal.scope.service.ReportPeriods.ReportPeriod;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.AuditLogPermissions;
import com.cambae.backend.m15.auditlog.api.AuditLogRecord;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.m15.parameter.api.ParameterValue;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xuất báo cáo ({@code XuatBaoCaoService}; FR-060 → FR-062, research O-08): tính báo cáo như màn hình, quyết định nội dung
 * theo vai trò ({@link ExportContent}), dựng XLSX, lưu ở kho tệp loại {@link #FILE_OWNER} rồi ghi nhật ký
 * {@code REPORT_EXPORTED} trong cùng transaction; mã tệp chỉ trả sau khi nhật ký đã ghi, lỗi ghi nhật ký thì không có tệp
 * (000 FR-047). Tệp không đổi về sau; muốn số mới thì xuất lại. Lần xuất là bản ghi nhật ký, M14 không có bảng.
 */
@Service
@Transactional
public class ReportExportService {

    /** Loại chủ sở hữu tệp xuất ({@code M14.BAO_CAO_XUAT}), cũng là loại đối tượng của bản ghi nhật ký. */
    public static final String FILE_OWNER = "M14.REPORT_EXPORT";

    /** Hành động nhật ký; bắt buộc lý do (mục đích) khi có định danh người cao tuổi (000 FR-044, Q-198). */
    static final String ACTION = "REPORT_EXPORTED";

    static final AuditAction LISTED = new AuditAction(ACTION, false);

    public enum ExportFormat {
        XLSX,
        CSV
    }

    public record ExportRequest(
            ReportGroup group,
            LocalDate from,
            LocalDate to,
            ReportFilters filters,
            boolean includeIdentity,
            Optional<String> purpose,
            ExportFormat format) {}

    public record ExportResult(long fileId, long auditLogId) {}

    /** Một lần xuất đọc từ nhật ký ({@code GET /reports/exports}). */
    public record ExportRecord(
            long auditLogId,
            long fileId,
            Instant exportedAt,
            Long exportedBy,
            String role,
            String group,
            String from,
            String to,
            boolean includeIdentity,
            String purpose,
            Integer rowCount) {

        static ExportRecord of(AuditLogRecord r) {
            Map<String, Object> d = r.details() == null ? Map.of() : r.details();
            return new ExportRecord(
                    r.id(),
                    Long.parseLong(r.entityId()),
                    r.occurredAt(),
                    r.actorAccountId(),
                    r.role(),
                    (String) d.get("group"),
                    (String) d.get("from"),
                    (String) d.get("to"),
                    Boolean.TRUE.equals(d.get("includeIdentity")),
                    r.reason(),
                    d.get("rowCount") instanceof Number n ? n.intValue() : null);
        }
    }

    private final CurrentUser user;
    private final ReportPeriods periods;
    private final ReportService reports;
    private final FileApi files;
    private final AuditLogApi auditLog;
    private final AccountApi accounts;
    private final PermissionApi permissions;
    private final ParameterApi parameters;

    public ReportExportService(
            CurrentUser user,
            ReportPeriods periods,
            ReportService reports,
            FileApi files,
            AuditLogApi auditLog,
            AccountApi accounts,
            PermissionApi permissions,
            ParameterApi parameters) {
        this.user = user;
        this.periods = periods;
        this.reports = reports;
        this.files = files;
        this.auditLog = auditLog;
        this.accounts = accounts;
        this.permissions = permissions;
        this.parameters = parameters;
    }

    public ExportResult export(ExportRequest request) {
        if (request.format() == ExportFormat.CSV) {
            // kho tệp chưa nhận text/csv (common.file.FileTypes.SYSTEM_GENERATED); CSV là tùy chọn ở research O-08
            throw new InvalidValueException("Chưa hỗ trợ xuất CSV; chọn XLSX");
        }
        ReportPeriod period = periods.check(request.from(), request.to());
        Report report = reports.period(user, request.group(), period, request.filters());
        ExportContent content =
                ExportContent.decide(user.roles(), request.includeIdentity(), request.purpose(), report.results());
        Optional<String> purpose = request.purpose().filter(p -> !p.isBlank());

        byte[] xlsx = ExportWorkbook.build(
                facility(),
                new ExportWorkbook.Header(
                        "Báo cáo " + request.group().label(),
                        exportedBy(),
                        report.asOf(),
                        period.from(),
                        period.to(),
                        request.filters().toMap(),
                        request.group() == ReportGroup.INCURRED_COSTS ? costMode(request.filters()) : null,
                        purpose.orElse(null)),
                content);
        String name =
                "bao-cao-" + request.group().name().toLowerCase() + "-" + period.from() + "-" + period.to() + ".xlsx";
        long fileId = files.upload(name, FileTypes.XLSX, xlsx, user.accountId());
        files.attach(fileId, FILE_OWNER, fileId);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("group", request.group().name());
        details.put("from", period.from().toString());
        details.put("to", period.to().toString());
        details.put("filters", request.filters().toMap());
        details.put("scope", scope(report.scope()));
        if (request.group() == ReportGroup.INCURRED_COSTS) {
            details.put("costMode", costMode(request.filters()));
        }
        details.put("rowCount", content.rowCount());
        details.put("includeIdentity", content.includeIdentity());
        details.put("format", request.format().name());
        auditLog.write(AuditLogEntry.builder()
                .action(new AuditAction(ACTION, content.includeIdentity()))
                .entity(FILE_OWNER, fileId)
                .reason(purpose.map(Reason::new).orElse(null))
                .details(details)
                .build());
        long auditLogId = auditLog.recorded(LISTED, FILE_OWNER, String.valueOf(fileId), null)
                .getFirst()
                .id();
        return new ExportResult(fileId, auditLogId);
    }

    /** Quản lý viện thấy mọi lần xuất (000 FR-049); người khác chỉ lần của mình (FR-062). */
    @Transactional(readOnly = true)
    public List<ExportRecord> list() {
        Long actor = seesEveryExport() ? null : user.accountId();
        return auditLog.recorded(LISTED, FILE_OWNER, null, actor).stream()
                .map(ExportRecord::of)
                .toList();
    }

    /** Người được tải tệp xuất: người xuất, Quản lý viện (data-model mục 1). */
    @Transactional(readOnly = true)
    public boolean canDownload(long fileId) {
        return seesEveryExport()
                || !auditLog.recorded(LISTED, FILE_OWNER, String.valueOf(fileId), user.accountId())
                        .isEmpty();
    }

    private boolean seesEveryExport() {
        return permissions
                .hasPermission(user, AuditLogPermissions.VIEW, Target.UNSPECIFIED)
                .allowed();
    }

    private String exportedBy() {
        String roles = user.roles().stream().map(Role::code).sorted().collect(Collectors.joining(", "));
        return accounts.fullName(user.accountId()).orElse("Tài khoản " + user.accountId()) + " (" + roles + ")";
    }

    private FacilityInfo facility() {
        ParameterValue value = parameters.read(ParameterCode.CFG_M11_11);
        return value.isSet() ? value.asTable(FacilityInfo.class) : null;
    }

    private static String costMode(ReportFilters filters) {
        return (filters.costMode() == null ? ReportFilters.CostMode.FINALIZED : filters.costMode()).name();
    }

    private static Object scope(ScopeFilter scope) {
        if (scope.facilityWide()) {
            return "FACILITY";
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("floorAreaIds", scope.floorAreaIds().stream().sorted().toList());
        map.put("roomIds", scope.roomIds().stream().sorted().toList());
        map.put("residentIds", scope.residentIds().stream().sorted().toList());
        return map;
    }
}
