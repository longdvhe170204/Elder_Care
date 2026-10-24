package com.cambae.backend.m13.internal.channel.domain;

import com.cambae.backend.common.notification.NotificationLevel;
import java.util.Set;

/** Một kênh thông báo đã cấu hình ({@code kenh_thong_bao}; FR-016); {@code levels} chỉ dùng cho kênh khác. */
public record ChannelConfig(String code, String name, boolean enabled, Set<NotificationLevel> levels, long version) {}
