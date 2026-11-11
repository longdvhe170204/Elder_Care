package com.eldercare.backend.resident.api;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.file.FileView;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.StatusPeriod;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Lệnh đổi trạng thái từ module khác (research H-03).
 *
 * <p>Người gọi và lệnh được phép: 014 {@code ROLL_CALL_DEPARTURE}, {@code ROLL_CALL_RETURN}, {@code
 * CANCEL_DEPARTURE_RECORD}, {@code CANCEL_RETURN_RECORD} (do hệ thống); 007 {@code TRANSFER_OUT} khi ghi chuyển viện trên sự cố Khẩn cấp (BR-M05-14);
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

    /**
     * Lệnh điểm danh rời viện, về ghi ngoại tuyến (Q-01; đổi api chủ dự án duyệt 2026-11-11): {@code occurredAt} là thời
     * điểm thực tế trên thiết bị, ghi vào lịch sử trạng thái. Chỉ {@code ROLL_CALL_DEPARTURE}, {@code ROLL_CALL_RETURN};
     * ở tương lai hoặc trước lần đổi trạng thái trước thì 422. {@code Propagation.MANDATORY}.
     */
    TransitionResult execute(
            long residentId,
            StatusCommand command,
            CurrentUser user,
            Optional<Reason> reason,
            JsonNode details,
            Basis basis,
            Instant occurredAt);

    /**
     * 014 FR-031a (a) (đổi api chủ dự án duyệt 2026-11-11): đính chính thời điểm thực tế của dòng lịch sử {@code
     * ROLL_CALL_DEPARTURE} hoặc {@code ROLL_CALL_RETURN} mới nhất có căn cứ là chuyến {@code tripId}. Lịch sử chỉ thêm:
     * dòng gốc giữ nguyên, thời điểm đúng ghi thành dòng mới và mọi chỗ đọc lịch sử (khoảng trạng thái, số liệu) dùng thời
     * điểm này. Không đổi trạng thái, không chạy lại tác động của lệnh; người gọi báo các module tính lại. Ở tương lai,
     * không sau dòng trước hoặc không trước dòng sau của người đó thì 422; không có dòng thì 404. Trả thời điểm trước khi
     * đính chính. {@code Propagation.MANDATORY}.
     */
    Instant correctRollCallTime(
            long residentId,
            StatusCommand command,
            long tripId,
            Instant correctedAt,
            CurrentUser user,
            Optional<Reason> reason);

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
