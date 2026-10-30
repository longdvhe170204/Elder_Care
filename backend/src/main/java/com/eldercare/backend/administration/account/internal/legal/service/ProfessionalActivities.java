package com.eldercare.backend.administration.account.internal.legal.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.internal.legal.domain.ProfessionalActivity;
import com.eldercare.backend.administration.account.internal.legal.domain.ProfessionalActivityAttributes;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Danh mục nghiệp vụ chuyên môn ở danh mục chung loại {@code M15.PROFESSIONAL_ACTIVITY} (002 T063, T098; research A-09,
 * R-15; FR-036 → FR-038): tên, trạng thái, version của mục danh mục; chức năng, loại giấy phép, phạm vi hành nghề trong
 * thuộc tính. Mỗi lệnh sửa đổi một lần mục danh mục nên version tăng một.
 */
@Component
public class ProfessionalActivities {

    private static final Pattern GENERATED_CODE = Pattern.compile("PA-(\\d+)");

    private final CatalogApi catalog;
    private final TechnicalLocks locks;

    ProfessionalActivities(CatalogApi catalog, TechnicalLocks locks) {
        this.catalog = catalog;
        this.locks = locks;
    }

    /** Theo id. */
    public List<ProfessionalActivity> all() {
        return items().stream().map(ProfessionalActivities::activity).toList();
    }

    /** Mục đang hiệu lực của chức năng: thao tác chuyên môn phải thỏa mọi mục (FR-037). */
    public List<ProfessionalActivity> activeFor(Function function) {
        return all().stream()
                .filter(a -> a.active() && a.function() == function)
                .toList();
    }

    public Optional<ProfessionalActivity> byCode(String code) {
        return catalog.findByCode(ProfessionalActivityAttributes.KIND, null, code)
                .map(ProfessionalActivities::activity);
    }

    /** Khóa mục cho lệnh sửa, ngừng hiệu lực tới hết transaction (kiểm lại trên bản mới, 000 FR-040). */
    public Optional<ProfessionalActivity> lock(long id) {
        locks.lock(ProfessionalActivityAttributes.KIND.code() + ":" + id);
        return find(id);
    }

    public Optional<ProfessionalActivity> find(long id) {
        try {
            return Optional.of(activity(catalog.require(ProfessionalActivityAttributes.KIND, id)));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /** Có mục đang hiệu lực khác {@code exceptId} cùng tên (FR-036). */
    public boolean nameTaken(String name, long exceptId) {
        return items().stream()
                .anyMatch(i -> i.id() != exceptId
                        && i.status() == CatalogStatus.ACTIVE
                        && i.name().equals(name));
    }

    /**
     * Mục mới, mã {@code PA-<n>} (mã kỹ thuật, cũng là mã phạm vi hoạt động của cơ sở khi mục cần): n kế tiếp số lớn nhất
     * đã cấp, tính dưới khóa để hai lệnh đồng thời không cùng số.
     */
    public long insert(
            String name, Function function, String licenseType, boolean requiresFacilityScope, Set<Long> scopes) {
        locks.lock(ProfessionalActivityAttributes.KIND.code() + ":code");
        long next = items().stream()
                        .map(i -> GENERATED_CODE.matcher(i.code()))
                        .filter(Matcher::matches)
                        .mapToLong(m -> Long.parseLong(m.group(1)))
                        .max()
                        .orElse(0)
                + 1;
        return catalog.create(
                        ProfessionalActivityAttributes.KIND,
                        new NewCatalogItem<>(
                                "PA-" + next,
                                name,
                                null,
                                0,
                                new ProfessionalActivityAttributes(
                                        function, licenseType, requiresFacilityScope, false, sorted(scopes))))
                .id();
    }

    public void update(
            long id,
            String name,
            Function function,
            String licenseType,
            boolean requiresFacilityScope,
            Set<Long> scopes) {
        CatalogItem<ProfessionalActivityAttributes> item = catalog.require(ProfessionalActivityAttributes.KIND, id);
        catalog.update(
                ProfessionalActivityAttributes.KIND,
                id,
                item.version(),
                name,
                item.sortOrder(),
                new ProfessionalActivityAttributes(
                        function,
                        licenseType,
                        requiresFacilityScope,
                        item.attributes().isDefault(),
                        sorted(scopes)));
    }

    public void deactivate(long id) {
        CatalogItem<ProfessionalActivityAttributes> item = catalog.require(ProfessionalActivityAttributes.KIND, id);
        catalog.deactivate(ProfessionalActivityAttributes.KIND, id, item.version());
    }

    private List<CatalogItem<ProfessionalActivityAttributes>> items() {
        return catalog.list(ProfessionalActivityAttributes.KIND, null, false).stream()
                .sorted(Comparator.comparingLong(CatalogItem::id))
                .toList();
    }

    private static List<Long> sorted(Set<Long> scopes) {
        return scopes.stream().sorted().toList();
    }

    private static ProfessionalActivity activity(CatalogItem<ProfessionalActivityAttributes> item) {
        ProfessionalActivityAttributes a = item.attributes();
        return new ProfessionalActivity(
                item.id(),
                item.code(),
                item.name(),
                a.functionCode(),
                a.requiredLicenseType(),
                Set.copyOf(a.practiceScopeIds()),
                a.requiresFacilityScope(),
                a.isDefault(),
                item.status() == CatalogStatus.ACTIVE,
                item.version());
    }
}
