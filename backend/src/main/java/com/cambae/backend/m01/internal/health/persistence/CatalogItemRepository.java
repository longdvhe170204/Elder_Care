package com.cambae.backend.m01.internal.health.persistence;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.data.CatalogStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

/** Truy cập chung của các danh mục nhóm 1 của M01. */
@NoRepositoryBean
public interface CatalogItemRepository<E extends CatalogBase> extends JpaRepository<E, Long> {

    Optional<E> findByNameAndStatus(String name, CatalogStatus status);

    List<E> findByStatusOrderByIdAsc(CatalogStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from #{#entityName} e where e.id = :id")
    Optional<E> lockById(@Param("id") Long id);
}
