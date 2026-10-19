package com.cambae.backend.m15.sample;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lần xuất báo cáo mẫu (000 T065): hành động không đổi dữ liệu, ghi một bản nhật ký có lý do là mục đích xuất và phần
 * chi tiết là bộ lọc (FR-043a, FR-044). Chức năng xuất thật thuộc 016.
 */
@Service
public class SampleReportService {

    public static final String REPORT = "M16.REPORT";
    static final AuditAction REPORT_EXPORTED = new AuditAction("REPORT_EXPORTED", true);

    private final AuditLogApi auditLog;

    SampleReportService(AuditLogApi auditLog) {
        this.auditLog = auditLog;
    }

    @Transactional
    public void export(String reportCode, Reason purpose, Map<String, Object> filter) {
        auditLog.write(AuditLogEntry.builder()
                .action(REPORT_EXPORTED)
                .entity(REPORT, reportCode)
                .reason(purpose)
                .details(Map.of("filter", filter, "identifiesResidents", true))
                .build());
    }
}
