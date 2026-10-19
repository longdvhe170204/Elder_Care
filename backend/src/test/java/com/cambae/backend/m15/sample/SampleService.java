package com.cambae.backend.m15.sample;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.data.CatalogStatus;
import com.cambae.backend.common.data.EffectiveVersioning;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lệnh trên dữ liệu mẫu của ba nhóm, mỗi lệnh ghi nhật ký cùng transaction (000 US1, T048; FR-043, FR-007). */
@Service
public class SampleService {

    static final String CATALOG = "M15.SAMPLE_CATALOG";
    static final String CASE = "M15.SAMPLE_CASE";
    static final AuditAction CATALOG_CREATED = new AuditAction("SAMPLE_CATALOG_CREATED", false);
    static final AuditAction CATALOG_DEACTIVATED = new AuditAction("SAMPLE_CATALOG_DEACTIVATED", false);
    static final AuditAction CATALOG_REACTIVATED = new AuditAction("SAMPLE_CATALOG_REACTIVATED", false);
    static final AuditAction CATALOG_DELETED = new AuditAction("SAMPLE_CATALOG_DELETED", false);
    static final AuditAction CASE_CREATED = new AuditAction("SAMPLE_CASE_CREATED", false);
    static final AuditAction CASE_CANCELLED = new AuditAction("SAMPLE_CASE_CANCELLED", true);
    static final AuditAction CASE_VERSION_ADDED = new AuditAction("SAMPLE_CASE_VERSION_ADDED", true);
    private static final EffectiveVersioning.VersionedTable CASE_VERSIONS =
            new EffectiveVersioning.VersionedTable("sample_case", "sample_case_version", "case_id");

    private final SampleCatalogRepository catalog;
    private final SampleCaseRepository cases;
    private final AuditLogApi auditLog;
    private final EffectiveVersioning versioning;
    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    SampleService(
            SampleCatalogRepository catalog,
            SampleCaseRepository cases,
            AuditLogApi auditLog,
            EffectiveVersioning versioning,
            JdbcTemplate jdbc,
            BusinessTime time) {
        this.catalog = catalog;
        this.cases = cases;
        this.auditLog = auditLog;
        this.versioning = versioning;
        this.jdbc = jdbc;
        this.time = time;
    }

    @Transactional
    public long createCatalogItem(String name) {
        SampleCatalogItem item = catalog.saveAndFlush(new SampleCatalogItem(name));
        log(CATALOG_CREATED, CATALOG, item.getId(), null, Map.of("name", name));
        return item.getId();
    }

    @Transactional
    public void deactivate(long id) {
        SampleCatalogItem item = catalogItem(id);
        item.deactivate();
        log(CATALOG_DEACTIVATED, CATALOG, id, Map.of("status", "ACTIVE"), Map.of("status", "INACTIVE"));
    }

    @Transactional
    public void reactivate(long id) {
        SampleCatalogItem item = catalogItem(id);
        item.reactivate(catalog.findByNameAndStatus(item.getName(), CatalogStatus.ACTIVE));
        catalog.flush();
        log(CATALOG_REACTIVATED, CATALOG, id, Map.of("status", "INACTIVE"), Map.of("status", "ACTIVE"));
    }

    /** Xóa chỉ khi chưa được bản ghi nhóm 2, 3 nào tham chiếu; nếu có, khóa ngoại RESTRICT chặn (FR-002). */
    @Transactional
    public void deleteCatalogItem(long id) {
        SampleCatalogItem item = catalogItem(id);
        catalog.delete(item);
        catalog.flush();
        log(CATALOG_DELETED, CATALOG, id, Map.of("name", item.getName()), null);
    }

    /** Nghiệp vụ mới chỉ chọn được mục đang Hiệu lực (FR-003). */
    @Transactional
    public long createCase(long catalogId, long actorId) {
        SampleCatalogItem item = catalogItem(catalogId);
        if (!item.isActive()) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-003", "Mục danh mục đã ngừng hiệu lực")));
        }
        SampleCase created = cases.saveAndFlush(new SampleCase(item, time.now(), actorId));
        log(CASE_CREATED, CASE, created.getId(), null, Map.of("status", SampleCase.OPEN));
        return created.getId();
    }

    @BusinessCommand
    @Transactional
    public void cancelCase(long id, Reason reason) {
        SampleCase sample = cases.findById(id).orElseThrow(NotFoundException::new);
        sample.cancel();
        cases.flush();
        auditLog.write(AuditLogEntry.builder()
                .action(CASE_CANCELLED)
                .entity(CASE, id)
                .before(Map.of("status", SampleCase.OPEN))
                .after(Map.of("status", SampleCase.CANCELLED))
                .reason(reason)
                .build());
    }

    /** Thêm phiên bản có hiệu lực, không chồng khoảng với phiên bản khác (FR-008). */
    @BusinessCommand
    @Transactional
    public void addCaseVersion(long caseId, LocalDate from, LocalDate to, Reason reason) {
        versioning.addVersion(
                CASE_VERSIONS,
                caseId,
                from,
                to,
                () -> jdbc.update(
                        "INSERT INTO sample_case_version (case_id, effective_from, effective_to) VALUES (?, ?, ?)",
                        caseId,
                        from,
                        to));
        auditLog.write(AuditLogEntry.builder()
                .action(CASE_VERSION_ADDED)
                .entity(CASE, caseId)
                .after(Map.of("effectiveFrom", from.toString(), "effectiveTo", String.valueOf(to)))
                .reason(reason)
                .build());
    }

    /** Nghiệp vụ cũ vẫn hiển thị đúng mục danh mục đã ngừng hiệu lực (FR-003). */
    @Transactional(readOnly = true)
    public Map<String, Object> readCase(long id) {
        SampleCase sample = cases.findById(id).orElseThrow(NotFoundException::new);
        return Map.of(
                "id",
                id,
                "status",
                sample.getStatus(),
                "catalogName",
                sample.getCatalog().getName());
    }

    private SampleCatalogItem catalogItem(long id) {
        return catalog.findById(id).orElseThrow(NotFoundException::new);
    }

    private void log(AuditAction action, String type, long id, Map<String, Object> before, Map<String, Object> after) {
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(type, id)
                .before(before)
                .after(after)
                .build());
    }
}
