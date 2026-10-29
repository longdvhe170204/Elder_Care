package com.eldercare.backend.common.data;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp cơ sở cho mọi repository JPA: thêm {@code append} cho {@link AppendOnlyRepository}. {@code append} dùng
 * {@code persist} nên bản ghi đã có (đối tượng tách rời có id) bị từ chối thay vì bị {@code merge} đè.
 */
public class BaseJpaRepository<T, ID> extends SimpleJpaRepository<T, ID> {

    private final EntityManager entityManager;

    public BaseJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Transactional
    public <S extends T> S append(S entity) {
        entityManager.persist(entity);
        return entity;
    }
}
