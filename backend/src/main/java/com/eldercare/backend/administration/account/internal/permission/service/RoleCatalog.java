package com.eldercare.backend.administration.account.internal.permission.service;

import com.eldercare.backend.administration.account.internal.permission.domain.PermissionScope;
import com.eldercare.backend.administration.account.internal.permission.domain.PermissionViews.RoleInfo;
import com.eldercare.backend.administration.account.internal.permission.domain.RoleAttributes;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.common.error.VersionMismatchException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Vai trò ở danh mục chung loại {@code M15.ROLE} (002 T098, research R-15; FR-016, A-05): thông tin vai trò, phạm vi của
 * T, D và số phiên bản quyền của vai trò.
 */
@Service
public class RoleCatalog {

    private final CatalogApi catalog;

    RoleCatalog(CatalogApi catalog) {
        this.catalog = catalog;
    }

    /** Mọi vai trò, theo mã. */
    public List<RoleInfo> roles() {
        return items().stream()
                .map(r -> new RoleInfo(
                        r.code(),
                        r.name(),
                        r.attributes().accountType(),
                        r.attributes().executionScope()))
                .toList();
    }

    public Optional<Integer> version(String role) {
        return item(role).map(r -> r.attributes().permissionVersion());
    }

    /** Tổng số phiên bản quyền của các vai trò đã cho (phần vai trò của khóa bộ đệm quyền, FR-027). */
    public long versionSum(Collection<String> roles) {
        Map<String, Integer> versions = items().stream()
                .collect(Collectors.toMap(CatalogItem::code, r -> r.attributes().permissionVersion()));
        return roles.stream().mapToLong(r -> versions.getOrDefault(r, 0)).sum();
    }

    /** Phạm vi của T, D theo vai trò (FR-034). */
    public Map<String, PermissionScope> executionScopes() {
        return items().stream()
                .collect(Collectors.toMap(CatalogItem::code, r -> r.attributes().executionScope()));
    }

    /**
     * Tăng số phiên bản quyền của vai trò khi bằng {@code expected} (BR-M15-17); trả {@code false} khi phiên bản đã khác,
     * kể cả khi lệnh khác vừa tăng trong lúc chờ khóa dòng của mục danh mục.
     */
    public boolean bumpVersion(String role, long expected) {
        Optional<CatalogItem<RoleAttributes>> found = item(role);
        if (found.isEmpty() || found.get().attributes().permissionVersion() != expected) {
            return false;
        }
        CatalogItem<RoleAttributes> r = found.get();
        try {
            catalog.update(
                    RoleAttributes.KIND,
                    r.id(),
                    r.version(),
                    r.name(),
                    r.sortOrder(),
                    r.attributes().withNextVersion());
            return true;
        } catch (VersionMismatchException e) {
            return false;
        }
    }

    private Optional<CatalogItem<RoleAttributes>> item(String role) {
        return catalog.findByCode(RoleAttributes.KIND, null, role);
    }

    private List<CatalogItem<RoleAttributes>> items() {
        return catalog.list(RoleAttributes.KIND, null, false).stream()
                .sorted(Comparator.comparing(CatalogItem::code))
                .toList();
    }
}
