package com.eldercare.backend.care.internal.catalog.persistence;

import com.eldercare.backend.care.internal.catalog.domain.NonPerformanceReasonType;
import com.eldercare.backend.common.error.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Truy cập duy nhất danh mục loại lý do, không đọc bảng của story công việc. */
@Repository
public class NonPerformanceReasonStore {
    private final EntityManager entities;

    public NonPerformanceReasonStore(EntityManager entities) {
        this.entities = entities;
    }

    public List<NonPerformanceReasonType> list() {
        return entities.createQuery(
                        "SELECT r FROM NonPerformanceReasonType r ORDER BY r.id", NonPerformanceReasonType.class)
                .getResultList();
    }

    public NonPerformanceReasonType lock(long id) {
        var row = entities.find(NonPerformanceReasonType.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (row == null) throw new NotFoundException();
        return row;
    }

    public NonPerformanceReasonType create(String name) {
        var row = new NonPerformanceReasonType(name);
        entities.persist(row);
        entities.flush();
        return row;
    }

    public void flush() {
        entities.flush();
    }
}
