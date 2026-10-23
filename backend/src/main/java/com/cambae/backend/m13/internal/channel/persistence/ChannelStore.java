package com.cambae.backend.m13.internal.channel.persistence;

import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.internal.channel.domain.ChannelConfig;
import com.cambae.backend.m13.internal.delivery.domain.ChannelSelection;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Kênh thông báo ({@code kenh_thong_bao}, nhóm 1; FR-016, data-model mục 1). */
@Repository
public class ChannelStore {

    private final JdbcTemplate jdbc;

    ChannelStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ChannelConfig> all() {
        return jdbc.query(
                "SELECT code, name, enabled, levels, version FROM notification_channel ORDER BY code",
                (rs, i) -> new ChannelConfig(
                        rs.getString(1), rs.getString(2), rs.getBoolean(3), levels(rs.getString(4)), rs.getLong(5)));
    }

    /** Kênh ngoài ứng dụng cho chọn kênh. */
    public List<ChannelSelection.Channel> channels() {
        return all().stream()
                .map(c -> new ChannelSelection.Channel(c.code(), c.enabled(), c.levels()))
                .toList();
    }

    /** Đổi một kênh; trả số dòng đổi (0 khi không có kênh). */
    public int update(String code, boolean enabled, Set<NotificationLevel> levels) {
        return jdbc.update(
                "UPDATE notification_channel SET enabled = ?, levels = ?, version = version + 1 WHERE code = ?",
                enabled,
                levels.stream().map(Enum::name).collect(Collectors.joining(",")),
                code);
    }

    private static Set<NotificationLevel> levels(String set) {
        Set<NotificationLevel> out = EnumSet.noneOf(NotificationLevel.class);
        if (set != null && !set.isBlank()) {
            Arrays.stream(set.split(",")).map(NotificationLevel::valueOf).forEach(out::add);
        }
        return out;
    }
}
