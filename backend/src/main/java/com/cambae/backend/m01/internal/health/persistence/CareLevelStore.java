package com.cambae.backend.m01.internal.health.persistence;

import com.cambae.backend.common.data.CatalogStatus;
import com.cambae.backend.m01.internal.health.domain.CatalogItemView;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Ba mức chăm sóc cố định, chỉ đọc (data-model mục 3; Q-291). {@code id} của mục là thứ tự hiển thị. */
@Repository
public class CareLevelStore {

    private final JdbcTemplate jdbc;

    CareLevelStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CatalogItemView> all() {
        return jdbc.query(
                "SELECT name, display_order FROM care_level ORDER BY display_order",
                (rs, i) -> new CatalogItemView(
                        rs.getInt("display_order"),
                        rs.getString("name"),
                        null,
                        rs.getInt("display_order"),
                        CatalogStatus.ACTIVE,
                        0));
    }
}
