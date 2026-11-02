package com.eldercare.backend.notification.internal.channel.persistence;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.notification.internal.channel.domain.ChannelAttributes;
import com.eldercare.backend.notification.internal.channel.domain.ChannelConfig;
import com.eldercare.backend.notification.internal.delivery.domain.ChannelSelection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

/**
 * Kênh thông báo ({@code kenh_thong_bao}, nhóm 1; FR-016, data-model mục 0.1, 1) ở danh mục chung loại
 * {@code M13.NOTIFICATION_CHANNEL} (009 T043).
 */
@Repository
public class ChannelStore {

    private final CatalogApi catalog;

    ChannelStore(CatalogApi catalog) {
        this.catalog = catalog;
    }

    public List<ChannelConfig> all() {
        return catalog.list(ChannelAttributes.KIND, null, false).stream()
                .sorted(Comparator.comparing(CatalogItem::code))
                .map(c -> new ChannelConfig(
                        c.code(), c.name(), c.attributes().enabled(), levels(c.attributes()), c.version()))
                .toList();
    }

    /** Kênh ngoài ứng dụng cho chọn kênh. */
    public List<ChannelSelection.Channel> channels() {
        return all().stream()
                .map(c -> new ChannelSelection.Channel(c.code(), c.enabled(), c.levels()))
                .toList();
    }

    /** Đổi một kênh; trả số dòng đổi (0 khi không có kênh). */
    public int update(String code, boolean enabled, Set<NotificationLevel> levels) {
        Optional<CatalogItem<ChannelAttributes>> found = catalog.findByCode(ChannelAttributes.KIND, null, code);
        if (found.isEmpty()) {
            return 0;
        }
        CatalogItem<ChannelAttributes> c = found.get();
        catalog.update(
                ChannelAttributes.KIND,
                c.id(),
                c.version(),
                c.name(),
                c.sortOrder(),
                new ChannelAttributes(
                        enabled,
                        levels.stream().sorted().toList(),
                        c.attributes().config()));
        return 1;
    }

    private static Set<NotificationLevel> levels(ChannelAttributes a) {
        Set<NotificationLevel> out = EnumSet.noneOf(NotificationLevel.class);
        if (a.levels() != null) {
            out.addAll(a.levels());
        }
        return out;
    }
}
