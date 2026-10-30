package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.api.BoundDeviceApi;
import com.eldercare.backend.administration.account.api.dto.ResidentDevice;
import com.eldercare.backend.administration.account.internal.auth.domain.AccountAuditActions;
import com.eldercare.backend.administration.account.internal.auth.domain.Binding;
import com.eldercare.backend.administration.account.internal.auth.persistence.DeviceBindings;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thiết bị gắn của tài khoản người cao tuổi (002 T092, T093; FR-070, FR-071, FR-014a, BR-M15-24; research A-15): một
 * tài khoản một thiết bị, một thiết bị một tài khoản; gắn máy mới gỡ máy cũ và kết thúc phiên trên đó; token thiết bị
 * 256 bit, chỉ lưu SHA-256, trả cho app một lần. Cài {@link BoundDeviceApi} cho nút SOS của 007.
 */
@Service
public class ResidentDevices implements BoundDeviceApi {

    private final DeviceBindings bindings;
    private final AuditLogApi auditLog;

    ResidentDevices(DeviceBindings bindings, AuditLogApi auditLog) {
        this.bindings = bindings;
        this.auditLog = auditLog;
    }

    /** Thiết bị đang gắn với tài khoản khác thì lệnh đăng nhập trả 409 {@code DEVICE_ALREADY_BOUND} (FR-014a, A-15). */
    public boolean boundToOther(long accountId, String deviceId) {
        return bindings.lockActiveOfDevice(deviceId)
                .filter(b -> b.accountId() != accountId)
                .isPresent();
    }

    /**
     * Gắn {@code deviceId} với tài khoản trong transaction của lệnh đăng nhập: gỡ thiết bị đang gắn (kể cả chính thiết
     * bị này) với lý do {@code NEW_DEVICE_BOUND}, kết thúc phiên trên đó; trả token thiết bị. Nhật ký ghi theo người
     * dùng hiện tại (người cao tuổi vừa đăng nhập).
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public String bind(long accountId, String deviceId, Long qrCodeId) {
        bindings.lockActiveOfAccount(accountId).ifPresent(old -> unbind(old, "NEW_DEVICE_BOUND", null, null, null));
        String token = SessionTokens.generate();
        bindings.bind(accountId, deviceId, SessionTokens.hash(token), qrCodeId);
        Map<String, Object> after = new HashMap<>();
        after.put("deviceId", deviceId);
        after.put("method", qrCodeId == null ? "PASSWORD" : "QR_CODE");
        auditLog.write(AuditLogEntry.builder()
                .action(AccountAuditActions.DEVICE_BOUND)
                .entity(AccountAuditActions.ENTITY, accountId)
                .after(after)
                .build());
        return token;
    }

    /** Gỡ thiết bị đang gắn (thủ công hoặc khi khóa); rỗng khi không có thiết bị đang gắn. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Optional<Binding> unbindActive(long accountId, String reason, Long actorId, String note) {
        Optional<Binding> active = bindings.lockActiveOfAccount(accountId);
        active.ifPresent(b -> unbind(b, reason, actorId, note, null));
        return active;
    }

    /** Gỡ thiết bị do hệ thống (Bộ lập lịch) khi khóa tài khoản theo sự kiện; nhật ký ghi căn cứ (FR-045). */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Optional<Binding> unbindActive(long accountId, String reason, Basis basis) {
        Optional<Binding> active = bindings.lockActiveOfAccount(accountId);
        active.ifPresent(b -> unbind(b, reason, null, null, basis));
        return active;
    }

    public Optional<Binding> active(long accountId) {
        return bindings.activeOfAccount(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResidentDevice> identify(String deviceToken) {
        if (deviceToken == null || deviceToken.isBlank()) {
            return Optional.empty();
        }
        return bindings.owner(SessionTokens.hash(deviceToken))
                .filter(o -> !"LOCKED".equals(o.accountStatus()))
                .map(o -> new ResidentDevice(o.residentId(), o.accountId(), o.deviceId()));
    }

    private void unbind(Binding binding, String reason, Long actorId, String note, Basis basis) {
        bindings.unbind(binding.id(), reason, actorId, note);
        bindings.endSessionsOnDevice(binding.accountId(), binding.deviceId(), "DEVICE_UNBOUND");
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(AccountAuditActions.DEVICE_UNBOUND)
                .entity(AccountAuditActions.ENTITY, binding.accountId())
                .before(Map.of("deviceId", binding.deviceId()))
                .details(Map.of("unbindReason", reason));
        if (note != null) {
            entry.reason(new Reason(note));
        }
        if (basis != null) {
            entry.basis(basis);
        }
        auditLog.write(entry.build());
    }

    static BusinessException deviceAlreadyBound() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "DEVICE_ALREADY_BOUND",
                "Thiết bị đang gắn với tài khoản người cao tuổi khác; cần gỡ thiết bị trước (FR-014a)");
    }
}
