package com.cambae.backend.m01.internal.health.persistence;

import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.internal.health.domain.HealthItem;
import com.cambae.backend.m01.internal.health.domain.HealthItemStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthItemRepository extends JpaRepository<HealthItem, Long> {

    List<HealthItem> findByResidentIdOrderByIdDesc(long residentId);

    List<HealthItem> findByResidentIdAndStatusOrderByIdDesc(long residentId, HealthItemStatus status);

    List<HealthItem> findByResidentIdAndStatusAndTypeInOrderByIdAsc(
            long residentId, HealthItemStatus status, Collection<HealthItemType> types);

    /** Mục Hiệu lực cùng loại, để so trùng (FR-021). */
    List<HealthItem> findByResidentIdAndTypeAndStatus(long residentId, HealthItemType type, HealthItemStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from HealthItem i where i.id = :id")
    Optional<HealthItem> lockById(@Param("id") Long id);

    /** Dị ứng Hiệu lực chọn từ danh mục còn cờ chờ kiểm tra, ghi nhận trước {@code before} (BR-M01-12). */
    @Query(
            "select i from HealthItem i where i.status = com.cambae.backend.m01.internal.health.domain.HealthItemStatus.ACTIVE"
                    + " and i.allergenId is not null and i.recordedAt < :before"
                    + " and (i.pendingMedicationCheck = true or i.pendingNutritionCheck = true) order by i.id")
    List<HealthItem> findStillPending(@Param("before") Instant before);
}
