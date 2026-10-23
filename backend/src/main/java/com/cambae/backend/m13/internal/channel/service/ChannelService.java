package com.cambae.backend.m13.internal.channel.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.m13.internal.channel.domain.ChannelConfig;
import com.cambae.backend.m13.internal.channel.persistence.ChannelStore;
import com.cambae.backend.m13.internal.delivery.domain.ChannelSelection;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cấu hình kênh thông báo (FR-016, FR-039, FR-040): Quản lý viện bật/tắt kênh, gắn kênh khác vào mức; bắt buộc lý do,
 * ghi nhật ký. Chống ghi đè theo ETag của cả danh sách kênh (tổng phiên bản), vì schema {@code NotificationChannel} của
 * hợp đồng không có phiên bản.
 */
@Service
public class ChannelService {

    static final AuditAction CHANGED = new AuditAction("NOTIFICATION_CHANNEL_CHANGED", true);
    static final String ENTITY = "M13.NOTIFICATION_CHANNEL";

    /** Danh sách kênh kèm ETag. */
    public record Channels(List<ChannelConfig> channels, String etag) {}

    private final ChannelStore channels;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;

    ChannelService(ChannelStore channels, ObjectProvider<CurrentUser> currentUser, AuditLogApi auditLog) {
        this.channels = channels;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
    }

    @Transactional(readOnly = true)
    public Channels list() {
        requireManager();
        return current();
    }

    /** Đổi một kênh; {@code levels} chỉ áp cho kênh khác ({@code OTHER_*}). */
    @BusinessCommand(requiresReason = true)
    @Transactional
    public Channels change(
            String code, String ifMatch, boolean enabled, Optional<Set<NotificationLevel>> levels, Reason reason) {
        requireManager();
        Channels before = current();
        if (!before.etag().equals(ifMatch == null ? "" : ifMatch.strip())) {
            throw new VersionMismatchException(before.channels());
        }
        ChannelConfig row = before.channels().stream()
                .filter(c -> c.code().equals(code))
                .findFirst()
                .orElseThrow(NotFoundException::new);
        Set<NotificationLevel> nextLevels =
                code.startsWith(ChannelSelection.OTHER_PREFIX) ? levels.orElse(row.levels()) : row.levels();
        channels.update(code, enabled, nextLevels);
        auditLog.write(AuditLogEntry.builder()
                .action(CHANGED)
                .entity(ENTITY, code)
                .before(Map.of("enabled", row.enabled(), "levels", new TreeSet<>(row.levels())))
                .after(Map.of("enabled", enabled, "levels", new TreeSet<>(nextLevels)))
                .reason(reason)
                .build());
        return current();
    }

    private Channels current() {
        List<ChannelConfig> all = channels.all();
        long sum = all.stream().mapToLong(ChannelConfig::version).sum();
        return new Channels(all, "\"" + sum + "\"");
    }

    private void requireManager() {
        if (!currentUser.getObject().roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Chỉ Quản lý viện cấu hình kênh thông báo (FR-039)");
        }
    }
}
