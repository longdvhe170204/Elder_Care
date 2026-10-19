package com.cambae.backend.m15.account.api;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Kiểm quyền ba lớp BR-M15-01 (vai trò, phạm vi, pháp lý), điểm kiểm tra duy nhất (002 research A-10). 000 khai báo
 * trước (000 T025); 002 thêm các hàm còn lại của contracts/module-interfaces.md mục 1.2 (T008).
 */
public interface PermissionApi {

    PermissionResult hasPermission(CurrentUser user, PermissionKey key, Target target);

    /** Kiểm tại thời điểm thiết bị, cho bản ghi ngoại tuyến (FR-041a; research A-11). */
    PermissionResult hasPermissionAt(CurrentUser user, PermissionKey key, Target target, Instant at);

    /** Tập khóa quyền của người dùng, cho {@code GET /me/permissions} (FR-042). */
    Set<PermissionKey> permissions(CurrentUser user);

    /** Tên lệnh được phép trên đối tượng, cho {@code allowedCommands} (FR-042); {@code commands}: tên lệnh → khóa quyền. */
    Set<String> allowedCommands(CurrentUser user, Target target, Map<String, PermissionKey> commands);
}
