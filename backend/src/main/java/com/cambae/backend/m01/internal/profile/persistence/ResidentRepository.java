package com.cambae.backend.m01.internal.profile.persistence;

import com.cambae.backend.m01.internal.profile.domain.Resident;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResidentRepository extends JpaRepository<Resident, Long> {

    /** Khóa dòng hồ sơ trước khi sửa (lệnh ghi của M01 khóa rồi kiểm lại trạng thái; FR-048). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resident r where r.id = :id")
    Optional<Resident> lockById(@Param("id") Long id);
}
