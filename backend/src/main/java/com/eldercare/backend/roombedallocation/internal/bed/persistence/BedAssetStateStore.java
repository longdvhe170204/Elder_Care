package com.eldercare.backend.roombedallocation.internal.bed.persistence;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T042: dữ liệu phân bổ cần kiểm/hủy trong tác động tài sản; không ghi trạng thái giường. */
@Repository
public class BedAssetStateStore {
    public record FutureAllocation(long id, long residentId, String basisType, Long basisId, Long previousId) {
        public boolean hasLinkedSource() {
            return basisType != null || basisId != null || previousId != null;
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

    public void cancel(long id, long actorId) {
        jdbc.update(
                "UPDATE bed_allocation SET status='CANCELLED',cancellation_reason='Giường hỏng',closed_by_id=? WHERE id=? AND status='FUTURE'",
                actorId,
                id);
    }
}
