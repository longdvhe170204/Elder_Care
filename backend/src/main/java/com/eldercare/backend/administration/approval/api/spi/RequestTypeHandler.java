package com.eldercare.backend.administration.approval.api.spi;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.UnmetCondition;
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

    /**
     * Loại tự kiểm phạm vi của người lập (bổ sung theo plan 015, 000 T-015/T033; 015 FR-019, FR-026): có giá trị thì 000
     * chỉ kiểm người lập giữ khóa {@link #createPermission()} (không xét phạm vi theo phân công hiện tại của 002) và lấy
     * giá trị này làm kết quả kiểm phạm vi, ví dụ "có tên trong ca Đã công bố chưa bắt đầu"; rỗng thì kiểm quyền theo
     * phạm vi như mọi loại.
     */
    default Optional<Boolean> creatorInOwnScope(CurrentUser user, JsonNode content) {
        return Optional.empty();
    }

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

    /**
     * Người được sửa nội dung và gửi duyệt yêu cầu còn Nháp (bổ sung theo plan 004 FR-039: Hành chính hoàn thiện yêu cầu
     * người đại diện gửi qua cổng người thân): mặc định chỉ người yêu cầu. Người yêu cầu giữ nguyên; chỉ người yêu cầu
     * được Hủy.
     */
    default boolean mayActOnDraft(CurrentUser user, ApprovalRequestView request) {
        return request.requesterId() == user.accountId();
    }
}
