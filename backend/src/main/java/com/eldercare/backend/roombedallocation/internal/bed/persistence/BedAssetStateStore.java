package com.eldercare.backend.roombedallocation.internal.bed.persistence;

import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T042: dữ liệu phân bổ cần kiểm/hủy trong tác động tài sản; không ghi trạng thái giường. */
@Repository
public class BedAssetStateStore {
    public record FutureAllocation(long id, long residentId, String basisType, Long basisId, Long previousId) {
        public boolean hasLinkedSource() {
            return basisType != null || basisId != null || previousId != null;
        }

        /** Phân bổ đặt trước cho yêu cầu chuyển theo lịch (căn cứ là yêu cầu phê duyệt, T033). */
        public boolean scheduledTransfer() {
            return "APPROVAL_REQUEST".equals(basisType) && basisId != null;
        }
    }

    private final JdbcTemplate jdbc;

    public BedAssetStateStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<FutureAllocation> future(long bedId, boolean current) {
        return jdbc.query(
                "SELECT id,resident_id,basis_type,basis_id,previous_allocation_id FROM bed_allocation WHERE bed_id=? AND status='FUTURE' ORDER BY resident_id,id"
                        + (current ? " FOR UPDATE" : ""),
                (r, i) -> new FutureAllocation(
                        r.getLong(1),
                        r.getLong(2),
                        r.getString(3),
                        r.getObject(4, Long.class),
                        r.getObject(5, Long.class)),
                bedId);
    }

    public boolean hasActive(long bedId) {
        return !jdbc.queryForList(
                        "SELECT id FROM bed_allocation WHERE bed_id=? AND status='ACTIVE' FOR UPDATE",
                        Long.class,
                        bedId)
                .isEmpty();
    }

    /** Người đang nằm (phân bổ đang hiệu lực) hoặc người vắng được giữ chỗ của giường (Q-256). */
    public Optional<Long> occupant(long bedId) {
        return jdbc
                .queryForList(
                        "SELECT resident_id FROM bed_allocation WHERE bed_id=? AND status='ACTIVE'"
                                + " UNION ALL SELECT hold_resident_id FROM bed WHERE id=? AND hold_resident_id IS NOT NULL",
                        Long.class,
                        bedId,
                        bedId)
                .stream()
                .findFirst();
    }

    /** Giường Trống của phòng đang hiệu lực, trừ {@code excludedBedId}, theo tầng (Q-256). */
    public List<LocationView> emptyBeds(long excludedBedId) {
        return jdbc.query(
                "SELECT a.id,a.name,f.id,f.name,r.id,r.name,b.id,b.code FROM bed b JOIN room r ON r.id=b.room_id"
                        + " JOIN floor f ON f.id=r.floor_id JOIN area a ON a.id=f.area_id"
                        + " WHERE b.state='EMPTY' AND r.status='ACTIVE' AND b.id<>? ORDER BY f.id,r.id,b.id",
                (r, i) -> new LocationView(
                        r.getLong(1),
                        r.getString(2),
                        r.getLong(3),
                        r.getString(4),
                        r.getLong(5),
                        r.getString(6),
                        r.getLong(7),
                        r.getString(8)),
                excludedBedId);
    }

    public com.eldercare.backend.roombedallocation.api.dto.BedStatus state(long bedId) {
        return com.eldercare.backend.roombedallocation.api.dto.BedStatus.valueOf(
                jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bedId));
    }

    public void cancel(long id, long actorId) {
        jdbc.update(
                "UPDATE bed_allocation SET status='CANCELLED',cancellation_reason='Giường hỏng',closed_by_id=? WHERE id=? AND status='FUTURE'",
                actorId,
                id);
    }
}
