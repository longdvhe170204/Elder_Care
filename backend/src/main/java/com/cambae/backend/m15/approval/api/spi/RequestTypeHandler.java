package com.cambae.backend.m15.approval.api.spi;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ExtendedTransitions;
import com.cambae.backend.m15.approval.api.ResultRef;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * SPI của một loại yêu cầu, do module sở hữu cài (contracts/module-interfaces.md mục 1.5, research R-10). Nội dung là
 * JSON, handler tự kiểm và tự đọc ({@code kieuNoiDung} của contracts gộp vào {@link #validateContent}).
 */
public interface RequestTypeHandler {

    /** Mã loại, ví dụ {@code M02.RESIDENCY_CHANGE}. */
    String typeCode();

    /** Nội dung sai thì ném lỗi 422. */
    void validateContent(JsonNode content);

    /** Khóa quyền tạo (cột T của Phụ lục 27). */
    PermissionKey createPermission();

    /** Khóa quyền duyệt (cột D của Phụ lục 27). */
    PermissionKey approvePermission();

    /** Đối tượng chính của yêu cầu, để kiểm phạm vi duyệt (FR-034); tính từ nội dung. */
    Target scope(JsonNode content);

    /** Điều kiện áp dụng chưa thỏa; rỗng là thỏa (FR-038). */
    List<UnmetCondition> checkApplyConditions(ApprovalRequestView request);

    /** Ghi tác động trong transaction áp dụng; ném lỗi thì rollback, yêu cầu giữ Đã duyệt (FR-037b). */
    ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate);

    /** 013 FR-027a: false thì người yêu cầu không được tự duyệt. */
    default boolean allowSelfApproval() {
        return true;
    }

    /** Mốc nhắc riêng ngắn hơn CFG-M15-05 do module quy định (FR-031a). */
    default Optional<ParameterCode> reminderParameter() {
        return Optional.empty();
    }

    default ExtendedTransitions extendedTransitions() {
        return ExtendedTransitions.NONE;
    }

    /** Kiểm thêm khi Duyệt, trước khi đổi trạng thái; không rỗng thì 409, yêu cầu giữ Chờ duyệt (bổ sung theo plan 003). */
    default List<UnmetCondition> checkOnApprove(ApprovalRequestView request) {
        return List.of();
    }

    /** Tác động kèm quyết định Duyệt, trong transaction của lệnh (bổ sung theo plan 003); không gọi khi Từ chối. */
    default void onApprove(ApprovalRequestView request, LocalDate actualEffectiveDate) {}

    /**
     * Giới hạn vai trò người duyệt (bổ sung theo plan 004, Q-338): rỗng là không giới hạn thêm; có giá trị thì người duyệt
     * phải có ít nhất một vai trò trong tập. Tính lại ở mỗi lần xét (FR-034a).
     */
    default Optional<Set<Role>> approverRoleRestriction(ApprovalRequestView request) {
        return Optional.empty();
    }
}
