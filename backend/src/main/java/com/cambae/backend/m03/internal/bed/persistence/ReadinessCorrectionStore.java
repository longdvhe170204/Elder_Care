package com.cambae.backend.m03.internal.bed.persistence;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m03.internal.bed.domain.ReadinessCorrectionOriginal;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T047: không cập nhật lịch sử gốc hoặc chạy lại máy trạng thái khi đính chính. */
@Repository
public class ReadinessCorrectionStore {
    private final JdbcTemplate jdbc;

    public ReadinessCorrectionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ReadinessCorrectionOriginal> find(long id) {
        return jdbc
                .query(
                        "SELECT * FROM bed_state_history WHERE id=?",
                        (rs, i) -> new ReadinessCorrectionOriginal(
                                rs.getLong("id"),
                                rs.getLong("bed_id"),
                                rs.getLong("floor_area_id"),
                                rs.getObject("actor_id", Long.class),
                                rs.getString("event"),
                                rs.getObject("disinfected", Boolean.class),
                                rs.getObject("ppe_used", Boolean.class),
                                rs.getString("note")),
                        id)
                .stream()
                .findFirst();
    }

    public ReadinessCorrectionOriginal required(long id) {
        return find(id).orElseThrow(NotFoundException::new);
    }
}
