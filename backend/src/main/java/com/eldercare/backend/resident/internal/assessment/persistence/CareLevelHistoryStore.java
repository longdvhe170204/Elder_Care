package com.eldercare.backend.resident.internal.assessment.persistence;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.api.dto.HistoryRow;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.CareLevelAtDate;
import com.eldercare.backend.resident.api.dto.CareLevelBasisType;
import com.eldercare.backend.resident.internal.assessment.domain.CareLevelChange;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Mức chăm sóc hiện hành ({@code resident.current_care_level}) và lịch sử chỉ thêm (FR-052, FR-053; từ 001 T084 là dòng
 * {@code status_history} của {@link CareLevelChange#TRACK}, khóa {@code M01.CLH:<residentId>:<từ>} thay
 * {@code UNIQUE (resident_id, effective_from)}). Hai thay đổi đi cùng nhau trong transaction của lệnh gọi.
 */
@Repository
public class CareLevelHistoryStore {

    private final JdbcTemplate jdbc;
    private final StatusHistoryApi histories;

    CareLevelHistoryStore(JdbcTemplate jdbc, StatusHistoryApi histories) {
        this.jdbc = jdbc;
        this.histories = histories;
    }

    public Optional<CareLevel> current(long residentId) {
        return jdbc
                .query("SELECT current_care_level FROM resident WHERE id = ?", (rs, i) -> rs.getString(1), residentId)
                .stream()
                .filter(code -> code != null)
                .map(CareLevel::valueOf)
                .findFirst();
    }

    /** Đặt mức hiện hành và thêm dòng lịch sử. */
    public void apply(
            long residentId,
            CareLevel level,
            Instant effectiveFrom,
            CareLevelBasisType basisType,
            long basisId,
            Instant recordedAt,
            long recordedBy) {
        jdbc.update("UPDATE resident SET current_care_level = ? WHERE id = ?", level.name(), residentId);
        histories.add(
                CareLevelChange.TRACK,
                residentId,
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        null,
                        level.name(),
                        null,
                        effectiveFrom,
                        null,
                        recordedBy,
                        false,
                        null,
                        basisType.name(),
                        basisId,
                        new CareLevelChange(recordedAt),
                        null,
                        CareLevelChange.uniqueKey(residentId, effectiveFrom)));
    }

    /** Dòng có thời điểm hiệu lực lớn nhất mà không sau {@code at} (FR-053). */
    public Optional<CareLevelAtDate> at(long residentId, Instant at) {
        return histories.at(CareLevelChange.TRACK, residentId, at).map(CareLevelHistoryStore::view);
    }

    /** Mới nhất trước. */
    public List<CareLevelAtDate> history(long residentId) {
        return histories.list(CareLevelChange.TRACK, residentId).reversed().stream()
                .map(CareLevelHistoryStore::view)
                .toList();
    }

    private static CareLevelAtDate view(HistoryRow<CareLevelChange> row) {
        return new CareLevelAtDate(
                CareLevel.valueOf(row.value()),
                row.startsAt(),
                CareLevelBasisType.valueOf(row.basisType()),
                row.basisId());
    }
}
