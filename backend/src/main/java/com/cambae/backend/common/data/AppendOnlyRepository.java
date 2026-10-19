package com.cambae.backend.common.data;

import java.util.Optional;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Repository của bản ghi nhóm 3 (FR-009, research R-05): chỉ thêm và đọc; không có {@code save} cho bản ghi đã có, không
 * có {@code delete}. Module khai thêm truy vấn đọc trong interface con.
 */
@NoRepositoryBean
public interface AppendOnlyRepository<T, ID> extends Repository<T, ID> {

    /** Thêm bản ghi mới; bản ghi đã có (có id) thì lỗi, không ghi đè. */
    <S extends T> S append(S entity);

    Optional<T> findById(ID id);

    boolean existsById(ID id);
}
