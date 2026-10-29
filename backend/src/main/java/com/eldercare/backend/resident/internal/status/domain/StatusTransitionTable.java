package com.eldercare.backend.resident.internal.status.domain;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bảng chuyển trạng thái người cao tuổi (FR-044, BR-M01-04; data-model mục 2). Cặp (trạng thái, lệnh) ngoài bảng không
 * có trạng thái đến; không có thao tác "sửa trạng thái". {@code CREATE_PROFILE} không phải một lần chuyển vì chưa có
 * trạng thái "từ".
 */
public final class StatusTransitionTable {

    /** Trạng thái cuối: hồ sơ chỉ đọc (BR-M01-05, FR-049). */
    public static final Set<ResidentStatus> FINAL_STATUSES = Set.copyOf(
            EnumSet.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED));

    /** Ba lệnh của điểm danh (014): do hệ thống, không bắt buộc lý do (FR-046, research H-03). */
    public static final Set<StatusCommand> SYSTEM_COMMANDS = Set.copyOf(EnumSet.of(
            StatusCommand.ROLL_CALL_DEPARTURE, StatusCommand.ROLL_CALL_RETURN, StatusCommand.CANCEL_DEPARTURE_RECORD));

    private static final Map<ResidentStatus, Map<StatusCommand, ResidentStatus>> TABLE =
            new EnumMap<>(ResidentStatus.class);

    static {
        from(
                ResidentStatus.ADMISSION_IN_PROGRESS,
                StatusCommand.COMPLETE_ADMISSION,
                ResidentStatus.IN_RESIDENCE,
                StatusCommand.CANCEL_ADMISSION,
                ResidentStatus.ADMISSION_CANCELLED);
        from(
                ResidentStatus.IN_RESIDENCE,
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                ResidentStatus.TEMPORARILY_ABSENT,
                StatusCommand.ROLL_CALL_DEPARTURE,
                ResidentStatus.OFF_SITE_ACTIVITY,
                StatusCommand.TRANSFER_OUT,
                ResidentStatus.HOSPITALIZED,
                StatusCommand.END_RESIDENCY,
                ResidentStatus.RESIDENCY_ENDED,
                StatusCommand.RECORD_DEATH,
                ResidentStatus.DECEASED);
        from(
                ResidentStatus.TEMPORARILY_ABSENT,
                StatusCommand.RECORD_RETURN,
                ResidentStatus.IN_RESIDENCE,
                StatusCommand.TRANSFER_OUT,
                ResidentStatus.HOSPITALIZED,
                StatusCommand.END_RESIDENCY,
                ResidentStatus.RESIDENCY_ENDED,
                StatusCommand.RECORD_DEATH,
                ResidentStatus.DECEASED);
        from(
                ResidentStatus.OFF_SITE_ACTIVITY,
                StatusCommand.ROLL_CALL_RETURN,
                ResidentStatus.IN_RESIDENCE,
                StatusCommand.CANCEL_DEPARTURE_RECORD,
                ResidentStatus.IN_RESIDENCE,
                StatusCommand.TRANSFER_OUT,
                ResidentStatus.HOSPITALIZED,
                StatusCommand.RECORD_DEATH,
                ResidentStatus.DECEASED);
        from(
                ResidentStatus.HOSPITALIZED,
                StatusCommand.RECORD_RETURN,
                ResidentStatus.IN_RESIDENCE,
                StatusCommand.END_RESIDENCY,
                ResidentStatus.RESIDENCY_ENDED,
                StatusCommand.RECORD_DEATH,
                ResidentStatus.DECEASED);
    }

    private StatusTransitionTable() {}

    /** Trạng thái đến, hoặc rỗng nếu cặp không có trong bảng. */
    public static Optional<ResidentStatus> next(ResidentStatus from, StatusCommand command) {
        return Optional.ofNullable(TABLE.getOrDefault(from, Map.of()).get(command));
    }

    /** Các cặp (lệnh, trạng thái đến) viết liền nhau cho một trạng thái "từ". */
    private static void from(ResidentStatus from, Object... commandAndTarget) {
        Map<StatusCommand, ResidentStatus> row = new EnumMap<>(StatusCommand.class);
        for (int i = 0; i < commandAndTarget.length; i += 2) {
            row.put((StatusCommand) commandAndTarget[i], (ResidentStatus) commandAndTarget[i + 1]);
        }
        TABLE.put(from, row);
    }
}
