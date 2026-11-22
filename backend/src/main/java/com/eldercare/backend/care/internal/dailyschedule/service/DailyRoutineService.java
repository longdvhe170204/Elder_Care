package com.eldercare.backend.care.internal.dailyschedule.service;

import static com.eldercare.backend.care.internal.dailyschedule.domain.DailyRoutineKinds.DAILY_ROUTINES;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.dailyschedule.domain.DailyRoutineAttributes;
import com.eldercare.backend.care.internal.dailyschedule.domain.DailyRoutineAttributes.Version;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 005 FR-056 (Q-416): Quản lý viện lập, sửa, ngừng mốc sinh hoạt cố định của toàn viện; mọi thay đổi áp dụng từ ngày hôm
 * sau. Dùng quyền "Danh mục công việc chăm sóc" (Phụ lục 27, Q-391): Quản lý viện cấu hình, nhân viên khác xem.
 */
@Service
public class DailyRoutineService {
    private final CareCatalogStore store;
    private final AuditLogApi audit;
    private final BusinessTime time;

    public DailyRoutineService(CareCatalogStore store, AuditLogApi audit, BusinessTime time) {
        this.store = store;
        this.audit = audit;
        this.time = time;
    }

    public record Routine(long id, String name, LocalTime time, boolean active, LocalDate effectiveFrom, int version) {}

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Routine> list() {
        return store.list(DAILY_ROUTINES).stream()
                .map(DailyRoutineService::view)
                .sorted(Comparator.comparing(Routine::time).thenComparing(Routine::name))
                .toList();
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public CatalogItem<DailyRoutineAttributes> create(String name, String at) {
        var version = new Version(tomorrow(), validName(name), validTime(at), true);
        var row = store.create(DAILY_ROUTINES, version.name(), new DailyRoutineAttributes(List.of(version)));
        log("CREATED", null, row);
        return row;
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public CatalogItem<DailyRoutineAttributes> update(long id, String etag, String name, String at) {
        var before = matched(id, etag);
        requireActive(before);
        var version = new Version(tomorrow(), validName(name), validTime(at), true);
        var row = store.update(
                DAILY_ROUTINES, before, version.name(), before.attributes().with(version));
        log("UPDATED", before, row);
        return row;
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void deactivate(long id, String etag) {
        var before = matched(id, etag);
        requireActive(before);
        var last = before.attributes().latest().orElseThrow();
        var changed = store.update(
                DAILY_ROUTINES,
                before,
                before.name(),
                before.attributes().with(new Version(tomorrow(), last.name(), last.time(), false)));
        var row = store.deactivate(DAILY_ROUTINES, changed);
        log("DEACTIVATED", before, row);
    }

    static Routine view(CatalogItem<DailyRoutineAttributes> row) {
        var last = row.attributes().latest().orElseThrow();
        return new Routine(row.id(), last.name(), last.time(), last.active(), last.from(), row.version());
    }

    private LocalDate tomorrow() {
        return time.today().plusDays(1);
    }

    private CatalogItem<DailyRoutineAttributes> matched(long id, String etag) {
        var row = store.require(DAILY_ROUTINES, id);
        VersionEtag.requireMatch(etag, row.version(), Map.of("id", id, "name", row.name(), "version", row.version()));
        return row;
    }

    private static void requireActive(CatalogItem<DailyRoutineAttributes> row) {
        if (row.attributes().latest().map(v -> !v.active()).orElse(true))
            throw new BusinessException(HttpStatus.CONFLICT, "CATALOG_ITEM_INACTIVE", "Mốc sinh hoạt đã ngừng");
    }

    static String validName(String name) {
        if (name == null || name.isBlank() || name.length() > 200)
            throw new InvalidValueException("Tên mốc sinh hoạt phải có từ 1 đến 200 ký tự");
        return name.strip();
    }

    static LocalTime validTime(String at) {
        try {
            if (at == null || !at.matches("[0-9]{2}:[0-9]{2}")) throw new DateTimeParseException("", "", 0);
            return LocalTime.parse(at);
        } catch (DateTimeParseException ex) {
            throw new InvalidValueException("Giờ mốc sinh hoạt phải dạng HH:mm");
        }
    }

    private void log(
            String action, CatalogItem<DailyRoutineAttributes> before, CatalogItem<DailyRoutineAttributes> after) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("DAILY_ROUTINE_" + action, false))
                .entity(DAILY_ROUTINES.code(), after.id())
                .before(before == null ? Map.of() : Map.of("attributes", before.attributes()))
                .after(Map.of(
                        "attributes",
                        after.attributes(),
                        "status",
                        after.status().name()))
                .build());
    }
}
