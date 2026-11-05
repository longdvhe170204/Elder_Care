package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.belongings.internal.belonging.domain.LockedBelonging;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingResidentLock;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khóa của lệnh trên đồ gửi ({@code KhoaDoGui}; research K-03, FR-011b): dòng M12 của người cao tuổi trước, rồi từng
 * đồ theo {@code id} tăng; phiên bản mỗi đồ trong lệnh phải bằng phiên bản hiện hành, lệch thì 409
 * {@code STATE_CHANGED} kèm trạng thái mới của các đồ.
 */
@Service
public class BelongingLocks {

    private final BelongingResidentLock residents;
    private final BelongingStore belongings;

    BelongingLocks(BelongingResidentLock residents, BelongingStore belongings) {
        this.residents = residents;
        this.belongings = belongings;
    }

    /**
     * Khóa như {@link #lock} nhưng không so phiên bản: cho việc do hệ thống thực hiện trên đồ đọc ngay trong transaction
     * (áp dụng đề nghị xử lý, hoàn thành phiếu kiểm kê). Đồ không thuộc người cao tuổi không có trong kết quả.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<LockedBelonging> lockCurrent(long residentId, java.util.Collection<Long> ids) {
        residents.lock(residentId);
        return belongings.lockForUpdate(residentId, new TreeSet<>(ids));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<LockedBelonging> lock(long residentId, Map<Long, Integer> versions) {
        residents.lock(residentId);
        List<LockedBelonging> locked = belongings.lockForUpdate(residentId, new TreeSet<>(versions.keySet()));
        if (locked.size() != versions.size()) {
            throw new NotFoundException();
        }
        List<LockedBelonging> stale =
                locked.stream().filter(b -> b.version() != versions.get(b.id())).toList();
        if (!stale.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "STATE_CHANGED",
                    "Đồ gửi đã đổi trạng thái sau khi lập lệnh (FR-011b)",
                    Map.of("currentValue", stale));
        }
        return locked;
    }
}
