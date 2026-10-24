package com.cambae.backend.m01.api;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.file.FileView;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.api.dto.StatusPeriod;
import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Lệnh đổi trạng thái từ module khác (research H-03).
 *
 * <p>Người gọi và lệnh được phép: 014 {@code ROLL_CALL_DEPARTURE}, {@code ROLL_CALL_RETURN}, {@code
 * CANCEL_DEPARTURE_RECORD} (do hệ thống); 007 {@code TRANSFER_OUT} khi ghi chuyển viện trên sự cố Khẩn cấp (BR-M05-14);
 * 004 {@code CANCEL_ADMISSION} khi hồ sơ chờ chuyển Từ chối hoặc Hủy chờ (004 FR-002). Lệnh ngoài bảng này bị từ chối.
 */
public interface ResidentStatusApi {

    /** {@code Propagation.MANDATORY}. */
    TransitionResult execute(
            long residentId,
            StatusCommand command,
            CurrentUser user,
            Optional<Reason> reason,
            JsonNode details,
            Basis basis);

    /** Danh sách điều kiện chưa thỏa, cho 004 FR-004. */
    List<Condition> previewConditions(long residentId, StatusCommand command);

    /**
     * Giấy tờ bệnh viện gắn vào dòng lịch sử của lệnh Ghi nhận trở về, Chuyển viện (FR-047c, BR-M05-32; 006 hiện cạnh
     * phiếu đối chiếu thuốc). {@code historyId} lấy từ {@code ResidentStatusChanged}. Chỉ siêu dữ liệu; tải xuống đi qua
     * {@code GET /files/{id}} và kiểm quyền xem dị ứng, bệnh nền của người cao tuổi đó.
     */
    List<FileView> hospitalDocuments(long historyId);

    /** Khoảng Tạm vắng, Điều trị tại bệnh viện, Hoạt động bên ngoài (003 FR-027 c, FR-036); đọc, không khóa. */
    List<StatusPeriod> statusPeriods(long residentId, Set<ResidentStatus> statuses, Instant from, Instant to);
}
