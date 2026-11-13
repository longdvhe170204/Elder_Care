package com.eldercare.backend.roombedallocation.internal.daycare.persistence;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.roombedallocation.internal.daycare.domain.DayCareAreaState;
import com.eldercare.backend.roombedallocation.internal.daycare.domain.DayCareSessionState;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Khóa ổn định trước khi đọc cấu hình; locking read tránh snapshot cũ của transaction gọi từ 004. */
@Repository
public class DayCareStore {
    private final JdbcTemplate jdbc;
    private final EntityManager entities;

    public DayCareStore(JdbcTemplate jdbc, EntityManager entities) {
        this.jdbc = jdbc;
        this.entities = entities;
    }

    public <T extends CatalogBase> T find(Class<T> type, long id) {
        T value = entities.find(type, id, LockModeType.PESSIMISTIC_WRITE);
        if (value == null) throw new NotFoundException();
        entities.refresh(value, LockModeType.PESSIMISTIC_WRITE);
        return value;
    }

    public void save(CatalogBase value) {
        entities.persist(value);
        entities.flush();
    }

    public void flush() {
        entities.flush();
    }

    public void lock() {
        // T066: dòng technical_lock M03.SUC_CHUA_BAN_TRU do V003_95 tạo (thay day_care_capacity_guard).
        jdbc.queryForObject(
                "SELECT lock_key FROM technical_lock WHERE lock_key='M03.SUC_CHUA_BAN_TRU' FOR UPDATE", String.class);
    }

    public List<DayCareAreaState> areas(boolean current) {
        return jdbc.query(
                "SELECT id,JSON_VALUE(attributes,'$.code') code,name,JSON_VALUE(attributes,'$.floorId' RETURNING SIGNED) floor_id,JSON_VALUE(attributes,'$.areaId' RETURNING SIGNED) area_id,status,version"
                        + " FROM catalog_item WHERE catalog_type='M03.DAY_CARE_REST_AREA' ORDER BY id"
                        + (current ? " FOR UPDATE" : ""),
                (r, n) -> new DayCareAreaState(
                        r.getLong("id"),
                        r.getString("code"),
                        r.getString("name"),
                        r.getObject("floor_id", Long.class),
                        r.getObject("area_id", Long.class),
                        r.getString("status"),
                        r.getInt("version")));
    }

    public Optional<DayCareAreaState> activeArea(boolean current) {
        return areas(current).stream().filter(x -> "ACTIVE".equals(x.status())).findFirst();
    }

    public List<DayCareSessionState> sessions(boolean current) {
        return jdbc.query(
                "SELECT * FROM day_care_session ORDER BY start_time,id" + (current ? " FOR SHARE" : ""),
                (r, n) -> new DayCareSessionState(
                        r.getLong("id"),
                        r.getString("name"),
                        r.getObject("start_time", java.time.LocalTime.class),
                        r.getObject("end_time", java.time.LocalTime.class),
                        r.getString("status"),
                        r.getInt("version")));
    }
}
